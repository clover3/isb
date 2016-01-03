package com.postech.isb.boardList;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

public class IsbDBAdapter {
  private static final String DATABASE_NAME = "isb.db";
  private static final String DATABASE_TABLE = "boards";
  private static final int DATABASE_VERSION = 1;
 
  public static final String KEY_ID = "_id";
  public static final int ID_COLUMN = 0;
  public static final String KEY_BOARD = "name";
  public static final int BOARD_COLUMN = 1;
  public static final String KEY_FAVORITE = "favorite";
  public static final int FAVORITE_COLUMN = 2;
  
  private SQLiteDatabase db;
  private final Context context;
  private boardDBOpenHelper dbHelper;

  public IsbDBAdapter(Context _context) {
    this.context = _context;
    dbHelper = new boardDBOpenHelper(context, DATABASE_NAME, 
                                    null, DATABASE_VERSION);
  }
 
  public void close() {
    db.close();
  }
  
  public void open() throws SQLiteException {  
    try {
      db = dbHelper.getWritableDatabase();
    } catch (SQLiteException ex) {
      db = dbHelper.getReadableDatabase();
    }
  }  
  
  //Insert a new board
  public long insertBoard(Board _board) {
    // Create a new row of values to insert.
    ContentValues newBoardValues = new ContentValues();
    // Assign values for each row.
    newBoardValues.put(KEY_BOARD, _board.name);
    newBoardValues.put(KEY_FAVORITE, _board.favorite);
    // Insert the row.
    return db.insert(DATABASE_TABLE, null, newBoardValues);
  }

  // Remove a task based on its index
  public boolean removeBoard(long _rowIndex) {
    return db.delete(DATABASE_TABLE, KEY_ID + "=" + _rowIndex, null) > 0;
  }

  // Update a task
  public boolean updateBoard(ArrayList<Board> _board) {
	  if (_board == null || _board.isEmpty())
		  return false;

	  Board favoriteIt;
	  int idx;
	  
	  Cursor cursor = db.query(true, DATABASE_TABLE, 
              new String[] {KEY_BOARD, KEY_FAVORITE},
              KEY_FAVORITE+ "= 1", null, null, null, 
              null, null);
	  
	  if ((cursor.getCount() > 0) && cursor.moveToFirst()) {
		  do {
			  favoriteIt = new Board(cursor.getString(0), true);
			  Log.i("debug", "Favorite : " + favoriteIt.name);
			  if ((idx = _board.lastIndexOf(favoriteIt)) != -1)
				  _board.get(idx).favorite = true;
		  } while (cursor.moveToNext());
	  } else {
		  Log.i("debug", "You have no favorite");
	  }
		  
	  
	  dbHelper.deleteAll(db);
	  
	  db.beginTransaction();
	  try {
		  Iterator<Board> iter = _board.iterator();	  
		  while (iter.hasNext()) {
			  insertBoard(iter.next());		  	  		  		  
		  }
	     db.setTransactionSuccessful();
	  } finally {
	     db.endTransaction();
	  }
	  
	  return true;
  }
  
  public boolean setFavoriteByName(String name, boolean favorite) {
	  ContentValues newValue = new ContentValues();
	  newValue.put(KEY_FAVORITE, favorite);
	  return db.update(DATABASE_TABLE, newValue, KEY_BOARD + "=\"" + name+ "\"", null) > 0;
  }
  
  public Cursor getAllBoardCursor() {
    return db.query(DATABASE_TABLE, 
                    new String[] { KEY_ID, KEY_BOARD, KEY_FAVORITE}, 
                    null, null, null, null, null);
  }

  public Cursor setCursorToBoardById(long _rowIndex) throws SQLException {
    Cursor result = db.query(true, DATABASE_TABLE, 
	                           null,
                             KEY_ID + "=" + _rowIndex, null, null, null, 
                             null, null);
    if ((result.getCount() == 0) || !result.moveToFirst()) {
      throw new SQLException("No items found for row: " + _rowIndex);
    }
    return result;
  }

  public Board getBoardById(long _rowIndex) throws SQLException {
    Cursor cursor = db.query(true, DATABASE_TABLE, 
                             new String[] {KEY_ID, KEY_BOARD, KEY_FAVORITE},
                             KEY_ID + "=" + _rowIndex, null, null, null, 
                             null, null);
    if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
      throw new SQLException("No item found for row: " + _rowIndex);
    }
    
    int id = cursor.getInt(ID_COLUMN);
    String name = cursor.getString(BOARD_COLUMN); //cursor.getColumnIndex(KEY_BOARD));
    boolean favorite = cursor.getInt(FAVORITE_COLUMN)==1; // cursor.getColumnIndex(KEY_FAVORITE)
		  
    Board result = new Board(id, name, favorite);
    return result;  
  }
  
  public Board getBoardByName(String _name) throws SQLException {
	    Cursor cursor = db.query(true, DATABASE_TABLE, 
	                             new String[] {KEY_ID, KEY_BOARD, KEY_FAVORITE},
	                             KEY_BOARD + "=" + _name, null, null, null, 
	                             null, null);
	    if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
	      throw new SQLException("No item found for name: " + _name);
	    }

	    int id = cursor.getInt(ID_COLUMN);
	    String name = cursor.getString(BOARD_COLUMN); //cursor.getColumnIndex(KEY_BOARD));
	    boolean favorite = cursor.getInt(FAVORITE_COLUMN)==1; // cursor.getColumnIndex(KEY_FAVORITE)
			  
	    Board result = new Board(id, name, favorite);
	    return result;  
	  }
  
  private static class boardDBOpenHelper extends SQLiteOpenHelper {

	  public boardDBOpenHelper(Context context, String name,
	                          CursorFactory factory, int version) {
	    super(context, name, factory, version);
	  }

	  // SQL Statement to create a new database.
	  private static final String DATABASE_CREATE = "create table " + 
	    DATABASE_TABLE + " (" + KEY_ID + " integer primary key autoincrement, " +
	    KEY_BOARD + " text not null, " + KEY_FAVORITE + " integer);";

	  public void deleteAll(SQLiteDatabase _db) {
		  // Drop the old table.
		  _db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
		  // Create a new one.
		  onCreate(_db);
	  }
	  
	  @Override
	  public void onCreate(SQLiteDatabase _db) {
	    _db.execSQL(DATABASE_CREATE);
	  }

	  @Override
	  public void onUpgrade(SQLiteDatabase _db, int _oldVersion, int _newVersion) {
	    Log.w("TaskDBAdapter", "Upgrading from version " + 
	                           _oldVersion + " to " +
	                           _newVersion + ", which will destroy all old data");

	    // Drop the old table.
	    _db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
	    // Create a new one.
	    onCreate(_db);
	  }
	}
}
