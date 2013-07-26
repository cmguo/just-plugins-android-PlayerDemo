package com.pplive.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class AndroidPlayer extends WebViewClient {


	private Activity owner;
	
	public AndroidPlayer(Activity owner)
	{
		this.owner = owner;
	}
	
	@Override
	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
	{
		play("pplive2:///e9301e073cf94732a380b765c8b9573d-5-400");
	}
	
	@JavascriptInterface
	public void play(final String url)
	{
		owner.runOnUiThread(new Runnable() {
			@Override
			public void run()
			{
		        try
		        {
		            Intent intent = new Intent(
		            		owner.getBaseContext(), PlayerActivity.class);
		            Bundle bl = new Bundle();
					bl.putString("url", url);
					intent.putExtras(bl);
		            owner.startActivityForResult(intent, 0);
		        }
		        catch (Exception e)
		        {
		            Toast.makeText(owner, "Open Failed", Toast.LENGTH_SHORT).show();
		        }
			}
		});
	}
}
