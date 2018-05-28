package com.postech.isb.query;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.postech.isb.PostechIsb;
import com.postech.isb.R;
import com.postech.isb.util.IsbSession;

import java.io.IOException;

/**
 * Created by newmbewb on 2018-05-29.
 */
public class Query extends Activity {
    private IsbSession isb;
    private Button search;
    private TextView userID;
    private TextView queryResult;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isb = ((PostechIsb) getApplicationContext()).isb;
        setContentView(R.layout.query);
        getActionBar().setTitle("Query");


        search = (Button) findViewById(R.id.search);
        userID = (TextView) findViewById(R.id.userID);
        queryResult = (TextView) findViewById(R.id.queryResult);

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                queryUser();
            }
        });
    }

    private void queryUser() {
        String user_id = userID.getText().toString();
        try {
            if (isb.isMain()) {
                String result = isb.queryUser(user_id);
                final String no_such_user = "No such user";
                String []s = result.split("\033\\[4;1H");
                if (s.length < 2) {
                    queryResult.setText(no_such_user);
                    return;
                }
                queryResult.setText(s[1]);
            }
            else {
                Toast.makeText(getApplicationContext(), "Login first plz...",
                        Toast.LENGTH_SHORT).show();
            }
        }
        catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Connection lost!",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
