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
package org.operaton.bpm.engine.cdi.test.impl.el.beans;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.TaskListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastian Menski
 */
@Named
@Dependent
public class CdiTaskListenerBean implements TaskListener {

  public static final String VARIABLE_NAME = "variable";
  public static final String INITIAL_VALUE = "a";
  public static final String UPDATED_VALUE = "b";

  @Inject
  BusinessProcess businessProcess;

  public void notify(DelegateTask delegateTask) {
    String variable = businessProcess.getVariable(VARIABLE_NAME);
    assertThat(variable).isEqualTo(INITIAL_VALUE);
    businessProcess.setVariable(VARIABLE_NAME, UPDATED_VALUE);
  }
}
