/*
 * Copyright 2009 Brian L. Browning
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
package blbutil;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;
import net.sf.samtools.util.BlockCompressedInputStream;

/* Code review on 19 Jan 2013 */

/**
 * Class <code>InputStreamIterator</code> is an iterator for iterating
 * through lines of a text input stream.  Class
 * <code>InputStreamIterator</code> uses a buffer when reading the
 * underlying the input stream.  The <code>next</code> method returns
 * the lines of the input stream as <code>String</code> instances.
 * If an <code>IOException</code> is thrown while reading a file, the
 * <code>IOException</code> is trapped, an error message is written
 * to standard out, and the Java Virtual Machine is terminated.
 */
public class InputStreamIterator implements FileIterator<String> {

    private static final int BUFFER_SIZE = 10*(1<<20);
    private final BufferedReader in;
    private String next = null;

    /**
     * Creates an <code>InputStreamIterator</code> to iterate through
     * the lines of the specified input stream.
     *
     * @param is input stream that will be read one line at a time.
     */
    public InputStreamIterator(InputStream is) {
        this(is, BUFFER_SIZE);
    }

    /**
     * Creates an <code>InputStreamIterator</code> to iterate through
     * the lines of the specified input stream.  If an
     * <code>IOException</code> or <code>FileNotFoundException</code> is
     * thrown, the virtual machine will exit with an error message.
     *
     * @param is input stream of text that will be read one line at a time.
     * @param bufferSize the buffer size in bytes.
     *
     * @throws IllegalArgumentException if <code>size < 0</code>.
     */
    public InputStreamIterator(InputStream is, int bufferSize) {
        BufferedReader br = null;
        try {
            InputStreamReader isr = new InputStreamReader(is);
            br = new BufferedReader(isr, bufferSize);
            next = br.readLine();
        }
        catch(IOException e) {
            Utilities.exit("Error reading " + is, e);
        }
        this.in = br;
    }

    @Override
    public File file() {
        return null;
    }

    @Override
    public boolean hasNext() {
        return (next != null);
    }

    @Override
    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        String current = next;
        try {
            next = in.readLine();
        }
        catch (IOException e) {
            Utilities.exit("Error reading " + in, e);
        }
        return current;
    }

    @Override
    public void remove() {
        String s = "remove() is not supported by LineIterator";
        throw new UnsupportedOperationException(s);
    }

    @Override
    public void close() {
        try {
            in.close();
        }
        catch (IOException e) {
            Utilities.exit("Error closing " + in, e);
        }
        next=null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append("[next = ");
        sb.append(next);
        sb.append("\nBufferedReader = ");
        sb.append(in);
        return sb.toString();
    }

    /**
     * Returns an <code>FileIterator<String></code> for the specified
     * text file.  If the filename ends in ".gz", the file is assumed
     * to be either BGZIP-compressed or GZIP-compressed.  If an
     * <code>IOException</code> or <code>FileNotFoundException</code> is
     * thrown, the virtual machine will exit with an error message.
     * @param file a text file.
     * @return a <code>FileIterator<String></code> for the specified
     * text file.
     *
     * @throws NullPointerException if <code>file==null</code>.
     */
    public static InputStreamIterator fromGzipFile(File file) {
        try {
            InputStream is = new FileInputStream(file);
            if (file.getName().endsWith(".gz")) {
                if (isBGZipFile(file)) {
                    return new InputStreamIterator(
                            new BlockCompressedInputStream(is));
                }
                else {
                    return new InputStreamIterator(new GZIPInputStream(is));
                }
            }
            else {
                return new InputStreamIterator(is);
            }
        }
        catch(FileNotFoundException e) {
            Utilities.exit("Error opening " + file, e);
        }
        catch(IOException e) {
            Utilities.exit("Error reading " + file, e);
        }
        assert false;
        return null;
    }

    private static boolean isBGZipFile(File file) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        boolean result = BlockCompressedInputStream.isValidFile(is);
        is.close();
        return result;
     }

    /**
     * Returns an <code>FileIterator<String></code> for the specified
     * text file.  If the filename ends in ".gz", the file is assumed
     * to be either BGZIP-compressed or GZIP-compressed.  If an
     * <code>IOException</code> or <code>FileNotFoundException</code> is
     * thrown, the virtual machine will exit with an error message.
     * @param filename name of a text file.
     * @return a <code>FileIterator<String></code> for the specified
     * text file.
     *
     * @throws NullPointerException if <code>file==null</code>.
     */
    public static InputStreamIterator fromGzipFile(String filename) {
        return fromGzipFile(new File(filename));
    }

    /**
     * Returns an <code>FileIterator<String></code> for the specified
     * text file.  If an <code>IOException</code> or
     * <code>FileNotFoundException</code> is thrown, the virtual machine
     * will exit with an error message.
     * @param file a text file.
     * @return a <code>FileIterator<String></code> for the specified
     * text file.
     *
     * @throws NullPointerException if <code>file==null</code>.
     */
    public static InputStreamIterator fromTextFile(File file) {
        try {
            return new InputStreamIterator(new FileInputStream(file));
        }
        catch(FileNotFoundException e) {
            Utilities.exit("Error opening " + file, e);
        }
        assert false;
        return null;
    }

    /**
     * Returns an <code>FileIterator<String></code> for the specified
     * text file.  If an <code>IOException</code> or
     * <code>FileNotFoundException</code> is thrown, the virtual machine
     * will exit with an error message.
     * @param filename name of a text file.
     * @return an <code>FileIterator<String></code> for the specified
     * text file.
     *
     * @throws NullPointerException if <code>filename==null</code>.
     */
    public static InputStreamIterator fromTextFile(String filename) {
        return fromTextFile(new File(filename));
    }
}
