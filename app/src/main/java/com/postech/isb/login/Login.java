package com.postech.isb.login;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.postech.isb.PostechIsb;
import com.postech.isb.R;
import com.postech.isb.R.id;
import com.postech.isb.R.layout;
import com.postech.isb.boardList.Board;
import com.postech.isb.boardList.BoardList;
import com.postech.isb.info.Info;
import com.postech.isb.preference.PreferenceList;
import com.postech.isb.util.HeartbeaterReceiver;
import com.postech.isb.util.IsbSession;
import com.postech.isb.util.TouchMenuManager;
import com.postech.isb.viewUser.ViewUser;

public class Login extends Activity {

	static final private int INFO = Menu.FIRST;
	static final private int USER = Menu.FIRST+1;
	static final private int PREFERENCE = Menu.FIRST+2;

	private IsbSession isb;
	private EditText loginId;
	private EditText loginPw;
	private Button loginButton;
	private Button logoutButton;
	private Button goSurfButton;
	private RelativeLayout idPwInput;
	private RelativeLayout logedIn;
	private TextView logedId;
	private TextView helloment;
	private ImageView mailIcon;
	private CheckBox saveIdPw;

	static private String logName = "Login";

	private String signedInId;

	private ProgressDialog pd;

	private Intent goSurf;
	private Intent goSetting;
	private Intent goInfo;
	private Intent goUserList;

	private LoginThread loginThread;

	protected static final String UTF8 = "utf-8";
	private static final byte[] SEKRIT = "damnthdusl1219".getBytes() ;
	private Handler heartbeatMessageHandler = new HearbeatMessageHandler();

	// Handler for thread. Show error messages.
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0: pd.dismiss(); break;
			case 1: Toast.makeText(Login.this, "Connection Failed.", Toast.LENGTH_SHORT).show(); break;
			case 2: Toast.makeText(Login.this, "Connection Lost.", Toast.LENGTH_SHORT).show(); break;
			case 3: Toast.makeText(Login.this, "Login Failed!", Toast.LENGTH_SHORT).show(); break;
			case 4: {
				idPwInput.setVisibility(View.INVISIBLE);
				logedIn.setVisibility(View.VISIBLE);
				logedId.setText(signedInId);
				setNewMail();
				showNotice();
			}; break;
			}
		}
	};

	private static final String SETTING_VER = "SETTING_VER";
	private static final String SAVE_ID_PW_KEY = "SAVE_ID_PW_KEY";
	private static final String SAVED_ID = "SAVED_ID";
	private static final String SAVED_PW = "SAVED_PW";
	private static final String LAST_NOTICE_HASH = "LAST_NOTICE_HASH";

	@Override
	protected void onResume () {
		super.onResume();
		if (!isb.isConnected()) {
			idPwInput.setVisibility(View.VISIBLE);
			logedIn.setVisibility(View.INVISIBLE);

			if (saveIdPw.isChecked())
				login();
		}
		setNewMail();
	}

	@Override
	protected void onPause() {
		super.onPause();
		/* Save ID/PW */
		SharedPreferences uiState = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = uiState.edit();

		boolean saveMe = saveIdPw.isChecked();
		editor.putBoolean(SAVE_ID_PW_KEY, saveMe);
		if (saveMe) {
			editor.putInt(SETTING_VER, 1);
			editor.putString(SAVED_ID, encrypt(loginId.getText().toString()));
			editor.putString(SAVED_PW, encrypt(loginPw.getText().toString()));
		}
		editor.commit();
	}

	private void backwardCompatibility(SharedPreferences SP){
		// Convert swipe menu option into menu option
		// If menu option is not set, set it following swipe_menu
		String newValue;
		String notSelected = getResources().getString(
				R.string.preference_value_menuoption_not_selected);
		String menuOption = SP.getString("menu_option", notSelected);
		if (!menuOption.equals(notSelected)) {
			// If menu option is already set
			return;
		}
		boolean swipeMenuOption = SP.getBoolean("swipe_menu", true);
		if (swipeMenuOption)
			newValue = getResources().getString(R.string.preference_value_menuoption_swipe);
		else
			newValue = getResources().getString(R.string.preference_value_menuoption_menubutton);

		SharedPreferences.Editor editor = SP.edit();

		editor.putString("menu_option", newValue);
		editor.commit();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);

		isb = ((PostechIsb)getApplicationContext()).isb;
		loginId = (EditText)findViewById(R.id.loginId);
		loginPw = (EditText)findViewById(R.id.loginPw);
		saveIdPw = (CheckBox)findViewById(R.id.rememberIdPw);

		loginButton = (Button) findViewById(R.id.loginBtn);
		logoutButton = (Button) findViewById(R.id.logoutBtn);
		goSurfButton = (Button) findViewById(R.id.goSurf);

		idPwInput = (RelativeLayout) findViewById(R.id.idPwInput);
		logedIn = (RelativeLayout) findViewById(R.id.logedIn);
		logedId = (TextView) findViewById(R.id.currentId);
		helloment = (TextView) findViewById(id.helloment);
		mailIcon = (ImageView) findViewById(id.mailIcon);

		loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				login();
			}
		});

		logoutButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				logout();
			}
		});

		goSurfButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivity(goSurf);
			}
		});

		goSurf = new Intent(this, BoardList.class);
		goSetting = new Intent(this, PreferenceList.class);
		goInfo = new Intent(this, Info.class);
		goUserList = new Intent(this, ViewUser.class);

		SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		backwardCompatibility(SP);

		// Start to run heartbeater
		Boolean runHeartbeat = SP.getBoolean("heartbeat", true);
		Log.i(logName, "heartbeat: " + runHeartbeat);
		if (runHeartbeat) {
			int alarmId = 0;
			Intent alarm = new Intent(this, HeartbeaterReceiver.class);
			alarm.putExtra("MESSENGER", new Messenger(heartbeatMessageHandler));
			alarm.putExtra("alarmId", alarmId); /* So we can catch the id on BroadcastReceiver */

			PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarmId, alarm, PendingIntent.FLAG_CANCEL_CURRENT);
			AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

			// isb server timeout limit: 15 minutes
			Log.i(logName, "heartbeat_period: " + getResources().getInteger(R.integer.heartbeat_period));
			// XXX: use setExact for API 19 or later.
			alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + getResources().getInteger(R.integer.heartbeat_period), pendingIntent);
		}
		restoreUIState();
	}

	public class HearbeatMessageHandler extends Handler {
		@Override
		public void handleMessage(Message message) {
			Log.i(logName, "sending heartbeat...");
			isb.sendHeartbeat();
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);

      menu.add(0, INFO, Menu.NONE,R.string.info)
      				.setIcon(android.R.drawable.ic_menu_info_details);
      menu.add(1, USER, Menu.NONE, R.string.user)
					.setIcon(android.R.drawable.ic_menu_myplaces);
      return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
      super.onPrepareOptionsMenu(menu);

      return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      super.onOptionsItemSelected(item);

      switch (item.getItemId()) {
      case INFO: {
    	  startActivity(goInfo);
    	  return true;
      }
      case USER: {
    	  startActivity(goUserList);
    	  return true;
      }
      case PREFERENCE: {
    	  //startActivity(goSetting);
    	  return true;
      }
      }

      return false;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if(keyCode == KeyEvent.KEYCODE_BACK) {
    		if( null != loginThread )
    		{
    			loginThread.interrupt();
    		}
    	}
    	return false;
    }

	private void restoreUIState() {
		SharedPreferences settings = getPreferences(MODE_PRIVATE);

		boolean isSaveIdPw = settings.getBoolean(SAVE_ID_PW_KEY, false);
		String savedId = settings.getString(SAVED_ID, "");
		String savedPw = settings.getString(SAVED_PW, "");
		int nSettingVer = settings.getInt(SETTING_VER, 0);
		if( nSettingVer > 0 ) {
			savedId = decrypt(savedId);
			savedPw = decrypt(savedPw);
		}

		if (isSaveIdPw) {
			saveIdPw.setChecked(isSaveIdPw);
			loginId.setText(savedId);
			loginPw.setText(savedPw);
		}
	}

	private class LoginThread extends Thread implements Runnable {
		private String id, pw;

		LoginThread(String _id, String _pw) {
			id = new String(_id);
			pw = new String(_pw);
		}

		@Override
		public void run() {
			isb.disconnect();

			if (!isb.isConnected()) {
				try {
					isb.connect();
				} catch (Exception e) {
					isb.disconnect();
					handler.sendEmptyMessage(1);
					Log.i(logName, "Connection Failed.");
				}
			}

			if (isb.isConnected()) {
				try {
					if (isb.login(id, pw, true)) {
						handler.sendEmptyMessage(4);
						Log.i(logName, "Login Success.");
					} else {
						handler.sendEmptyMessage(3);
						Log.i(logName, "Login Fail.");
					}
				} catch (Exception e) {
					isb.disconnect();
					handler.sendEmptyMessage(2);
					Log.i(logName, "Connection Lost.");
				}
			}

			handler.sendEmptyMessage(0);
		}
	}

	private synchronized void login() {
		signedInId = loginId.getText().toString();
		if (signedInId.length() < 1) {
			Toast.makeText(Login.this, "ID is emptry. Check it plz.", Toast.LENGTH_SHORT).show();
			return;
		}

		String pw = loginPw.getText().toString();
		if (pw.length() < 1) {
			Toast.makeText(Login.this, "PW is emptry. Check it plz.", Toast.LENGTH_SHORT).show();
			return;
		}

		pd = ProgressDialog.show(Login.this, "Isb", "Logging in...", true, true);

		(loginThread = new LoginThread(signedInId, pw)).start();
	}

	private void logout() {
		if (isb.isConnected()) {
			isb.disconnect();
			idPwInput.setVisibility(View.VISIBLE);
			logedIn.setVisibility(View.INVISIBLE);

			Log.i(logName, "Connection Failed.");
		}
	}
	private byte[] xorWithKey(byte[] a, byte[] key) {
		byte[] out = new byte[a.length];
		for (int i = 0; i < a.length; i++) {
			out[i] = (byte) (a[i] ^ key[i%key.length]);
		}
		return out;
	}

	private final String ENC_TYPE_XOR_BASE64 = "!xorb64!";

	protected String encrypt( String value ) {
		try {
			return ENC_TYPE_XOR_BASE64 + new String(Base64.encode(xorWithKey(value.getBytes(), SEKRIT), Base64.NO_WRAP),UTF8);
		} catch( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	protected String decrypt(String value){
		if (value.length() < 9)
			return "";
		String enc_type = value.substring(0,8);
		value = value.substring(8);
		try {
			if (enc_type.equals(ENC_TYPE_XOR_BASE64))
				return new String(xorWithKey(Base64.decode(value.getBytes(), Base64.DEFAULT), SEKRIT),UTF8);
			else
				return "";
		} catch( Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void setNewMail() {
		if (IsbSession.new_mail) {
			helloment.setText("New mail!");
			mailIcon.setVisibility(View.VISIBLE);
		}
		else {
			helloment.setText("Hello,");
			mailIcon.setVisibility(View.GONE);
		}
	}

	private static String toHexString(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();

		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}

		return hexString.toString();
	}

	private void showNotice() {
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		String savedNoticeHash = settings.getString(LAST_NOTICE_HASH, "");
		String notice_content = getResources().getString(R.string.notice_content);
		try {
			// If the notice have been shown, do not show it anymore.
			MessageDigest md = MessageDigest.getInstance("MD5");
			String thedigest = toHexString(md.digest(notice_content.getBytes("UTF-8")));
			if (savedNoticeHash.equals(thedigest))
				return;
			else {
				SharedPreferences uiState = getPreferences(MODE_PRIVATE);
				SharedPreferences.Editor editor = uiState.edit();

				editor.putString(LAST_NOTICE_HASH, thedigest);
				editor.commit();
			}
		}
		catch (Exception e){
			// Any exception
			return;
		}

		new AlertDialog.Builder(Login.this)
				.setTitle(R.string.notice_title)
				.setMessage(R.string.notice_content)
				.setNeutralButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(
									DialogInterface dialog,
									int whichButton) {
								// ...ÇÒÀÏ
							}
						}).show();
	}
}