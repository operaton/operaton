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
package org.operaton.bpm.spring.boot.starter.test.nonpa.service;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.spring.boot.starter.test.nonpa.jpa.domain.TestEntity;
import org.operaton.bpm.spring.boot.starter.test.nonpa.jpa.repository.TestEntityRepository;

import java.util.HashMap;
import java.util.Map;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
class TransactionalTestServiceImpl implements TransactionalTestService {

  private final TestEntityRepository testEntityRepository;
  private final RuntimeService runtimeService;

  @Autowired
  public TransactionalTestServiceImpl(TestEntityRepository testEntityRepository, RuntimeService runtimeService) {
      this.testEntityRepository = testEntityRepository;
      this.runtimeService = runtimeService;
  }

  @Override
  public ProcessInstance doOk() {
    TestEntity entity = new TestEntity();
    entity.setText("text");
    TestEntity testEntity = testEntityRepository.save(entity);
    Map<String, Object> variables = new HashMap<>();
    variables.put("test", testEntity);
    return runtimeService.startProcessInstanceByKey("TestProcess", variables);
  }

  @Override
  @Transactional(TxType.REQUIRES_NEW)
  public void doThrowing() {
    doOk();
    throw new IllegalStateException();
  }
}
