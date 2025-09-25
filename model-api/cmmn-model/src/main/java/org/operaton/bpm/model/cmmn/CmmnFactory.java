/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.model.cmmn;

/**
 * The factory for creating {@link Cmmn} instances.
 *
 * <p>
 * Implementations of this interface are discovered using Java's {@link java.util.ServiceLoader} mechanism.
 * To provide a custom implementation, create a class that implements {@code CmmnFactory} and
 * register it by adding its fully qualified class name to a file named
 * {@code META-INF/services/org.operaton.bpm.model.cmmn.CmmnFactory} in your JAR.
 * </p>
 *
 * <p>
 * Example:
 * <pre>
 * // In your implementation JAR:
 * // File: META-INF/services/org.operaton.bpm.model.cmmn.CmmnFactory
 * com.example.MyCustomCmmnFactory
 * </pre>
 * </p>
 *
 * <p>
 * To obtain an instance, use:
 * <pre>
 * ServiceLoader&lt;CmmnFactory&gt; loader = ServiceLoader.load(CmmnFactory.class);
 * for (CmmnFactory factory : loader) {
 *   Cmmn cmmn = factory.newInstance();
 *   // use cmmn
 * }
 * </pre>
 * </p>
 */
public interface CmmnFactory {
  Cmmn newInstance();
}
