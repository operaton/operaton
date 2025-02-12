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
package org.operaton.bpm.engine.test.api.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.operaton.bpm.application.ProcessApplicationRegistration;
import org.operaton.bpm.application.impl.EmbeddedProcessApplication;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.query.Query;
import org.operaton.bpm.engine.repository.*;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;


/**
 * @author Roman Smirnov
 *
 */
public class RedeploymentTest {

  public static final String DEPLOYMENT_NAME = "my-deployment";
  public static final String PROCESS_KEY = "process";
  public static final String PROCESS_1_KEY = "process-1";
  public static final String PROCESS_2_KEY = "process-2";
  public static final String PROCESS_3_KEY = "process-3";
  public static final String RESOURCE_NAME = "path/to/my/process.bpmn";
  public static final String RESOURCE_1_NAME = "path/to/my/process1.bpmn";
  public static final String RESOURCE_2_NAME = "path/to/my/process2.bpmn";
  public static final String RESOURCE_3_NAME = "path/to/my/process3.bpmn";

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  protected RepositoryService repositoryService;
  protected boolean enforceHistoryTimeToLive;

  @Before
  public void setUp() {
    repositoryService = engineRule.getRepositoryService();
    enforceHistoryTimeToLive = engineRule.getProcessEngineConfiguration().isEnforceHistoryTimeToLive();
  }

  @After
  public void tearDown() {
    engineRule.getProcessEngineConfiguration().setEnforceHistoryTimeToLive(enforceHistoryTimeToLive);
  }

  @Test
  public void testRedeployInvalidDeployment() {
    var deploymentBuilder = repositoryService
      .createDeployment()
      .name(DEPLOYMENT_NAME)
      .addDeploymentResources("not-existing");
    try {
      deploymentBuilder.deploy();
      fail("It should not be able to re-deploy an unexisting deployment");
    } catch (NotFoundException e) {
      // expected
    }

    var deploymentBuilder1 = repositoryService
      .createDeployment()
      .name(DEPLOYMENT_NAME)
      .addDeploymentResourceById("not-existing", "an-id");
    try {
      deploymentBuilder1.deploy();
      fail("It should not be able to re-deploy an unexisting deployment");
    } catch (NotFoundException e) {
      // expected
    }

    var deploymentBuilder2 = repositoryService
      .createDeployment()
      .name(DEPLOYMENT_NAME)
      .addDeploymentResourcesById("not-existing", Collections.singletonList("an-id"));
    try {
      deploymentBuilder2.deploy();
      fail("It should not be able to re-deploy an unexisting deployment");
    } catch (NotFoundException e) {
      // expected
    }

    var deploymentBuilder3 = repositoryService
      .createDeployment()
      .name(DEPLOYMENT_NAME)
      .addDeploymentResourceByName("not-existing", "a-name");
    try {
      deploymentBuilder3.deploy();
      fail("It should not be able to re-deploy an unexisting deployment");
    } catch (NotFoundException e) {
      // expected
    }

    var deploymentBuilder4 = repositoryService
      .createDeployment()
      .name(DEPLOYMENT_NAME)
      .addDeploymentResourcesByName("not-existing", Collections.singletonList("a-name"));
    try {
      deploymentBuilder4.deploy();
      fail("It should not be able to re-deploy an unexisting deployment");
    } catch (NotFoundException e) {
      // expected
    }
  }

  @Test
  public void testNotValidDeploymentId() {
    var deploymentBuilder = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME);
    var resourceIdList = Collections.singletonList("a-name");

    try {
      deploymentBuilder.addDeploymentResources(null);
      fail("It should not be possible to pass a null deployment id");
    } catch (NotValidException e) {
      // expected
    }

    try {
      deploymentBuilder.addDeploymentResourceById(null, "an-id");
      fail("It should not be possible to pass a null deployment id");
    } catch (NotValidException e) {
      // expected
    }

    try {
      deploymentBuilder.addDeploymentResourcesById(null, resourceIdList);
      fail("It should not be possible to pass a null deployment id");
    } catch (NotValidException e) {
      // expected
    }

    try {
      deploymentBuilder.addDeploymentResourceByName(null, "a-name");
      fail("It should not be possible to pass a null deployment id");
    } catch (NotValidException e) {
      // expected
    }

    try {
      deploymentBuilder.addDeploymentResourcesByName(null, resourceIdList);
      fail("It should not be possible to pass a null deployment id");
    } catch (NotValidException e) {
      // expected
    }
  }

  @Test
  public void testRedeployUnexistingDeploymentResource() {
    // given
    BpmnModelInstance model = createProcessWithServiceTask(PROCESS_KEY);

    Deployment deployment = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, model));
    var deploymentBuilder = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResourcesById(deployment.getId(), Collections.singletonList("not-existing" +
                                                                                      "-resource-id"));

    try {
      // when
      deploymentBuilder.deploy();
      fail("It should not be possible to re-deploy a not existing deployment resource");
    } catch (NotFoundException e) {
      // then
      // expected
    }

    try {
      // when
      deploymentBuilder.deploy();
      fail("It should not be possible to re-deploy a not existing deployment resource");
    } catch (NotFoundException e) {
      // then
      // expected
    }

    try {
      // when
      deploymentBuilder.deploy();
      fail("It should not be possible to re-deploy a not existing deployment resource");
    } catch (NotFoundException e) {
      // then
      // expected
    }

    try {
      // when
      deploymentBuilder.deploy();
      fail("It should not be possible to re-deploy a not existing deployment resource");
    } catch (NotFoundException e) {
      // then
      // expected
    }
  }

  @Test
  public void testNotValidResource() {
    var deploymentBuilder = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME);
    var listWithNullResourceId = Collections.<String>singletonList(null);

    try {
      deploymentBuilder.addDeploymentResourceById("an-id", null);
      fail("It should not be possible to pass a null resource id");
    } catch (NotValidException e) {
      // expected
    }

    try {
      deploymentBuilder.addDeploymentResourcesById("an-id", null);
      fail("It should not be possible to pass a null resource id");
    } catch (NotValidException e) {
      // expected
    }

    try {
      deploymentBuilder.addDeploymentResourcesById("an-id", listWithNullResourceId);
      fail("It should not be possible to pass a null resource id");
    } catch (NotValidException e) {
      // expected
    }

    ArrayList<String> emptyResourceIds = new ArrayList<>();
    try {
      deploymentBuilder.addDeploymentResourcesById("an-id", emptyResourceIds);
      fail("It should not be possible to pass a null resource id");
    } catch (NotValidException e) {
      // expected
    }

    try {
      deploymentBuilder.addDeploymentResourceByName("an-id", null);
      fail("It should not be possible to pass a null resource name");
    } catch (NotValidException e) {
      // expected
    }

    try {
      deploymentBuilder.addDeploymentResourcesByName("an-id", null);
      fail("It should not be possible to pass a null resource name");
    } catch (NotValidException e) {
      // expected
    }

    try {
      deploymentBuilder.addDeploymentResourcesByName("an-id", listWithNullResourceId);
      fail("It should not be possible to pass a null resource name");
    } catch (NotValidException e) {
      // expected
    }

    try {
      deploymentBuilder.addDeploymentResourcesByName("an-id", emptyResourceIds);
      fail("It should not be possible to pass a null resource name");
    } catch (NotValidException e) {
      // expected
    }
  }

  @Test
  public void testRedeployNewDeployment() {
    // given
    BpmnModelInstance model = createProcessWithServiceTask(PROCESS_KEY);

    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, model));

    DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentName(DEPLOYMENT_NAME);

    assertNotNull(deployment1.getId());
    verifyQueryResults(query, 1);

    // when
    Deployment deployment2 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResources(deployment1.getId()));

    // then
    assertNotNull(deployment2);
    assertNotNull(deployment2.getId());
    assertNotEquals(deployment1.getId(), deployment2.getId());

    verifyQueryResults(query, 2);
  }

  @Test
  public void testFailingDeploymentName() {
    var deploymentBuilder = repositoryService
      .createDeployment()
      .name(DEPLOYMENT_NAME);
    try {
      deploymentBuilder.nameFromDeployment("a-deployment-id");
      fail("Cannot set name() and nameFromDeployment().");
    } catch (NotValidException e) {
      // expected
    }

    var deploymentBuilder2 = repositoryService
      .createDeployment()
      .nameFromDeployment("a-deployment-id");
    try {
      deploymentBuilder2.name(DEPLOYMENT_NAME);
      fail("Cannot set name() and nameFromDeployment().");
    } catch (NotValidException e) {
      // expected
    }
  }

  @Test
  public void testRedeployDeploymentName() {
    // given
    BpmnModelInstance model = createProcessWithServiceTask(PROCESS_KEY);

    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, model));

    assertThat(deployment1.getName()).isEqualTo(DEPLOYMENT_NAME);

    // when
    Deployment deployment2 = testRule.deploy(repositoryService
        .createDeployment()
        .nameFromDeployment(deployment1.getId())
        .addDeploymentResources(deployment1.getId()));

    // then
    assertNotNull(deployment2);
    assertThat(deployment2.getName()).isEqualTo(deployment1.getName());
  }

  @Test
  public void testRedeployDeploymentDifferentName() {
    // given
    BpmnModelInstance model = createProcessWithServiceTask(PROCESS_KEY);

    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, model));

    assertThat(deployment1.getName()).isEqualTo(DEPLOYMENT_NAME);

    // when
    Deployment deployment2 = testRule.deploy(repositoryService
        .createDeployment()
        .name("my-another-deployment")
        .addDeploymentResources(deployment1.getId()));

    // then
    assertNotNull(deployment2);
    assertNotEquals(deployment1.getName(), deployment2.getName());
  }

  @Test
  public void testRedeployDeploymentSourcePropertyNotSet() {
    // given
    BpmnModelInstance model = createProcessWithServiceTask(PROCESS_KEY);

    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .source("my-deployment-source")
        .addModelInstance(RESOURCE_NAME, model));

    assertThat(deployment1.getSource()).isEqualTo("my-deployment-source");

    // when
    Deployment deployment2 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResources(deployment1.getId()));

    // then
    assertNotNull(deployment2);
    assertNull(deployment2.getSource());
  }

  @Test
  public void testRedeploySetDeploymentSourceProperty() {
    // given
    BpmnModelInstance model = createProcessWithServiceTask(PROCESS_KEY);

    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .source("my-deployment-source")
        .addModelInstance(RESOURCE_NAME, model));

    assertThat(deployment1.getSource()).isEqualTo("my-deployment-source");

    // when
    Deployment deployment2 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResources(deployment1.getId())
        .source("my-another-deployment-source"));

    // then
    assertNotNull(deployment2);
    assertThat(deployment2.getSource()).isEqualTo("my-another-deployment-source");
  }

  @Test
  public void testRedeployDeploymentResource() {
    // given

    // first deployment
    BpmnModelInstance model = createProcessWithServiceTask(PROCESS_KEY);
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, model));

    Resource resource1 = getResourceByName(deployment1.getId(), RESOURCE_NAME);

    // second deployment
    model = createProcessWithUserTask(PROCESS_KEY);
    Deployment deployment2 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, model));

    Resource resource2 = getResourceByName(deployment2.getId(), RESOURCE_NAME);

    // when
    Deployment deployment3 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResources(deployment1.getId()));

    // then
    Resource resource3 = getResourceByName(deployment3.getId(), RESOURCE_NAME);
    assertNotNull(resource3);

    // id
    assertNotNull(resource3.getId());
    assertNotEquals(resource1.getId(), resource3.getId());

    // deployment id
    assertThat(resource3.getDeploymentId()).isEqualTo(deployment3.getId());

    // name
    assertThat(resource3.getName()).isEqualTo(resource1.getName());

    // bytes
    byte[] bytes1 = resource1.getBytes();
    byte[] bytes2 = resource2.getBytes();
    byte[] bytes3 = resource3.getBytes();
    assertArrayEquals(bytes1, bytes3);
    assertFalse(Arrays.equals(bytes2, bytes3));
  }

  @Test
  public void testRedeployAllDeploymentResources() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);
    BpmnModelInstance model2 = createProcessWithUserTask(PROCESS_2_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);

    // second deployment
    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model2)
        .addModelInstance(RESOURCE_2_NAME, model1));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);

    // when
    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResources(deployment1.getId()));

    // then
    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 3);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 3);
  }

  @Test
  public void testRedeployOneDeploymentResourcesByName() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);
    BpmnModelInstance model2 = createProcessWithUserTask(PROCESS_2_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);

    // second deployment
    model1 = createProcessWithScriptTask(PROCESS_1_KEY);
    model2 = createProcessWithReceiveTask(PROCESS_2_KEY);

    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);

    // when
    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResourceByName(deployment1.getId(), RESOURCE_1_NAME));

    // then
    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 3);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
  }

  @Test
  public void testRedeployMultipleDeploymentResourcesByName() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);
    BpmnModelInstance model2 = createProcessWithUserTask(PROCESS_2_KEY);
    BpmnModelInstance model3 = createProcessWithScriptTask(PROCESS_3_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2)
        .addModelInstance(RESOURCE_3_NAME, model3));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_3_KEY), 1);

    // second deployment
    model1 = createProcessWithScriptTask(PROCESS_1_KEY);
    model2 = createProcessWithReceiveTask(PROCESS_2_KEY);
    model3 = createProcessWithUserTask(PROCESS_3_KEY);

    Deployment deployment2 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2)
        .addModelInstance(RESOURCE_3_NAME, model3));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_3_KEY), 2);

    // when (1)
    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResourceByName(deployment1.getId(), RESOURCE_1_NAME)
        .addDeploymentResourceByName(deployment1.getId(), RESOURCE_3_NAME));

    // then (1)
    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 3);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_3_KEY), 3);

    // when (2)
    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResourcesByName(deployment2.getId(), Arrays.asList(RESOURCE_1_NAME, RESOURCE_3_NAME)));

    // then (2)
    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 4);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_3_KEY), 4);
  }

  @Test
  public void testRedeployOneAndMultipleDeploymentResourcesByName() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);
    BpmnModelInstance model2 = createProcessWithUserTask(PROCESS_2_KEY);
    BpmnModelInstance model3 = createProcessWithScriptTask(PROCESS_3_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2)
        .addModelInstance(RESOURCE_3_NAME, model3));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_3_KEY), 1);

    // second deployment
    model1 = createProcessWithScriptTask(PROCESS_1_KEY);
    model2 = createProcessWithReceiveTask(PROCESS_2_KEY);
    model3 = createProcessWithUserTask(PROCESS_3_KEY);

    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2)
        .addModelInstance(RESOURCE_3_NAME, model3));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_3_KEY), 2);

    // when
    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResourceByName(deployment1.getId(), RESOURCE_1_NAME)
        .addDeploymentResourcesByName(deployment1.getId(), Arrays.asList(RESOURCE_2_NAME, RESOURCE_3_NAME)));

    // then
    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 3);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 3);
    verifyQueryResults(query.processDefinitionKey(PROCESS_3_KEY), 3);
  }

  @Test
  public void testSameDeploymentResourceByName() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);
    BpmnModelInstance model2 = createProcessWithUserTask(PROCESS_2_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);

    // second deployment
    model1 = createProcessWithScriptTask(PROCESS_1_KEY);
    model2 = createProcessWithReceiveTask(PROCESS_2_KEY);

    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);

    // when
    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResourceByName(deployment1.getId(), RESOURCE_1_NAME)
        .addDeploymentResourcesByName(deployment1.getId(), Collections.singletonList(RESOURCE_1_NAME)));

    // then
    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 3);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
  }

  @Test
  public void testRedeployOneDeploymentResourcesById() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);
    BpmnModelInstance model2 = createProcessWithUserTask(PROCESS_2_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);

    Resource resource = getResourceByName(deployment1.getId(), RESOURCE_1_NAME);

    // second deployment
    model1 = createProcessWithScriptTask(PROCESS_1_KEY);
    model2 = createProcessWithReceiveTask(PROCESS_2_KEY);

    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);

    // when
    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResourceById(deployment1.getId(), resource.getId()));

    // then
    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 3);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
  }

  @Test
  public void testRedeployMultipleDeploymentResourcesById() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);
    BpmnModelInstance model2 = createProcessWithUserTask(PROCESS_2_KEY);
    BpmnModelInstance model3 = createProcessWithScriptTask(PROCESS_3_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2)
        .addModelInstance(RESOURCE_3_NAME, model3));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_3_KEY), 1);

    Resource resource11 = getResourceByName(deployment1.getId(), RESOURCE_1_NAME);
    Resource resource13 = getResourceByName(deployment1.getId(), RESOURCE_3_NAME);

    // second deployment
    model1 = createProcessWithScriptTask(PROCESS_1_KEY);
    model2 = createProcessWithReceiveTask(PROCESS_2_KEY);
    model3 = createProcessWithUserTask(PROCESS_3_KEY);

    Deployment deployment2 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2)
        .addModelInstance(RESOURCE_3_NAME, model3));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_3_KEY), 2);

    Resource resource21 = getResourceByName(deployment2.getId(), RESOURCE_1_NAME);
    Resource resource23 = getResourceByName(deployment2.getId(), RESOURCE_3_NAME);

    // when (1)
    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResourceById(deployment1.getId(), resource11.getId())
        .addDeploymentResourceById(deployment1.getId(), resource13.getId()));

    // then (1)
    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 3);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_3_KEY), 3);

    // when (2)
    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResourcesById(deployment2.getId(), Arrays.asList(resource21.getId(), resource23.getId())));

    // then (2)
    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 4);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_3_KEY), 4);
  }

  @Test
  public void testRedeployOneAndMultipleDeploymentResourcesById() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);
    BpmnModelInstance model2 = createProcessWithUserTask(PROCESS_2_KEY);
    BpmnModelInstance model3 = createProcessWithScriptTask(PROCESS_3_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2)
        .addModelInstance(RESOURCE_3_NAME, model3));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_3_KEY), 1);

    Resource resource1 = getResourceByName(deployment1.getId(), RESOURCE_1_NAME);
    Resource resource2 = getResourceByName(deployment1.getId(), RESOURCE_2_NAME);
    Resource resource3 = getResourceByName(deployment1.getId(), RESOURCE_3_NAME);

    // second deployment
    model1 = createProcessWithScriptTask(PROCESS_1_KEY);
    model2 = createProcessWithReceiveTask(PROCESS_2_KEY);
    model3 = createProcessWithUserTask(PROCESS_3_KEY);

    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2)
        .addModelInstance(RESOURCE_3_NAME, model3));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_3_KEY), 2);

    // when
    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResourceById(deployment1.getId(), resource1.getId())
        .addDeploymentResourcesById(deployment1.getId(), Arrays.asList(resource2.getId(), resource3.getId())));

    // then
    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 3);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 3);
    verifyQueryResults(query.processDefinitionKey(PROCESS_3_KEY), 3);
  }

  @Test
  public void testRedeploySameDeploymentResourceById() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);
    BpmnModelInstance model2 = createProcessWithUserTask(PROCESS_2_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);

    Resource resource1 = getResourceByName(deployment1.getId(), RESOURCE_1_NAME);

    // second deployment
    model1 = createProcessWithScriptTask(PROCESS_1_KEY);
    model2 = createProcessWithReceiveTask(PROCESS_2_KEY);

    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);

    // when
    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResourceById(deployment1.getId(), resource1.getId())
        .addDeploymentResourcesById(deployment1.getId(), Collections.singletonList(resource1.getId())));

    // then
    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 3);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
  }

  @Test
  public void testRedeployDeploymentResourceByIdAndName() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);
    BpmnModelInstance model2 = createProcessWithUserTask(PROCESS_2_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);

    Resource resource1 = getResourceByName(deployment1.getId(), RESOURCE_1_NAME);
    Resource resource2 = getResourceByName(deployment1.getId(), RESOURCE_2_NAME);

    // second deployment
    model1 = createProcessWithScriptTask(PROCESS_1_KEY);
    model2 = createProcessWithReceiveTask(PROCESS_2_KEY);

    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);

    // when
    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResourceById(deployment1.getId(), resource1.getId())
        .addDeploymentResourceByName(deployment1.getId(), resource2.getName()));

    // then
    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 3);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 3);
  }

  @Test
  public void testRedeployDeploymentResourceByIdAndNameMultiple() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);
    BpmnModelInstance model2 = createProcessWithUserTask(PROCESS_2_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);

    Resource resource1 = getResourceByName(deployment1.getId(), RESOURCE_1_NAME);
    Resource resource2 = getResourceByName(deployment1.getId(), RESOURCE_2_NAME);

    // second deployment
    model1 = createProcessWithScriptTask(PROCESS_1_KEY);
    model2 = createProcessWithReceiveTask(PROCESS_2_KEY);

    testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1)
        .addModelInstance(RESOURCE_2_NAME, model2));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);

    // when
    testRule.deploy(repositoryService
        .createDeployment()
        .addDeploymentResourcesById(deployment1.getId(), Collections.singletonList(resource1.getId()))
        .addDeploymentResourcesByName(deployment1.getId(), Collections.singletonList(resource2.getName())));

    // then
    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 3);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 3);
  }

  @Test
  public void testRedeployFormDifferentDeployments() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-1")
        .addModelInstance(RESOURCE_1_NAME, model1));

    assertThat(repositoryService.getDeploymentResources(deployment1.getId())).hasSize(1);

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);

    // second deployment
    BpmnModelInstance model2 = createProcessWithReceiveTask(PROCESS_2_KEY);

    Deployment deployment2 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-2")
        .addModelInstance(RESOURCE_2_NAME, model2));

    assertThat(repositoryService.getDeploymentResources(deployment2.getId())).hasSize(1);

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);

    // when
    Deployment deployment3 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-3")
        .addDeploymentResources(deployment1.getId())
        .addDeploymentResources(deployment2.getId()));

    assertThat(repositoryService.getDeploymentResources(deployment3.getId())).hasSize(2);

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
  }

  @Test
  public void testRedeployFormDifferentDeploymentsById() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-1")
        .addModelInstance(RESOURCE_1_NAME, model1));

    assertThat(repositoryService.getDeploymentResources(deployment1.getId())).hasSize(1);
    Resource resource1 = getResourceByName(deployment1.getId(), RESOURCE_1_NAME);

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);

    // second deployment
    BpmnModelInstance model2 = createProcessWithReceiveTask(PROCESS_2_KEY);

    Deployment deployment2 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-2")
        .addModelInstance(RESOURCE_2_NAME, model2));

    assertThat(repositoryService.getDeploymentResources(deployment2.getId())).hasSize(1);
    Resource resource2 = getResourceByName(deployment2.getId(), RESOURCE_2_NAME);

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);

    // when
    Deployment deployment3 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-3")
        .addDeploymentResourceById(deployment1.getId(), resource1.getId())
        .addDeploymentResourceById(deployment2.getId(), resource2.getId()));

    assertThat(repositoryService.getDeploymentResources(deployment3.getId())).hasSize(2);

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
  }

  @Test
  public void testRedeployFormDifferentDeploymentsByName() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-1")
        .addModelInstance(RESOURCE_1_NAME, model1));

    assertThat(repositoryService.getDeploymentResources(deployment1.getId())).hasSize(1);

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);

    // second deployment
    BpmnModelInstance model2 = createProcessWithReceiveTask(PROCESS_2_KEY);

    Deployment deployment2 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-2")
        .addModelInstance(RESOURCE_2_NAME, model2));

    assertThat(repositoryService.getDeploymentResources(deployment2.getId())).hasSize(1);

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);

    // when
    Deployment deployment3 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-3")
        .addDeploymentResourceByName(deployment1.getId(), RESOURCE_1_NAME)
        .addDeploymentResourceByName(deployment2.getId(), RESOURCE_2_NAME));

    assertThat(repositoryService.getDeploymentResources(deployment3.getId())).hasSize(2);

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
  }

  @Test
  public void testRedeployFormDifferentDeploymentsByNameAndId() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-1")
        .addModelInstance(RESOURCE_1_NAME, model1));

    assertThat(repositoryService.getDeploymentResources(deployment1.getId())).hasSize(1);
    Resource resource1 = getResourceByName(deployment1.getId(), RESOURCE_1_NAME);

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);

    // second deployment
    BpmnModelInstance model2 = createProcessWithReceiveTask(PROCESS_2_KEY);

    Deployment deployment2 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-2")
        .addModelInstance(RESOURCE_2_NAME, model2));

    assertThat(repositoryService.getDeploymentResources(deployment2.getId())).hasSize(1);

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);

    // when
    Deployment deployment3 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-3")
        .addDeploymentResourceById(deployment1.getId(), resource1.getId())
        .addDeploymentResourceByName(deployment2.getId(), RESOURCE_2_NAME));

    assertThat(repositoryService.getDeploymentResources(deployment3.getId())).hasSize(2);

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
  }

  @Test
  public void testRedeployFormDifferentDeploymentsAddsNewSource() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-1")
        .addModelInstance(RESOURCE_1_NAME, model1));

    assertThat(repositoryService.getDeploymentResources(deployment1.getId())).hasSize(1);

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);

    // second deployment
    BpmnModelInstance model2 = createProcessWithReceiveTask(PROCESS_2_KEY);

    Deployment deployment2 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-2")
        .addModelInstance(RESOURCE_2_NAME, model2));

    assertThat(repositoryService.getDeploymentResources(deployment2.getId())).hasSize(1);

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 1);

    // when
    BpmnModelInstance model3 = createProcessWithUserTask(PROCESS_3_KEY);
    Deployment deployment3 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-3")
        .addDeploymentResources(deployment1.getId())
        .addDeploymentResources(deployment2.getId())
        .addModelInstance(RESOURCE_3_NAME, model3));

    assertThat(repositoryService.getDeploymentResources(deployment3.getId())).hasSize(3);

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_2_KEY), 2);
    verifyQueryResults(query.processDefinitionKey(PROCESS_3_KEY), 1);
  }

  @Test
  public void testRedeployFormDifferentDeploymentsSameResourceName() {
    // given
    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);

    // first deployment
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-1")
        .addModelInstance(RESOURCE_1_NAME, model1));

    // second deployment
    BpmnModelInstance model2 = createProcessWithReceiveTask(PROCESS_2_KEY);

    Deployment deployment2 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-2")
        .addModelInstance(RESOURCE_1_NAME, model2));
    var deploymentBuilder = repositoryService
          .createDeployment()
          .name(DEPLOYMENT_NAME + "-3")
          .addDeploymentResources(deployment1.getId())
          .addDeploymentResources(deployment2.getId());

    // when
    try {
      deploymentBuilder.deploy();
      fail("It should not be possible to deploy different resources with same name.");
    } catch (NotValidException e) {
      // expected
    }
  }

  @Test
  public void testRedeployAndAddNewResourceWithSameName() {
    // given
    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);

    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-1")
        .addModelInstance(RESOURCE_1_NAME, model1));

    // when
    BpmnModelInstance model2 = createProcessWithReceiveTask(PROCESS_2_KEY);
    var deploymentBuilder = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME + "-2")
        .addModelInstance(RESOURCE_1_NAME, model2)
        .addDeploymentResourceByName(deployment1.getId(), RESOURCE_1_NAME);

    try {
      deploymentBuilder.deploy();
      fail("It should not be possible to deploy different resources with same name.");
    } catch (NotValidException e) {
      // expected
    }
  }

  @Test
  public void testRedeployEnableDuplcateChecking() {
    // given
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    BpmnModelInstance model1 = createProcessWithServiceTask(PROCESS_1_KEY);
    Deployment deployment1 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_1_NAME, model1));

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);

    // when
    Deployment deployment2 = testRule.deploy(repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResources(deployment1.getId())
        .enableDuplicateFiltering(true));

    assertThat(deployment2.getId()).isEqualTo(deployment1.getId());

    verifyQueryResults(query.processDefinitionKey(PROCESS_1_KEY), 1);
  }

  @Test
  public void testSimpleProcessApplicationDeployment() {
    // given
    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication();

    BpmnModelInstance model = createProcessWithServiceTask(PROCESS_KEY);
    ProcessApplicationDeployment deployment1 = testRule.deploy(
        repositoryService
            .createDeployment(processApplication.getReference())
            .name(DEPLOYMENT_NAME)
            .addModelInstance(RESOURCE_NAME, model)
            .enableDuplicateFiltering(true));

    Resource resource1 = getResourceByName(deployment1.getId(), RESOURCE_NAME);

    // when
    ProcessApplicationDeployment deployment2 = testRule.deploy(
        repositoryService
            .createDeployment(processApplication.getReference())
            .name(DEPLOYMENT_NAME)
            .addDeploymentResourceById(deployment1.getId(), resource1.getId()));

    // then
    // registration was performed:
    ProcessApplicationRegistration registration = deployment2.getProcessApplicationRegistration();
    Set<String> deploymentIds = registration.getDeploymentIds();
    assertThat(deploymentIds).hasSize(1);
    assertTrue(deploymentIds.contains(deployment2.getId()));
  }

  @Test
  public void testRedeployProcessApplicationDeploymentResumePreviousVersions() {
    // given
    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication();

    // first deployment
    BpmnModelInstance model = createProcessWithServiceTask(PROCESS_KEY);
    ProcessApplicationDeployment deployment1 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, model)
        .enableDuplicateFiltering(true));

    Resource resource1 = getResourceByName(deployment1.getId(), RESOURCE_NAME);

    // second deployment
    model = createProcessWithUserTask(PROCESS_KEY);
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, model)
        .enableDuplicateFiltering(true));

    // when
    ProcessApplicationDeployment deployment3 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name(DEPLOYMENT_NAME)
        .resumePreviousVersions()
        .addDeploymentResourceById(deployment1.getId(), resource1.getId()));

    // then
    // old deployments was resumed
    ProcessApplicationRegistration registration = deployment3.getProcessApplicationRegistration();
    Set<String> deploymentIds = registration.getDeploymentIds();
    assertThat(deploymentIds).hasSize(3);
  }

  @Test
  public void testProcessApplicationDeploymentResumePreviousVersionsByDeploymentName() {
    // given
    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication();

    // first deployment
    BpmnModelInstance model = createProcessWithServiceTask(PROCESS_KEY);
    ProcessApplicationDeployment deployment1 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, model)
        .enableDuplicateFiltering(true));

    Resource resource1 = getResourceByName(deployment1.getId(), RESOURCE_NAME);

    // second deployment
    model = createProcessWithUserTask(PROCESS_KEY);
    testRule.deploy(repositoryService.createDeployment(processApplication.getReference())
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, model)
        .enableDuplicateFiltering(true));

    // when
    ProcessApplicationDeployment deployment3 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name(DEPLOYMENT_NAME)
        .resumePreviousVersions()
        .resumePreviousVersionsBy(ResumePreviousBy.RESUME_BY_DEPLOYMENT_NAME)
        .addDeploymentResourceById(deployment1.getId(), resource1.getId()));

    // then
    // old deployment was resumed
    ProcessApplicationRegistration registration = deployment3.getProcessApplicationRegistration();
    Set<String> deploymentIds = registration.getDeploymentIds();
    assertThat(deploymentIds).hasSize(3);
  }

  // helper ///////////////////////////////////////////////////////////

  protected void verifyQueryResults(Query<?, ?> query, int countExpected) {
    assertThat(query.count()).isEqualTo(countExpected);
  }

  protected Resource getResourceByName(String deploymentId, String resourceName) {
    List<Resource> resources = repositoryService.getDeploymentResources(deploymentId);

    for (Resource resource : resources) {
      if (resource.getName().equals(resourceName)) {
        return resource;
      }
    }

    return null;
  }

  protected BpmnModelInstance createProcessWithServiceTask(String key) {
    return Bpmn.createExecutableProcess(key)
      .startEvent()
      .serviceTask()
        .operatonExpression("${true}")
      .endEvent()
    .done();
  }

  protected BpmnModelInstance createProcessWithUserTask(String key) {
    return Bpmn.createExecutableProcess(key)
      .startEvent()
      .userTask()
      .endEvent()
    .done();
  }

  protected BpmnModelInstance createProcessWithReceiveTask(String key) {
    return Bpmn.createExecutableProcess(key)
      .startEvent()
      .receiveTask()
      .endEvent()
    .done();
  }

  protected BpmnModelInstance createProcessWithScriptTask(String key) {
    return Bpmn.createExecutableProcess(key)
      .startEvent()
      .scriptTask()
        .scriptFormat("javascript")
        .scriptText("return true")
      .userTask()
      .endEvent()
    .done();
  }

}
