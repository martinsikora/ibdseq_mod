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
package blbutil;

/* Code review on 13 Dec 2012 */

/*
 * Class <code>Const</code> provides public static fields for global
 * string and character constants.
 *
 * @author Brian L Browning
 */
public class Const {

    public static final String nl = System.getProperty("line.separator");
    public static final int giga = 1000000000;
    public static final int mega = 1000000;
    public static final String EMPTY_STRING = "";
    public static final String MISSING_DATA = ".";
    public static final char colon = ':';
    public static final char hyphen = '-';
    public static final char tab = '\t';
    public static final char semicolon = ';';
    public static final char comma = ',';
    public static final char period = '.';
    public static final char phasedSep = '|';
    public static final char unphasedSep = '/';
}
