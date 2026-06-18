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
import blbutil.Utilities;
import blbutil.Validate;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Class IbdSeqPar represents the command line parameters and default
 * parameters for an ibdseq analysis.  Class <code>IbdSeqPr</code> is
 * thread-safe.
 *
 * @author Brian L Browning
 */
public final class IbdSeqPar {

    private static final String nl = Const.nl;
    private static final int IMAX = Integer.MAX_VALUE;
    private static final int IMIN = Integer.MIN_VALUE;
    private static final long LMIN = Long.MIN_VALUE;
    private static final long LMAX = Long.MAX_VALUE;
    private static final float FMIN = Float.MIN_VALUE;
    private static final float FMAX = Float.MAX_VALUE;

    // data input/output parameters
    private final File gt;
    private final String out;
    private final File excludemarkers;
    private final File excludesamples;
    private final File focussamples;
    private final File scorefreq;
    private final String chrom;
    private final int minalleles;

    // algorithm parameters
    private final float ibdlod;
    private final float ibdtrim;
    private final float errormax;
    private final float errorprop;
    private final int r2window;
    private final float r2max;
    private final int nthreads;

    // undocumented parameters
    private final boolean debug;

    /**
     * Constructs an <code>IbdSeqPar</code> object that represents the
     * command line arguments for the ibdseq program.  See the
     * <code>usage()</code> method for a description of the command line
     * parameters.   The constructor exists with an error message if
     * a command line parameter name is unrecognized.
     *
     * @param args the command line arguments.
     * @throws IllegalArgumentException if the command line parameters
     * represented by <code>args</code> are incorrectly specified.
    */
    public IbdSeqPar(String[] args) {
        readOptions(args);
        Map<String, String[]> argsMap = Utilities.argsToMultimap(args, '=');

        // data input/output parameters
        gt = Validate.getFile(Validate.sString("gt", argsMap, true, null, null));
        out = Validate.sString("out", argsMap, true, null, null);
        excludesamples = Validate.getFile(Validate.sString("excludesamples",
                argsMap, false, null, null));
        excludemarkers = Validate.getFile(Validate.sString("excludemarkers",
                argsMap, false, null, null));
        focussamples = Validate.getFile(Validate.sString("focussamples",
                argsMap, false, null, null));
        scorefreq = Validate.getFile(Validate.sString("scorefreq",
                argsMap, false, null, null));
        chrom = Validate.sString("chrom", argsMap, false, null, null);
        minalleles = Validate.sInt("minalleles", argsMap, false, 2, 2, Integer.MAX_VALUE);

        // algorithm parameters
        ibdlod = Validate.sFloat("ibdlod", argsMap, false, 3.0f, FMIN, FMAX);
        ibdtrim = Validate.sFloat("ibdtrim", argsMap, false, 0.0f, 0.0f, FMAX);
        errormax = Validate.sFloat("errormax", argsMap, false, 0.001f, 0.0f, 1.0f);
        errorprop = Validate.sFloat("errorprop", argsMap, false, 0.25f, 0.0f, FMAX);
        r2window = Validate.sInt("r2window", argsMap, false, 500, 0, IMAX);
        r2max = Validate.sFloat("r2max", argsMap, false, 0.15f, 0.0f, 1.0f);
        nthreads = Validate.sInt("nthreads", argsMap, false, 1, 1, IMAX);

        // undocumented parameters
        debug = Validate.sBoolean("debug", argsMap, false, false);

        Validate.confirmEmptyMap(argsMap);
    }

    /*
     * Verifies that there are no options.  Options are command
     * arguments that do not contain an equal sign ("=").
     * Currently, no options are permitted on the
     * command line.  This prohibition of command line
     * options could be removed in the future.
     *
     * @param args the command line arguments.
     */
    private void readOptions(String[] args) {
        List<String> argsList = Utilities.argsToList(args, '=');
        if (argsList.size() > 0) {
            printErrorAndExit("unrecognized options: " + argsList);
        }
    }

    /*
     * Prints the <code>this.err</code> field followed by the
     * specified message to standard out and exits the java virtual machine.
     *
     * @param message a message to be written to standard err.
     */
    private static void printErrorAndExit(String message) {
        System.err.print(usage());
        System.err.println(message);
        System.err.println();
        System.err.flush();
        System.exit(0);
    }

    /**
     * Returns a string describing the command line arguments.
     * The format of the returned string is unspecified and subject to change.
     * @return a string describing the command line arguments.
     */
    public static String usage() {
        return  IbdSeqMain.version + nl
                + "usage: java -jar ibdseq.jar [parameters]" + nl
                + nl
                + "Data Parameters: " + nl
                + "  gt=<VCF file with GT field>                       (required)" + nl
                + "  out=<output file prefix>                          (required)" + nl
                + "  excludesamples=<excluded samples file>            (optional)" + nl
                + "  excludemarkers=<excluded markers file>            (optional)" + nl
                + "  focussamples=<focus samples file>                 (optional)" + nl
                + "  scorefreq=<retained marker score file>            (optional)" + nl
                + "  chrom=<[chrom]:[start]-[end]>                     (optional)" + nl
                + "  minalleles=<minimum minor allele count>           (default=2)" + nl + nl

                + "Algorithm Parameters: " + nl
                + "  ibdlod=<min LOD score for reported IBD>           (default=3.0)" + nl
                + "  ibdtrim=<LOD score to trim from segment ends>     (default=0.3)" + nl
                + "  errormax=<max allele error rate>                  (default=0.001)" + nl
                + "  errorprop=<allele error as proportion of MAF>     (default=0.25)" + nl
                + "  r2window=<window-size when checking marker R2>    (default=500)"  + nl
                + "  r2max=<max R2 permitted between markers>          (default=0.15)" + nl
                + "  nthreads=<number of threads to use>               (default=1)" + nl;
    }

    // data input/output parameters

    /**
     * Returns the gt parameter.
     * @return the gt parameter.
     */
    public File gt() {
        return gt;
    }

    /**
     * Returns the out parameter.
     * @return the out parameter.
     */
    public String out() {
        return out;
    }

    /**
     * Returns the excludesamples parameter or <code>null</code>
     * if no excludesamples parameter was specified.
     *
     * @return the excludesamples parameter or <code>null</code>
     * if no excludesamples parameter was specified.
     */
    public File excludesamples() {
        return excludesamples;
    }

    /**
     * Returns the excludemarkers parameter or <code>null</code>
     * if no excludemarkers parameter was specified.
     *
     * @return the excludemarkers parameter or <code>null</code>
     * if no excludemarkers parameter was specified.
     */
    public File excludemarkers() {
        return excludemarkers;
    }

    /**
     * Returns the focussamples parameter or <code>null</code>
     * if no focussamples parameter was specified.
     *
     * @return the focussamples parameter or <code>null</code>
     * if no focussamples parameter was specified.
     */
    public File focussamples() {
        return focussamples;
    }

    /**
     * Returns the scorefreq parameter or <code>null</code>
     * if no scorefreq parameter was specified.
     *
     * @return the scorefreq parameter or <code>null</code>
     * if no scorefreq parameter was specified.
     */
    public File scorefreq() {
        return scorefreq;
    }

    /**
     * Returns the chrom parameter or <code>null</code>
     * if no chrom parameter was specified.
     *
     * @return the chrom parameter or <code>null</code>
     * if no chrom parameter was specified.
     */
    public String chrom() {
        return chrom;
    }

    /**
     * Returns the minalleles parameter.
     * @return the minalleles parameter
     */
    public int minalleles() {
        return minalleles;
    }

    // algorithm parameters

    /**
     * Returns the ibdlod parameter.
     * @return the ibdlod parameter
     */
    public float ibdlod() {
        return ibdlod;
    }

    /**
     * Returns the ibdtrim parameter.
     * @return the ibdtrim parameter
     */
    public float ibdtrim() {
        return ibdtrim;
    }

    /**
     * Returns the errormax parameter.
     * @return the errormax parameter.
     */
    public float errormax() {
        return errormax;
    }

    /**
     * Returns the errorprop parameter.
     * @return the errorprop parameter.
     */
    public float errorprop() {
        return errorprop;
    }

    /**
     * Returns the r2window parameter.
     * @return the r2window parameter
     */
    public int r2window() {
        return r2window;
    }

    /**
     * Returns the r2max parameter.
     * @return the r2max parameter
     */
    public float r2max() {
        return r2max;
    }

    /**
     * Returns the nthreads parameter.
     * @return the nthreads parameter
     */
    public int nthreads() {
        return nthreads;
    }

    // undocumented parameters

    /**
     * Returns the debug parameter.
     * @return the debug parameter
     */
    public boolean debug() {
        return debug;
    }
}
