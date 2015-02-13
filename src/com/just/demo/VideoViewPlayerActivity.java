package com.just.demo;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class VideoViewPlayerActivity extends Activity implements OnErrorListener, OnCompletionListener {

	private VideoView mVideoView;
	private MediaController mMediaCtrl;
	
	private boolean bPlaying = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_view_player);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mVideoView = (VideoView)findViewById(R.id.videoView);
        mVideoView.setOnErrorListener(this);
        mVideoView.setOnCompletionListener(this);
        
        mMediaCtrl = new MediaController(this);
        mMediaCtrl.setAnchorView(mVideoView);
        
        mVideoView.setMediaController(mMediaCtrl);
        
        Intent intent = getIntent();
		Bundle bl = intent.getExtras();
		String url = bl.getString("url");

		final Uri uri = Uri.parse(url);
		mVideoView.setVideoURI(uri);
		mVideoView.start();
		bPlaying = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.video_view_player, menu);
		return true;
	}

    public void onPause()
    {
        super.onPause();
        mVideoView.pause();
        bPlaying = false;
    }

    public void onResume()
    {
    	super.onResume();
        mVideoView.resume();
        bPlaying = true;
    }

    public void onDestroy()
    {
        super.onDestroy();
    	mVideoView.stopPlayback();
    }
    	
	@Override
	public void onCompletion(MediaPlayer arg0) {
		mVideoView.seekTo(0);
        mVideoView.stopPlayback();
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		if(what == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
            
        } else if(what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            
        }
        Toast.makeText(this, "±®¥Ì¡À", Toast.LENGTH_LONG).show();
        return true;
	}
}
