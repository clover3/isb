package com.postech.isb.readThread;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.postech.isb.PostechIsb;
import com.postech.isb.R;
import com.postech.isb.R.id;
import com.postech.isb.R.layout;
import com.postech.isb.boardList.Board;
import com.postech.isb.boardList.BoardList;
import com.postech.isb.boardList.IsbDBAdapter;
import com.postech.isb.compose.NotePad.Notes;
import com.postech.isb.util.IsbSession;
import com.postech.isb.util.IsbThread;
import com.postech.isb.util.ThreadList;
import com.postech.isb.readBoard.ReadBoards;

import android.R.string;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AnimationUtils;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EdgeEffect;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.widget.Toast;
import android.widget.TextView.BufferType;
import android.text.TextWatcher;
import android.text.Editable;

public class ReadThread extends Activity {
	private IsbSession isb;
	private String board;
	private int num;

	private TextView boardName;
	private Button prev;
	private Button next;
	private TextView threadHead;
	private TextView threadBody;
	private TableLayout comments;

	private TextView commentMessage;
	private Button leaveCommentBtn;
	private MyScrollView readThreadScroll;

	private LinearLayout linearLayout;
	private LinearLayout linearLayoutInner;

	static final private int WRITE = Menu.FIRST;
	static final private int DELETE = Menu.FIRST + 1;

	/**
	 * Standard projection for the interesting columns of a normal note.
	 */
	private static final String[] PROJECTION = new String[] { Notes._ID, // 0
			Notes.TITLE, // 1
			Notes.NOTE, // 2
			Notes.TARGET_BOARD, // 3
	};
	/** The index of the note column */
	private static final int COLUMN_INDEX_TITLE = 1;
	private static final int COLUMN_INDEX_NOTE = 2;
	private static final int COLUMN_INDEX_TARGET_BOARD = 3;

	static final private int NewNote = 1;

	private Intent gotoAnotherBoard;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		Intent intent = getIntent();
		board = intent.getStringExtra("board");
		num = intent.getIntExtra("num", -1);

		isb = ((PostechIsb) getApplicationContext()).isb;

		if (board == null) {
			Toast.makeText(getApplicationContext(), "Board is missing",
					Toast.LENGTH_SHORT).show();
			retResultNothing();
			finish();
		}

		if (num == -1) {
			Toast.makeText(getApplicationContext(), "Thread number is missing",
					Toast.LENGTH_SHORT).show();
			retResultNothing();
			finish();
		}

		setContentView(R.layout.read_thread);

		boardName = (TextView) findViewById(R.id.curBoardinReadThead);
		threadHead = (TextView) findViewById(R.id.threadHead);
		threadBody = (TextView) findViewById(R.id.threadBody);
		comments = (TableLayout) findViewById(R.id.threadComment);

		commentMessage = (TextView) findViewById(R.id.commentText);
		leaveCommentBtn = (Button) findViewById(R.id.commentBtn);

		prev = (Button) findViewById(R.id.prevThread);
		next = (Button) findViewById(R.id.nextThread);

		readThreadScroll = (MyScrollView) findViewById(R.id.readThreadScroll);

		boardName.setText(board);

		gotoAnotherBoard = new Intent(this, BoardList.class);
		gotoAnotherBoard.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		boardName.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivity(gotoAnotherBoard);
				retResultNothing();
				finish();
			}
		});

		prev.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateThread(num - 1);
			}
		});

		next.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				updateThread(num + 1);
			}
		});

		leaveCommentBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String message = commentMessage.getText().toString();

				if (message.length() > 0) {
					try {
						if (isb.isMain()) {
							leaveCommentBtn.setVisibility(ImageButton.GONE);
							if (isb.leaveComment(board, num, message)) {
								updateThread(num);
								commentMessage.setText("");
							} else
								Toast.makeText(getApplicationContext(),
										"Unexpected error.T.T",
										Toast.LENGTH_SHORT).show();
							leaveCommentBtn.setVisibility(ImageButton.VISIBLE);

						} else {
							Toast.makeText(getApplicationContext(),
									"Login first plz...", Toast.LENGTH_SHORT)
									.show();
						}
					} catch (IOException e) {
						Toast.makeText(getApplicationContext(),
								"Connection lost!", Toast.LENGTH_SHORT).show();
					}
				}
			}
		});

		SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
		linearLayoutInner = (LinearLayout) findViewById(R.id.linearLayoutInsideScroll);
		if (SP.getBoolean("swipe_thread", true)) {
			linearLayout.setOnTouchListener(MyTouchListener);
			readThreadScroll.setOnTouchListener(MyTouchListener);
			linearLayoutInner.setOnTouchListener(MyTouchListener);
			threadBody.setOnTouchListener(MyTouchListener);
			comments.setOnTouchListener(MyTouchListener);
		}
		commentMessage.addTextChangedListener(commentWatcherInput);

		registerForContextMenu(boardName);
		updateThread(num);
	} // End of onCreate

	public class ClickableSpanLink extends ClickableSpan {
		String m_str;

		public ClickableSpanLink(String _str) {
			m_str = _str;
		}

		@Override
		public void onClick(View view) {
			boolean fValidFormat = true;
			try {
				//Log.i("debug", "onClick : " + m_str);
				String strNum = m_str.substring(m_str.indexOf('/') + 3);
				int _num = Integer.parseInt(strNum.trim());
				num = _num;
			} catch (NumberFormatException e) {
				fValidFormat = false;
			}
			int loc = m_str.indexOf('/');

			String strShortBoard = Character.toString(m_str.charAt(loc - 1))
					+ m_str.charAt(loc + 1);
			board = getBoardNameByInitial(strShortBoard);
			if (board.length() <= 0)
				fValidFormat = false;

			if (fValidFormat)
				updateThread(num);
		}

		public String getBoardNameByInitial(String initial) {
			String boardname = "";
			ArrayList<Board> boardList;
			try {
				boardList = isb.getBoardList();
				Iterator<Board> iter = boardList.iterator();
				while (iter.hasNext()) {
					Board board = iter.next();
					String name = board.name;
					if (name.charAt(0) != initial.charAt(0))
						continue;
					int idx2 = name.indexOf('/') + 1;
					if (name.charAt(idx2) == initial.charAt(1)) {
						boardname = name;
						break;
					}

				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return boardname;
		}
	}

	private void addRefLink(TextView textview, String str) {
		String strThreadBody = str;
		// Find all highlight lines in advance
		List<Integer> highlightStartList = new ArrayList<Integer>();
		List<Integer> highlightEndList = new ArrayList<Integer>();
		int deletedBytes = 0;

		Pattern highlightPattern = Pattern
				.compile("^!.*$", Pattern.MULTILINE);
		Matcher match = highlightPattern.matcher(strThreadBody);
		while (match.find()) { // Find each match in turn; String can't do this.
			highlightStartList.add(match.start() - deletedBytes);
			deletedBytes += 1;
			highlightEndList.add(match.end() - deletedBytes);
		}
		// Delete '!' for highlights
		StringBuilder sb = new StringBuilder(strThreadBody);
		ListIterator<Integer> li_s = highlightStartList.listIterator();
		// Iterate in reverse.
		while(li_s.hasNext()) {
			sb.deleteCharAt(li_s.next());
		}
		strThreadBody = sb.toString();

		SpannableString text = new SpannableString(strThreadBody);
		textview.setText(text);


		// Pattern pattern =
		// Pattern.compile("\\s[a-zA-z]/[a-zA-z]\\s\\d{1,}\\s");
		// Set thread link
		// FIXME: cannot parse hangul link...
		Pattern linkPattern = Pattern
				.compile("(\\s|^)[a-zA-z0-9]/[a-zA-z0-9]\\s\\d{1,}(\\s|$)");
		match = linkPattern.matcher(strThreadBody);
		while (match.find()) { // Find each match in turn; String can't do this.
			ClickableSpan clickableSpan = new ClickableSpanLink(match.group());
			text.setSpan(clickableSpan, match.start(), match.end(), 0);
		}
		textview.setMovementMethod(LinkMovementMethod.getInstance());

		// Set highlight
		li_s = highlightStartList.listIterator();
		ListIterator<Integer> li_e = highlightEndList.listIterator();
		// Iterate in reverse.
		while(li_s.hasNext()) {
			int idx_s = li_s.next();
			int idx_e = li_e.next();
			text.setSpan(new ForegroundColorSpan(Color.BLACK), idx_s, idx_e, 0);
			text.setSpan(new BackgroundColorSpan(Color.WHITE), idx_s, idx_e, 0);
		}

		textview.setText(text, BufferType.SPANNABLE);

	}

	private boolean isUrl(String str) {
		boolean f = true;
		try {
			URL url = new URL(str);
		} catch (MalformedURLException e) {
			f = false;// it wasn't a URL
		}
		return f;
	}

	private String preprocessLink(String str) {
		StringBuffer result = new StringBuffer();
		String regUrl = "https?://([\\w-]+\\.)+[\\w-]+(/[\\w-./?&%=]*)?";
		String regUrlTail = "[a-zA-Z0-9\\-\\._\\?\\,\\'/\\\\+&amp;%\\$#\\=~]*";
		String delimiter = "\n";
		String[] astr = str.split(delimiter);

		boolean fReadingUrl = false;
		boolean fAddLF = true;
		for (int i = 0; i < astr.length; i++) {
			// Log.i("clover", String.format("%d : ", i) + astr[i] );
			if (isUrl(astr[i]) && astr[i].length() >= 79)
				fReadingUrl = true;
			else if (fReadingUrl && astr[i].matches(regUrlTail)) {
				fAddLF = false;
				if (astr[i].length() < 79)
					fReadingUrl = false;
			} else
				fReadingUrl = false;

			// Log.i("clover", String.format("fReadingUrl= %b , fAddLF=%b ",
			// fReadingUrl, fAddLF) );

			if (fAddLF)
				result.append(delimiter);
			result.append(astr[i]);
			if (!fReadingUrl)
				fAddLF = true;
		}
		result.append(delimiter);

		return result.toString();
	}

	static String TrimLine(String str) {
		// TODO: Optimize me
		int lastNewline = -1;
		int i;
		for (i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '\n')
				lastNewline = i;
			else if (str.charAt(i) != '\n' && str.charAt(i) != ' '
					&& str.charAt(i) != '\f' && str.charAt(i) != '\r'
					&& str.charAt(i) != '\b' && str.charAt(i) != '\t')
				break;
		}
		if (lastNewline >= 0 && i < str.length() && lastNewline < str.length())
			str = str.substring(lastNewline + 1);

		int lastChar = -1;
		for (i = 0; i < str.length(); i++) {
			if (str.charAt(i) != '\n' && str.charAt(i) != ' '
					&& str.charAt(i) != '\f' && str.charAt(i) != '\r'
					&& str.charAt(i) != '\b' && str.charAt(i) != '\t')
				lastChar = i;
		}
		if (lastChar >= 0 && lastChar < str.length()) {
			str = str.substring(0, lastChar + 1);
		}
		return str;
	}

	private class CommentList
	{
		public class Comment {
			public String strWriter;
			public String strWhen;
			public String strComment;

			Comment(String _strWriter, String _strComment, String _strWhen) {
				strWriter = _strWriter;
				strWhen = _strWhen;
				strComment = _strComment;
			}
		};
		boolean fPending;
		public ArrayList<Comment> arrComment;
		public CommentList()
		{
			arrComment = new ArrayList<Comment>();
			fPending = false;
		}
		
		void Add(String strWriter, String strComment, String strWhen)
		{
		//	Log.i("isb", "CommentList::Add() -" + strWriter + ":" + strComment);
			if( fPending )
			{
				int idxLast = arrComment.size()-1;
				if( strWriter.equals(arrComment.get(idxLast).strWriter) )
				{
		//			Log.i("isb", "CommentList::Add() Match. Concat comment");
					arrComment.get(idxLast).strComment = arrComment.get(idxLast).strComment + strComment;
				}
				else
				{
					// Log.i("isb", "CommentList::Add() No match add new line");
					arrComment.add(new Comment(strWriter, strComment, strWhen));
				}
			} 
			else
				arrComment.add(new Comment(strWriter, strComment, strWhen) );

			int[] ret = hangulCountLines(strComment);
			//Log.i("isb", "line=" + ret[0] + " / byte=" + ret[1] );
			if( ret[0] > 0 || ret[1] >= 48 )
				fPending = true;
			else
				fPending = false;
		}
	};

	private void _addCommentLine(CommentList.Comment PComment)
	{
		_addCommentLine(PComment.strWriter, PComment.strComment, PComment.strWhen);
	}
	private void _addCommentLine(String strWriter, String strComment, String strWhen)
	{
		TextView writer, comment, when;
		writer = new TextView(this);
		writer.setText(strWriter);
		writer.setPadding(0, 0, 5, 0);
		writer.setTextColor(0xFFCCCCFF);
		writer.setTextSize(15);

		comment = new TextView(this);

		addRefLink(comment, strComment);
		comment.setTextColor(0xFFFFFFFF);
		comment.setTextSize(15);

		Linkify.addLinks(comment, Linkify.WEB_URLS);

		TableRow row;
		row = new TableRow(this);
		row.setLayoutParams(new LayoutParams(
				LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT));

		row.addView(writer);
		when = new TextView(this);
		when.setPadding(0, 0, 5, 0);
		SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		if (SP.getBoolean("display_comment_time", false)) {
			when.setText(strWhen);
		}
		row.addView(when);
		row.addView(comment);

		comments.addView(row);
	}

	private void _CommitCommentList(CommentList _CommentList)
	{
		int nSize = _CommentList.arrComment.size();
		for(int i=0; i < nSize; i++)
		{
			_addCommentLine(_CommentList.arrComment.get(i));
		}
	}

	private String byteArrayToHex(byte[] a) {
		// Just for debugging
		StringBuilder sb = new StringBuilder();
		for(final byte b: a)
			sb.append(String.format("%02x ", b&0xff));
		return sb.toString();
	}

	private boolean updateThread(int _num) {
		try {
			if (isb.isMain()) {
				boolean readNext = (_num >= num);
				IsbThread t = isb.readThread(board, _num);
				CommentList _CommentList = new CommentList();
				if (t != null) {
					readThreadScroll.fullScroll(View.FOCUS_UP);
					boardName.setText(board + " (" + _num + ")");
					// t.cc has \n itself.
					threadHead.setText(t.writer + "\n" + t.date + "\n" + t.title + t.cc);
					//Log.i("debug", "write : " + t.writer);

					// link ref maker
					// XXX: I cannot remember why I TrimLine the contents....
					String strContent = preprocessLink(TrimLine(t.contents));
					addRefLink(threadBody, strContent);

					Linkify.addLinks(threadBody, Linkify.WEB_URLS);

					// t.comments
					Scanner s = new Scanner(t.comments);
					s.useDelimiter("\n");

					//Log.i("debug", "ReadThread : " + t.comments);

					Pattern p = Pattern
							.compile("^\\s*(\\w+)\\((\\d+,\\d+:\\d+)\\):\"(.*)\"\n?$");

					comments.removeAllViews();

					while (s.hasNext()) {
						String token = s.next();
						Matcher m = p.matcher(token);

						if (m.matches()) {
							String strWriter = m.replaceAll("$1");
							String strWhen = m.replaceAll("$2");
							String strComment = m.replaceAll("$3");
							_CommentList.Add(strWriter, strComment, strWhen);

						} else
							Log.i("debug", "ReadThread unmatch : " + token);
					}

					_CommitCommentList(_CommentList);
					num = _num;
					prev.setVisibility(num == 1 ? View.INVISIBLE : View.VISIBLE);
					next.setVisibility(View.VISIBLE);
					readThreadScroll.smoothScrollTo(0, 0);

					return true;
				} else {
					Toast.makeText(getApplicationContext(), "No more thread!",
							Toast.LENGTH_SHORT).show();
					// next.setVisibility(View.INVISIBLE);
					retResultNothing();
					finish();
				}
			} else {
				Toast.makeText(getApplicationContext(), "Login first plz...",
						Toast.LENGTH_SHORT).show();
			}
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), "Connection lost!",
					Toast.LENGTH_SHORT).show();
		}

		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		menu.add(0, WRITE, Menu.NONE, R.string.write).setShortcut('3', 'w')
				.setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, DELETE, Menu.NONE, R.string.delete).setShortcut('4', 'd')
				.setIcon(android.R.drawable.ic_menu_delete)
				.setEnabled(!SP.getBoolean("disable_delete", false));

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case WRITE: {
			Intent giveMeNewThread = new Intent(Intent.ACTION_INSERT,
					Notes.CONTENT_URI);
			giveMeNewThread.putExtra("board", board);
			startActivityForResult(giveMeNewThread, NewNote);
			return true;
		}
		case DELETE: {
			if (isb.isMain()) {
//				try {
					AlertDialog.Builder alert_confirm = new AlertDialog.Builder(ReadThread.this);
					alert_confirm.setMessage(R.string.delete_confirm).setCancelable(false).setPositiveButton(R.string.delete,
					new DialogInterface.OnClickListener() {
					    @Override
					    public void onClick(DialogInterface dialog, int which) {
					        // 'YES'
					    	try{
								if (isb.modifyThread(board, num, isb.THREAD_DELETE)) {
									Toast.makeText(getApplicationContext(),
											"Delete success", Toast.LENGTH_SHORT).show();
									retResultNothing();
									finish();
								} else {
									Toast.makeText(getApplicationContext(), "Delete fail.",
											Toast.LENGTH_SHORT).show();
								}					}
							catch (IOException e) {
								e.printStackTrace();
								Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
								isb.disconnect();
							}
					    }
					}).setNegativeButton(R.string.cancle,
					new DialogInterface.OnClickListener() {
					    @Override
					    public void onClick(DialogInterface dialog, int which) {
					        // 'No'
					    return;
					    }
					});
					AlertDialog alert = alert_confirm.create();
					alert.show();

			} else
				Toast.makeText(getApplicationContext(), "Login first plz...",
						Toast.LENGTH_SHORT).show();

			return true;
		}
		}

		return false;
	}

	@Override
	public void onBackPressed() {
		Intent intent = new Intent();
		intent.putExtra("idx", num);
		setResult(RESULT_OK, intent);
		super.onBackPressed();
	}

	// TODO: Can I be removed?
	private void retResultNothing() {
		Intent intent = new Intent();
		intent.putExtra("idx", ReadBoards.lastReadNothing);
		setResult(RESULT_OK, intent);
	}

	private int m_nPreTouchPosY = 0;
	private int m_nPreTouchPosX = 0;
	private boolean on_drag = false;
	private boolean over_line = false;
	View.OnTouchListener MyTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			//Log.i("newm", "v.getClass(): "+v.getClass());
			if ((event.getAction() == MotionEvent.ACTION_MOVE && !on_drag) ||
					event.getAction() == MotionEvent.ACTION_DOWN) {
				on_drag = true;
				m_nPreTouchPosX = (int) event.getX();
				m_nPreTouchPosY = (int) event.getY();
			}
			else if( event.getAction() == MotionEvent.ACTION_MOVE && on_drag)
			{
				int nTouchPosX = (int) event.getX();
				int nTouchPosY = (int) event.getY();
				int result = GetTouchResult(nTouchPosX, nTouchPosY);
				if( result == + 1 )
				{
					readThreadScroll.SetEffect(+1);
					readThreadScroll.postInvalidate();
					over_line = true;
				}	
				else if ( result == -1 )
				{
					readThreadScroll.SetEffect(-1);
					readThreadScroll.postInvalidate();
					over_line = true;
				}
				
				if( result == 0 && over_line)
				{
					over_line = false;
					readThreadScroll.SetEffect(0);
					readThreadScroll.postInvalidate();
					
				}
			}
			else if (event.getAction() == MotionEvent.ACTION_UP && on_drag) {
				int nTouchPosX = (int) event.getX();
				int nTouchPosY = (int) event.getY();
				on_drag = false;
				
				readThreadScroll.SetEffect(0);
				over_line = false;
				int result = GetTouchResult(nTouchPosX, nTouchPosY);

				if( result == + 1 )
					updateThread(num + 1);		
				else if ( result == -1 )
					updateThread(num - 1);		
				
				m_nPreTouchPosX = nTouchPosX;
				m_nPreTouchPosY = nTouchPosY;
			}
			else{
				//Log.i("newm", "event.getAction(): "+event.getAction());
			}
			return false;
		}
	};
	
	
	int GetTouchResult(int nTouchPosX, int nTouchPosY)
	{
		int result = 0;
		
		Display display = getWindowManager().getDefaultDisplay();
		int width = display.getWidth();
		
		if( Math.abs(nTouchPosX - m_nPreTouchPosX) < 4 
		 && Math.abs(nTouchPosY - m_nPreTouchPosY) < 4  )
		{
			// Do Nothing 
		}
		else if(commentMessage.getText().length() == 0)
		{
			int gap = width / 5;
			
			if ( nTouchPosX - m_nPreTouchPosX < -gap && m_nPreTouchPosX > width * (3./4) ) 
			{
				result = +1;				
			} 
			else if (nTouchPosX - m_nPreTouchPosX > gap && m_nPreTouchPosX < width / 4. ) 
			{
				result = -1;
			}
		}
		return result;
	}
	
	@Override
	public void onActivityResult(int reqCode, int resCode, Intent data) {
		super.onActivityResult(reqCode, resCode, data);

		switch (reqCode) {
		case (NewNote): {
			if (resCode == Activity.RESULT_OK) {
				String result = data.getAction();
				Uri resultUri = Uri.parse(result);

				Cursor cursor = managedQuery(resultUri, PROJECTION, null, null,
						Notes.DEFAULT_SORT_ORDER);

				if (cursor.moveToFirst()) {
					String title = cursor.getString(COLUMN_INDEX_TITLE);
					String content = cursor.getString(COLUMN_INDEX_NOTE);
					String targetBoard = cursor
							.getString(COLUMN_INDEX_TARGET_BOARD);

					if (isb.isMain()) {
						try {
							if (isb.writeToBoard(targetBoard, title, content)) {
								Toast.makeText(getApplicationContext(),
										"Write success", Toast.LENGTH_SHORT)
										.show();
								ContentResolver cr = getContentResolver();
								if (cr.delete(resultUri, null, null) > 0) {
									Log.i("debug", "Delete success");
								}
							} else {
								Toast.makeText(getApplicationContext(),
										"Fail! Invalid board?",
										Toast.LENGTH_SHORT).show();
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							Toast.makeText(getApplicationContext(),
									"Connection lost", Toast.LENGTH_SHORT)
									.show();
							isb.disconnect();
						}
					} else
						Toast.makeText(getApplicationContext(),
								"Login first plz...", Toast.LENGTH_SHORT)
								.show();
				}
			}
			retResultNothing();
			finish();
			break;
		}
		}
	}
	TextWatcher commentWatcherInput = new TextWatcher() {
		private boolean overflowed = false;
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			// TODO Auto-generated method stub
			int[] ret=hangulCountLines(s.toString());
			if ((ret[0] > 0 && ret[1] > 0) || ret[0] > 1){
				leaveCommentBtn.setText("Leave comment. "+ret[0]+"lines + "+ret[1]+"bytes");
				if (!overflowed){
					readThreadScroll.fullScroll(View.FOCUS_DOWN);
					overflowed = true;
				}
			}
			else
				leaveCommentBtn.setText("Leave comment");
		}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {			
		}
		@Override
		public void afterTextChanged(Editable s) {	
		}

	};	
	public native int[] hangulCountLines(String jstr);
	static {
		System.loadLibrary("hangul_cutter");
}

}
