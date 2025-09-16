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
package org.operaton.bpm.integrationtest.functional.scriptengine;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.test.util.JobExecutorWaitUtils;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ArquillianExtension.class)
public class GroovyAsyncScriptExecutionTest extends AbstractFoxPlatformIntegrationTest {

  protected static String process =
  """
  <?xml version="1.0" encoding="UTF-8"?>
  <definitions id="definitions"
    xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
    xmlns:operaton="http://operaton.org/schema/1.0/bpmn"
    targetNamespace="Examples">
    <process id="process" isExecutable="true" operaton:historyTimeToLive="P180D">
      <startEvent id="theStart" />
      <sequenceFlow id="flow1" sourceRef="theStart" targetRef="theScriptTask" />
      <scriptTask id="theScriptTask" name="Execute script" scriptFormat="groovy" operaton:asyncBefore="true">
        <script>execution.setVariable("foo", S("&lt;bar /&gt;").name())</script>
      </scriptTask>
      <sequenceFlow id="flow2" sourceRef="theScriptTask" targetRef="theTask" />
      <userTask id="theTask" name="my task" />
      <sequenceFlow id="flow3" sourceRef="theTask" targetRef="theEnd" />
      <endEvent id="theEnd" />
    </process>
  </definitions>
  """;

  @Deployment(name="clientDeployment")
  public static WebArchive clientDeployment() {
    WebArchive deployment = ShrinkWrap.create(WebArchive.class, "client.war")
            .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
            .addClass(AbstractFoxPlatformIntegrationTest.class)
            .addClass(JobExecutorWaitUtils.class)
            .addAsLibraries(DeploymentHelper.getEngineCdi())
            .addAsLibraries(DeploymentHelper.getTestingLibs());
    TestContainer.addContainerSpecificResourcesForNonPa(deployment);
    return deployment;
  }

  @Test
  @OperateOnDeployment("clientDeployment")
  void shouldSetVariable() {
    String deploymentId = repositoryService.createDeployment()
        .addString("process.bpmn", process)
        .deploy()
        .getId();

    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();
    waitForJobExecutorToProcessAllJobs(30000);

    Object foo = runtimeService.getVariable(processInstanceId, "foo");
    assertThat(foo).isNotNull();
    assertThat(foo).isEqualTo("bar");

    repositoryService.deleteDeployment(deploymentId, true);
  }
}
