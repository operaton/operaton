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
package org.operaton.bpm.integrationtest.functional.spin.dataformat;

import java.util.Date;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.delegate.TaskListener;

/**
 * @author Thorben Lindhauer
 *
 */
public class ImplicitObjectValueUpdateHandler implements JavaDelegate, TaskListener {

  public static final String VARIABLE_NAME = "var";
  public static final long ONE_DAY_IN_MILLIS = 1000 * 60 * 60 * 24;

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    JsonSerializable variable = (JsonSerializable) execution.getVariable(VARIABLE_NAME);

    addADay(variable);  // implicit update, i.e. no setVariable call

  }

  @Override
  public void notify(DelegateTask delegateTask) {
    JsonSerializable variable = (JsonSerializable) delegateTask.getVariable(VARIABLE_NAME);

    addADay(variable);  // implicit update, i.e. no setVariable call

  }

  public static void addADay(JsonSerializable jsonSerializable) {
    Date newDate = new Date(jsonSerializable.getDateProperty().getTime() + ONE_DAY_IN_MILLIS);
    jsonSerializable.setDateProperty(newDate);
  }


}
