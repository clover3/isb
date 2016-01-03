package com.postech.isb.util;
import java.io.Serializable;

/**
 * Route information class. Serializable class to send over activity switching.
 *  
 * @author Jeongbong Seo.
 *
 */
public class ThreadList implements Serializable, Comparable {
	private static final long serialVersionUID = 1L;
	public int num;
	public boolean notdel;
	public boolean newt;
	public boolean comment;
	public String writer;
	public String date;
	public String header;
	public int cnt;
	public String title;
	private String rawTitle;
	public boolean highlight;
	
	public ThreadList () {
		writer = "Error";
		date = "Error";
		newt = false;
		comment = false;			
		title = "Error";
		highlight = false;
		notdel = false;
		rawTitle = "";
	}
	
	public String GetRawTitle()
	{
		if( rawTitle.length() == 0 )
			return title;
		else
			return rawTitle;
	}
	
	public void SetTitle(String str)
	{
		if( rawTitle.length() == 0 )
			rawTitle = title;
		title = str;
	}

	@Override
	public int compareTo(Object another) {
		// TODO Auto-generated method stub
		return (((ThreadList)another).num) - num;
	}
}
