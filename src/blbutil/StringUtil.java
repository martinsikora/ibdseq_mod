/*
 * Copyright 2009-12 Brian L. Browning
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

/* Code review on 15 Dec 2012 */

/**
 * Class <code>StringUtil</code> is a utility class for counting and
 * retrieving delimited fields in a string.
 */
public class StringUtil {

    /* Private constructor to prevent instantiation */
    private StringUtil() {
    }

    /*
     * Returns the smallest index <code>i < s.length()</code> such that
     * <code>(i &ge; start) && (s.charAt(i) > ' ')</code>
     * (the space character), or returns <code>s.length()</code> if no
     * such index exists.
     *
     * @param s a string.
     * @param start an index of the string <code>s</code> satisfying
     * <code>(0 &le; start) && (start < s.length())</code>.
     * @param delimiter the delimiter character.
     * @return the smallest index <code>i < s.length()</code> such that
     * <code>(i &ge; start) && (s.charAt(i) == delimiter)</code>,
     * or returns <code>s.length()</code> if no such index exists.
     *
     * @throws NullPointerException if <code>s==null</code>.
     * @throws IndexOutOfBoundsException if
     * <code>start < 0 || start >= line.length()</code>.
     */
    public static int nextDelimiter(String s, int start, char delimiter) {
        int end = s.length();
        while (start < end && s.charAt(start) != delimiter) {
            ++start;
        }
        return start;
    }

    /**
     * Returns the number of delimited fields in the specified
     * string.  If the specified string has length 0, then 0 is returned.
     * Quotation characters are ignored.
     *
     * @param s a string with 0 or more white-space delimited fields.
     * @return the number of delimited fields in the specified
     * string.
     * @throws NullPointerException if <code>s==null</code>.
     */
    public static int countFields(String s, char delimiter) {
        int cnt = 0;
        int n = s.length();
        if (n==0) {
            return 0;
        }
        for (int j=0; j<n; ++j) {
            if (s.charAt(j)==delimiter) {
                ++cnt;
            }
        }
        return cnt + 1;
    }

    /**
     * Returns the minimum of the specified limit and the number of
     * delimited fields in the specified string.  If the specified string
     * has length 0, then 0 is returned.  Quotation characters are ignored.
     *
     * @param s a string with 0 or more white-space delimited fields.
     * @param delimiter the field delimiter.
     * @param limit the maximum value returned.
     *
     * @return Returns the minimum of the specified limit and the number of
     * delimited fields in the specified string.
     *
     * @throws IllegalArgumentException if <code>limit < 0</code>.
     * @throws NullPointerException if <code>s==null</code>.
     */
    public static int countFields(String s, char delimiter, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit < 0: " + limit);
        }
        int cnt = 0;
        int strLength = s.length();
        if (strLength==0) {
            return 0;
        }
        for (int j=0; j<strLength && cnt<(limit-1); ++j) {
            if (s.charAt(j)==delimiter) {
                ++cnt;
            }
        }
        return cnt + 1;
    }

    /**
     * Returns an array obtained by splitting the specified string
     * around the specified delimiter.
     * The array returned by this method contains each substring of
     * this string that does not contain the delimiter and that
     * is preceded by the delimiter or the beginning of
     * the string and that is terminated by the delimiter or the end
     * of the string.  The substrings in the array are in
     * the order in which they occur in this string. If there are no
     * delimiters in the specified string then returned array
     * has just one element which is the specified string.
     *
     * @param line a string to split around the delimiter.
     *
     * @return the array of strings computed by splitting the specified
     * delimiter.
     *
     * @throws NullPointerException if <code>line==null</code>.
     */
    public static String[] getFields(String line, char delimiter) {
        int fieldCount = countFields(line, delimiter);
        String[] fields = new String[fieldCount];
        if (fields.length > 0) {
            int start = 0;
            for (int j=0; j<fields.length; ++j)  {
                int end = nextDelimiter(line, start, delimiter);
                fields[j] = line.substring(start, end);
                start = end + 1;
            }
        }
        return fields;
    }

    /**
     * Returns an array obtained by splitting the specified string
     * around the first <code>limit-1</code> occurrences of the specified
     * delimiter.
     *
     * @param line a string to split around the specified delimiter.
     * @param limit controls the length of the returned array as specified
     * above.
     *
     * @return an array obtained by splitting the specified string
     * around the first <code>limit-1</code> occurrences of the specified
     * delimiter.
     *
     * @throws NullPointerException if <code>line==null</code>.
     * @throws IllegalArgumentException if <code>limit < 2</code>.
     */
    public static String[] getFields(String line, char delimiter, int limit) {
        if (limit < 2) {
            throw new IllegalArgumentException("limit < 2: " + limit);
        }

        int fieldCount = countFields(line, delimiter, limit);
        String[] fields = new String[fieldCount];
        if (fields.length > 0) {
            int start = 0;
            for (int j=0, n=fields.length-1; j<n; ++j)  {
                int end = nextDelimiter(line, start, delimiter);
                fields[j] = line.substring(start, end);
                start = end + 1;
            }
            fields[fields.length - 1] = line.substring(start);
        }
        return fields;
    }

    /*
     * Returns the smallest index <code>i < s.length()</code> such that
     * <code>(i &ge; start) && (s.charAt(i) > ' ')</code>
     * (the space character), or returns <code>s.length()</code> if no
     * such index exists.
     *
     * @param s a string.
     * @param start an index of the string <code>s</code> satisfying
     * <code>(0 &le; start) && (start < s.length())</code>.
     * @return the smallest index <code>i < s.length()</code> such that
     * <code>(i &ge; start) && (s.charAt(i) > ' ')</code>
     * (the space character), or returns <code>s.length()</code> if no
     * such index exists.
     *
     * @throws NullPointerException if <code>s==null</code>.
     * @throws IndexOutOfBoundsException if
     * <code>start < 0 || start >= line.length()</code>.
     */
    public static int nextNonWhiteSpace(String s, int start) {
        int end = s.length();
        if (s.charAt(start) <= ' ') {
            while (++start < end && s.charAt(start) <= ' ') {
            }
        }
        return start;
    }

    /*
     * Returns the the smallest index <code>i < s.length()</code> such that
     * <code>(i &ge; start) && (s.charAt(i) &le; ' ')</code>
     * (the space character), or returns <code>s.length()</code> if no
     * such index exists.
     *
     * @param s a string.
     * @param start an index of the string <code>s</code> satisfying
     * <code>(0 &le; start) && (start < s.length())</code>.
     * @return the the smallest index <code>i < s.length()</code> such that
     * <code>(i &ge; start) && (s.charAt(i) &le; ' ')</code>
     * (the space character), or returns <code>s.length()</code> if no
     * such index exists.
     *
     * @throws NullPointerException if <code>s==null</code>.
     * @throws IndexOutOfBoundsException if
     * <code>start < 0 || start >= s.length()</code>.
     */
    public static int nextWhiteSpace(String s, int start) {
        if (s.charAt(start) > ' ') {
            int end = s.length();
            while (++start < end && s.charAt(start) > ' ') {
            }
        }
        return start;
    }


    /**
     * Returns the number of white-space delimited fields in the specified
     * string.  A field is one or more consecutive characters that are not
     * white space characters.  White space is defined as any unicode
     * characters less than or equal to '\u0020' (the space character).
     * If the specified string contains only white-space characters then
     * 0 is returned.
     *
     * @param s a string with 0 or more white-space delimited fields.
     * @return the number of white-space delimited fields in the specified
     * string.
     * @throws NullPointerException if <code>s==null</code>.
     */
    public static int countFields(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && s.charAt(start) <= ' ') {
            ++start;
        }
        while (end > start && s.charAt(end - 1) <= ' ') {
            --end;
        }
        int fieldCount = (start < end) ? 1 : 0;
        while (++start < end && s.charAt(start) > ' ') {
        }
        while (start < end) {
            while (s.charAt(++start) <= ' ') {
            }
            ++fieldCount;
            while (++start<end && s.charAt(start) > ' ') {
            }
        }
        return fieldCount;
    }

    /**
     * Splits the specified string around white space. White space is defined
     * as any unicode characters less than or equal to '\u0020'
     * (the space character). The array returned by this method
     * contains each substring of this string that does not contain
     * white-space characters and that is preceded by
     * a white-space character or the beginning of
     * the string and that is terminated by white-space character or the end
     * of the string.  The substrings in the array are in
     * the order in which they occur in this string. If there are no
     * white-space characters in the specified string then returned array
     * has just one element, namely the specified string.   If the specified
     * string contains only white-space characters then a string array of
     * length 0 is returned.
     *
     * @param line a string to split around white space.
     *
     * @return the array of strings computed by splitting the specified string
     * around white space.
     *
     * @throws NullPointerException if <code>line==null</code>.
     */
    public static String[] getFields(String line) {
        line = line.trim();
        int n = line.length();
        int fieldCount = countFields(line);
        String[] fields = new String[fieldCount];
        if (fields.length > 0) {
            fieldCount = -1;
            int start = 0;
            int j = -1;
            while (++j < n && line.charAt(j) > ' ')  {
            }
            fields[++fieldCount] = line.substring(start, j);
            while (j<n) {
                while (line.charAt(++j) <= ' ') {
                }
                start = j;
                while (++j < n && line.charAt(j) > ' ') {
                }
                fields[++fieldCount] = line.substring(start, j);
            }
        }
        return fields;
    }

    /**
     * Splits the specified string around white space. White space is defined
     * as any unicode characters less than or equal to '\u0020'
     * (the space character). The array returned by this method
     * contains each substring of this string that does not contain
     * white-space characters and that is preceded by
     * a white-space character or the beginning of
     * the string and that is terminated by white-space character or the end
     * of the string.  The substrings in the array are in
     * the order in which they occur in this string. If there are no
     * white-space characters in the specified string then returned array
     * has just one element which is specified string.   If the specified
     * string contains only white-space characters then a string array of
     * length 0 is returned.
     *
     * The specified limit <code>limit</code> controls the length of the
     * returned array and must be greater than 1.  The returned array's
     * length will be no greater than <code>limit</code>.  If there
     * are <code>n</code> or more substrings that do not contain
     * white-space characters in the specified string, then
     * the returned array's last entry will contain
     * the end of the specified string, beginning with the
     * <code>limit - 1</code>-th white-space delimited substring.
     *
     * @param line a string to split around white space.
     * @param limit controls the length of the returned array as specified
     * above.
     *
     * @return the array of strings computed by splitting the specified string
     * around white space.
     *
     * @throws NullPointerException if <code>line==null</code>.
     * @throws IllegalArgumentException if <code>limit < 2</code>.
     */
    public static String[] getFields(String line, final int limit) {
        if (limit < 2) {
            throw new IllegalArgumentException("limit < 2: " + limit);
        }
        line = line.trim();
        int n = line.length();
        int j=-1;
        while (++j < n && line.charAt(j) > ' ') {
        }
        int fieldCount = (j > 0) ? 1 : 0;
        while (j < n && fieldCount < limit) {
            while (line.charAt(++j) <= ' ') {
            }
            ++fieldCount;
            while (++j<n && line.charAt(j) > ' ') {
            }
        }
        String[] fields = new String[fieldCount];
        if (fields.length > 0) {
            fieldCount = -1;
            int start = 0;
            j = -1;
            while (++j < n && line.charAt(j) > ' ') {
            }
            fields[++fieldCount] = line.substring(start, j);
            while (j < n && ++fieldCount < limit) {
                while (line.charAt(++j) <= ' ') {
                }
                start = j;
                while (++j < n && line.charAt(j) > ' ') {
                }
                if (fieldCount < limit - 1) {
                    fields[fieldCount] = line.substring(start, j);
                }
                else {
                    fields[fieldCount] = line.substring(start);
                }
            }
        }
        return fields;
    }
}
