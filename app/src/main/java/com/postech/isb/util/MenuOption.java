package com.postech.isb.util;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.postech.isb.R;

/**
 * Created by newmbewb on 2017-12-01.
 */
public class MenuOption {
    static public boolean useActionBar = false;

    static public void setUseActionBar(Activity activity) {
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());
        String actionbar = activity.getResources().getString(
                R.string.preference_value_menuoption_actionbar);
        String menuOption = SP.getString("menu_option", "");
        if (menuOption.equals(actionbar))
            useActionBar = true;
        else
            useActionBar = false;
    }

    static public void setTitleBar(Activity activity) {
        if (useActionBar)
            activity.setTheme(R.style.MyYesTitleBar);
    }
    static public void setNoteEditorTitleBar(Activity activity) {
        if (useActionBar)
            activity.setTheme(R.style.NoteEditorTitleBar);
    }
}
