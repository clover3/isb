package com.postech.isb.readBoard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import com.postech.isb.PostechIsb;
import com.postech.isb.R;
import com.postech.isb.R.id;
import com.postech.isb.R.layout;
import com.postech.isb.R.string;
import com.postech.isb.boardList.Board;
import com.postech.isb.boardList.BoardList;
import com.postech.isb.compose.NotePad.Notes;
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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
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
	
	private IsbSession isb;
	private ThreadList global_t;
	private int editing_t;
	private final int NUM_ROWS = 80; //FIX HERE FOR THE NEW NUMBER OF ROWS
	private final int threadPerPage = NUM_ROWS - 4;
	
	/**
     * Standard projection for the interesting columns of a normal note.
     */
    private static final String[] PROJECTION = new String[] {
            Notes._ID, // 0
            Notes.TITLE, // 1
            Notes.NOTE, // 2
            Notes.TARGET_BOARD, // 3
    };
    /** The index of the note column */
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_NOTE = 2;
    private static final int COLUMN_INDEX_TARGET_BOARD = 3;
    
	private static final int NewNote = 1;
	private static final int EditNote = 2;
	
	private Intent gotoAnotherBoard;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.read_board);
        
        isb = ((PostechIsb)getApplicationContext()).isb;
        
        Intent intent = getIntent();
        board = intent.getStringExtra("board");
        
        if (board == null) {
        	Toast.makeText(getApplicationContext(), "Wrong Board", Toast.LENGTH_SHORT).show();
        	finish();
        }
                
        isbThreadItems = new ArrayList<ThreadList>();
        listAdapter = new ThreadListAdapter(this, R.layout.isbthreadlist_item, isbThreadItems);
        
        lv = (ListView)findViewById(android.R.id.list);
        lv.setAdapter(listAdapter);
        lv.setItemsCanFocus(true);
        
        registerForContextMenu(lv);
        
        boardName = (TextView)findViewById(R.id.currentBoard);
        boardName.setText(board);
        
        gotoAnotherBoard = new Intent(this, BoardList.class);
        gotoAnotherBoard.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        boardName.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivity(gotoAnotherBoard);
				finish();
			}
		});
                
        prevBtn = (Button)findViewById(R.id.prevList);
        prevBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {

		        	if (isb.isMain()) {        		
			        	ArrayList<ThreadList> lastPage = isb.getThreadList(board, Math.max(curFstIdx - threadPerPage,1), Math.max(curFstIdx - 1, 1));
			        	Collections.sort(lastPage);
			        	if (lastPage.size() > 0) {
			        		isbThreadItems.clear();
				        	isbThreadItems.addAll(lastPage);
				        	listAdapter.notifyDataSetInvalidated();
			        		curFstIdx = lastPage.get(lastPage.size()-1).num;
			        		curLstIdx = lastPage.get(0).num;
			        		if (curLstIdx > lastIdx)
			        			lastIdx = curLstIdx;

			        		if (curFstIdx > 1)
			        			prevBtn.setVisibility(View.VISIBLE);
			        		else
			        			prevBtn.setVisibility(View.INVISIBLE);
			        		
			        		nextBtn.setVisibility(View.VISIBLE);
			        		//lv.scrollTo(0, 0);
			        	} else
			        		prevBtn.setVisibility(View.INVISIBLE);
		        	} else {
		        		Toast.makeText(getApplicationContext(), "Login First, plz...", Toast.LENGTH_SHORT).show();
		        		//finish();
		        	}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), "Connection Lost", Toast.LENGTH_SHORT).show();
					isb.disconnect();
				}
			}
		});
        
        nextBtn = (Button)findViewById(R.id.nextList);
        nextBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
		        	if (isb.isMain()) {        		
		        		ArrayList<ThreadList> lastPage = isb.getThreadList(board, Math.min(curLstIdx + 1, lastIdx), curLstIdx + threadPerPage);
		        		Collections.sort(lastPage);
		        		if (lastPage.size() > 0) {
			        		isbThreadItems.clear();
				        	isbThreadItems.addAll(lastPage);
				        	listAdapter.notifyDataSetInvalidated();
				        	
			        		curFstIdx = lastPage.get(lastPage.size()-1).num;
			        		curLstIdx = lastPage.get(0).num;
			        		if (lastIdx > curLstIdx)
			        			nextBtn.setVisibility(View.VISIBLE);
			        		else
			        			nextBtn.setVisibility(View.INVISIBLE);
			        		
			        		prevBtn.setVisibility(View.VISIBLE);			        		
			        		//lv.scrollTo(0, 0);
			        	} else
			        		prevBtn.setVisibility(View.INVISIBLE);
		        	} else {
		        		Toast.makeText(getApplicationContext(), "Login First, plz...", Toast.LENGTH_SHORT).show();
		        		//finish();
		        	}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), "Connection Lost", Toast.LENGTH_SHORT).show();
					isb.disconnect();
				}
			}
		});               
    }
    
    @Override
    public void onResume() {

		Log.i("debug", "onResume ");
    	super.onResume();
    	try {
        	if (isb.isMain()) {
	        	ArrayList<ThreadList> lastPage = isb.getLastPageThreadList(board);
	        	Collections.sort(lastPage);
	        	isbThreadItems.clear();
	        	isbThreadItems.addAll(lastPage);
	        	listAdapter.notifyDataSetChanged();
	        	if (lastPage.size() > 0) {
	        		curFstIdx = lastPage.get(lastPage.size()-1).num;
	        		curLstIdx = lastPage.get(0).num;
	        		lastIdx = curLstIdx;
	        		
	        		if (curFstIdx > 1)
	        			prevBtn.setVisibility(View.VISIBLE);
	        		
	        		if (curLstIdx < lastIdx)
	        			nextBtn.setVisibility(View.VISIBLE);
	        	}        	
        	} else {
        		Toast.makeText(getApplicationContext(), "Login First, plz...", Toast.LENGTH_SHORT).show();
        		//finish();
        	}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), "Connection Lost", Toast.LENGTH_SHORT).show();
			isb.disconnect();
		}
    }
    
    @Override
    protected void onListItemClick(ListView l, View view, int position, long id) {
    	ThreadList t = listAdapter.getItem(position);
    	
    	Intent readThread = new Intent(ReadBoards.this, ReadThread.class);
		readThread.putExtra("board", board);
		readThread.putExtra("num", t.num);
		startActivity(readThread);
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);
      
      menu.add(0, WRITE, Menu.NONE,R.string.write)
      				.setShortcut('3', 'a')
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
    	  Intent giveMeNewThread = new Intent(Intent.ACTION_INSERT, Notes.CONTENT_URI);
			giveMeNewThread.putExtra("board", board);
			startActivityForResult(giveMeNewThread, NewNote);
    	  return true;
      }
      }
      
      return false;
    }
    
    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
    	super.onActivityResult(reqCode, resCode, data);

    	switch (reqCode) {
    	case (NewNote) : {
    		if (resCode == Activity.RESULT_OK) {
    			String result = data.getAction();
    			Uri resultUri = Uri.parse(result);
    			
    			Cursor cursor = managedQuery(resultUri, PROJECTION, null, null,
    	                Notes.DEFAULT_SORT_ORDER);
    			
    			if (cursor.moveToFirst()) {
                	String title = cursor.getString(COLUMN_INDEX_TITLE);
                	String content = cursor.getString(COLUMN_INDEX_NOTE);
                	String targetBoard = cursor.getString(COLUMN_INDEX_TARGET_BOARD);
                	
                	if (isb.isMain()) {
    	            	try {
    						if (isb.writeToBoard(targetBoard, title, content)) {
    							Toast.makeText(getApplicationContext(), "Write success", Toast.LENGTH_SHORT).show();
    							ContentResolver cr = getContentResolver();
    		    				if (cr.delete(resultUri, null, null) > 0) {
    		    					Log.i("debug", "Delete success");    					
    		    				}
    						} else {
    							Toast.makeText(getApplicationContext(), "Fail! Invalid board?", Toast.LENGTH_SHORT).show();
    						}
    					} catch (IOException e) {
    						// TODO Auto-generated catch block
    						e.printStackTrace();
    						Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
    						isb.disconnect();
    					}
                	} else
                		Toast.makeText(getApplicationContext(), "Login first plz...", Toast.LENGTH_SHORT).show();
    			}
    		}
    		break;
    	}
    	case (EditNote) : {
    		if (resCode == Activity.RESULT_OK) {
    			String result = data.getAction();
    			Uri resultUri = Uri.parse(result);
    			
    			Cursor cursor = managedQuery(resultUri, PROJECTION, null, null,
    	                Notes.DEFAULT_SORT_ORDER);
    			
    			if (cursor.moveToFirst()) {
                	String title = cursor.getString(COLUMN_INDEX_TITLE);
                	String content = cursor.getString(COLUMN_INDEX_NOTE);
                	String targetBoard = cursor.getString(COLUMN_INDEX_TARGET_BOARD);
                	
                	if (isb.isMain()) {
    	            	try {
    						if (isb.EditToBoard(targetBoard, title, content, editing_t)) 
    							Toast.makeText(getApplicationContext(), "Edit success", Toast.LENGTH_SHORT).show();
    						else
    							Toast.makeText(getApplicationContext(), "Edit fail.", Toast.LENGTH_SHORT).show();
    						ContentResolver cr = getContentResolver();
		    				if (cr.delete(resultUri, null, null) > 0) {
		    					Log.i("debug", "Edit success");
		    				}
    					} catch (IOException e) {
    						// TODO Auto-generated catch block
    						e.printStackTrace();
    						Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
    						isb.disconnect();
    					}
                	} else{
                		Toast.makeText(getApplicationContext(), "Login first plz...", Toast.LENGTH_SHORT).show();
                	}
    			}
    		}
    		break;
    	}
    	}
    }    

    
    @Override
    public void onCreateContextMenu(ContextMenu menu, 
                                    View v, 
                                    ContextMenu.ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      
      menu.setHeaderTitle("Selected Thread");
      menu.add(0, INVERSE, Menu.NONE, R.string.inverse);
      menu.add(0, MARK, Menu.NONE, R.string.mark);
      menu.add(0, EDIT, Menu.NONE, R.string.edit);
      menu.add(0, DELETE, Menu.NONE, R.string.delete);
      menu.add(0, READER, Menu.NONE, R.string.reader);
      
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {  
      super.onContextItemSelected(item);
      
      switch (item.getItemId()) {
        case (DELETE): {
        	AdapterView.AdapterContextMenuInfo menuInfo;
            menuInfo =(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
            int index = menuInfo.position;
            ThreadList t = listAdapter.getItem(index);
            
            if (isb.isMain()) {
            	AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
				alertDlg.setTitle(R.string.delete);
				global_t = t;
				alertDlg.setMessage(R.string.delete_confirm);
				alertDlg.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							//TODO: Remove global_t and do proper code. 
							if (isb.modifyThread(board, global_t.num, isb.THREAD_DELETE)) {
								Toast.makeText(getApplicationContext(), "Delete success", Toast.LENGTH_SHORT).show();
								listAdapter.remove(global_t);
								listAdapter.notifyDataSetChanged();
							} else {
								Toast.makeText(getApplicationContext(), "Delete fail.", Toast.LENGTH_SHORT).show();
							}
						}
						catch (IOException e) {
							e.printStackTrace();
							Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
							isb.disconnect();
						}
					}
				});
				alertDlg.setNegativeButton(R.string.cancle, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
					}
				});
				alertDlg.show();
        	} else
        		Toast.makeText(getApplicationContext(), "Login first plz...", Toast.LENGTH_SHORT).show();
            
        	return true;
        }
        case (INVERSE): {
        	AdapterView.AdapterContextMenuInfo menuInfo;
            menuInfo =(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
            int index = menuInfo.position;
            ThreadList t = listAdapter.getItem(index);
            
            if (isb.isMain()) {
            	try {
					if (isb.modifyThread(board, t.num, isb.THREAD_INVERSE)) {
						t.highlight = !t.highlight;
						listAdapter.notifyDataSetChanged();
					} else {
						Toast.makeText(getApplicationContext(), "Inverse fail.", Toast.LENGTH_SHORT).show();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
					isb.disconnect();
				}
        	} else
        		Toast.makeText(getApplicationContext(), "Login first plz...", Toast.LENGTH_SHORT).show();
            
        	return true;
        }
        case (MARK): {
        	AdapterView.AdapterContextMenuInfo menuInfo;
            menuInfo =(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
            int index = menuInfo.position;
            ThreadList t = listAdapter.getItem(index);
            
            if (isb.isMain()) {
            	try {
					if (isb.modifyThread(board, t.num, isb.THREAD_MARK)) {
						t.notdel = !t.notdel;
						listAdapter.notifyDataSetChanged();
					} else {
						Toast.makeText(getApplicationContext(), "Mark fail.", Toast.LENGTH_SHORT).show();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
					isb.disconnect();
				}
        	} else
        		Toast.makeText(getApplicationContext(), "Login first plz...", Toast.LENGTH_SHORT).show();
            
        	return true;
        }  
        case (READER): {
        	AdapterView.AdapterContextMenuInfo menuInfo;
            menuInfo =(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
            int index = menuInfo.position;
            ThreadList t = listAdapter.getItem(index);
            
            if (isb.isMain()) {
            	try {
            		String strReader = isb.viewThreadReader(board, t.num);
            		
            		if( strReader == null )
            		{
            			strReader = "권한이 없습니다.";
            		}
            		String title = String.format("이 글을 읽은 사용자(%d)", t.cnt);
            		new AlertDialog.Builder(ReadBoards.this)
            		.setTitle(title)
            		.setMessage(strReader)
            		.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            			public void onClick(DialogInterface dialog, int whichButton) {                                        
            				//...할일
            			}
            		})   
            		.show();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
					isb.disconnect();
				}
        	} else
        		Toast.makeText(getApplicationContext(), "Login first plz...", Toast.LENGTH_SHORT).show();
            
        	return true;
        }
        case (EDIT): {
        	Intent giveMeNewThread = new Intent(Intent.ACTION_INSERT, Notes.CONTENT_URI);
        	giveMeNewThread.putExtra("board", board);
        	AdapterView.AdapterContextMenuInfo menuInfo;
            menuInfo =(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
            int index = menuInfo.position;
            editing_t = listAdapter.getItem(index).num;
        	IsbThread t;
			try {
				t = isb.readThread(board, editing_t);
				String title = listAdapter.getItem(index).title;
				String note;
				if (title.matches("[\\s]*$")){
					title = "tmp_title_for_empty_title_haha_hehe_hoho_huhu_nyahahahahahahaha";
					note = (title+t.contents).trim();
					note = note.replace("tmp_title_for_empty_title_haha_hehe_hoho_huhu_nyahahahahahahaha", "");
				}
				else
					note = (title+t.contents).trim();
				giveMeNewThread.putExtra("note", note);
				startActivityForResult(giveMeNewThread, EditNote);
			} catch (IOException e) {
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
				isb.disconnect();
			}
			return true;
        }
      }
      return false;
    }
    
}
