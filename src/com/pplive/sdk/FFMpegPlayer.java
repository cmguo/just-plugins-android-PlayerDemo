package com.pplive.sdk;

import android.content.Context;
import android.view.SurfaceView;

public class FFMpegPlayer
{
    private SDLAndroid mSDL;

    private Thread mThread;
    
    public FFMpegPlayer(Context context, SurfaceView surface)
    {
    	mSDL = new SDLAndroid();
    	mSDL.open(context, surface);
    }
    
    public boolean start(String url)
    {
    	final String[] argv = new String[2];
    	argv[0] = "org.ffmpeg.ffplay.Player";
    	//argv[1] = "-nodisp";
    	argv[1] = url;
    	mSDL.start(argv);
        return true;
    }
    
    public boolean pause()
    {
    	mSDL.pause();
    	return true;
    }

    public boolean resume()
    {
    	mSDL.resume();
    	return true;
    }

    public boolean stop()
    {
    	mSDL.stop();
        return true;
    }

    private native void native_main(String [] argv);
}
