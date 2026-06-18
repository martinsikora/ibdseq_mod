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

import beagleutil.SampleIds;
import beagleutil.Samples;
import blbutil.Const;
import main.Marker;
import main.NuclearFamilies;

/* Code Review on 03 Dec 2013 */

/**
 * Class <code>LowMemGT</code> represents per-samples genotype for a marker.
 * The <code>LowMemGt</code> class uses slightly less memory than the
 * <code>MedMemGt</code> class, but provides a slower implementation of the
 * <code>gl(int, int)</code> method.
 */
public final class LowMemGT implements VcfMarker {

    public static final String GT_FORMAT = "GT";

    private final byte bitsPerAllele;
    private final Samples samples;
    private final Marker marker;
    private final boolean isRefData;

    private final boolean[] allele1;
    private final boolean[] allele2;
    private final boolean[] isMissing1;
    private final boolean[] isMissing2;
    private final boolean[] isPhased;

    /**
     * Constructs a <code>ObsDataInterface</code> instance corresponding
     * to the specified VCF file record "GT" format field.
     *
     * @param record a VCF file record.
     * @param usePhase <code>true</code> if phase information in the specified
     * VCF file record will be used, and <code>false</code> if phase
     * information in the specified VCF file record will be ignored.
     *
     * @throws IllegalArgumentException if
     * <code>record.nSamples()==0</code>, if
     * the "GT" format field is undefined.
     * @throws NullPointerException if <code>vcfRecord==null</code>.
     */
    public LowMemGT(VcfRecord record, boolean usePhase) {
        this(record);
        setBits(record, usePhase, bitsPerAllele, allele1, allele2, isMissing1,
                isMissing2, isPhased);
    }

    /**
     * Constructs a <code>ObsDataInterface</code> instance corresponding
     * to the specified VCF file record "GT" format field.
     *
     * @param record a VCF file record.
     * @param fam the parent-offspring relationships.
     * @param usePhase <code>true</code> if phase information in the specified
     * VCF file record will be used, and <code>false</code> if phase
     * information in the specified VCF file record will be ignored.
     *
     * @throws IllegalArgumentException if
     * <code>vcfRecord.nSamples()==0</code>, if
     * <code>record.samples().equals(fam.samples())==false</code>, or if
     * the "GT" format field is undefined.
     * @throws NullPointerException if <code>vcfRecord==null || fam==null</code>.
     */
    public LowMemGT(VcfRecord record, NuclearFamilies fam, boolean usePhase) {
        this(record);
        if (record.samples().equals(fam.samples())==false) {
            throw new IllegalArgumentException("inconsistent samples");
        }
        setBits(record, usePhase, bitsPerAllele, allele1, allele2, isMissing1,
                isMissing2, isPhased);
        removeMendelianInconsistencies(record, fam, isPhased, isMissing1,
                isMissing2);
    }

    private LowMemGT(VcfRecord record) {
        int nSamples = record.nSamples();
        if (nSamples==0) {
            String s = "missing sample data: " + record;
            throw new IllegalArgumentException(s);
        }
        if (record.hasFormat(GT_FORMAT)==false) {
            String s = "missing GT FORMAT: " + record;
            throw new IllegalArgumentException(s);
        }
        this.bitsPerAllele = bitsPerAllele(record.marker());
        this.samples = record.vcfHeader().sample();
        this.marker = record.marker();
        this.isRefData = isRef(record);

        this.allele1 = new boolean[nSamples * bitsPerAllele];
        this.allele2 = new boolean[nSamples * bitsPerAllele];
        this.isMissing1 = new boolean[nSamples];
        this.isMissing2 = new boolean[nSamples];
        this.isPhased = new boolean[nSamples];
    }

    private static boolean isRef(VcfRecord rec) {
        for (int j=0, n=rec.nSamples(); j<n; ++j) {
            if (rec.isPhased(j)==false || rec.gt(j, 0)<0 || rec.gt(j,1)<0) {
                return false;
            }
        }
        return true;
    }

    private static void setBits(VcfRecord rec, boolean usePhase,
            int bitsPerAllele, boolean[] allele1, boolean[] allele2,
            boolean[] isMissing1, boolean[] isMissing2, boolean[] isPhased) {
        int index1 = 0;
        int index2 = 0;
        for (int j=0, n=rec.nSamples(); j<n; ++j) {
            if (usePhase && rec.isPhased(j)) {
                isPhased[j] = true;
            }
            byte a1 = rec.gt(j, 0);
            byte a2 = rec.gt(j, 1);
            if (a1 < 0) {
                isMissing1[j] = true;
                index1 += bitsPerAllele;
            }
            else {
                int mask = 1;
                for (int k=0; k<bitsPerAllele; ++k) {
                    if ((a1 & mask)==mask) {
                        allele1[index1] = true;
                    }
                    ++index1;
                    mask <<= 1;
                }
            }

            if (a2 < 0) {
                isMissing2[j] = true;
                index2 += bitsPerAllele;
            }
            else {
                int mask = 1;
                for (int k=0; k<bitsPerAllele; ++k) {
                    if ((a2 & mask)==mask) {
                        allele2[index2] = true;
                    }
                    ++index2;
                    mask <<= 1;
                }
            }
        }
    }

    private static byte bitsPerAllele(Marker marker) {
        int nAllelesM1 = marker.nAlleles() - 1;
        int nStorageBits = Integer.SIZE - Integer.numberOfLeadingZeros(nAllelesM1);
        return (byte) nStorageBits;
    }

    /*
     * Sets phase to unknown for all parent-offspring relationships, and sets
     * all genotypes in a duo or trio genotypes to missing if a Mendelian
     * inconsistency is found.
     */
    private static void removeMendelianInconsistencies(VcfRecord rec,
            NuclearFamilies fam, boolean[] isPhased,
            boolean[] isMissing1, boolean[] isMissing2) {
        for (int j=0, n=fam.nDuos(); j<n; ++j) {
            int p = fam.duoParent(j);
            int o = fam.duoOffspring(j);
            isPhased[p] = false;
            isPhased[o] = false;
            if (duoIsConsistent(rec, p, o) == false) {
                logDuoInconsistency(rec, p, o);
                isMissing1[p] = true;
                isMissing2[p] = true;
                isMissing1[o] = true;
                isMissing2[o] = true;
            }
        }
        for (int j=0, n=fam.nTrios(); j<n; ++j) {
            int f = fam.trioFather(j);
            int m = fam.trioMother(j);
            int o = fam.trioOffspring(j);
            isPhased[f] = false;
            isPhased[m] = false;
            isPhased[o] = false;
            if (trioIsConsistent(rec, f, m, o) == false) {
                logTrioInconsistency(rec, f, m, o);
                isMissing1[f] = true;
                isMissing2[f] = true;
                isMissing1[m] = true;
                isMissing2[m] = true;
                isMissing1[o] = true;
                isMissing2[o] = true;
            }
        }
    }

    private static boolean duoIsConsistent(VcfRecord rec, int parent,
            int offspring) {
        byte p1 = rec.gt(parent, 0);
        byte p2 = rec.gt(parent, 1);
        byte o1 = rec.gt(offspring, 0);
        byte o2 = rec.gt(offspring, 1);
        boolean alleleMissing = (p1<0 || p2<0 || o1<0 || o2<0);
        return (alleleMissing || p1==o1 || p1==o2 || p2==o1 || p2==o2);
    }

    private static boolean trioIsConsistent(VcfRecord rec, int father,
            int mother, int offspring) {
        byte f1 = rec.gt(father, 0);
        byte f2 = rec.gt(father, 1);
        byte m1 = rec.gt(mother, 0);
        byte m2 = rec.gt(mother, 1);
        byte o1 = rec.gt(offspring, 0);
        byte o2 = rec.gt(offspring, 1);
        boolean fo1 = (o1<0 || f1<0 || f2<0 || o1==f1 || o1==f2);
        boolean mo2 = (o2<0 || m1<0 || m2<0 || o2==m1 || o2==m2);
        if (fo1 && mo2) {
            return true;
        }
        else {
            boolean fo2 = (o2<0 || f1<0 || f2<0 || o2==f1 || o2==f2);
            boolean mo1 = (o1<0 || m1<0 || m2<0 || o1==m1 || o1==m2);
            return (fo2 && mo1);
        }
    }

    private static void logDuoInconsistency(VcfRecord rec, int parent,
            int offspring) {
        StringBuilder sb = new StringBuilder(80);
        sb.append("WARNING: Inconsistent duo genotype set to missing");
        sb.append(Const.tab);
        sb.append(rec.marker());
        sb.append(Const.colon);
        sb.append(SampleIds.id(parent));
        sb.append(Const.tab);
        sb.append(SampleIds.id(offspring));
        sb.append(Const.nl);
        main.Logger.getInstance().print(sb.toString());
    }

    private static void logTrioInconsistency(VcfRecord rec, int father,
            int mother, int offspring) {
        StringBuilder sb = new StringBuilder(80);
        sb.append("WARNING: Inconsistent trio genotype set to missing");
        sb.append(Const.tab);
        sb.append(rec.marker());
        sb.append(Const.tab);
        sb.append(SampleIds.id(father));
        sb.append(Const.tab);
        sb.append(SampleIds.id(mother));
        sb.append(Const.tab);
        sb.append(SampleIds.id(offspring));
        sb.append(Const.nl);
        main.Logger.getInstance().print(sb.toString());
    }

    @Override
    public Samples samples() {
        return samples;
    }

    @Override
    public int nSamples() {
        return samples.nSamples();
    }

    @Override
    public Marker marker() {
        return marker;
    }

    @Override
    public boolean isRefData() {
        return isRefData;
    }

    @Override
    public boolean isPhased(int sample) {
        return isPhased[sample];
    }

    @Override
    public byte allele1(int sample) {
        return isMissing1[sample] ? -1 : allele(allele1, sample);
    }

    @Override
    public byte allele2(int sample) {
        return isMissing2[sample] ? -1 : allele(allele2, sample);
    }

    @Override
    public float gl(int sample, byte a1, byte a2) {
        if ( a1 < 0 || a1 >= marker.nAlleles())  {
            String s = "invalid alleles: (" + a1 + "): " + marker;
            throw new IllegalArgumentException(s);
        }
        if ( a2 < 0 || a2 >= marker.nAlleles()) {
            String s = "invalid alleles: (" + a2 + "): " + marker;
            throw new IllegalArgumentException(s);
        }
        if (isMissing1[sample] && isMissing2[sample]) {
            return 1.0f;
        }
        else if (isMissing1[sample] ^ isMissing2[sample]) {
            byte obsA1 = allele(allele1, sample);
            byte obsA2 = allele(allele2, sample);
            boolean consistent = (obsA1<0 || obsA1==a1) && (obsA2<0 || obsA2==a2);
            if (isPhased[sample]==false && consistent==false) {
                consistent = (obsA1<0 || obsA1==a2) && (obsA2<0 || obsA2==a1);
            }
            return consistent ? 1.0f : 0.0f;
        }
        else {
            byte obsA1 = allele(allele1, sample);
            byte obsA2 = allele(allele2, sample);
            if (isPhased[sample]) {
                return (obsA1==a1 && obsA2==a2) ? 1.0f : 0.0f;
            }
            else {
                boolean isConsistent = (obsA1==a1 && obsA2==a2)
                        || (obsA1==a2 && obsA2==a1);
                return isConsistent ? 1.0f : 0.0f;
            }
        }
    }

    private byte allele(boolean[] bits, int sample) {
        int start = bitsPerAllele*sample;
        int end = start + bitsPerAllele;
        byte allele = 0;
        byte mask = 1;
        for (int j=start; j<end; ++j) {
            if (bits[j]) {
                allele += mask;
            }
            mask <<= 1;
        }
        return allele;
    }

    /**
     * Returns the data represented by <code>this</code> as a VCF file
     * record with a GT format field.
     * @return the data represented by <code>this</code> as a VCF file
     * record with a GT format field.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(marker);
        sb.append(Const.tab);
        sb.append(Const.MISSING_DATA);
        sb.append(Const.tab);
        sb.append("PASS");
        sb.append(Const.tab);
        sb.append(Const.MISSING_DATA);
        sb.append(Const.tab);
        sb.append("GT");
        for (int j=0, n=samples.nSamples(); j<n; ++j) {
            sb.append(Const.tab);
            sb.append(isMissing1[j] ? Const.MISSING_DATA : allele(allele1, j));
            sb.append(isPhased[j] ? Const.phasedSep : Const.unphasedSep);
            sb.append(isMissing2[j] ? Const.MISSING_DATA : allele(allele2, j));
        }
        return sb.toString();
    }
}
