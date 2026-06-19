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

package ibdseq;

import vcf.VcfMarkerData;

/**
 * Class <code>IbdScores</code> stored pre-computed IBD and HBD scores got
 * each marker and allele dose combination.
 * Class <code>IbdScores</code> is immutable.
 *
 * @author Brian L Browning
 */
public class IbdScores {

    private final int nMarkers;
    private final double[][] ibdScores; // marker index is dim 2
    private final double[][] hbdScores; // marker index is dim 2

    /*
     * Marker-major, branchless score lookup tables used by the optimized
     * detection scan. Doses are 0,1,2 with missing encoded as 3.
     *   ibdCell[(j<<4) + (doseA<<2) + doseB] == ibdScore(doseA, doseB, j),
     *     or 0 if either dose is missing, or 0 if marker j is LD-correlated and
     *     the dose pair is not an opposite-homozygote (exclusion-only scoring).
     *   hbdCell[(j<<2) + dose] == hbdScore(dose, j), or 0 if dose is missing,
     *     or 0 if marker j is LD-correlated and dose != 1.
     * Cells are doubles so that <code>float sum += cell</code> reproduces the
     * exact arithmetic of the original <code>float sum += (double) score</code>.
     */
    private final double[] ibdCell;
    private final double[] hbdCell;

    private static final int N_DOSES = 3;
    private static final int[][] dosePairIndex = dosePair2Index();

    private static int[][] dosePair2Index() {
        int[][] index = new int[N_DOSES][N_DOSES];
        for (int dose1=0; dose1<N_DOSES; ++dose1) {
            for (int dose2=0; dose2<N_DOSES; ++dose2) {
                if (isDiscordantHomozygote(dose1, dose2)) {
                    index[dose1][dose2] = 2*(N_DOSES-1) + 1;
                }
                else {
                    index[dose1][dose2] = dose1 + dose2;
                }
            }
        }
        return index;
    }

    private static boolean isDiscordantHomozygote(int dose1, int dose2) {
        return (dose1==0 && dose2==2) || (dose1==2 && dose2==0);
    }

    /**
     * Constructs an <code>IbdScores</code> instance for the speccified
     * HBD/IBD scorer and genotyep data.
     * @param scorer the HBD/IBD scorer.
     * @param vcfData the genotype data.
     *
     * @throws NullPointerException if
     * <code>scorer==null || vcfData==null</code>.
     */
    public IbdScores(IbdScorer scorer, VcfMarkerData vcfData) {
        this.nMarkers = vcfData.nMarkers();
        this.ibdScores = getIbdScores(scorer, vcfData);
        this.hbdScores = getHbdScores(scorer, vcfData);
        this.ibdCell = buildIbdCell(ibdScores, vcfData);
        this.hbdCell = buildHbdCell(hbdScores, vcfData);
    }

    /*
     * Folds the per-marker IBD scores, missing-genotype handling, and the
     * LD-correlated exclusion-only rule into a flat marker-major table, so the
     * detection scan is a single branchless array lookup per marker.
     * Reproduces ProduceIbd.score() for cross-sample pairs exactly.
     */
    private static double[] buildIbdCell(double[][] ibdScores,
            VcfMarkerData data) {
        int n = data.nMarkers();
        double[] cell = new double[n << 4];
        for (int j=0; j<n; ++j) {
            boolean isCor = data.isCorrelated(j);
            int base = j << 4;
            for (int dA=0; dA<N_DOSES; ++dA) {
                for (int dB=0; dB<N_DOSES; ++dB) {
                    boolean notIbs = (dA==0 && dB==2) || (dA==2 && dB==0);
                    if (notIbs || isCor==false) {
                        cell[base + (dA<<2) + dB] =
                                ibdScores[dosePairIndex[dA][dB]][j];
                    }
                }
            }
        }
        return cell;
    }

    /*
     * Folds the per-marker HBD scores, missing-genotype handling, and the
     * LD-correlated exclusion-only rule into a flat marker-major table.
     * Reproduces ProduceIbd.score() for self pairs exactly.
     */
    private static double[] buildHbdCell(double[][] hbdScores,
            VcfMarkerData data) {
        int n = data.nMarkers();
        double[] cell = new double[n << 2];
        for (int j=0; j<n; ++j) {
            boolean isCor = data.isCorrelated(j);
            int base = j << 2;
            for (int dose=0; dose<N_DOSES; ++dose) {
                if (isCor==false || dose==1) {
                    cell[base + dose] = hbdScores[dose][j];
                }
            }
        }
        return cell;
    }

    /**
     * Returns the flat marker-major IBD score table, indexed by
     * <code>(marker&lt;&lt;4) + (doseA&lt;&lt;2) + doseB</code> with doses in
     * 0,1,2 and missing encoded as 3.
     * @return the flat marker-major IBD score table.
     */
    public double[] ibdCell() {
        return ibdCell;
    }

    /**
     * Returns the flat marker-major HBD score table, indexed by
     * <code>(marker&lt;&lt;2) + dose</code> with dose in 0,1,2 and missing
     * encoded as 3.
     * @return the flat marker-major HBD score table.
     */
    public double[] hbdCell() {
        return hbdCell;
    }

    private static double[][] getIbdScores(IbdScorer scorer, VcfMarkerData data) {
        int nIndices = 6;
        int nMarkers = data.nMarkers();
        double[][] ibdScores = new double[nIndices][nMarkers];
        for (int j=0; j<nMarkers; ++j) {
            double maf = maf(data, j);
            for (int dose1=0; dose1<N_DOSES; ++dose1) {
                for (int dose2=dose1; dose2<N_DOSES; ++dose2) {
                    int index = dosePairIndex[dose1][dose2];
                    ibdScores[index][j] = scorer.ibdScore(dose1, dose2, maf);
                }
            }
        }
        return ibdScores;
    }

    private static double[][] getHbdScores(IbdScorer scorer, VcfMarkerData data) {
        int nMarkers = data.nMarkers();
        double[][] hbdScores = new double[N_DOSES][nMarkers];
        for (int j=0; j<nMarkers; ++j) {
            double maf = maf(data, j);
            for (int dose=0; dose<N_DOSES; ++dose) {
                hbdScores[dose][j] = scorer.hbdScore(dose, maf);
            }
        }
        return hbdScores;
    }

    private static double maf(VcfMarkerData data, int marker) {
        return data.scoreFrequency(marker);
    }

    /**
     * Returns the number of markers.
     * @return the number of markers;
     */
    public int nMarkers() {
        return nMarkers;
    }

    /**
     * Returns the HBD score for the specified minor allele dose and marker.
     * @param dose the minor allele dose.
     * @param marker the marker index.
     * @return the HBD score for the specified minor allele dose and marker.
     * @throws IndexOutOfBoundsException if
     * <code>dose<0 || dose &ge IbdScores.N_DOSES</code>, or if
     * <code>marker < 0 || marker &ge; this.nMarkers()</code>.
     */
    public double hbdScore(int dose, int marker) {
        return hbdScores[dose][marker];
    }

    /**
     * Returns the IBD score for the specified minor allele doses and marker.
     * @param dose the minor allele dose for the first sample.
     * @param dose the minor allele dose for the second sample.
     * @param marker the marker index.
     * @return the IBD score for the specified minor allele doses and marker.
     *
     * @throws IndexOutOfBoundsException if
     * <code>dose1<0 || dose1 &ge IbdScores.N_DOSES</code>, if
     * <code>dose2<0 || dose2 &ge IbdScores.N_DOSES</code>, or if
     * <code>marker < 0 || marker &ge; this.nMarkers()</code>.
     */
    public double ibdScore(int dose1, int dose2, int marker) {
        int index = dosePairIndex[dose1][dose2];
        return ibdScores[index][marker];
    }
}
