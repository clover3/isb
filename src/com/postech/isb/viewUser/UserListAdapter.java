package com.postech.isb.viewUser;

import java.util.ArrayList;

import com.postech.isb.R;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class UserListAdapter extends ArrayAdapter<IsbUser> {
	private Context context;
	private int resource;
	
	public UserListAdapter(Context _context, int _resource, ArrayList<IsbUser> _items) {
		super(_context, _resource, _items);
		resource = _resource;	
		context = _context;
		;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View userView = convertView;
		
		if (convertView == null) {
			LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			userView = vi.inflate(R.layout.user_item, null);
		}
		IsbUser item;
		item = getItem(position);

		if (item != null) {
			String userName = item.GetName();
			String loginUserId = ((ViewUser)context).getisb().userId;
			Log.i("debug", "userid: "+loginUserId);
			
			((TextView)userView.findViewById(R.id.UserName)).setText(userName); 
			((TextView)userView.findViewById(R.id.UserState)).setText("["+item.GetStateString()+"]");
			
			boolean isMobileUser = loginUserId.equals(userName) 
					|| ( item.GetState() == IsbUser.USER_STATE_MAIN );
			if( isMobileUser )
					((ImageView)userView.findViewById(R.id.userIcon)).setImageResource(R.drawable.user_m);
			else
				((ImageView)userView.findViewById(R.id.userIcon)).setImageResource(R.drawable.user);
		}

		return userView;
	}

}
