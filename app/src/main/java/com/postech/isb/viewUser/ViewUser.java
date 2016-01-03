package com.postech.isb.viewUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import com.postech.isb.PostechIsb;
import com.postech.isb.R;
import com.postech.isb.util.IsbSession;
import com.postech.isb.util.ThreadList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ViewUser extends ListActivity {
	private ArrayList<IsbUser> userItems;
	private UserListAdapter listAdapter;
	private ListView lv;
	private IsbSession isb;
	private TextView textView;

    /** Called when the activity is first created. */
    @Override 
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_user);

        isb = ((PostechIsb)getApplicationContext()).isb;
         
        Intent intent = getIntent();
        

        userItems = new ArrayList<IsbUser>();
        listAdapter = new UserListAdapter(this, R.layout.view_user, userItems);

        setListAdapter(listAdapter);
        
        lv = (ListView)findViewById(android.R.id.list);
        lv.setAdapter(listAdapter);
        lv.setItemsCanFocus(true);

        registerForContextMenu(lv);
        
    }
    

    @Override
    public void onResume() {
      	super.onResume();
    	try {
        	if (isb.isMain()) {
	        	ArrayList<IsbUser> userList = isb.readUserList();
	        	userItems.clear();
	        	userItems.addAll(userList);
	        	listAdapter.notifyDataSetInvalidated();
        	}
        	else {
        		Toast.makeText(getApplicationContext(), "Login First, plz...", Toast.LENGTH_SHORT).show();
        		//finish();
        	}
		} 
    	catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), "Connection Lost", Toast.LENGTH_SHORT).show();
			isb.disconnect();
		}
    }


	public IsbSession getisb() {
		// TODO Auto-generated method stub
		return isb;
	}
}
