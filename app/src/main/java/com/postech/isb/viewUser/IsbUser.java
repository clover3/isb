package com.postech.isb.viewUser;

import java.io.Serializable;

public class IsbUser implements Serializable {
	private String name;
	public int userState;

	static public final int USER_STATE_MAIN = 1;
	static public final int USER_STATE_NEW = 2;
	static public final int USER_STATE_READ = 3;
	static public final int USER_STATE_WRITE = 4;
	static public final int USER_STATE_LIST = 5;
	static public final int USER_STATE_JOURNEY = 6;
	static public final int USER_STATE_TALK = 7;
	static public final int USER_STATE_MYROOM = 8;
	static public final int USER_STATE_USER = 9;
	static public final int USER_STATE_MISC = 10;
	public String GetName()
	{
		return name.substring(2);
	}
	public int GetState()
	{
		return userState;
	}

	public static String StateToString(int state)
	{
		String str;
		if(state == USER_STATE_MAIN )
			str = "메인";
		else if(state == USER_STATE_NEW )
			str = "새글";
		else if(state == USER_STATE_READ )
			str = "읽기";
		else if(state == USER_STATE_WRITE )
			str = "쓰기";
		else if(state == USER_STATE_LIST )
			str = "목록";
		else if(state == USER_STATE_JOURNEY )
			str = "여행";
		else if(state == USER_STATE_TALK )
			str = "대화";
		else if(state == USER_STATE_MYROOM )
			str = "내방";
		else if(state == USER_STATE_USER )
			str = "조사";
		else //if(state == USER_STATE_MISC )
			str = "기타";
		return str;
	}
	public String GetStateString()
	{
		return StateToString(userState);
	}
	public IsbUser()
	{
	}
	public IsbUser(String rawName)
	{
		int state = 0;
		int idx = -1;
		for(int i = 0 ; i < rawName.length() ; i++)
		{
			char c = rawName.charAt(i);
			if( c == 'M' )
				state = USER_STATE_MAIN;
			else if( c == 'N' )
				state = USER_STATE_NEW;
			else if( c == 'R' )
				state = USER_STATE_READ;
			else if( c == 'W' )
				state = USER_STATE_WRITE;
			else if( c == 'L' )
				state = USER_STATE_LIST;
			else if( c == 'J' )
				state = USER_STATE_JOURNEY;
			else if( c == 'T' )
				state = USER_STATE_TALK;
			else if( c == 'X' )
				state = USER_STATE_MYROOM;
			else if( c == 'U' )
				state = USER_STATE_USER;
			else if( c == '-' )
				state = USER_STATE_MISC;
			
			if( state > 0 )
			{
				idx = i;
				break;
			}
		}

		if( idx < 0 )
		{
			name = "Parse Fail";
			state = USER_STATE_MISC;
		}
		else 
		{
			name = rawName.substring(idx);
			int idxEnd = name.indexOf(' ', 2);
			if( idxEnd > 0)
				name = name.substring(0,idxEnd);
		}
		userState = state;
	}
}
