/*
 * Copyright 2009-2013 Brian L. Browning
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* Code review on 22 May 2013 */

/**
 * Class <code>Utilities</code> contains general utility methods.
 *
 * @author Brian L Browning, except where noted
 */
public class Utilities {

    /**
     * Prints a summary of memory usage at the time of method invocation
     * to standard output.
     * @param msg a string a message to be printed with the summary
     * of memory usage.
     */
    public static void printMemoryUsage(String msg) {
        long Mb = 1024*1024;
        Runtime rt = Runtime.getRuntime();
        System.out.println(Const.nl + msg
                + Const.tab + "maxMb=" + (rt.maxMemory()/Mb)
                + Const.tab + "totalMb=" + (rt.totalMemory()/Mb)
                + Const.tab + "freeMb=" + (rt.freeMemory()/Mb)
                + Const.tab + "usedMb=" + ((rt.totalMemory() - rt.freeMemory())/Mb));
    }


    /**
     * Returns a string representation of the specified character array
     * with the character elements converted to integers for printing.
     * The representation has the format
     * "[a[1], a[2], a[3], ..., a[length-1]]".
     *
     * @param a a character array.
     *
     * @return a string representation of the specified character array
     * with the character elements converted to integers for printing.
     */
    public static String charArrayToString(char[] a) {
        StringBuilder sb = new StringBuilder(a.length * 4 + 10);
        sb.append("[");
        sb.append((int) a[0]);
        for (int j = 1; j < a.length; ++j) {
            sb.append(", ");
            sb.append((int) a[j]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Returns a string representation of the initial <code>length</code>
     * elements of the specified character array
     * with the character elements converted to integers for printing.
     * The representation has the format
     * "[a[1], a[2], a[3], ..., a[length-1]]".
     *
     * @param a a character array.
     * @param length the number of initial elements of the specified
     * array to be printed.
     *
     * @return a string representation of the initial <code>length</code>
     * elements of the specified character array
     * with the character elements converted to integers for printing.
     */
    public static String charArrayToString(char[] a, int length) {
        StringBuilder sb = new StringBuilder(a.length * 4 + 10);
        sb.append("[");
        sb.append((int) a[0]);
        for (int j = 1; j < length; ++j) {
            sb.append(", ");
            sb.append((int) a[j]);
        }
        sb.append("]");
        return sb.toString();
    }

    private Utilities() {
        // private constructor to prevent instantiation
    }

    /**
     * Returns the number of bytes per line in the specified file.
     * @param file a file with a constant number of bytes per line.
     * @param nLines the number of lines in the specified file.
     * @return the number of bytes per line in the specified file.
     *
     * @throws IllegalArgumentException if
     * <code>(file.length() % nLines) != 0</code>, or if
     * the number of bytes per line exceeds <code>Integer.MAX_VALUE</code>.
     * @throws NullPointerException if <code>file==null</code>.
     */
    public static int bytesPerLine(File file, int nLines) {
        long fileLength = file.length();
        long longBytesPerLine = fileLength / nLines;
        if ((fileLength % nLines) != 0) {
            String s = "file.length() % nMarkers() != 0";
            throw new IllegalArgumentException(s);
        }
        if (longBytesPerLine > Integer.MAX_VALUE) {
            String s = "file.length() / nMarkers > Integer.MAX_VALUE: "
                    + longBytesPerLine;
            throw new IllegalArgumentException(s);
        }
        return (int) longBytesPerLine;
    }

    /**
     * Writes the specified array of bytes to the specified output stream.
     * Exits the java virtual machine if an <code>IOException</code> is
     * thrown.
     *
     * @param ba a <code>byte[]</code> array
     * @param out a data output stream.
     *
     * @throws NullPointerException if <code>ba==null || out==null</code>
     *
     */
    public static void write(byte[] ba, DataOutputStream out) {
        try {
            out.write(ba, 0, ba.length);
        }
        catch (IOException ex) {
            Utilities.exit("error writing to outputStream: " + out, ex);
        }
    }

    /**
     * Returns a map whose keys are elements of the specified array and
     * whose values are <code>Integer</code> objects encapsulating the
     * array index of the key.
     * @param array an array of elements.
     * @return a map whose keys are elements of the specified array and
     * whose values are <code>Integer</code> objects encapsulating the
     * array index of the key.
     * @throws IllegalArgumentException if the specified array if two
     * elements of the specified array are equal.
     * @throws NullPointerException if <code>array==null</code>.
     */
    public static <E> Map<E, Integer> indexMap(E[] array) {
        Map<E, Integer> map = new HashMap<E, Integer>();
        for (int j=0; j<array.length; ++j) {
            if (map.keySet().contains(array[j])) {
                String s = "duplicate element: " + array[j];
                throw new IllegalArgumentException(s);
            }
            map.put(array[j], j);
        }
        return map;
    }

    /**
     * Returns a string representing the current local time.  The
     * exact details of the representation are unspecified and
     * subject to change.
     *
     * @return a string representing the current local time.
     */
    public static String timeStamp() {
        Date now = new Date();
        SimpleDateFormat sdf =
                new SimpleDateFormat("hh:mm a z 'on' dd MMM yyyy");
        return sdf.format(now);
    }

    /**
     * Returns the number of null entries in the specified array.
     * @param array an array
     * @return the number of null entries in the specified array.
     * @throws NullPointerException if <code>array==null</code>
     */
    public static int countNull(Object[] array) {
        int cnt = 0;
        for (Object o : array) {
            if (o==null) {
                ++cnt;
            }
        }
        return cnt;
    }

    /**
     * Returns the number of true entries in the specified array.
     * @param array a boolean array
     * @return the number of true entries in the specified array.
     * @throws NullPointerException if <code>array==null</code>
     */
    public static int countTrue(boolean[] array) {
        int cnt = 0;
        for (boolean b : array) {
            if (b) {
                ++cnt;
            }
        }
        return cnt;
    }

    /**
     * Returns the first non-blank line of the specified file.
     * If an <code>IOException</code> is thrown, an error message is printed
     * and the java interpreter exits.
     *
     * @param file a text file
     * @return the first line of the specified file.
     * @throws NullPointerException if <code>f==null</code>.
     * @throws IllegalArgumentException if the specified file has
     * no blank lines.
     */
    public static String readFirstNonBlankLine(File file) {
        FileIterator<String> it = InputStreamIterator.fromGzipFile(file);
        String firstLine = null;
        while (it.hasNext() && firstLine == null) {
            String candidateLine = it.next().trim();
            if (candidateLine.length() > 0) {
                firstLine = candidateLine;
            }
        }
        it.close();
        if (firstLine==null) {
            String s = "ERROR: empty file: " + file;
            throw new IllegalArgumentException(s);
        }
        return firstLine;
    }

    /**
     * Returns a set of identifiers found in a single column text file.
     * The empty set is returned if <code>file==null</code>.
     * If an <code>IOException</code> is thrown, an error message is printed
     * and the java interpreter exits.
     *
     * @param file a text file with a single column of data
     *
     * @return a set of identifiers found in a single column text file.
     *
     * @throws IllegalArgumentException if the specified file does not exist,
     * or if the specified file is a directory, or if any line of the specified
     * file contains more than one white-space delimited fields.
     */
    public static Set<String> getIdSet(File file) {
        Set<String> exclusions = new HashSet<String>();
        if (file==null) {
            return exclusions;
        }
        else {
            if (file.exists()==false) {
                String s = "File does not exist: " + file;
                throw new IllegalArgumentException(s);
            }
            if (file.isDirectory()) {
                String s = "File is a directory: " + file;
                throw new IllegalArgumentException(s);
            }
            FileIterator<String> it = InputStreamIterator.fromGzipFile(file);
            while (it.hasNext()) {
                String id = it.next().trim();
                int length = id.length();
                if (length > 0) {
                    if (StringUtil.countFields(id) > 1) {
                        String s = "Error: >1 excluded identifier in line: "+id;
                        throw new IllegalArgumentException(s);
                    }
                    exclusions.add(id);
                }
            }
            it.close();
            return exclusions;
        }
    }

    /**
     * Returns a set that contains all non-empty lines of the
     * specified text file containing a single column of data.
     * Any white space at the beginning or end of each line is removed.
     * If an <code>IOException</code> is thrown, an error message is printed
     * and the java interpreter exits.
     *
     * @param filename a text file
     * @return a set that contains all non-empty lines of the
     * specified text file.
     *
     * @throws IllegalArgumentException if any line of the specified text file
     * contains two or more white-space delimited fields.
     * @throws NullPointerException if <code>filename==null</code>.
     */
    public static Set<String> column2Set(String filename) {
        return column2Set(new File(filename));
    }

    /**
     * Returns a set that contains all non-empty lines of the
     * specified text file containing a single column of data.
     * Any white space at the beginning or end of each line is removed.
     * If an <code>IOException</code> is thrown, an error message is printed
     * and the java interpreter exits.
     *
     * @param file a text file
     * @return a set that contains all non-empty lines of the
     * specified text file.
     *
     * @throws IllegalArgumentException if any line of the specified text file
     * contains two or more white-space delimited fields.
     * @throws NullPointerException if <code>filename==null</code>.
     */
    public static Set<String> column2Set(File file) {
        FileIterator<String> it = InputStreamIterator.fromGzipFile(file);
        Set<String> set = new HashSet<String>(5000);
        while (it.hasNext()) {
            String line = it.next().trim();
            if (line.length() > 0) {
                if (StringUtil.countFields(line) != 1) {
                    String s = "Error: more than one field on line in file ("
                            + file + "): " + line;
                    throw new IllegalArgumentException(s);
                }
                set.add(line);
            }
        }
        return set;
    }

    /*
     * Prints the specified string to both the specified PrintWriter and
     * to standard out.  A line separator characters is not appended to the
     * specified string.
     *
     * @param out a PrintWriter used to print the specified string.
     * @param s a string to be printed.
     *
     * @throws NullPointerException if <code>out==null</code>.
     */
    public static void duoPrint(PrintWriter out, String s) {
        System.out.print(s);
        out.print(s);
    }

   /*
     * Prints the specified string to both the specified PrintWriter and
     * to standard out.  A line separator characters is appended to the
     * specified string.
     *
     * @param out a PrintWriter used to print the specified string.
     * @param s a string to be printed.
     *
     * @throws NullPointerException if <code>out==null</code>.
     */
    public static void duoPrintln(PrintWriter out, String s) {
        System.out.println(s);
        out.println(s);
    }

    /**
     * Prints the delimited fields to the specified <code>PrintWriter</code>
     * followed by a new line character.
     *
     * @param fields fields to be printed.
     * @param delimiter a delimiter for the fields
     * @param out the <code>PrintWriter</code> to which output will be written.
     * @throws NullPointerException if <code>fields==null || out==null</code>.
     */
    public static void printLine(String[] fields, String delimiter,
            PrintWriter out) {
        if (fields.length > 0) {
            out.print(fields[0]);
        }
        for (int j=1; j<fields.length; ++j) {
            out.print(delimiter);
            out.print(fields[j]);
        }
        out.println();
    }

    /**
     * Prints the specified string, the specified exception, and a
     * stack trace to standard out and terminates the java interpreter.
     *
     * @param s a message to be printed to standard err.
     * @param e an exception or error to be printed to standard err.
     */
    public static void exit(String s, Throwable e) {
        e.printStackTrace();
        System.err.println(s);
        System.err.println("terminating program.");
        System.exit(1);
    }

    /*
     * Prints the specified message to standard out and exits the
     * java virtual machine.
     *
     * @param message a message to be written to standard err.
     */
    public static void printErrorAndExit(String message) {
        System.out.println(message);
        System.out.println();
        System.out.flush();
        System.exit(0);
    }

    /**
     * Returns the number of nonempty lines in the specified file.
     * A nonempty line contains at least one character that is
     * not white space.  Files whose names end in ".gz" are
     * assumed to be GZIP compressed.
     * @param file a text file.
     * @return the number of nonempty lines in the specified file.
     *
     * @throws NullPointerException if <code>file==null</code>.
     */
    public static int countNonBlankLines(File file) {
        int cnt = 0;
        FileIterator<String> it = InputStreamIterator.fromGzipFile(file);
        while (it.hasNext()) {
            String line = it.next().trim();
            if (line.length() > 0) {
                ++cnt;
            }
        }
        it.close();
        return cnt;
    }

    /**
     * Closes the specified output stream.  If the output stream cannot be
     * closed, an error message is printed and the java interpreter exits.
     *
     * @param os a output stream.
     */
    public static void closeOutputStream(OutputStream os) {
        try {
            os.close();
        }
        catch (IOException e) {
            Utilities.exit("Error closing output stream" + os, e);
        }
    }

    /*
     * Closes the specified data input stream.  If the data input
     * stream cannot be closed, an error message is printed and the
     * java interpreter exits.
     *
     * @param is a data input stream.
     */
    public static void closeDataInputStream(DataInputStream is) {
        try {
            is.close();
        }
        catch (IOException e) {
            Utilities.exit("Error closing input stream: " + is, e);
        }
    }

    /**
     * Returns an <code>int</code> array obtained from parsing
     * the elements of the specified <code>String</code> array.
     *
     * @param a <code>String</code> array whose elements are
     * parsable integers.
     * @return an <code>int</code> array obtained from parsing the
     * elements of the specified <code>String</code> array.
     * @throws NumberFormatException if the specified array has a
     * <code>String</code> element that does not contain a parsable
     * integer.
     */
    public static int[] toIntArray(String[] a) {
        int[] intArray = new int[a.length];
        for (int j=0; j<intArray.length; ++j) {
            intArray[j] = Integer.parseInt(a[j]);
        }
        return intArray;
    }

    /**
     * Returns an <code>int</code> array obtained from parsing
     * the specified elements of the specified <code>String</code>
     * array .
     *
     * @param a <code>String</code> array whose elements are
     * parsable integers.
     * @param start the index of the first array entry to
     * be parsed.
     * @param end one more than the index of the last array entry
     * to be parsed.
     * @return an <code>int</code> array of length <code>end - start</code>
     * obtained from parsing the specified elements of the specified
     * <code>String</code> array.
     * @throws NumberFormatException if the specified array has a
     * <code>String</code> element that does not contain a parsable
     * integer.
     * @throws ArrayIndexOutOfBoundsException if
     * <code> start < 0 || end &ge; a.length</code>.
     * @throws NegativeArraySizeException if <code>end > start</code>.
     */
    public static int[] toIntArray(String[] a, final int start, final int end) {
        int[] intArray = new int[end - start];
        for (int j=start; j<end; ++j) {
            intArray[j-start] = Integer.parseInt(a[j]);
        }
        return intArray;
    }

    /**
     * Returns a map with one (key, value) pair for each string element
     * of the specified array that contains the specified separator.
     * If an array element <code>s</code> of the specified array contains
     * the specific separator character then the key is
     * <code> s.substring(0, s.indexOf('sep'))</code>
     * and the value is <code>s.substring(s.indexOf(s.indexOf(sep) + 1)</code>.
     * Thus if the first occurrence of the specified separator is the
     * last character of the string, the map value will be the
     * empty string ("").
     * <p/>
     *
     * If the array element <code>s</code> of the specified array does not
     * contain the specified separator character then the entry is ignored.
     *
     * @param args a string array.
     * @param sep a separator character used to create a (key, value) pair
     * from a string of the form <code>key + sep + value</code>.
     * @return a <code>java.util.Map<String, String></code> with one
     *        (key, value) pair for each element of the specified
     *        string array with the form <code>key + sep + value</code>.
     * @throws NullPointerException if <code>args==null</code>.
     * @throws IllegalArgumentException if any two elements of the
     * specified string array have the same key.
     */
    public static Map<String,String> argsToMap(String[] args, char sep) {
        Map<String,String> argMap = new HashMap<String,String>();
        for (String arg : args) {
            String value = null;
            int index = arg.indexOf(sep);
            if (index != -1) {
                String key = arg.substring(0, index);
                value = arg.substring(index + 1);
                if (argMap.containsKey(key)) {
                    String s = "duplicate arguments: " + key;
                    throw new IllegalArgumentException(s);
                }
                argMap.put(key, value);
            }
        }
        return argMap;
    }

    /**
     * Returns a multimap whose values are <code>String[]</code> arrays
     * which contain all values associated with the key.  The order of
     * elements in the array corresponding to a key is not required to
     * correspond to the order of the elements in the <code>args</code>
     * array.
     *
     * If an array element <code>s</code> of the specified array contains
     * the specific separator character then the key is
     * <code> s.substring(0, s.indexOf('sep'))</code>
     * and the value is <code>s.substring(s.indexOf(s.indexOf(sep) + 1)</code>.
     * Thus if the first occurrence of the specified separator is the
     * last character of the string, an element of the map value will be the
     * empty string ("").
     * <p/>
     *
     * If the array element <code>s</code> of the specified array does not
     * contain the specified separator character then the entry is ignored.
     *
     * @param args a string array.
     * @param sep a separator character delimiting a (key, value) pair
     * from a string of the form <code>key + sep + value</code>.
     * @return a <code>java.util.Map<String, String[]></code> whose key set
     *        contains all keys in the specified string array.  The value
     *        associated with each key in the map key set is the
     *        string array of all values associated with that key in the
     *        specified string array.
     * @throws NullPointerException if <code>args==null</code>.
     * @throws IllegalArgumentException if any two elements of the
     * specified string array denote the same the same (key, value) pair.
     */
    public static Map<String,String[]> argsToMultimap(String[] args, char sep) {
        Map<String,Collection<String>> map =
                new HashMap<String,Collection<String>>();
        for (String arg : args) {
            String value = null;
            int index = arg.indexOf(sep);
            if (index != -1) {
                String key = arg.substring(0, index);
                value = arg.substring(index + 1);
                if (map.containsKey(key)) {
                    Collection<String> c = map.get(key);
                    if (c.contains(value)) {
                        String s = "duplicate arguments: " + arg;
                        throw new IllegalArgumentException(s);
                    }
                    else {
                        c.add(value);
                    }
                }
                else {
                    Collection<String> c = new ArrayList<String>();
                    c.add(value);
                    map.put(key, c);
                }
            }
        }
        Map<String,String[]> argMap = new HashMap<String,String[]>();
        String[] keySet = map.keySet().toArray(new String[0]);
        for (String key : keySet) {
            Collection<String> c = map.get(key);
            argMap.put(key, c.toArray(new String[0]));
            map.remove(key);
        }
        return argMap;
    }

    /**
     * Returns the string entries that do not contain the specified
     * separator character as a list.  The elements of the returned list
     * will have the same ordering as in the specified string array.
     *
     * @param args a string array.
     * @param sep a separator character.  Elements of the string array
     * that contains the separator character are ignored.
     *
     * @return the string entries that do not contain the specified
     * separator character as a list.
     *
     * @throws NullPointerException if <code>args==null</code>.
     */
    public static List<String> argsToList(String[] args, char sep) {
        List<String> argList = new ArrayList<String>();
        for (String arg : args) {
            int index = arg.indexOf(sep);
            if (index==-1) {
                argList.add(arg);
            }
        }
        return argList;
    }

     /**
     * Returns a string representation of the specified elapsed time.
     *
     * @param milliseconds the elapsed time in milliseconds
     *
     * @return a string representation of the specified elapsed time.
     */
    public static String elapsedMillis(long milliseconds) {
        long seconds = Math.round(milliseconds /1000.0);
        StringBuilder sb = new StringBuilder(80);
        if (seconds >= 3600) {
            sb.append(seconds / 3600);
            sb.append(" hours ");
            seconds %= 3600;

        }
        if (seconds >= 60) {
            sb.append(seconds / 60);
            sb.append(" minutes ");
            seconds %= 60;
        }
        sb.append(seconds);
        sb.append(" seconds");
        return sb.toString();
    }

     /**
     * Returns a string representation of the specified elapsed time.
     *
     * @param nanoseconds the elapsed time in nanoseconds
     *
     * @return a string representation of the specified elapsed time.
     */
    public static String elapsedNanos(long nanoseconds) {
        long seconds = Math.round(nanoseconds /1000000000.0);
        StringBuilder sb = new StringBuilder(80);
        if (seconds >= 3600) {
            sb.append(seconds / 3600);
            sb.append(" hours ");
            seconds %= 3600;

        }
        if (seconds >= 60) {
            sb.append(seconds / 60);
            sb.append(" minutes ");
            seconds %= 60;
        }
        sb.append(seconds);
        sb.append(" seconds");
        return sb.toString();
    }
}

