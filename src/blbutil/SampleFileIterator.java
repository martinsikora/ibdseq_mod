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
package blbutil;

import beagleutil.Samples;

/* Code review on 26 Sep 2013 */

/**
 *
 * Interface <code>VcfIteratorInterface</code> iterates over VCF records
 * in a VCF file.
 * @author Brian L. Browning <browning@uw.edu>
 */
public interface SampleFileIterator<E> extends FileIterator<E> {

    /**
     * Returns the samples corresponding to the data returned by the
     * <code>next()</code> method.
     * @return the samples corresponding to the data returned by the
     * <code>next()</code> method.
     */
    Samples samples();
}
