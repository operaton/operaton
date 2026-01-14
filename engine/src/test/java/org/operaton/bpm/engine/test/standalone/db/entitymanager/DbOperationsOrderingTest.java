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
package org.operaton.bpm.engine.test.standalone.db.entitymanager;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.cfg.IdGenerator;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.PersistenceSession;
import org.operaton.bpm.engine.impl.db.entitymanager.DbEntityManager;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbEntityOperation;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperation;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Meyer
 *
 */
class DbOperationsOrderingTest {

  protected ExposingDbEntityManager entityManager;

  // setup some entities
  ExecutionEntity execution1;
  ExecutionEntity execution2;
  ExecutionEntity execution3;
  ExecutionEntity execution4;
  ExecutionEntity execution5;
  ExecutionEntity execution6;
  ExecutionEntity execution7;
  ExecutionEntity execution8;

  TaskEntity task1;
  TaskEntity task2;
  TaskEntity task3;
  TaskEntity task4;

  VariableInstanceEntity variable1;
  VariableInstanceEntity variable2;
  VariableInstanceEntity variable3;
  VariableInstanceEntity variable4;


  @BeforeEach
  void setup() {
    TestIdGenerator idGenerator = new TestIdGenerator();
    entityManager = new ExposingDbEntityManager(idGenerator, null);

    execution1 = new ExecutionEntity();
    execution1.setId("101");
    execution2 = new ExecutionEntity();
    execution2.setId("102");
    execution3 = new ExecutionEntity();
    execution3.setId("103");
    execution4 = new ExecutionEntity();
    execution4.setId("104");
    execution5 = new ExecutionEntity();
    execution5.setId("105");
    execution6 = new ExecutionEntity();
    execution6.setId("106");
    execution7 = new ExecutionEntity();
    execution7.setId("107");
    execution8 = new ExecutionEntity();
    execution8.setId("108");

    task1 = new TaskEntity();
    task1.setId("104");
    task2 = new TaskEntity();
    task2.setId("105");
    task3 = new TaskEntity();
    task3.setId("106");
    task4 = new TaskEntity();
    task4.setId("107");

    variable1 = new VariableInstanceEntity();
    variable1.setId("108");
    variable2 = new VariableInstanceEntity();
    variable2.setId("109");
    variable3 = new VariableInstanceEntity();
    variable3.setId("110");
    variable4 = new VariableInstanceEntity();
    variable4.setId("111");
  }

  @Test
  void testInsertSingleEntity() {

    entityManager.insert(execution1);
    entityManager.flushEntityCache();

    List<DbOperation> flush = entityManager.getDbOperationManager().calculateFlush();
    assertThat(flush).hasSize(1);
  }

  @Test
  void testInsertReferenceOrdering() {

    execution2.setParentExecution(execution3);

    entityManager.insert(execution2);
    entityManager.insert(execution3);

    // the parent (3) is inserted before the child (2)
    entityManager.flushEntityCache();
    List<DbOperation> flush = entityManager.getDbOperationManager().calculateFlush();
    assertHappensAfter(execution2, execution3, flush);

  }


  @Test
  void testInsertReferenceOrderingAndIdOrdering() {

    execution2.setParentExecution(execution3);

    entityManager.insert(execution2);
    entityManager.insert(execution3);
    entityManager.insert(execution1);

    // the parent (3) is inserted before the child (2)
    entityManager.flushEntityCache();
    List<DbOperation> flush = entityManager.getDbOperationManager().calculateFlush();
    assertHappensAfter(execution2, execution3, flush);
    assertHappensAfter(execution3, execution1, flush);
    assertHappensAfter(execution2, execution1, flush);

  }

  @Test
  void testInsertReferenceOrderingMultipleTrees() {

    // tree1
    execution3.setParentExecution(execution4);
    execution2.setParentExecution(execution4);
    execution5.setParentExecution(execution3);

    // tree2
    execution1.setParentExecution(execution8);

    entityManager.insert(execution8);
    entityManager.insert(execution6);
    entityManager.insert(execution2);
    entityManager.insert(execution5);
    entityManager.insert(execution1);
    entityManager.insert(execution4);
    entityManager.insert(execution7);
    entityManager.insert(execution3);

    // the parent (3) is inserted before the child (2)
    entityManager.flushEntityCache();
    List<DbOperation> insertOperations = entityManager.getDbOperationManager().calculateFlush();
    assertHappensAfter(execution3, execution4, insertOperations);
    assertHappensAfter(execution2, execution4, insertOperations);
    assertHappensAfter(execution5, execution3, insertOperations);
    assertHappensAfter(execution1, execution8, insertOperations);

  }

  @Test
  void testDeleteReferenceOrdering() {
    // given
    execution1.setParentExecution(execution2);
    entityManager.getDbEntityCache().putPersistent(execution1);
    entityManager.getDbEntityCache().putPersistent(execution2);

    // when deleting the entities
    entityManager.delete(execution1);
    entityManager.delete(execution2);

    entityManager.flushEntityCache();

    // then the flush is based on the persistent relationships
    List<DbOperation> deleteOperations = entityManager.getDbOperationManager().calculateFlush();
    assertHappensBefore(execution1, execution2, deleteOperations);
  }

  @Test
  void testDeleteReferenceOrderingAfterTransientUpdate() {
    // given
    execution1.setParentExecution(execution2);
    entityManager.getDbEntityCache().putPersistent(execution1);
    entityManager.getDbEntityCache().putPersistent(execution2);

    // when reverting the relation in memory
    execution1.setParentExecution(null);
    execution2.setParentExecution(execution1);

    // and deleting the entities
    entityManager.delete(execution1);
    entityManager.delete(execution2);

    entityManager.flushEntityCache();

    // then the flush is based on the persistent relationships
    List<DbOperation> deleteOperations = entityManager.getDbOperationManager().calculateFlush();
    assertHappensBefore(execution1, execution2, deleteOperations);
  }

  protected void assertHappensAfter(DbEntity entity1, DbEntity entity2, List<DbOperation> operations) {
    int idx1 = indexOfEntity(entity1, operations);
    int idx2 = indexOfEntity(entity2, operations);
    assertThat(idx1).as("operation for %s should be executed after operation for %s".formatted(entity1, entity2)).isGreaterThan(idx2);
  }

  protected void assertHappensBefore(DbEntity entity1, DbEntity entity2, List<DbOperation> operations) {
    int idx1 = indexOfEntity(entity1, operations);
    int idx2 = indexOfEntity(entity2, operations);
    assertThat(idx1).as("operation for %s should be executed before operation for %s".formatted(entity1, entity2)).isLessThan(idx2);
  }

  protected int indexOfEntity(DbEntity entity, List<DbOperation> operations) {
    for (int i = 0; i < operations.size(); i++) {
      if(entity == ((DbEntityOperation) operations.get(i)).getEntity()) {
        return i;
      }
    }
    return -1;
  }

  @Test
  void testInsertIdOrdering() {

    entityManager.insert(execution1);
    entityManager.insert(execution2);

    entityManager.flushEntityCache();
    List<DbOperation> insertOperations = entityManager.getDbOperationManager().calculateFlush();
    assertHappensAfter(execution2, execution1, insertOperations);
  }

  public static class ExposingDbEntityManager extends DbEntityManager {

    public ExposingDbEntityManager(IdGenerator idGenerator, PersistenceSession persistenceSession) {
      super(idGenerator, persistenceSession);
    }

    /**
     * Expose this method for test purposes
     */
    @Override
    public void flushEntityCache() {
      super.flushEntityCache();
    }
  }


}
