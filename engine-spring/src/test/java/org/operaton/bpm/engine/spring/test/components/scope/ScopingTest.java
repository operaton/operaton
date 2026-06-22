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
package org.operaton.bpm.engine.spring.test.components.scope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.StringUtils;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.spring.test.components.ProcessInitiatingPojo;
import org.operaton.bpm.engine.task.Task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * tests the scoped beans
 *
 * @author Josh Long
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:org/operaton/bpm/engine/spring/test/components/ScopingTests-context.xml")
public class ScopingTest {

	@Autowired
	private ProcessInitiatingPojo processInitiatingPojo;

	private static final Logger LOGGER = Logger.getLogger(ScopingTest.class.getName());

	@Autowired
	private ProcessEngine processEngine;

	private RepositoryService repositoryService;
	private TaskService taskService;

  @BeforeEach
  void before() {
	  this.repositoryService = this.processEngine.getRepositoryService();
		this.taskService = this.processEngine.getTaskService();

		repositoryService.createDeployment()
		  .addClasspathResource("org/operaton/bpm/engine/spring/test/autodeployment/autodeploy.b.bpmn20.xml")
		  .addClasspathResource("org/operaton/bpm/engine/spring/test/components/waiter.bpmn20.xml")
		  .addClasspathResource("org/operaton/bpm/engine/spring/test/components/spring-component-waiter.bpmn20.xml")
		  .deploy();
	}

  @AfterEach
  void after() {
	  for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
	    repositoryService.deleteDeployment(deployment.getId(), true);
	  }
	  processEngine.close();
	  processEngine = null;
	  repositoryService = null;
	  taskService = null;
	  processInitiatingPojo = null;
	}

	public static final long CUSTOMER_ID_PROC_VAR_VALUE = 343;

	public static final String CUSTOMER_ID_PROC_VAR_NAME = "customerId";

	/**
	 * this code instantiates a business process that in turn delegates to a few Spring beans that in turn inject a process scoped object, {@link StatefulObject}.
	 *
	 * @return the StatefulObject that was injected across different components, that all share the same state.
	 * @throws Throwable if anything goes wrong
	 */
	private StatefulObject run() throws Throwable {
		LOGGER.info("----------------------------------------------");
		Map<String, Object> vars = new HashMap<>();
		vars.put(CUSTOMER_ID_PROC_VAR_NAME, CUSTOMER_ID_PROC_VAR_VALUE);
		ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceByKey("component-waiter", vars);
		StatefulObject scopedObject = (StatefulObject) processEngine.getRuntimeService().getVariable(processInstance.getId(), "scopedTarget.c1");
    assertThat(scopedObject).as("the scopedObject can't be null").isNotNull();
    assertThat(StringUtils.hasText(scopedObject.getName())).as("the 'name' property can't be null.").isTrue();
    assertThat(scopedObject.getVisitedCount()).isEqualTo(2);

		// the process has paused
		String procId = processInstance.getProcessInstanceId();

		List<Task> tasks = taskService.createTaskQuery().executionId(procId).list();

    assertThat(tasks).as("there should be 1 (one) task enqueued at this point.").hasSize(1);

		Task t = tasks.iterator().next();

		this.taskService.claim(t.getId(), "me");

		LOGGER.info("sleeping for 10 seconds while a user performs his task. " +
				"The first transaction has committed. A new one will start in 10 seconds");

		await().atMost(10, TimeUnit.SECONDS)
			.pollInterval(250, TimeUnit.MILLISECONDS)
			.until(() -> {
				Task task = taskService.createTaskQuery().taskId(t.getId()).singleResult();
				return task != null && "me".equals(task.getAssignee());
			});

		this.taskService.complete(t.getId());

		scopedObject = (StatefulObject) processEngine.getRuntimeService().getVariable(processInstance.getId(), "scopedTarget.c1");
    assertThat(scopedObject.getVisitedCount()).isEqualTo(3);

    assertThat(scopedObject.getCustomerId()).as("the customerId injected should " +
      "be what was given as a processVariable parameter.").isEqualTo(ScopingTest.CUSTOMER_ID_PROC_VAR_VALUE) ;
		return scopedObject;
	}

  @Test
  void usingAnInjectedScopedProxy() throws Throwable {
		LOGGER.info("Running 'component-waiter' process instance with scoped beans.");
		StatefulObject one = run();
		StatefulObject two = run();
		assertThat(one.getName()).isNotSameAs(two.getName());
    assertThat(two.getVisitedCount()).isEqualTo(one.getVisitedCount());
	}

  @Test
  void startingAProcessWithScopedBeans() {
		this.processInitiatingPojo.startScopedProcess(3243);
		ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery()
			.processDefinitionKey("component-waiter")
			.singleResult();
		assertThat(processInstance).isNotNull();
	}


}
