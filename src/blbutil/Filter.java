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

/* Code Review on 10 May 2013 */

/**
 * A filter.
 */
public interface Filter<E> {

    /**
     * Returns <code>true</code> if the specified object is
     * accepted and returns <code>false</code> otherwise.
     * @param e the object to be filtered.
     * @return <code>true</code> if the specified object is
     * accepted and returns <code>false</code> otherwise.
     * @throws NullPointerException if <code>e==null</code>.
     */
    public abstract boolean accept(E e);
}
