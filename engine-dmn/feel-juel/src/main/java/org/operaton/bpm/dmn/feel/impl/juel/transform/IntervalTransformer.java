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
package org.operaton.bpm.dmn.feel.impl.juel.transform;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineLogger;
import org.operaton.bpm.dmn.feel.impl.juel.FeelLogger;

public class IntervalTransformer implements FeelToJuelTransformer {

  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;
  public static final Pattern INTERVAL_PATTERN = Pattern.compile("^([(\\[\\]])(.*[^.])\\.\\.(.+)([)\\]\\[])$");

  @Override
  public boolean canTransform(String feelExpression) {
    return feelExpression.startsWith("(") || feelExpression.startsWith("[") || feelExpression.startsWith("]");
  }

  @Override
  public String transform(FeelToJuelTransform transform, String feelExpression, String inputName) {
    Matcher matcher = INTERVAL_PATTERN.matcher(feelExpression);
    if (matcher.matches()) {
      return transformInterval(transform, matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), inputName);
    }
    else {
      throw LOG.invalidIntervalExpression(feelExpression);
    }
  }

  public String transformInterval(FeelToJuelTransform transform, String startIntervalSymbol, String lowerEndpoint, String upperEndpoint, String stopIntervalSymbol, String inputName) {
    String juelLowerEndpoint = transform.transformEndpoint(lowerEndpoint, inputName);
    String juelUpperEndpoint = transform.transformEndpoint(upperEndpoint, inputName);
    String lowerEndpointComparator = transformLowerEndpointComparator(startIntervalSymbol);
    String upperEndpointComparator = transformUpperEndpointComparator(stopIntervalSymbol);

    return "%s %s %s && %s %s %s".formatted(inputName, lowerEndpointComparator, juelLowerEndpoint, inputName, upperEndpointComparator, juelUpperEndpoint);
  }

  protected String transformLowerEndpointComparator(String startIntervalSymbol) {
    if ("[".equals(startIntervalSymbol)) {
      return ">=";
    }
    else {
      return ">";
    }
  }

  protected String transformUpperEndpointComparator(String stopIntervalSymbol) {
    if ("]".equals(stopIntervalSymbol)) {
      return "<=";
    }
    else {
      return "<";
    }
  }

}
