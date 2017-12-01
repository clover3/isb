package com.postech.isb.readBoard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.postech.isb.PostechIsb;
import com.postech.isb.R;
import com.postech.isb.R.id;
import com.postech.isb.R.layout;
import com.postech.isb.R.string;
import com.postech.isb.boardList.Board;
import com.postech.isb.boardList.BoardList;
import com.postech.isb.compose.NotePad.Notes;
import com.postech.isb.readThread.MyScrollView;
import com.postech.isb.readThread.ReadThread;
import com.postech.isb.util.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ReadBoards extends ListActivity {

	static final private int WRITE = Menu.FIRST;
	static final private int DELETE = Menu.FIRST + 1;
	static final private int INVERSE = Menu.FIRST + 2;
	static final private int MARK = Menu.FIRST + 3;
	static final private int EDIT = Menu.FIRST + 4;
	static final private int READER = Menu.FIRST + 5;
	static final private int SEND_MAIL = Menu.FIRST + 6;

	private ArrayList<ThreadList> isbThreadItems;
	private ThreadListAdapter listAdapter;
	private ListView lv;
	private String board;
	private TextView boardName;

	private Button prevBtn;
	private Button nextBtn;

	private int curFstIdx;
	private int curLstIdx;
	private int lastIdx;

	private int lastReadIdx;
	private boolean lastFollowLink;
	static final public int lastReadNothing = -1;

	private IsbSession isb;
	private ThreadList global_t;
	private int editing_t;
	private static final int NUM_ROWS = 80; // FIX HERE FOR THE NEW NUMBER OF ROWS
	public static final int threadPerPage = NUM_ROWS - 4;
	private TouchMenuManager menuMan;
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

	private static final int reqNewNote = 1;
	private static final int reqEditNote = 2;
	private static final int reqReadThread = 3;

	private Intent gotoAnotherBoard;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		MenuOption.setUseActionBar(this);
		super.onCreate(savedInstanceState);

		lastReadIdx = lastReadNothing;
		lastFollowLink = false;
		setContentView(R.layout.read_board);

		isb = ((PostechIsb) getApplicationContext()).isb;

		Intent intent = getIntent();
		board = intent.getStringExtra("board");

		if (board == null) {
			Toast.makeText(getApplicationContext(), "Wrong Board",
					Toast.LENGTH_SHORT).show();
			finish();
		}

		isbThreadItems = new ArrayList<ThreadList>();
		listAdapter = new ThreadListAdapter(this, R.layout.isbthreadlist_item,
				isbThreadItems);

		lv = (ListView) findViewById(android.R.id.list);
		lv.setAdapter(listAdapter);
		lv.setItemsCanFocus(true);

		registerForContextMenu(lv);

		boardName = (TextView) findViewById(R.id.currentBoard);
		boardName.setText(board);
		if (MenuOption.useActionBar)
			boardName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.menubar_white_36, 0);

		gotoAnotherBoard = new Intent(this, BoardList.class);
		gotoAnotherBoard.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		menuMan = new TouchMenuManager(this);
		lv.setOnTouchListener(menuMan.MyTouchListener);

		boardName.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (MenuOption.useActionBar)
					openOptionsMenu();
				else {
					startActivity(gotoAnotherBoard);
					finish();
				}
			}
		});

		prevBtn = (Button) findViewById(R.id.prevList);
		prevBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					if (isb.isMain()) {
						int fstIdx = Math.max(curFstIdx - threadPerPage, 1);
						int lstIdx = Math.max(curFstIdx - 1, 1);
						displayPage(fstIdx, lstIdx, lstIdx);
					} else {
						Toast.makeText(getApplicationContext(),
								"Login First, plz...", Toast.LENGTH_SHORT)
								.show();
						// finish();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), "Connection Lost",
							Toast.LENGTH_SHORT).show();
					isb.disconnect();
				}
			}
		});

		nextBtn = (Button) findViewById(R.id.nextList);
		nextBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					if (isb.isMain()) {
						if (curLstIdx + threadPerPage == lastIdx)
							displayFirstPage(lastIdx + threadPerPage);
						else {
							int fstIdx = Math.min(curLstIdx + 1, lastIdx);
							int lstIdx = curLstIdx + threadPerPage;
							displayPage(fstIdx, lstIdx, lstIdx);
						}
					} else {
						Toast.makeText(getApplicationContext(),
								"Login First, plz...", Toast.LENGTH_SHORT)
								.show();
						// finish();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), "Connection Lost",
							Toast.LENGTH_SHORT).show();
					isb.disconnect();
				}
			}
		});
	}

	private void displayFirstPage(int focus) throws IOException {
		ArrayList<ThreadList> lastPage = isb
				.getLastPageThreadList(board);
		if (lastPage == null) {
			Toast.makeText(getApplicationContext(),
					"This board does not exist. Try 'Refresh' at the board list.", Toast.LENGTH_SHORT)
					.show();
			lastPage = new ArrayList<ThreadList>(); // Empty list
		}
		Collections.sort(lastPage);
		isbThreadItems.clear();
		isbThreadItems.addAll(lastPage);
		listAdapter.notifyDataSetChanged();
		if (lastPage.size() > 0) {
			curFstIdx = lastPage.get(lastPage.size() - 1).num;
			curLstIdx = lastPage.get(0).num;
			lastIdx = curLstIdx;

			if (curFstIdx > 1)
				prevBtn.setVisibility(View.VISIBLE);

			if (curLstIdx < lastIdx)
				nextBtn.setVisibility(View.VISIBLE);

			// Set focus
			if (focus == 0) {
				// Do nothing
			}
			else {
				int listIdx;
				if (focus == lastReadNothing)
					listIdx = 0;
				else {
					listIdx = curLstIdx - focus - 5;
					if (listIdx < 0)
						listIdx = 0;
					else if (listIdx > lastPage.size() - 1)
						listIdx = lastPage.size() - 1;
				}
				lv.setSelection(listIdx);
				lv.requestFocus();
			}
		}
	}

	private void displayPage(int FstIdx, int LstIdx, int focus) throws IOException {
		ArrayList<ThreadList> lastPage = isb.getThreadList(
				board, FstIdx, LstIdx);
		Collections.sort(lastPage);
		if (lastPage.size() > 0) {
			isbThreadItems.clear();
			isbThreadItems.addAll(lastPage);
			listAdapter.notifyDataSetInvalidated();

			curFstIdx = lastPage.get(lastPage.size() - 1).num;
			curLstIdx = lastPage.get(0).num;
			if (curLstIdx > lastIdx)
				lastIdx = curLstIdx;

			// Set visibility of buttons
			if (curFstIdx > 1)
				prevBtn.setVisibility(View.VISIBLE);
			else
				prevBtn.setVisibility(View.INVISIBLE);

			if (curLstIdx < lastIdx)
				nextBtn.setVisibility(View.VISIBLE);
			else
				nextBtn.setVisibility(View.INVISIBLE);

			//lv.scrollTo(0, 0);
		} else
			prevBtn.setVisibility(View.INVISIBLE);

		// Set focus
		if (focus == 0) {
			// Do nothing
		}
		else {
			int listIdx;
			if (focus == lastReadNothing)
				listIdx = 0;
			else {
				listIdx = curLstIdx - focus - 5;
				if (listIdx < 0)
					listIdx = 0;
				else if (listIdx > lastPage.size() - 1)
					listIdx = lastPage.size() - 1;
			}
			lv.setSelection(listIdx);
			lv.requestFocus();
		}
	}

	boolean idxInPage(int idx, int fstIdx) {
		if (idx < fstIdx)
			return false;
		if (idx >= fstIdx + threadPerPage)
			return false;
		return true;
	}

	boolean idxInCurPage(int idx) {
		return idxInPage(idx, curFstIdx);
	}
	boolean idxInFirstPage(int idx) {
		if (idx == lastReadNothing)
			return true;
		if (idx > lastIdx - threadPerPage)
			return true;
		return false;
	}

	@Override
	public void onResume() {
		Log.i("debug", "onResume ");
		super.onResume();
		try {
			if (isb.isMain()) {
				if (idxInFirstPage(lastReadIdx)) {
					// This case should be checked first
					if (idxInCurPage(lastReadIdx)) {
						displayFirstPage(0);
					}
					else {
						// Moved by reading threads
						displayFirstPage(lastReadIdx);
					}
				}
				else if (idxInCurPage(lastReadIdx)) {
					displayPage(curFstIdx, curLstIdx, 0);
				}
				else {
					// Find a proper page
					int newFstIdx;
					int newLstIdx;
					if (lastReadIdx < curFstIdx) {
						for (newFstIdx = curFstIdx; newFstIdx > 1; newFstIdx -= threadPerPage) {
							if (idxInPage(lastReadIdx, newFstIdx))
								break;
						}
					}
					else {
						for (newFstIdx = curFstIdx; newFstIdx < lastIdx;
							 newFstIdx += threadPerPage) {
							if (idxInPage(lastReadIdx, newFstIdx))
								break;
						}
					}
					newFstIdx = Math.max(newFstIdx, 1);
					newLstIdx = newFstIdx + threadPerPage - 1;
					if (newLstIdx >= lastIdx) {
						displayFirstPage(lastReadIdx);
					}
					else
						displayPage(newFstIdx, newLstIdx, lastReadIdx);
				}
			} else {
				Toast.makeText(getApplicationContext(), "Login First, plz...",
						Toast.LENGTH_SHORT).show();
				// finish();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), "Connection Lost",
					Toast.LENGTH_SHORT).show();
			isb.disconnect();
		}
	}

	@Override
	protected void onListItemClick(ListView l, View view, int position, long id) {
		ThreadList t = listAdapter.getItem(position);

		Intent readThread = new Intent(ReadBoards.this, ReadThread.class);
		readThread.putExtra("board", board);
		readThread.putExtra("num", t.num);
		lastReadIdx = t.num;
		startActivityForResult(readThread, reqReadThread);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (board.equals("mail"))
			menu.add(0, SEND_MAIL, Menu.NONE, R.string.send_mail).setShortcut('3', 'a')
					.setIcon(R.drawable.ic_menu_compose);
		else
			menu.add(0, WRITE, Menu.NONE, R.string.write).setShortcut('3', 'a')
					.setIcon(android.R.drawable.ic_menu_add);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

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
				startActivityForResult(giveMeNewThread, reqNewNote);
				return true;
			}
			case SEND_MAIL: {
				Intent giveMeNewThread = new Intent(Intent.ACTION_INSERT,
						Notes.CONTENT_URI);
				giveMeNewThread.putExtra("board", board);
				startActivityForResult(giveMeNewThread, reqNewNote);
				return true;
			}
		}

		return false;
	}

	@Override
	public void onActivityResult(int reqCode, int resCode, Intent data) {
		super.onActivityResult(reqCode, resCode, data);

		switch (reqCode) {
			case (reqReadThread): {
				if (resCode == Activity.RESULT_OK) {
					int idx = data.getIntExtra("idx", lastReadNothing);

					lastFollowLink = data.getBooleanExtra("follow_link", false);
					if (!lastFollowLink)
						// Apply the new read idx only when follow_link is false
						lastReadIdx = idx;
				}
				break;
			}
			case (reqNewNote): {
				lastReadIdx = lastReadNothing;
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
								if (board.equals("mail")) {
									if (isb.writeMail(targetBoard, title, content)) {
										Toast.makeText(getApplicationContext(),
												"Write success", Toast.LENGTH_SHORT)
												.show();
										ContentResolver cr = getContentResolver();
										if (cr.delete(resultUri, null, null) > 0) {
											Log.i("debug", "Delete success");
										}
									} else {
										Toast.makeText(getApplicationContext(),
												"Fail! Invalid user?",
												Toast.LENGTH_SHORT).show();
									}
								}
								else {
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
				break;
			}
			case (reqEditNote): {
				lastReadIdx = lastReadNothing;
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
								if (isb.EditToBoard(targetBoard, title, content,
										editing_t))
									Toast.makeText(getApplicationContext(),
											"Edit success", Toast.LENGTH_SHORT)
											.show();
								else
									Toast.makeText(getApplicationContext(),
											"Edit fail.", Toast.LENGTH_SHORT)
											.show();
								ContentResolver cr = getContentResolver();
								if (cr.delete(resultUri, null, null) > 0) {
									Log.i("debug", "Edit success");
								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								Toast.makeText(getApplicationContext(),
										"Connection lost", Toast.LENGTH_SHORT)
										.show();
								isb.disconnect();
							}
						} else {
							Toast.makeText(getApplicationContext(),
									"Login first plz...", Toast.LENGTH_SHORT)
									.show();
						}
					}
				}
				break;
			}
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		menu.setHeaderTitle("Selected Thread");
		if (board.equals("diary")) {
			menu.add(0, INVERSE, Menu.NONE, R.string.inverse);
			menu.add(0, MARK, Menu.NONE, R.string.mark);
			menu.add(0, EDIT, Menu.NONE, R.string.edit);
			menu.add(0, DELETE, Menu.NONE, R.string.delete)
					.setEnabled(!SP.getBoolean("disable_delete", false));
		} else if (board.equals("mail")) {
			menu.add(0, INVERSE, Menu.NONE, R.string.inverse);
			menu.add(0, MARK, Menu.NONE, R.string.mark);
			menu.add(0, DELETE, Menu.NONE, R.string.delete)
					.setEnabled(!SP.getBoolean("disable_delete", false));
		} else {
			// board
			menu.add(0, INVERSE, Menu.NONE, R.string.inverse);
			menu.add(0, MARK, Menu.NONE, R.string.mark);
			menu.add(0, EDIT, Menu.NONE, R.string.edit);
			menu.add(0, DELETE, Menu.NONE, R.string.delete)
					.setEnabled(!SP.getBoolean("disable_delete", false));
			menu.add(0, READER, Menu.NONE, R.string.reader);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);

		switch (item.getItemId()) {
		case (DELETE): {
			AdapterView.AdapterContextMenuInfo menuInfo;
			menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			int index = menuInfo.position;
			ThreadList t = listAdapter.getItem(index);

			if (isb.isMain()) {
				AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
				alertDlg.setTitle(R.string.delete);
				global_t = t;
				alertDlg.setMessage(R.string.delete_confirm);
				alertDlg.setNegativeButton(R.string.delete,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								try {
									// TODO: Remove global_t and do proper code.
									if (isb.modifyThread(board, global_t.num,
											isb.THREAD_DELETE)) {
										Toast.makeText(getApplicationContext(),
												"Delete success",
												Toast.LENGTH_SHORT).show();
										listAdapter.remove(global_t);
										listAdapter.notifyDataSetChanged();
									} else {
										Toast.makeText(getApplicationContext(),
												"Delete fail.",
												Toast.LENGTH_SHORT).show();
									}
								} catch (IOException e) {
									e.printStackTrace();
									Toast.makeText(getApplicationContext(),
											"Connection lost",
											Toast.LENGTH_SHORT).show();
									isb.disconnect();
								}
							}
						});
				alertDlg.setPositiveButton(R.string.cancle,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

							}
						});
				alertDlg.show();
			} else
				Toast.makeText(getApplicationContext(), "Login first plz...",
						Toast.LENGTH_SHORT).show();

			return true;
		}
		case (INVERSE): {
			AdapterView.AdapterContextMenuInfo menuInfo;
			menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			int index = menuInfo.position;
			ThreadList t = listAdapter.getItem(index);

			if (isb.isMain()) {
				try {
					if (isb.modifyThread(board, t.num, isb.THREAD_INVERSE)) {
						t.highlight = !t.highlight;
						listAdapter.notifyDataSetChanged();
					} else {
						Toast.makeText(getApplicationContext(),
								"Inverse fail.", Toast.LENGTH_SHORT).show();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), "Connection lost",
							Toast.LENGTH_SHORT).show();
					isb.disconnect();
				}
			} else
				Toast.makeText(getApplicationContext(), "Login first plz...",
						Toast.LENGTH_SHORT).show();

			return true;
		}
		case (MARK): {
			AdapterView.AdapterContextMenuInfo menuInfo;
			menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			int index = menuInfo.position;
			ThreadList t = listAdapter.getItem(index);

			if (isb.isMain()) {
				try {
					if (isb.modifyThread(board, t.num, isb.THREAD_MARK)) {
						t.notdel = !t.notdel;
						listAdapter.notifyDataSetChanged();
					} else {
						Toast.makeText(getApplicationContext(), "Mark fail.",
								Toast.LENGTH_SHORT).show();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), "Connection lost",
							Toast.LENGTH_SHORT).show();
					isb.disconnect();
				}
			} else
				Toast.makeText(getApplicationContext(), "Login first plz...",
						Toast.LENGTH_SHORT).show();

			return true;
		}
		case (READER): {
			AdapterView.AdapterContextMenuInfo menuInfo;
			menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			int index = menuInfo.position;
			ThreadList t = listAdapter.getItem(index);

			if (isb.isMain()) {
				try {
					String strReader = isb.viewThreadReader(board, t.num);

					if (strReader == null) {
						strReader = "권한이 없습니다.";
					}
					String title = String.format("이 글을 읽은 사용자(%d)", t.cnt);
					new AlertDialog.Builder(ReadBoards.this)
							.setTitle(title)
							.setMessage(strReader)
							.setNeutralButton("OK",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int whichButton) {
											// ...할일
										}
									}).show();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), "Connection lost",
							Toast.LENGTH_SHORT).show();
					isb.disconnect();
				}
			} else
				Toast.makeText(getApplicationContext(), "Login first plz...",
						Toast.LENGTH_SHORT).show();

			return true;
		}
		case (EDIT): {
			Intent giveMeNewThread = new Intent(Intent.ACTION_INSERT,
					Notes.CONTENT_URI);
			giveMeNewThread.putExtra("board", board);
			AdapterView.AdapterContextMenuInfo menuInfo;
			menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			int index = menuInfo.position;
			editing_t = listAdapter.getItem(index).num;
			IsbThread t;
			try {
				t = isb.readThread(board, editing_t);
				String title = listAdapter.getItem(index).GetRawTitle();
				String note;
				// Delete whitespace at the end of highlighted lines,
				Pattern p = Pattern.compile("^(!.*?) *$", Pattern.MULTILINE);
				Matcher m = p.matcher(t.contents);
				t.contents = m.replaceAll("$1");

				if (title.matches("[\\s]*$")) {
					title = "tmp_title_for_empty_title_haha_hehe_hoho_huhu_nyahahahahahahaha";
					note = (title + t.contents).trim();
					note = note
							.replace(
									"tmp_title_for_empty_title_haha_hehe_hoho_huhu_nyahahahahahahaha",
									"");
				} else
					note = (title + t.contents).replaceAll("\\s*$", "");
				giveMeNewThread.putExtra("note", note);
				giveMeNewThread.putExtra("edit", true);
				startActivityForResult(giveMeNewThread, reqEditNote);
			} catch (IOException e) {
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), "Connection lost",
						Toast.LENGTH_SHORT).show();
				isb.disconnect();
			}
			return true;
		}
		}
		return false;
	}

}
