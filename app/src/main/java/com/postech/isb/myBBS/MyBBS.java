package com.postech.isb.myBBS;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.postech.isb.R;
import com.postech.isb.R.id;
import com.postech.isb.preference.PreferenceList;
import com.postech.isb.readBoard.ReadBoards;
import com.postech.isb.util.IsbSession;

public class MyBBS extends Activity {
	private Button diaryButton;
	private Button mailButton;
	private Button preferenceButton;
	private TextView logedId;
	private TextView helloment;
	private ImageView mailIcon;

	private Intent diary;
	private Intent mail;
	private Intent preference;
	// Handler for thread. Show error messages.

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mybbs);

		diaryButton = (Button) findViewById(id.diary);
		mailButton = (Button) findViewById(id.mail);
		preferenceButton = (Button) findViewById(id.preference);

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
		preferenceButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivity(preference);
			}
		});

		diary = new Intent(this, ReadBoards.class);
		mail = new Intent(this, ReadBoards.class);
		preference = new Intent(this, PreferenceList.class);

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
