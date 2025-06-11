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
package org.operaton.bpm.engine.test.cmmn.listener;

import java.io.Serializable;

import org.operaton.bpm.engine.delegate.CaseExecutionListener;
import org.operaton.bpm.engine.delegate.DelegateCaseExecution;

/**
 * @author Roman Smirnov
 *
 */
public class CloseCaseExecutionListener implements CaseExecutionListener, Serializable {

  private static final long serialVersionUID = 1L;

  protected static String event;
  protected static int counter = 0;
  protected static String onCaseExecutionId;

  @Override
  public void notify(DelegateCaseExecution caseExecution) throws Exception {
    event = caseExecution.getEventName();
    counter = counter + 1;
    onCaseExecutionId = caseExecution.getId();
  }

  public static void clear() {
    event = null;
    counter = 0;
    onCaseExecutionId = null;
  }

}
