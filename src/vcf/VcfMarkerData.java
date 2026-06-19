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
import blbutil.FileIterator;
import blbutil.InputStreamIterator;
import blbutil.SampleFileIterator;
import blbutil.StringUtil;
import beagleutil.ChromInterval;
import beagleutil.Samples;
import blbutil.Filter;
import ibd.IntPairToIntMap;
import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.ExcludeIdFilter;
import main.Marker;

/* Code review on 03 Oct 2013 */

/**
 * Class <code>VcfMarkerData</code> represents genotype data (GT FORMAT field)
 * in a VCF file together with per-marker missing and allele count summary
 * statistics.
 *
 * @author Brian L. Browning <browning@uw.edu>
 */
public final class VcfMarkerData {

    private static final int INIT_N_MARKERS = 100000;
    private static final float LOAD_FACTOR = 0.75f;
    public static final int NULL_VALUE = -79;

    private final int nInitialMarkers; // includes markers failing MAF filter
    private final int nMafFilteredMarkers;
    private final List<Marker> freqExcludedMarkers;
    private final List<Marker> r2ExcludedMarkers;
    private final List<MarkerData> dataList;
    private final List<ScoreMarkerData> scoreDataList;
    private final BitSet isCorrelated;
    private final Samples samples;
    private byte[][] sampleDoses; // lazily built; [sample][marker], missing==3

    /**
     * Constructs a <code>VcfMarkerData</code> instance for the specified
     * genotype data.
     *
     * @param gtFile VCF file with GT FORMAT fields for each VCF record.
     * @param usePhase <code>true</code> if phase information in the specified
     * VCF file record will be used, and <code>false</code> if phase
     * information in the specified VCF file record will be ignored.
     * @param sampleExclusions samples to be excluded.
     * @param markerExclusions markers to be excluded.
     * @param chromInterval a string representing a marker filter.
     * @param minAlleleCount minimum count required for at least two of
     * a marker's alleles in order for the marker to pass the
     * minor allele frequency filter.
     * @param r2Window number of markers in sliding marker window used
     * to detect high-correlated alleles  Pairs of markers with indices whose
     * difference exceeds <code>r2Window</code> are assumed to
     * have correlation less than or equal to the specified
     * <code>r2Max</code> value.  Marker indices are assigned after excluding
     * markers in the specified excluded markers file and after excluding
     * markers failing the minor allele frequency filter (see
     * <code>minAlleleCount</code> parameter).
     * @param r2Max maximum squared correlation permitted between
     * any two markers.
     *
     * @throws NullPointerException if <code>gtFile==null/code>.
     *
     * @throws IllegalArgumentException if the specified <code>gtFile</code>
     * does not conform to the specifications for a VCF file, if
     * <code>MarkerFilter.isValidRegion(markerFilterString)==false</code>,
     * or if <code>minAlleleCount < 0 || r2Window < 0 || r2Max < 0.0f
     * || r2Max > 1.0f || float.isNaN(r2Max)</code>.
     */
    public VcfMarkerData(File gtFile, boolean usePhase, File exclMarkersFile,
            File exclSamplesFile, String chromInterval,
            int minAlleleCount, int r2Window, float r2Max) {
        this(gtFile, usePhase, exclMarkersFile, exclSamplesFile, chromInterval,
                minAlleleCount, r2Window, r2Max, null);
    }

    /**
     * Constructs a <code>VcfMarkerData</code> instance for the specified
     * genotype data.
     *
     * @param gtFile VCF file with GT FORMAT fields for each VCF record.
     * @param usePhase <code>true</code> if phase information in the specified
     * VCF file record will be used, and <code>false</code> if phase
     * information in the specified VCF file record will be ignored.
     * @param sampleExclusions samples to be excluded.
     * @param markerExclusions markers to be excluded.
     * @param chromInterval a string representing a marker filter.
     * @param minAlleleCount minimum count required for at least two of
     * a marker's alleles in order for the marker to pass the
     * minor allele frequency filter.
     * @param r2Window number of markers in sliding marker window used
     * to detect high-correlated alleles.
     * @param r2Max maximum squared correlation permitted between
     * any two markers.
     * @param scoreFreqFile a score-frequency file, or <code>null</code>.
     *
     * @throws NullPointerException if <code>gtFile==null/code>.
     *
     * @throws IllegalArgumentException if the specified <code>gtFile</code>
     * does not conform to the specifications for a VCF file, if
     * <code>MarkerFilter.isValidRegion(markerFilterString)==false</code>,
     * or if <code>minAlleleCount < 0 || r2Window < 0 || r2Max < 0.0f
     * || r2Max > 1.0f || float.isNaN(r2Max)</code>.
     */
    public VcfMarkerData(File gtFile, boolean usePhase, File exclMarkersFile,
            File exclSamplesFile, String chromInterval,
            int minAlleleCount, int r2Window, float r2Max, File scoreFreqFile) {
        if (minAlleleCount < 0) {
            throw new IllegalArgumentException("minAlleles: " + minAlleleCount);
        }
        if (r2Window < 0) {
            throw new IllegalArgumentException("r2Window: " + r2Window);
        }
        if (r2Max < 0.0f || r2Max > 1.0f || Float.isNaN(r2Max)) {
            throw new IllegalArgumentException("r2Max: " + r2Max);
        }
        SampleFileIterator<VcfRecord> it = vcfIterator(gtFile, exclMarkersFile,
                exclSamplesFile, chromInterval);
        this.samples = it.samples();
        this.freqExcludedMarkers = new ArrayList<Marker>(1000);
        this.r2ExcludedMarkers = new ArrayList<Marker>(1000);
        this.dataList = new ArrayList<MarkerData>(INIT_N_MARKERS);
        this.scoreDataList = new ArrayList<ScoreMarkerData>(INIT_N_MARKERS);
        this.isCorrelated = new BitSet(INIT_N_MARKERS);

        if (scoreFreqFile==null) {
            this.nInitialMarkers = readData(it, usePhase, minAlleleCount,
                    r2Window, r2Max, dataList, scoreDataList, isCorrelated,
                    freqExcludedMarkers);
            this.nMafFilteredMarkers = dataList.size();
            // Keep LD-correlated markers for exclusion-only scoring (stock
            // IBDseq behavior); record them only for the .r2.filtered report.
            recordCorrelatedMarkers(dataList, isCorrelated, r2ExcludedMarkers);
        }
        else {
            ScoreFileData scoreFileData = readScoreFile(scoreFreqFile);
            this.nInitialMarkers = readScoreData(it, usePhase, scoreFileData,
                    dataList, scoreDataList, isCorrelated);
            this.nMafFilteredMarkers = dataList.size();
        }
        it.close();
    }

    private static SampleFileIterator<VcfRecord> vcfIterator(File gtFile,
            File exclMarkersFile, File exclSamplesFile, String chromInterval) {
        Filter<String> sampleFilter = VcfUtils.ACCEPT_ALL_FILTER;
        if (exclSamplesFile!=null) {
            sampleFilter = new VcfUtils.ExcludeSampleFilter(exclSamplesFile);
        }
        ChromInterval ci = ChromInterval.parse(chromInterval);
        SampleFileIterator<VcfRecord> it = new VcfIterator(gtFile, sampleFilter);
        if (ci != null) {
            it = new IntervalVcfIterator(it, ci);
        }
        if (exclMarkersFile!=null) {
            Filter<Marker> markerFilter = new ExcludeIdFilter(exclMarkersFile);
            it = new FilteredVcfIterator(it, markerFilter);
        }
        return it;
    }

    private static int readData(SampleFileIterator<VcfRecord> it, boolean usePhase,
            int minAlleleCount, int r2Window, float r2Max,
            List<MarkerData> dataList, List<ScoreMarkerData> scoreDataList,
            BitSet isExcluded, List<Marker> lowAlleleList) {
        int markerCnt = 0;
        int chromIndex = -1;
        while (it.hasNext()) {
            ++markerCnt;
            VcfMarker obsData = new LowMemGT(it.next(), usePhase);
            if (chromIndex == -1) {
                chromIndex = obsData.marker().chromIndex();
            }
            if (obsData.marker().chromIndex()!=chromIndex) {
                break;
            }
            MarkerData mData = new MarkerData(obsData);
            byte minor = mData.minorAllele();
            if (minor != -1 && mData.alleleCount(minor) >= minAlleleCount) {
                dataList.add(mData);
                float freq = mData.alleleFrequency(minor);
                scoreDataList.add(new ScoreMarkerData(minor, freq));
                if (r2Window>0) {
                    checkLastMarkerR2(r2Window, r2Max, dataList, isExcluded);
                }
            }
            else {
                lowAlleleList.add(mData.marker());
            }
        }
        return markerCnt;
    }

    /*
     * Records the LD-correlated markers in <code>r2ExcludedMarkers</code> (for
     * the .r2.filtered report) without removing them. The markers are retained
     * in <code>dataList</code> with their <code>isCorrelated</code> flags so
     * they contribute exclusion-only scores (opposite homozygotes for IBD,
     * heterozygotes for HBD), matching stock IBDseq on the merged sample set.
     */
    private static void recordCorrelatedMarkers(List<MarkerData> dataList,
            BitSet isCorrelated, List<Marker> r2ExcludedMarkers) {
        for (int j=0, n=dataList.size(); j<n; ++j) {
            if (isCorrelated.get(j)) {
                r2ExcludedMarkers.add(dataList.get(j).marker());
            }
        }
    }

    private static int readScoreData(SampleFileIterator<VcfRecord> it,
            boolean usePhase, ScoreFileData scoreFileData,
            List<MarkerData> dataList, List<ScoreMarkerData> scoreDataList,
            BitSet isCorrelated) {
        MarkerData[] matchedData = new MarkerData[scoreFileData.size()];
        ScoreMarkerData[] matchedScoreData = new ScoreMarkerData[scoreFileData.size()];
        Map<CoordKey, ScoreKey> nonMatchingCoordMap =
                new HashMap<CoordKey, ScoreKey>();
        int markerCnt = 0;
        int chromIndex = -1;
        while (it.hasNext()) {
            ++markerCnt;
            VcfMarker obsData = new LowMemGT(it.next(), usePhase);
            Marker marker = obsData.marker();
            if (chromIndex == -1) {
                chromIndex = marker.chromIndex();
            }
            if (marker.chromIndex()!=chromIndex) {
                break;
            }
            ScoreKey key = scoreKey(marker);
            ScoreFileRecord scoreRecord = scoreFileData.record(key);
            if (scoreRecord!=null) {
                int index = scoreRecord.index();
                if (matchedData[index]!=null) {
                    String s = "Duplicate VCF marker matches scorefreq marker: "
                            + key;
                    throw new IllegalArgumentException(s);
                }
                MarkerData mData = new MarkerData(obsData);
                byte allele = alleleIndex(marker, scoreRecord.allele());
                if (allele < 0) {
                    String s = "scorefreq allele not found in VCF marker: "
                            + scoreRecord.allele() + " at " + key;
                    throw new IllegalArgumentException(s);
                }
                matchedData[index] = mData;
                matchedScoreData[index] =
                        new ScoreMarkerData(allele, scoreRecord.frequency());
            }
            else if (scoreFileData.hasCoord(key.coord())) {
                nonMatchingCoordMap.put(key.coord(), key);
            }
        }
        for (int j=0; j<matchedData.length; ++j) {
            if (matchedData[j]==null) {
                ScoreFileRecord scoreRecord = scoreFileData.record(j);
                ScoreKey observed = nonMatchingCoordMap.get(scoreRecord.key().coord());
                if (observed==null) {
                    String s = "scorefreq marker not found after VCF filters: "
                            + scoreRecord.key();
                    throw new IllegalArgumentException(s);
                }
                else {
                    String s = "scorefreq allele mismatch at "
                            + scoreRecord.key().chrom() + Const.colon
                            + scoreRecord.key().pos() + ": expected "
                            + scoreRecord.key() + ", found " + observed;
                    throw new IllegalArgumentException(s);
                }
            }
            if (scoreFileData.record(j).correlated()) {
                isCorrelated.set(dataList.size());
            }
            dataList.add(matchedData[j]);
            scoreDataList.add(matchedScoreData[j]);
        }
        return markerCnt;
    }

    private static void checkLastMarkerR2(int r2Window, float r2Max,
            List<MarkerData> dataList, BitSet isExcluded) {
        int radius = 1 + r2Window/2;
        int startIndex = Math.max(0, dataList.size() - radius);
        MarkerData mdB = dataList.get(dataList.size()-1);
        byte alleleB = mdB.majorAllele();
        boolean finished = false;
        for (int k=dataList.size()-2; k>=startIndex && finished==false; --k) {
            if (isExcluded.get(k) == false) {
                MarkerData mdA = dataList.get(k);
                byte alleleA = mdA.majorAllele();
                double r2 = MarkerData.r2(mdA, mdB, alleleA, alleleB);
                if (r2 > r2Max) {
                    if (mdB.alleleFrequency(alleleB) > mdA.alleleFrequency(alleleA)) {
                        isExcluded.set(k);
                    }
                    else {
                        isExcluded.set(dataList.size()-1);
                        finished = true;
                    }
                }
            }
        }
    }

    /**
     * Returns the number of markers read from the VCF file (including
     * markers failing the minimum allele carrier filter).
     */
    public int nInitialMarkers() {
        return nInitialMarkers;
    }

    /**
     * Returns the number of markers excluded by the minor allele frequency
     * filter.
     * @return the number of markers excluded by the minor allele frequency
     * filter.
     */
    public int nFrequencyExcludedMarkers() {
        return freqExcludedMarkers.size();
    }

    /**
     * Returns the number of markers passing the minor allele frequency
     * filter before LD thinning.
     * @return the number of markers passing the minor allele frequency
     * filter before LD thinning.
     */
    public int nMafFilteredMarkers() {
        return nMafFilteredMarkers;
    }

    /**
     * Returns the specified marker excluded by the minor allele frequency
     * filter.
     * @param index the index in the list of markers that failed
     * the minor allele frequency filter.
     * @return the specified marker excluded by the minor allele frequency
     * filter.
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.nFrequencyExcludedMarkers</code>.
     */
    public Marker frequencyExcludedMarkers(int index) {
        return freqExcludedMarkers.get(index);
    }

    /**
     * Returns the specified marker excluded by the LD filter.
     * @param index the index in the list of markers that failed
     * the LD filter.
     * @return the specified marker excluded by the LD filter.
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.nCorrelatedMarkers</code>.
     */
    public Marker correlatedMarker(int index) {
        return r2ExcludedMarkers.get(index);
    }

    /**
     * Returned the number of markers.  The returned count does not include
     * markers failing the minor allele frequency filter.
     * @return the number of markers.
     */
    public int nMarkers() {
        return dataList.size();
    }

    /**
     * Returns the number of samples represented by <code>this</code>.
     * @return the number of samples represented by <code>this</code>.
     */
    public int nSamples() {
        return samples.nSamples();
    }

    /**
     * Returns the samples represented by <code>this</code>.
     * @return the samples represented by <code>this</code>.
     */
    public Samples samples() {
        return samples;
    }

    /**
     * Returns the specified marker data.
     * @param index a marker index.
     * @return the specified marker data.
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.nMarkers()</code>.
     */
    public MarkerData get(int index) {
        return dataList.get(index);
    }

    /**
     * Returns the allele used for allele-dose scoring at the specified marker.
     * @param index a marker index.
     * @return the allele used for allele-dose scoring at the specified marker.
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.nMarkers()</code>.
     */
    public byte scoreAllele(int index) {
        return scoreDataList.get(index).allele();
    }

    /**
     * Returns the allele frequency used for IBD/HBD scoring at the
     * specified marker.
     * @param index a marker index.
     * @return the allele frequency used for IBD/HBD scoring at the
     * specified marker.
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.nMarkers()</code>.
     */
    public float scoreFrequency(int index) {
        return scoreDataList.get(index).frequency();
    }

    /**
     * Returns <code>true</code> if the specified marker must be excluded
     * to remove remove excess inter-marker correlation, and returns
     * <code>false</code> otherwise.
     *
     * @param index a marker index.
     *
     * @return <code>true</code> if the specified marker must be excluded
     * to remove remove excess inter-marker correlation, and returns
     * <code>false</code> otherwise.
     *
     * @throws IndexOutOfBoundsException if
     * <code>index < 0 || index &ge; this.nMarkers()</code>.
     */
    public boolean isCorrelated(int index) {
        return isCorrelated.get(index);
    }

    /**
     * Returns a sample-major table of scored-allele doses, indexed
     * <code>[sample][marker]</code>. Each entry is the dose (0, 1, or 2) of the
     * marker's scored allele for the sample, with missing genotypes encoded as
     * 3. The table is built once on first call and cached. The contiguous
     * per-sample marker rows give the detection scan two sequential streams per
     * pair, replacing per-marker genotype reconstruction.
     * @return a sample-major table of scored-allele doses.
     */
    public byte[][] sampleDoses() {
        if (sampleDoses == null) {
            int nMarkers = dataList.size();
            int nSamples = samples.nSamples();
            byte[][] doses = new byte[nSamples][nMarkers];
            for (int j=0; j<nMarkers; ++j) {
                MarkerData md = dataList.get(j);
                byte allele = scoreDataList.get(j).allele();
                for (int s=0; s<nSamples; ++s) {
                    int dose = md.dose(s, allele);
                    doses[s][j] = (byte) (dose < 0 ? 3 : dose);
                }
            }
            sampleDoses = doses;
        }
        return sampleDoses;
    }

    /**
     * Returns the number of markers that must be excluded to remove
     * excess inter-marker correlation.
     * @return the number of markers that must be excluded to remove
     * excess inter-marker correlation.
     */
    public int nCorrelatedMarkers() {
        return r2ExcludedMarkers.size();
    }

    /**
     * Returns a map of genome coordinates to marker indices.  The
     * genome coordinates are an ordered pair: (chromosome index, position).
     * The marker indices are determined after excluding markers
     * failing the minor allele frequency filter.
     *
     * @return a map of genome coordinates to marker indices.
     */
    public IntPairToIntMap markerIndexMap() {
        IntPairToIntMap map = new IntPairToIntMap(INIT_N_MARKERS,
                LOAD_FACTOR, NULL_VALUE);
        for (int j=0, n=dataList.size(); j<n; ++j) {
            Marker m = dataList.get(j).marker();
            map.put(m.chromIndex(), m.pos(), j);
        }
        return map;
    }

    private static ScoreFileData readScoreFile(File scoreFile) {
        List<ScoreFileRecord> records = new ArrayList<ScoreFileRecord>();
        Map<ScoreKey, ScoreFileRecord> recordMap =
                new HashMap<ScoreKey, ScoreFileRecord>();
        Map<CoordKey, Boolean> coordMap = new HashMap<CoordKey, Boolean>();
        FileIterator<String> it = InputStreamIterator.fromGzipFile(scoreFile);
        int lineCnt = 0;
        while (it.hasNext()) {
            ++lineCnt;
            String line = it.next().trim();
            if (line.length() > 0) {
                String[] fields = StringUtil.getFields(line, Const.tab);
                if (isScoreHeader(fields)==false) {
                    ScoreFileRecord record =
                            scoreFileRecord(scoreFile, fields, lineCnt,
                            records.size());
                    if (recordMap.containsKey(record.key())) {
                        String s = "Duplicate scorefreq marker on line "
                                + lineCnt + " of " + scoreFile + ": "
                                + record.key();
                        throw new IllegalArgumentException(s);
                    }
                    records.add(record);
                    recordMap.put(record.key(), record);
                    coordMap.put(record.key().coord(), Boolean.TRUE);
                }
            }
        }
        it.close();
        if (records.isEmpty()) {
            throw new IllegalArgumentException("Empty scorefreq file: " + scoreFile);
        }
        return new ScoreFileData(records, recordMap, coordMap);
    }

    private static boolean isScoreHeader(String[] fields) {
        boolean baseMatch = fields.length>=7
                && fields[0].equals("CHROM")
                && fields[1].equals("POS")
                && fields[2].equals("ID")
                && fields[3].equals("REF")
                && fields[4].equals("ALT")
                && fields[5].equals("ALLELE")
                && fields[6].equals("FREQ");
        if (baseMatch==false) {
            return false;
        }
        if (fields.length==7) {
            return true;
        }
        return fields.length==8 && fields[7].equals("LD_PRUNED");
    }

    private static ScoreFileRecord scoreFileRecord(File scoreFile,
            String[] fields, int line, int index) {
        if (fields.length != 7 && fields.length != 8) {
            String s = "Expected 7 or 8 tab-delimited fields on line " + line
                    + " of " + scoreFile + ", found " + fields.length;
            throw new IllegalArgumentException(s);
        }
        if (fields[0].length()==0 || fields[3].length()==0
                || fields[4].length()==0 || fields[5].length()==0) {
            String s = "Missing scorefreq field on line " + line + " of "
                    + scoreFile;
            throw new IllegalArgumentException(s);
        }
        int pos = parseScorePos(scoreFile, fields[1], line);
        float freq = parseScoreFreq(scoreFile, fields[6], line);
        boolean correlated = (fields.length==8)
                && parseLdPruned(scoreFile, fields[7], line);
        ScoreKey key = new ScoreKey(fields[0], pos, fields[3], fields[4]);
        return new ScoreFileRecord(index, key, fields[5], freq, correlated);
    }

    private static boolean parseLdPruned(File scoreFile, String value, int line) {
        if (value.equals("0")) {
            return false;
        }
        if (value.equals("1")) {
            return true;
        }
        String s = "Invalid LD_PRUNED value on line " + line + " of "
                + scoreFile + ": " + value + " (must be 0 or 1)";
        throw new IllegalArgumentException(s);
    }

    private static int parseScorePos(File scoreFile, String value, int line) {
        try {
            int pos = Integer.parseInt(value);
            if (pos < 1) {
                String s = "Invalid scorefreq position on line " + line
                        + " of " + scoreFile + ": " + value;
                throw new IllegalArgumentException(s);
            }
            return pos;
        }
        catch (NumberFormatException ex) {
            String s = "Invalid scorefreq position on line " + line
                    + " of " + scoreFile + ": " + value;
            throw new IllegalArgumentException(s);
        }
    }

    private static float parseScoreFreq(File scoreFile, String value, int line) {
        try {
            float freq = Float.parseFloat(value);
            if (freq <= 0.0f || freq >= 1.0f || Float.isInfinite(freq)
                    || Float.isNaN(freq)) {
                String s = "Invalid scorefreq frequency on line " + line
                        + " of " + scoreFile + ": " + value
                        + " (must be >0 and <1)";
                throw new IllegalArgumentException(s);
            }
            return freq;
        }
        catch (NumberFormatException ex) {
            String s = "Invalid scorefreq frequency on line " + line
                    + " of " + scoreFile + ": " + value;
            throw new IllegalArgumentException(s);
        }
    }

    private static ScoreKey scoreKey(Marker marker) {
        return new ScoreKey(marker.chrom(), marker.pos(), marker.allele(0),
                alt(marker));
    }

    private static String alt(Marker marker) {
        if (marker.nAlleles()==1) {
            return Const.MISSING_DATA;
        }
        StringBuilder sb = new StringBuilder(20);
        sb.append(marker.allele(1));
        for (int j=2, n=marker.nAlleles(); j<n; ++j) {
            sb.append(Const.comma);
            sb.append(marker.allele(j));
        }
        return sb.toString();
    }

    private static byte alleleIndex(Marker marker, String allele) {
        for (byte j=0, n=(byte) marker.nAlleles(); j<n; ++j) {
            if (marker.allele(j).equals(allele)) {
                return j;
            }
        }
        return -1;
    }

    private static final class ScoreFileData {

        private final List<ScoreFileRecord> records;
        private final Map<ScoreKey, ScoreFileRecord> recordMap;
        private final Map<CoordKey, Boolean> coordMap;

        private ScoreFileData(List<ScoreFileRecord> records,
                Map<ScoreKey, ScoreFileRecord> recordMap,
                Map<CoordKey, Boolean> coordMap) {
            this.records = records;
            this.recordMap = recordMap;
            this.coordMap = coordMap;
        }

        private int size() {
            return records.size();
        }

        private ScoreFileRecord record(int index) {
            return records.get(index);
        }

        private ScoreFileRecord record(ScoreKey key) {
            return recordMap.get(key);
        }

        private boolean hasCoord(CoordKey coord) {
            return coordMap.containsKey(coord);
        }
    }

    private static final class ScoreFileRecord {

        private final int index;
        private final ScoreKey key;
        private final String allele;
        private final float frequency;
        private final boolean correlated;

        private ScoreFileRecord(int index, ScoreKey key, String allele,
                float frequency, boolean correlated) {
            this.index = index;
            this.key = key;
            this.allele = allele;
            this.frequency = frequency;
            this.correlated = correlated;
        }

        private int index() {
            return index;
        }

        private ScoreKey key() {
            return key;
        }

        private String allele() {
            return allele;
        }

        private float frequency() {
            return frequency;
        }

        private boolean correlated() {
            return correlated;
        }
    }

    private static final class ScoreKey {

        private final String chrom;
        private final int pos;
        private final String ref;
        private final String alt;
        private final CoordKey coord;

        private ScoreKey(String chrom, int pos, String ref, String alt) {
            this.chrom = chrom;
            this.pos = pos;
            this.ref = ref;
            this.alt = alt;
            this.coord = new CoordKey(chrom, pos);
        }

        private String chrom() {
            return chrom;
        }

        private int pos() {
            return pos;
        }

        private CoordKey coord() {
            return coord;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + chrom.hashCode();
            hash = 29 * hash + pos;
            hash = 29 * hash + ref.hashCode();
            hash = 29 * hash + alt.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this==obj) {
                return true;
            }
            if (!(obj instanceof ScoreKey)) {
                return false;
            }
            ScoreKey other = (ScoreKey) obj;
            return pos==other.pos
                    && chrom.equals(other.chrom)
                    && ref.equals(other.ref)
                    && alt.equals(other.alt);
        }

        @Override
        public String toString() {
            return chrom + Const.colon + pos + " REF=" + ref + " ALT=" + alt;
        }
    }

    private static final class CoordKey {

        private final String chrom;
        private final int pos;

        private CoordKey(String chrom, int pos) {
            this.chrom = chrom;
            this.pos = pos;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + chrom.hashCode();
            hash = 29 * hash + pos;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this==obj) {
                return true;
            }
            if (!(obj instanceof CoordKey)) {
                return false;
            }
            CoordKey other = (CoordKey) obj;
            return pos==other.pos && chrom.equals(other.chrom);
        }
    }
}
