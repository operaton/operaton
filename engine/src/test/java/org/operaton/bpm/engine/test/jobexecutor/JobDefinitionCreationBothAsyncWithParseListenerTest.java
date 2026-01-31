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
package org.operaton.bpm.engine.test.jobexecutor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.operaton.bpm.engine.impl.jobexecutor.MessageJobDeclaration;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.util.xml.Element;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.management.JobDefinitionQuery;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static junit.framework.TestCase.assertEquals;

/**
 * Represents a test class, which uses parse listeners
 * to create job definitions for asyncBefore and asyncAfter activities.
 * The parse listeners are called after the bpmn xml was parsed.
 * They set the activity asyncBefore and asyncAfter property to true.
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
class JobDefinitionCreationBothAsyncWithParseListenerTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(configuration -> {
      List<BpmnParseListener> listeners = new ArrayList<>();
      listeners.add(new BpmnParseListener(){
        
        @Override
        public void parseServiceTask(Element serviceTaskElement, ScopeImpl scope, ActivityImpl activity) {
          activity.setAsyncBefore(true);
          activity.setAsyncAfter(true);
        }
      });
      
      configuration.setCustomPreBPMNParseListeners(listeners);
    })
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @ParameterizedTest
  @ValueSource(strings = {
      "jobCreationWithinParseListener.bpmn20.xml",
      "jobAsyncBeforeCreationWithinParseListener.bpmn20.xml", // the asyncBefore is set in the xml
      "jobAsyncBothCreationWithinParseListener.bpmn20.xml", // the asyncBefore AND asyncAfter is set in the xml
  })
  void testCreateJobDefinition(String bpmnResource) {
    //given
    InputStream in = JobDefinitionCreationWithParseListenerTest.class.getResourceAsStream(bpmnResource);
    DeploymentBuilder builder = engineRule.getRepositoryService().createDeployment().addInputStream(bpmnResource, in);

    //when the asyncBefore and asyncAfter is set to true in the parse listener
    Deployment deployment = builder.deploy();
    engineRule.manageDeployment(deployment);

    //then there exists two job definitions
    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    List<JobDefinition> definitions = query.orderByJobConfiguration().asc().list();
    assertEquals(definitions.size(), 2);

    //asyncAfter
    JobDefinition asyncAfterAfter = definitions.get(0);
    assertEquals(asyncAfterAfter.getProcessDefinitionKey(), "oneTaskProcess");
    assertEquals(asyncAfterAfter.getActivityId(), "servicetask1");
    assertEquals(asyncAfterAfter.getJobConfiguration(), MessageJobDeclaration.ASYNC_AFTER);

    //asyncBefore
    JobDefinition asyncAfterBefore = definitions.get(1);
    assertEquals(asyncAfterBefore.getProcessDefinitionKey(), "oneTaskProcess");
    assertEquals(asyncAfterBefore.getActivityId(), "servicetask1");
    assertEquals(asyncAfterBefore.getJobConfiguration(), MessageJobDeclaration.ASYNC_BEFORE);
  }

}
