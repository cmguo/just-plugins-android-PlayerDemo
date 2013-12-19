package com.pplive.demo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.pplive.sdk.pplink.ClientInfo;
import com.pplive.sdk.pplink.Protocol;
import com.pplive.sdk.pplink.Service;
import com.pplive.sdk.pplink.ServiceDescription;
import com.pplive.sdk.pplink.ServiceInfo;
import com.pplive.sdk.pplink.Session;
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

    //private AndroidPlayer player;
    
    private Protocol pplink;
    
    MyPlayerService player;
    
    MySpeakerService speaker;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//player = new AndroidPlayer(this);

		//mWebView = (WebView) findViewById(R.id.webview);
		//mWebView.getSettings().setJavaScriptEnabled(true);
		//mWebView.addJavascriptInterface(player, "android_player");
		//mWebView.setWebViewClient(player);
		//mWebView.loadUrl("http://192.168.14.205/test-page/player.htm");
		
		//File cacheDirFile = getApplicationContext().getCacheDir();
		//String cacheDir = cacheDirFile.getAbsolutePath();
		//String dataDir = cacheDirFile.getParentFile().getAbsolutePath();		
		//String tmpDir = "/mnt/sdcard/ppsdk";
		//File tmpDirFile = new File(tmpDir);
		//tmpDirFile.mkdirs();
		
		//BreakpadUtil.registerBreakpad(tmpDirFile);


		System.loadLibrary("pplink_jni-arm-android-r9-gcc46-mt");
		pplink = Protocol.global_protocol();
		pplink.start();
		player = new MyPlayerService(this);
		pplink.local_device().add_service(player);
		speaker = new MySpeakerService(this);
		pplink.local_device().add_service(speaker);

		/*
		try {
			javax.jmdns.JmDNS jmdns = javax.jmdns.JmDNS.create();
			Map<String, String> properties = new HashMap<String, String>();
			//deviceid=FF:FF:FF:FF:FF:F2 feares=0x2077 model=PPTV,1 srcvers=130.14
			properties.put("deveiceid", "FF:FF:FF:FF:FF:F2");
			properties.put("feares", "0x2077");
			properties.put("model", "PPTV,1");
			properties.put("srcvers", "130.14");
			javax.jmdns.ServiceInfo service = javax.jmdns.ServiceInfo.create(
					"_airplay._tcp.local", "HTC-JMDNS", 8000, 0, 0, true, properties);
			jmdns.registerService(service);
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
