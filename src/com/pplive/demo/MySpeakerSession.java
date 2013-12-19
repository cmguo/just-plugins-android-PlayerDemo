package com.pplive.demo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.util.Log;
import android.widget.VideoView;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.net.Uri;

import com.pplive.sdk.pplink.KeyValueMap;
import com.pplive.sdk.pplink.SpeakerConfig;
import com.pplive.sdk.pplink.SpeakerSession;
import com.pplive.sdk.pplink.SessionStatus;
import com.pplive.sdk.pplink.SessionStatusListener;

class MySpeakerSession extends SpeakerSession 
{
	private Activity owner = null;
	private AudioTrack audio = null;
	private SessionStatus status = null;
	private List<SessionStatusListener> listeners;

	MySpeakerSession(Activity owner) {
		this.owner = owner;
		status = new SessionStatus();
		KeyValueMap stats = status.getStats();
        stats.set("state", "stopped");
        stats.set("volume", "0.0");
        listeners = new ArrayList<SessionStatusListener>();
	}
	
	long getId() {
		return getCPtr(this);
	}

	public void get_status(SessionStatus status) {
		//Log.d("MySpeakerSession", "get_status");
		synchronized(status) {
			status.setStats(this.status.getStats());
		}
	}

	public void add_status_listener(SessionStatusListener listener) {
		Log.d("MySpeakerSession", "add_status_listener");
		listeners.add(listener);
	}

	public void release() {
		Log.d("MySpeakerSession", "release");
		audio.stop();
		audio = null;
	}
	
	public void config(SpeakerConfig config) {
		Log.d("MySpeakerSession", "config");
		final SpeakerConfig config1 = config;
		runOnUiThreadAndWait(new Runnable() {
			@Override
			public void run()
			{
				status.getStats().set("state", "loading");
				int bufSize = AudioTrack.getMinBufferSize(
						config1.getSample_rate(),
						channelConfig(config1.getChannels()),
						sampleFormat(config1.getBits_per_sample()));
				audio = new AudioTrack(
						AudioManager.STREAM_MUSIC, 
						config1.getSample_rate(),
						channelConfig(config1.getChannels()),
						sampleFormat(config1.getBits_per_sample()),
						bufSize, 
						AudioTrack.MODE_STREAM);
				audio.play();
			}
		});
	}

	public void play(ByteBuffer data) {
		//Log.d("MySpeakerSession", "play");
		data.limit(data.capacity());
		final byte[] buffer = new byte[data.remaining()];
		data.get(buffer);
		/*
		runOnUiThreadAndWait(new Runnable() {
			@Override
			public void run()
			{
				audio.write(buffer, 0, buffer.length);
			}
		});
		*/
		audio.write(buffer, 0, buffer.length);
	}

	public void set_volume(float volume) {
		Log.d("MySpeakerSession", "set_volume: " + volume);
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
	
	private int channelConfig(short channels)
	{
		switch (channels) {
		case 1:
			return AudioFormat.CHANNEL_OUT_MONO;
		case 2:
			return AudioFormat.CHANNEL_OUT_STEREO;
		}
		return AudioFormat.CHANNEL_OUT_MONO;
	}
	
	private int sampleFormat(short bitspersample)
	{
		switch (bitspersample) {
		case 16:
			return AudioFormat.ENCODING_PCM_16BIT;
		case 2:
			return AudioFormat.ENCODING_PCM_8BIT;
		}
		return AudioFormat.ENCODING_PCM_8BIT;
	}
}
