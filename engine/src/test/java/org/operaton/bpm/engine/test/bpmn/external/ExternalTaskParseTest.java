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
package org.operaton.bpm.engine.test.bpmn.external;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class ExternalTaskParseTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RepositoryService repositoryService;
  RuntimeService runtimeService;
  ExternalTaskService externalTaskService;

  @Test
  void testParseExternalTaskWithoutTopic() {
    // given
    DeploymentBuilder deploymentBuilder = repositoryService
      .createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/bpmn/external/ExternalTaskParseTest.testParseExternalTaskWithoutTopic.bpmn20.xml");

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ParseException.class)
      .satisfies(e -> {
        ParseException parseException = (ParseException) e;
        assertThat(parseException.getMessage()).contains("External tasks must specify a 'topic' attribute in the operaton namespace");
        assertThat(parseException.getResourceReports().get(0).getErrors()).hasSize(1);
        assertThat(parseException.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("externalTask");
      });
  }

  @Deployment
  @Test
  void testParseExternalTaskWithExpressionTopic() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("topicName", "testTopicExpression");

    runtimeService.startProcessInstanceByKey("oneExternalTaskWithExpressionTopicProcess", variables);
    ExternalTask task = externalTaskService.createExternalTaskQuery().singleResult();
    assertThat(task.getTopicName()).isEqualTo("testTopicExpression");
  }

  @Deployment
  @Test
  void testParseExternalTaskWithStringTopic() {
    Map<String, Object> variables = new HashMap<>();

    runtimeService.startProcessInstanceByKey("oneExternalTaskWithStringTopicProcess", variables);
    ExternalTask task = externalTaskService.createExternalTaskQuery().singleResult();
    assertThat(task.getTopicName()).isEqualTo("testTopicString");
  }
}
