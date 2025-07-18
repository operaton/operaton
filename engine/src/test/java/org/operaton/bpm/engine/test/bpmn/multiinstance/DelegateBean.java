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
package org.operaton.bpm.engine.test.bpmn.multiinstance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.operaton.bpm.engine.delegate.DelegateExecution;

/**
 * @author Thorben Lindhauer
 *
 */
public class DelegateBean implements Serializable {

  private static final long serialVersionUID = 1L;

  protected static final List<DelegateEvent> RECORDED_EVENTS = new ArrayList<>();

  public List<String> resolveCollection(DelegateExecution delegateExecution) {

    DelegateEvent.recordEventFor(delegateExecution);
    return Arrays.asList("1");
  }
}
