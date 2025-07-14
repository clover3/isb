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
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

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

        Log.i("newm","count lines: " + lines.length);

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

        String plan = "";
        try {
            ss = lines[3].split("\\[\\d+;\\dH", 2);
            plan = ss[1];
            if (lines.length >= 4) {
                ss = Arrays.copyOfRange(lines, 4, lines.length);
                plan = plan + "\n" + TextUtils.join("\n", ss).replaceAll("^\\033\\[K", "");
            }
            //plan = plan_first_line + "\n" + plan_rest_lines;
            //Log.i("newm", "plain plan: " + plan);
            plan = plan.replaceAll("\n\r", "\n");
            plan = plan.split("\\033\\[80;1H")[0];
            //plan = plan.replaceAll("\\033\\[K", "\n");
            plan = plan.replaceAll("\\033\\[K", "\n");
            plan = plan.replaceAll("\\033\\[\\d+;\\d+H", "\n\n");
            //Log.i("newm", "final line: " + plan);
        }
        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Failed to load the plan.",
                    Toast.LENGTH_SHORT).show();
        }

        String [] ret = {name, email, login, posting, last, where, newmail, plan};

        return ret;
    }

    private static String formatHexDump(byte[] array, int offset, int length) {
        final int width = 16;

        StringBuilder builder = new StringBuilder();

        for (int rowOffset = offset; rowOffset < offset + length; rowOffset += width) {
            builder.append(String.format("%06d:  ", rowOffset));

            for (int index = 0; index < width; index++) {
                if (rowOffset + index < array.length) {
                    builder.append(String.format("%02x ", array[rowOffset + index]));
                } else {
                    builder.append("   ");
                }
            }

            if (rowOffset < array.length) {
                int asciiWidth = Math.min(width, array.length - rowOffset);
                builder.append("  |  ");
                for (int i = rowOffset; i < asciiWidth + rowOffset; i++) {
                    int c = (int)array[i];
                    if (c >= 0x20 && c <= 0x7e)
                        builder.append(String.format("%c", array[i]));
                    else
                        builder.append(".");
                }
            }

            builder.append(String.format("%n"));
        }

        return builder.toString();
    }
    private void queryUser() {
        String user_id = userID.getText().toString();
        try {
            if (isb.isMain()) {
                String result = isb.queryUser(user_id);

                final String no_such_user = "No such user!";
                String []s = result.split("이  름:");
                String []s_id_tmp = s[0].split("\\033\\[2;11H");
                String id_result;
                if (s_id_tmp.length > 1)
                    id_result = s_id_tmp[1];
                else {
                    id_result = s[0].split("사용자명: ")[1];
                }
                id_result = id_result.split("[\\000\\033]")[0].trim();

                if (s.length < 2) {
                    queryResult.setText(no_such_user);
                    return;
                }
                String [] parsed = parseResult(s[1]);
                String name = parsed[0], email = parsed[1], login = parsed[2],
                        posting = parsed[3], last = parsed[4],
                        where = parsed[5], newmail = parsed[6], plan = parsed[7];
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
                output += newmail + "\n\n";
                output += plan;
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
