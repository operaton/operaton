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
package org.operaton.bpm.engine.test.api.authorization.externaltask;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
public class FetchExternalTaskAuthorizationTest extends AuthorizationTest {

  public static final String WORKER_ID = "workerId";
  public static final long LOCK_TIME = 10000L;

  protected String instance1Id;
  protected String instance2Id;

  @Override
  @BeforeEach
  public void setUp() {
    testRule.deploy(
        "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml");

    instance1Id = startProcessInstanceByKey("oneExternalTaskProcess").getId();
    instance2Id = startProcessInstanceByKey("twoExternalTaskProcess").getId();
    super.setUp();
  }

  @Test
  void testFetchWithoutAuthorization() {
    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("externalTaskTopic", LOCK_TIME)
      .execute();

    // then
    assertThat(tasks).isEmpty();
  }

  @Test
  void testFetchWithReadOnProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, instance1Id, userId, READ);

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("externalTaskTopic", LOCK_TIME)
      .execute();

    // then
    assertThat(tasks).isEmpty();
  }

  @Test
  void testFetchWithUpdateOnProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, instance1Id, userId, UPDATE);

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("externalTaskTopic", LOCK_TIME)
      .execute();

    // then
    assertThat(tasks).isEmpty();
  }

  @Test
  void testFetchWithReadAndUpdateOnProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, instance1Id, userId, READ, UPDATE);

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("externalTaskTopic", LOCK_TIME)
      .execute();

    // then
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance1Id);
  }

  @Test
  void testFetchWithReadInstanceOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, "oneExternalTaskProcess", userId, READ_INSTANCE);

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("externalTaskTopic", LOCK_TIME)
      .execute();

    // then
    assertThat(tasks).isEmpty();
  }

  @Test
  void testFetchWithUpdateInstanceOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, "oneExternalTaskProcess", userId, UPDATE_INSTANCE);

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("externalTaskTopic", LOCK_TIME)
      .execute();

    // then
    assertThat(tasks).isEmpty();
  }

  @Test
  void testFetchWithReadAndUpdateInstanceOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, "oneExternalTaskProcess", userId, READ_INSTANCE, UPDATE_INSTANCE);

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("externalTaskTopic", LOCK_TIME)
      .execute();

    // then
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance1Id);
  }

  @Test
  void testFetchWithReadOnProcessInstanceAndUpdateInstanceOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, instance1Id, userId, READ);
    createGrantAuthorization(PROCESS_DEFINITION, "oneExternalTaskProcess", userId, UPDATE_INSTANCE);

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("externalTaskTopic", LOCK_TIME)
      .execute();

    // then
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance1Id);
  }

  @Test
  void testFetchWithUpdateOnProcessInstanceAndReadInstanceOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, instance1Id, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, "oneExternalTaskProcess", userId, READ_INSTANCE);

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("externalTaskTopic", LOCK_TIME)
      .execute();

    // then
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance1Id);
  }

  @Test
  void testFetchWithReadAndUpdateOnAnyProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ, UPDATE);

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("externalTaskTopic", LOCK_TIME)
      .execute();

    // then
    assertThat(tasks).hasSize(2);
  }

  @Test
  void testFetchWithMultipleMatchingAuthorizations() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ, UPDATE);
    createGrantAuthorization(PROCESS_INSTANCE, instance1Id, userId, READ, UPDATE);

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("externalTaskTopic", LOCK_TIME)
      .execute();

    // then
    assertThat(tasks).hasSize(2);
  }

  @Test
  void testQueryWithReadAndUpdateInstanceOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_INSTANCE, UPDATE_INSTANCE);

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("externalTaskTopic", LOCK_TIME)
      .execute();

    // then
    assertThat(tasks).hasSize(2);
  }

  @Test
  void testQueryWithReadProcessInstanceAndUpdateInstanceOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, UPDATE_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, instance1Id, userId, READ);

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("externalTaskTopic", LOCK_TIME)
      .execute();

    // then
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance1Id);
  }

  @Test
  void shouldLockNoTaskForProcessDefinitionWithRevokedUpdateInstancePermission() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_INSTANCE, UPDATE_INSTANCE);
    createRevokeAuthorization(PROCESS_DEFINITION, "oneExternalTaskProcess", userId, UPDATE_INSTANCE);

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("externalTaskTopic", LOCK_TIME)
      .execute();

    // then
    assertThat(tasks).hasSize(1);
  }
}
