package com.postech.isb.util;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;
import android.view.Menu;

import com.postech.isb.boardList.Board;
import com.postech.isb.compose.NoteEditor;
import com.postech.isb.viewUser.IsbUser;

public class IsbSession {

	private final static int NOT_CONNECTED = 0;
	private final static int CONNECTED = 1;
	private final static int MAIN = 2;
	private final static int SELECT_BOARD = 3;
	private final static int BOARD = 4;
	private final static int DIARY = 5;
	private final static int MAIL = 6;
	private final static int WRITE_MAIL = 7;
	private final static int MYBBS_MAIN = 8;
	private int debug = 0;
	
	private final static int ERROR = 1;
	private final static int WARN = 2;
	private final static int INFO = 3;
	
	private final static int FIRST = 1;
	private final static int PRESS_F = 2;
	private final static int PRESS_J = 3;
	
	public final int THREAD_DELETE = 1;
	public final int THREAD_INVERSE = 2;
	public final int THREAD_MARK = 3;
	public final int THREAD_READERS = 4;
	

	public final int NUM_ROWS = 80; //FIX HERE FOR THE NEW NUMBER OF ROWS
	
	private TelnetInterface telnet;
	private int state;
	public static String userId;
	public static boolean new_mail = false;

	public IsbSession() {
		telnet = new TelnetInterface(80, NUM_ROWS);
		state = NOT_CONNECTED;

		// Run heartbeat
		HeartBeater heartBeater = new HeartBeater();
		Timer heartBeatTimer = new Timer();
		// isb server timeout limit: 15 minutes
		heartBeatTimer.schedule(heartBeater, 1 * 60 * 1000, 1 * 60 * 1000);
	}

	class HeartBeater extends TimerTask {
		private boolean sending_heartbeat = true;
		public void run() {
			try{
				if (sending_heartbeat)
					sendHeartBeat();
			}
			catch (IOException e){
				sending_heartbeat = false;
			}
		}
	}
	
	private void debugMessage(String msg, int degree) {
		if (degree <= debug)
			Log.i("IsbSession", msg);
	}

	private void checkNewMail(String msg) {
		// Note! Just entering the board list does not refresh 'new_mail'.
		if (msg.contains("[새편지왔어요~]")) {
			Log.i("newmbewb", "new mail!");
			new_mail = true;
		}
		else
			new_mail = false;
	}

	public void connect() throws Exception, IOException, SocketTimeoutException {
		String msg;

		if (state == NOT_CONNECTED)
		{
			telnet.connect("isb.or.kr", 22);


			msg = telnet.waitfor("(?s).*\\[5;12H$");
			debugMessage(msg, INFO);

			state = CONNECTED;
		}
		else
			debugMessage("Already connected!", ERROR);
	}
	
	public boolean isConnected() {
		return state != NOT_CONNECTED;
	}
	
	public boolean isMain() {
		return state == MAIN;
	}

	public void disconnect() {
		try {
		switch (state)
		{
		case NOT_CONNECTED : break;
		case CONNECTED : telnet.send("off"); break;
		default : gotoMenu(NOT_CONNECTED);
		}
		telnet.disconnect();
		} catch (Exception e) {
			;
		}
		state = NOT_CONNECTED;
	}

	static private String [] pwdPrompt = {"(?s).* : $", "(?s).*6;12H$"};
	private final String LOGIN_SUCCESS  = "(?s).*\\(end\\)\033\\[27m$";
	private final String LOGIN_FAIL = "(?s).*\033\\[5;12H$";
	private final String LOGIN_ALREADY = "(?s).*\\?\\?\033\\[1;12H$";
	private final String [] LOGIN = {LOGIN_SUCCESS, LOGIN_FAIL, LOGIN_ALREADY};

	public boolean login(String id, String pwd, boolean evict) throws IOException, SocketTimeoutException {
		boolean result = false;
		String msg;
		userId = id;
		if (state == CONNECTED)
		{		
			telnet.send(id);
			msg = telnet.waitfor(pwdPrompt);
			debugMessage(msg, INFO);

			telnet.send(pwd);
			msg = telnet.waitfor(LOGIN);
			debugMessage(msg, INFO);

			if (msg.matches(LOGIN_SUCCESS))
			{
				debugMessage("Login Success.", debug);
				telnet.send_wo_r(" ");
				msg = telnet.waitfor("(?s).*\033\\[27m$");
				checkNewMail(msg);
				debugMessage(msg, INFO);

				result = true;
				state = MAIN;
			}
			else if (msg.matches(LOGIN_ALREADY))
			{
				debugMessage("Login conflict.", INFO);
				if (evict) {
					telnet.send_wo_r("y");
					telnet.flush();
					telnet.send_wo_r("\r");
					telnet.flush();

					msg = telnet.waitfor(LOGIN_SUCCESS);
					debugMessage(msg, INFO);

					telnet.send_wo_r(" ");
					msg = telnet.waitfor("(?s).*\033\\[27m$");
					checkNewMail(msg);
					debugMessage(msg, INFO);

					debugMessage("Login Success with evict.", INFO);

					result = true;
					state = MAIN;						
				} else {
					telnet.send("n");
					debugMessage("Login gave up.", INFO);						
				}
			}
			else
				debugMessage("Login Fail.", INFO);					
		}			

		return result;
	}
	
	private String expect(final int dst) {
		String result = new String("");

		switch (dst) {
			case SELECT_BOARD: result = new String("(?s).*게시판 선택 : (?:\033\\[K)?$"); break;
			case MAIN: result = new String("(?s).*(?:\033\\[2;73H|\\[27m)$"); break;
			case NOT_CONNECTED: result = new String("(?s).*-\r\n$"); break;
			case BOARD:
			case DIARY:
			case MAIL:
				result = new String("(?s).*\\[\\d+;2H$|.*게시판이 비어있습니다.\n\r$"); break;
			case WRITE_MAIL: result = new String("(?s).*받을 사람: $"); break;
			// Do we need \033[\\d+;2H$ for the end of board/diary/mail?
		default : debugMessage("Unexpected case for expect!", ERROR); break;
		}

		return result;
	}

	private String gotoCommand(final int dst) {
		String result = new String ("");

		switch (dst) {
			case SELECT_BOARD: result = new String("s"); break;
			case NOT_CONNECTED: result = new String("g"); break;
			case DIARY: result = new String("m\rd"); break;
			case MAIL: result = new String("m\rr"); break;
			case WRITE_MAIL: result = new String("m\rs"); break;
			default : debugMessage("Unexpected case!", ERROR); break;
		}
		
		return result;
	}
	
	private String quitCommand(final int src) {
		String result = new String ("");

		switch (src) {
			case SELECT_BOARD: result = new String("\r"); break;
			case BOARD: result = new String("q"); break;
			case DIARY: result = new String("qe\r"); break;
			case MAIL: result = new String("qe\r"); break;
			case MYBBS_MAIN: result = new String("e\r"); break;
			default : debugMessage("Unexpected case in quitCommand!", ERROR); break;
		}
		
		return result;		
	}
	
	private boolean gotoMenu(final int where) {
		boolean result = false;
			
		if (state == where)
			return true;
			
		try {
			if (state == MAIN)
			{	
				debugMessage("Go to the real destination.", INFO);
				telnet.send(gotoCommand(where));
				if (where != NOT_CONNECTED)
					telnet.waitfor(expect(where));
				state = where;
				debugMessage("Arrived.", INFO);
				result = true;
			}
			else //Go back to main and retry.
			{
				debugMessage("Go back to main first", INFO);
				telnet.send_wo_r(quitCommand(state));
				checkNewMail(telnet.waitfor(expect(MAIN)));
				state = MAIN;
				debugMessage("Now in main.", INFO);
				result = gotoMenu (where);
			}
		} catch (java.io.IOException e) {
			debugMessage("IOException on login", ERROR);
		}
		
		return result;
	}
		
	public ArrayList<Board> getBoardList() throws IOException {
		ArrayList<Board> boardList = new ArrayList<Board>();
		String list = null;

		gotoMenu(SELECT_BOARD);

		boolean hasNext = true;
		Pattern p;
		Matcher m;
		String match;
		while (hasNext)
		{
			telnet.send_wo_r(" ");
			hasNext = false;

			list = telnet.waitfor("(?s).*\\[3;15H$");

			p = Pattern.compile("\\w+/\\w+");
			m = p.matcher(list);

			while (m.find()) {
				match = m.group();
				boardList.add(new Board(match, false));
				debugMessage("Match : " + match, INFO);
			}
			
			if (list.contains("계속"))
				hasNext = true;
		}
				
		Collections.sort(boardList);
		
		if (gotoMenu(MAIN))
			debugMessage("Go back to main success", INFO);

		return boardList;		
	}
	
	public ArrayList<IsbUser> readUserList() throws IOException {
		//// Network Activity /////////
		telnet.send_wo_r("\024");
		String data = telnet.waitfor("(?s).*\\[\\d+;5H$");
		telnet.send_wo_r("\024");
		//// End of Network Activity ////

		debugMessage ("readUserList data : "+ data, debug);

		ArrayList<IsbUser> UserItems = new ArrayList<IsbUser>();
		
		final String regex = " [MNRWLJTXU-][*x ].+? ";
		Pattern pattern = Pattern.compile(regex);
		Matcher match = pattern.matcher(data);
		while(match.find())
		{
			debugMessage ("Match:"+match.group(), debug);
			IsbUser user = new IsbUser(match.group());
			UserItems.add(user);
		}
		
		debugMessage ("readUserList EXIT", debug);
		return UserItems;
	}

	public boolean goToBoard(String board) throws IOException {
		if (board.equals("diary")) {
			if (state != MAIN) {
				telnet.send_wo_r(quitCommand(state));
				checkNewMail(telnet.waitfor(expect(MAIN)));
				state = MAIN;
			}

			telnet.send(gotoCommand(DIARY));
			state = DIARY;
			return true;
		} else if (board.equals("mail")) {
			if (state != MAIN) {
				telnet.send_wo_r(quitCommand(state));
				checkNewMail(telnet.waitfor(expect(MAIN)));
				state = MAIN;
			}

			telnet.send(gotoCommand(MAIL));
			state = MAIL;
			return true;
		} else {
			gotoMenu(SELECT_BOARD);

			telnet.send_wo_r(board);

			String result = telnet.waitfor(board + "|\007+.*");

			if (result.startsWith("\007")) {
				debugMessage("Invalid board.", INFO);
				for (int i = 0; i < board.length(); i++)
					telnet.send_wo_r("\b");
				telnet.send_wo_r("\r");
				telnet.waitfor(expect(MAIN));
				state = MAIN;
				return false;
			} else {
				debugMessage("Go to board success.", INFO);
				telnet.send_wo_r("\r");
				state = BOARD;
				return true;
			}
		}
	}
	
	
	/* < 정규표현식 해석 >
	 * Raw String : "  175   pietype       9/ 8   11  > 슬비버그"
	 * 
	 * "^(?:\033\\[\\d+;1H) : 커서 이동 ANSI escape code 
	 * ?[ >]  : 공백이거나 꺽쇠문자('>')  
	 * (\033\\[7m) : Highlight 여부
	 * ?\\s*(\\d+) : 글 번호
	 * (?:\033\\[27m) : Highlight 끝
	 * ?(.)(.) : N과 m 으로 마킹된 여부
	 * \\s* : 공백
	 * (\\S+) : 아이디
	 * \\s* : 공백 
	 * ([ 1][0-9]/[ 1-3][0-9]) : 날짜
	 * \\s* : 공백
	 * (\\d+) : 조회수 
	 * \\s* : 공백
	 * (.*?) : 제목 
	 * (?:\033\\[K) :
	 * ?(?:\033\\[\\d+;2H)?$");
	*/
	private ArrayList<ThreadList> parseOnePage(String str){
		ArrayList<ThreadList> result = new ArrayList<ThreadList>();
		Scanner s = new Scanner(new String(str));
		int g_hl, g_num, g_notdel, g_nc, g_writer, g_date, g_cnt, g_title, g_mailstuff;
		Pattern p;
		s.useDelimiter("[\n\r\0]");

		if (state == BOARD) {
			p = Pattern.compile("^(?:\033\\[\\d+;1H)?[ >](\033\\[7m)?\\s*(\\d+)(?:\033\\[27m)?(.)(.)\\s*(\\S+)\\s*([ 1][0-9]/[ 1-3][0-9])\\s*(\\d+)\\s(.*?)(?:\033\\[K)?(?:\033\\[\\d+;2H)?$");
			g_hl = 1;
			g_num = 2;
			g_notdel = 3;
			g_nc = 4;
			g_writer = 5;
			g_date = 6;
			g_cnt = 7;
			g_title = 8;
			g_mailstuff = 0;
		}
		else if (state == DIARY) {
			p = Pattern.compile("^(?:\033\\[\\d+;1H)?[ >](\033\\[7m)?\\s*(\\d+)(?:\033\\[27m)?\\s*(.)\\s*(\\S+)\\s*([ 1][0-9]/[ 1-3][0-9])\\s(.*?)(?:\033\\[K)?(?:\033\\[\\d+;2H)?$");
			g_hl = 1;
			g_num = 2;
			g_notdel = 3;
			g_nc = 0;
			g_writer = 4;
			g_date = 5;
			g_cnt = 0;
			g_title = 6;
			g_mailstuff = 0;
		}
		else if (state == MAIL) {
			p = Pattern.compile("^(?:\033\\[\\d+;1H)?[ >](\033\\[7m)?\\s*(\\d+)(?:\033\\[27m)?(.)(.)\\s*(.*?)(?:\033\\[K)?(?:\033\\[\\d+;2H)?$");
			g_hl = 1;
			g_num = 2;
			g_notdel = 3;
			g_nc = 4;
			g_mailstuff = 5;
			g_writer = 0;
			g_date = 0;
			g_cnt = 0;
			g_title = 0;
			/* Mail is parsed based on the position of characters. */
		}
		else {
			Log.i("newmbewb", "Unrecognized situation!");
			/* Error! */
			return result;
		}
		
		while (s.hasNext())
		{
			String token = s.next();
			Matcher m = p.matcher(token);
			
			if (m.matches())
			{
				ThreadList line = new ThreadList();
				line.num = Integer.valueOf(m.replaceAll("$" + g_num));
				line.highlight = m.replaceAll("$" + g_hl).contains("[7m");
				line.notdel = m.replaceAll("$" + g_notdel).equals("m");

				if (g_nc == 0) {
					line.newt = false;
					line.comment = false;
				} else {
					String tmp = m.replaceAll("$" + g_nc);
					line.newt = tmp.equals("N");
					line.comment = tmp.equals("C");
				}

				if (state == MAIL) {
					String remain = m.replaceAll("$" + g_mailstuff);
					int date_position = hangulCutMailSender(remain);
					line.writer = remain.substring(0,date_position);
					String date = remain.substring(date_position + 1, date_position + 6);
					line.date = date.replaceAll("^\\s+", "").replaceAll("\\s+$", "");
					line.cnt = 0;
					line.header = "";
					line.title = remain.substring(date_position + 7);
					Log.i("newmbewb", "data:"+date);
				}
				else {
					line.writer = new String(m.replaceAll("$" + g_writer));
					line.date = new String(m.replaceAll("$" + g_date));

					if (g_cnt == 0)
						line.cnt = 0;
					else
						line.cnt = Integer.valueOf(m.replaceAll("$" + g_cnt));

					line.header = "";
					line.title = new String(m.replaceAll("$" + g_title));
				}
				result.add(line);
				debugMessage("Add thread " + line.num, INFO);
			}
			else if (token.equals(""))
				continue;
			else if (token.matches("(?:\033\\[\\d+;1H[> ])*(?:\033\\[\\d+;2H)?"))
				continue;
			else
				debugMessage("Bad one " + token + "\"", WARN);
		}
		
		return result;
	}
	
	/**
	 * @param board: name for board
	 * @return 0: no new therad, no new comment
	 * @return 1: new thread, no new comment
	 * @return 2: no new therad, new comment
	 * @return 3: new thread, new comment
	 */
	public int searchNewPost(String board) throws IOException{
		int i, result = 0, cursize;
		if (!goToBoard(board)) {
			debugMessage("getThreadList: go to board fail.", WARN);
			return 0;
		}
		String msg = telnet.waitfor(expect(BOARD));
		msg = msg.split("\033\\[4;1H")[1];

		ArrayList<ThreadList> current;
		current = parseOnePage(msg);
		// Getting the last page start here
		telnet.send_wo_r("$"); // in case of having been visited.
		
		String [] response = new String[3];
		response[0] = "(?s).*>$"; // Already last page.
		response[1] = "(?s).*;2H$"; // Page change && cursor was not on top.
		response[2] = "\007"; // No thread
		
		msg = telnet.waitfor(response);
		
		if (msg.matches(response[0]) == false) // Page change occur.
		{
			debugMessage("Current Pape is not the last page.", INFO);
			current = parseOnePage(msg);
			debugMessage("Parsing the last page finish.", INFO);
		}
		// End of getting the last page
		
		if (gotoMenu(MAIN))
			debugMessage("Go back to main successfully", INFO);
		cursize = current.size();
		for (i = 0; i < cursize; i++){
			if (current.get(cursize - i - 1).newt)
				result |= 1;
			if (current.get(cursize - i - 1).comment)
				result |= 2;
		}
		return result;
	}
	/**
	 * 
	 * @param start : Start thread number lower boundary?
	 * @param end : End thread number upper boundary?
	 * @return thread_list. null if board doesn't exist.
	 */
	public ArrayList<ThreadList> getThreadList(String board, int start, int end) throws IOException {
		ArrayList<ThreadList> result = new ArrayList<ThreadList>();

		if (start < 1)
			return result;

		if (!goToBoard(board)) {
			debugMessage("getThreadList: go to board fail.", WARN);
			return null;
		}

		debugMessage("getThreadList: go to board success.", INFO);

		// TODO: index check.
		String msg = telnet.waitfor(expect(BOARD));
		msg = msg.split("\033\\[4;1H")[1];

		ArrayList<ThreadList> current;

		// Is there any thread?
		current = parseOnePage(msg);
		if (current.size() != 0) {
			debugMessage("getThreadList: Parsing current page success.", INFO);

			// Isn't there 'start thread' in current page ?
			if (current.get(0).num > start || current.get(current.size()-1).num < start) {

				debugMessage("Thread is not in current page.", INFO);				
				telnet.send(String.valueOf(start)); // Move to thread

				msg = telnet.waitfor("(?s).*;2H$");

				current = parseOnePage(msg);
			}

			for (int i = 0; i < current.size(); i++) {
				if (current.get(i).num == start) {
					current.subList(0, i).clear();
					break;
				}
			}

			if (current.size() == 0)
				debugMessage("Critical bug. It cannot be empty.", ERROR);

			while (current.get(current.size()-1).num < end)
			{
				result.addAll(current);

				telnet.send_wo_r("N");
				String [] response = new String[2];
				response[0] = "(?s).*;2H$";
				response[1] = "\07"; 
				msg = telnet.waitfor(response);

				// No more next page.
				if (msg.endsWith(response[1])) {
					current.clear();
					break;
				}

				current = parseOnePage(msg);
				if (current.size() == 0)
					break;
			}

			for (int i = 0; i < current.size() && current.get(i).num <= end; i++) {
				result.add(current.get(i));
			}

			//TODO Move cursor to the last thread. It's better safe than sorry...
			telnet.send_wo_r("$"); // in case of having been visited.

			String [] response = new String[2];
			response[0] = "(?s).*>$"; // Already last page.
			response[1] = "(?s).*;2H$"; // Page change && cursor was not on top.

			msg = telnet.waitfor(response);
		}
		
		debugMessage("Get list successfully.", INFO);
		if (gotoMenu(MAIN))
			debugMessage("Go back to main successfully", INFO);

		return result;
	}
		
	public ArrayList<ThreadList> getLastPageThreadList(String board) throws IOException {
		ArrayList<ThreadList> result = new ArrayList<ThreadList>();
		String msg;
		
		if (!goToBoard(board)) {
			debugMessage("getLastPageThread: go to board fail.", INFO);
			return result;
		}
		debugMessage("getLastPageThread: go to board success.", INFO);
		
		// Current page.
		// It could not be last page since this board could have been visited.
		msg = telnet.waitfor(expect(state));
		msg = msg.split("\033\\[4;1H")[1];

		result = parseOnePage(msg);
		debugMessage("Parsing the current pape finish.", INFO);

		telnet.send_wo_r("$"); // in case of having been visited.

		String [] response = new String[3];
		response[0] = "(?s).*>$"; // Already last page.
		response[1] = "(?s).*;2H$"; // Page change && cursor was not on top.
		response[2] = "\007"; // No thread

		msg = telnet.waitfor(response);

		if (msg.matches(response[0]) == false) // Page change occur.
		{
			debugMessage("Current Pape is not the last page.", INFO);
			result = parseOnePage(msg);
			debugMessage("Parsing the last page finish.", INFO);
		}
					
		debugMessage("Get list success", INFO);
		if (gotoMenu(MAIN))
			debugMessage("Go back to main success", INFO);
		
		return result;
	}

	public boolean writeMail(String recver, String title, String content) throws IOException {
		String [] user_list = recver.split("\\s");
		String expected_response = "(?s).*받을 사람: ";
		String result;
		boolean ret;

		if (recver.equals(NoteEditor.EMPTY_RECEIVER )) {
			debugMessage ("Invalid username.", ERROR);
			return false;
		}

		if (title == null || title.length() > 73) {
			debugMessage ("Invalid title.", ERROR);
			return false;
		}

		if (content == null) {
			debugMessage ("Invalid content.", ERROR);
			return false;
		}
		content = content.replaceFirst(".*?\n", "");

		// Check the receiver count
		if (user_list.length > 10)
			return false;

		telnet.send(gotoCommand(WRITE_MAIL));
		telnet.waitfor(expect(WRITE_MAIL));
		/* Enter users */
		for (int i = 0; i < user_list.length; i++) {
			telnet.send(user_list[i]);
			expected_response += "(\\S+) ";
			if (i < 9) {
				telnet.waitfor(expected_response+"$");
			}
		}

		if (user_list.length < 10) {
			telnet.send("");
		}
		telnet.waitfor("(?s).*제  목: $");

		telnet.send(title);
		telnet.waitfor("(?s).*\\[1;1H$");

		telnet.send(content);
		telnet.send_wo_r("\027");
		telnet.waitfor("(?s).* \\[S\\] $");

		telnet.send_wo_r("S");
		result = telnet.waitfor("(?s).*\\033\\[2;71H$");
		if (!result.contains("사용자명이 틀립니다.") && result.contains("편지를 보냈습니다."))
			ret = true;
		else
			ret = false;

		state = MYBBS_MAIN;
		gotoMenu(MAIN);
		return ret;
	}

	public boolean writeToBoard(String board, String title, String content) throws IOException {
		if (title == null || title.length() > 73) {
			debugMessage ("Invalid title.", ERROR);
			return false;
		}
		
		if (content == null) {
			debugMessage ("Invalid content.", ERROR);
			return false;			
		}
		content = content.replaceFirst(".*?\n", "");
					
		if (!goToBoard(board)) {
			debugMessage("writeToBoard: go to board fail.", INFO);
			return false;
		}
		
			telnet.send_wo_r("W"); // We use capital 'W' for writing in an empty board
			String msg = telnet.waitfor("(?s).*제목 : $");
			
			telnet.send(title);			
			msg = telnet.waitfor("(?s).*\\[1;1H$");
			
			telnet.send(content);
			telnet.send_wo_r("\027");
			msg = telnet.waitfor("(?s).* \\[S\\] $");
			
			telnet.send_wo_r("S");
			msg = telnet.waitfor("(?s).*누르세요\\]$");
			
			telnet.send_wo_r(" ");
			msg = telnet.waitfor(expect(BOARD));
				
		debugMessage("Write success", INFO);
		if (gotoMenu(MAIN))
			debugMessage("Back to main success", INFO);
		
		return true;
	}
	
	public boolean EditToBoard(String board, String title, String content, int idx) throws IOException {
		if (title == null || title.length() > 73) {
			debugMessage ("Invalid title.", ERROR);
			return false;
		}
		
		if (content == null) {
			debugMessage ("Invalid content.", ERROR);
			return false;			
		}
		content = content.replaceFirst(".*?\n", "");
					
		if (!goToBoard(board)) {
			debugMessage("witeToBoard: go to board fail.", INFO);
			return false;
		}
		telnet.send(String.valueOf(idx));
		telnet.waitfor("(?s).*(?:;2H|>)$");
		telnet.send_wo_r("E");
		String [] response = new String[2];
		response[0] = "(?s).*\033\\[3;\\d+H$"; // Start Editing
		response[1] = "(?s).*\007$"; // fail
		String msg = telnet.waitfor(response);
		if (msg.matches("(?s).*\007")){
			if (gotoMenu(MAIN))
				debugMessage("Back to main success", INFO);
			return false;
		}
		telnet.send_wo_r("\001"); //Ctrl-a
		msg = telnet.waitfor("(?s).*\033\\[3;7H$");
		telnet.send_wo_r("\013"); //Ctrl-k
		msg = telnet.waitfor("(?s).*\033\\[3;7H$");
		telnet.send(title);
		msg = telnet.waitfor("(?s).*\033\\[1;1H$");
		while(true){
			String [] response1 = new String[3];
			String clear_string = "\013";
			telnet.send_wo_r(clear_string); //Ctrl-k
			response1[0] = "\033\\[.?K$";
			response1[1] = "(?s).*\033\\[1;1H$";
			response1[2] = "(?s).*\007$"; // EOF
			msg = telnet.waitfor(response1);
			if (msg.contains("\007"))
				break;
		}
			
		telnet.send(content);
		telnet.send_wo_r("\027");
		msg = telnet.waitfor("(?s).* \\[S\\] $");
		telnet.send_wo_r("S");
		msg = telnet.waitfor("(?s).* \\[N\\]\\? $");
		telnet.send_wo_r("\r\n");
		msg = telnet.waitfor(expect(BOARD));
		debugMessage("Edit success", INFO);
		if (gotoMenu(MAIN))
			debugMessage("Back to main success", INFO);
		
		return true;
	}
	
	private final byte [] threadHeader = {(byte)0xa6, (byte)0xa1, (byte)0x1b, (byte)0x5b, (byte)0x4b, (byte)0xa};
	private final byte [] threadTail = {(byte)0x1b, (byte)0x5b, (byte)0x32, (byte)0x37, (byte)0x6d};

	private String byteArrayToHex(byte[] a) {
		// Just for debugging
		StringBuilder sb = new StringBuilder();
		for(final byte b: a)
			sb.append(String.format("%02x ", b&0xff));
		return sb.toString();
	}

	private IsbThread parsingThread(String str) {
		IsbThread t = new IsbThread();
		
		// Delete all '[\033[K' and '\r\000's 
		Pattern p = Pattern.compile("\033\\[K|\r\0|\033\\[\\d+;\\d+H|\033\\[7m|\033\\[27m");
		Matcher m = p.matcher(str);

		str = m.replaceAll("");

		// Warning : No exception handling.
		Scanner s = new Scanner(new String(str));
		s.useDelimiter("[\n\r\0]");

		//s.next();
		String writer = s.next();
		t.writer = writer;
		
		String date = s.next();
		t.date = date;
		
		String title = s.next();
		t.title = title;

		String commentHeader = "───────────────────────────────── 커멘트 ∇ ─";
		String commentTail   = "───────────────────────────────── 커멘트 △ ─";

		boolean end_of_thread = true;
		Pattern p_comment = Pattern
				.compile("^\\s*(\\w+)\\((\\d+,\\d+:\\d+)\\):\"(.*)\"\n?$");
		Pattern p_not_white_space = Pattern.compile(".+");
		t.contents = "";
		t.comments = "";
		t.cc = "";
		StringBuffer contents = new StringBuffer();
		// new line bug (github #4)
		String contentFirstLine = s.next();
		contents.append(contentFirstLine);
		if (state == MAIL) {
			String cc = contents.toString();
			if (!cc.equals("")) {
				t.cc = "\n"+cc;
				s.next();
				// Skip a line
				// FIXME if 'cc' is over two lines?
			}
			else {
				// For give a space.
				// Only 'mail' does not give a space between the title and the content.
				// FIXME if you find the reason.
			}
		}
		do{
			contents = new StringBuffer();
			while (s.hasNext())
			{
				String token = s.next();
				if (token.contains(commentHeader))
					break;
				contents.append("\n" + token);
			}

			t.contents += contents.toString();
		
			StringBuffer comments = new StringBuffer();
			boolean end_of_comments = true;
			while (s.hasNext())
			{
				String token = s.next();
				if (token.contains(commentTail)){
					t.comments = comments.toString();
					break;
				}
				comments.append(token);
				comments.append("\n");
				Matcher match = p_comment.matcher(token.toString());
				if (token.length() > 0 && !match.matches()){
					Log.i("newm", "Unfinished comment!");
					Log.i("newm", "hex: " + byteArrayToHex(token.getBytes()) + " length: " + token.length());
					t.contents += commentHeader+"\n";
					t.contents += comments.toString();
					t.comments = "";
					end_of_thread = false;
					end_of_comments = false;
					break;
				}
			}
			if (!end_of_comments)
				continue;
			
			
			end_of_thread = true;
			StringBuffer remaining_lines = new StringBuffer();
			
			while (s.hasNext()){
				String token = s.next();
				remaining_lines.append(token);
				if (s.hasNext(p_not_white_space)){
					Log.i("newm", "Lines after comment have been detected!");
					end_of_thread = false;
					t.contents += commentHeader + "\n";
					t.contents += t.comments;
					t.contents += commentTail + "\n";
					t.contents += remaining_lines.toString();
					t.comments = "";
					break;
				}
			}
		} while(!end_of_thread);

		return t;
	}
	
	//static final Pattern lastNumP = Pattern.compile("^(?:\033\\[\\d+;1H)?[ >](?:\033\\[7m)?\\s*(\\d+)(?:\033\\[27m)?.*$");
	static final Pattern lastNumP = Pattern.compile("^[ >](?:\033\\[7m)? *(\\d+)[^\n]*$");
	
	private int getLastThreadNum(String msg) {
		int lastIdx = msg.lastIndexOf("\n\r");
		msg = msg.substring(lastIdx+2);
		Log.i("newmbewb", "getLastThreadNum start");
		Log.i("newmbewb", msg);
		
		Matcher m = lastNumP.matcher(msg);

		if (m.matches()){
			Log.i("newmbewb", m.replaceAll("$1"));
			
			lastIdx = Integer.valueOf(m.replaceAll("$1"));
			Log.i("newmbewb", "Last number: "+Integer.toString(lastIdx));
		}
		else {
			Log.i("newmbewb", "get last thread num error");
			debugMessage ("readThead: get last thread num error.", ERROR);
			lastIdx = 100000;
		}
		
		return lastIdx;
	}
	
	private String parsePage(String msg){
		Pattern p2 = Pattern.compile("^\033\\[(\\d+);1H.*");
		int line = 2;
		String [] ret_buf = new String[NUM_ROWS+2];
		String tmp_msg;
		String ret = "";
		Matcher m;
		
		tmp_msg = msg.replaceAll("\033\\[(\\d+);1H", "\n\033\\[$1;1H");
		tmp_msg = tmp_msg.replace("\r", "");
		tmp_msg = tmp_msg.replace("\000", "");
		String [] list = tmp_msg.split("\n");
		for (String l: list){
			m = p2.matcher(l);
			if (m.find())
				line = Integer.valueOf(m.group(1));
			ret_buf[line] = l;
			line ++;
		}
		for (int i = 3; i < NUM_ROWS; i++){// start line: 3, end line: 79
			if (ret_buf[i] == null)
				ret += "\n";
			else
				ret += ret_buf[i] + "\n";
		}
		return ret;
	}

	public IsbThread readThread(String board, int idx)	throws IOException {
		IsbThread thread = new IsbThread();
		int state = FIRST;
		int initial_percent = 0;
		int current_percent = 0;
		boolean in_loop = true;
		Pattern p = Pattern.compile("(?s).*\\((\\d+)%\\)\033\\[27m(?:\033\\[K)?$");
		
		String end_symbol = "(?s).*\\(end\\)\033\\[27m(?:\033\\[K)?$";
		Matcher m;
		Log.i("newmbewb", "readThread start");
		if (!goToBoard(board)) {
			debugMessage("readThread: go to board fail.", INFO);
			return null;
		}
		
		debugMessage ("readThead: go to board success.", INFO);
				
		//TODO check idx
		String msg = telnet.waitfor(expect(BOARD));
		int lastIdx = getLastThreadNum(msg);
		Log.i("newmbewb", Integer.toString(lastIdx));
		
		if (idx < 1 || lastIdx < idx) {
			debugMessage("Invalid thread number", WARN);
			Log.i("newmbewb", "Invalid thread number");
			
			if (gotoMenu(MAIN))
				debugMessage("Go back to main successfully", WARN);
			return null;
		}
		
		telnet.send(String.valueOf(idx));
		telnet.waitfor("(?s).*(?:;2H|>)$");
		telnet.send_wo_r(" ");
		
		// Get Contents
		String tail = new String("(?s).*\033\\[27m(?:\033\\[K)?$");
		debugMessage ("Get body.", INFO);
		msg = telnet.waitfor(tail);
		
		StringBuffer result = new StringBuffer();
		//result.append(msg.split("\033\\[7m─ 계속:sp")[0]);
		result.append(parsePage(msg)); //For threads in almost empty board
		if (msg.matches(end_symbol))
			in_loop = false;
					
		// Until meet (end)
		while(in_loop) {
			m = p.matcher(msg);
			if (m.find())
				current_percent = Integer.valueOf(m.replaceAll("$1")); //Know how many lines.
			else //if it is end of file (prevent from wrong regex match)
				in_loop = false;
			
			if (state == FIRST){
				initial_percent = current_percent;
				initial_percent += 1;
			}
			else if (state == PRESS_F){
				result.append(parsePage(msg));
			}
			else if (state == PRESS_J){
				debugMessage ("More lines. appending..", INFO);
				String [] list = msg.split("\n");
				if (list.length == 1)
					result.append("\n");
				else if (list[list.length - 1].matches("(?s).*\033\\[\\d+;1H.*"))
					result.append("\n");
				else
					result.append(list[list.length - 2]+"\n");
			}
			if (!in_loop)
				break;
			if (initial_percent + current_percent <= 100) state = PRESS_F;
			else state = PRESS_J;
			
			if (state == PRESS_F) telnet.send_wo_r("f"); // Send command
			else if (state == PRESS_J) telnet.send_wo_r("j");
			msg = telnet.waitfor(tail);
		}
		//TODO: add last line
		thread = parsingThread(result.toString());
		thread.num = idx;
		debugMessage ("Thread pasing finish.", INFO);
		
		// Go back to board.
		telnet.send_wo_r(" ");
		msg = telnet.waitfor("(?s).*;2H$");
		debugMessage ("Go back to board successfully.", INFO);
		
		//TODO Move cursor to the last thread. It's better safe than sorry...
		telnet.send_wo_r("$"); // in case of having been visited.
		
		String [] response = new String[2];
		response[0] = "(?s).*>$"; // Already last page.
		response[1] = "(?s).*;2H$"; // Page change && cursor was not on top.
		
		msg = telnet.waitfor(response);
		
		debugMessage("Read thread successfully.", INFO);
		if (gotoMenu(MAIN))
			debugMessage("Go back to main successfully", WARN);

		return thread;
	}
	
	public boolean leaveComment(String board, int idx, String cmt) throws IOException {
		
		if (cmt == null || cmt.length() == 0) {
			debugMessage("leaveComment: wrong comment.", INFO);
			return false;
		}
		
		if (!goToBoard(board)) {
			debugMessage("leaveComment: go to board fail.", INFO);
			return false;
		}

		debugMessage ("leaveComment: go to board success.", INFO);

		//TODO check idx
		String msg = telnet.waitfor(expect(BOARD));
		int lastIdx = getLastThreadNum(msg);

		if (idx < 1 || lastIdx < idx) {
			debugMessage("Invalid thread number", WARN);

			if (gotoMenu(MAIN))
				debugMessage("Go back to main successfully", WARN);
			return false;
		}
		
		telnet.send(String.valueOf(idx));
		telnet.waitfor("(?s).*(?:;2H|>)$");

		//String remaining = cmt;
		int[] cut = hangulCutter(cmt);
		int cut_len = cut.length;
		
		int begin = 0, end = 0;
		for (int i = 0; i < cut_len; i++)
		{		
			end = cut[i];
			Log.i("newmbewb", "error:"+Integer.toString(cmt.length())+Integer.toString(end));
			telnet.send_wo_r("c");
			telnet.send(cmt.substring(begin, end));
			telnet.waitfor(expect(BOARD));
			begin = end;
		}
		telnet.send_wo_r("c");
		telnet.send(cmt.substring(begin, cmt.length()));
		telnet.waitfor(expect(BOARD));
		debugMessage ("LeaveMessage finish.", INFO);

		//TODO Move cursor to the last thread. It's better safe than sorry...
		telnet.send_wo_r("$"); // in case of having been visited.

		String [] response = new String[2];
		response[0] = "(?s).*>$"; // Already last page.
		response[1] = "(?s).*;2H$"; // Page change && cursor was not on top.

		msg = telnet.waitfor(response);

		debugMessage("Leave Comment successfully.", INFO);
		if (gotoMenu(MAIN))
			debugMessage("Go back to main successfully", WARN);

		return true;
	}
	
	public String viewThreadReader(String board, int idx) throws IOException {
		String [] response = new String[2];
		String strReader;
		boolean result = false;
		if (!goToBoard(board)) {
			debugMessage("deleteThread: go to board fail.", INFO);
			return null;
		}

		debugMessage ("deleteThread: go to board success.", INFO);

		//TODO check idx
		String msg = telnet.waitfor(expect(BOARD));
		int lastIdx = getLastThreadNum(msg);

		if (idx < 1 || lastIdx < idx) {
			debugMessage("Invalid thread number", WARN);

			if (gotoMenu(MAIN))
				debugMessage("Go back to main successfully", WARN);
			return null;
		}
		
		telnet.send(String.valueOf(idx));
		telnet.waitfor("(?s).*(?:;2H|>)$");
		telnet.send_wo_r("\025");

		String [] response1 = new String[2];
		response1[0] = "(?s).*\\]$"; // Already last page.
		response1[1] = "\007"; // No thread

		msg = telnet.waitfor(response1);
		Log.i("clover",msg);
		if (msg.equals("\007")) // Case : Ctrl+U is unavailable
			strReader = null;
		else
		{
			int idxBeg = msg.indexOf(':');
			int idxEnd = msg.lastIndexOf("80;1H");
			if( idxEnd < 0)
			{
				debugMessage("Parsing Error : view Readers", WARN);
			}
			else
				msg = msg.substring(idxBeg+1, idxEnd - 2);
			strReader = msg.replace("\n\r", "");
		}
		debugMessage ("View readers finish.", INFO);

		telnet.send_wo_r("$"); // in case of having been visited.

		response[0] = "(?s).*>$"; // Already last page.
		response[1] = "(?s).*;2H$"; // Page change && cursor was not on top.

		msg = telnet.waitfor(response);

		//debugMessage("DeleteThread successfully.", INFO);
		if (gotoMenu(MAIN))
			debugMessage("Go back to main successfully", WARN);

		return strReader;
	}
	public boolean modifyThread(String board, int idx, int command) throws IOException {
		String [] response = new String[2];
		boolean result = false;
		if (!goToBoard(board)) {
			debugMessage("deleteThread: go to board fail.", INFO);
			return false;
		}

		debugMessage ("deleteThread: go to board success.", INFO);

		//TODO check idx
		String msg = telnet.waitfor(expect(BOARD));
		int lastIdx = getLastThreadNum(msg);

		if (idx < 1 || lastIdx < idx) {
			debugMessage("Invalid thread number", WARN);

			if (gotoMenu(MAIN))
				debugMessage("Go back to main successfully", WARN);
			return false;
		}
		
		telnet.send(String.valueOf(idx));
		telnet.waitfor("(?s).*(?:;2H|>)$");
		if (command == THREAD_DELETE){
			telnet.send_wo_r("d");
		
			response[0] = "(?s).*\\? $";
			response[1] = "\07"; 
			msg = telnet.waitfor(response);
		
		// 	No more next page.
			if (!msg.endsWith(response[1])) {
				telnet.send_wo_r("y");
				telnet.waitfor(expect(BOARD));
				result = true;
			}

			debugMessage ("DeleteThread finish.", INFO);
		}
		if (command == THREAD_INVERSE){
			telnet.send_wo_r("i");
		 
			String [] response1 = new String[3];
			response1[0] = "(?s).*>$"; // Already last page.
			response1[1] = "(?s).*;2H$"; // Page change && cursor was not on top.
			response1[2] = "\007"; // No thread
			
			msg = telnet.waitfor(response1);
			if (msg.equals("\007"))
				result = false;
			else
				result = true;
			debugMessage ("Inverse finish.", INFO);
		}
		if (command == THREAD_MARK){
			telnet.send_wo_r("m");
		 
			String [] response1 = new String[3];
			response1[0] = "(?s).*>$"; // Already last page.
			response1[1] = "(?s).*;2H$"; // Page change && cursor was not on top.
			response1[2] = "\007"; // No thread
			
			msg = telnet.waitfor(response1);
			if (msg.equals("\007"))
				result = false;
			else
				result = true;
			debugMessage ("Inverse finish.", INFO);
		}
	
		//TODO Move cursor to the last thread. It's better safe than sorry...
		telnet.send_wo_r("$"); // in case of having been visited.

		response[0] = "(?s).*>$"; // Already last page.
		response[1] = "(?s).*;2H$"; // Page change && cursor was not on top.

		msg = telnet.waitfor(response);

		//debugMessage("DeleteThread successfully.", INFO);
		if (gotoMenu(MAIN))
			debugMessage("Go back to main successfully", WARN);

		return result;
	}
	
	public void sendHeartBeat() throws IOException {
		if (state == MAIN){
			telnet.send_wo_r("p");
			telnet.waitfor("\007");
		}
	}
	
	
	public native int[] hangulCutter(String jstr);
	public native int[] hangulAscii(String jstr);
	public native int[] hangulDebug(String jstr);
	public native int[] hangulDebug2(String jstr);
	public native int hangulCutMailSender(String jstr);
	public static native int hangulLength(String jstr);
	static {
		System.loadLibrary("hangul_cutter");
	}
}