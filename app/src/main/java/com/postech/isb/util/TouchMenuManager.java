package com.postech.isb.util;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

import com.postech.isb.R;


public class TouchMenuManager
{
	private Activity activity;
	private int m_nPreTouchPosY = 0;
	private int m_nPreTouchPosX = 0;
	private boolean on_drag = false;
	
	public TouchMenuManager(Activity act)
	{
		activity = act;
	}
	
	public View.OnTouchListener MyTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			//Log.i("newm", "v.getClass(): "+v.getClass());
			if ((event.getAction() == MotionEvent.ACTION_MOVE && !on_drag) ||
					event.getAction() == MotionEvent.ACTION_DOWN) {
				on_drag = true;
				m_nPreTouchPosX = (int) event.getX();
				m_nPreTouchPosY = (int) event.getY();
			}
			else if( event.getAction() == MotionEvent.ACTION_MOVE && on_drag)
			{
				int nTouchPosX = (int) event.getX();
				int nTouchPosY = (int) event.getY();
				int result = GetTouchResult(nTouchPosX, nTouchPosY);
			}
			else if (event.getAction() == MotionEvent.ACTION_UP && on_drag) {
				int nTouchPosX = (int) event.getX();
				int nTouchPosY = (int) event.getY();
				on_drag = false;
				
				int result = GetTouchResult(nTouchPosX, nTouchPosY);

				if( result == 1 )
				{
					SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());
					String swipe = activity.getResources().getString(
							R.string.preference_value_menuoption_swipe);
					String menuOption = SP.getString("menu_option", swipe);
					if (menuOption.equals(swipe)) {
						activity.openOptionsMenu();
					}
				}
					
				m_nPreTouchPosX = nTouchPosX;
				m_nPreTouchPosY = nTouchPosY;
			}
			else{
				Log.i("newm", "event.getAction(): "+event.getAction());
			}
			return false;
		}
	};
	
	
	int GetTouchResult(int nTouchPosX, int nTouchPosY)
	{
		int result = 0;
		
		Display display = activity.getWindowManager().getDefaultDisplay();
		int height = display.getHeight();
		
		if( Math.abs(nTouchPosX - m_nPreTouchPosX) < 4 
		 && Math.abs(nTouchPosY - m_nPreTouchPosY) < 4  )
		{
			// Do Nothing 
		}
		else
		{
			double minGap = height * 0.03;
			double maxGap = height * 0.30;
			double maxDist = height * 0.20;
 
			int dy = m_nPreTouchPosY - nTouchPosY ;
			int dx = m_nPreTouchPosX - nTouchPosX ;
			int initPosFromBtm = height - m_nPreTouchPosY;
			if( maxGap > dy && dy > minGap && initPosFromBtm < maxDist)
			{
				if( Math.abs(dy) > Math.abs(dx) * 0.5)
				{
					result = 1;
				}
			}
			
		}
		return result;
	}
}