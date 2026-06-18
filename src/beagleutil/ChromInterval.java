/*
 * Copyright 2012-2013 Brian L. Browning
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
package beagleutil;

import blbutil.Const;
import main.Marker;

/* Code review on 27 Apr 2013 */

/**
 * Class <code>ChromInterval</code> represents a chromosome interval.
 * The class enforces now restrictions on interpretation of the
 * integers representing the starting and ending markers.  These
 * integers can represent marker indices or base positions.
 *
 * @author Brian L. Browning <browning@uw.edu>
*/
public final class ChromInterval implements IntInterval,
        Comparable<ChromInterval> {

    private final int chromIndex;
    private final int start;
    private final int end;

    /**
     * Construct a new <code>ChromInterval</code> instance.
     *
     * @param chrom the chromosome,
     * @param start the first marker in the interval.
     * @param end the last marker in the interval.
     *
     * @throws IllegalArgumentException if
     * <code>start.chromIndex() != end.chromIndex()</code>, or if
     * <code>start.pos() < 0 || start.end() > end</code>.
     * @throws NullPointerException if
     * <code>chrom==null || chrom.isEmpty()</code>.
     */
    public ChromInterval(Marker start, Marker end) {
        if (start.chromIndex() != end.chromIndex()) {
            String s = "start.chromIndex() != end.chromIndex()";
            throw new IllegalArgumentException(s);
        }
        if (start.pos() < 0 || start.pos() > end.pos()) {
            String s = "start=" + start + " end=" + end;
            throw new IllegalArgumentException(s);
        }
        this.chromIndex = start.chromIndex();
        this.start = start.pos();
        this.end = end.pos();
    }

    /**
     * Construct a new <code>ChromInterval</code> instance.
     *
     * @param chrom the chromosome,
     * @param start the first position in the interval.
     * @param end the last position in the interval.
     *
     * @throws IllegalArgumentException if <code>start < 0 || start > end</code>.
     * @throws NullPointerException if
     * <code>chrom==null || chrom.isEmpty()</code>.
     */
    public ChromInterval(String chrom, int start, int end) {
        if (start < 0 || start > end) {
            String s = "start=" + start + " end=" + end;
            throw new IllegalArgumentException(s);
        }
        this.chromIndex = ChromIds.indexOf(chrom);
        this.start = start;
        this.end = end;
    }

    /**
     * Returns <code>ChromInterval</code> instance corresponding to the
     * specified string, or returns <code>null</code> if <code>ci==null</code>
     * or if <code>ci</code> is not a valid chromosome interval.
     * If the specified string does not contain a start position,
     * the <code>start()</code> method of the returned
     * <code>ChromInterval</code> returns 0.  If no end position is specified,
     * the <code>end()</code> method of the returned <code>ChromInterval</code>
     * returns <code>Integer.MAX_VALUE</code>.
     * <br/>
     * A chromosomal segment has the format:
     * <br/>
     * <code>[chrom]:[start]-[end]</code>, <br/>
     * <code>[chrom]</code>, <br/>
     * <code>[chrom]:</code>, <br/>
     * <code>[chrom]:[start]-</code>, or <br/>
     * <code>[chrom]:-end</code>, where <br/>
     * <br/>
     * <code>[chrom]</code> is a string chromosome identifier, and
     * <code>[start]</code> and </code>[end]</code> are non-negative integers
     * satisfying <code>[start] < [end]</code>.
     *
     * @return <code>ChromInterval</code> instance corresponding to the
     * specified string, or returns <code>null</code> if  <code>ci==null</code>
     * or if <code>ci</code> is not a valid chromosome interval.
     */
    public static ChromInterval parse(String ci) {
        if (ci==null) {
            return null;
        }
        ci = ci.trim();
        int length = ci.length();
        int start = 0;
        int end = Integer.MAX_VALUE;
        int chrDelim = ci.lastIndexOf(Const.colon);
        int posDelim = ci.lastIndexOf(Const.hyphen);
        if (length==0) {
            return null;
        }
        else if (chrDelim == -1) {
            return new ChromInterval(ci, start, end);
        }
        else if (chrDelim == length -1) {
            return new ChromInterval(ci.substring(0, length-1), start, end);
        }
        else {
            if ( (posDelim == -1) || (posDelim <= chrDelim)
                        || (chrDelim == length-2)
                        || (isValidPos(ci, chrDelim+1, posDelim)==false)
                        || (isValidPos(ci, posDelim+1, length)==false) ) {
                return null;
            }
            if (posDelim - chrDelim > 1) {
                start = Integer.parseInt(ci.substring(chrDelim+1, posDelim));
            }
            if (length - posDelim > 1) {
                end = Integer.parseInt(ci.substring(posDelim+1, length));
            }
            if (start < 0 || start > end) {
                return null;
            }
        }
        return new ChromInterval(ci.substring(0, chrDelim), start, end);
    }

    private static boolean isValidPos(String s, int startIndex,
            int endIndex) {
        if (startIndex==endIndex) {
            return true;
        }
        int length = endIndex - startIndex;
        if ((length > 1) && s.charAt(startIndex)==0) {
            return false;
        }
        for (int j=startIndex; j<endIndex; ++j) {
            char c = s.charAt(j);
            if (Character.isDigit(c)==false) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the chromosome index which is equivalent to
     * <code>ChromIds.indexOf(chrom())</code>.
     * @return the chromosome index which is equivalent to
     * <code>ChromIds.indexOf(chrom())</code>.
     */
    public int chromIndex() {
        return chromIndex;
    }

    /**
     * Returns the chromosome.
     * @return the chromosome.
     */
    public String chrom() {
        return ChromIds.id(chromIndex);
    }

    /**
     * Returns the first marker in the interval.
     * @return the first marker in the interval.
     */
    @Override
    public int start() {
        return start;
    }

    /**
     * Returns the last marker in the interval
     * @return the last marker in the interval.
     */
    @Override
    public int end() {
        return end;
    }

    /*
     * Compares this <code>ChromInteval</code> with the specified
     * <code>ChromInterval</code> instance for order, and
     * returns -1, 0, or 1 depending on whether <code>this</code>
     * is less than, equal or greater than the specified instance.
     * </p>
     *
     * <code>ChromInterval</code> objects are ordered first by
     * <code>ChromIds.indexOf(this.chrom())</code>, then by
     * <code>this.start()</code>, and finally by <code>this.end()</code>.
     * Integers are ordered in ascending order.
     */
    @Override
    public int compareTo(ChromInterval o) {
        if (this.chromIndex != o.chromIndex) {
            return (this.chromIndex < o.chromIndex) ? -1 : 1;
        }
        if (this.start != o.start) {
            return (this.start < o.start) ? -1 : 1;
        }
        if (this.end != o.end) {
            return (this.end < o.end) ? -1 : 1;
        }
        return 0;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + this.chromIndex;
        hash = 67 * hash + this.start;
        hash = 67 * hash + this.end;
        return hash;
    }

    /**
     * Returns <code>true</code> if the specified object is a
     * <code>ChromInterval</code> instance representing the same
     * chromosome, starting marker, and ending marker as <code>this</code>,
     * and returns <code>false</code> otherwise.
     *
     * @param obj the object to be compared with <code>this</code> for
     * equality.
     * @return <code>true</code> if the specified object equals
     * <code>this</code>is and returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ChromInterval other = (ChromInterval) obj;
        if (this.chromIndex != other.chromIndex) {
            return false;
        }
        if (this.start != other.start) {
            return false;
        }
        if (this.end != other.end) {
            return false;
        }
        return true;
    }

    /**
     * Returns the string:
     * <code>this.chrom() + ":" + this.start() + "-" + this.end()</code>.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ChromIds.id(chromIndex));
        sb.append(Const.colon);
        sb.append(start);
        sb.append(Const.hyphen);
        sb.append(end);
        return sb.toString();
    }

    /**
     * Returns <code>true</code> if the specified chromosome intervals
     * have non-empty intersection and returns <code>false</code> otherwise.
     * @param a a chromosome interval.
     * @param b a chromosome interval.
     * @return <code>true</code> if the specified chromosome intervals
     * have non-empty intersection and returns <code>false</code> otherwise.
     */
    public static boolean overlap(ChromInterval a, ChromInterval b) {
        if (a.chromIndex() != b.chromIndex()) {
            return false;
        }
        else {
            return (a.start() <= b.end()) && (b.start() <= a.end());
        }
    }

    /**
     * Returns the union of the specified chromosome intervals.
     * @param a a chromosome interval.
     * @param b a chromosome interval.
     * @return the union of the specified chromosome intervals.
     * @throws IllegalArgumentException if <code>overlap(a, b)==false</code>.
     */
    public static ChromInterval merge(ChromInterval a, ChromInterval b) {
        if (overlap(a, b)==false) {
            String s = "non-overlappng intervals: " + a + " " + b;
            throw new IllegalArgumentException(s);
        }
        int start = Math.min(a.start(), b.start());
        int end = Math.max(a.end(), b.end());
        return new ChromInterval(a.chrom(), start, end);
    }
}
