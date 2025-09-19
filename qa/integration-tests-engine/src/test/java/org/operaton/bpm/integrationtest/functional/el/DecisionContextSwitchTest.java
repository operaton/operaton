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
package org.operaton.bpm.integrationtest.functional.el;

import java.util.List;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.integrationtest.functional.el.beans.GreeterBean;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class DecisionContextSwitchTest extends AbstractFoxPlatformIntegrationTest {

  protected static final String DMN_RESOURCE_NAME = "org/operaton/bpm/integrationtest/functional/el/BeanResolvingDecision.dmn11.xml";

  @Deployment(name="bpmnDeployment")
  public static WebArchive createBpmnDeployment() {
    return initWebArchiveDeployment("bpmn-deployment.war")
      .addAsResource("org/operaton/bpm/integrationtest/functional/el/BusinessRuleProcess.bpmn20.xml");
  }

  @Deployment(name="dmnDeployment")
  public static WebArchive createDmnDeployment() {
    return initWebArchiveDeployment("dmn-deployment.war")
      .addClass(GreeterBean.class)
      .addAsResource(DMN_RESOURCE_NAME);
  }


  @Deployment(name="clientDeployment")
  public static WebArchive clientDeployment() {
    WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "client.war")
            .addAsLibraries(DeploymentHelper.getTestingLibs())
            .addClass(AbstractFoxPlatformIntegrationTest.class);

    TestContainer.addContainerSpecificResources(webArchive);

    return webArchive;
  }

  @Test
  @OperateOnDeployment("clientDeployment")
  void shouldSwitchContextWhenUsingDecisionService() {
    DmnDecisionTableResult decisionResult = decisionService.evaluateDecisionTableByKey("decision", Variables.createVariables());
    String firstResult = decisionResult.getFirstResult().getFirstEntry();
    assertThat(firstResult).isEqualTo("ok");
  }

  @Test
  @SuppressWarnings("unchecked")
  @OperateOnDeployment("clientDeployment")
  void shouldSwitchContextWhenCallingFromBpmn() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance decisionResult = runtimeService.createVariableInstanceQuery()
      .processInstanceIdIn(pi.getId())
      .variableName("result").singleResult();
    List<Map<String, Object>> result = (List<Map<String, Object>>) decisionResult.getValue();
    assertThat(result.get(0)).containsEntry("result", "ok");
  }

  @Test
  @OperateOnDeployment("clientDeployment")
  void shouldSwitchContextWhenUsingDecisionServiceAfterRedeployment() {

    // given
    List<org.operaton.bpm.engine.repository.Deployment> deployments = repositoryService.createDeploymentQuery()
        .list();

    // find dmn deployment
    org.operaton.bpm.engine.repository.Deployment dmnDeployment = null;
    for (org.operaton.bpm.engine.repository.Deployment deployment : deployments) {
      List<String> resourceNames = repositoryService.getDeploymentResourceNames(deployment.getId());
      if(resourceNames.contains(DMN_RESOURCE_NAME)) {
        dmnDeployment = deployment;
      }
    }

    if(dmnDeployment == null) {
      fail("Expected to find DMN deployment");
    }

    org.operaton.bpm.engine.repository.Deployment deployment2 = repositoryService
      .createDeployment()
      .nameFromDeployment(dmnDeployment.getId())
      .addDeploymentResources(dmnDeployment.getId())
      .deploy();

    try {
      // when then
      DmnDecisionTableResult decisionResult = decisionService.evaluateDecisionTableByKey("decision", Variables.createVariables());
      String firstResult = decisionResult.getFirstResult().getFirstEntry();
      assertThat(firstResult).isEqualTo("ok");
    }
    finally {
      repositoryService.deleteDeployment(deployment2.getId(), true);
    }

  }

  @Test
  @SuppressWarnings("unchecked")
  @OperateOnDeployment("clientDeployment")
  void shouldSwitchContextWhenCallingFromBpmnAfterRedeployment() {
    // given
    List<org.operaton.bpm.engine.repository.Deployment> deployments = repositoryService.createDeploymentQuery()
        .list();

    // find dmn deployment
    org.operaton.bpm.engine.repository.Deployment dmnDeployment = null;
    for (org.operaton.bpm.engine.repository.Deployment deployment : deployments) {
      List<String> resourceNames = repositoryService.getDeploymentResourceNames(deployment.getId());
      if(resourceNames.contains(DMN_RESOURCE_NAME)) {
        dmnDeployment = deployment;
      }
    }

    if(dmnDeployment == null) {
      fail("Expected to find DMN deployment");
    }

    org.operaton.bpm.engine.repository.Deployment deployment2 = repositoryService
      .createDeployment()
      .nameFromDeployment(dmnDeployment.getId())
      .addDeploymentResources(dmnDeployment.getId())
      .deploy();

    try {
      // when then
      ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

      VariableInstance decisionResult = runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(pi.getId())
        .variableName("result")
        .singleResult();
      List<Map<String, Object>> result = (List<Map<String, Object>>) decisionResult.getValue();
      assertThat(result.get(0)).containsEntry("result", "ok");
    }
    finally {
      repositoryService.deleteDeployment(deployment2.getId(), true);
    }
  }

}
