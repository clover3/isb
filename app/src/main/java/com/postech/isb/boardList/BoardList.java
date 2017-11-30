package com.postech.isb.boardList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.postech.isb.PostechIsb;
import com.postech.isb.R;
import com.postech.isb.R.drawable;
import com.postech.isb.R.id;
import com.postech.isb.R.layout;
import com.postech.isb.R.string;
import com.postech.isb.readBoard.ReadBoards;
import com.postech.isb.util.IsbSession;
import com.postech.isb.util.MenuOption;
import com.postech.isb.util.TouchMenuManager;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

public class BoardList extends ListActivity {

	static final private int REFRESH = Menu.FIRST;
	static final private int FAVORITE = Menu.FIRST + 1;
	static final private int FAVORITE_ONLY = Menu.FIRST + 2;
	static final private int SEARCH_NEW = Menu.FIRST + 3;
	static final private int MY_BOARD = Menu.FIRST + 4;

	private IsbSession isb;
	private ArrayList<Board> boardItems;
	private BoardAdapter boardAdapter;
	private EditText search;
	private IsbDBAdapter boardDBAdapter;
	private TextWatcher textWatcher;
	private ProgressDialog pd;
	private boolean isEmpty = true;
	private boolean fExistMyboard = false;
	private String myBoardGroup;
	private String myBoardName; 
	private TouchMenuManager menuMan;


	private static final String FAVORITE_ONLY_KEY = "FAVORITE_ONLY_KEY";
	private static final String SAVE_MY_BOARD = "SAVE_MY_BOARD";
	private static final String SAVED_MYBOARD_GROUP = "SAVED_MYBOARD_GROUP";
	private static final String SAVED_MYBOARD_NAME = "SAVED_MYBOARD_NAME";
	private static final String PREFERENCE_COPIED_1 = "PREFERENCE_COPIED_1";

	private boolean favoriteOnly = false;
	private ArrayList<Board> board_items;

	public static String getMyBoard(SharedPreferences settings) {
		boolean fExistMyboard = settings.getBoolean(SAVE_MY_BOARD, false);
		if( fExistMyboard )
		{
			return settings.getString(SAVED_MYBOARD_NAME, "");
		}
		else {
			return "";
		}
	}
	public static void setMyboard(SharedPreferences settings, String name)
	{
		SharedPreferences.Editor editor = settings.edit();

		editor.putBoolean(SAVE_MY_BOARD, true);
		editor.putString(SAVED_MYBOARD_NAME, name);
		editor.commit();
	}
	// Handler for thread. Show error messages.
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.i("BoardList", "Msg : " + msg.what);
			switch (msg.what) {
			case 0:
				updateArray();
				pd.dismiss();
				break;
			case 1:
				Toast.makeText(BoardList.this, "Connection Lost....",
						Toast.LENGTH_SHORT).show();
				pd.dismiss();
				break;
			case 2:
				pd.setMessage("Insert to Database...");
				break;
			case 3: {
				UpdateBoardAdapter(board_items);
			}
				pd.dismiss();
				break;
			}
		}
	};
	
	void UpdateBoardAdapter(ArrayList<Board> list)
	{
		boardAdapter.clear();
		
		if( fExistMyboard )
		{
			for( Board board : list )
			{
				if( board.name.equalsIgnoreCase(myBoardName) )
					boardAdapter.add(board);
			}
			for( Board board : list )
			{
				if( !board.name.equalsIgnoreCase(myBoardName) )
					boardAdapter.add(board);
			}
		}
		else
		{
			for( Board board : list )
			{
				boardAdapter.add(board);
			}
		}
		
		boardAdapter.notifyDataSetChanged();
		if (isEmpty == true)
			isEmpty = boardAdapter.isEmpty();
		boardAdapter.getFilter().filter(search.getText().toString());
		
	}

	// Just for backward compatibility
	// Old versions used 'getPreferences(MODE_PRIVATE)', so I need to copy them
	// into new SharedPreferences
	void copySharedPreferences()
	{
		SharedPreferences newSettings = PreferenceManager.getDefaultSharedPreferences(this);
		if (newSettings.getBoolean(PREFERENCE_COPIED_1, false))
			// Already copied
			return;
		else {
			// Start to copy
			SharedPreferences oldSettings = getPreferences(MODE_PRIVATE);
			SharedPreferences.Editor editor = newSettings.edit();
			editor.putBoolean(PREFERENCE_COPIED_1, true);

			editor.putBoolean(FAVORITE_ONLY_KEY, oldSettings.getBoolean(FAVORITE_ONLY_KEY, false));
			editor.putBoolean(SAVE_MY_BOARD, oldSettings.getBoolean(SAVE_MY_BOARD, false));
			editor.putString(SAVED_MYBOARD_NAME,  oldSettings.getString(SAVED_MYBOARD_NAME, ""));
			editor.commit();
		}
	}

	/*
	 * static class theLock extends Object { } static public theLock lockObject
	 * = new theLock();
	 */

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		MenuOption.setUseActionBar(this);
		MenuOption.setTitleBar(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.board_list);

		isb = ((PostechIsb) getApplicationContext()).isb;

		boardItems = new ArrayList<Board>();
		boardAdapter = new BoardAdapter(this, R.layout.boardlist_item,
				boardItems);

		setListAdapter(boardAdapter); 
		registerForContextMenu(getListView());


		menuMan = new TouchMenuManager(this);
		ListView lv = (ListView) findViewById(android.R.id.list);
		lv.setOnTouchListener(menuMan.MyTouchListener);
		
		search = (EditText) findViewById(R.id.searchBoard);
		textWatcher = new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				boardAdapter.getFilter().filter(s);
			}
		};
		search.addTextChangedListener(textWatcher);

		restoreUIState();

		// DB
		copySharedPreferences();
		loadPreference();
		boardDBAdapter = new IsbDBAdapter(this);
		boardDBAdapter.open();
		populateBoardList();
	}

	private void restoreUIState() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		favoriteOnly = settings.getBoolean(FAVORITE_ONLY_KEY, false);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (isEmpty) {
			AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
			alt_bld.setMessage("Do you want to update now ?")
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									updateBoard();
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alert = alt_bld.create();
			alert.setTitle("List is empty");
			alert.setIcon(R.drawable.icon);
			alert.show();
		}
	}

	@Override
	protected void onPause() {
		// FIXME:Am i needed?
		super.onPause();
		SharedPreferences uiState = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = uiState.edit();
		editor.putBoolean(FAVORITE_ONLY_KEY, favoriteOnly);
		editor.commit();
	}

	private Cursor boardListCursor;

	private void populateBoardList() {
		boardListCursor = boardDBAdapter.getAllBoardCursor();
		startManagingCursor(boardListCursor);

		updateArray();
	}

	private void SearchNew() {
		if (isb.isMain()) {
			boardListCursor.requery();
			pd = ProgressDialog.show(BoardList.this, "Searching...",
					"Getting data from Isb...", true, false);
			new Thread(new Runnable() {
				@Override
				public void run() {
					//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
					String name;
					Board newItem;
					board_items = new ArrayList<Board>();
					try {
						if (boardListCursor.moveToFirst()) {
							do {
								if (boardListCursor
										.getInt(IsbDBAdapter.FAVORITE_COLUMN) == 1) {
									int result;
									name = boardListCursor
											.getString(IsbDBAdapter.BOARD_COLUMN);
									result = isb.searchNewPost(name);
									newItem = CreateBoard(name, true);
									newItem.setNewPost(result);
									board_items.add(newItem);

								}
							} while (boardListCursor.moveToNext());
						}
					} catch (IOException e) {
						handler.sendEmptyMessage(1);
						isb.disconnect();
					}
					handler.sendEmptyMessage(3);
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
				}
			}).start(); // 스레드 실행

		} else {
			Toast.makeText(BoardList.this, "Login first plz.",
					Toast.LENGTH_SHORT).show();
		}
	}

	Board CreateBoard(String name, boolean favorite)
	{
		Board newItem;
		if( isMyBoard(name))
		{
			newItem = new Board(name, true, true);
		}
		else
			newItem = new Board(name, favorite, false);
		return newItem;
	}

	
	private void updateArray() {
		boardListCursor.requery();
		boardAdapter.clear();

		String name;
		boolean favorite;
		Board newItem;
		ArrayList<Board> list = new ArrayList<Board>();
		if (boardListCursor.moveToFirst())
		{
			do {
				name = boardListCursor.getString(IsbDBAdapter.BOARD_COLUMN);
				favorite = boardListCursor.getInt(IsbDBAdapter.FAVORITE_COLUMN) == 1;
				boolean fAddItem = false;
				
				if( favoriteOnly ){
					if( favorite ){
						fAddItem  = true;
					}
				}
				else
					fAddItem = true;
				
				if ( fAddItem ) {
					newItem = CreateBoard(name, favorite);
					list.add(newItem);
				}
			} while (boardListCursor.moveToNext());
		}
		
		UpdateBoardAdapter(list);
	}
	
	boolean isMyBoard(String name)
	{
		boolean f = false;
		if( fExistMyboard )
		{
			f = myBoardName.equalsIgnoreCase(name);
		}
		
		return f;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		search.removeTextChangedListener(textWatcher);
		boardDBAdapter.close();
	}

	@Override
	protected void onListItemClick(ListView l, View view, int position, long id) {

		String boardName = ((TextView) view.findViewById(R.id.BoardListTitle))
				.getText().toString();
		String action = getIntent().getAction();

		if (Intent.ACTION_PICK.equals(action)
				|| Intent.ACTION_GET_CONTENT.equals(action)) {
			// The caller is waiting for us to return a note selected by
			// the user. The have clicked on one, so return it now.
			setResult(RESULT_OK, new Intent().putExtra("board", boardName));
			finish();
		} else {
			Intent readBoard = new Intent(BoardList.this, ReadBoards.class);
			readBoard.putExtra("board", boardName);
			startActivity(readBoard);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (MenuOption.useActionBar) {
			getMenuInflater().inflate(R.menu.board_list, menu);
			return true;
		}
		else {
			menu.add(0, REFRESH, Menu.NONE, R.string.refresh).setIcon(
					R.drawable.ic_menu_refresh);
			menu.add(0, FAVORITE_ONLY, Menu.NONE, R.string.list_favorite_only)
					.setIcon(R.drawable.list_favorite_only);
			menu.add(0, SEARCH_NEW, Menu.NONE, R.string.list_new).setIcon(
					R.drawable.list_new);
			return true;
		}
	}

	private void setFavoriteIcon(MenuItem item) {
			item.setTitle(favoriteOnly ? R.string.list_all
					: R.string.list_favorite_only);
			item.setIcon(favoriteOnly ? R.drawable.list_all
					: R.drawable.list_favorite_only);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem item;
		if (MenuOption.useActionBar)
			item = menu.findItem(R.id.favorite);
		else
			item = menu.findItem(FAVORITE_ONLY);
		setFavoriteIcon(item);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		if (MenuOption.useActionBar) {
			switch (item.getItemId()) {
				case R.id.refresh: {
					updateBoard();
					return true;
				}
				case R.id.favorite: {
					favoriteOnly = !favoriteOnly;
					updateArray();
					setFavoriteIcon(item);
					return true;
				}
				case R.id.search_new: {
					favoriteOnly = true;
					SearchNew();
					return true;
				}
			}
		}
		else {
			switch (item.getItemId()) {
				case REFRESH: {
					updateBoard();
					return true;
				}
				case FAVORITE_ONLY: {
					favoriteOnly = !favoriteOnly;
					updateArray();
					return true;
				}
				case SEARCH_NEW: {
					favoriteOnly = true;
					SearchNew();
					return true;
				}
			}
		}

		return false;
	}

	private void updateBoard() {
		if (isb.isMain()) {
			pd = ProgressDialog.show(BoardList.this, "Refresh Board list...",
					"Getting data from Isb...", true, false);
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						ArrayList<Board> tmpList = isb.getBoardList();
						handler.sendEmptyMessage(2);
						boardDBAdapter.updateBoard(tmpList);
					} catch (IOException e) {
						handler.sendEmptyMessage(1);
						isb.disconnect();
					}
					handler.sendEmptyMessage(0);
				}
			}).start(); // 스레드 실행
		} else {
			Toast.makeText(BoardList.this, "Login first plz.",
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		menu.setHeaderTitle("Selected Board");
		menu.add(0, FAVORITE, Menu.NONE, R.string.favorite);
		menu.add(0, MY_BOARD, Menu.NONE, R.string.myboard);

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);

		switch (item.getItemId()) {
		case (FAVORITE): {
			AdapterView.AdapterContextMenuInfo menuInfo;
			menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			int index = menuInfo.position;
			Board b = boardAdapter.getItem(index);
			boardDBAdapter.setFavoriteByName(b.name, !b.favorite);
			boardAdapter.toggleFavorite(index);
			return true;
		}
		case (MY_BOARD): {
			AdapterView.AdapterContextMenuInfo menuInfo;
			menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			int index = menuInfo.position;
			Board b = boardAdapter.getItem(index);
			if( fExistMyboard && b.myBoard )
			{
				SetAsMyboard(b.name, false);
			}
			else
			{
				if( !b.favorite )
				{
					boardDBAdapter.setFavoriteByName(b.name, !b.favorite);
					boardAdapter.toggleFavorite(index);
				}
				SetAsMyboard(b.name, true);
			}
			boardAdapter.toggleMyboard(index);
			loadPreference();
		}
		}
		return false;
	}
	
	// f == true -> Set 
	// f == false -> Reset
	private void SetAsMyboard(String name, boolean f)
	{
		SharedPreferences uiState = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = uiState.edit();

		editor.putBoolean(SAVE_MY_BOARD, f);
		editor.putString(SAVED_MYBOARD_NAME, name);
		editor.commit();
	}

	public String getBoardNameByInitial(String initial) {
		String boardname = "";
		try {
			if (boardListCursor.moveToFirst()) {
				do {
					if (boardListCursor.getInt(IsbDBAdapter.FAVORITE_COLUMN) == 1) {
						String name = boardListCursor
								.getString(IsbDBAdapter.BOARD_COLUMN);
						if (name.charAt(0) != initial.charAt(0))
							continue;
						int idx2 = name.indexOf('/') + 1;
						if (name.charAt(idx2) == initial.charAt(2)) {
							boardname = name;
							break;
						}

					}
				} while (boardListCursor.moveToNext());
			}
		} catch (NullPointerException e) {
		}

		return boardname;
	}
	

	private void loadPreference() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
				
		fExistMyboard = settings.getBoolean(SAVE_MY_BOARD, false);
		if( fExistMyboard )
		{
			myBoardName = settings.getString(SAVED_MYBOARD_NAME, "");
		}
		else 
			myBoardName = "";
		
	}
}
