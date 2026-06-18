/*
 * Copyright 2011 Brian L. Browning
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
package vcf;

import blbutil.Filter;
import blbutil.Utilities;
import java.io.File;
import java.util.Set;

/* Code review on 10 May 2013 */

/**
 * Class <code>VcfUtils</code> contains utilities used in parsing VCF files.
 */
public final class VcfUtils {

    private VcfUtils() {
        // private constructor to prevent instantiation
    }

    /**
     * A <code>Filter<String></code> whose accept method returns
     * <code>true</code> if the parameter is not <code>null</code> and throws a
     * <code>NullPointerException</code> if the parameter is <code>null</code>.
     */
    public static Filter<String> ACCEPT_ALL_FILTER = new Filter<String>() {
        @Override
        public boolean accept(String id) {
            if (id==null) {
                throw new NullPointerException(id);
            }
            return true;
        }
    };

    /**
     * Class <code>IncludeSampleFilter</code> will exclude all samples that are
     * not present in a specified list of samples.
     */
    public static class IncludeSampleFilter implements Filter<String> {
        private final Set<String> includedSampleIds;

        /**
         * Constructs an <code>IncludeSampleFilter</code> instance.
         * @param inclSampleFile a file with one sample identifier per line.
         * @throws IllegalArgumentException if the specified file contains a line
         * with more than one white-space-delimited field.
         * @throws NullPointerException if <code>inclSampleFile==null</code>.
         */
        public IncludeSampleFilter(File inclSampleFile) {
            if (inclSampleFile==null) {
                throw new NullPointerException("inclSampleFile==null");
            }
            this.includedSampleIds = Utilities.getIdSet(inclSampleFile);
        }

        @Override
        public boolean accept(String id) {
            return includedSampleIds.contains(id);
        }
    }

    /**
     * Class <code>ExcludeSampleFilter</code> will exclude all samples that are
     * present in a specified list of samples.
     */
    public static class ExcludeSampleFilter implements Filter<String> {
        private final Set<String> excludedSampleIds;

        /**
         * Constructs an <code>ExcludeSampleFilter</code> instance.
         * @param exclSampleFile a file with one sample identifier per line.
         * @throws IllegalArgumentException if the specified file contains a line
         * with more than one white-space-delimited field.
         * @throws NullPointerException if <code>exclSampleFile==null</code>.
         */
        public ExcludeSampleFilter(File exclSampleFile) {
            if (exclSampleFile==null) {
                throw new NullPointerException("exclSampleFile==null");
            }
            this.excludedSampleIds = Utilities.getIdSet(exclSampleFile);
        }

        @Override
        public boolean accept(String id) {
            return (excludedSampleIds.contains(id)==false);
        }
    }

    /**
     * Class <code>SampleFilter</code> will exclude all samples that are
     * not present in a list of included samples, OR that are present
     * in a list of excluded samples.
     */
    public static class SampleFilter implements Filter<String> {
        private final Filter<String> inclFilter;
        private final Filter<String> exclFilter;

        /**
         * Constructs an <code>SampleFilter</code> instance.
         * @param inclSampleFile a file with one sample identifier per line.
         * @param exclSampleFile a file with one sample identifier per line.
         * @throws IllegalArgumentException if either specified file contains
         * a line with more than one white-space-delimited field.
         * @throws NullPointerException if
         * <code>inclSampleFile==null || exclSampleFile==null</code>.
         */
        public SampleFilter(File inclSampleFile, File exclSampleFile) {
            this.inclFilter = new IncludeSampleFilter(inclSampleFile);
            this.exclFilter = new ExcludeSampleFilter(exclSampleFile);
        }

        @Override
        public boolean accept(String id) {
            return ( inclFilter.accept(id) && exclFilter.accept(id) );
        }
    }

    /**
     * Returns the genotype index for the specified pair of alleles.
     * @param a1 the first allele.
     * @param a2 the second allele.
     * @return the genotype index for the specified pair of alleles.
     * @throws IllegalArgumentException if
     * <code>a1 < 0 || a2 < 0</code>.
     */
    public static int genotypeIndex(byte a1, byte a2) {
        if (a1 < 0) {
            throw new IllegalArgumentException("a1<0: " + a1);
        }
        if (a2 < 0) {
            throw new IllegalArgumentException("a2<0: " + a2);
        } else if (a1 < a2) {
            return (a2 * (a2 + 1)) / 2 + a1;
        } else {
            return (a1 * (a1 + 1)) / 2 + a2;
        }
    }

    /**
     * Returns <code>string.substring(prefix.length())</code>.
     * @param string a string beginning with the specified prefix.
     * @param prefix a prefix to the specified string.
     * @return <code>string.substring(prefix.length())</code>.
     *
     * @throws IllegalArgumentException if the specified string does not
     * begin with the specified prefix.
     * @throws NullPointerException if
     * <code>string==null || prefix==null</code>.
     */
    public static String stripPrefix(String string, String prefix) {
        if (string.startsWith(prefix)==false) {
            String s = "string=" + string + " prefix=" + prefix;
            throw new IllegalArgumentException(s);
        }
        return string.substring(prefix.length());
    }

    /**
     * Returns
     * <code>string.substring(0, string.length() - suffix.length())</code>.
     * @param string a string ending with the specified suffix.
     * @param suffix a suffix to the specified string.
     * @return <code>string.substring(0, string.length() - suffix.length())</code>.
     *
     * @throws IllegalArgumentException if the specified string does not
     * end with the specified suffix.
     * @throws NullPointerException if <code>string==null || suffix==null</code>.
     */
    public static String stripSuffix(String string, String suffix) {
        if (string.endsWith(suffix)==false) {
            String s = "string=" + string + " suffix=" + suffix;
            throw new IllegalArgumentException(s);
        }
        return string.substring(0, string.length() - suffix.length());
    }

    /**
     * Returns <code>string.substring(1, string.length()-1)</code>.
     * @param string a string beginning and ending with the specified quote
     * characters.
     * @param leftQuote the first character of the specified string.
     * @param rightQuote the last character of the specified string.
     * @return <code>string.substring(1, string.length()-1)</code>.
     *
     * @throws IllegalArgumentException if the specified string does not
     * begin and end with the specified quote characters.
     * @throws NullPointerException if <code>string==null</code>.
     */
    public static String stripQuotes(String string, char leftQuote,
            char rightQuote) {
        if (string.length() < 2 || string.charAt(0)!=leftQuote
                || string.charAt(string.length()-1)!=rightQuote) {
            String s = "string=" + string + " leftQuote=" + leftQuote
                    + " rightQuote=" + rightQuote;
            throw new IllegalArgumentException(s);
        }
        return string.substring(1, string.length()-1);
    }


    /**
     * Returns the value in the specified key-value pair.   The string
     * must begin with the specified key, and the key and value
     * must be separated by the specified delimiter.
     * @param string a string representing a key-value pair.
     * @param key the key in a key-value pair.
     * @param delimiter the delimiter separating the key and value in a
     * key-value pair.
     * @return the value in the specified key-value pair.
     * @throws IllegalArgumentException if
     * <code>string.length() < (key.length() + 2)
     *           || string.startsWith(key)==false
     *           || string.charAt(key.length())!=delim</code>
     * @throws NullPointerException if <code>string==null || key==null</code>.
     */
    public static String value(String string, String key, char delimiter) {
        if (string.length() < (key.length() + 2)
                || string.startsWith(key)==false
                || string.charAt(key.length())!=delimiter) {
            String s = "string=" + string + " key=" + key + " delimiter="
                    + delimiter;
            throw new IllegalArgumentException(s);
        }
        return string.substring(key.length() + 1);
    }

    /**
     * Returns the value in the specified key-value pair.  The string
     * must begin with the specified key, and the key and value
     * must be separated by the specified left delimiter.  If the
     * specified right delimiter occurs after the left delimiter in the
     * specified string then the left and right delimiters delimit the
     * value, and otherwise the left delimiter and end of the string
     * delimit the value.
     *
     * @param string a string representing a key-value pair.
     * @param key the key in a key-value pair.
     * @param leftDelimiter the delimiter separating the key and value in a
     * key-value pair.
     * @param rightDelimiter the delimiter denoting the end of the value.
     *
     * @return the value in the specified key-value pair.
     *
     * @throws IllegalArgumentException if
     * <code>string.length() < (key.length() + 2)
     *           || string.startsWith(key)==false
     *           || string.charAt(key.length())!=delim</code>
     * @throws NullPointerException if <code>string==null || key==null</code>.
     */
    public static String value(String string, String key,
            char leftDelimiter, char rightDelimiter) {
        if (string.length() < (key.length() + 2)
                || string.startsWith(key)==false
                || string.charAt(key.length())!=leftDelimiter) {
            String s = "string=" + string + " key=" + key + " leftDelimiter="
                    + leftDelimiter + " rightDelimiter=" + rightDelimiter;
            throw new IllegalArgumentException(s);
        }
        int start = key.length() + 1;
        int endIndex = string.indexOf(rightDelimiter, start);
        if (endIndex==-1) {
            endIndex = string.length();
        }
        return string.substring(key.length() + 1, endIndex);
    }

    /**
     * Strips the specified key-value pair from the start of
     * the specified string returns the resulting string.  The string
     * must begin with the specified key, and the key and value
     * must be separated by the specified left delimiter.  If the
     * specified right delimiter occurs after the left delimiter in the
     * specified string then the left and right delimiters delimit the
     * value, and otherwise the left delimiter and end of the string
     * delimit the value.
     * @param string a string representing a key-value pair.
     * @param key the key in a key-value pair.
     * @param leftDelimiter the delimiter separating the key and value in a
     * key-value pair.
     * @param rightDelimiter the delimiter denoting the end of the value.
     * @return the value in the specified key-value pair.
     *
     * @throws IllegalArgumentException if
     * <code>string.length() < (key.length() + 2)
     *           || string.startsWith(key)==false
     *           || string.charAt(key.length())!=delim</code>
     * @throws NullPointerException if <code>string==null || key==null</code>.
     */
    public static String stripKeyValuePair(String string, String key,
            char leftDelimiter, char rightDelimiter) {
        if (string.length() < (key.length() + 2)
                || string.startsWith(key)==false
                || string.charAt(key.length())!=leftDelimiter) {
            String s = "string=" + string + " key=" + key + " leftDelimiter="
                    + leftDelimiter + " rightDelimiter=" + rightDelimiter;
            throw new IllegalArgumentException(s);
        }
        int start = key.length() + 1;
        int endIndex = string.indexOf(rightDelimiter, start) + 1;
        if (endIndex==0) {
            endIndex = string.length();
        }
        return string.substring(endIndex);
    }
}
