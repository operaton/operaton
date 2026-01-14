/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.spin.spi;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

import org.operaton.spin.impl.logging.SpinLogger;

/**
 * Can be used as a base class to determine whether an input reader
 * is readable by applying regular expression matching.
 *
 * @author Lindhauer
 */
public abstract class TextBasedDataFormatReader implements DataFormatReader {

  private static final SpinLogger LOG = SpinLogger.CORE_LOGGER;

  @Override
  public boolean canRead(Reader input, int readLimit) {
    char[] firstCharacters = new char[readLimit];

    try {
      input.read(firstCharacters, 0, readLimit);
    } catch (IOException e) {
      throw LOG.unableToReadFromReader(e);
    }


    Pattern pattern = getInputDetectionPattern();

    return pattern.matcher(new String(firstCharacters)).find();
  }

  protected abstract Pattern getInputDetectionPattern();
}
