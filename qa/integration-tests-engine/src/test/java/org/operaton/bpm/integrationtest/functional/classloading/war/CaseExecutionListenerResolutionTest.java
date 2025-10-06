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
package org.operaton.bpm.integrationtest.functional.classloading.war;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.runtime.VariableInstanceQuery;
import org.operaton.bpm.integrationtest.functional.classloading.beans.ExampleCaseExecutionListener;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
@ExtendWith(ArquillianExtension.class)
public class CaseExecutionListenerResolutionTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createProcessArchiveDeplyoment() {
    return initWebArchiveDeployment()
            .addClass(ExampleCaseExecutionListener.class)
            .addAsResource("org/operaton/bpm/integrationtest/functional/classloading/CaseExecutionListenerResolutionTest.cmmn");
  }

  @Deployment(name="clientDeployment")
  public static WebArchive clientDeployment() {
    WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "client.war")
            .addClass(AbstractFoxPlatformIntegrationTest.class)
            .addAsLibraries(DeploymentHelper.getTestingLibs());

    TestContainer.addContainerSpecificResources(webArchive);

    return webArchive;

  }

  @Test
  @OperateOnDeployment("clientDeployment")
  void testResolveCaseExecutionListenerClass() {
    // assert that we cannot load the delegate here:
    assertThatThrownBy(() -> Class.forName("org.operaton.bpm.integrationtest.functional.classloading.beans.ExampleCaseExecutionListener")).isInstanceOf(ClassNotFoundException.class);

    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .variableName("listener")
        .caseInstanceIdIn(caseInstanceId);

    assertThat(query.singleResult()).isNotNull();
    assertThat(query.singleResult().getValue()).isEqualTo("listener-notified");

    caseService
      .withCaseExecution(caseInstanceId)
      .removeVariable("listener")
      .execute();

    assertThat(query.count()).isZero();

    // the delegate expression listener should execute successfully
    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    assertThat(query.singleResult()).isNotNull();
    assertThat(query.singleResult().getValue()).isEqualTo("listener-notified");

  }

}
