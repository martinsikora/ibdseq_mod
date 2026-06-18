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

/**
 * Class <code>IbdScorer</code> calculates per-locus IBD and HBD scores.
 * Class <code>IbdScorer</code> is thread-safe.
 *
 * @author Brian L. Browning <browning@uw.edu>
 */
public final class IbdScorer {

    private final double errorMax;
    private final double errorProp;
    private final double[] maxErrorArray;

    /**
     * Constructs an <code>IbdScorer</code> instance with the specified
     * error rate.
     *
     * @param errorMax the allele error rate.
     * @throws IllegalArgumentException if
     * <code>error < 0.0f || error >= 1.0f || Float.isInfinite(error)
     * || Float.isNaN(error)</code>.
     */
    public IbdScorer(float errorMax, float errorProp) {
        if (errorMax < 0.0f || errorMax >= 1.0f || Float.isInfinite(errorMax)
                || Float.isNaN(errorMax)) {
            String s = "errorMax=" + errorMax;
            throw new IllegalArgumentException(s);
        }
        this.errorMax = errorMax;
        this.errorProp = errorProp;
        this.maxErrorArray = errorArray(errorMax);
    }

    /**
     * Returns the log-transformed IBD score for a pair of individuals.
     *
     * @param dose1 allele dose for individual 1 (0, 1, or 2, or <0 if dose
     * is unknown).
     * @param dose2 allele dose for individual 2 (0, 1, or 2, or <0 if dose
     * is unknown).
     * @param fB the observed allele frequency.
     * @return the log-transformed IBD score for a pair of individuals.
     *
     * @throws IllegalArgumentException if
     * <code>dose1>2 || dose2>2 || p<0.0 || p>1.0</code>
     */
    public double ibdScore(int dose1, int dose2, double fB) {
        checkArgs(dose1, dose2, fB);
        if (dose1 < 0 || dose2 < 0) {
            return 0.0;
        }
        double e = errorRate(fB);
        double pB = estTrueMAF(fB, e);
        double r = ibdLike(dose1, dose2, e, pB) / nullLike(dose1, dose2, fB);
        return Math.log10(r);
    }

    private double nullLike(int dose1, int dose2,  double fB) {
        double fA = 1.0 - fB;
        switch (dose1 + dose2) {
            case 0: return Math.pow(fA, 4);
            case 1: return 4*Math.pow(fA, 3) * fB;
            case 2:
                if (dose1==dose2) {
                    return Math.pow( (2*fA*fB), 2);
                }
                else {
                    return 2*Math.pow( (fA*fB), 2);
                }
            case 3: return 4*fA*Math.pow(fB, 3);
            case 4: return Math.pow(fB, 4);
            default: assert false; return Double.NaN;
        }
    }

    private double ibdLike(int dose1, int dose2, double err, double pB) {
        double pA = 1.0 - pB;
        double[] e = (err==errorMax) ? maxErrorArray : errorArray(err);
        switch (dose1 + dose2) {
            case 0: return e[0]*Math.pow(pA, 3) + 2*e[1]*pA*pA*pB + e[2]*pA*pB;
            case 1: return 2*(e[0]*pA*pA*pB + e[1]*(pA*pB + 2*Math.pow(pA, 3)) + 3*e[2]*pA*pB);
            case 2:
                if (dose1==dose2) {
                    return (e[0]+4*e[1] + 2*e[2])*pA*pB + 4*e[2]*(Math.pow(pA, 3) + Math.pow(pB, 3));
                }
                else {
                    return  2*( (e[1]+e[2]+e[3])*pA*pB + e[2]*(Math.pow(pA, 3) + Math.pow(pB, 3)) );
                }
            case 3: return 2*(e[0]*pA*pB*pB + 2*e[1]*Math.pow(pB, 3)
                    + (e[1] + 3*e[2] + e[3])*pA*pB + 2*e[3]*Math.pow(pA, 3) );
            case 4: return ( e[0]*Math.pow(pB, 3) + 2*e[1]*pA*pB*pB + e[2]*pA*pB + 2*e[3]*pA*pA*pB + e[4]*Math.pow(pA, 3) );
            default: assert false; return Double.NaN;
        }
    }

    private static double[] errorArray(double err) {
        double[] errArray = new double[5];
        errArray[0] = Math.pow((1-err), 4);
        for (int j=1; j<errArray.length; ++j) {
            errArray[j] = errArray[j-1]*err/(1-err);
        }
        return errArray;
    }

    private double errorRate(double fB) {
        double err = errorProp * fB;
        return (err <= errorMax) ? err : errorMax;
    }

    private double estTrueMAF(double fB, double errorRate) {
        return (fB - errorRate) / (1.0 - 2*errorRate);
    }

    /**
     * Returns the log-transformed HBD score for a pair of individuals.
     *
     * @param dose allele dose for the individual 1 (0, 1, or 2, or <0 if dose
     * is unknown).
     * @param fB the minor allele frequency.
     * @return the the log-transformed HBD score for a pair of individuals.
     * @throws IllegalArgumentException if
     * <code>dose>2|| p<0.0 || p>1.0</code>
     */
    public double hbdScore(int dose, double fB) {
        checkProb(fB);
        checkDose(dose);
        if (dose < 0) {
            return 0.0;
        }
        double e = errorRate(fB);
        double fA = 1.0 - fB;
        double pB = estTrueMAF(fB, e);
        double pA = 1.0 - pB;
        switch (dose) {
            case 0: return Math.log10((pA + e*e*pB)/(fA*fA));
            case 1: return Math.log10( e*(1-e)/(fA*fB) );
            case 2: return Math.log10( (e*e*pA + pB)/(fB*fB) );
            default: assert false; return 0.0;
        }
    }

    private void checkArgs(int dose1, int dose2, double p) {
        checkProb(p);
        checkDose(dose1);
        checkDose(dose2);
    }

    private void checkProb(double p) {
        if (p < 0.0 || p > 1.0) {
            throw new IllegalArgumentException("p: " + p);
        }
    }

    private void checkDose(int dose) {
        if (dose > 2) {
            throw new IllegalArgumentException("dose: " + dose);
        }
    }
}
