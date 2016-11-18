package com.postech.isb.util;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

import com.jcraft.jsch.*;

/**
 * Created by Clover on 2016-11-17.
 */
public class TelnetInterface {
    static final int WAITFOR = 3;
    static final int SEND = 6;

    private JSch jsch;
    private Session session;
    private ChannelShell channel;
    static final String charset = "MS949";

    private OutputStream out;
    private InputStream in;

    static final int ROWS = 80;
    static final int COLS = 40;
    static final String username = "bbs";

    TelnetInterface(final int rows, final int cols) {
        jsch=new JSch();
    }

    public void connect(String host, int port) throws Exception {
        session = jsch.getSession(username, host, port);

        class MyUserInfo
                implements UserInfo, UIKeyboardInteractive{
            public String getPassword(){ return null; }
            public boolean promptYesNo(String str){ return true; }
            public String getPassphrase(){ return null; }
            public boolean promptPassphrase(String message){ return false; }
            public boolean promptPassword(String message){ return false; }
            public void showMessage(String message){ }
            public String[] promptKeyboardInteractive(String destination,
                                                      String name,
                                                      String instruction,
                                                      String[] prompt,
                                                      boolean[] echo){
                return null;
            }
        }

        session.setUserInfo(new MyUserInfo());
        session.connect(30000);
        channel=(ChannelShell)session.openChannel("shell");
        channel.setPtySize(COLS, ROWS, COLS*8, ROWS*12);
        in = channel.getInputStream();
        out = channel.getOutputStream();
        channel.connect();
    }

    public void disconnect() throws IOException {
        session.disconnect();
    }
    public void flush() throws IOException {
        out.flush();
    }

    public String waitfor(String[] matches) throws IOException {
        if (matches == null)
            return null;
        if (matches.length == 0)
            return null;

        String oneMatch = TextUtils.join("|", matches);
        return waitfor(oneMatch);
    }

    public String waitfor(String match) throws IOException {
        Holder<String> ret = new Holder<String>();
        SshAsyncTask task = new SshAsyncTask(match, ret);
        IOException e;
        try {
            e = task.execute(WAITFOR).get();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return null;
        } catch (ExecutionException e1) {
            e1.printStackTrace();
            return null;
        }
        if (e != null)
            throw e;
        return ret.value;
    }

    public void send_wo_r(String cmd) throws IOException {
        Log.i("isb", "send_wo_r(" + cmd + ")");
        SshAsyncTask task = new SshAsyncTask(cmd);
        IOException e;
        try {
            e = task.execute(SEND).get();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return;
        } catch (ExecutionException e1) {
            e1.printStackTrace();
            return;
        }
        if (e != null)
            throw e;
    }

    public void send(String cmd) throws IOException{
        Log.i("isb", "send(" + cmd + ")");
        send_wo_r(cmd + "\r");
    }

    public class Holder<T>
    {
        public T value;
    }
    public class SshAsyncTask extends AsyncTask<Integer, Void, IOException> {

        byte[] b;
        String s;
        String[] sa;
        Holder<String> ret_s;

        public void raw_send(String cmd) throws IOException {
            out.write((cmd).getBytes(charset));
            out.flush();
        }

        public String raw_waitfor(String match) throws IOException {
            String ret = new String();
            String block;

            byte[] tmp=new byte[1024];
            Log.i("isb", "waitfor(" + match + ")");
            boolean fKeepRead = true;
            while(fKeepRead) {
                int n_read = in.read(tmp, 0, 1024);
                block = new String(tmp, 0, n_read, charset);
                Log.i("isb", block);
                ret += block;
                if (ret.matches(match)) {
                    break;
                }
            }
            return ret;
        }

        public SshAsyncTask(String cmd) {
            s = cmd;
        }

        public SshAsyncTask(String cmd, Holder<String> ret) {
            s = cmd;
            ret_s = ret;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected IOException doInBackground(Integer... cmnd) {
            switch(cmnd[0]){
                case SEND:
                    try {
                        raw_send(s);
                    } catch (IOException e) {
                        return e;
                    }
                    break;
                case WAITFOR:
                    try {
                        ret_s.value = raw_waitfor(s);
                    } catch (IOException e) {
                        return e;
                    }
                    break;
            }
            return null;
        }

    }
}