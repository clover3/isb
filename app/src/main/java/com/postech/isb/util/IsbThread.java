package com.postech.isb.util;
import java.io.Serializable;

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
	public String content;
	public String comments;
	public String cc;
}
