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
package org.operaton.bpm.integrationtest.functional.jpa;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;

import static org.assertj.core.api.Assertions.assertThat;

@Named
@RequestScoped
public class PersistenceDelegateBean implements JavaDelegate {

  @PersistenceContext
  private EntityManager em;

  private SomeEntity entity;

  private boolean invoked = false;
  private boolean entityManaged = false;

  @Override
  public void execute(DelegateExecution execution) throws Exception {

    // we assert that the entity manager contains the entity
    // this means that we obtain the same entity manager we used to
    // persist the entity before starting the process

    assertThat(em.contains(entity)).isTrue();
    invoked = true;
    entityManaged = em.contains(entity);
  }

  public void setEntity(SomeEntity entity) {
    this.entity = entity;
  }

  public boolean isInvoked() {
    return invoked;
  }

  public boolean isEntityManaged() {
    return entityManaged;
  }
}
