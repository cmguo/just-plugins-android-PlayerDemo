package com.pplive.demo;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.util.Log;
import android.widget.VideoView;
import android.media.MediaPlayer;
import android.net.Uri;

import com.pplive.sdk.pplink.KeyValueMap;
import com.pplive.sdk.pplink.PlayerSession;
import com.pplive.sdk.pplink.SessionStatus;
import com.pplive.sdk.pplink.SessionStatusListener;

class MyPlayerSession extends PlayerSession 
	implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, 
	MediaPlayer.OnInfoListener, MediaPlayer.OnCompletionListener
{
	private Activity owner = null;
	private VideoView video = null;
	private SessionStatus status = null;
	private List<SessionStatusListener> listeners;

	MyPlayerSession() {
		status = new SessionStatus();
		KeyValueMap stats = status.getStats();
        stats.set("state", "stopped");
        stats.set("seekable", "false");
        stats.set("volume", "0.0");
        stats.set("duration", "0");
        stats.set("rate", "0");
        stats.set("position", "0");
        stats.set("cache", "0");
        listeners = new ArrayList<SessionStatusListener>();
	}
	
	void attach(Activity owner, VideoView video) {
		Log.d("MyPlayerSession", "attach");
		this.owner = owner;
		this.video = video;
		video.setOnPreparedListener(this);
		video.setOnErrorListener(this);
		video.setOnCompletionListener(this);
	}
	
	long getId() {
		return getCPtr(this);
	}

	public void get_status(SessionStatus status) {
		//Log.d("MyPlayerSession", "get_status");
		synchronized(status) {
			status.setStats(this.status.getStats());
		}
	}

	public void add_status_listener(SessionStatusListener listener) {
		Log.d("MyPlayerSession", "add_status_listener");
		listeners.add(listener);
	}

	public void release() {
		Log.d("MyPlayerSession", "release");
	}
	
	public void set_uri(String uri) {
		Log.d("MyPlayerSession", "set_uri: " + uri);
		uri = uri.substring(0, uri.indexOf("|"));
		final Uri uri1 = Uri.parse(uri);
		//final Uri uri1 = Uri.parse("http://192.168.14.205/movies/mp4/yu.mp4");
		runOnUiThreadAndWait(new Runnable() {
			@Override
			public void run()
			{
				status.getStats().set("state", "loading");
				video.setVideoURI(uri1);
			}
		});
	}

	public void play() {
		Log.d("MyPlayerSession", "play");
		runOnUiThreadAndWait(new Runnable() {
			@Override
			public void run()
			{
				video.start();
			}
		});
	}

	public void stop() {
		Log.d("MyPlayerSession", "stop");
		runOnUiThreadAndWait(new Runnable() {
			@Override
			public void run()
			{
				video.stopPlayback();
			}
		});
	}

	public void pause() {
		Log.d("MyPlayerSession", "pause");
		runOnUiThreadAndWait(new Runnable() {
			@Override
			public void run()
			{
				video.pause();
			}
		});
	}

	public void resume() {
		Log.d("MyPlayerSession", "resume");
		runOnUiThreadAndWait(new Runnable() {
			@Override
			public void run()
			{
				//video.resume();
				video.start();
			}
		});
	}

	public void seek(int time) {
		Log.d("MyPlayerSession", "seek: " + time);
		final int time1 = time;
		runOnUiThreadAndWait(new Runnable() {
			@Override
			public void run()
			{
				video.seekTo(time1);
			}
		});
	}

	public void set_volume(float volume) {
		Log.d("MyPlayerSession", "set_volume: " + volume);
	}

	public void next() {
		Log.d("MyPlayerSession", "next");
	}

	public void previous() {
		Log.d("MyPlayerSession", "previous");
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		Log.d("MyPlayerSession", "onPrepared");
		video.start();
		set_status_silence("seekable", Boolean.toString(video.canSeekForward()));
        set_status_silence("duration", Integer.toString(video.getDuration()));
		set_status("state", "playing");
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		Log.d("MyPlayerSession", "onCompletion");
		video.stopPlayback();
		set_status("state", "stopped");
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		Log.d("MyPlayerSession", "onInfo");
		return true;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.d("MyPlayerSession", "onError");
		set_status("state", "stopped");
		return false;
	}
	
	private void set_status_silence(String key, String value)
	{
		synchronized(status) {
			status.getStats().set(key, value);
		}
	}
	
	private void set_status(String key, String value)
	{
		synchronized(status) {
			status.getStats().set(key, value);
			notify_listeners();
		}
	}
	
	private void notify_listeners() {
		for (SessionStatusListener listener : listeners) {
			listener.invoke(status);
		}
	}
	
	private void runOnUiThreadAndWait(final Runnable runable)
	{
		synchronized(runable) {
			owner.runOnUiThread(new Runnable() {
				@Override
				public void run()
				{
					runable.run();
					synchronized(runable) {
						runable.notify();
					}
				}
			});
			try {
				runable.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
