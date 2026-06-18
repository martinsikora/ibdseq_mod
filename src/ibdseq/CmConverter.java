/*
 * Copyright 2012 Brian L. Browning
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
package ibdseq;

import blbutil.Const;
import blbutil.FileIterator;
import blbutil.InputStreamIterator;
import blbutil.StringUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Converts base-pair coordinates to centiMorgan coordinates and bins segment
 * lengths.
 */
final class CmConverter {

    private static final double BASES_PER_MB = 1000000.0;
    private static final String POS_COLUMN = "Position(bp)";
    private static final String CM_COLUMN = "Map(cM)";

    private final double[] positions;
    private final double[] centimorgans;
    private final double[] bins;
    private final String[] binLabels;
    private final double cmPerMb;

    private CmConverter(double[] positions, double[] centimorgans,
            double[] bins, double cmPerMb) {
        this.positions = positions;
        this.centimorgans = centimorgans;
        this.bins = bins;
        this.binLabels = binLabels(bins);
        this.cmPerMb = cmPerMb;
    }

    static CmConverter create(File mapFile, String binsArg, double cmPerMb) {
        double[] parsedBins = parseBins(binsArg);
        checkFinite("cmpermb", cmPerMb);
        if (cmPerMb < 0.0) {
            throw new IllegalArgumentException("cmpermb must be >= 0: "
                    + cmPerMb);
        }
        if (mapFile==null) {
            return new CmConverter(null, null, parsedBins, cmPerMb);
        }
        GeneticMapData mapData = readMap(mapFile);
        return new CmConverter(mapData.positions, mapData.centimorgans,
                parsedBins, cmPerMb);
    }

    double cm(int pos) {
        if (positions==null) {
            return (pos / BASES_PER_MB) * cmPerMb;
        }
        double x = pos;
        if (x <= positions[0]) {
            return centimorgans[0];
        }
        int last = positions.length - 1;
        if (x >= positions[last]) {
            return centimorgans[last];
        }
        int index = Arrays.binarySearch(positions, x);
        if (index >= 0) {
            return centimorgans[index];
        }
        int hi = -index - 1;
        int lo = hi - 1;
        double scale = (x - positions[lo]) / (positions[hi] - positions[lo]);
        return centimorgans[lo]
                + scale*(centimorgans[hi] - centimorgans[lo]);
    }

    String bin(double lengthCm) {
        checkFinite("segment cM length", lengthCm);
        int hi = Arrays.binarySearch(bins, lengthCm);
        if (hi >= 0) {
            ++hi;
        }
        else {
            hi = -hi - 1;
        }
        if (hi <= 0 || hi >= bins.length) {
            String s = "Segment cM length outside bins: " + lengthCm
                    + ". Increase bins upper bound or adjust map/cmpermb.";
            throw new IllegalArgumentException(s);
        }
        return binLabels[hi - 1] + "-" + binLabels[hi];
    }

    private static String[] binLabels(double[] bins) {
        String[] labels = new String[bins.length];
        for (int j=0; j<bins.length; ++j) {
            labels[j] = Double.toString(bins[j]);
        }
        return labels;
    }

    private static double[] parseBins(String binsArg) {
        String[] fields = StringUtil.getFields(binsArg, Const.comma);
        if (fields.length < 2) {
            throw new IllegalArgumentException(
                    "bins must contain at least two comma-delimited values");
        }
        double[] parsed = new double[fields.length];
        for (int j=0; j<fields.length; ++j) {
            parsed[j] = parseDouble("bins", fields[j].trim());
            if (j>0 && parsed[j] <= parsed[j - 1]) {
                String s = "bins values must be strictly increasing: "
                        + binsArg;
                throw new IllegalArgumentException(s);
            }
        }
        return parsed;
    }

    private static GeneticMapData readMap(File mapFile) {
        FileIterator<String> it = InputStreamIterator.fromGzipFile(mapFile);
        if (it.hasNext()==false) {
            throw new IllegalArgumentException("Empty map file: " + mapFile);
        }
        String header = it.next();
        String[] headerFields = StringUtil.getFields(header, Const.tab);
        int posColumn = columnIndex(headerFields, POS_COLUMN, mapFile);
        int cmColumn = columnIndex(headerFields, CM_COLUMN, mapFile);
        int maxColumn = Math.max(posColumn, cmColumn);
        ArrayList<Double> positions = new ArrayList<Double>();
        ArrayList<Double> centimorgans = new ArrayList<Double>();
        int lineNumber = 1;
        while (it.hasNext()) {
            String line = it.next();
            ++lineNumber;
            if (line.trim().length()==0) {
                continue;
            }
            String[] fields = StringUtil.getFields(line, Const.tab);
            if (fields.length <= maxColumn) {
                String s = "Missing map field on line " + lineNumber
                        + " of " + mapFile;
                throw new IllegalArgumentException(s);
            }
            double pos = parseDouble(POS_COLUMN, fields[posColumn].trim());
            double cm = parseDouble(CM_COLUMN, fields[cmColumn].trim());
            if (positions.isEmpty()==false) {
                double previousPos = positions.get(positions.size() - 1);
                double previousCm = centimorgans.get(centimorgans.size() - 1);
                if (pos <= previousPos) {
                    String s = "Map positions must be strictly increasing on "
                            + "line " + lineNumber + " of " + mapFile;
                    throw new IllegalArgumentException(s);
                }
                if (cm < previousCm) {
                    String s = "Map cM values must be nondecreasing on line "
                            + lineNumber + " of " + mapFile;
                    throw new IllegalArgumentException(s);
                }
            }
            positions.add(pos);
            centimorgans.add(cm);
        }
        it.close();
        if (positions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Map file contains no map rows: " + mapFile);
        }
        return new GeneticMapData(toArray(positions), toArray(centimorgans));
    }

    private static int columnIndex(String[] headerFields, String column,
            File mapFile) {
        for (int j=0; j<headerFields.length; ++j) {
            if (headerFields[j].trim().equals(column)) {
                return j;
            }
        }
        String s = "Map file missing required column " + column + ": "
                + mapFile;
        throw new IllegalArgumentException(s);
    }

    private static double parseDouble(String label, String value) {
        try {
            double d = Double.parseDouble(value);
            checkFinite(label, d);
            return d;
        }
        catch (NumberFormatException e) {
            String s = "Invalid " + label + " value: " + value;
            throw new IllegalArgumentException(s);
        }
    }

    private static void checkFinite(String label, double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            String s = label + " must be finite: " + value;
            throw new IllegalArgumentException(s);
        }
    }

    private static double[] toArray(ArrayList<Double> values) {
        double[] array = new double[values.size()];
        for (int j=0; j<array.length; ++j) {
            array[j] = values.get(j);
        }
        return array;
    }

    private static final class GeneticMapData {

        private final double[] positions;
        private final double[] centimorgans;

        private GeneticMapData(double[] positions, double[] centimorgans) {
            this.positions = positions;
            this.centimorgans = centimorgans;
        }
    }
}
