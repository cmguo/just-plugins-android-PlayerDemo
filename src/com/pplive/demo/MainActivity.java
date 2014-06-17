package com.pplive.demo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.pplive.sdk.PPBOX;
import com.pplive.thirdparty.BreakpadUtil;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.webkit.WebView;
import android.widget.Toast;

@SuppressLint("SetJavaScriptEnabled")
public class MainActivity extends Activity {

    private WebView mWebView;

    private WebViewPlayer player;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		player = new WebViewPlayer(this);

		mWebView = (WebView) findViewById(R.id.webview);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.addJavascriptInterface(player, "android_player");
		mWebView.setWebViewClient(player);
		mWebView.loadUrl("http://innrom.pptv.com/test-page/player.htm");
		
		System.out.println("java.library.path: " + System.getProperty("java.library.path"));
		
		File cacheDirFile = getApplicationContext().getCacheDir();
		String cacheDir = cacheDirFile.getAbsolutePath();
		String dataDir = cacheDirFile.getParentFile().getAbsolutePath();		
		String libDir = dataDir + "/lib";
		String tmpDir = "/sdcard/ppsdk";
		File tmpDirFile = new File(tmpDir);
		tmpDirFile.mkdirs();
		
		BreakpadUtil.registerBreakpad(tmpDirFile);

		PPBOX.libPath = libDir;
		//cacheDir.getAbsolutePath();
		PPBOX.logPath = tmpDir;
		PPBOX.logLevel = PPBOX.LEVEL_TRACE;
		PPBOX.load();
		PPBOX.StartEngine("161", "12", "111");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
