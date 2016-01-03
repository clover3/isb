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
	static final private int USER_NONE = 0;
	static final private int USER_PIETYPE = 1;
	static final private int USER_CLOVER3 = 2;
	static final private int USER_HEADERUSER = 3;
	ArrayList<String> headerUsers;
	public ThreadListAdapter(Context _context, int _resource, ArrayList<ThreadList> _items) {
		super(_context, _resource, _items);
		resource = _resource;
		headerUsers = new ArrayList<String>();
		headerUsers.add("³Ä¿Ë");
		headerUsers.add("akdal");
		headerUsers.add("lightningz");
		headerUsers.add("´½•m");
		headerUsers.add("clover3");
		headerUsers.add("dari6");
		headerUsers.add("zogondragon");
		headerUsers.add("hersammc");
		headerUsers.add("jellyfish");
		headerUsers.add("nyamnyam33");
		headerUsers.add("uneasiness");
		headerUsers.add("dreamseed");
		headerUsers.add("yanggil");
		headerUsers.add("Chun");
		headerUsers.add("jinmel");
		headerUsers.add("´Ï¿À´Ã");
		headerUsers.add("shinichi");
		headerUsers.add("icothos");
		headerUsers.add("il0428");
		headerUsers.add("codinger");
	}
	
	class ViewWrapper {
		View base;
		TextView title=null;
		TextView writer=null;
		TextView date=null;
		TextView header=null;
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

		TextView getHeader() {
			if (header==null)
				header=(TextView)base.findViewById(R.id.readBoardHeader);
			return header;
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
	
	String TranslateHeader_Pietype(String header)
	{
		String NewHeader = "";
		if(header.equals(" "))
			NewHeader = "[Life]";
		else if( header.length() > 0)
		{
			if(header.equals("M"))
				NewHeader = "[Math]";
			else if(header.equals("C"))
				NewHeader = "[CS]";
			else if(header.equals("S"))
				NewHeader = "[Software]";
			else if(header.equals("E"))
				NewHeader = "[English]";
			else if(header.equals("A"))
				NewHeader = "[Academic]";
			else if(header.equals("P"))
				NewHeader = "[People]";
			else if(header.equals("N"))
				NewHeader = "[News]";
			else if(header.equals("B"))
				NewHeader = "[Book]";
			else if(header.equals("V"))
				NewHeader = "[Video]";
			else if(header.equals("F"))
				NewHeader = "[Photo]";
			else if(header.equals("O"))
				NewHeader = "[Others]";
			else if(header.equals("K"))
				NewHeader = "[Musik]";
			else if(header.equals("Y"))
				NewHeader = "[Yeonae]";
		}
		return NewHeader;
	}
	
	boolean ExtractHeader_Clover(ThreadList item)
	{
		boolean f = true;
		if( item.title.startsWith("MS ") || 
				item.title.startsWith("À©µµ¿ì ") )
		{
			item.header = "[MS]";
		}
		else if (item.title.contains("½½ºñ¾Û")) {
			item.header = "[½½ºñ¾Û]";
		}
		else if (item.title.startsWith("[C++]")) {
			item.header = "[C++]";
			item.SetTitle(item.title.substring(6));
		}
		else 
			f = false;
		
		return f;
	}

	void TranslateItem(ThreadList item) {

		int userID = USER_NONE;
		if( item.writer.equals("pietype") )
			userID = USER_PIETYPE;
		else if( item.writer.equals("clover3") )
			userID = USER_CLOVER3;
		else if( headerUsers.contains(item.writer) )
			userID = USER_HEADERUSER;
		
		if( userID == USER_PIETYPE )
		{
			if (item.title.contains(">")) {
				int idx = item.title.indexOf(">");
				item.header = TranslateHeader_Pietype(item.title.substring(0, idx));
				item.SetTitle(item.title.substring(idx + 1));
			} else {

			}
		}
		else if( userID == USER_CLOVER3 )
		{
			ExtractHeader_Clover(item);
		}
		else if( userID == USER_HEADERUSER )
		{
			if( item.title.startsWith("[") && item.title.contains("]") )
			{
				int idx = item.title.indexOf(']')+1;
				item.header = item.title.substring(0, idx);
				item.SetTitle(item.title.substring(idx));
			}
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
		TranslateItem(item);
		
		wrapper.getWriter().setText(item.writer);
		wrapper.getDate().setText(item.date);
		wrapper.getTitle().setText(item.title);
		wrapper.getHeader().setText(item.header);
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