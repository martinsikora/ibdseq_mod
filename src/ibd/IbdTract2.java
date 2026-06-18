/*
 * Copyright 2011-2012 Brian L. Browning
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
package ibd;

import beagleutil.IntInterval;
import blbutil.Const;
import blbutil.IntPair;
import java.text.DecimalFormat;
import java.util.Comparator;

/* Code Review on 03 Sep 2013  */

/**
 * Class <code>IbdTract2</code> represents an IBD tract for a pair of
 * haplotypes or a pair of samples.  Class <code>IbdTract2</code> is immutable.
 */
public final class IbdTract2 implements IntInterval, Comparable<IbdTract2> {

    private static final DecimalFormat df2 = new DecimalFormat("0.00");

    private final IntPair idPair;
    private final int start;
    private final int end;
    private final float score;

    /**
     * Constructs an <code>IbdTract2</code> instance.
     * @param id1 the first haplotype or sample index.
     * @param id2 the second haplotype or sample index.
     * @param start the starting marker index for the IBD segment (inclusive)
     * @param end the ending marker index for the IBD segment (inclusive).
     * @param score the score for the IBD segment.
     *
     * @throws IllegalArgumentException if <code>id1 < 0 || id2 < hap1
     * || start > end || score < 0.0</code>.
     */
    public IbdTract2(int id1, int id2, int start, int end, float score) {
        checkArguments(id1, id2, start, end, score);
        this.idPair = new IntPair(id1, id2);
        this.start = start;
        this.end = end;
        this.score = score;
    }

    /**
     * Constructs an <code>IbdTract2</code> instance.
     * @param idPair an ordered pair of indices.
     * @param start the starting marker index for the IBD segment (inclusive)
     * @param end the ending marker index for the IBD segment (inclusive).
     * @param score the score for the IBD segment.
     *
     * @throws NullPointerException if <code>idPair==null</code>.
     * @throws IllegalArgumentException if <code>idPair.first() < 0
     * || idPair.second() < idPair.first() || start >= end
     * || score < 0</code>.
     */
    public IbdTract2(IntPair idPair, int start, int end, float score) {
        checkArguments(idPair.first(), idPair.second(), start, end, score);
        this.idPair = idPair;
        this.start = start;
        this.end = end;
        this.score = score;
    }

    private void checkArguments(int id1, int id2, int start, int end,
            float score) {
        if (id1 < 0 || id2 < id1) {
            String s = "id1=" + id1 + " id2=" + id2;
            throw new IllegalArgumentException(s);
        }
        else if (start > end){
            String s = "start: " + start + " end: " + end;
            throw new IllegalArgumentException(s);
        }
        else if (score < 0.0f) {
           String s = "score < 0.0: " + score;
           throw new IllegalArgumentException(s);
        }
    }

    /**
     * Returns <code>true</code> if the specified object is an
     * <code>IbdTract2</code> instance and if this <code>IbdTract2</code> is
     * equal to the specified <code>IbdTract2</code> and returns
     * <code>false</code> otherwise.  Two <code>IbdTract2</code> instances
     * are equal if they have the same ordered pair of indices,
     * identical starting and ending marker indices, and identical scores.
     * @param o the reference object with which to compare.
     * @return <code>true</code> if the specified object is an
     * <code>IbdTract2</code> instance and if this <code>IbdTract2</code> is
     * equal to the specified <code>IbdTract2</code> and returns
     * <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this==o) {
            return true;
        }
        if ((o instanceof IbdTract2)==false) {
            return false;
        }
        IbdTract2 other = (IbdTract2) o;
        if (this.idPair.equals(other.idPair)==false) {
            return false;
        }
        if (this.start!=other.start) {
            return false;
        }
        if (this.end!=other.end) {
            return false;
        }
        return Float.compare(this.score, other.score)==0;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + (this.idPair != null ? this.idPair.hashCode() : 0);
        hash = 43 * hash + this.start;
        hash = 43 * hash + this.end;
        hash = 43 * hash + Float.floatToIntBits(this.score);
        return hash;
    }

    /**
     * Compares <code>this</code> IBD segment with the specified IBD segment
     * for order.  Returns -1, 0, or 1 as this IBD segment is less than,
     * equal, or greater than the specified object.  IBD segments are
     * ordered by their first sample or haplotype index, then by their
     * second sample or haplotype index, then by their starting marker index,
     * then by their ending marker index, and finally by their score
     * (using <code>Float.compare()</code>).
     *
     * @param o the IBD segment to be compared.
     * @return -1, 0, or 1 as this IBD segment is less than,
     * equal, or grater than the specified object.
     */
    @Override
    public int compareTo(IbdTract2 o) {
        int cmp = this.idPair.compareTo(o.idPair);
        if (cmp != 0) {
            return cmp;
        }
        if (this.start != o.start) {
            return (this.start < o.start) ? -1 : 1;
        }
        if (this.end != o.end) {
            return (this.end < o.end) ? -1 : 1;
        }
        return Float.compare(this.score, o.score);
    }

    /**
     * Returns a comparator which compares two IBD segments for order.
     * IBD segments are ordered by their starting marker index,
     * then by their ending marker index, then by their first sample or
     * haplotype index, then by their second sample or haplotype index,
     * and finally by their score (using <code>Float.compare()</code>).
     *
     * @param o the IBD segment to be compared.
     * @return -1, 0, or 1 as this IBD segment is less than,
     * equal, or grater than the specified object.
     */
    public static Comparator<IbdTract2> intervalComparator() {
        return new Comparator<IbdTract2>() {

            @Override
            public int compare(IbdTract2 o1, IbdTract2 o2) {
                if (o1.start != o2.start) {
                    return (o1.start < o2.start) ? -1 : 1;
                }
                if (o1.end != o2.end) {
                    return (o1.end < o2.end) ? -1 : 1;
                }
                int cmp = o1.idPair.compareTo(o2.idPair);
                if (cmp != 0) {
                    return cmp;
                }
                return Float.compare(o1.score, o2.score);
            }
        } ;
    }

    /**
     * Returns the first sample or haplotype index.
     * @return the first sample or haplotype index.
     */
    public int id1() {
        return idPair.first();
    }

    /**
     * Returns the second sample or haplotype index.
     * @return the second sample or haplotype index.
     */
    public int id2() {
        return idPair.second();
    }

    /**
     * Returns the ordered pair of sample or haplotype indices.
     * @return the ordered pair of sample or haplotype indices.
     */
    public IntPair idPair() {
        return idPair;
    }


    /**
     * Returns the starting marker (inclusive).
     * @return the starting marker (inclusive).
     */
    @Override
    public int start() {
        return start;
    }

    /**
     * Returns the ending marker (inclusive).
     * @return the ending marker (inclusive).
     */
    @Override
    public int end() {
        return end;
    }

    /**
     * Returns the score for the IBD segment.
     * @return the score for the IBD segment.
     */
    public float score() {
        return score;
    }

    /**
     * Returns a string representation of <code>this</code>.  The exact
     * details of the representation are unspecified and subject to change.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(idPair.first());
        sb.append(Const.tab);
        sb.append(idPair.second());
        sb.append(Const.tab);
        sb.append(start);
        sb.append(Const.tab);
        sb.append(end);
        sb.append(Const.tab);
        sb.append(df2.format(score));
        return sb.toString();
    }
}
