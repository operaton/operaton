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
package org.operaton.bpm.model.dmn;

import org.operaton.bpm.model.dmn.impl.DmnParser;

/**
 * The factory for creating {@link DmnParser} instances.
 *
 * <p>
 * Implementations of this interface are discovered using Java's {@link java.util.ServiceLoader} mechanism.
 * To provide a custom implementation, create a class that implements {@code DmnParserFactory} and
 * register it by adding its fully qualified class name to a file named
 * {@code META-INF/services/org.operaton.bpm.model.dmn.DmnParserFactory} in your JAR.
 * </p>
 *
 * <p>
 * Example:
 * <pre>
 * // In your implementation JAR:
 * // File: META-INF/services/org.operaton.bpm.model.dmn.DmnParserFactory
 * com.example.MyCustomDmnParserFactory
 * </pre>
 * </p>
 *
 * <p>
 * To obtain an instance, use:
 * <pre>
 * ServiceLoader&lt;DmnParserFactory&gt; loader = ServiceLoader.load(DmnParserFactory.class);
 * for (DmnParserFactory factory : loader) {
 *   DmnParser parser = factory.newInstance();
 *   // use parser
 * }
 * </pre>
 * </p>
 */
public interface DmnParserFactory {
  DmnParser newInstance();
}
