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
package blbutil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;
import net.sf.samtools.util.BlockCompressedOutputStream;

/* Code review on 01 Nov 2013 */

/**
 * Class <code>Utilities</code> contains general utility methods for
 * working with files.
 *
 * @author Brian L Browning.
 */
public class FileUtil {
    static final int BUFFER_SIZE = 10 * (1 << 20);

    /**
     * Returns a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.  The resulting file will be compressed using
     * the BGZIP compression algorithm.  Any existing file corresponding
     * to the specified file will be deleted.  If the file cannot be
     * opened, an error message is printed and the java interpreter exits.
     *
     * @param file the file to be opened for output.
     * @param size the buffer size.
     * @param append <code>true</code> if data will be written to the end of
     * the file, and <code>false</code> if the file will be overwritten.
     * @return a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.
     * @throws IllegalArgumentException if <code>size &le; 0</code>.
     */
    public static PrintWriter bgzipPrintWriter(File file, int size, boolean append) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BlockCompressedOutputStream(
                    new BufferedOutputStream(
                    new FileOutputStream(file, append), size), file));
        } catch (IOException e) {
            Utilities.exit("Error opening " + file, e);
        }
        return out;
    }

    /**
     * Returns a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.  The resulting file will be compressed using
     * the BGZIP compression algorithm.  Any existing file corresponding
     * to the specified file will be deleted.  If the file
     * cannot be opened, an error message is printed and the
     * java interpreter exits.
     *
     * @param file the file to be opened for output.
     * @param append <code>true</code> if data will be written to the end of
     * the file, and <code>false</code> if the file will be overwritten.
     * @return a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.
     */
    public static PrintWriter bgzipPrintWriter(File file, boolean append) {
        return bgzipPrintWriter(file, BUFFER_SIZE, append);
    }

    /**
     * Returns a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.  The resulting file will be compressed using
     * the BGZIP compression algorithm.  Any existing file corresponding
     * to the specified file will be deleted.  If the file
     * cannot be opened, an error message is printed and the
     * java interpreter exits.
     *
     * @param file the file to be opened for output.
     * @return a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.
     */
    public static PrintWriter bgzipPrintWriter(File file) {
        boolean append = false;
        return bgzipPrintWriter(file, BUFFER_SIZE, append);
    }

    /**
     * Returns a <code>java.io.DataOutputStream</code> corresponding to
     * the specified file. Any existing file will be overwritten.
     * If the file cannot be opened, an error message is printed and the
     * java interpreter exits.
     *
     * @param file the file to be opened for output.
     * @param size size of the buffer in bytes.
     *
     * @return a <code>java.io.DataOutputStream</code> corresponding to
     * the specified file.
     */
    public static DataOutputStream createDataOutputStream(File file, int size) {
        OutputStream dos = null;
        try {
            dos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Utilities.exit("Error opening " + file, e);
        }
        DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(dos, size));
        return out;
    }

    /**
     * Returns a <code>java.io.DataOutputStream</code> corresponding to
     * the specified file. If the file cannot be opened, an error
     * message is printed and the java interpreter exits.
     *
     * @param file the file to be opened for output.
     * @param append if <code>true</code>, then data will be appended
     * to the end of any existing file.
     * @param size size of buffer in bytes.
     *
     * @return a <code>java.io.DataOutputStream</code> corresponding to
     * the specified file.
     */
    public static DataOutputStream createDataOutputStream(File file,
            boolean append, int size) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file, append);
        } catch (FileNotFoundException e) {
            Utilities.exit("Error opening " + file, e);
        }
        DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(os, size));
        return out;
    }

    /**
     * Returns a <code>java.io.DataOutputStream</code> corresponding to
     * the specified file.  Any existing file corresponding to the
     * <code>File</code> object will be deleted.   If the file cannot be
     * opened, an error message is printed and the java interpreter exits.
     *
     * @param file the file to be opened for output.
     * @param append if <code>true</code>, then data will be appended
     * to the end of any existing file.
     *
     * @return a <code>java.io.DataOutputStream</code> corresponding to
     * the specified file.
     */
    public static DataOutputStream createDataOutputStream(File file,
            boolean append) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file, append);
        } catch (FileNotFoundException e) {
            Utilities.exit("Error opening " + file, e);
        }
        DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(os, BUFFER_SIZE));
        return out;
    }

    /**
     * Returns a <code>java.io.DataOutputStream</code> corresponding to
     * the specified file.  Any existing file corresponding to the
     * <code>File</code> object will be deleted.   If the file cannot be
     * opened, an error message is printed and the java interpreter exits.
     *
     * @param file the file to be opened for output.
     *
     * @return a <code>java.io.DataOutputStream</code> corresponding to
     * the specified file.
     */
    public static DataOutputStream createDataOutputStream(File file) {
        return createDataOutputStream(file, false);
    }

    /**
     * Returns a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.  The resulting file will be compressed using
     * the GZIP compression algorithm.  Any existing file corresponding
     * to the specified file will be deleted.  If the file
     * cannot be opened, an error message is printed and the
     * java interpreter exits.
     *
     * @param filename the name of the file to be opened.
     * @return a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.
     *
     * @throws NullPointerException if <code>filename==null</code>.
     */
    public static PrintWriter gzipPrintWriter(String filename) {
        return gzipPrintWriter(new File(filename));
    }

    /**
     * Returns a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.  The resulting file will be compressed using
     * the GZIP compression algorithm.  Any existing file corresponding
     * to the specified file will be deleted.  If the file
     * cannot be opened, an error message is printed and the
     * java interpreter exits.
     *
     * @param file the file to be opened for output.
     * @return a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.
     */
    public static PrintWriter gzipPrintWriter(File file) {
        return gzipPrintWriter(file, BUFFER_SIZE);
    }

    /**
     * Returns a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.  The resulting file will be compressed using
     * the GZIP compression algorithm.  Any existing file corresponding
     * to the specified file will be deleted.  If the file cannot be
     * opened, an error message is printed and the java interpreter exits.
     *
     * @param file the file to be opened for output.
     * @param size the buffer size.
     * @return a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.
     * @throws IllegalArgumentException if <code>size &le; 0</code>.
     */
    public static PrintWriter gzipPrintWriter(File file, int size) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(
                    new GZIPOutputStream(new FileOutputStream(file), size));
        } catch (IOException e) {
            Utilities.exit("Error opening " + file, e);
        }
        return out;
    }

    /**
     * Returns a <code>java.io.PrintWriter</code> corresponding to
     * the specified filename.  Any existing file corresponding
     * with the specified filename will be deleted.  If the file
     * cannot be opened, an error message is printed and the
     * java interpreter exits.
     *
     * @param outFile the name of the file to be opened for output.
     *
     * @return a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.
     */
    public static PrintWriter printWriter(String outFile) {
        return printWriter(new File(outFile), false);
    }

    /**
     * Returns a <code>java.io.PrintWriter</code> corresponding to
     * the specified filename.  If <code>append==false</code>
     * any existing file corresponding with the specified filename will
     * be deleted.  If the file cannot be opened, an error message is
     * printed and the java interpreter exits.
     *
     * @param outFile the name of the file to be opened for output.
     * @param append if <code>true</code>, then data will be appended
     * to the end of any existing file.
     *
     * @return a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.
     */
    public static PrintWriter printWriter(String outFile, boolean append) {
        return printWriter(new File(outFile), append);
    }

    /**
     * Returns a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.  Any existing file corresponding
     * with the specified filename will be deleted.  If the file
     * cannot be opened, an error message is printed and the
     * java interpreter exits.
     *
     * @param file the file to be opened for output.
     *
     * @return a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.
     */
    public static PrintWriter printWriter(File file) {
        return printWriter(file, false);
    }

    /**
     * Returns a <code>java.io.PrintWriter</code> that writes
     * to standard out.
     *
     * @return a <code>java.io.PrintWriter</code> that writes
     * to standard out.
     */
    public static PrintWriter stdOutPrintWriter() {
        return new PrintWriter(
                new BufferedOutputStream(System.out, BUFFER_SIZE));
    }

    /**
     * Returns a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.  If <code>append==false</code>
     * any existing file with the specified filename will be deleted.
     * If the file cannot be opened, an error message is printed and the
     * java interpreter exits.
     *
     * @param file the file to be opened for output.
     * @param append if <code>true</code>, then data will be appended
     * to the end of any existing file.
     * @return a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.
     */
    public static PrintWriter printWriter(File file, boolean append) {
        return printWriter(file, append, BUFFER_SIZE);
    }

    /**
     * Returns a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.  If <code>append==false</code>
     * any existing file corresponding with the specified filename
     * will be deleted.  If the file cannot be opened, an error message
     * is printed and the java interpreter exits.
     *
     * @param file the file to be opened for output.
     * @param append if <code>true</code>, then data will be appended
     * to the end of any existing file.
     * @param size the buffer size
     * @return a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.
     * @throws IllegalArgumentException if <code>size &le; 0</code>.
     */
    public static PrintWriter printWriter(File file, boolean append, int size) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(
                    new BufferedWriter(new FileWriter(file, append), size));
        } catch (IOException e) {
            Utilities.exit("Error opening " + file, e);
        }
        return out;
    }

     /**
     * Returns a nonbuffered <code>java.io.PrintWriter</code> corresponding to
     * the specified file.  If <code>append==false</code>
     * any existing file corresponding with the specified filename
     * will be deleted.  If the file cannot be opened, an error message
     * is printed and the java interpreter exits.
     *
     * @param file the file to be opened for output.
     * @param append if <code>true</code>, then data will be appended
     * to the end of any existing file.
     * @return a <code>java.io.PrintWriter</code> corresponding to
     * the specified file.
     * @throws IllegalArgumentException if <code>size &le; 0</code>.
     */
    public static PrintWriter nonBufferedFileWriter(File file, boolean append) {
        boolean autoflush = true;
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter(file, append), autoflush);
        } catch (IOException e) {
            Utilities.exit("Error opening " + file, e);
        }
        return pw;
    }

    /**
     * Returns a data input stream reading from the specified file.  If the
     * input stream cannot be opened, an error message is printed and the
     * java interpreter exits.
     *
     * @param file a binary file.
     *
     * @return a data input stream reading from the specified file.
     */
    public static DataInputStream dataInputStream(File file) {
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new BufferedInputStream(
                    new FileInputStream(file), BUFFER_SIZE));
        } catch (FileNotFoundException e) {
            Utilities.exit("Error opening " + file, e);
        }
        return dis;
    }

    /**
     * Copies the specified text file.
     *
     * @param from text file to copy
     * @param to copy of text file to be created.
     *
     * @throws NullPointerException if
     * <code>from==null || to==null</code>.
     */
    public static void copyFile(String from, String to) {
        FileIterator<String> it = InputStreamIterator.fromTextFile(from);
        PrintWriter out = FileUtil.printWriter(to);
        while (it.hasNext()) {
            out.println(it.next());
        }
        out.close();
        it.close();
    }

    /**
     * Returns an array of empty temporary files.  The temporary files
     * will be deleted when the java virtual machine exits.
     *
     * @param size the number of temporary files that will be created.
     * @param prefix the prefix string to be used in generating each
     * file's name; must be at least three characters long.
     *
     * @return an array of empty temporary files. The temporary files
     * will be deleted when the java virtual machine exits.
     *
     * @throws IllegalArgumentException if the <code>prefix</code>
     * argument contains fewer than three characters.
     * @throws NullPointerException if <code>prefix==null</code>.
     * @throws NegativeArraySizeException if <code>size < 0</code>.
     */
    public static File[] getTempFileArray(int size, String prefix) {
        if (prefix == null) {
            throw new NullPointerException("prefix==null");
        }
        File[] fa = new File[size];
        for (int j = 0; j < fa.length; ++j) {
            fa[j] = tempFile(prefix);
        }
        return fa;
    }

    /**
     * Returns a temporary <code>File</code> that will be deleted when
     * the java virtual machine exits.
     *
     * @param prefix the prefix string to be used in generating the file's
     * name; must be at least three characters long.
     *
     * @return a <code>File</code> with an abstract pathname denoting a
     * newly created empty file.
     *
     * @throws IllegalArgumentException if the <code>prefix</code>
     * argument contains fewer than three characters.
     */
    public static File tempFile(String prefix) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile(prefix, null);
            tempFile.deleteOnExit();
        } catch (Exception e) {
            Utilities.exit("Exception thrown by createTempFile: ", e);
        }
        return tempFile;
    }
}
