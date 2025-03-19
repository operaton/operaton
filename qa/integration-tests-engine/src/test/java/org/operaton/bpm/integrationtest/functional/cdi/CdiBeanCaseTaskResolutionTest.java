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
package org.operaton.bpm.integrationtest.functional.cdi;

import org.junit.Ignore;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.integrationtest.functional.cdi.beans.CaseVariableBean;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Thorben Lindhauer
 *
 */
@RunWith(Arquillian.class)
@Ignore("https://github.com/operaton/operaton/issues/616")
public class CdiBeanCaseTaskResolutionTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment(name="pa1")
  public static WebArchive createCallingProcessDeployment() {
    return initWebArchiveDeployment("pa1.war")
            .addAsResource("org/operaton/bpm/integrationtest/functional/cdi/CdiBeanCallActivityResolutionTest.callingCase.cmmn");
  }

  @Deployment(name="pa2")
  public static WebArchive createCalledProcessDeployment() {
    return initWebArchiveDeployment("pa2.war")
            .addClass(CaseVariableBean.class)
            .addAsResource("org/operaton/bpm/integrationtest/functional/cdi/CdiBeanCallActivityResolutionTest.calledCase.cmmn");
  }

  @Deployment(name="clientDeployment")
  public static WebArchive clientDeployment() {
    WebArchive deployment = ShrinkWrap.create(WebArchive.class, "client.war")
            .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
            .addClass(AbstractFoxPlatformIntegrationTest.class)
            .addAsLibraries(DeploymentHelper.getEngineCdi());

    TestContainer.addContainerSpecificResourcesForNonPaEmbedCdiLib(deployment);

    return deployment;
  }

  @Test
  @OperateOnDeployment("clientDeployment")
  public void testResolveBeanInCmmnCase() {
    CaseInstance caseInstance = caseService.withCaseDefinitionByKey("callingCase")
        .create();

    CaseExecution caseTaskInstance = caseService.createCaseExecutionQuery().activityId("PI_CaseTask_1")
        .singleResult();

    CaseExecution calledCaseHumanTaskInstance = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1")
        .singleResult();

    Task calledCaseTask = taskService.createTaskQuery().singleResult();

    taskService.complete(calledCaseTask.getId(),
        Variables.createVariables().putValue("var", "value"));

    CaseInstance calledInstance = caseService.createCaseInstanceQuery()
        .caseDefinitionKey("calledCase").singleResult();

    caseService.withCaseExecution(calledInstance.getId()).close();

    // then
    String variable = (String) caseService.getVariable(caseInstance.getId(), "var");
    Assert.assertEquals("valuevalue", variable);
  }
}
