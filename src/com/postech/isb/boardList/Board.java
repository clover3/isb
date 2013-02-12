package com.postech.isb.boardList;

public class Board implements Comparable {
	public int key;
	public String name;
	public boolean favorite;
	public boolean newt;
	public boolean comment;
	
	public Board (String name, boolean favorite) {
		this(-1, name, favorite);
	}
	
	public Board (int key, String name, boolean favorite) {
		this.key = key;
		this.name = name;
		this.favorite = favorite;
		this.newt = false;
		this.comment = false;
	}
	
	@Override
	public String toString() {
		return (favorite?"F":" ") + " " + name;
	}
	
	public void setNewPost(int result) {
		if ((result & 0x1) != 0)
			this.newt = true;
		if ((result & 0x2) != 0)
			this.comment = true;
	}

	@Override
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		return name.compareToIgnoreCase(((Board)arg0).name);
	}
	
	@Override
	public boolean equals(Object arg0) {
		return name.equals(((Board)arg0).name);
	}
}