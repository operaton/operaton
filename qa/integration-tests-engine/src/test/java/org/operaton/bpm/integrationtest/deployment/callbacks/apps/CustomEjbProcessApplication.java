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
package org.operaton.bpm.integrationtest.deployment.callbacks.apps;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.application.PostDeploy;
import org.operaton.bpm.application.PreUndeploy;
import org.operaton.bpm.application.ProcessApplication;
import org.operaton.bpm.application.ProcessApplicationInterface;
import org.operaton.bpm.engine.ProcessEngine;

import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Custom {@link org.operaton.bpm.application.impl.JakartaEjbProcessApplication} with PA lifecycle callbacks
 *
 * @author Daniel Meyer
 *
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@ProcessApplication
@Local(ProcessApplicationInterface.class)
// Using fully-qualified class name instead of import statement to allow for automatic Jakarta transformation
public class CustomEjbProcessApplication extends org.operaton.bpm.application.impl.JakartaEjbProcessApplication {

  @PostDeploy
  public void postDeploy(ProcessEngine processEngine) {
    assertThat(processEngine).isNotNull();
  }

  @PreUndeploy
  public void preUnDeploy(ProcessEngine processEngine) {
    assertThat(processEngine).isNotNull();
  }

}
