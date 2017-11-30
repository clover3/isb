package com.postech.isb.preference;

import com.postech.isb.PostechIsb;
import com.postech.isb.R;
import com.postech.isb.boardList.BoardList;
import com.postech.isb.boardList.IsbDBAdapter;
import com.postech.isb.readBoard.ReadBoards;
import com.postech.isb.util.IsbSession;
import com.postech.isb.util.IsbThread;
import com.postech.isb.util.ThreadList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

public class PreferenceList extends PreferenceActivity  {
	static final private String FAVORITE_LIST_THREAD_TITLE = "** Favorite board list **";
	static final private String FAVORITE_LIST_REPORT_THREAD_TITLE =
			"** Favorite board restoring report!  **";
	static final private String FAVORITE_LIST_THREAD_FOOTER = "- END -";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferencelist);

		// Delete swipe menu preference: Do not use it anymore
		Preference swipeMenuPreference = findPreference("swipe_menu");
		PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("cat_gesture");
		preferenceCategory.removePreference(swipeMenuPreference);

		// Setup onClick
		Preference favBackup = findPreference("fav_backup");
		favBackup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				new AlertDialog.Builder(PreferenceList.this)
						.setTitle("Backup favorite list")
						.setMessage("Save your favorite list in your diary")
						.setNeutralButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(
											DialogInterface dialog,
											int whichButton) {
										backupFavList();
									}
								}).show();
				return false;
			}
		});

		Preference favRestore = findPreference("fav_restore");
		favRestore.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				restoreFavList();
				return false;
			}
		});

		// Set menu option summary
		ListPreference listpref_menu_option = (ListPreference) findPreference("menu_option");
		changeMenuOptionSummary(listpref_menu_option, listpref_menu_option.getValue());
		listpref_menu_option.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				changeMenuOptionSummary((ListPreference)preference, (String)newValue);
				return true;
			}
		});
	}

	private void changeMenuOptionSummary(ListPreference listpref_menu_option, String menuoption_entry) {
		if (menuoption_entry != null) {
			String summary = null;
			if (menuoption_entry == getResources().getString(R.string.preference_value_menuoption_menubutton))
				summary = getResources().getString(R.string.preference_label_menuoption_menubutton) + ": " +
						getResources().getString(R.string.preference_summary_menuoption_menubutton);
			else if (menuoption_entry.equals(getResources().getString(R.string.preference_value_menuoption_swipe)))
				summary = getResources().getString(R.string.preference_label_menuoption_swipe) + ": " +
						getResources().getString(R.string.preference_summary_menuoption_swipe);
			else if (menuoption_entry == getResources().getString(R.string.preference_value_menuoption_actionbar))
				summary = getResources().getString(R.string.preference_label_menuoption_actionbar) + ": " +
						getResources().getString(R.string.preference_summary_menuoption_actionbar);
			if (summary != null)
				listpref_menu_option.setSummary(summary);
		}
	}

	private void restoreFavList() {
		// Find the last backup list
		IsbSession isb = ((PostechIsb) getApplicationContext()).isb;
		if (isb.isMain()) {
			try {
				ArrayList<ThreadList> page = isb.getLastPageThreadList("diary");
				if (page == null) {
					// This should not be happen
					Toast.makeText(getApplicationContext(),
							"Something wrong! Please ask the developer", Toast.LENGTH_SHORT)
							.show();
					page = new ArrayList<ThreadList>(); // Empty list
				}

				int targetNum = -1;
				do {
					Collections.sort(page);
					ListIterator<ThreadList> li = page.listIterator();

					// Debug
					Log.i("newm", "last: " + page.get(0).num);
					Log.i("newm", "first: " + page.get(page.size() - 1).num);

					while (li.hasNext()) {
						ThreadList t = li.next();
						if (t.title.equals(FAVORITE_LIST_THREAD_TITLE)) {
							targetNum = t.num;
							break;
						}
					}
					if (targetNum == -1) {
						int lastFstIdx = page.get(page.size() - 1).num;
						if (lastFstIdx == 1)
							break;
						// Search next page
						int lstIdx = lastFstIdx -1;
						int fstIdx = lstIdx - ReadBoards.threadPerPage + 1;
						if (fstIdx < 1)
							fstIdx = 1;
						page = isb.getThreadList("diary", fstIdx, lstIdx);
					}
				} while (targetNum == -1);

				if (targetNum == -1) {
					// Failed to find backed up favorite list
					new AlertDialog.Builder(PreferenceList.this)
							.setMessage("Failed to find favorite list in your diary.")
							.setNeutralButton("OK",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int whichButton) {
											// Do nothing
										}
									}).show();
					return;
				}
				final int favListNum = targetNum; // To pass parameter

				// Set DB and preference
				final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
				final IsbDBAdapter boardDBAdapter = new IsbDBAdapter(this);
				boardDBAdapter.open();

				// Confirm restore
				AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
				alertDlg.setMessage("Press OK to restore favorite list. " +
						"Existing favorite boards will remain.");
				alertDlg.setNegativeButton("OK",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
												int which) {
								IsbSession isb = ((PostechIsb) getApplicationContext()).isb;
								try {
									IsbThread t = isb.readThread("diary", favListNum);
									String lines[] = t.contents.split("[\\r\\n]");
									String unrestoredBoards = "";
									for (int i = 0; i < lines.length; i++) {
										String line = lines[i];
										if (line.length() == 0)
											continue;
										if (line.charAt(0) == '#')
											continue;
										if (line.equals(FAVORITE_LIST_THREAD_FOOTER))
											break;

										if (line.charAt(0) == '*') {
											// Add my board
											line = line.substring(1);
											BoardList.setMyboard(settings, line);
										}

										if (boardDBAdapter.existBoardByName(line)) {
											boardDBAdapter.setFavoriteByName(line, true);
										}
										else {
											unrestoredBoards += line + "\n";
										}
									}
									// Finish restoring & report the result
									if (unrestoredBoards.length() == 0) {
										Toast.makeText(getApplicationContext(), "Successfully restored",
												Toast.LENGTH_SHORT).show();
									} else {
										String report = "\n";
										report += "!Warning! Following boards do not exist:\n";
										report += unrestoredBoards;
										isb.writeToBoard("diary", FAVORITE_LIST_REPORT_THREAD_TITLE, report);
										Toast.makeText(getApplicationContext(), "Some favorite boards are not restored. Read the report in your diary",
												Toast.LENGTH_SHORT).show();
									}
								} catch (IOException e) {
									e.printStackTrace();
									Toast.makeText(getApplicationContext(),
											"Connection lost",
											Toast.LENGTH_SHORT).show();
									isb.disconnect();
								} finally {
									// End of DB
									boardDBAdapter.close();
								}
							}
						});
				alertDlg.setPositiveButton("Cancle",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
												int which) {
								boardDBAdapter.close();
							}
						});
				alertDlg.show();

				// Start to restore favorite list
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

	private void backupFavList() {
		// Init ISB session and boardDBAdapter
		IsbDBAdapter boardDBAdapter = new IsbDBAdapter(this);
		boardDBAdapter.open();
		Cursor boardListCursor = boardDBAdapter.getAllBoardCursor();
		startManagingCursor(boardListCursor);
		IsbSession isb = ((PostechIsb) getApplicationContext()).isb;

		// Start to backup favorite list
		boardListCursor.requery();
		List<String> favList = new ArrayList<String>();
		if (boardListCursor.moveToFirst())
		{
			do {
				String name = boardListCursor.getString(IsbDBAdapter.BOARD_COLUMN);
				boolean favorite = boardListCursor.getInt(IsbDBAdapter.FAVORITE_COLUMN) == 1;

				if( favorite ) {
					favList.add(name);
				}
			} while (boardListCursor.moveToNext());

			// Make title and content
			String title = FAVORITE_LIST_THREAD_TITLE;
			String content = "\n";

			content += "# Date: ";
			content += DateFormat.getDateTimeInstance().format(new Date()) + "\n";
			content += "# Do not change the title of this thread.\n";

			// Get 'My board'
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			String myBoard = BoardList.getMyBoard(settings);
			// Write my board
			if (myBoard.length() > 0)
				content += "*" + myBoard + "\n";

			// Write favorite boards
			ListIterator<String> li = favList.listIterator();
			// Iterate in reverse.
			while(li.hasNext()) {
				content += li.next() + "\n";
			}
			content += FAVORITE_LIST_THREAD_FOOTER;

			// Write to diary
			if (isb.isMain()) {
				try {
					if (isb.writeToBoard("diary", title, content)) {
						Toast.makeText(getApplicationContext(),
								"Write success", Toast.LENGTH_SHORT)
								.show();
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
		else {
			Toast.makeText(getApplicationContext(),
					"Failed..", Toast.LENGTH_SHORT)
					.show();
		}
	}
}
