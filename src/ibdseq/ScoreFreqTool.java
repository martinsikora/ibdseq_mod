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

import blbutil.Const;
import blbutil.FileIterator;
import blbutil.FileUtil;
import blbutil.InputStreamIterator;
import blbutil.SampleFileIterator;
import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import main.Marker;
import vcf.LowMemGT;
import vcf.MarkerData;
import vcf.VcfIterator;
import vcf.VcfMarker;
import vcf.VcfRecord;

/**
 * Class <code>ScoreFreqTool</code> writes the <code>.scorefreq</code> file
 * that a full IBDSeq run would produce, without performing IBD/HBD detection.
 *
 * <p>It streams the VCF one marker at a time (constant memory) and reproduces
 * the exact {@code readData}/{@code MarkerData} minor-allele and allele
 * frequency computation used by a full IBDSeq run. Every marker passing the
 * {@code minalleles} filter is emitted, in genomic order. Each marker is
 * flagged in the {@code LD_PRUNED} column: {@code 1} if it was removed by the
 * reference run's LD pruning (i.e. present in the supplied {@code prunedmarkers}
 * list), {@code 0} otherwise. Retained markers ({@code LD_PRUNED=0}) supply
 * full IBD/HBD scores; LD-pruned markers ({@code LD_PRUNED=1}) are used for
 * exclusion-only scoring (opposite homozygotes), matching stock IBDSeq.</p>
 *
 * <p>Only the first chromosome in the VCF is processed, matching the
 * single-chromosome behavior of a full IBDSeq run.</p>
 *
 * <p>Arguments (key=value):</p>
 * <ul>
 *   <li>{@code gt=<file>}             input VCF (single chromosome)</li>
 *   <li>{@code out=<prefix>}          output prefix; writes {@code <prefix>.scorefreq}</li>
 *   <li>{@code prunedmarkers=<file>}  gzip list of marker IDs removed by the
 *       reference run's LD pruning (column 3 of the {@code .r2.filtered} file);
 *       omit to flag all markers as retained</li>
 *   <li>{@code minalleles=<int>}      minimum minor allele count (default 2)</li>
 * </ul>
 */
public final class ScoreFreqTool {

    private ScoreFreqTool() {}

    public static void main(String[] args) {
        File gt = null;
        File prunedMarkers = null;
        String out = null;
        int minAlleles = 2;
        for (String arg : args) {
            int i = arg.indexOf('=');
            if (i < 0) {
                throw new IllegalArgumentException("Invalid argument: " + arg);
            }
            String key = arg.substring(0, i);
            String value = arg.substring(i + 1);
            if (key.equals("gt")) {
                gt = new File(value);
            }
            else if (key.equals("out")) {
                out = value;
            }
            else if (key.equals("prunedmarkers")) {
                prunedMarkers = new File(value);
            }
            else if (key.equals("minalleles")) {
                minAlleles = Integer.parseInt(value);
            }
            else {
                throw new IllegalArgumentException("Unknown argument: " + key);
            }
        }
        if (gt == null || out == null) {
            throw new IllegalArgumentException(
                    "Usage: gt=<vcf> out=<prefix> [prunedmarkers=<file>] [minalleles=<int>]");
        }

        Set<String> prunedIds = readIds(prunedMarkers);
        writeScoreFrequencies(gt, out, minAlleles, prunedIds);
    }

    private static Set<String> readIds(File idFile) {
        Set<String> ids = new HashSet<String>(100000);
        if (idFile != null) {
            FileIterator<String> it = InputStreamIterator.fromGzipFile(idFile);
            while (it.hasNext()) {
                ids.add(it.next().trim());
            }
            it.close();
        }
        return ids;
    }

    /*
     * Returns true if the marker was removed by the reference run's LD pruning.
     * Mirrors ExcludeIdFilter matching: any of the marker's IDs, or "chrom:pos".
     */
    private static boolean isPruned(Marker marker, Set<String> prunedIds) {
        for (int j=0, n=marker.nIds(); j<n; ++j) {
            if (prunedIds.contains(marker.id(j))) {
                return true;
            }
        }
        return prunedIds.contains(marker.chrom() + ':' + marker.pos());
    }

    /*
     * Streams the first chromosome of the VCF, emitting one row per marker that
     * passes the minor-allele-count filter, in genomic order. Output format:
     * CHROM POS ID REF ALT ALLELE FREQ LD_PRUNED. ALLELE is the minor allele
     * and FREQ its frequency, computed exactly as MarkerData/readData do.
     */
    private static void writeScoreFrequencies(File gt, String outPrefix,
            int minAlleles, Set<String> prunedIds) {
        boolean usePhase = false;
        SampleFileIterator<VcfRecord> it = new VcfIterator(gt);
        PrintWriter out = FileUtil.printWriter(new File(outPrefix + ".scorefreq"));
        out.print("CHROM");
        out.print(Const.tab);
        out.print("POS");
        out.print(Const.tab);
        out.print("ID");
        out.print(Const.tab);
        out.print("REF");
        out.print(Const.tab);
        out.print("ALT");
        out.print(Const.tab);
        out.print("ALLELE");
        out.print(Const.tab);
        out.print("FREQ");
        out.print(Const.tab);
        out.println("LD_PRUNED");
        int chromIndex = -1;
        while (it.hasNext()) {
            VcfMarker obsData = new LowMemGT(it.next(), usePhase);
            Marker marker = obsData.marker();
            if (chromIndex == -1) {
                chromIndex = marker.chromIndex();
            }
            if (marker.chromIndex() != chromIndex) {
                break;
            }
            MarkerData md = new MarkerData(obsData);
            byte minor = md.minorAllele();
            if (minor != -1 && md.alleleCount(minor) >= minAlleles) {
                out.print(marker);
                out.print(Const.tab);
                out.print(marker.allele(minor));
                out.print(Const.tab);
                out.print(md.alleleFrequency(minor));
                out.print(Const.tab);
                out.println(isPruned(marker, prunedIds) ? 1 : 0);
            }
        }
        it.close();
        out.close();
    }
}
