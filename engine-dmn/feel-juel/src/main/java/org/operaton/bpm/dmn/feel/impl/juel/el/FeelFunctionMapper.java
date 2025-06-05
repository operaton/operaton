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
package org.operaton.bpm.dmn.feel.impl.juel.el;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import jakarta.el.FunctionMapper;

import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineLogger;
import org.operaton.bpm.dmn.feel.impl.juel.FeelLogger;

/**
 * A custom implementation of the `FunctionMapper` class for mapping FEEL (Friendly Enough Expression Language)
 * functions to Java methods. This class provides support for resolving and executing FEEL functions.
 */
public class FeelFunctionMapper extends FunctionMapper {

  // Logger instance for logging errors and messages
  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;

  // Date and time format used for parsing FEEL date and time strings
  protected static final String FEEL_DATE_AND_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

  // Name of the JUEL method for date and time parsing
  public static final String JUEL_DATE_AND_TIME_METHOD = "dateAndTime";

  // Map to store method references for FEEL functions
  protected static final Map<String, Method> methods = new HashMap<>();

  // Static initializer block to populate the methods map
  static {
    methods.put(JUEL_DATE_AND_TIME_METHOD, getMethod("parseDateAndTime", String.class));
  }

  /**
   * Resolves a function by its prefix and local name.
   *
   * @param prefix the prefix of the function (not used in this implementation)
   * @param localName the local name of the function
   * @return the corresponding `Method` object, or `null` if no method is found
   */
  @Override
  public Method resolveFunction(String prefix, String localName) {
    return methods.get(localName);
  }

  /**
   * Retrieves a `Method` object for the specified method name and parameter types.
   *
   * @param name the name of the method
   * @param parameterTypes the parameter types of the method
   * @return the `Method` object
   * @throws IllegalStateException if the method cannot be found
   */
  protected static Method getMethod(String name, Class<?>... parameterTypes) {
    try {
      return FeelFunctionMapper.class.getMethod(name, parameterTypes);
    } catch (NoSuchMethodException e) {
      throw LOG.unableToFindMethod(e, name, parameterTypes);
    }
  }

  /**
   * Parses a date and time string into a `Date` object using the FEEL date and time format.
   *
   * @param dateAndTimeString the date and time string to parse
   * @return the parsed `Date` object
   * @throws IllegalArgumentException if the string cannot be parsed
   */
  public static Date parseDateAndTime(String dateAndTimeString) {
    try {
      SimpleDateFormat dateFormat = new SimpleDateFormat(FEEL_DATE_AND_TIME_FORMAT);
      return dateFormat.parse(dateAndTimeString);
    } catch (ParseException e) {
      throw LOG.invalidDateAndTimeFormat(dateAndTimeString, e);
    }
  }

}
