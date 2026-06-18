/*
 * Copyright 2013 Brian L. Browning
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

/* Code review on 04 Dec 2013 */

/**
 * Class <code>VcfMetaInfo</code> represents a VCF meta-information
 * line. Class <code>VcfMetaInfo</code> is immutable.
 */
public final class VcfMetaInfo {

    public static final String PREFIX = "##";
    private static final char EQUAL_SIGN = '=';

    private final String line;
    private final String key;
    private final String value;

    /**
     * Constructs a <code>VcfMetaInfo</code> instance representing
     * the specified VCF meta-information line.
     *
     * @param line a VCF meta-information line.
     *
     * @throws IllegalArgumentException if the specified information line
     * after trimming any beginning and ending white-space does not begin with
     * <code>VcfMetaInfo.PREFIX</code>, and does not contain the
     * <code>VcfMetaInfo.EQUAL_SIGN</code> character.
     *
     * @throws NullPointerException if <code>line==null</code>.
     */
    public VcfMetaInfo(String line) {
        line = line.trim();
        if (line.startsWith(PREFIX)==false) {
            String s = "VCF header line does not start with \"" + PREFIX
                    + "\": " + line;
            throw new IllegalArgumentException(s);
        }
        int index = line.indexOf(EQUAL_SIGN);
        if (index == -1) {
            String s = "VCF meta-information line is missing \"" + EQUAL_SIGN + "\"";
            throw new IllegalArgumentException(s);
        }
        this.line = line;
        this.key = line.substring(2, index);
        this.value = line.substring(index+1);
    }

    /**
     * Returns the identifier.
     * @return the identifier.
     */
    public String key() {
        return key;
    }

    /**
     * Returns the value of the VCF meta-information line.
     *
     * @return the value of the VCF meta-information line.
     */
    public String value() {
        return value;
    }

    /**
     * Returns the VCF meta-information line represented by <code>this</code>.
     *
     * @return the VCF meta-information line represented by <code>this</code>.
     */
    @Override
    public String toString() {
        return line;
    }
}
