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
package org.operaton.bpm.example.invoice;

import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.application.PostDeploy;
import org.operaton.bpm.application.ProcessApplication;
import org.operaton.bpm.application.impl.JakartaServletProcessApplication;
import org.operaton.bpm.application.impl.ServletProcessApplication;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.repository.DeploymentBuilder;

import jakarta.servlet.annotation.WebListener;

/**
 * Process Application exposing this application's resources the process engine.
 */
@ProcessApplication(name = "InvoiceProcessApplication")
@WebListener
public class InvoiceProcessApplication extends JakartaServletProcessApplication {

  /**
   * In a @PostDeploy hook you can interact with the process engine and access
   * the processes the application has deployed.
   */
  @PostDeploy
  public void startFirstProcess(ProcessEngine processEngine) {
    InvoiceApplicationHelper.startFirstProcess(processEngine);
  }

  @Override
  public void createDeployment(String processArchiveName, DeploymentBuilder deploymentBuilder) {
    ProcessEngine processEngine = BpmPlatform.getProcessEngineService().getProcessEngine("default");
    InvoiceApplicationHelper.createDeployment(processArchiveName, processEngine, getProcessApplicationClassloader(), getReference());
  }
}
