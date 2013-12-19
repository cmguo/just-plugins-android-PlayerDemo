package com.pplive.demo;

import java.util.Map;
import java.util.TreeMap;

import android.app.Activity;
import android.util.Log;

import com.pplive.sdk.pplink.ClientInfo;
import com.pplive.sdk.pplink.Service;
import com.pplive.sdk.pplink.ServiceDescription;
import com.pplive.sdk.pplink.ServiceInfo;
import com.pplive.sdk.pplink.Session;

class MySpeakerService extends Service
{
	static private Map<Long, MySpeakerSession> sessions = new TreeMap<Long, MySpeakerSession>();
	
	private Activity owner;
	
	static MySpeakerSession get(Long key)
	{
		return sessions.get(key);
	}
	
	public MySpeakerService(Activity owner)
	{
		this.owner = owner;
	}

	public void get_description(ServiceDescription description) {
		Log.d("MySpeakerService", "get_description");
		description.setName(android.os.Build.MANUFACTURER);
		description.setType("speaker");
		description.setUid("A0B1:C2:D3:E4:F5");
	}

	public void get_info(ServiceInfo info) {
		Log.d("MySpeakerService", "get_info");
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
		Log.d("MySpeakerService", "connect");
		final MySpeakerSession session = new MySpeakerSession(owner);
		sessions.put(session.getId(), session);
		return session;
	}

}
