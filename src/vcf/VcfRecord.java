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

import beagleutil.Samples;
import blbutil.Const;
import blbutil.StringUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import main.Marker;

/* Code review on 07 Jul 2013 */

/**
 * Class <code>VcfRecord</code> represents a VCF record in VCF version 4.1
 * format.  Class <code>VcfRecord</code> is immutable.
 *
 * @author Brian L Browning
 */
public class VcfRecord {

    public static final int nFixedFields = 8;

    private final String vcfRecord;
    private final VcfHeader vcfHeader;
    private final Marker marker;
    private final String qual;
    private final String filter;
    private final String info;
    private final String format;

    private final double qualityScore;

    private final String[] failedFilters;
    private final boolean filtersApplied;
    private final String[] infoFields;

    private final String[] formatFields;
    private final String[][] sampleData;
    private final boolean hasGTFormat;
    private final byte[][] codedGenotypes;
    private final boolean[] isPhased;

    private Map<String, Integer> formatMap;

    /**
     * Creates a VCF record for the specified line.
     *
     * @param vcfRecord a VCF file record.
     * @param vcfHeader meta-information and header lines for the
     * specified VCF record.
     *
     * @throws IllegalArgumentException if specified VCF record is not
     * consistent with the specified <code>VcfHeader</code> object,
     * if the fixed fields of the VCF record are incorrectly formated, or
     * if there there are an incorrect number of format fields, or if the
     * genotype field of a non-excluded sample is incorrectly formatted.
     * @throws NullPointerException if
     * <code>vcfRecord==null || vcfHeader==null</code>.
     */
    public VcfRecord(String vcfRecord, VcfHeader vcfHeader) {
        this.vcfRecord = vcfRecord;
        if (vcfHeader==null) {
            throw new NullPointerException("vcfHeader==null");
        }
        this.vcfHeader = vcfHeader;
        this.marker = new Marker(vcfRecord);
        String[] fields = StringUtil.getFields(vcfRecord, Const.tab);
        checkNumFields(fields, vcfHeader.unfilteredSize());
        this.qualityScore = fromPhred(quality(fields[5]));
        this.qual = fields[5];
        this.filter = fields[6];
        this.info = fields[7];
        this.format = (fields.length > 8) ? fields[8] : "";
        this.filtersApplied = fields[6].equals(Const.MISSING_DATA)==false;
        this.failedFilters = failedFilters(fields[6]);
        this.infoFields = StringUtil.getFields(fields[7], Const.semicolon);

        this.formatFields = fields.length > 8 ? formats(fields[8]) : new String[0];
        this.formatMap = formatToIndexMap(vcfHeader, vcfRecord, formatFields);
        this.hasGTFormat = formatMap.containsKey("GT");
        int n = vcfHeader.sample().nSamples();
        this.codedGenotypes = new byte[2][n];
        this.isPhased = new boolean[n];
        this.sampleData = new String[n][formatFields.length];
        if (n > 0) {
            String[] fmtFields = Arrays.copyOfRange(fields, 9, fields.length);
            storePerSampleData(fmtFields);
        }
    }

    private static void checkNumFields(String[] fields, int nUnfilteredSamples) {
        int nVarFields = nUnfilteredSamples==0 ? 0 : (1 + nUnfilteredSamples);
        int nExpectedFields = nFixedFields + nVarFields;
        if (nExpectedFields != fields.length) {
            String s = "Expected " + nExpectedFields + " fields, but found "
                    + fields.length + Arrays.toString(fields);
            throw new IllegalArgumentException(s);
        }
    }

    /**
     * Return <code>true</code> if all characters in the specified
     * string are letters or digits and returns <code>false</code> otherwise.
     * @param s a string.
     * @return <code>true</code> if all characters in the specified
     * string are letters or digits and returns <code>false</code> otherwise.
     */
    public static boolean isAlphanumeric(String s) {
        for (int j=0, n=s.length(); j<n; ++j) {
            if (Character.isLetterOrDigit(s.charAt(j))==false) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns <code>-10.0*Math.log10(d)</code>.
     * @param d a double to convert to the Phred scale.
     * @return <code>-10.0*Math.log10(d)</code>
     */
    public static double toPhred(double d) {
        return -10.0*Math.log10(d);
    }

   /**
     * Returns <code>Math.pow(10.0, -phredScore/10.0)</code>.
     * @param d a double to convert from the Phred scale.
     * @return <code>Math.pow(10.0, -phredScore/10.0)</code>.
     */
    public static double fromPhred(double phredScore) {
        return Math.pow(10.0, -phredScore/10.0);
    }

    private double quality(String quality) {
        if (quality.equals(Const.MISSING_DATA)) {
            return Double.NaN;
        }
        else {
            return Double.parseDouble(quality);
        }
    }

    private String[] failedFilters(String filters) {
        if (filters.isEmpty()) {
            String s = "missing filters field: " + vcfRecord;
            throw new IllegalArgumentException(s);
        }
        if (filters.equals(Const.MISSING_DATA) || filters.equals("PASS")) {
            return new String[0];
        }
        String[] fields = StringUtil.getFields(filters, Const.semicolon);
        for (String f : fields) {
            if (f.isEmpty()) {
                String s = "missing filter in filter list: " + filters;
                throw new IllegalArgumentException(s);
            }
        }
        return fields;
    }

    private String[] formats(String formats) {
        if (formats.equals(Const.MISSING_DATA)) {
            return new String[0];
        }
        String[] fields =  StringUtil.getFields(formats, Const.colon);
        for (String f : fields) {
            if (f.isEmpty()) {
                String s = "missing format in format list: " + vcfRecord;
                throw new IllegalArgumentException(s);
            }
            // commented-out alpha-numeric check to avoid throwing errors on mis-specified VCF files
//            if (isAlphanumeric(f)==false) {
//                 String s = "format must be alphanumeric (" + f + "): " + vcfRecord;
//                 throw new IllegalArgumentException(s);
//            }
        }
        return fields;
    }

    private static Map<String, Integer> formatToIndexMap(VcfHeader vcfHeader,
            String vcfRecord, String[] formatFields) {
        if (vcfHeader.sample().nSamples()==0) {
            return Collections.emptyMap();
        }
        Map<String, Integer> map = new HashMap<String, Integer>(formatFields.length);
        for (int j=0; j<formatFields.length; ++j) {
            map.put(formatFields[j], j);
        }
        if (map.containsKey("GT") && map.get("GT")!=0) {
            String s = "GT format is not first format: " + vcfRecord;
            throw new IllegalArgumentException(s);
        }
        return map;
    }

    /* Note: only GT field (optional first data field) is checked for validaty */
    private void storePerSampleData(String[] sampleFields) {
        int index = 0;
        for (int j=0; j<sampleFields.length; ++j) {
            if (vcfHeader.filter(j)==false) {
                String[] fields = parseFormatFields(sampleFields, j);
                if (hasGTFormat) {
                    String gt = fields[0];
                    // To Do: need to handle deletions represented as ploidy==1
                    int sepIndex = separatorIndex(gt);
                    codedGenotypes[0][index] = stringAlleleToByte(gt.substring(0, sepIndex));
                    codedGenotypes[1][index] = stringAlleleToByte(gt.substring(sepIndex+1));
                    isPhased[index] = (gt.charAt(sepIndex)==Const.phasedSep);
                }
                else {
                    codedGenotypes[0][index] = -1;
                    codedGenotypes[1][index] = -1;
                    isPhased[index] = false;
                }
                sampleData[index++] = fields;
            }
        }
        assert index==vcfHeader.sample().nSamples();
    }

    private String[] parseFormatFields(String[] sampleFields, int sampleIndex) {
        String sampleField = sampleFields[sampleIndex];
        if (sampleField.isEmpty()) {
            String s = "Missing format field for sample " +
                    vcfHeader.sample().id(sampleIndex) + ": " + vcfRecord;
            throw new IllegalArgumentException(s);
        }
        if (sampleField.equals(Const.MISSING_DATA)) {
            String[] fields = new String[formatFields.length];
            Arrays.fill(fields, Const.MISSING_DATA);
            if (hasGTFormat) {
                fields[0] = "./.";
            }
            return fields;
        }
        else {
            String[] fields = StringUtil.getFields(sampleField, Const.colon);
            for (String f : fields) {
                if (f.isEmpty()) {
                    String s = "empty format field for sample " + sampleIndex
                            + ": " + sampleFields[sampleIndex];
                    throw new IllegalArgumentException(s);
                }
            }
            if (fields.length < formatFields.length) {
                String[] newFields = Arrays.copyOf(fields, formatFields.length);
                for (int k=fields.length; k<newFields.length; ++k) {
                    newFields[k] = Const.MISSING_DATA;
                }
                fields = newFields;
            }
            if (fields.length > formatFields.length) {
                String s = "Expected " + formatFields.length + " format fields, "
                        + "but found " + fields.length + "(" +
                        sampleFields[sampleIndex] + ") : " + vcfRecord;
                throw new IllegalArgumentException(s);
            }
            return fields;
        }
    }

    /**
     * Returns the index of the genotype separator;
     */
    private int separatorIndex(String gt) {
        int index = gt.indexOf(Const.unphasedSep);
        if (index == -1) {
            index = gt.indexOf(Const.phasedSep);
            if (index== -1) {
                String s = "missing genotype separator ("
                        + gt + "): " + vcfRecord;
                throw new IllegalArgumentException(s);
            }
        }
        return index;
    }

    private byte stringAlleleToByte(String allele) {
        if (allele.equals(Const.MISSING_DATA)) {
            return -1;
        }
        int a = Integer.parseInt(allele);
        if (a < 0) {
            String s = "allele cannot be negative (" + a + "): " + vcfRecord;
            throw new IllegalArgumentException(s);
        }
        if (a >= marker.nAlleles()) {
            String s = "allele " + a + " is not defined: " + vcfRecord;
            throw new IllegalArgumentException(s);
        }
        if (a > Byte.MAX_VALUE) {
            String s = "Marker cannot have more than " + Byte.MAX_VALUE
                    + " alternate alleles: " + vcfRecord;
            throw new IllegalArgumentException(s);
        }
        return (byte) a;
    }

    /**
     * Returns the QUAL field of the VCF record represented by
     * <code>this</code>.
     * @return the QUAL field of the VCF record represented by
     * <code>this</code>.
     */
    public String qual() {
        return qual;
    }

    /**
     * Returns phred-scaled quality score for the probability that the
     * asserted alternate alleles are wrong.
     * If ALT is ”.” (no variant) then this is -10log_10 p(variant),
     * and if ALT is not ”.” then this is -10log_10 p(no variant).
     *
     * @return phred-scaled quality score for the probability that the
     * asserted alternate alleles are wrong.
     */
    public double qualityScore() {
        return qualityScore;
    }

    /**
     * Returns <code>true</code> if filters were applied, and
     * <code>false</code> if the filters field data is missing.
     *
     * @returns <code>true</code> if filters were applied, and
     * <code>false</code> if the filters field data is missing.
     */
    public boolean filtersApplied() {
        return filtersApplied;
    }

    /**
     * Returns the FILTER field of the VCF record represented by
     * <code>this</code>.
     * @return the FILTER field of the VCF record represented by
     * <code>this</code>.
     */
    public String filter() {
        return filter;
    }

    /**
     * Returns the number of failed filters.
     * @return the number of failed filters.
     */
    public int failedFilters() {
        return failedFilters.length;
    }

    /**
     * Returns the specified failed filter
     *
     * @param index a failed filter index.
     * @return the specified failed filter.
     *
     * @throws IndexOutOfBoundsException if
     * <code>indexOf < 0 || indexOf &ge; this.failedFilters()</code>.
     */
    public String failedFilters(int index) {
        return failedFilters[index];
    }

    /**
     * Returns the INFO field of the VCF record represented by
     * <code>this</code>.
     * @return the INFO field of the VCF record represented by
     * <code>this</code>.
     */
    public String info() {
        return info;
    }

    /**
     * Returns the number of information fields.
     * @return the number of information fields.
     */
    public int infoFields() {
        return infoFields.length;
    }

    /**
     * Returns the specified information field.
     * @param indexOf an index of an information field.
     * @return an index of an information field.
     *
     * @throws IndexOutOfBoundsException if
     * <code>indexOf < 0 || indexOf &ge; this.infoFields()</code>.
     */
    public String infoField(int index) {
        return infoFields[index];
    }

    /**
     * Returns the FORMAT field of the VCF record represented by
     * <code>this</code>.  Returns the empty string ("") if the FORMAT
     * field is missing.
     * @return the FORMAT field of the VCF record represented by
     * <code>this</code>.
     */
    public String format() {
        return format;
    }

    /**
     * Returns the number of format fields.
     * @return the number of format fields.
     */
    public int formatFields() {
        return formatFields.length;
    }

    /**
     * Returns the specified format field.
     * @param index a format field index.
     * @return the specified format field.
     *
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || indexOf &ge; this.formatFields()</code>.
     */
    public String formatField(int index) {
        if (formatFields==null) {
            throw new IllegalArgumentException("No format exists");
        }
        return formatFields[index];
    }

    /**
     * Returns <code>true</code> if the specified format field is
     * defined for the VCF record and false otherwise.
     * @param formatCode the string format code.
     * @return <code>true</code> if the specified format field is
     * defined for the VCF record and false otherwise.
     */
    public boolean hasFormat(String formatCode) {
        return formatMap.get(formatCode)!=null;
    }

    /**
     * Returns the index of the specified format if the specified format
     * field is defined for the VCF record and returns -1 otherwise.
     * @param formatCode the string format code.
     * @return the index of the specified format if the specified format
     * field is defined for the VCF record and returns -1 otherwise.
     */
    public int formatIndex(String formatCode) {
        Integer index = formatMap.get(formatCode);
        return (index==null) ? -1 : index.intValue();
    }

    /**
     * Returns the specified allele in the GT field of the specified sample.
     * @param sampleIndex a sample indexOf.
     * @return the specified allele in the GT field of the specified sample.
     *
     * @throws IndexOutOfBoundsException if
     * <code>sampleIndex < 0 || sampleIndex &ge; this.samples()
     * || alleleIndex < 0 || alleleIndex &ge; 2</code>.
     */
    public byte gt(int sampleIndex, int alleleIndex) {
        return codedGenotypes[alleleIndex][sampleIndex];
    }

    /**
     * Returns <code>true</code> if the genotype for the specified sample is
     * phased and <code>false</code> if the genotype is unphased.
     * @param sampleIndex a sample index.
     * @return  <code>true</code> if the genotype for the specified sample is
     * phased and <code>false</code> if the genotype is unphased.
     *
     * @throws IndexOutOfBoundsException if
     * <code>sampleIndex < 0 || sampleIndex &ge; this.samples()</code>.
     */
    public boolean isPhased(int sampleIndex) {
        return isPhased[sampleIndex];
    }

    /**
     * Returns the specified format-field data for the specified sample.
     * @param formatCode a format code.
     * @param sampleIndex a sample index
     * @return the specified format-field data for the specified sample.
     *
     * @throws IllegalArgumentException if
     * <code>this.hasFormat(formatCode)==false</code>.
     * @throws IndexOutOfBoundsException if
     * <code>sampleIndex < 0 || sampleIndex &ge; this.samples()</code>.
     */
    public String sampleData(String formatCode, int sampleIndex) {
        Integer formatIndex = formatMap.get(formatCode);
        if (formatIndex==null) {
            String s = "missing format data: " + formatCode;
            throw new IllegalArgumentException(s);
        }
        return sampleData[sampleIndex][formatIndex.intValue()];
    }

    /**
     * Returns the specified format-field data for the specified sample.
     * @param formatIndex a format field index.
     * @param sampleIndex a sample index.
     * @return the specified format-field data for the specified sample.
     *
     * @throws IndexOutOfBoundsException if
     * <code>formatIndex < 0 || formatIndex &ge; this.formatFields()</code>,
     * or if
     * <code>sampleIndex < 0 || sampleIndex &ge; this.samples()</code>.
     */
    public String sampleData(int formatIndex, int sampleIndex) {
        return sampleData[sampleIndex][formatIndex];
    }

    /**
     * Returns an array of length <code>this.samples()</code>
     * containing the specified format-field data for all samples.  The
     * <code>k</code>-th element of the array is the format-field data
     * for the <code>k</code>-th sample.
     * @param formatCode a format-field code.
     * @return an array of length <code>this.samples()</code>
     * containing the specified format-field data for all samples.
     *
     * @throws IllegalArgumentException if
     * <code>this.hasFormat(formatCode)==false</code>.
     */
    public String[] sampleData(String formatCode) {
        Integer formatIndex = formatMap.get(formatCode);
        if (formatIndex==null) {
            String s = "missing format data: " + formatCode;
            throw new IllegalArgumentException(s);
        }
        return sampleData(formatIndex);
    }

   /**
     * Returns an array of length <code>this.samples()</code>
     * containing the specified format-field data for all samples.  The
     * <code>k</code>-th element of the array is the specified format-field
     * data for the <code>k</code>-th sample.
     * @param formatIndex a format field index.
     * @return an array of length <code>this.samples()</code>
     * containing the specified format-field data for all samples.
     *
     * @throws IndexOutOfBoundsException if
     * <code>formatIndex < 0 || formatIndex &ge; this.formatFields()</code>.
     */
    public String[] sampleData(int formatIndex) {
        String[] sa = new String[sampleData.length];
        for (int j=0; j<sa.length; ++j) {
            sa[j] = sampleData[j][formatIndex];
        }
        return sa;
    }

    /**
     * Returns the samples. The returned samples are the filtered samples
     * obtained after all sample exclusions.
     *
     * @return the samples.
     */
    public Samples samples() {
        return vcfHeader.sample();
    }

    /**
     * Returns the number of samples.  The number of samples is the
     * number of filtered samples after all sample exclusions.
     *
     * @return the number of samples.
     */
    public int nSamples() {
        return vcfHeader.sample().nSamples();
    }

    /**
     * Returns the <code>VcfHeader</code> corresponding to <code>this</code>.
     * @return the <code>VcfHeader</code> corresponding to <code>this</code>.
     */
    public VcfHeader vcfHeader() {
        return vcfHeader;
    }

    /**
     * Returns the <code>Marker</code> represented by <code>this</code>.
     * @return the <code>Marker</code> represented by <code>this</code>.
     */
    public Marker marker() {
        return marker;
    }

    /**
     * Returns a string equal to the VCF record represented by
     * <code>this</code>.
     * @return a string equal to the VCF record represented by
     * <code>this</code>.
     */
    @Override
    public String toString() {
        return vcfRecord;
    }
}
