/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
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
package org.operaton.bpm.dmn.feel.impl.juel.el;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.operaton.bpm.impl.juel.jakarta.el.FunctionMapper;

import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineLogger;
import org.operaton.bpm.dmn.feel.impl.juel.FeelLogger;

public class FeelFunctionMapper extends FunctionMapper {

  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;

  protected static final SimpleDateFormat FEEL_DATE_AND_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  public static final String JUEL_DATE_AND_TIME_METHOD = "dateAndTime";

  protected static final Map<String, Method> methods = new HashMap<String, Method>();

  static {
    methods.put(JUEL_DATE_AND_TIME_METHOD, getMethod("parseDateAndTime", String.class));
  }

    /**
   * Retrieves a method based on its local name.
   * 
   * @param prefix the namespace prefix of the method
   * @param localName the local name of the method
   * @return the method object associated with the localName, or null if not found
   */
  public Method resolveFunction(String prefix, String localName) {
    return methods.get(localName);
  }

    /**
   * Retrieves a Method object with the specified name and parameter types from the FeelFunctionMapper class.
   * 
   * @param name the name of the method to retrieve
   * @param parameterTypes the parameter types of the method
   * @return the Method object representing the method with the specified name and parameter types
   * @throws RuntimeException if the method with the specified name and parameter types cannot be found
   */
  protected static Method getMethod(String name, Class<?>... parameterTypes) {
    try {
      return FeelFunctionMapper.class.getMethod(name, parameterTypes);
    } catch (NoSuchMethodException e) {
      throw LOG.unableToFindMethod(e, name, parameterTypes);
    }
  }

    /**
   * Parses a date and time string using a specific date and time format, returning a Date object.
   * 
   * @param dateAndTimeString the date and time string to be parsed
   * @return the Date object representing the parsed date and time string
   * @throws IllegalArgumentException if the input date and time string is not in the expected format
   */
  public static Date parseDateAndTime(String dateAndTimeString) {
    try {
      SimpleDateFormat clonedDateFormat = (SimpleDateFormat) FEEL_DATE_AND_TIME_FORMAT.clone();
      return clonedDateFormat.parse(dateAndTimeString);
    } catch (ParseException e) {
      throw LOG.invalidDateAndTimeFormat(dateAndTimeString, e);
    }
  }

}
