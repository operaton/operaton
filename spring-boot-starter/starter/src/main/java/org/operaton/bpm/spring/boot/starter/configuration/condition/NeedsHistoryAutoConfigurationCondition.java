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
package org.operaton.bpm.spring.boot.starter.configuration.condition;

import java.lang.reflect.Field;
import java.util.Optional;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ReflectionUtils;

public class NeedsHistoryAutoConfigurationCondition extends SpringBootCondition {

  protected static final String HISTORY_AUTO = "auto";

  protected String historyAutoFieldName = "HISTORY_AUTO";

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return needsAdditionalConfiguration(context)
      ? ConditionOutcome.match("operaton version needs additional configuration for history level auto")
      : ConditionOutcome.noMatch("operaton version supports history level auto");
  }

  protected boolean needsAdditionalConfiguration(ConditionContext context) {
    String historyLevel = context.getEnvironment().getProperty("operaton.bpm.history-level");
    if (HISTORY_AUTO.equals(historyLevel)) {
      return !isHistoryAutoSupported();
    }
    return false;
  }

  protected boolean isHistoryAutoSupported() {
    Field historyAutoField = ReflectionUtils.findField(ProcessEngineConfiguration.class, historyAutoFieldName);
    return Optional.ofNullable(historyAutoField).map(f -> {
      try {
        return f.get(null);
      } catch (IllegalAccessException e) {
        return null;
      }
    })
      .map("auto"::equals)
      .orElse(false);
  }
}
