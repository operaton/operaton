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
package org.operaton.bpm.engine.spring.test.components;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.runtime.ProcessInstance;

import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Josh Long
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:org/operaton/bpm/engine/spring/test/components/ProcessStartingBeanPostProcessorTest-context.xml")
class ProcessStartingBeanPostProcessorTest {

	private static final Logger LOG = Logger.getLogger(ProcessStartingBeanPostProcessorTest.class.getName());

	@Autowired
	private ProcessEngine processEngine;

	@Autowired
	private ProcessInitiatingPojo processInitiatingPojo;

	@Autowired
	private RepositoryService repositoryService;

  @BeforeEach
  void before() {
	  repositoryService.createDeployment()
	    .addClasspathResource("org/operaton/bpm/engine/spring/test/autodeployment/autodeploy.b.bpmn20.xml")
	    .addClasspathResource("org/operaton/bpm/engine/spring/test/components/waiter.bpmn20.xml")
	    .deploy();
	}

  @AfterEach
  void after() {
    for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
    processEngine.close();
    processEngine = null;
    processInitiatingPojo = null;
    repositoryService = null;
  }

  @Test
  void returnedProcessInstance() {
		String processInstanceId = this.processInitiatingPojo.startProcessA(22);
    assertThat(processInstanceId).as("the process instance id should not be null").isNotNull();
	}

  @Test
  void reflectingSideEffects() {
    assertThat(this.processInitiatingPojo).as("the processInitiatingPojo mustn't be null.").isNotNull();

		this.processInitiatingPojo.reset();

    assertThat(this.processInitiatingPojo.getMethodState()).isZero();

		this.processInitiatingPojo.startProcess(53);

    assertThat(this.processInitiatingPojo.getMethodState()).isOne();
	}

  @Test
  void usingBusinessKey() {
		long id = 5;
		String businessKey = "usersKey" + System.currentTimeMillis();
		ProcessInstance pi = processInitiatingPojo.enrollCustomer(businessKey, id);
    assertThat(pi.getBusinessKey()).as("the business key of the resultant ProcessInstance should match " +
      "the one specified through the AOP-intercepted method").isEqualTo(businessKey);

	}

  @Test
  void launchingProcessInstance() {
		long id = 343;
		String processInstance = processInitiatingPojo.startProcessA(id);
		Long customerId = (Long) processEngine.getRuntimeService().getVariable(processInstance, "customerId");
    assertThat((Long) id).as("the process variable should both exist and be equal to the value given, " + id).isEqualTo(customerId);
		LOG.info("the customerId from the ProcessInstance is " + customerId);
    assertThat(processInstance).as("processInstance can't be null").isNotNull();
    assertThat(customerId).as("the variable should be non-null").isNotNull();
	}
}
