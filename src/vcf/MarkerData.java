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
package vcf;

import blbutil.Const;
import java.util.Arrays;
import main.Marker;

/* Code Review on 03 Oct 2013 */

/**
 * Class <code>MarkerData</code> represents genotype data (GT FORMAT field)
 * for one VCF file record together with missing and allele count summary
 * statistics.  Class <code>MarkerData</code> is immutable.
 */
public final class MarkerData {

    public static final int N_DOSES = 3;

    private final VcfMarker data;
    private final byte majorAllele;
    private final byte minorAllele;
    private final int missingAlleleCnt;
    private final int missingGenotypeCnt;
    private final int[] alleleCnts;
    private final int[][] doseCnts;

    /**
     * Constructs a <code>MarkerData</code> instance corresponding to the
     * specified genotype data.
     * @param data the genotype data.
     * @throws NullPointerException if <code>data==null</code>.
     */
    public MarkerData(VcfMarker data) {
        int nAlleles = data.marker().nAlleles();
        this.alleleCnts = new int[nAlleles];
        this.doseCnts = new int[N_DOSES][];
        for (int j=1; j<doseCnts.length; ++j) {
            this.doseCnts[j] = new int[nAlleles];
        }
        int nMissingAlleles = 0;
        int nMissingGenotypes = 0;
        for (int j=0, n=data.nSamples(); j<n; ++j) {
            byte a1 = data.allele1(j);
            byte a2 = data.allele2(j);
            if (a1<0 || a2 < 0) {
                ++nMissingGenotypes;
            }
            else {
                if (a1==a2) {
                    ++doseCnts[2][a1];
                }
                else {
                    ++doseCnts[1][a1];
                    ++doseCnts[1][a2];
                }
            }
            if (a1<0) {
                ++nMissingAlleles;
            }
            else {
                ++alleleCnts[a1];

            }
            if (a2 < 0) {
                ++nMissingAlleles;
            }
            else {
                ++alleleCnts[a2];
            }
        }
        this.data = data;
        this.majorAllele = largestIndex(alleleCnts);
        this.minorAllele = largestIndex(alleleCnts, majorAllele);
        this.missingAlleleCnt = nMissingAlleles;
        this.missingGenotypeCnt = nMissingGenotypes;
    }

    /*
     * Returns the index of a maximal entry.
     */
    private static byte largestIndex(int[] counts) {
        byte largestIndex = 0;
        for (byte j=1; j<counts.length; ++j) {
            if (counts[j] > counts[largestIndex]) {
                largestIndex = j;
            }
        }
        return largestIndex;
    }

    /*
     * Returns the index of the maximal entry  after excluding
     * the entry with index <code>skipIndex</code>.
     */
    private static byte largestIndex(int[] counts, byte skipIndex) {
        if (counts.length==1) {
            return -1;
        }
        byte largestIndex = skipIndex>0 ? (byte) 0 : (byte) 1;
        for (byte j=0; j<counts.length; ++j) {
            if (j!=skipIndex && counts[j]>counts[largestIndex]) {
                largestIndex = j;
            }
        }
        return largestIndex;
    }

    /**
     * returns the number of represented by <code>this</code>.
     * @return the number of samples represented by <code>this</code>.
     */
    public int nSamples() {
        return data.nSamples();
    }

    /**
     * Returns the number of alleles.
     * @return the number of alleles.
     */
    public int nAlleles() {
        return alleleCnts.length;
    }

    /**
     * Returns the genotype data.
     * @return the genotype data.
     */
    public VcfMarker data() {
        return data;
    }

    /**
     * Returns the marker.
     * @return the marker.
     */
    public Marker marker() {
        return data.marker();
    }

    /**
     * Returns an allele which has the largest allele count.  If two or more
     * distinct alleles have maximal allele count, it is guaranteed that
     * <code>this.majorAllele()!=this.minorAllele()</code>.
     * @return the allele with the largest allele count.
     */
    public byte majorAllele() {
        return majorAllele;
    }

    /**
     * Returns an allele with the largest allele count after excluding
     * <code>this.majorAllele()</code>.
     * Returns -1 if <code>this.nAlleles()==1</code>.
     * @return  an allele with the largest allele count after excluding
     * <code>this.majorAllele()</code>.
     */
    public byte minorAllele() {
        return minorAllele;
    }

    /**
     * Returns the allele count of the specified allele.
     * @param allele an allele index.
     * @return the allele count for the specified allele.
     * @throws IndexOutOfBoundsException if
     * <code>allele < 0 || allele &ge; this.nAlleles()</code>.
     */
    public int alleleCount(int allele) {
        return alleleCnts[allele];
    }

    /**
     * Returns the frequency of the specified allele.
     * @param allele an allele index.
     * @return the frequency of the specified allele.
     * @throws IndexOutOfBoundsException if
     * <code>allele<0 || allele &ge; this.nAlleles()</code>.
     */
    public float alleleFrequency(int allele) {
        float nNonMissing = (2.0f * data.nSamples()) - missingAlleleCnt;
        return alleleCount(allele) / nNonMissing;
    }

    /**
     * Returns the count of the genotypes with the specified allele dose.
     * @param allele an allele index.
     * @param dose an allele dose.
     * @return the count of the genotypes with the specified allele dose.
     * @throws IndexOutOfBoundsException if
     * <code>allele<0 || allele &ge; this.nAlleles()</code> or if
     * <code>dose<0 || dose &ge; MarkerData.N_DOSES</code>
     */
    public int genotypeCount(int allele, int dose) {
        if (dose==0) {
            return data.nSamples() - doseCnts[1][allele] - doseCnts[2][allele];
        }
        else if (dose==1 || dose==2) {
            return doseCnts[dose][allele];
        }
        else {
            throw new IndexOutOfBoundsException("dose: " + dose);
        }
    }

    /**
     * Returns the frequency of the genotypes with the specified allele
     * dose.
     * @param allele an allele index.
     * @param dose an allele dose.
     * @return the frequency of the genotypes with the specified allele
     * dose.
     * @throws IndexOutOfBoundsException if
     * <code>allele<0 || allele &ge; this.nAlleles()</code>, or if
     * <code>dose<0 || dose &ge; MarkerData.N_DOSES</code>
     */

    public float genotypeFrequency(int allele, int dose) {
        float nNonMissing = (float) (data.nSamples() - missingGenotypeCnt);
        if (nNonMissing==0) {
            return 0.0f;
        }
        else {
            return genotypeCount(allele, dose) / nNonMissing;
        }
    }

    /**
     * Returns the number of missing alleles.
     * @return the number of missing alleles.
     */
    public int missingAlleleCount() {
        return missingAlleleCnt;
    }

    /**
     * Returns the missing alleles frequency.
     * @return the missing allele frequency.
     */
    public float missingAlleleFrequency() {
        return missingAlleleCnt / (2.0f * data.nSamples());
    }

    /**
     * Returns the number of missing genotypes.  A genotype is considered
     * missing if at least one allele is missing.
     * @return the number of missing genotypes.
     */
    public int missingGenotypeCount() {
        return missingGenotypeCnt;
    }

    /**
     * Returns the number of missing genotype frequency.  A genotype is considered
     * missing if at least one allele is missing.
     * @return the number of missing genotype frequency.
     */
    public float missingGenotypeFrequency() {
        return missingGenotypeCnt / (float) data.nSamples();
    }


    /**
     * Returns a string representation of <code>this</code>.  The exact
     * details of the representation are unspecified and subject to change.
     *
     * @return a string representation of <code>this</code>.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append("majorAllele=");
        sb.append(majorAllele());
        sb.append(" minorAllele=");
        sb.append(minorAllele());
        sb.append(" missingAlleleCount=");
        sb.append(missingAlleleCnt);
        sb.append(" alleleCnts=");
        sb.append(Arrays.toString(alleleCnts));
        for (int allele=0; allele<alleleCnts.length; ++allele) {
            sb.append(Const.nl);
            sb.append(allele);
            for (int dose=0; dose<N_DOSES; ++dose) {
                sb.append(Const.tab);
                sb.append(genotypeCount(allele, dose));
            }
        }
        sb.append(Const.nl);
        sb.append("data=");
        sb.append(data);
        return sb.toString();
    }

    /**
     * Returns the sample squared dosage correlation for the specified alleles.
     * @param mA the first marker.
     * @param mB the second marker.
     * @param alleleA the allele used to determine per-sample dosage at
     * the first marker.
     * @param alleleB the allele used to determine per-sample dosage at
     * the first marker.
     * @return the squared dosage correlation for the specified alleles.
     * @param mA the first marker.
     *
     * @throws IllegalArgumentException if
     * <code>mA.data().samples() != mB.data().samples()</code>.
     * @throws IndexOutOfBoundsException if
     * <code>alleleA < 0 || allelelB < 0 || alleleA &ge; mA.nAlleles()
     * || alleleB &ge; mB.nAlleles()</code>.
     * @throws NullPointerException if <code>mA==null || mB==null</code>.
     */
    public static double r2(MarkerData mA, MarkerData mB, byte alleleA, byte alleleB) {
        if (mA.data().samples() != mB.data().samples()) {
            throw new IllegalArgumentException("inconsistent samples");
        }
        int cnt = 0;
        int cntA = 0;
        int cntB = 0;
        int cntAA = 0;
        int cntAB = 0;
        int cntBB = 0;
        for (int j=0, n=mA.nSamples(); j<n; ++j) {
            int doseA = mA.dose(j, alleleA);
            int doseB = mB.dose(j, alleleB);
            if (doseA >= 0 && doseB >= 0) {
                ++cnt;
                cntA += doseA;
                cntB += doseB;
                cntAB += (doseA*doseB);
                cntAA += (doseA*doseA);
                cntBB += (doseB*doseB);
            }
        }
        double n = cnt;
        double meanA = cntA/n;
        double meanB = cntB/n;
        double cov = (cntAB/n - meanA*meanB);
        double num = cov*cov;
        double den = (cntAA/n - meanA*meanA) * (cntBB/n - meanB*meanB);
        return (num/den);
    }

    /**
     * Returns the sample dosage covariance for the specified alleles.
     *
     * @param mA the first marker.
     * @param mB the second marker.
     * @param alleleA the allele used to determine per-sample dosage at
     * the first marker.
     * @param alleleB the allele used to determine per-sample dosage at
     * the first marker.
     *
     * @return the dosage covariance for the specified alleles.
     *
     * @throws IllegalArgumentException if
     * <code>mA.data().samples() != mB.data().samples()</code>.
     * @throws IndexOutOfBoundsException if
     * <code>alleleA < 0 || allelelB < 0 || alleleA &ge; mA.nAlleles()
     * || alleleB &ge; mB.nAlleles()</code>.
     * @throws NullPointerException if <code>mA==null || mB==null</code>.
    */
    public static double cov(MarkerData mA, MarkerData mB, byte alleleA,
            byte alleleB) {
        if (mA.data().samples() != mB.data().samples()) {
            throw new IllegalArgumentException("inconsistent samples");
        }
        int cnt = 0;
        int cntA = 0;
        int cntB = 0;
        int cntAB = 0;
        for (int j=0, n=mA.nSamples(); j<n; ++j) {
            int doseA = mA.dose(j, alleleA);
            int doseB = mB.dose(j, alleleB);
            if (doseA >=0 && doseB >= 0) {
                ++cnt;
                cntA += doseA;
                cntB += doseB;
                cntAB += (doseA*doseB);
            }
        }
        double meanA = cntA/ ( (double) cnt );
        double meanB = cntB/ ( (double) cnt );
        double cov = (cntAB/ (double) cnt) - meanA*meanB;
        return cov;
    }

    /**
     * Returns the number of copies of the specified allele for the specified
     * sample, and returns -1 if either allele is missing.
     *
     * @param sample the sample index.
     * @param allele the dose allele
     *
     * @return the number of copies of the specified allele for the specified
     * sample, and returns -1 if either allele is missing.
     *
     * @throws IndexOutOfBoundsException if
     * <code>sample<0 || sample &ge; this.nSamples()</code>.
     */
    public int dose(int sample, byte allele) {
        int dose = 0;
        byte a1 = data.allele1(sample);
        byte a2 = data.allele2(sample);
        if (a1 < 0 || a2 <  0) {
            return -1;
        }
        else {
            if (a1==allele) {
                ++dose;
            }
            if (a2==allele) {
                ++dose;
            }
            return dose;
        }
    }
}
