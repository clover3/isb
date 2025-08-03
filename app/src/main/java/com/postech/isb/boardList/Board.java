package com.postech.isb.boardList;

public class Board implements Comparable {
	public int key;
	public String name;
	public boolean favorite;
	public boolean myBoard;
	public boolean hasNewPost;
	public boolean hasNewComment;
	
	public Board (String name, boolean isFavorite) {
		this(-1, name, isFavorite);
	}
	
	public Board (int key, String name, boolean favorite) {
		this.key = key;
		this.name = name;
		this.favorite = favorite;
		this.myBoard = false;
		this.hasNewPost = false;
		this.hasNewComment = false;
	}

	public Board (String name, boolean isFavorite, boolean isMyBoard) {
		this(-1, name, isFavorite, isMyBoard);
	}
	public Board (int key, String _name, boolean _favorite, boolean isMyBoard) {
		this.key = key;
		this.name = _name;
		this.favorite = _favorite;
		this.myBoard = isMyBoard;
		this.hasNewPost = false;
		this.hasNewComment = false;
	}
	
	@Override
	public String toString() {
		return (favorite?"F":" ") + " " + name;
	}
	
	public void setNewPost(int result) {
		if ((result & 0x1) != 0){
			this.hasNewPost = true;
		}
		if ((result & 0x2) != 0)
			this.hasNewComment = true;
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
