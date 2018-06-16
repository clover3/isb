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
        String newmail_message = "�� ����";

        ss = lines[0].replace("\033[K", "").split("�̸���:");
        name = ss[0].replace("��  ��:", "").trim();
        email = ss[1].trim();

        ss = lines[1].replace("\033[K", "").split("������:");
        login = ss[0].replace("�α���:", "").trim();
        posting = ss[1].trim();

        last = lines[2].replace("\033[K", "").replace("������:", "").trim();

        ss = lines[3].replace("\033[K", "").split(newmail_message);
        where = ss[0].replace("���:", "").trim();
        newmail = newmail_message + ss[1].split("\\.")[0] + ".";

        String [] ret = {name, email, login, posting, last, where, newmail};

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
                String []s = result.split("��  ��:");
                String []s_id_tmp = s[0].split("\\033\\[2;11H");
                String id_result;
                if (s_id_tmp.length > 1)
                    id_result = s_id_tmp[1];
                else {
                    id_result = s[0].split("����ڸ�: ")[1];
                }
                id_result = id_result.split("[\\000\\033]")[0].trim();

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
                output += "�̸�: " + name + "\n";
                output += "�̸���: " + email + "\n";
                output += "�α���: " + login + "\n";
                output += "������: " + posting + "\n";
                output += "������: " + last + "\n";
                output += "���: " + where + "\n";
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
