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

/**
 * Class <code>ScoreMarkerData</code> stores the allele and frequency used
 * for per-marker IBD/HBD scoring.  Class <code>ScoreMarkerData</code> is
 * immutable.
 */
public final class ScoreMarkerData {

    private final byte allele;
    private final float frequency;

    /**
     * Constructs a <code>ScoreMarkerData</code> instance.
     * @param allele the allele used for allele-dose scoring.
     * @param frequency the allele frequency used for IBD/HBD scores.
     * @throws IllegalArgumentException if
     * <code>allele < 0 || frequency <= 0.0f || frequency >= 1.0f</code>,
     * or if <code>frequency</code> is infinite or NaN.
     */
    public ScoreMarkerData(byte allele, float frequency) {
        if (allele < 0) {
            throw new IllegalArgumentException("allele: " + allele);
        }
        if (frequency <= 0.0f || frequency >= 1.0f
                || Float.isInfinite(frequency) || Float.isNaN(frequency)) {
            throw new IllegalArgumentException("frequency: " + frequency);
        }
        this.allele = allele;
        this.frequency = frequency;
    }

    /**
     * Returns the allele used for allele-dose scoring.
     * @return the allele used for allele-dose scoring.
     */
    public byte allele() {
        return allele;
    }

    /**
     * Returns the allele frequency used for IBD/HBD scores.
     * @return the allele frequency used for IBD/HBD scores.
     */
    public float frequency() {
        return frequency;
    }
}
