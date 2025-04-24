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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentQuery;
import org.operaton.bpm.engine.repository.ProcessApplicationDeployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Tom Baeyens
 * @author Ingo Richtsmeier
 */
@ExtendWith(ProcessEngineExtension.class)
class DeploymentQueryTest {

  private String deploymentOneId;
  private String deploymentTwoId;

  RepositoryService repositoryService;

  @BeforeEach
  void setUp() {
    deploymentOneId = repositoryService
      .createDeployment()
      .name("org/operaton/bpm/engine/test/repository/one.bpmn20.xml")
      .addClasspathResource("org/operaton/bpm/engine/test/repository/one.bpmn20.xml")
      .source(ProcessApplicationDeployment.PROCESS_APPLICATION_DEPLOYMENT_SOURCE)
      .deploy()
      .getId();

    deploymentTwoId = repositoryService
      .createDeployment()
      .name("org/operaton/bpm/engine/test/repository/two_.bpmn20.xml")
      .addClasspathResource("org/operaton/bpm/engine/test/repository/two.bpmn20.xml")
      .deploy()
      .getId();


  }

  @AfterEach
  void tearDown() {

    repositoryService.deleteDeployment(deploymentOneId, true);
    repositoryService.deleteDeployment(deploymentTwoId, true);
  }

  @Test
  void testQueryNoCriteria() {
    DeploymentQuery query = repositoryService.createDeploymentQuery();
    assertThat(query.list()).hasSize(2);
    assertThat(query.count()).isEqualTo(2);

    try {
      query.singleResult();
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Query return 2 results instead of max 1");
    }
  }

  @Test
  void testQueryByDeploymentId() {
    DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentId(deploymentOneId);
    assertThat(query.singleResult()).isNotNull();
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  void testQueryByInvalidDeploymentId() {
    DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentId("invalid");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
    var deploymentQuery = repositoryService.createDeploymentQuery();

    try {
      deploymentQuery.deploymentId(null);
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Deployment id is null");
    }
  }

  @Test
  void testQueryByName() {
    DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentName("org/operaton/bpm/engine/test/repository/two_.bpmn20.xml");
    assertThat(query.singleResult()).isNotNull();
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  void testQueryByInvalidName() {
    DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentName("invalid");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
    var deploymentQuery = repositoryService.createDeploymentQuery();

    try {
      deploymentQuery.deploymentName(null);
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("deploymentName is null");
    }
  }

  @Test
  void testQueryByNameLike() {
    DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentNameLike("%operaton%");
    assertThat(query.list()).hasSize(2);
    assertThat(query.count()).isEqualTo(2);

    query = repositoryService.createDeploymentQuery().deploymentNameLike("%two\\_%");
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.singleResult().getName()).isEqualTo("org/operaton/bpm/engine/test/repository/two_.bpmn20.xml");
  }

  @Test
  void testQueryByInvalidNameLike() {
    DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentNameLike("invalid");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
    var deploymentQuery = repositoryService.createDeploymentQuery();

    try {
      deploymentQuery.deploymentNameLike(null);
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("deploymentNameLike is null");
    }
  }

  @Test
  void testQueryByDeploymentBefore() {
    Date later = DateTimeUtil.now().plus(10 * 3600).toDate();
    Date earlier = DateTimeUtil.now().minus(10 * 3600).toDate();

    long count = repositoryService.createDeploymentQuery().deploymentBefore(later).count();
    assertThat(count).isEqualTo(2);

    count = repositoryService.createDeploymentQuery().deploymentBefore(earlier).count();
    assertThat(count).isZero();
    var deploymentQuery = repositoryService.createDeploymentQuery();

    try {
      deploymentQuery.deploymentBefore(null);
      fail("Exception expected");
    } catch (NullValueException e) {
      // expected
    }
  }

  @Test
  void testQueryDeploymentAfter() {
    Date later = DateTimeUtil.now().plus(10 * 3600).toDate();
    Date earlier = DateTimeUtil.now().minus(10 * 3600).toDate();

    long count = repositoryService.createDeploymentQuery().deploymentAfter(later).count();
    assertThat(count).isZero();

    count = repositoryService.createDeploymentQuery().deploymentAfter(earlier).count();
    assertThat(count).isEqualTo(2);
    var deploymentQuery = repositoryService.createDeploymentQuery();

    try {
      deploymentQuery.deploymentAfter(null);
      fail("Exception expected");
    } catch (NullValueException e) {
      // expected
    }
  }

  @Test
  void testQueryBySource() {
    DeploymentQuery query = repositoryService
        .createDeploymentQuery()
        .deploymentSource(ProcessApplicationDeployment.PROCESS_APPLICATION_DEPLOYMENT_SOURCE);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  void testQueryByNullSource() {
    DeploymentQuery query = repositoryService
        .createDeploymentQuery()
        .deploymentSource(null);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  void testQueryByInvalidSource() {
    DeploymentQuery query = repositoryService
        .createDeploymentQuery()
        .deploymentSource("invalid");

    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
  }

  @Test
  void testQueryDeploymentBetween() {
    Date later = DateTimeUtil.now().plus(10 * 3600).toDate();
    Date earlier = DateTimeUtil.now().minus(10 * 3600).toDate();

    long count = repositoryService
        .createDeploymentQuery()
        .deploymentAfter(earlier)
        .deploymentBefore(later).count();
    assertThat(count).isEqualTo(2);

    count = repositoryService
      .createDeploymentQuery()
      .deploymentAfter(later)
      .deploymentBefore(later)
      .count();
    assertThat(count).isZero();

    count = repositoryService
      .createDeploymentQuery()
      .deploymentAfter(earlier)
      .deploymentBefore(earlier)
      .count();
    assertThat(count).isZero();

    count = repositoryService
        .createDeploymentQuery()
        .deploymentAfter(later)
        .deploymentBefore(earlier)
        .count();
    assertThat(count).isZero();
  }

  @Test
  void testVerifyDeploymentProperties() {
    List<Deployment> deployments = repositoryService.createDeploymentQuery()
      .orderByDeploymentName()
      .asc()
      .list();

    Deployment deploymentOne = deployments.get(0);
    assertThat(deploymentOne.getName()).isEqualTo("org/operaton/bpm/engine/test/repository/one.bpmn20.xml");
    assertThat(deploymentOne.getId()).isEqualTo(deploymentOneId);
    assertThat(deploymentOne.getSource()).isEqualTo(ProcessApplicationDeployment.PROCESS_APPLICATION_DEPLOYMENT_SOURCE);
    assertThat(deploymentOne.getTenantId()).isNull();

    Deployment deploymentTwo = deployments.get(1);
    assertThat(deploymentTwo.getName()).isEqualTo("org/operaton/bpm/engine/test/repository/two_.bpmn20.xml");
    assertThat(deploymentTwo.getId()).isEqualTo(deploymentTwoId);
    assertThat(deploymentTwo.getSource()).isNull();
    assertThat(deploymentTwo.getTenantId()).isNull();
  }

  @Test
  void testQuerySorting() {
    assertThat(repositoryService.createDeploymentQuery()
        .orderByDeploymentName()
        .asc()
        .list()).hasSize(2);

    assertThat(repositoryService.createDeploymentQuery()
        .orderByDeploymentId()
        .asc()
        .list()).hasSize(2);

    assertThat(repositoryService.createDeploymentQuery()
        .orderByDeploymentTime()
        .asc()
        .list()).hasSize(2);
  }

}
