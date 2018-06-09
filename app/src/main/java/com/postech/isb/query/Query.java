package com.postech.isb.query;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
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

    private String [] parseResult(String s) {
        String [] lines = s.split("\n\r");
        String [] ss;
        String name, email, login, posting, last, where, newmail;
        String newmail_message = "새 편지";

        ss = lines[0].replace("\033[K", "").split("이메일:");
        name = ss[0].replace("이  름:", "").trim();
        email = ss[1].trim();

        ss = lines[1].replace("\033[K", "").split("포스팅:");
        login = ss[0].replace("로그인:", "").trim();
        posting = ss[1].trim();

        last = lines[2].replace("\033[K", "").replace("마지막:", "").trim();

        ss = lines[3].replace("\033[K", "").split(newmail_message);
        where = ss[0].replace("어디서:", "").trim();
        newmail = newmail_message + ss[1].split("\\.")[0] + ".";

        String [] ret = {name, email, login, posting, last, where, newmail};

        return ret;
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    private void queryUser() {
        String user_id = userID.getText().toString();
        try {
            if (isb.isMain()) {
                String result = isb.queryUser(user_id);
                final String no_such_user = "No such user";
                String []s = result.split("\033\\[4;1H");
                String id_result = s[0].split("\\033\\[2;11H")[1].trim();
                Log.i("newm", "id: "+ id_result);
                if (s.length < 2) {
                    queryResult.setText(no_such_user);
                    return;
                }
                String [] parsed = parseResult(s[1]);
                String name = parsed[0], email = parsed[1], login = parsed[2],
                        posting = parsed[3], last = parsed[4],
                        where = parsed[5], newmail = parsed[6];
                if (!id_result.equals(user_id)) {
                    queryResult.setText(no_such_user);
                    return;
                }
                String output = "";
                output += "이름: " + name + "\n";
                output += "이메일: " + email + "\n";
                output += "로그인: " + login + "\n";
                output += "포스팅: " + posting + "\n";
                output += "마지막: " + last + "\n";
                output += "어디서: " + where + "\n";
                output += newmail;
                queryResult.setText(output);
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
