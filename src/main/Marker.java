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
package main;

import beagleutil.ChromIds;
import blbutil.Const;
import blbutil.StringUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/* Code Review on 13 Sep 2013 */

/**
 * Class <code>Marker</code> represents a marker.
 */
public class Marker implements Comparable<Marker> {

    private static final String[] EMPTY_ID_ARRAY = new String[0];
    private static final Map<String, String[]> allelesMap
            = new HashMap<String, String[]>(24);

    private final int chromIndex;
    private final int pos;
    private final String[] ids;
    private final String uniqueId;
    private final String[] alleles;
    private final int nGenotypes;
    private final int end;

    /**
     * Constructs a <code>Marker</code> from the specified
     * VCF File record.
     * @param vcfRecord a VCF file record.
     * @throws IllegalArgumentException if the specified VCF file
     * record has fewer than 6 tab-delimited fields, or if the
     * first five tab-delimited fields have incorrect format.
     */
    @SuppressWarnings("RedundantStringConstructorCall")
    public Marker(String vcfRecord) {
        int nFields = 8;
        String[] fields = StringUtil.getFields(vcfRecord, Const.tab, nFields+1);
        if (fields.length < nFields) {
            String s = "VCF record does not contain at least " + nFields
                    + " tab-delimited fields: " + vcfRecord;
            throw new IllegalArgumentException(s);
        }
        // Make string backed by minimal required data, not entire VCF record
        for (int j=0; j<5; ++j) {
            fields[j] = new String(fields[j]);
        }
        checkCHROM(fields[0], vcfRecord);
        checkPOS(fields[1], vcfRecord);
        String[] markerIds = checkID(new String(fields[2]), vcfRecord);
        checkREF(fields[3], vcfRecord);
        String[] altAlleles = checkALT(new String(fields[4]), vcfRecord);

        this.chromIndex = ChromIds.indexOf(fields[0]);
        this.pos = Integer.parseInt(fields[1]);
        this.ids = markerIds;
        this.uniqueId = markerIds.length>0 ? markerIds[0] :
                (fields[0] + Const.colon + pos);
        this.alleles = alleles(fields[3], altAlleles);
        this.nGenotypes = alleles.length*(alleles.length+1)/2;
        this.end = extractEnd(fields[7]);
    }

    /**
     * Returns the marker obtained from the specified marker by changing
     * the marker's alleles to the alleles on the other chromosome strand.
     * @param marker a marker.
     * @return he marker obtained from the specified marker by changing
     * the marker's alleles to the alleles on the other chromosome strand.
     * @thorws NullPointerException if <code>marke==null</code>.
     */
    public static Marker flipStrand(Marker marker) {
        return new Marker(marker);
    }

    /* Private constructor used by flipStrand(Marker) method */
    private Marker(Marker markerOnReverseStrand) {
        Marker m = markerOnReverseStrand;
        this.chromIndex = m.chromIndex;
        this.pos = m.pos;
        this.ids = m.ids;
        this.uniqueId = m.uniqueId;
        this.alleles = m.alleles.clone();
        for (int j=0; j<m.alleles.length; ++j) {
            if (isSimpleAllele(alleles[j])) {
                this.alleles[j] = flipAllele(m.alleles[j]);
            }
        }
        this.nGenotypes = m.nGenotypes;
        this.end = m.end;
    }

    private static String flipAllele(String allele) {
        char[] ca = new char[allele.length()];
        for (int j=0; j<ca.length; ++j) {
            ca[j] = flipBase(allele.charAt(j));
        }
        return new String(ca);
    }

    private static char flipBase(char c) {
        switch (c) {
            case 'A' : return 'T';
            case 'C' : return 'G';
            case 'G' : return 'C';
            case 'T' : return 'A';
            case 'N' : return 'N';
            default: assert false; return 0;
        }
    }

    private static void checkCHROM(String chrom, String vcfRecord) {
        if (chrom.isEmpty() || chrom.equals(Const.MISSING_DATA)) {
            String s = "Missing chromosome identifier in VCF Record: "
                    + vcfRecord;
            throw new IllegalArgumentException(s);
        }
        for (int j=0, n=chrom.length(); j<n; ++j) {
            char c = chrom.charAt(j);
            if (c==Const.colon || Character.isWhitespace(c)) {
                String s = "invalid chromosome identifier character ('" + c
                        + "'): " + vcfRecord;
                throw new IllegalArgumentException(s);
            }
        }
    }

    private static void checkPOS(String pos, String vcfRecord) {
        for (int j=0, n=pos.length(); j<n; ++j) {
            if (Character.isDigit(pos.charAt(j))==false) {
                String s = "invalid position (" + pos + "): " + vcfRecord;
                throw new IllegalArgumentException(s);
            }
        }
    }

    private static String[] checkID(String id, String vcfRecord) {
        if (id.isEmpty()) {
            String s = "missing ID field: " + vcfRecord;
            throw new IllegalArgumentException(s);
        }
        if (id.equals(Const.MISSING_DATA)) {
            return EMPTY_ID_ARRAY;
        }
        String[] sa = StringUtil.getFields(id, Const.semicolon);
        for (String s : sa) {
            for (int j=0, n=s.length(); j<n; ++j) {
                char c = s.charAt(j);
                if (Character.isWhitespace(c)) {
                    String msg = "marker identifier (" + s
                            + ") contains white-space: " + vcfRecord;
                    throw new IllegalArgumentException(msg);
                }
            }
        }
        return sa;
    }

    private static void checkREF(String ref, String vcfRecord) {
        checkSimpleAllele(ref, vcfRecord);
    }

    private static String[] checkALT(String alt, String vcfRecord) {
        String[] altAlleles = EMPTY_ID_ARRAY;
        if (alt.equals(Const.MISSING_DATA)==false) {
            altAlleles = StringUtil.getFields(alt, Const.comma);
        }
        for (String s : altAlleles) {
            checkComplexAllele(s, vcfRecord);
        }
        if (altAlleles.length >= Byte.MAX_VALUE - 1) {
            String s = "Too many alternate alleles: " + vcfRecord;
            throw new IllegalArgumentException(s);
        }
        return altAlleles;
    }

    private static boolean isSimpleAllele(String allele) {
        for  (int j=0, n=allele.length(); j<n; ++j) {
            char c = Character.toUpperCase(allele.charAt(j));
            if ((c=='A' || c=='C' || c=='G' || c=='T' || c=='N' || c=='*')==false) {
                return false;
            }
        }
        return true;
    }

    private static void checkSimpleAllele(String allele, String vcfRecord) {
        int n = allele.length();
        if (n==0) {
            throw new IllegalArgumentException("missing allele");
        }
        if (isSimpleAllele(allele)==false) {
            String s = "invalid allele [" + allele + "].  Each allele"
                    + " in REF and ALT fields must be a sequence of one"
                    + " or more nucleotides (A, C, T, G)" + Const.nl + vcfRecord;
            throw new IllegalArgumentException(s);
        }
    }

    private static void checkComplexAllele(String allele, String vcfRecord) {
        int n = allele.length();
        if (n >= 2 && allele.charAt(0)=='<' && allele.charAt(n-1)=='>') {
            for (int j=1; j<n-1; ++j) {
                char c = allele.charAt(j);
                if (Character.isWhitespace(c) || c==Const.comma || c=='<'
                        || c=='>') {
                    String s = "invalid allele (" + allele + "): " + vcfRecord;
                    throw new IllegalArgumentException(s);
                }
            }
        }
        else {
            checkSimpleAllele(allele, vcfRecord);
        }
    }

    private static String[] alleles(String ref, String[] altAlleles) {
        if (isSNV(ref, altAlleles)) {
            String key = ref;
            for (String a : altAlleles) {
                key += a;
            }
            String[] alleles = allelesMap.get(key);
            if (alleles==null) {
                alleles = createAllelesArray(ref, altAlleles);
                allelesMap.put(key, alleles);
            }
            return alleles;
        }
        else {
            return createAllelesArray(ref, altAlleles);
        }
    }

    private static boolean isSNV(String ref, String[] altAlleles) {
        if (ref.length()!=1) {
            return false;
        }
        for (String a : altAlleles) {
            if (a.length()!=1) {
                return false;
            }
        }
        return true;
    }


    private static String[] createAllelesArray(String ref, String[] altAlleles) {
        String[] alleles = new String[altAlleles.length + 1];
        alleles[0] = ref;
        System.arraycopy(altAlleles, 0, alleles, 1, altAlleles.length);
        return alleles;
    }

    /*
     * Returns value of first END key in the specified INFO field, or
     * returns -1 if there is no END key in INFO field.
     */
    private static int extractEnd(String info) {
        String[] fields = StringUtil.getFields(info, Const.semicolon);
        String key = "END=";
        for (String field : fields) {
            if (field.startsWith(key)) {
                String value = field.substring(4);
                for (int j=0, n=value.length(); j<n; ++j) {
                    char c = value.charAt(j);
                    if (Character.isDigit(c)==false) {
                        String s = "INFO END field has non-numeric value: "
                                + info;
                        throw new IllegalArgumentException(s);
                    }
                }
                return Integer.parseInt(value);
            }
        }
        return -1;
    }

    /**
     * Returns the marker chromosome.
     * @return the marker chromosome.
     */
    public String chrom() {
        return ChromIds.id(chromIndex);
    }

    /**
     * Returns the chromosome index.
     * @return the chromosome index.
     */
    public int chromIndex() {
        return chromIndex;
    }

    /**
     * Returns the marker position.
     * @return the marker position.
     */
    public int pos() {
        return pos;
    }

    /**
     * Returns the number of marker identifiers.
     * @return the number of marker identifiers.
     */
    public int nIds() {
        return ids.length;
    }

    /**
     * Returns the specified marker identifier.
     * @param index an index of a marker identifier.
     * @return the specified marker identifier.
     *
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.nIds()</code>.
     */
    public String id(int index) {
        return ids[index];
    }

    /**
     * Returns the unique marker identifier.  The unique marker identifiers
     * is the first marker identifier if one or more marker identifiers are
     * given and is <code>"this.chr() + ":" + this.pos()"</code> if no
     * marker identifers are given.
     *
     * @returns the unique marker identifier.
     */
    public String uniqueId() {
        return uniqueId;
    }

    /**
     * Returns the number of alleles for the marker.  The number of
     * alleles includes the reference allele and all alternate nAlleles.
     * @return the number of alleles for the marker.
     */
    public int nAlleles() {
        return alleles.length;
    }

    /**
     * Returns the number of distinct genotypes:
     * <code>this.nAlleles()*(1 + this.nAlleles())/2</code>.
     *
     * @return the number of distinct genotypes:
     * <code>this.nAlleles()*(1 + this.nAlleles())/2</code>.
     */
    public int nGenotypes() {
        return nGenotypes;
    }

    /**
     * Returns the specified allele.  The reference allele has index 0.
     * @param index an allele index.
     * @return the specified allele.
     *
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.Alleles()</code>.
     */
    public String allele(int index) {
        return alleles[index];
    }

    /**
     * Returns the END INFO field, or -1 if there is no END INFO field.
     *
     * @returns the END INFO field, or -1 if there is no END INFO field.
     */
    public int end() {
        return end;
    }

    /**
     * Returns a string equal to the first five fields the VCF
     * record represented by <code>this</code>.
     *
     * @return a string equal to the first five fields the VCF
     * record represented by <code>this</code>.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(50);
        sb.append(ChromIds.id(chromIndex));
        sb.append(Const.tab);
        sb.append(pos);
        if (ids.length==0) {
            sb.append(Const.tab);
            sb.append(Const.period);
        }
        else {
            for (int j=0; j<ids.length; ++j) {
                sb.append(j==0 ? Const.tab : Const.semicolon);
                sb.append(ids[j]);
            }
        }
        if (alleles.length==1) {
            sb.append(Const.tab);
            sb.append(alleles[0]);
            sb.append(Const.tab);
            sb.append(Const.MISSING_DATA);
        }
        else {
            for (int j=0; j<alleles.length; ++j) {
                sb.append(j<2 ? Const.tab : Const.comma);
                sb.append(alleles[j]);
            }
        }
        return sb.toString();
    }

    /**
     * Returns the hash code value for this marker.  The hash code is defined
     * by the following calculation:
     * </p>
     *
     *   int hash = 5;
     *   hash = 29 * hash + this.chromIndex();
     *   hash = 29 * hash + this.pos();
     *   for (int j=0, n=this.nAlleles(); j<n; ++j) {
     *       hash = 29 * hash + alleles[j].hashCode();
     *   }
     *   hash = 29 * hash + end;
     *
     * @return the code value for this marker.
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + chromIndex;
        hash = 29 * hash + this.pos;
        for (int j=0; j<alleles.length; ++j) {
            hash = 29 * hash + alleles[j].hashCode();
        }
        hash = 29 * hash + end;
        return hash;
    }

    /**
     * Returns <code>true</code> if the specified object is a
     * <code>Marker</code> with the same chromosome,
     * position, allele lists, and end value, and
     * returns <code>false</code> otherwise.
     *
     * @param obj object to be compared with <code>this</code> for equality.
     *
     * @return <code>true</code> if the specified object is a
     * <code>Marker</code> with the same chromosome,
     * position, and allele lists, and end value, and
     * returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this==obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Marker other = (Marker) obj;
        if (this.chromIndex != other.chromIndex) {
            return false;
        }
        if (this.pos != other.pos) {
            return false;
        }
        if (!Arrays.equals(this.alleles, other.alleles)) {
            return false;
        }
        return this.end == other.end;
    }

    /**
     * Compares this marker with the specified marker
     * for order, and returns a negative integer, 0, or a positive integer
     * depending on whether this marker is less than, equal to,
     * or greater than the specified marker.  Comparison is
     * on chromosome index (<code>chromIndex()</code>), position,
     * allele identifier lists, and end value in that order.  Allele
     * identifier lists are compared for lexicographical order.
     *
     * @param other the <code>Marker</code> to be compared.
     * @return a negative integer, 0, or a positive integer
     * depending on whether this marker is less than, equal,
     * or greater than the specified marker.
     */
    @Override
    public int compareTo(Marker other) {
        if (this.chromIndex != other.chromIndex) {
            return (this.chromIndex < other.chromIndex) ? -1 : 1;
        }
        if (this.pos != other.pos) {
            return (this.pos < other.pos) ? -1 : 1;
        }
        int n = Math.min(this.alleles.length, other.alleles.length);
        for (int j=0; j<n; ++j) {
            int cmp = this.alleles[j].compareTo(other.alleles[j]);
            if (cmp != 0) {
                return cmp;
            }
        }
        if (this.alleles.length != other.alleles.length) {
            return (this.alleles.length < other.alleles.length) ? -1 : 1;
        }
        if (this.end != other.end) {
            return (this.end < other.end) ? -1 : 1;
        }
        return 0;
    }
}
