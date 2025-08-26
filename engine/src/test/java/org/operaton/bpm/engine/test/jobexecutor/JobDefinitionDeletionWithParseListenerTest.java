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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.util.xml.Element;
import org.operaton.bpm.engine.management.JobDefinitionQuery;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static junit.framework.TestCase.assertNull;

/**
 * Represents a test class, which uses parse listeners
 * to delete job definitions for async activities.
 * The parse listeners are called after the bpmn xml was parsed.
 * They set the activity asyncBefore property to false. In this case
 * there should delete some job declarations for the activity which was async before.
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
class JobDefinitionDeletionWithParseListenerTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(configuration -> {
      List<BpmnParseListener> listeners = new ArrayList<>();
      listeners.add(new BpmnParseListener(){
        
        @Override
        public void parseServiceTask(Element serviceTaskElement, ScopeImpl scope, ActivityImpl activity) {
          activity.setAsyncBefore(false);
        }
      });
      
      configuration.setCustomPreBPMNParseListeners(listeners);
    })
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @Test
  void testDeleteNonExistingJobDefinitionWithParseListener() {
    //given
    String modelFileName = "jobCreationWithinParseListener.bpmn20.xml";
    InputStream in = JobDefinitionCreationWithParseListenerTest.class.getResourceAsStream(modelFileName);
    DeploymentBuilder builder = engineRule.getRepositoryService().createDeployment().addInputStream(modelFileName, in);
    //when the asyncBefore is set to false in the parse listener
    Deployment deployment = builder.deploy();
    engineRule.manageDeployment(deployment);
    //then there exists no job definition
    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertNull(query.singleResult());
  }

  @Test
  void testDeleteJobDefinitionWithParseListenerAndAsyncInXml() {
    //given the asyncBefore is set in the xml
    String modelFileName = "jobAsyncBeforeCreationWithinParseListener.bpmn20.xml";
    InputStream in = JobDefinitionCreationWithParseListenerTest.class.getResourceAsStream(modelFileName);
    DeploymentBuilder builder = engineRule.getRepositoryService().createDeployment().addInputStream(modelFileName, in);
    //when the asyncBefore is set to false in the parse listener
    Deployment deployment = builder.deploy();
    engineRule.manageDeployment(deployment);
    //then there exists no job definition
    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertNull(query.singleResult());
  }
}
