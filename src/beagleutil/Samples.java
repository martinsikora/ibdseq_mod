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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/* Code Review on 01 Feb 2013 */

/**
 * Class <code>Samples</code> is an immutable class that represents
 * an ordered list of samples.
 */
public final class Samples {

    private final int[] indexToIdIndex;
    private final int[] idIndexToIndex;

    /**
     * Constructs a new instance of </code>Samples</code>.
     * @param indices the indices of the sample identifiers to include
     * in <code>this</code>.
     *
     * @throws IllegalArgumentException if the specified array
     * hasIndex two or more entries that are equal.
     * @throws IndexOutOfBoundsException if any element of the specified
     * array is negative or greater than or equal to
     * <code>SampleIds.size()</code>.
     * @throws NullPointerException if <code>indices==null</code>.
     */
    public Samples(int[] indices) {
        int sampleSize = SampleIds.size();
        int[] copy = indices.clone();
        Arrays.sort(copy);
        int maxIdIndex = 0;
        if (copy.length > 0) {
            maxIdIndex = copy[copy.length-1];
            if (copy[0] < 0) {
                throw new IndexOutOfBoundsException(String.valueOf(copy[0]));
            }
            for (int j=1 ; j<copy.length; ++j) {
                if (copy[j-1]==copy[j]) {
                    String s = "duplicate index: " + copy[j];
                    throw new IllegalArgumentException(s);
                }
            }
            if (maxIdIndex >= sampleSize) {
                throw new IndexOutOfBoundsException(String.valueOf(maxIdIndex));
            }
        }
        this.indexToIdIndex = indices.clone();
        this.idIndexToIndex = new int[maxIdIndex+1];
        Arrays.fill(idIndexToIndex, -1);
        for (int j=0; j<indices.length; ++j) {
            this.idIndexToIndex[indices[j]] = j;
        }
    }

    /**
     * Constructs and returns an instance of </code>Samples</code>
     * corresponding to the specified array of sample identifiers.
     * @param ids the sample identifiers to include in <code>this</code>.
     *
     * @throws IllegalArgumentException if the specified array
     * hasIndex two or more identifiers that are equal.
     * @throws NullPointerException if <code>ids==null</code>.
     */
    public static Samples fromIds(String[] ids) {
        Set<String> idSet = new HashSet<String>(ids.length);
        int[] indices = new int[ids.length];
        for (int j=0; j<ids.length; ++j) {
            if (idSet.add(ids[j])==false) {
                throw new IllegalArgumentException("duplicate sample ID: "
                        + ids[j]);
            }
            indices[j] = SampleIds.indexOf(ids[j]);
        }
        return new Samples(indices);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.indexToIdIndex);
    }

    /**
     * Returns <code>true</code> if the specified object is a
     * <code>Samples</code> object which represents the same ordered
     * list of samples as <code>this</code>, and returns <code>false</code>
     * otherwise.
     * @param obj the object to be tested for equality with <code>this</code>.
     * @return <code>true</code> if the specified object is a
     * <code>Samples</code> object which represents the same ordered
     * list of samples as <code>this</code>, and returns <code>false</code>
     * otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this==obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final Samples other = (Samples) obj;
        return Arrays.equals(this.indexToIdIndex, other.indexToIdIndex);
    }

    /**
     * Returns the global sample identifier index for the specified element of
     * <code>this</code>.
     * @param index a sample index in <code>this</code>.
     * @return the global sample identifier index for the specified element of
     * <code>this</code>.
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.size()</code>.
     */
    public int idIndex(int index) {
        return indexToIdIndex[index];
    }

    /**
     * Returns the index of the specified sample identifier in
     * <code>this</code>, or returns <code>-1</code> if the specified
     * sample identifier is not in <code>this</code>.
     * @param idIndex a sample identifier index.
     * @return the index of the specified sample identifier in
     * <code>this</code>, or returns <code>-1</code> if the specified
     * sample identifier is not in <code>this</code>.
     * @throws IndexOutOfBoundsException if <code>index < 0</code>.
     */
    public int index(int idIndex) {
        if (idIndex >= idIndexToIndex.length) {
            return -1;
        }
        return idIndexToIndex[idIndex];
    }

    /**
     * Returns the number of samples in <code>this</code>.
     * @return the number of samples in <code>this</code>.
     */
    public int nSamples() {
        return indexToIdIndex.length;
    }

    /**
     * Returns the sample identifier for the specified element of
     * <code>this</code>.
     * @param index an index of an element in <code>this</code>.
     * @return the sample identifier for the specified element of
     * <code>this</code>.
     * @throws IndexOutOfBoundsException if
     * <code>indexOf < 0 || indexOf &ge; this.size()</code>.
     */
    public String id(int index) {
        return SampleIds.id(indexToIdIndex[index]);
    }

    /**
     * Returns an array of length <code>this.size()</code> whose elements
     * satisfy <code>this.ids()[index].equals(this.id(index)</code> for
     * <code>0 &le indexOf < this.size()</code>.
     * @return an array of length <code>this.size()</code> whose elements
     * satisfy <code>this.ids()[index].equals(this.id(index)</code> for
     * <code>0 &le indexOf < this.size()</code>.
     */
    public String[] ids() {
        String[] ids = new String[indexToIdIndex.length];
        for (int j=0; j<ids.length; ++j) {
            ids[j] = SampleIds.id(indexToIdIndex[j]);
        }
        return ids;
    }

    /**
     * Returns a string representation of <code>this</code>.
     * The exact details of the representation are unspecified and
     * subject to change.
     * @return a string representation of <code>this</code>.
     */
    @Override
    public String toString() {
        return Arrays.toString(indexToIdIndex);
    }
}
