package com.pplive.sdk;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.*;

import com.pplive.thirdparty.BreakpadUtil;

import android.content.*;
import android.view.*;
import android.os.*;
import android.util.Log;
import android.graphics.*;
import android.media.*;
import android.hardware.*;

import java.io.File;


/**
    SDLAndroid
*/
public class SDLAndroid {

    // Main components
    private static SDLAndroid mSingleton;
    private static Context mContext;
    private static SurfaceView mSurface;
    private static SDLSurfaceCallback mSurfaceCallback;

    // This is what SDLAndroid runs in. It invokes SDLAndroid_main(), eventually
    private static Thread mSDLThread;

    // Audio
    private static Thread mAudioThread;
    private static AudioTrack mAudioTrack;

    // EGL private objects
    private static EGLContext  mEGLContext;
    private static EGLSurface  mEGLSurface;
    private static EGLDisplay  mEGLDisplay;
    private static EGLConfig   mEGLConfig;
    private static int mGLMajor, mGLMinor;

    private static String[] argv;
    // Load the .so
    static {
		String tmpDir = System.getProperty("java.io.tmpdir") + "/ppsdk";
		File tmpDirFile = new File(tmpDir);
		tmpDirFile.mkdir();
		BreakpadUtil.registerBreakpad(tmpDirFile);
        //System.loadLibrary("SDL");
        //System.loadLibrary("main");
        //System.loadLibrary("sdl_jni-armv7a-android-gcc44-mt");
        //System.loadLibrary("sdl_main_jni-armv7a-android-gcc44-mt");
        System.loadLibrary("lenthevcdec");
        System.loadLibrary("ffmpeg");
        System.loadLibrary("sdl_jni-arm-android-r6-gcc44-mt");
        System.loadLibrary("sdl_main_jni-arm-android-r6-gcc44-mt");
    }

    // Setup
    public SDLAndroid() {
        Log.v("SDLAndroid", "construct()");
    	
        // So we can call stuff from static callbacks
        mSingleton = this;
    }
    
    public void open(Context context, SurfaceView surface) {
        Log.v("SDLAndroid", "open()");
    	mContext = context;

    	// Set up the surface
        mSurface = surface;
        mSurfaceCallback = new SDLSurfaceCallback(context, surface);
    }

    public void start(String[] argv) {
        Log.v("SDLAndroid", "start()");
        this.argv = argv;
    }
    
    public static void startApp() {
        Log.v("SDLAndroid", "startApp()");
        // Start up the C app thread
        if (mSDLThread == null) {
            mSDLThread = new Thread(new SDLAndroidMain(argv), "SDLAndroidThread");
            mSDLThread.start();
        }
    }
    
    // Events
    public void pause() {
        Log.v("SDLAndroid", "pause()");
        SDLAndroid.nativePause();
    }

    public void resume() {
        Log.v("SDLAndroid", "resume()");
        SDLAndroid.nativeResume();
    }

    public void stop() {
        Log.v("SDLAndroid", "close()");
        // Send a quit message to the application
        SDLAndroid.nativeQuit();
        
        // Now wait for the SDLAndroid thread to quit
        if (mSDLThread != null) {
            try {
                mSDLThread.join();
            } catch(Exception e) {
                Log.v("SDLAndroid", "Problem stopping thread: " + e);
            }
            mSDLThread = null;

            //Log.v("SDLAndroid", "Finished waiting for SDLAndroid thread");
        }
    }

    // Messages from the SDLAndroidMain thread
    static int COMMAND_CHANGE_TITLE = 1;

    // Handler for the messages
    static Handler commandHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.arg1 == COMMAND_CHANGE_TITLE) {
                //setTitle((String)msg.obj);
            }
        }
    };

    // Send a message from the SDLAndroidMain thread
    void sendCommand(int command, Object data) {
        Message msg = commandHandler.obtainMessage();
        msg.arg1 = command;
        msg.obj = data;
        commandHandler.sendMessage(msg);
    }

	public static void notify(int msg){
		Log.v("SDL", "notify :" + msg);
		//mHandler.sendEmptyMessage(msg);
	}
    
    // C functions we call
    public static native void nativeMain(String[] argv);
    public static native void nativeQuit();
    public static native void nativePause();
    public static native void nativeResume();
    public static native void onNativeResize(int x, int y, int format);
    public static native void onNativeKeyDown(int keycode);
    public static native void onNativeKeyUp(int keycode);
    public static native void onNativeTouch(int touchDevId, int pointerFingerId,
                                            int action, float x, 
                                            float y, float p);
    public static native void onNativeAccel(float x, float y, float z);
    public static native void nativeRunAudioThread();


    // Java functions called from C

    public static boolean createGLContext(int majorVersion, int minorVersion) {
        return initEGL(majorVersion, minorVersion);
    }

    public static void flipBuffers() {
        flipEGL();
    }

    public static void setActivityTitle(String title) {
        // Called from SDLAndroidMain() thread and can't directly affect the view
        mSingleton.sendCommand(COMMAND_CHANGE_TITLE, title);
    }

    public static Context getContext() {
        return mContext;
    }
    
    // EGL functions
    public static boolean initEGL(int majorVersion, int minorVersion) {
        if (SDLAndroid.mEGLDisplay == null) {
            Log.v("SDLAndroid", "Starting up OpenGL ES " + majorVersion + "." + minorVersion);

            try {
                EGL10 egl = (EGL10)EGLContext.getEGL();

                EGLDisplay dpy = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

                int[] version = new int[2];
                egl.eglInitialize(dpy, version);

                int EGL_OPENGL_ES_BIT = 1;
                int EGL_OPENGL_ES2_BIT = 4;
                int renderableType = 0;
                if (majorVersion == 2) {
                    renderableType = EGL_OPENGL_ES2_BIT;
                } else if (majorVersion == 1) {
                    renderableType = EGL_OPENGL_ES_BIT;
                }
                int[] configSpec = {
                    //EGL10.EGL_DEPTH_SIZE,   16,
                    EGL10.EGL_RENDERABLE_TYPE, renderableType,
                    EGL10.EGL_NONE
                };
                EGLConfig[] configs = new EGLConfig[1];
                int[] num_config = new int[1];
                if (!egl.eglChooseConfig(dpy, configSpec, configs, 1, num_config) || num_config[0] == 0) {
                    Log.e("SDLAndroid", "No EGL config available");
                    return false;
                }
                EGLConfig config = configs[0];

                /*int EGL_CONTEXT_CLIENT_VERSION=0x3098;
                int contextAttrs[] = new int[] { EGL_CONTEXT_CLIENT_VERSION, majorVersion, EGL10.EGL_NONE };
                EGLContext ctx = egl.eglCreateContext(dpy, config, EGL10.EGL_NO_CONTEXT, contextAttrs);

                if (ctx == EGL10.EGL_NO_CONTEXT) {
                    Log.e("SDLAndroid", "Couldn't create context");
                    return false;
                }
                SDLAndroid.mEGLContext = ctx;*/
                SDLAndroid.mEGLDisplay = dpy;
                SDLAndroid.mEGLConfig = config;
                SDLAndroid.mGLMajor = majorVersion;
                SDLAndroid.mGLMinor = minorVersion;

                SDLAndroid.createEGLSurface();
            } catch(Exception e) {
                Log.v("SDLAndroid", e + "");
                for (StackTraceElement s : e.getStackTrace()) {
                    Log.v("SDLAndroid", s.toString());
                }
            }
        }
        else SDLAndroid.createEGLSurface();

        return true;
    }

    public static boolean createEGLContext() {
        EGL10 egl = (EGL10)EGLContext.getEGL();
        int EGL_CONTEXT_CLIENT_VERSION=0x3098;
        int contextAttrs[] = new int[] { EGL_CONTEXT_CLIENT_VERSION, SDLAndroid.mGLMajor, EGL10.EGL_NONE };
        SDLAndroid.mEGLContext = egl.eglCreateContext(SDLAndroid.mEGLDisplay, SDLAndroid.mEGLConfig, EGL10.EGL_NO_CONTEXT, contextAttrs);
        if (SDLAndroid.mEGLContext == EGL10.EGL_NO_CONTEXT) {
            Log.e("SDLAndroid", "Couldn't create context");
            return false;
        }
        return true;
    }

    public static boolean createEGLSurface() {
        if (SDLAndroid.mEGLDisplay != null && SDLAndroid.mEGLConfig != null) {
            EGL10 egl = (EGL10)EGLContext.getEGL();
            if (SDLAndroid.mEGLContext == null) createEGLContext();

            Log.v("SDLAndroid", "Creating new EGL Surface");
            EGLSurface surface = egl.eglCreateWindowSurface(SDLAndroid.mEGLDisplay, SDLAndroid.mEGLConfig, SDLAndroid.mSurface, null);
            if (surface == EGL10.EGL_NO_SURFACE) {
                Log.e("SDLAndroid", "Couldn't create surface");
                return false;
            }

            if (!egl.eglMakeCurrent(SDLAndroid.mEGLDisplay, surface, surface, SDLAndroid.mEGLContext)) {
                Log.e("SDLAndroid", "Old EGL Context doesnt work, trying with a new one");
                createEGLContext();
                if (!egl.eglMakeCurrent(SDLAndroid.mEGLDisplay, surface, surface, SDLAndroid.mEGLContext)) {
                    Log.e("SDLAndroid", "Failed making EGL Context current");
                    return false;
                }
            }
            SDLAndroid.mEGLSurface = surface;
            return true;
        }
        return false;
    }

    // EGL buffer flip
    public static void flipEGL() {
        try {
            EGL10 egl = (EGL10)EGLContext.getEGL();

            egl.eglWaitNative(EGL10.EGL_CORE_NATIVE_ENGINE, null);

            // drawing here

            egl.eglWaitGL();

            egl.eglSwapBuffers(SDLAndroid.mEGLDisplay, SDLAndroid.mEGLSurface);


        } catch(Exception e) {
            Log.v("SDLAndroid", "flipEGL(): " + e);
            for (StackTraceElement s : e.getStackTrace()) {
                Log.v("SDLAndroid", s.toString());
            }
        }
    }

    // Audio
    private static Object buf;
    
    public static Object audioInit(int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames) {
        int channelConfig = isStereo ? AudioFormat.CHANNEL_CONFIGURATION_STEREO : AudioFormat.CHANNEL_CONFIGURATION_MONO;
        int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        int frameSize = (isStereo ? 2 : 1) * (is16Bit ? 2 : 1);
        
        Log.v("SDLAndroid", "SDLAndroid audio: wanted " + (isStereo ? "stereo" : "mono") + " " + (is16Bit ? "16-bit" : "8-bit") + " " + ((float)sampleRate / 1000f) + "kHz, " + desiredFrames + " frames buffer");
        
        // Let the user pick a larger buffer if they really want -- but ye
        // gods they probably shouldn't, the minimums are horrifyingly high
        // latency already
        desiredFrames = Math.max(desiredFrames, (AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) + frameSize - 1) / frameSize);
        
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                channelConfig, audioFormat, desiredFrames * frameSize, AudioTrack.MODE_STREAM);
        
        audioStartThread();
        
        Log.v("SDLAndroid", "SDLAndroid audio: got " + ((mAudioTrack.getChannelCount() >= 2) ? "stereo" : "mono") + " " + ((mAudioTrack.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) ? "16-bit" : "8-bit") + " " + ((float)mAudioTrack.getSampleRate() / 1000f) + "kHz, " + desiredFrames + " frames buffer");
        
        if (is16Bit) {
            buf = new short[desiredFrames * (isStereo ? 2 : 1)];
        } else {
            buf = new byte[desiredFrames * (isStereo ? 2 : 1)]; 
        }
        return buf;
    }
    
    public static void audioStartThread() {
    	Log.v("SDLAndroid", "audioStartThread");
        mAudioThread = new Thread(new Runnable() {
            public void run() {
                mAudioTrack.play();
                nativeRunAudioThread();
            }
        });
        
        // I'd take REALTIME if I could get it!
        mAudioThread.setPriority(Thread.MAX_PRIORITY);
        mAudioThread.start();
    }
    
    public static void audioWriteShortBuffer(short[] buffer) {
    	//Log.v("SDLAndroid", "audioWriteShortBuffer");
        for (int i = 0; i < buffer.length; ) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch(InterruptedException e) {
                    // Nom nom
                }
            } else {
                Log.w("SDLAndroid", "SDLAndroid audio: error return from write(short)");
                return;
            }
        }
    }
    
    public static void audioWriteByteBuffer(byte[] buffer) {
    	Log.v("SDLAndroid", "audioWriteByteBuffer");
        for (int i = 0; i < buffer.length; ) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch(InterruptedException e) {
                    // Nom nom
                }
            } else {
                Log.w("SDLAndroid", "SDLAndroid audio: error return from write(short)");
                return;
            }
        }
    }

    public static void audioQuit() {
        if (mAudioThread != null) {
            try {
                mAudioThread.join();
            } catch(Exception e) {
                Log.v("SDLAndroid", "Problem stopping audio thread: " + e);
            }
            mAudioThread = null;

            //Log.v("SDLAndroid", "Finished waiting for audio thread");
        }

        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }
    }
}

/**
    Simple nativeMain() runnable
*/

class SDLAndroidMain implements Runnable {
	final String[] argv;
	SDLAndroidMain(final String[] argv) {
		this.argv = argv;
	}
    public void run() {
        // Runs SDLAndroid_main()
        SDLAndroid.nativeMain(argv);

        //Log.v("SDLAndroid", "SDLAndroid thread terminated");
    }
}


/**
    SDLAndroidSurface. This is what we draw on, so we need to know when it's created
    in order to do anything useful. 

    Because of this, that's where we set up the SDLAndroid thread
*/
class SDLSurfaceCallback implements SurfaceHolder.Callback, 
    View.OnKeyListener, View.OnTouchListener, SensorEventListener  {

    // Sensors
    private static SensorManager mSensorManager;

    // Startup    
    public SDLSurfaceCallback(Context context, SurfaceView surface) {
        surface.getHolder().addCallback(this); 
    
        surface.setFocusable(true);
        surface.setFocusableInTouchMode(true);
        surface.requestFocus();
        surface.setOnKeyListener(this); 
        surface.setOnTouchListener(this);   

        mSensorManager = (SensorManager)context.getSystemService("sensor");  
    }

    // Called when we have a valid drawing surface
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v("SDLAndroid", "surfaceCreated()");
        holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
        SDLAndroid.createEGLSurface();
        enableSensor(Sensor.TYPE_ACCELEROMETER, true);
    }

    // Called when we lose the surface
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v("SDLAndroid", "surfaceDestroyed()");
        SDLAndroid.nativePause();
        enableSensor(Sensor.TYPE_ACCELEROMETER, false);
    }

    // Called when the surface is resized
    public void surfaceChanged(SurfaceHolder holder,
                               int format, int width, int height) {
        Log.v("SDLAndroid", "surfaceChanged()");

        int SDLAndroidFormat = 0x85151002; // SDLAndroid_PIXELFORMAT_RGB565 by default
        switch (format) {
        case PixelFormat.A_8:
            Log.v("SDLAndroid", "pixel format A_8");
            break;
        case PixelFormat.LA_88:
            Log.v("SDLAndroid", "pixel format LA_88");
            break;
        case PixelFormat.L_8:
            Log.v("SDLAndroid", "pixel format L_8");
            break;
        case PixelFormat.RGBA_4444:
            Log.v("SDLAndroid", "pixel format RGBA_4444");
            SDLAndroidFormat = 0x85421002; // SDLAndroid_PIXELFORMAT_RGBA4444
            break;
        case PixelFormat.RGBA_5551:
            Log.v("SDLAndroid", "pixel format RGBA_5551");
            SDLAndroidFormat = 0x85441002; // SDLAndroid_PIXELFORMAT_RGBA5551
            break;
        case PixelFormat.RGBA_8888:
            Log.v("SDLAndroid", "pixel format RGBA_8888");
            SDLAndroidFormat = 0x86462004; // SDLAndroid_PIXELFORMAT_RGBA8888
            break;
        case PixelFormat.RGBX_8888:
            Log.v("SDLAndroid", "pixel format RGBX_8888");
            SDLAndroidFormat = 0x86262004; // SDLAndroid_PIXELFORMAT_RGBX8888
            break;
        case PixelFormat.RGB_332:
            Log.v("SDLAndroid", "pixel format RGB_332");
            SDLAndroidFormat = 0x84110801; // SDLAndroid_PIXELFORMAT_RGB332
            break;
        case PixelFormat.RGB_565:
            Log.v("SDLAndroid", "pixel format RGB_565");
            SDLAndroidFormat = 0x85151002; // SDLAndroid_PIXELFORMAT_RGB565
            break;
        case PixelFormat.RGB_888:
            Log.v("SDLAndroid", "pixel format RGB_888");
            // Not sure this is right, maybe SDLAndroid_PIXELFORMAT_RGB24 instead?
            SDLAndroidFormat = 0x86161804; // SDLAndroid_PIXELFORMAT_RGB888
            break;
        default:
            Log.v("SDLAndroid", "pixel format unknown " + format);
            break;
        }
        SDLAndroid.onNativeResize(width, height, SDLAndroidFormat);
        Log.v("SDLAndroid", "Window size:" + width + "x"+height);

        SDLAndroid.startApp();
    }

    // unused
    public void onDraw(Canvas canvas) {}




    // Key events
    public boolean onKey(View  v, int keyCode, KeyEvent event) {

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            //Log.v("SDLAndroid", "key down: " + keyCode);
            SDLAndroid.onNativeKeyDown(keyCode);
            return true;
        }
        else if (event.getAction() == KeyEvent.ACTION_UP) {
            //Log.v("SDLAndroid", "key up: " + keyCode);
            SDLAndroid.onNativeKeyUp(keyCode);
            return true;
        }
        
        return false;
    }

    // Touch events
    public boolean onTouch(View v, MotionEvent event) {
        {
             final int touchDevId = event.getDeviceId();
             final int pointerCount = event.getPointerCount();
             // touchId, pointerId, action, x, y, pressure
             int actionPointerIndex = event.getActionIndex();
             int pointerFingerId = event.getPointerId(actionPointerIndex);
             int action = event.getActionMasked();

             float x = event.getX(actionPointerIndex);
             float y = event.getY(actionPointerIndex);
             float p = event.getPressure(actionPointerIndex);

             Log.v("SDLAndroid", "onTouch x: " + x);
             
             if (action == MotionEvent.ACTION_MOVE && pointerCount > 1) {
                // TODO send motion to every pointer if its position has
                // changed since prev event.
                for (int i = 0; i < pointerCount; i++) {
                    pointerFingerId = event.getPointerId(i);
                    x = event.getX(i);
                    y = event.getY(i);
                    p = event.getPressure(i);
                    SDLAndroid.onNativeTouch(touchDevId, pointerFingerId, action, x, y, p);
                }
             } else {
                SDLAndroid.onNativeTouch(touchDevId, pointerFingerId, action, x, y, p);
             }
        }
      return true;
   } 

    // Sensor events
    public void enableSensor(int sensortype, boolean enabled) {
        // TODO: This uses getDefaultSensor - what if we have >1 accels?
        if (enabled) {
            mSensorManager.registerListener(this, 
                            mSensorManager.getDefaultSensor(sensortype), 
                            SensorManager.SENSOR_DELAY_GAME, null);
        } else {
            mSensorManager.unregisterListener(this, 
                            mSensorManager.getDefaultSensor(sensortype));
        }
    }
    
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            SDLAndroid.onNativeAccel(event.values[0] / SensorManager.GRAVITY_EARTH,
                                      event.values[1] / SensorManager.GRAVITY_EARTH,
                                      event.values[2] / SensorManager.GRAVITY_EARTH);
        }
    }

}

