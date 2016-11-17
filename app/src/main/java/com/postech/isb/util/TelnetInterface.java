package com.postech.isb.util;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jcraft.jsch.*;

/**
 * Created by Clover on 2016-11-17.
 */
public class TelnetInterface {
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
        String ret = new String();
        String block;

        byte[] tmp=new byte[1024];

        boolean fKeepRead = true;
        while(fKeepRead) {
            int n_read = in.read(tmp, 0, 1024);
            block = new String(tmp, 0, n_read, charset);
            ret += block;
            for( String r : matches) {
                if (ret.matches(r)) {
                    fKeepRead = false;
                    break;
                }
            }
        }
        return ret;
    }

    public String waitfor(String match) throws IOException {
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

    public void send_wo_r(String cmd) throws IOException {
        Log.i("isb", "send_wo_r(" + cmd + ")");
        out.write((cmd).getBytes(charset));

        out.flush();
    }

    public void send(String cmd) throws IOException{
        Log.i("isb", "send(" + cmd + ")");
        send_wo_r(cmd + "\r");
    }
}