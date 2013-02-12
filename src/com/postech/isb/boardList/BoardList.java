package com.postech.isb.boardList;

import java.io.IOException;
import java.util.ArrayList;

import com.postech.isb.PostechIsb;
import com.postech.isb.R;
import com.postech.isb.R.drawable;
import com.postech.isb.R.id;
import com.postech.isb.R.layout;
import com.postech.isb.R.string;
import com.postech.isb.readBoard.ReadBoards;
import com.postech.isb.util.IsbSession;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

public class BoardList extends ListActivity {

	static final private int REFRESH = Menu.FIRST;
	static final private int FAVORITE = Menu.FIRST + 1;
	static final private int FAVORITE_ONLY = Menu.FIRST + 2;
	static final private int SEARCH_NEW = Menu.FIRST + 3;
	
	
	private IsbSession isb;
	private ArrayList<Board> boardItems;
	private BoardAdapter boardAdapter;
	private EditText search;
	private IsbDBAdapter boardDBAdapter;
	private TextWatcher textWatcher;
	private ProgressDialog pd;
	private boolean isEmpty=true;
	
	private static final String FAVORITE_ONLY_KEY = "FAVORITE_ONLY_KEY";
	
	private boolean favoriteOnly = false;
	private ArrayList<Board> board_items;

	// Handler for thread. Show error messages.
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.i("BoardList", "Msg : " + msg.what);
			switch (msg.what) {
			case 0: updateArray(); pd.dismiss(); break; 
			case 1: Toast.makeText(BoardList.this, "Connection Lost....", Toast.LENGTH_SHORT).show(); pd.dismiss(); break;
			case 2: pd.setMessage("Insert to Database..."); break;
			case 3: {int i;
			boardAdapter.clear();
			for (i = 0; i < board_items.size(); i++){
				Board tmp = board_items.get(i);
				Log.i("newmbewb", "result: "+Boolean.toString(tmp.newt));
				boardAdapter.add(tmp);
			}
			boardAdapter.notifyDataSetChanged();
			isEmpty = boardAdapter.isEmpty(); 
			boardAdapter.getFilter().filter(search.getText().toString());
			}
			pd.dismiss(); break;
			}
		}
	};
	
	/*
	static class theLock extends Object { }
	static public theLock lockObject = new theLock();
	*/
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.board_list);
        
        isb = ((PostechIsb)getApplicationContext()).isb;

        boardItems = new ArrayList<Board>();
        boardAdapter = new BoardAdapter(this, R.layout.boardlist_item, boardItems);
                
        setListAdapter(boardAdapter);
        registerForContextMenu(getListView());
        
        search = (EditText)findViewById(R.id.searchBoard);
        textWatcher = new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				boardAdapter.getFilter().filter(s);
			}
        };
        search.addTextChangedListener(textWatcher);
        
        restoreUIState();
        
        // DB
        boardDBAdapter = new IsbDBAdapter(this);
        boardDBAdapter.open();        
        populateBoardList();        
    }
    
    private void restoreUIState() {
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		favoriteOnly = settings.getBoolean(FAVORITE_ONLY_KEY, false);
	}
    
    @Override
	protected void onResume() {
    	super.onResume();
        if (isEmpty) {
       	 AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
       	 alt_bld.setMessage("Do you want to update now ?")
       	 	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
       			public void onClick(DialogInterface dialog, int id) {
       				updateBoard();
       			}
       		}).setNegativeButton("No", new DialogInterface.OnClickListener() {
       			public void onClick(DialogInterface dialog, int id) {
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
    	super.onPause();
    	SharedPreferences uiState = getPreferences(MODE_PRIVATE);
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
    		pd = ProgressDialog.show(BoardList.this, "Refresh Board list...", "Getting data from Isb...", true, false);
    		new Thread(new Runnable() {
    			@Override
    			public void run() {
    				String name;
    				Board newItem; 
    				board_items = new ArrayList<Board>();
    				try {
    					if (boardListCursor.moveToFirst()){
    						do{
    							if (boardListCursor.getInt(IsbDBAdapter.FAVORITE_COLUMN) == 1){
    								int result;
    								name = boardListCursor.getString(IsbDBAdapter.BOARD_COLUMN);
    								result = isb.searchNewPost(name);
    								newItem = new Board(name, true);
    								newItem.setNewPost(result);
    								board_items.add(newItem);

    							}
    						} while(boardListCursor.moveToNext());
    					}  
    				} catch (IOException e) {
    					handler.sendEmptyMessage(1);
    					isb.disconnect();
    				}
    				handler.sendEmptyMessage(3);
    			}
    		}).start(); // 스레드 실행
    		
    	} else {
    		Toast.makeText(BoardList.this, "Login first plz.", Toast.LENGTH_SHORT).show();
    	}
    }

    private void updateArray() {
    	boardListCursor.requery();
    	boardAdapter.clear();
    	
    	String name;
    	boolean favorite;
    	Board newItem; 
    	if (boardListCursor.moveToFirst())
    		do {
    			name = boardListCursor.getString(IsbDBAdapter.BOARD_COLUMN);
    			favorite = boardListCursor.getInt(IsbDBAdapter.FAVORITE_COLUMN) == 1;
    			if (!favoriteOnly || favorite) {
    				newItem = new Board(name, favorite);
    				boardAdapter.add(newItem);
    			}
    		} while (boardListCursor.moveToNext());
    	
    	boardAdapter.notifyDataSetChanged();
    	isEmpty = boardAdapter.isEmpty(); 
    	boardAdapter.getFilter().filter(search.getText().toString());
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	search.removeTextChangedListener(textWatcher);
    	boardDBAdapter.close();
    }
    
    @Override
    protected void onListItemClick(ListView l, View view, int position, long id) {

    	String boardName = ((TextView)view.findViewById(R.id.BoardListTitle)).getText().toString();    	
    	String action = getIntent().getAction();
    	
    	if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
    		// The caller is waiting for us to return a note selected by
    		// the user.  The have clicked on one, so return it now.
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
      
      menu.add(0, REFRESH, Menu.NONE,R.string.refresh)
      				.setIcon(R.drawable.ic_menu_refresh);
      menu.add(0, FAVORITE_ONLY, Menu.NONE, R.string.list_favorite_only)
      				.setIcon(R.drawable.list_favorite_only);
      menu.add(0, SEARCH_NEW, Menu.NONE, R.string.list_new)
      				.setIcon(R.drawable.list_new);
      return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
      super.onPrepareOptionsMenu(menu);
      
      MenuItem listOption = menu.findItem(FAVORITE_ONLY);
      listOption.setTitle(favoriteOnly ? R.string.list_all : R.string.list_favorite_only);
      listOption.setIcon(favoriteOnly ? R.drawable.list_all : R.drawable.list_favorite_only);
      
      return true;
    }    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      super.onOptionsItemSelected(item);
      
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
      
      return false;
    }
    
    private void updateBoard() {
    	if (isb.isMain()) {    		  
  		  pd = ProgressDialog.show(BoardList.this, "Refresh Board list...", "Getting data from Isb...", true, false);
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
  		  Toast.makeText(BoardList.this, "Login first plz.", Toast.LENGTH_SHORT).show();
  	  }
    }
    
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, 
                                    View v, 
                                    ContextMenu.ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      
      menu.setHeaderTitle("Selected Board");
      menu.add(0, FAVORITE, Menu.NONE, R.string.favorite);
      
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {  
      super.onContextItemSelected(item);
      
      switch (item.getItemId()) {
        case (FAVORITE): {
        	AdapterView.AdapterContextMenuInfo menuInfo;
            menuInfo =(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
            int index = menuInfo.position;
            Board b = boardAdapter.getItem(index);
            boardDBAdapter.setFavoriteByName(b.name, !b.favorite);
            boardAdapter.toggleFavorite(index);
        	return true;
        }
      }
      return false;
    }
    
    public String getBoardNameByInitial(String initial)
    {
    	String boardname = "";
		try {
			if (boardListCursor.moveToFirst()){
				do{
					if (boardListCursor.getInt(IsbDBAdapter.FAVORITE_COLUMN) == 1){
						String name = boardListCursor.getString(IsbDBAdapter.BOARD_COLUMN);
						if( name.charAt(0) != initial.charAt(0) )
							continue;
						int idx2 = name.indexOf('/') + 1;
						if( name.charAt(idx2) == initial.charAt(2) )
						{
							boardname = name;
							break;
						}
							
					}
				} while(boardListCursor.moveToNext());
			}  
		} catch (NullPointerException e) 
		{
		}

    	return boardname;
    }
}