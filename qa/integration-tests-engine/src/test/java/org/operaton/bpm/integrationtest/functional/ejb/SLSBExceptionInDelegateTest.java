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
package org.operaton.bpm.integrationtest.functional.ejb;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.integrationtest.functional.ejb.beans.SLSBClientDelegate;
import org.operaton.bpm.integrationtest.functional.ejb.beans.SLSBThrowExceptionDelegate;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

/**
 * Testcase verifying that if an exception is thrown inside an EJB the original
 * exception reaches the caller
 *
 * @author Ronny Bräunlich
 *
 */
@ExtendWith(ArquillianExtension.class)
public class SLSBExceptionInDelegateTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment().addClass(SLSBThrowExceptionDelegate.class).addClass(SLSBClientDelegate.class)
        .addAsResource("org/operaton/bpm/integrationtest/functional/ejb/SLSBExceptionInDelegateTest.testOriginalExceptionFromEjbReachesCaller.bpmn20.xml")
        .addAsResource("org/operaton/bpm/integrationtest/functional/ejb/SLSBExceptionInDelegateTest.callProcess.bpmn20.xml");
  }

  @Test
  public void testOriginalExceptionFromEjbReachesCaller() {
      runtimeService.startProcessInstanceByKey("callProcessWithExceptionFromEjb");
      Job job = managementService.createJobQuery().processDefinitionKey("testProcessEjbWithException").singleResult();
      managementService.setJobRetries(job.getId(), 1);

      waitForJobExecutorToProcessAllJobs();

      Incident incident = runtimeService.createIncidentQuery().activityId("servicetask1").singleResult();
      assertThat(incident.getIncidentMessage()).isEqualTo("error");
  }

}
