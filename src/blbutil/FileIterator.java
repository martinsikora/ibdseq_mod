/*
 * Copyright 2009 Brian L. Browning
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

import java.io.File;

/* Code review on 13 Dec 2012 */

/**
 * An iterator for data in a file.  If an IOExceptions is thrown while
 * reading a file, the IOException is trapped, an appropriate error message
 * is written to standard out, and the Java Virtual Machine is
 * terminated.  The <code>remove</code> method is unsupported and throws an
 * <code>UnsupportedOperationException</code>.
 * <p/>
 *
 * When the <code>FileIterator</code> instance is no longer needed
 * the <code>close()</code> method should be invoked to free up
 * system resources.  After calling <code>close()</code>, invoking
 * <code>hasNext()</code> returns <code>false</code>, and invoking
 * <code>next()</code> will throw a <code>NoSuchElementException</code>.
 *
 * @param <E> the type of elements returned by this file iterator.
 *
 * @author Brian L Browning
 */
public interface FileIterator<E> extends java.util.Iterator<E> {

    /**
     * Returns the file from which data are read, or returns
     * <code>null</code> if no file source is known.
     * @return the file from which data are read, or returns
     * <code>null</code> if no file source is known.
     */
    File file();

    /**
     * Closes the file iterator.  After invoking
     * <code>close</code>, further invocations of
     * <code>close</code> have no effect.
     */
    public void close();

    /**
     * Returns a string representation of <code>this</code>.  The exact
     * details of the representation are unspecified and subject to change.
     *
     * @return a string representation of <code>this</code>
     */
    @Override
    public String toString();
}
