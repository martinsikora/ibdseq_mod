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

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/* Code review on 07 Jan 2013 */

/**
 * Class Validate contains methods for validating command line
 * arguments.  The "s" (resp. "m") prefix in method names indicate that
 * single (resp. multiple) command line values are required.
 */
public final class Validate {

    /**
     * Checks whether the specified map is empty, and exits the
     * java virtual machine with an error message if the
     * specified map is non-empty.
     *
     * @param argsMap a multimap of <string, value> pairs.
     * @throws NullPointerException if <code>argsMap==null</code>.
     */
    public static void confirmEmptyMap(Map<String, String[]> argsMap) {
        Set<String> keySet = argsMap.keySet();
        if (keySet.size() > 0) {
            StringBuilder sb = new StringBuilder(100);
            sb.append("Error: unrecognized parameter");
            sb.append(keySet.size()==1 ? ":" : "s:");
            for (String key : keySet) {
                String[] value = argsMap.get(key);
                for (String s : value) {
                    sb.append(' ');
                    sb.append(key);
                    sb.append('=');
                    sb.append(s);
                }
            }
            Utilities.exit(sb.toString(), new IllegalArgumentException());
        }
    }

    /**
     * Returns a <code>File</code> array corresponding to the filenames
     * in the specified string array.
     * @param filenames an array of string filenames.
     * @return a <code>File</code> array whose elements correspond to the
     * string filename elements in the specified string array.
     *
     * @throws NullPointerException if <code>filenames==null</code>
     * or if <code>filenames[j]==null</code> for any
     * <code>0 &le j < filenames.length</code>.
     */
    public static File[] getFiles(String[] filenames) {
        File[] fa = new File[filenames.length];
        for (int j = 0; j < fa.length; ++j) {
            fa[j] = new File(filenames[j]);
        }
        return fa;
    }

    /**
     * Returns a File corresponding to the specified string filename or
     * <code>null</code> if <code>filename==null</code>.
     *
     * @param filename a filename.
     *
     * @throws IllegalArgumentException if the specified filename
     * is not <code>null</code> and the file does not exist, is a directory,
     * or contains aline with more than one white-space delimited field.
     */
    public static File getFile(String filename) {
        if (filename==null) {
            return null;
        }
        else {
            File file = new File(filename);
            if (file.exists()==false) {
                String s = "File does not exist: " + file;
                throw new IllegalArgumentException(s);
            }
            if (file.isDirectory()) {
                String s = "File is a directory: " + file;
                throw new IllegalArgumentException(s);
            }
            return file;
        }
    }

    /**
     * Returns the integer value corresponding to the specified key.
     *
     * @param key a string key associated with a <code>String[]</code> value.
     * @param argsMap a map of string keys to <code>String[]</code> values.
     * @param isRequired <code>true</code> if the specified <code>key</code>
     * is required to be in the specified <code>argsMap</code>.
     * @param defaultValue the value that will be returned if
     *   <code>isRequired==false &&
     *   (argsMap.get(key)==null || argsMap.get(key).length==0)</code>.
     * @param min the minimum valid integer value
     * @param max the maximum valid integer value
     *
     * @return the integer value corresponding to the specified key.
     *
     * @throws NullPointerException if <code>key==null || map==null</code>.
     * @throws IllegalArgumentException if
     * <code>(min > max) || (defaultValue < min) || (defaultValue > max)</code>,
     * if <code>isrequired &&
     *   (argsMap.get(key)==null || argsMap.get(key).length==0)</code>,
     * if <code>argsMap.getKey(key) != null && argsMap.get(key).length > 1</code>,
     * or if <code>argsMap.getKey(key) != null && argsMap.get(key).length == 1
     * &&(Integer.parseInt(argsMap.get(key)[0]) < min
     *   || Integer.parseInt(argsMap.get(key)[0]) > max)</code>.
     *
     * @throws NumberFormatException if
     * <code>argsMap.getKey != null && argsMap.get(key).length == 1</code>
     * and <code>argsMap.get(key)</code> is not a parsable <code>int</code>.
     */
    public static int sInt(String key, Map<String, String[]> argsMap,
            boolean isRequired, int defaultValue, int min, int max) {
        checkValue(key, defaultValue, min, max);
        String[] values = argsMap.remove(key);
        checkLength(key, values, isRequired);
        if (values==null || values.length==0) {
            return defaultValue;
        }
        return parseInt(key, values[0], min, max);
    }

    /**
     * Returns the long value corresponding to the specified key.
     *
     * @param key a string key associated with a <code>String[]</code> value.
     * @param argsMap a map of string keys to <code>String[]</code> values.
     * @param isRequired <code>true</code> if the specified <code>key</code>
     * is required to be in the specified <code>argsMap</code>.
     * @param defaultValue the value that will be returned if
     *   <code>isRequired==false &&
     *   (argsMap.get(key)==null || argsMap.get(key).length==0)</code>.
     * @param min the minimum valid long value
     * @param max the maximum valid long value
     *
     * @return the long value corresponding to the specified key.
     *
     * @throws NullPointerException if <code>key==null || map==null</code>.
     * @throws IllegalArgumentException if
     * <code>min > max || defaultValue < min || defaultValue > max</code>,
     * if <code>isrequired &&
     *   (argsMap.get(key)==null || argsMap.get(key).length==0)</code>,
     * if <code>argsMap.getKey(key) != null && argsMap.get(key).length > 1</code>,
     * or if <code>argsMap.getKey(key) != null && argsMap.get(key).length == 1
     *  && (Long.parseLong(argsMap.get(key)[0]) < min
     *   || Long.parseLong(argsMap.get(key)[0]) > max)</code>.
     *
     * @throws NumberFormatException if
     * <code>argsMap.getKey != null && argsMap.get(key).length == 1</code>
     * and <code>argsMap.get(key)</code> is not a parsable <code>long</code>.
     */
    public static long sLong(String key, Map<String, String[]> argsMap,
            boolean isRequired, long defaultValue, long min, long max) {
        checkValue(key, defaultValue, min, max);
        String[] values = argsMap.remove(key);
        checkLength(key, values, isRequired);
        if (values==null || values.length==0) {
            return defaultValue;
        }
        return parseLong(key, values[0], min, max);
    }

    /**
     * Returns the float value corresponding to the specified key.
     *
     * @param key a string key associated with a <code>String[]</code> value.
     * @param argsMap a map of string keys to <code>String[]</code> values.
     * @param isRequired <code>true</code> if the specified <code>key</code>
     * is required to be in the specified <code>argsMap</code>.
     * @param defaultValue the value that will be returned if
     *   <code>isRequired==false &&
     *   (argsMap.get(key)==null || argsMap.get(key).length==0)</code>.
     * @param min the minimum valid float value
     * @param max the maximum valid float value
     *
     * @return the float value corresponding to the specified key.
     *
     * @throws NullPointerException if <code>key==null || map==null</code>.
     * @throws IllegalArgumentException if
     * <code>min > max || defaultValue < min || defaultValue > max</code>,
     * if <code>isrequired &&
     *   (argsMap.get(key)==null || argsMap.get(key).length==0)</code>,
     * if <code>argsMap.getKey(key) != null && argsMap.get(key).length > 1</code>,
     * or if <code>argsMap.getKey(key) != null && argsMap.get(key).length == 1
     * && (Float.parseFloat(argsMap.get(key)[0]) < min
     *   || Float.parseFloat(argsMap.get(key)[0]) > max)</code>.
     *
     * @throws NumberFormatException if
     * <code>argsMap.getKey != null && argsMap.get(key).length == 1</code>
     * and <code>argsMap.get(key)</code> is not a parsable <code>float</code>.
     */
    public static float sFloat(String key, Map<String, String[]> argsMap,
            boolean isRequired, float defaultValue, float min, float max) {
        checkValue(key, defaultValue, min, max);
        String[] values = argsMap.remove(key);
        checkLength(key, values, isRequired);
        if (values==null || values.length==0) {
            return defaultValue;
        }
        return parseFloat(key, values[0], min, max);
    }

    /**
     * Returns the double value corresponding to the specified key.
     *
     * @param key a string key associated with a <code>String[]</code> value.
     * @param argsMap a map of string keys to <code>String[]</code> values.
     * @param isRequired <code>true</code> if the specified <code>key</code>
     * is required to be in the specified <code>argsMap</code>.
     * @param defaultValue the value that will be returned if
     *   <code>isRequired==false &&
     *   (argsMap.get(key)==null || argsMap.get(key).length==0)</code>.
     * @param min the minimum valid double value
     * @param max the maximum valid double value
     *
     * @return the double value corresponding to the specified key.
     *
     * @throws NullPointerException if <code>key==null || map==null</code>.
     * @throws IllegalArgumentException if
     * <code>min > max || defaultValue < min || defaultValue > max</code>,
     * if <code>isrequired &&
     *   (argsMap.get(key)==null || argsMap.get(key).length==0)</code>,
     * if <code>argsMap.getKey(key) != null && argsMap.get(key).length > 1</code>,
     * or if <code>argsMap.getKey(key) != null && argsMap.get(key).length == 1
     * && (double.parseDouble(argsMap.get(key)[0]) < min
     *   || Double.parseDouble(argsMap.get(key)[0]) > max)</code>.
     *
     * @throws NumberFormatException if
     * <code>argsMap.getKey != null && argsMap.get(key).length == 1</code>
     * and <code>argsMap.get(key)</code> is not a parsable <code>double</code>.
     */
    public static double sDouble(String key, Map<String, String[]> argsMap,
            boolean isRequired, double defaultValue, double min, double max) {
        checkValue(key, defaultValue, min, max);
        String[] values = argsMap.remove(key);
        checkLength(key, values, isRequired);
        if (values==null || values.length==0) {
            return defaultValue;
        }
        return parseDouble(key, values[0], min, max);
    }

    /**
     * Returns the boolean value corresponding to the specified key.  True
     * is returned the specified value <code>v</code> satisfies
     * <code>(v.equalsIgnoreCase("true") || v.equalsIgnoreCase("t"))</code>
     * and <code>false</code> is returned if the specified value
     * <code>v</code> satisfies
     * <code>(v.equalsIgnoreCase("false") || v.equalsIgnoreCase("f"))</code>.
     *
     * @param key a string key associated with a <code>String[]</code> value.
     * @param argsMap a map of string keys to <code>String[]</code> values.
     * @param isRequired <code>true</code> if the specified <code>key</code>
     * is required to be in the specified <code>argsMap</code>.
     * @param defaultValue the value that will be returned if
     *   <code>isRequired==false &&
     *   (argsMap.get(key)==null || argsMap.get(key).length==0)</code>.
     *
     * @return the boolean value corresponding to the specified key.
     *
     * @throws NullPointerException if <code>key==null || map==null</code>.
     * @throws IllegalArgumentException if <code>isrequired &&
     *   (argsMap.get(key)==null || argsMap.get(key).length==0)</code>,
     * if <code>argsMap.getKey(key)!=null && argsMap.get(key).length > 1</code>,
     * or if <code>argsMap.getKey(key)!=null && argsMap.get(key).length==1</code>
     * and
     * <code>(v.equalsIgnoreCase("true") || v.equalsIgnoreCase("t"))
     *   || v.equalsIgnoreCase("false") || v.equalsIgnoreCase("f")) == false
     * </code> where <code>v = argsMap.get(key)[0]</code>.
     */
    public static boolean sBoolean(String key, Map<String, String[]> argsMap,
            boolean isRequired, boolean defaultValue) {
        String[] values = argsMap.remove(key);
        checkLength(key, values, isRequired);
        if (values==null || values.length==0) {
            return defaultValue;
        }
        return parseBoolean(values[0]);
    }

    /**
     * Returns the string value corresponding to the specified key.
     *
     * @param key a string key associated with a <code>String[]</code> value.
     * @param argsMap a map of string keys to <code>String[]</code> values.
     * @param isRequired <code>true</code> if the specified <code>key</code>
     * is required to be in the specified <code>argsMap</code>.
     * @param defaultValue the value that will be returned if
     *   <code>isRequired==false &&
     *   (argsMap.get(key)==null || argsMap.get(key).length==0)</code>.
     * @param possibleValues a string array of permitted values for the
     * specified key or <code>null</code> if there are no restrictions on
     * possible values.  If <code>possibleValues!=null</code>, the returned
     * string will equal an element of the <code>possibleValues</code> array.
     * @return the string value corresponding to the specified key.
     *
     * @throws NullPointerException if <code>key==null || map==null</code>.
     * @throws IllegalArgumentException if
     * <code>defaultValue</code> is not equal to an element of the
     * <code>possibleValues</code> array, or if <code>isRequired &&
     *   (argsMap.get(key)==null || argsMap.get(key).length==0)</code>,
     * if <code>argsMap.getKey(key) != null && argsMap.getKey(key).length> 1 </code>,
     * or if <code>argsMap.getKey(key) != null && argsMap.get(key).length == 1
     * && possibleValues ! = null</code>
     * and <code>argsMap.get(key)[0]</code> is not equal to an element of the
     * <code>possibleValues</code> array.
     */
    public static String sString(String key, Map<String, String[]> argsMap,
            boolean isRequired, String defaultValue, String[] possibleValues) {
        checkValue(key, defaultValue, possibleValues);
        String[] values = argsMap.remove(key);
        checkLength(key, values, isRequired);
        if (values==null || values.length==0) {
            return defaultValue;
        }
        checkValue(key, values[0], possibleValues);
        return values[0];
    }

    /**
     * Returns the string array value corresponding to the specified key.  Zero
     * length arrays may be returned.
     *
     * @param key a string key associated with a <code>String[]</code> value.
     * @param argsMap a map of string keys to <code>String[]</code> values.
     * @param isRequired <code>true</code> if the specified <code>key</code>
     * is required to be in the specified <code>argsMap</code>.
     * @param possibleValues a string array of permitted values for the
     * specified key or <code>null</code> if there are no restrictions on
     * possible values.  If <code>possibleValues != null</code>, each element
     * of the returned string array will equal one of the permitted
     * string values.
     * @return the string array corresponding to the specified key.
     *
     * @throws NullPointerException if <code>key==null || map==null</code>.
     * @throws IllegalArgumentException if <code>isRequired
     *   && (argsMap.getKey(key)==null || argsMap.get(key).length == 0</code>,
     * or if <code>possibleValues!=null</code> and any element of
     * <code>argsMap.getKey(key)</code> is not equal to
     * an element of <code>possibleValues</code>.
     */
    public static String[] mString(String key, Map<String, String[]> argsMap,
            boolean isRequired, String[] possibleValues) {
        String[] values = argsMap.remove(key);
        if (isRequired && (values==null || values.length==0)) {
            throw new IllegalArgumentException("missing " + key + " argument");
        }
        else if (values==null) {
            return new String[0];
        }
        else {
            for (String s : values) {
                checkValue(key, s, possibleValues);
            }
            return values;
        }
    }

    /**
     * Returns the string value corresponding to the specified key.
     *
     * @param key a string key associated with a <code>String[]</code> value.
     * @param argsMap a map of <code>String</code> keys to
     * <code>String[]</code> values.
     * @param isRequired <code>true</code> if the specified <code>key</code>
     * is required to be in the specified <code>argsMap</code>.
     * @param defaultValue the value that will be returned if
     *   <code>isRequired==false &&
     *   (argsMap.get(key)==null || argsMap.get(key).length==0)</code>.
     * @param possibleChars a character array giving the set of characters
     * from which the returned value is constructed, or <code>null</code>
     * if there are no restrictions characters comprising the returned value.
     * @return the string corresponding to the specified key.
     *
     * @throws NullPointerException if <code>key==null || map==null</code>.
     * @throws IllegalArgumentException if
     * <code>defaultValue</code> contains a character that is not equal to
     * any element of the specified <code>possibleChars</code> array, or
     * if <code>isRequired &&
     *   (argsMap.get(key)==null || argsMap.get(key).length==0)</code>,
     * if <code>argsMap.getKey(key) != null && argsMap.getKey(key).length > 1 </code>,
     * or if <code>argsMap.getKey(key) != null && argsMap.get(key).length == 1
     * && possibleChars!=null</code>
     * and any character of <code>argsMap.get(key)[0]</code> is not equal to
     * any element of the <code>possibleValues</code> array.
     */
    public static String mChar(String key, Map<String, String[]> argsMap,
            boolean isRequired, String defaultValue, char[] possibleChars) {
        checkValue(key, defaultValue, possibleChars);
        String[] values = argsMap.remove(key);
        checkLength(key, values, isRequired);
        if (values==null || values.length==0) {
            return defaultValue;
        }
        checkValue(key, values[0], possibleChars);
        return values[0];
    }

    private static void checkLength(String key, String[] values,
            boolean isRequired) {
        if (values==null || values.length==0) {
            if (isRequired) {
                String s = "missing " + key + " argument";
                throw new IllegalArgumentException(s);
            }
        }
        else if (values.length > 1) {
            String s = "Error in " + key
                    + " parameter: key has multiple values: "
                    + java.util.Arrays.toString(values);
            throw new IllegalArgumentException(s);
        }
    }

    private static int parseInt(String key, String toParse, int min, int max) {
        assert toParse!=null;
        try {
            int i = Integer.parseInt(toParse);
            checkValue(key, i, min, max);
            return i;
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(toParse + " is not a number");
        }
    }

    private static long parseLong(String key, String toParse, long min, long max) {
        assert toParse!=null;
        try {
            long l = Long.parseLong(toParse);
            checkValue(key, l, min, max);
            return l;
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(toParse + " is not a number");
        }
    }

    private static float parseFloat(String key, String toParse, float min,
            float max) {
        assert toParse!=null;
        try {
            float f = Float.parseFloat(toParse);
            checkValue(key, f, min, max);
            return f;
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(toParse + " is not a number");
        }
    }

    private static double parseDouble(String key, String toParse, double min,
            double max) {
        assert toParse!=null;
        try {
            double d = Double.parseDouble(toParse);
            checkValue(key, d, min, max);
            return d;
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(toParse + " is not a number");
        }
    }

    private static boolean parseBoolean(String s) {
        assert s!= null;
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("t")) {
            return true;
        }
        else if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("f")) {
            return false;
        }
        else {
            String msg = s + " is not \"true\" or \"false\"";
            throw new IllegalArgumentException(msg);
        }
    }

    private static void checkValue(String key, int value, int min, int max) {
        String s = null;
        if (min > max) {
            s = "min=" + min + " > max=" + max;
        }
        if (value < min) {
            s = "value=" + value + " < " + min;
        }
        if (value > max) {
            s = "value=" + value + " > " + max;
        }
        if (s != null) {
            String prefix = "Error in \"" + key + "\" argument: ";
            throw new IllegalArgumentException(prefix + s);
        }
    }

    private static void checkValue(String key, long value, long min, long max) {
        String s = null;
        if (min > max) {
            s = "min=" + min + " > max=" + max;
        }
        if (value < min) {
            s = "value=" + value + " < " + min;
        }
        if (value > max) {
            s = "value=" + value + " > " + max;
        }
        if (s != null) {
            String prefix = "Error in \"" + key + "\" argument: ";
            throw new IllegalArgumentException(prefix + s);
        }
    }

    private static void checkValue(String key, double value, double min,
            double max) {
        String s = null;
        if (min > max) {
            s = "min=" + min + " > max=" + max;
        }
        if (value < min) {
            s = "value=" + value + " < " + min;
        }
        if (value > max) {
            s = "value=" + value + " > " + max;
        }
        if (s != null) {
            String prefix = "Error in \"" + key + "\" argument: ";
            throw new IllegalArgumentException(prefix + s);
        }
    }

    private static void checkValue(String key, String value,
            String[] possibleValues) {
        if (possibleValues != null) {
            boolean foundMatch = false;
            for (int j=0; j<possibleValues.length && foundMatch==false; ++j) {
                String s = possibleValues[j];
                foundMatch = s==null ? value==null : s.equals(value);
            }
            if (!foundMatch) {
                String s = "Error in \"" + key + "\" argument: \"" + value
                        + "\" is not in " + Arrays.toString(possibleValues);
                throw new IllegalArgumentException(s);
            }
        }
    }

    private static void checkValue(String key, String value,
            char[] possibleValues) {
        for (int j=0, n=value.length(); j<n; ++j) {
            char c = value.charAt(j);
            if (possibleValues != null) {
                boolean foundMatch = false;
                for (int k=0; k<possibleValues.length && foundMatch==false; ++k) {
                    foundMatch = possibleValues[k]==c;
                }
                if (!foundMatch) {
                    String s = "Error in \"" + key + "\" argument: character \""
                            + value + "\" is not in " +
                            Arrays.toString(possibleValues);
                    throw new IllegalArgumentException(s);
                }
            }
        }
    }
}
