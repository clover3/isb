package com.postech.isb.info;

import com.postech.isb.R;

import android.R.string;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class Info extends Activity {
	private TextView versionText;
	private TextView versionCheck;
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info);
		
		versionText = (TextView) findViewById(R.id.info_version);
		versionCheck = (TextView) findViewById(R.id.version_check);
		try{
			PackageInfo pInfo = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			String version = pInfo.versionName;
			versionText.setText("ISB for Android "+ version);
			versionCheck.setText(Html.fromHtml(getResources().getString(R.string.info_content), Html.FROM_HTML_MODE_LEGACY));
			versionCheck.setMovementMethod(LinkMovementMethod.getInstance());
			
		} catch (NameNotFoundException px) {

		}
	}

}
