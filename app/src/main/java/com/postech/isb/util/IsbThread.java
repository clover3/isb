package com.postech.isb.util;
import java.io.Serializable;

import com.postech.isb.PostechIsb;

/**
 * Route information class. Serializable class to send over activity switching.
 *  
 * @author Jeongbong Seo.
 *
 */
public class IsbThread implements Serializable{
	private static final long serialVersionUID = 1L;
	public int num;
	public String writer;
	public String date;
	public String title;
	public String contents;
	public String comments;
	public String cc;
}
