/*
 * JPty - A small PTY interface for Java.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package jpty.macosx;

import static jtermios.JTermios.B38400;
import static jtermios.JTermios.CLOCAL;
import static jtermios.JTermios.CREAD;
import static jtermios.JTermios.CS8;
import static jtermios.JTermios.ECHO;
import static jtermios.JTermios.ECHOE;
import static jtermios.JTermios.ICANON;
import static jtermios.JTermios.ISIG;
import static jtermios.JTermios.IXOFF;
import static jtermios.JTermios.IXON;
import static jtermios.JTermios.OPOST;
import static jtermios.JTermios.VEOF;
import static jtermios.JTermios.VSTART;
import static jtermios.JTermios.VSTOP;
import jpty.JPty.JPtyInterface;
import jpty.WinSize;
import jtermios.macosx.JTermiosImpl.MacOSX_C_lib.Termios;

import com.sun.jna.Native;
import com.sun.jna.StringArray;
import com.sun.jna.Structure;

/**
 * Provides a {@link JPtyInterface} implementation for MacOSX.
 */
public class JPtyImpl implements JPtyInterface {
    // INNER TYPES
    
    public static class winsize extends Structure {
        public short ws_row;
        public short ws_col;
        public short ws_xpixel;
        public short ws_ypixel;

        public winsize() {
        }

        public winsize(WinSize ws) {
            ws_row = ws.ws_row;
            ws_col = ws.ws_col;
            ws_xpixel = ws.ws_xpixel;
            ws_ypixel = ws.ws_ypixel;
        }
        
        public void update(WinSize winSize) {
            winSize.ws_col = ws_col;
            winSize.ws_row = ws_row;
            winSize.ws_xpixel = ws_xpixel;
            winSize.ws_ypixel = ws_ypixel;
        }
    }
    
    public interface MacOSX_C_lib extends com.sun.jna.Library {
        public int execv(String command, StringArray argv);
        
        public int execve(String command, StringArray argv, StringArray env);

        public int forkpty(int[] amaster, byte[] name, Termios termp, winsize winp);

        public int ioctl(int fd, int cmd, winsize data);

        public int waitpid(int pid, int[] stat, int options);
    }
    
    // CONSTANTS

    private static final int TIOCGWINSZ = 0x40087468;
    private static final int TIOCSWINSZ = 0x80087467;

    private static final int ONLCR = 0x02;

    private static final int VERASE = 3;
    private static final int VWERASE = 4;
    private static final int VKILL = 5;
    private static final int VREPRINT = 6;
    private static final int VINTR = 8;
    private static final int VQUIT = 9;
    private static final int VSUSP = 10;
    
    private static final int ECHOKE = 0x01;
    private static final int ECHOCTL = 0x40;

    // VARIABLES
    
    private static MacOSX_C_lib m_Clib = (MacOSX_C_lib) Native.loadLibrary("c", MacOSX_C_lib.class);

    // METHODS
     
    @Override
    public int execve(String command, String[] argv, String[] env) {
        StringArray argvp = (argv == null) ? new StringArray(new String[] { command }) : new StringArray(argv);
        StringArray envp = (env == null) ? null : new StringArray(env);
        return m_Clib.execve(command, argvp, envp);
    }
    
    @Override
    public int forkpty(int[] amaster, byte[] name, jtermios.Termios term, WinSize win) {
        Termios termp = (term == null) ? new Termios() : new Termios(term);
        winsize winp = (win == null) ? null : new winsize(win);
        return m_Clib.forkpty(amaster, name, termp, winp);
    }
    
    @Override
    public jtermios.Termios getDefaultTermios() {
        jtermios.Termios result = new jtermios.Termios();
        result.c_iflag = IXON | IXOFF;
        result.c_oflag = OPOST | ONLCR;
        result.c_cflag = CS8 | CREAD | CLOCAL | B38400;
        result.c_lflag = ICANON | ISIG | ECHO | ECHOE | ECHOKE | ECHOCTL;
        result.c_cc[VSTART] = 'Q' & 0x1f;
        result.c_cc[VSTOP] = 'S' & 0x1f;
        result.c_cc[VERASE] = 0x7f;
        result.c_cc[VKILL] = 'U' & 0x1f;
        result.c_cc[VINTR] = 'C' & 0x1f;
        result.c_cc[VQUIT] = '\\' & 0x1f;
        result.c_cc[VEOF] = 'D' & 0x1f;
        result.c_cc[VSUSP] = 'Z' & 0x1f;
        result.c_cc[VWERASE] = 'W' & 0x1f;
        result.c_cc[VREPRINT] = 'R' & 0x1f;
        return result;
    }
    
    @Override
    public int getWinSize(int fd, WinSize winSize) {
        int r;
        
        winsize ws = new winsize();
        if ((r = m_Clib.ioctl(fd, TIOCGWINSZ, ws)) < 0) {
            return r;
        }
        ws.update(winSize);

        return r;
    }

    @Override
    public int setWinSize(int fd, WinSize winSize) {
        winsize ws = new winsize(winSize);
        return m_Clib.ioctl(fd, TIOCSWINSZ, ws);
    }
    
    @Override
    public int waitpid(int pid, int[] stat, int options) {
        return m_Clib.waitpid(pid, stat, options);
    }
}
