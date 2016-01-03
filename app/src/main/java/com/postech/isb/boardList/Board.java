package com.postech.isb.boardList;

import android.util.Log;

public class Board implements Comparable {
	public int key;
	public String name;
	public boolean favorite;
	public boolean myBoard;
	public boolean newt;
	public boolean comment;
	
	public Board (String name, boolean favorite) {
		this(-1, name, favorite);
	}
	
	public Board (int key, String name, boolean favorite) {
		this.key = key;
		this.name = name;
		this.favorite = favorite;
		this.myBoard = false;
		this.newt = false;
		this.comment = false;
	}

	public Board (String _name, boolean _favorite, boolean _myboard) {
		this(-1, _name, _favorite, _myboard);
	}
	public Board (int key, String _name, boolean _favorite, boolean _myboard) {
		this.key = key;
		this.name = _name;
		this.favorite = _favorite;
		this.myBoard = _myboard;
		this.newt = false;
		this.comment = false;
	}
	
	@Override
	public String toString() {
		return (favorite?"F":" ") + " " + name;
	}
	
	public void setNewPost(int result) {
		if ((result & 0x1) != 0){
			this.newt = true;
		}
		if ((result & 0x2) != 0)
			this.comment = true;
	}

	@Override
	public int compareTo(Object arg0) {
		String name1 = name;
		String name2 = ((Board)arg0).name;
		if( myBoard)
			return -1;
		if( ((Board)arg0).myBoard )
			return 1;
		// TODO Auto-generated method stub
		return name1.compareToIgnoreCase(name2);
	}
	
	@Override
	public boolean equals(Object arg0) {
		return name.equals(((Board)arg0).name);
	}
}