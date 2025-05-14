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
package org.operaton.bpm.engine.spring.test.components.registry;

import org.operaton.bpm.engine.spring.components.registry.ActivitiStateHandlerRegistration;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tobias Metzke
 *
 */
class ActivitiStateHandlerRegistrationTest {

  @Test
  void shouldHaveDetailedStringRepresentation() throws Exception {
    Map<Integer, String> processVariablesExpected = Collections.singletonMap(34, "testValue");
    Method handlerMethod = this.getClass().getDeclaredMethod("shouldHaveDetailedStringRepresentation");
    Object handler = new Object() { public Integer testValue; {testValue = 76; testValue++; }};
    String stateName = "running";
    String beanName = "testBean";
    int processVariablesIndex = 4;
    int processIdIndex = 2;
    String processName = "testProcess";
    ActivitiStateHandlerRegistration registration = new ActivitiStateHandlerRegistration(processVariablesExpected, 
        handlerMethod, handler, stateName, beanName,
        processVariablesIndex, processIdIndex, processName);
    assertThat(registration).hasToString("org.operaton.bpm.engine.spring.components.registry"
        + ".ActivitiStateHandlerRegistration@" + Integer.toHexString(registration.hashCode()) + "["
      + "processVariablesExpected={34=testValue}, "
      + "handlerMethod=void org.operaton.bpm.engine.spring.test.components.registry.ActivitiStateHandlerRegistrationTest.shouldHaveDetailedStringRepresentation() throws java.lang.Exception, "
      + "handler=org.operaton.bpm.engine.spring.test.components.registry.ActivitiStateHandlerRegistrationTest$1@" + Integer.toHexString(handler.hashCode()) + ", "
      + "stateName=running, "
      + "beanName=testBean, "
      + "processVariablesIndex=4, "
      + "processIdIndex=2, "
      + "processName=testProcess]");
  }

  @Test
  void shouldHaveDetailedStringRepresentationWithNullValues() {
    Map<Integer, String> processVariablesExpected = Collections.singletonMap(34, "testValue");
    Method handlerMethod = null;
    Object handler = null;
    String stateName = "running";
    String beanName = "testBean";
    int processVariablesIndex = 4;
    int processIdIndex = 2;
    String processName = "testProcess";
    ActivitiStateHandlerRegistration registration = new ActivitiStateHandlerRegistration(processVariablesExpected, 
        handlerMethod, handler, stateName, beanName,
        processVariablesIndex, processIdIndex, processName);
    assertThat(registration).hasToString("org.operaton.bpm.engine.spring.components.registry"
        + ".ActivitiStateHandlerRegistration@" + Integer.toHexString(registration.hashCode()) + "["
      + "processVariablesExpected={34=testValue}, "
      + "handlerMethod=null, "
      + "handler=null, "
      + "stateName=running, "
      + "beanName=testBean, "
      + "processVariablesIndex=4, "
      + "processIdIndex=2, "
      + "processName=testProcess]");
  }
}
