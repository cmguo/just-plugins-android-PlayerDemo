package com.just.demo;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.VideoView;

public class VideoViewPlayerActivity extends Activity {

	private VideoView player;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_view_player);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        player = (VideoView)findViewById(R.id.videoView);

        Intent intent = getIntent();
		Bundle bl = intent.getExtras();
		String url = bl.getString("url");

		final Uri uri = Uri.parse(url);
		player.setVideoURI(uri);
		player.start();
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
        player.pause();
    }

    public void onResume()
    {
    	super.onResume();
        player.resume();
    }

    public void onDestroy()
    {
        super.onDestroy();
    	player.stopPlayback();
    }
    
}
