/*
 * Copyright 2009-2012 Brian L. Browning
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

/* Code review on 10 Mar 2013 */

/**
 * Class <code>IntPair</code> represents an immutable ordered pair of integers.
 */
public final class IntPair implements Comparable<IntPair> {

    private final int first;
    private final int second;

    /**
     * Constructs an <code>IntPair</code> instance.
     * @param first the first element of the ordered pair of integers.
     * @param second the second element of the ordered pair of integers.
     */
    public IntPair(int first, int second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Returns the first integer in the ordered pair of integers represented
     * by <code>this</code>.
     * @return the first integer in the ordered pair of integers represented
     * by <code>this</code>.
     */
    public int first() {
        return first;
    }

    /**
     * Returns the second integer in the ordered pair of integers represented
     * by <code>this</code>.
     * @return the second integer in the ordered pair of integers represented
     * by <code>this</code>.
     */
    public int second() {
        return second;
    }

    /**
     * Compares the specified object with this <code>IntPair</code> for
     * equality.  Returns >code>true</code> if and only if the specified object
     * is also a <code>IntPair</code>, and if both <code>IntPair</code>
     * instances represent the same ordered pair of integers.
     * @param obj the object to be compared for equality with this
     * <code>IntPair</code>.
     * @return <code>true</code> if the specified object is equal to
     * <code>this</code> and <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof IntPair)) {
            return false;
        }
        IntPair other = (IntPair) obj;
        return (this.first==other.first) && (this.second==other.second);
    }

     /**
     * Returns the hash code value for this <code>IntPair</code>.
     * @return the hash code value for this <code>IntPair</code>.
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + this.first;
        hash = 29 * hash + this.second;
        return hash;
    }

    /**
     * Returns a string representation of <code>this</code>.  The string
     * representation is <code>"[i1, i2]"</code> where
     * <code>i1, i2</code> are the first and second integers
     * in the ordered integer pair represented by <code>this</code>.
     * @return a string representation of <code>this</code>.
     */
    @Override
    public String toString() {
        return "[" + first + ", " + second + "]";
    }

    /**
     * Returns -1, 0, or 1 depending on whether <code>this</code> is
     * less than, equal, or greater than the specified <code>IntPair</code>
     * object with respect to lexicographical order.
     * @param other an element to be compared to <code>this</code>.
     * @return -1, 0, or 1 depending on whether <code>this</code> is
     * less than, equal, or greater than the specified <code>IntPair</code>
     * object with respect to lexicographical order.
     */
    @Override
    public int compareTo(IntPair other) {
        if (this.first < other.first) {
            return -1;
        }
        if (this.first > other.first) {
            return 1;
        }
        if (this.second < other.second) {
            return -1;
        }
        if (this.second > other.second) {
            return 1;
        }
        return 0;
    }
}
