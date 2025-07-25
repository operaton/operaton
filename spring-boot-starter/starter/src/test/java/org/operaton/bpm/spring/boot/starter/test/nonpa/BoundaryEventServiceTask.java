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
package org.operaton.bpm.spring.boot.starter.test.nonpa;

import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component("boundaryEventServiceTask")
public class BoundaryEventServiceTask implements JavaDelegate {

  public static final String ERROR_NAME = "errorName";

  @Override
  public void execute(DelegateExecution delegateExecution) {
    Optional.ofNullable(delegateExecution.getVariable(ERROR_NAME))
      .map(String.class::cast)
      .filter(StringUtils::isNotBlank)
      .ifPresent(value -> {
         throw new BpmnError(value);
      });
  }
}
