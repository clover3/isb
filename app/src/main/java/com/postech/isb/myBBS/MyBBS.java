package com.postech.isb.myBBS;

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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.postech.isb.PostechIsb;
import com.postech.isb.R;
import com.postech.isb.R.id;
import com.postech.isb.R.layout;
import com.postech.isb.boardList.BoardList;
import com.postech.isb.info.Info;
import com.postech.isb.preference.PreferenceList;
import com.postech.isb.readBoard.ReadBoards;
import com.postech.isb.util.IsbSession;
import com.postech.isb.viewUser.ViewUser;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class MyBBS extends Activity {
	private Button diaryButton;
	private Button mailButton;
	private TextView logedId;
	private TextView helloment;
	private ImageView mailIcon;

	private Intent diary;
	private Intent mail;
	// Handler for thread. Show error messages.

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mybbs);

		diaryButton = (Button) findViewById(id.diary);
		mailButton = (Button) findViewById(id.mail);

		logedId = (TextView) findViewById(id.currentId);
		helloment = (TextView) findViewById(id.helloment);
		mailIcon = (ImageView) findViewById(id.mailIcon);

		diaryButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				diary.putExtra("board", "diary");
				startActivity(diary);
			}
		});
		mailButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mail.putExtra("board", "mail");
				startActivity(mail);
			}
		});

		diary = new Intent(this, ReadBoards.class);
		mail = new Intent(this, ReadBoards.class);
		restoreUIState();
	}

	private void restoreUIState() {
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		logedId.setText(IsbSession.userId);
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
	@Override
	protected void onResume () {
		super.onResume();
		setNewMail();
	}
}