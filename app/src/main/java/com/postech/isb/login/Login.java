package com.postech.isb.login;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.LinearLayout;
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
import com.postech.isb.util.IsbSession;
import com.postech.isb.util.TouchMenuManager;
import com.postech.isb.viewUser.ViewUser;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

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
	private static final char[] SEKRIT = "damnthdusl1219".toCharArray() ;
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
			}; break;
			}
		}
	};

	private static final String SETTING_VER = "SETTING_VER";
	private static final String SAVE_ID_PW_KEY = "SAVE_ID_PW_KEY";
	private static final String SAVED_ID = "SAVED_ID";
	private static final String SAVED_PW = "SAVED_PW";
	
	@Override
	protected void onResume () {
		super.onResume();
		if (!isb.isConnected()) {
			idPwInput.setVisibility(View.VISIBLE);
			logedIn.setVisibility(View.INVISIBLE);
						
			if (saveIdPw.isChecked())
				login();
		}
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
		restoreUIState();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);

      menu.add(0, INFO, Menu.NONE,R.string.info)
      				.setIcon(android.R.drawable.ic_menu_info_details);
      menu.add(1, USER, Menu.NONE, R.string.user)
					.setIcon(android.R.drawable.ic_menu_myplaces);
//      menu.add(2, PREFERENCE, Menu.NONE,R.string.preference)
//      				.setIcon(android.R.drawable.ic_menu_preferences);
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
		
		(loginThread = new LoginThread(signedInId, pw)).start(); // ������ ����  
	}
	
	private void logout() {
		if (isb.isConnected()) {
			isb.disconnect();
			idPwInput.setVisibility(View.VISIBLE);
			logedIn.setVisibility(View.INVISIBLE);
											
			Log.i(logName, "Connection Failed.");
		}
	}

	protected String encrypt( String value ) {
		try {
			final byte[] bytes = value!=null ? value.getBytes(UTF8) : new byte[0];
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
			SecretKey key = keyFactory.generateSecret(new PBEKeySpec(SEKRIT));
			Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
			pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(Settings.Secure.getString(getApplicationContext().getContentResolver(),Settings.System.ANDROID_ID).getBytes(UTF8), 20));
			return new String(Base64.encode(pbeCipher.doFinal(bytes), Base64.NO_WRAP),UTF8);

		} catch( Exception e ) {
			throw new RuntimeException(e);
		}

	}

	protected String decrypt(String value){
		try {
			final byte[] bytes = value!=null ? Base64.decode(value,Base64.DEFAULT) : new byte[0];
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
			SecretKey key = keyFactory.generateSecret(new PBEKeySpec(SEKRIT));
			Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
			pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(Settings.Secure.getString(getApplicationContext().getContentResolver(),Settings.System.ANDROID_ID).getBytes(UTF8), 20));
			return new String(pbeCipher.doFinal(bytes),UTF8);

		} catch( Exception e) {
			throw new RuntimeException(e);
		}
	}
}