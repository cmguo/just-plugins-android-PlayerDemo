package com.pplive.demo;

import java.util.Map;
import java.util.TreeMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.pplive.sdk.pplink.ClientInfo;
import com.pplive.sdk.pplink.Service;
import com.pplive.sdk.pplink.ServiceDescription;
import com.pplive.sdk.pplink.ServiceInfo;
import com.pplive.sdk.pplink.Session;

class MyPlayerService extends Service
{
	static private Map<Long, MyPlayerSession> sessions = new TreeMap<Long, MyPlayerSession>();
	
	private Activity owner;
	
	static MyPlayerSession get(Long key)
	{
		return sessions.get(key);
	}
	
	public MyPlayerService(Activity owner)
	{
		this.owner = owner;
	}

	public void get_description(ServiceDescription description) {
		Log.d("MyPlayerService", "get_description");
		description.setName(android.os.Build.MANUFACTURER);
		description.setType("player");
		description.setUid("A0:B1:C2:D3:E4:F5");
	}

	public void get_info(ServiceInfo info) {
		Log.d("MyPlayerService", "get_info");
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
		
	public Session connect(ClientInfo client) {
		Log.d("MyPlayerService", "connect");
		final MyPlayerSession session = new MyPlayerSession();
		sessions.put(session.getId(), session);
		runOnUiThreadAndWait(new Runnable() {
			@Override
			public void run()
			{
				try
				{
					Intent intent = new Intent(
							owner.getBaseContext(), VVPlayerActivity.class);
		            Bundle bl = new Bundle();
					bl.putLong("session", session.getId());
					intent.putExtras(bl);
					owner.startActivity(intent);
				}
				catch (Exception e)
				{
					Toast.makeText(owner, "Open Failed", Toast.LENGTH_SHORT).show();
				}
			}
		});
		Log.d("MyPlayerService", "connect----");
		return session;
	}

}
