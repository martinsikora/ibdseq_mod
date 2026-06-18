/*
 * Copyright 2012-2013 Brian L. Browning
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.t
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

import beagleutil.Samples;
import main.Marker;

/* Code Review on 21 Nov 2013 */

/**
 * Interface <code>VcfMarker</code> represents per-samples
 * genotype data for a single marker.
 */
public interface VcfMarker {

    /**
     * Returns <code>this.samples().nSamples()</code>.
     * @return <code>this.samples().nSamples()</code>.
     */
    int nSamples();

    /**
     * Returns the samples with data represented by <code>this</code>.
     * @return the samples with data represented by <code>this</code>.
     */
    Samples samples();

    /**
     * Returns the <code>Marker</code> corresponding to <code>this</code>.
     * @return the <code>Marker</code> corresponding to <code>this</code>.
     */
    Marker marker();

    /**
     * Returns <code>true</code> if the data for each sample is a phased
     * genotype with no missing alleles, and returns <code>false</code>
     * otherwise.
     * @return <code>true</code> if the data for each sample is a phased
     * genotype with no missing alleles, and returns <code>false</code>
     * otherwise.
     */
    boolean isRefData();

    /**
     * Returns the likelihood of the specified ordered genotype for the
     * specified sample.  If <code>this.isPhased(sample)==false</code>,
     * then <code>1.0f</code> will be returned if the unordered specified
     * alleles equal the observed unordered alleles, and <code>0.0f</code>
     * will be returned otherwise.  If <code>this.isPhased(sample)==true</code>,
     * then <code>1.0f</code> will be returned if the specified ordered alleles
     * equal the observed ordered alleles, and <code>0.0f</code> will be
     * returned otherwise.
     * @param samples the sample index.
     * @param allele1 the first allele index.
     * @param allele2 the second allele index.
     * @return the likelihood of the specified ordered genotype for the
     * specified sample.
     *
     * @throws IndexOutOfBoundsException if
     * <code>sample < 0 || sample &ge; this.nSamples()</code>, or if
     * <code>allele1 < 0 || allele1 &ge; this.nAlleles()</code>, or if
     * <code>allele2 < 0 || allele2 &ge; this.nAlleles()</code>.
     */
    float gl(int sample, byte allele1, byte allele2);

    /**
     * Returns <code>true</code> if the observed data for the specified
     * sample is a phased genotype, and returns <code>false</code> otherwise.
     * @param sample the sample index.
     * @return <code>true</code> if the observed data for the specified
     * sample is a phased genotype, and returns <code>false</code> otherwise.
     *
     * @throws IndexOutOfBoundsException if
     * <code>sample < 0 || sample &ge; this.nSamples()</code>.
     */
    boolean isPhased(int sample);

    /**
     * Returns the first allele if the observed data for the sample is a
     * called genotype, and return -1 otherwise.
     * @param sample the sample index.
     * @return the first allele if the observed data for the sample is a
     * called genotype, and return -1 otherwise.
     *
     * @throws IndexOutOfBoundsException if
     * <code>sample < 0 || samples &ge; this.nSamples()</code>.
     */
    byte allele1(int sample);

    /**
     * Returns the second allele if the observed data for the sample is a
     * genotype, and return -1 otherwise.
     * @param sample the sample index.
     * @return the second allele if the observed data for the sample is a
     * genotype, and return -1 otherwise.
     *
     * @throws IndexOutOfBoundsException if
     * <code>sample < 0 || samples &ge; this.nSamples()</code>.
     */
    byte allele2(int sample);

    /**
     * Returns a VCF record representing <code>this</code>
     * with missing QUAL and INFO fields, with "PASS"
     * in the filter field.
     * @return a VCF record representing <code>this</code>.
     */
    @Override
    String toString();
}
