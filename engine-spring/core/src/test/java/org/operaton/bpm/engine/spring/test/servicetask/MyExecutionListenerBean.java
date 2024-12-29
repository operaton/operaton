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
package org.operaton.bpm.engine.spring.test.servicetask;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.impl.el.FixedValue;

/**
 * @author Joram Barrez
 * @author Bernd Ruecker (operaton)
 */
@SuppressWarnings("unused")
public class MyExecutionListenerBean implements ExecutionListener {

  private FixedValue someField;

  @Override
  public void notify(DelegateExecution execution) {
    execution.setVariable("executionListenerVar", "working");
    if (someField!=null) {
      execution.setVariable("executionListenerField", someField.getValue(execution));
    }
  }

  public FixedValue getSomeField() {
    return someField;
  }

  public void setSomeField(FixedValue someField) {
    this.someField = someField;
  }

}
