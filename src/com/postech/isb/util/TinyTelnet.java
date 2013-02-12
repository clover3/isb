package com.postech.isb.util;

/*
 * This file is part of "JTA - Telnet/SSH for the JAVA(tm) platform".
 *
 * (c) Matthias L. Jugel, Marcus Mei©¬ner 1996-2005. All Rights Reserved.
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


/**
 * To write a program using the wrapper
 * you may use the following piece of code as an example:
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id: Wrapper.java 499 2005-09-29 08:24:54Z leo $
 * @author Matthias L. Jugel, Marcus Mei©¬ner
 */


public class TinyTelnet {
	static final int ROWS = 80;
	static final int COLS = 40;
	static final int [] WINDOW_SIZE = {ROWS, COLS};
	static final String charset = "MS949";
	
	protected final int port = 23;
	protected final int timeout = 5000;			// Socket open try timeout
	protected final int readTimeout = 1500;		// Socket read timeout // Retry 5¹ø.

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
	public void connect(String host, int port) throws IOException {
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
			disconnect();
			throw ((IOException)e);
		}
	}  
	/** Disconnect the socket and close the connection. */
	public void disconnect() throws IOException {
		if (socket != null)
			socket.close();
	}

	public void flush() throws IOException {
		if (out != null)
			out.flush();
	}

	public void setNeged(boolean set) {
		isNeged = set;
	}

	public String waitfor(String match) throws IOException {
		String ret = new String();
		String block;
		
		while(true) {
			block = new String(read(), charset);
			ret += block;

			if (ret.matches(match))
				break;
		}

		return ret;
	}
	
	public String waitfor(String [] matches) throws IOException {
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
		
		return waitfor(oneMatch.toString());
	}
	
	public byte [] read() throws IOException{
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
	public void send(String cmd) throws IOException {
		byte arr[];
		arr = (cmd + "\r").getBytes(handler.getCharsetName());
		handler.transpose(arr);
	}

	public void send_wo_r(String cmd) throws IOException {
		byte arr[];
		arr = cmd.getBytes(handler.getCharsetName());
		handler.transpose(arr);
	}

	public byte[] negotiate() throws IOException {
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

}