/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.postech.isb.compose;

import com.postech.isb.R;
import com.postech.isb.R.id;
import com.postech.isb.R.layout;
import com.postech.isb.R.string;
import com.postech.isb.compose.NotePad.Notes;
import com.postech.isb.util.MenuOption;
import com.postech.isb.util.TouchMenuManager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.app.AlertDialog;

/**
 * A generic activity for editing a note in a database.  This can be used
 * either to simply view a note {@link Intent#ACTION_VIEW}, view and edit a note
 * {@link Intent#ACTION_EDIT}, or create a new note {@link Intent#ACTION_INSERT}.  
 */
public class NoteEditor extends Activity {
    private static final String TAG = "Notes";
    public static final String EMPTY_RECEIVER = "To:NoSuchUserBoy!!!!!!";

    /**
     * Standard projection for the interesting columns of a normal note.
     */
    private static final String[] PROJECTION = new String[] {
            Notes._ID, // 0
            Notes.NOTE, // 1
            Notes.TARGET_BOARD, // 2
    };
    /** The index of the note column */
    private static final int COLUMN_INDEX_NOTE = 1;
    private static final int COLUMN_INDEX_TARGET_BOARD = 2;
    
    // This is our state data that is stored when freezing.
    private static final String ORIGINAL_CONTENT = "origContent";

    // Identifiers for our menu items.
    private static final int REVERT_ID = Menu.FIRST;
    private static final int DISCARD_ID = Menu.FIRST + 1;
    private static final int DELETE_ID = Menu.FIRST + 2;
    private static final int FINISH_ID = Menu.FIRST + 3;
    private static final int SAVE_ID = Menu.FIRST + 4;

    // The different distinct states the activity can be run in.
    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;

    private int mState;
    private boolean mNoteOnly = false; // FIXME: What is it????????
    private Uri mUri;
    private Cursor mCursor;
    private EditText mText;
    private String mOriginalContent;
    
    private static final String NOBOARD = "Select a target board"; 
    private String mTargetBoard;
    private Button mButton;
    private String board;
    private boolean editFromBoard = false;
    
    /** Request code */
    public static final int PICK_BOARD = 1;

    
	private TouchMenuManager menuMan; 
    /**
     * A custom EditText that draws lines between each line of text that is displayed.
     */
    public static class LinedEditText extends EditText {
        private Rect mRect;
        private Paint mPaint;

        // we need this constructor for LayoutInflater
        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);
            
            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            int count = getLineCount();
            Rect r = mRect;
            Paint paint = mPaint;

            for (int i = 0; i < count; i++) {
                int baseline = getLineBounds(i, r);

                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
            }

            super.onDraw(canvas);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MenuOption.setUseActionBar(this);
        MenuOption.setNoteEditorTitleBar(this);
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //Modified by newmbewb
        // Do some setup based on the action being performed.

        final String action = intent.getAction();
        if (Intent.ACTION_EDIT.equals(action)) {
            // Requested to edit: set that state, and the data being edited.
            mState = STATE_EDIT;
            mUri = intent.getData();
        } else if (Intent.ACTION_INSERT.equals(action)) {
            // Requested to insert: set that state, and create a new entry
            // in the container.
            mState = STATE_INSERT;
                        
            ContentValues newNote = new ContentValues();
            String targetBoard = intent.getStringExtra("board");
            editFromBoard = intent.getBooleanExtra("edit", false);
            board = targetBoard;
            if (targetBoard != null && targetBoard.equals("mail")) {
                // When board == mail, targetBoard means the list of receiver separated by spaces.
                targetBoard = EMPTY_RECEIVER;
            }
            if (targetBoard != null)
            	newNote.put(NotePad.Notes.TARGET_BOARD, targetBoard);
            else {
            	newNote.put(NotePad.Notes.TARGET_BOARD, NOBOARD);
            	Intent intentForBoard = new Intent(Intent.ACTION_PICK, Uri.parse("content://boards/"));
            	startActivityForResult(intentForBoard, PICK_BOARD);
            }
            String note = intent.getStringExtra("note");
            if (note != null)
            	newNote.put(NotePad.Notes.NOTE, note);
            
            mUri = getContentResolver().insert(intent.getData(), newNote);
            
            // If we were unable to create a new note, then just finish
            // this activity.  A RESULT_CANCELED will be sent back to the
            // original activity if they requested a result.
            if (mUri == null) {
                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());
                finish();
                return;
            }

            // The new entry was created, so assume all will end well and
            // set the result to be returned.
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));
            Log.i("debug", mUri.toString());

        } else {
            // Whoops, unknown action!  Bail.
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        // Set the layout for this activity.  You can find it in res/layout/note_editor.xml
        setContentView(R.layout.note_editor);
        
        // The text view for our note, identified by its ID in the XML file.
        mText = (EditText) findViewById(R.id.note);

        // Touch Menu Call Handler

        menuMan = new TouchMenuManager(this);
        mText.setOnTouchListener(menuMan.MyTouchListener);
        
        // Get the note!
        mCursor = managedQuery(mUri, PROJECTION, null, null, null);
        
        mButton = (Button) findViewById(R.id.targetBoard);
        mButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                if (board.equals("mail")) {
                    final EditText recver = new EditText(NoteEditor.this);
                    if (!mTargetBoard.equals(EMPTY_RECEIVER))
                        recver.setText(mTargetBoard);

                    new AlertDialog.Builder(NoteEditor.this)
                            .setTitle("To:")
                            .setMessage("Separate ID by space (Max 10).")
                            .setView(recver)
                            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String ids = recver.getText().toString();
                                    mTargetBoard = ids;
                                    mButton.setText(mTargetBoard);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            })
                            .show();

                }
                else if (board.equals("diary")) {
                    Toast.makeText(getApplicationContext(),
                            "Diary!",
                            Toast.LENGTH_SHORT).show();
                }
                else {
                    Intent intentForBoard = new Intent(Intent.ACTION_PICK, Uri.parse("content://boards/"));
                    startActivityForResult(intentForBoard, PICK_BOARD);
                }
			}
		});
        
        // If an instance of this activity had previously stopped, we can
        // get the original text it started with.
        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mCursor = getContentResolver().query(mUri, PROJECTION, null, null, null);
        // If we didn't have any trouble retrieving the data, it is now
        // time to get at the stuff.
        if (mCursor != null) {
            // Make sure we are at the one and only row in the cursor.
            mCursor.moveToFirst();

            // Modify our overall title depending on the mode we are running in.
            if (mState == STATE_EDIT) {
                setTitle(getText(R.string.title_edit));
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.title_create));
            }

            // When board == mail, mTargetBoard means the list of receiver separated by spaces.
            if (mTargetBoard == null)
            	mTargetBoard = mCursor.getString(COLUMN_INDEX_TARGET_BOARD);
            if (mTargetBoard.equals(EMPTY_RECEIVER))
                mButton.setText("To:");
            else
                mButton.setText(mTargetBoard);
            if (mTargetBoard.contains("/")) {
                // board like name
                board = mTargetBoard;
            }
            else if (mTargetBoard.equals("diary")) {
                board = "diary";
            }
            else {
                board = "mail";
            }
            
            // This is a little tricky: we may be resumed after previously being
            // paused/stopped.  We want to put the new text in the text view,
            // but leave the user where they were (retain the cursor position
            // etc).  This version of setText does that for us.
            String note = mCursor.getString(COLUMN_INDEX_NOTE);
            mText.setTextKeepState(note);
            
            // If we hadn't previously retrieved the original text, do so
            // now.  This allows the user to revert their changes.
            if (mOriginalContent == null) {
                mOriginalContent = note;
            }

        } else {
            setTitle(getText(R.string.error_title));
            mText.setText(getText(R.string.error_message));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save away the original text, so we still have it if the activity
        // needs to be killed while paused.
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // The user is going somewhere else, so make sure their current
        // changes are safely saved away in the provider.  We don't need
        // to do this if only editing.
        if (mCursor != null) {
            String text = mText.getText().toString();
            int length = text.length();

            // If this activity is finished, and there is no text, then we
            // do something a little special: simply delete the note entry.
            // Note that we do this both for editing and inserting...  it
            // would be reasonable to only do it when inserting.
            if (isFinishing() && (length == 0) && !mNoteOnly) {
                setResult(RESULT_CANCELED);
                deleteNote();

            // Get out updates into the provider.
            } else {
                ContentValues values = new ContentValues();

                // This stuff is only done when working with a full-fledged note.
                if (!mNoteOnly) {
                    // Bump the modification time to now.
                    values.put(Notes.MODIFIED_DATE, System.currentTimeMillis());

                    String[] split_result = text.split("\n");
                    String title;
                    if (split_result.length == 0)
                        title = "";
                    else
                        title = split_result[0];
                    length = title.length();
                    title = title.substring(0, Math.min(30, length));
                    if (length > 30) {
                        int lastSpace = title.lastIndexOf(' ');
                        if (lastSpace > 0) {
                            title = title.substring(0, lastSpace);
                        }
                    }
                    values.put(Notes.TITLE, title);
                }

                // Write our text back into the provider.
                values.put(Notes.NOTE, text);
                
                // Write our target board back into the provider.
                // When board == mail, TARGET_BOARD means the list of receiver separated by spaces.
                values.put(Notes.TARGET_BOARD, mTargetBoard);

                // Commit all of our changes to persistent storage. When the update completes
                // the content provider will notify the cursor of the change, which will
                // cause the UI to be updated.
                getContentResolver().update(mUri, values, null, null);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if (MenuOption.useActionBar) {
            if (mState == STATE_EDIT) {
                SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                getMenuInflater().inflate(R.menu.note_editor_edit, menu);
                menu.findItem(id.delete).setEnabled(!SP.getBoolean("disable_delete", false));
                if (mNoteOnly) {
                    menu.findItem(id.save).setVisible(false);
                    menu.findItem(id.delete).setVisible(false);
                }
            }
            else if (editFromBoard)
                getMenuInflater().inflate(R.menu.note_editor_from_board, menu);
            else
                getMenuInflater().inflate(R.menu.note_editor_else, menu);
            return true;
        }
        else {
            // Build the menus that are shown when editing.
            if (mState == STATE_EDIT) {
                menu.add(0, REVERT_ID, 0, R.string.menu_revert)
                        .setShortcut('0', 'r')
                        .setIcon(android.R.drawable.ic_menu_revert);
                if (!mNoteOnly) {
                    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    menu.add(0, SAVE_ID, 0, R.string.menu_save)
                            .setIcon(android.R.drawable.ic_menu_save);
                    menu.add(0, DELETE_ID, 0, R.string.menu_delete)
                            .setShortcut('1', 'd')
                            .setIcon(android.R.drawable.ic_menu_delete)
                            .setEnabled(!SP.getBoolean("disable_delete", false));
                }

                // Build the menus that are shown when inserting.
            } else if (editFromBoard) {
                menu.add(0, FINISH_ID, 0, R.string.menu_finish)
                        .setIcon(android.R.drawable.ic_menu_add);
                menu.add(0, SAVE_ID, 0, R.string.menu_save)
                        .setIcon(android.R.drawable.ic_menu_save);
                menu.add(0, REVERT_ID, 0, R.string.menu_revert)
                        .setShortcut('0', 'r')
                        .setIcon(android.R.drawable.ic_menu_revert);
            } else {
                menu.add(0, FINISH_ID, 0, R.string.menu_finish)
                        .setIcon(android.R.drawable.ic_menu_add);
                menu.add(0, SAVE_ID, 0, R.string.menu_save)
                        .setIcon(android.R.drawable.ic_menu_save);
                menu.add(0, DISCARD_ID, 0, R.string.menu_discard)
                        .setShortcut('0', 'd')
                        .setIcon(android.R.drawable.ic_menu_delete);
            }


        /*
        // If we are working on a full note, then append to the
        // menu items for any other activities that can do stuff with it
        // as well.  This does a query on the system for any activities that
        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
        // for each one that is found.
        if (!mNoteOnly) {
            Intent intent = new Intent(null, getIntent().getData());
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                    new ComponentName(this, NoteEditor.class), null, intent, 0, null);
        }
        */

            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        if (MenuOption.useActionBar) {
            switch (item.getItemId()) {
                case R.id.finish:
                    finish();
                    break;
                case R.id.delete:
                    deleteNote();
                    finish();
                case R.id.discard:
                    cancelNote();
                    break;
                case R.id.save:
                    saveNote();
                    break;
                case R.id.revert:
                    cancelNote();
                    break;
            }
        }
        else {
            switch (item.getItemId()) {
                case FINISH_ID:
                    finish();
                    break;
                case DELETE_ID:
                    deleteNote();
                    finish();
                    break;
                case DISCARD_ID:
                    cancelNote();
                    break;
                case SAVE_ID:
                    saveNote();
                    break;
                case REVERT_ID:
                    cancelNote();
                    break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Take care of canceling work on a note.  Deletes the note if we
     * had created it, otherwise reverts to the original text.
     */
    private final void cancelNote() {
        if (mCursor != null) {
            if (mState == STATE_EDIT) {
                // Put the original note text back into the database
                mCursor.close();
                mCursor = null;
                ContentValues values = new ContentValues();
                values.put(Notes.NOTE, mOriginalContent);
                getContentResolver().update(mUri, values, null, null);
            } else if (mState == STATE_INSERT) {
                // We inserted an empty note, make sure to delete it
                deleteNote();
            }
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    private final void saveNote() {
        Toast.makeText(getApplicationContext(),
                "Saved!", Toast.LENGTH_SHORT)
                .show();
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * Take care of deleting a note.  Simply deletes the entry.
     */
    private final void deleteNote() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            getContentResolver().delete(mUri, null, null);
            mText.setText("");
        }
    }
    
    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
    	super.onActivityResult(reqCode, resCode, data);
    	
    	switch (reqCode) {
    	case (PICK_BOARD) : {
    		if (resCode == Activity.RESULT_OK) {
    			mTargetBoard = data.getStringExtra("board");
    			mButton.setText(mTargetBoard);
    		}
    		break;
    	}
    	}
    }
}
