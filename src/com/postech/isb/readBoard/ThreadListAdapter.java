package com.postech.isb.readBoard;

import com.postech.isb.R;
import com.postech.isb.R.id;
import com.postech.isb.util.*;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.view.*;

import java.util.*;

public class ThreadListAdapter extends ArrayAdapter<ThreadList>{
	private int resource;
	
	public ThreadListAdapter(Context _context, int _resource, ArrayList<ThreadList> _items) {
		super(_context, _resource, _items);
		resource = _resource;
	}
	
	class ViewWrapper {
		View base;
		TextView title=null;
		TextView writer=null;
		TextView date=null;
		ImageView newt=null;
		ImageView comment=null;
		ImageView notdel=null;
		RelativeLayout writter_line = null;
		LinearLayout title_line = null;

		ViewWrapper(View base) {
			this.base=base;
		}
		
		TextView getTitle() {
			if (title==null)
				title=(TextView)base.findViewById(R.id.readBoardTitle);
			return title;
		}

		TextView getWriter() {
			if (writer==null)
				writer=(TextView)base.findViewById(R.id.readBoardWriter);
			return writer;
		}

		TextView getDate() {
			if (date==null)
				date=(TextView)base.findViewById(R.id.readBoardDate);
			return date;
		}	    

		ImageView getNewt() {
			if (newt==null)
				newt=(ImageView)base.findViewById(R.id.readBoardNew);
			return newt;
		}

		ImageView getCommnet() {
			if (comment==null)
				comment=(ImageView)base.findViewById(R.id.readBoardComment);
			return comment;
		}
		ImageView getNotdel() {
			if (notdel==null)
				notdel=(ImageView)base.findViewById(R.id.notdel);
			return notdel;
		}
		RelativeLayout getWritterLine() {
			if (writter_line==null)
				writter_line=(RelativeLayout)base.findViewById(R.id.writterLine);
			return writter_line;
		}
		LinearLayout getTitleLine() {
			if (title_line==null)
				title_line=(LinearLayout)base.findViewById(R.id.titleLine);
			return title_line;
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View boardView = convertView;
		ViewWrapper wrapper=null;

		if (convertView == null) {
			String inflater = Context.LAYOUT_INFLATER_SERVICE;
			LayoutInflater vi = (LayoutInflater)getContext().getSystemService(inflater);
			boardView = vi.inflate(resource, null);
			
			wrapper = new ViewWrapper(boardView);
            boardView.setTag(wrapper);
            
		} else {
			wrapper=(ViewWrapper)convertView.getTag();
		}

		ThreadList item;
		try {
			item = getItem(position);
		} catch (IndexOutOfBoundsException e) {
			item = new ThreadList();		
		}
		
		wrapper.getWriter().setText(item.writer);
		wrapper.getDate().setText(item.date);
		wrapper.getTitle().setText(item.title);
		wrapper.getNewt().setVisibility(item.newt ? View.VISIBLE : View.GONE);
		wrapper.getCommnet().setVisibility(item.comment ? View.VISIBLE : View.GONE);
		wrapper.getNotdel().setVisibility(item.notdel ? View.VISIBLE : View.GONE);
		if (item.highlight){
			wrapper.getWriter().setTextColor(0xFFFFFFFF);
			wrapper.getDate().setTextColor(0xFFFFFFFF);
			wrapper.getTitleLine().setBackgroundResource(R.drawable.btn_thread_highlight);
			wrapper.getWritterLine().setBackgroundResource(R.drawable.btn_thread_highlight);
		}
		else{
			wrapper.getWriter().setTextColor(0xFFC8C8C8);
			wrapper.getDate().setTextColor(0xFFC8C8C8);
			wrapper.getTitleLine().setBackgroundDrawable(null);
			wrapper.getWritterLine().setBackgroundDrawable(null);
		}

		return boardView;
	}
}