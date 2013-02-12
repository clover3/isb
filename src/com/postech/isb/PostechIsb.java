package com.postech.isb;

import com.postech.isb.util.IsbSession;
import android.app.Application;
import android.util.Log;

public class PostechIsb extends Application {

	public IsbSession isb;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.i("PostechIsb", "Create new isb session.");
		isb = new IsbSession();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}
}
