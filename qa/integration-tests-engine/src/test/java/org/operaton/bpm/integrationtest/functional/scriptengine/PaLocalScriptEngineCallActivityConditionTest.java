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

import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.integrationtest.functional.scriptengine.engine.AbstractScriptEngineFactory;
import org.operaton.bpm.integrationtest.functional.scriptengine.engine.AlwaysTrueScriptEngineFactory;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

import org.jboss.arquillian.container.test.api.Deployment;

import static org.assertj.core.api.Assertions.assertThat;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Thorben Lindhauer
 */
@RunWith(Arquillian.class)
public class PaLocalScriptEngineCallActivityConditionTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment(name="pa1")
  public static WebArchive createCallingProcessDeployment() {
    return initWebArchiveDeployment("pa1.war")
      .addClass(AbstractScriptEngineFactory.class)
      .addClass(AlwaysTrueScriptEngineFactory.class)
      .addAsResource(new StringAsset(AlwaysTrueScriptEngineFactory.class.getName()),
          PaLocalScriptEngineSupportTest.SCRIPT_ENGINE_FACTORY_PATH)
      .addAsResource("org/operaton/bpm/integrationtest/functional/scriptengine/PaLocalScriptEngineCallActivityConditionTest.callingProcessScriptConditionalFlow.bpmn20.xml");
  }

  @Deployment(name="pa2")
  public static WebArchive createCalledProcessDeployment() {
    return initWebArchiveDeployment("pa2.war")
      .addAsResource("org/operaton/bpm/integrationtest/functional/scriptengine/PaLocalScriptEngineCallActivityConditionTest.calledProcess.bpmn20.xml");
  }

  @Deployment(name="clientDeployment")
  public static WebArchive clientDeployment() {
    WebArchive deployment = ShrinkWrap.create(WebArchive.class, "client.war")
            .addAsLibraries(DeploymentHelper.getTestingLibs())
            .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
            .addClass(AbstractFoxPlatformIntegrationTest.class);

    TestContainer.addContainerSpecificResourcesForNonPa(deployment);

    return deployment;
  }

  @Test
  @OperateOnDeployment("clientDeployment")
  public void shouldEvaluateCondition() {
    // given
    runtimeService.startProcessInstanceByKey("callingProcessScriptConditionalFlow").getId();

    Task calledProcessTask = taskService.createTaskQuery().singleResult();

    // when the called process instance returns
    taskService.complete(calledProcessTask.getId());

    // then the conditional flow leaving the call activity has been taken
    Task afterCallActivityTask = taskService.createTaskQuery().singleResult();
    assertThat(afterCallActivityTask).isNotNull();
    assertThat(afterCallActivityTask.getTaskDefinitionKey()).isEqualTo("afterCallActivityTask");
  }

}
