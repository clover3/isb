package com.postech.isb.util;

/*
 * This file is part of "JTA - Telnet/SSH for the JAVA(tm) platform".
 *
 * (c) Matthias L. Jugel, Marcus Meißner 1996-2005. All Rights Reserved.
 *
 * Please visit http://javatelnet.org/ for updates and contact.
 *
 * --LICENSE NOTICE--
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * --LICENSE NOTICE--
 *
 */

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutionException;

import com.postech.isb.util.TinyTelnet.Holder;

import android.os.AsyncTask;
import android.util.Log;


/**
 * To write a program using the wrapper
 * you may use the following piece of code as an example:
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id: Wrapper.java 499 2005-09-29 08:24:54Z leo $
 * @author Matthias L. Jugel, Marcus Meißner
 */


public class TinyTelnet {
	static final int ROWS = 80;
	static final int COLS = 40;
	static final int [] WINDOW_SIZE = {ROWS, COLS};
	static final String charset = "MS949";
	
	static final int CONNECT = 0;
	static final int DISCONNECT = 1;
	static final int FLUSH = 2;
	static final int WAITFOR = 3;
	static final int WAITFOR_ARRAY = 4;
	static final int READ = 5;
	static final int SEND = 6;
	static final int SEND_WO_R = 7;
	static final int NEGOTIATE = 8;

	
	protected final int port = 23;
	protected final int timeout = 5000;			// Socket open try timeout
	protected final int readTimeout = 1500;		// Socket read timeout // Retry 5번.

	/** debugging level */
	private final static int debug =0;

	protected InputStream in;
	protected BufferedInputStream bin;
	protected OutputStream out;
	protected Socket socket;
	protected String host;
	
	protected TelnetProtocolHandler handler;

	private boolean isNeged;
	
	static final int bufSize = 4000;
	protected byte [] buffer = new byte[bufSize];
	
	TinyTelnet() {
		this(ROWS, COLS);
	}
	
	TinyTelnet(final int rows, final int cols) {
		isNeged = false;
		handler = new TelnetProtocolHandler() {
			/** get the current terminal type */
			public String getTerminalType() {
				return "xterm";
			}

			/** get the current window size */
			public int[] getWindowSize() {
				int win_size[] = {rows, cols};
				return win_size;
			}

			/** notify about local echo */
			public void setLocalEcho(boolean echo) {
				/* EMPTY */
			}

			/** write data to our back end */
			public void write(byte[] b) throws IOException {
				out.write(b);
			}

			/** sent on IAC EOR (prompt terminator for remote access systems). */
			public void notifyEndOfRecord() {
			}

			protected String getCharsetName() {
				return charset;
			}
		};
	}

	/** Connect the socket and open the connection. */
	public void connect(String host, int port) throws IOException{
		TelnetAsyncTask task = new TelnetAsyncTask(host, port);
		IOException e;
		try {
			e = task.execute(CONNECT).get();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		if (e != null)
			throw e;
	}
	public void connect_async(String host, int port) throws IOException {
		try {
			SocketAddress socketAddress = new InetSocketAddress(host, port);
			socket = new Socket();
			socket.setSoTimeout(readTimeout); 
			socket.connect(socketAddress, timeout);
			
			in = socket.getInputStream();
			out = socket.getOutputStream();
			
			bin = new java.io.BufferedInputStream(in);
			handler.reset();
		} catch (SocketException se) {
			System.err.println("Wrapper: Could not set timeout: " + se.getMessage());
			socket = null;
			throw se;
		} catch(Exception e) {
			System.err.println("Wrapper: "+e);
			disconnect_async();
			throw ((IOException)e);
		}
	}  
	/** Disconnect the socket and close the connection. */
	public void disconnect() throws IOException{
		TelnetAsyncTask task = new TelnetAsyncTask();
		IOException e;
		try {
			e = task.execute(DISCONNECT).get();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		if (e != null)
			throw e;
	}
	public void disconnect_async() throws IOException {
		if (socket != null)
			socket.close();
	}

	public void flush() throws IOException{
		TelnetAsyncTask task = new TelnetAsyncTask();
		IOException e;
		try {
			e = task.execute(FLUSH).get();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		if (e != null)
			throw e;
	}
	public void flush_async() throws IOException {
		if (out != null)
			out.flush();
	}

	public void setNeged(boolean set) {
		isNeged = set;
	}

	public String waitfor(String match) throws IOException{
		Holder<String> ret = new Holder<String>();
		TelnetAsyncTask task = new TelnetAsyncTask(match);
		task.setString(ret);
		IOException e;
		try {
			e = task.execute(WAITFOR).get();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
		if (e != null)
			throw e;
		return ret.value;
	}
	public String waitfor_async(String match) throws IOException {
		String ret = new String();
		String block;
		
		while(true) {
			block = new String(read_async(), charset);
			ret += block;
			if (ret.matches(match))
				break;
		}

		return ret;
	}

	public String waitfor(String[] matches) throws IOException{
		Holder<String> ret = new Holder<String>();
		TelnetAsyncTask task = new TelnetAsyncTask(matches);
		task.setString(ret);
		IOException e;
		try {
			e = task.execute(WAITFOR_ARRAY).get();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
		if (e != null)
			throw e;
		return ret.value;
	}
	public String waitfor_async(String [] matches) throws IOException {
		StringBuffer oneMatch = new StringBuffer();
		int i;
		
		if (matches == null)
			return null;
		if (matches.length == 0)
			return null;
		
		for (i = 0; i < matches.length-1; i++) {
			oneMatch.append(matches[i]);
			oneMatch.append("|");
		}
		oneMatch.append(matches[i]);
		
		return waitfor_async(oneMatch.toString());
	}
	
	public class Holder<T>
	{
	    public T value;
	}
	public byte [] read() throws IOException{
		Holder<byte[]> ret = new Holder<byte[]>();
		TelnetAsyncTask task = new TelnetAsyncTask(ret);
		IOException e;
		try {
			e = task.execute(READ).get();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
		if (e != null)
			throw e;
		return ret.value;
	}
	public byte [] read_async() throws IOException{
		byte [] result;
		int len = 0;

		try {
			len = bin.read(buffer, 0, 4000);
		} catch (SocketTimeoutException e) {
			;  
		}
		
		if (len < 0)
			throw new IOException("End of buffer");			

		result = new byte[len];

		System.arraycopy(buffer, 0, result, 0, len);

		return result;
	}

	/**
	 * Send a command to the remote host. A newline is appended and if
	 * a prompt is set it will return the resulting data until the prompt
	 * is encountered.
	 * @param cmd the command
	 * @return output of the command or null if no prompt is set
	 */
	public void send(String cmd) throws IOException{
		TelnetAsyncTask task = new TelnetAsyncTask(cmd);
		IOException e;
		try {
			e = task.execute(SEND).get();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		if (e != null)
			throw e;
	}
	public void send_async(String cmd) throws IOException {
		byte arr[];
		arr = (cmd + "\r").getBytes(handler.getCharsetName());
		handler.transpose(arr);
	}

	public void send_wo_r(String cmd) throws IOException {
		TelnetAsyncTask task = new TelnetAsyncTask(cmd);
		IOException e;
		try {
			e = task.execute(SEND_WO_R).get();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		if (e != null)
			throw e;
	}
	public void send_wo_r_async(String cmd) throws IOException  {
		byte arr[];
		arr = cmd.getBytes(handler.getCharsetName());
		handler.transpose(arr);
	}

	public byte[] negotiate() throws IOException{
		Holder<byte[]> ret = new Holder<byte[]>();
		TelnetAsyncTask task = new TelnetAsyncTask(ret);
		IOException e;
		try {
			e = task.execute(NEGOTIATE).get();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
		if (e != null)
			throw e;
		return ret.value;
	}
	public byte[] negotiate_async() throws IOException {
		/* process all already read bytes */
		int n;
		byte[] b = new byte[1];

		do {
			n = in.read(b);
						
			if (n < 0) {
				System.err.println("negotiate: Connection lost?");
				isNeged = false;
				return null;
			}		   
			
			handler.inputfeed(b, 0, n); // is it really 0?
			n = handler.negotiate(b, 0);
		} while (n <= 0);

		isNeged = true;
		return b;
	}
	
	public class TelnetAsyncTask extends AsyncTask<Integer, Void, IOException> {
		byte[] b;
		String s;
		String[] sa;
		Holder<String> ret_s;
		Holder<byte[]> ret_byte;
		String host;
		int port;

        public void setString(Holder<String> ret) {
        	ret_s = ret;
		}

		public TelnetAsyncTask(String host2, int port2) {
			host = host2;
			port = port2;
		}

		public TelnetAsyncTask() {
			// Maybe Nothing?
		}

		public TelnetAsyncTask(String match) {
			s = match;
		}

		public TelnetAsyncTask(String[] matches) {
			sa = matches;
		}

		public TelnetAsyncTask(Holder<byte[]> ret) {
			ret_byte = ret;
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
			case CONNECT:
				try {
					connect_async(host, port);
				} catch (IOException e) {
					return e;
				}
				break;
			case DISCONNECT:
				try {
					disconnect_async();
				} catch (IOException e) {
					return e;
				}
				break;
			case FLUSH:
				try {
					flush_async();
				} catch (IOException e) {
					return e;
				}
				break;
			case WAITFOR:
				try {
					ret_s.value = waitfor_async(s);
				} catch (IOException e) {
					return e;
				}
				break;
			case WAITFOR_ARRAY:
				try {
					ret_s.value = waitfor_async(sa);
				} catch (IOException e) {
					return e;
				}
				break;
			case SEND:
				try {
					send_async(s);
				} catch (IOException e) {
					return e;
				}
				break;
			case SEND_WO_R:
				try {
					send_wo_r_async(s);
				} catch (IOException e) {
					return e;
				}
				break;
			case NEGOTIATE:
				try {
					ret_byte.value = negotiate_async();
				} catch (IOException e) {
					return e;
				}
				break;
				
			case READ:
				try {
					ret_byte.value = read_async();
				} catch (IOException e) {
					return e;
				}
				break;
			}
			return null;
		}
         
    }

}
