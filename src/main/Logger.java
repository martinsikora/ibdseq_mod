/*
 * Copyright 2013 Brian L. Browning
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package main;

import blbutil.FileUtil;
import java.io.File;
import java.io.PrintWriter;

/* Code Review on 18 Aug 2013 */

/**
 * Class <code>Logger</code> is a light weight singleton class for writing
 * log message.
 *
 * @author Brian L. Browning <browning@uw.edu>
 */
public class Logger {

    private static final Logger instance = new Logger();

    private File file = null;
    private boolean append;
    private PrintWriter out = null;
    private int logCnt = 0;

    private Logger() {
        // private constructor to prevent instantiation
    }

    /**
     * Returns the singleton Logger instance.
     * @return the singleton Logger instance.
     */
    public static Logger getInstance() {
        return instance;
    }

    /**
     * Replaces the log target at the time of method invocation
     * with the specified log target and returns the log target
     * at the time of method invocation.

     * @param target a log target to which log messages will be written.
     * Either a file or <code>null</code> if log messages should be
     * written to standard out.
     * @paramn append <code>true</code> if log messages should be written
     * to the end of the file, and <code>false</code> if log messages
     * should be written to the beginning of the file, over-writing any
     * existing data.  The target file is not created or modified unless at
     * unless one or more log messages are written. The <code>append</code>
     * parameter has no effect if <code>target==null</code>.
     * @return the current target at the invocation of the method.  The
     * returned value is <code>null</code> if the current log target is
     * standard out.
     */
    public synchronized File setTarget(File target, boolean append) {
        File prevTarget = file;
        this.close();
        this.file = target;
        this.append = append;
        return prevTarget;
    }

    /**
     * Replaces the log target at the time of method invocation
     * with the specified log target and returns the log target
     * at the time of method invocation.  If the log target is a file,
     * log messages will be written to the start of the file,
     * over-writing any existing data.

     * @param target a log target to which log messages will be written.
     * Either a file or <code>null</code> if log messages should be
     * written to standard out.
     * @return the log target at the invocation of the method.  The
     * returned value is <code>null</code> if the log target at the
     * invocation of the method is standard out.
     */
    public synchronized File setTarget(File target) {
        boolean appendFile = false;
        return setTarget(target, appendFile);
    }

    private static PrintWriter open(File file, boolean append) {
        if (file==null) {
            return FileUtil.stdOutPrintWriter();
        }
        else {
            return FileUtil.printWriter(file, append);
        }
    }

    /**
     * Closes the current log target, sets the count of log messages to 0,
     * and sets the log target to standard out.
     */
    public synchronized void close() {
        if (logCnt > 0) {
            if (file!=null) {
                out.close();
                file=null;
            }
            else {
                out.flush();
            }
        }
        out = null;
        logCnt = 0;
    }

    /**
     * Writes the specified log message followed by a new line character.
     * @param msg a log message.
     * @throws NullPointerException if <code>msg==null</code>.
     */
    public synchronized void println(String msg) {
        if (logCnt==0) {
            this.out = open(file, append);
        }
        ++logCnt;
        if (out!=null) {
            out.println(msg);
        }
    }

    /**
     * Writes the specified log message without appending a new line character.
     * @param msg a log message.
     * @throws NullPointerException if <code>msg==null</code>.
     */
    public synchronized void print(String msg) {
        if (logCnt==0) {
            this.out = open(file, append);
        }
        ++logCnt;
        if (out!=null) {
            out.print(msg);
        }
    }

    /**
     * Returns the number of calls to <code>this.print()</code> and
     * <code>this.println()</code> methods since the last invocation of
     * a <code>setTarget()</code> method or the close() method.
     *
     * @return the number of calls to <code>this.print()</code> and
     * <code>this.println()</code> methods since the last invocation of
     * a <code>setTarget()</code> methods or the close() method.
     */
    public synchronized int logCnt() {
        return logCnt;
    }
}
