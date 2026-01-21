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
package org.operaton.bpm.integrationtest.functional.cdi;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ArquillianExtension.class)
public class CdiBeanResolutionTwoEnginesTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment(name= "engine1", order = 1)
  public static WebArchive createDeployment() {
    final WebArchive webArchive = initWebArchiveDeployment("paEngine1.war", "org/operaton/bpm/integrationtest/paOnEngine1.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/cdi/CdiBeanResolutionTwoEnginesTest.testResolveBean.bpmn20.xml")
      .addAsLibraries(DeploymentHelper.getEngineCdi());

    TestContainer.addContainerSpecificProcessEngineConfigurationClass(webArchive);
    return webArchive;
  }

  @Test
  @OperateOnDeployment("engine1")
  void testResolveBean() {
    //given
    final ProcessEngine processEngine1 = processEngineService.getProcessEngine("engine1");
    assertThat(processEngine1.getName()).isEqualTo("engine1");
    createAuthorizations(processEngine1);

    //when we operate the process under authenticated user
    processEngine1.getIdentityService().setAuthentication("user1", List.of("group1"));

    processEngine1.getRuntimeService().startProcessInstanceByKey("testProcess");
    final List<Task> tasks = processEngine1.getTaskService().createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    processEngine1.getTaskService().complete(tasks.get(0).getId());

    //then
    //identityService resolution respects the engine, on which the process is being executed
    final List<VariableInstance> variableInstances = processEngine1.getRuntimeService().createVariableInstanceQuery().variableName("changeInitiatorUsername")
      .list();
    assertThat(variableInstances).hasSize(1);
    assertThat(variableInstances.get(0).getValue()).isEqualTo("user1");
  }

  private void createAuthorizations(ProcessEngine processEngine1) {
    Authorization newAuthorization = processEngine1.getAuthorizationService().createNewAuthorization(Authorization.AUTH_TYPE_GLOBAL);
    newAuthorization.setResource(Resources.PROCESS_INSTANCE);
    newAuthorization.setResourceId("*");
    newAuthorization.setPermissions(new Permission[] { Permissions.CREATE });
    processEngine1.getAuthorizationService().saveAuthorization(newAuthorization);

    newAuthorization = processEngine1.getAuthorizationService().createNewAuthorization(Authorization.AUTH_TYPE_GLOBAL);
    newAuthorization.setResource(Resources.PROCESS_DEFINITION);
    newAuthorization.setResourceId("*");
    newAuthorization.setPermissions(new Permission[] { Permissions.CREATE_INSTANCE });
    processEngine1.getAuthorizationService().saveAuthorization(newAuthorization);

    newAuthorization = processEngine1.getAuthorizationService().createNewAuthorization(Authorization.AUTH_TYPE_GLOBAL);
    newAuthorization.setResource(Resources.TASK);
    newAuthorization.setResourceId("*");
    newAuthorization.setPermissions(new Permission[] { Permissions.READ, Permissions.TASK_WORK });
    processEngine1.getAuthorizationService().saveAuthorization(newAuthorization);
  }

}
