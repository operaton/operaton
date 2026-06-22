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
package org.operaton.bpm.engine.test.jobexecutor;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redeploy process definition and assert that no new job definitions were created.
 *
 * @author Philipp Ossler
 *
 */
@Parameterized
@ExtendWith(ProcessEngineExtension.class)
public class JobDefinitionRedeploymentTest {

  @Parameters(name = "process definition = {0}")
  public static Collection<Object[]> data() {
    return List.of(new Object[][] {
        { "org/operaton/bpm/engine/test/jobexecutor/JobDefinitionDeploymentTest.testTimerStartEvent.bpmn20.xml" },
        { "org/operaton/bpm/engine/test/jobexecutor/JobDefinitionDeploymentTest.testTimerBoundaryEvent.bpmn20.xml" },
        { "org/operaton/bpm/engine/test/jobexecutor/JobDefinitionDeploymentTest.testMultipleTimerBoundaryEvents.bpmn20.xml" },
        { "org/operaton/bpm/engine/test/jobexecutor/JobDefinitionDeploymentTest.testEventBasedGateway.bpmn20.xml" },
        { "org/operaton/bpm/engine/test/jobexecutor/JobDefinitionDeploymentTest.testTimerIntermediateEvent.bpmn20.xml" },
        { "org/operaton/bpm/engine/test/jobexecutor/JobDefinitionDeploymentTest.testAsyncContinuation.bpmn20.xml" },
        { "org/operaton/bpm/engine/test/jobexecutor/JobDefinitionDeploymentTest.testAsyncContinuationOfMultiInstance.bpmn20.xml" },
        { "org/operaton/bpm/engine/test/jobexecutor/JobDefinitionDeploymentTest.testAsyncContinuationOfActivityWrappedInMultiInstance.bpmn20.xml" }
    });
  }

  @Parameter
  public String processDefinitionResource;

  ManagementService managementService;
  RepositoryService repositoryService;
  RuntimeService runtimeService;
  ProcessEngineConfigurationImpl processEngineConfiguration;

  @TestTemplate
  void testJobDefinitionsAfterRedeploment() {

    // initially there are no job definitions:
    assertThat(managementService.createJobDefinitionQuery().count()).isZero();

    // initial deployment
    String deploymentId = repositoryService.createDeployment()
                            .addClasspathResource(processDefinitionResource)
                            .deploy()
                            .getId();

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition).isNotNull();

    // this parses the process and created the Job definitions:
    List<JobDefinition> jobDefinitions = managementService.createJobDefinitionQuery().list();
    Set<String> jobDefinitionIds = getJobDefinitionIds(jobDefinitions);

    // now clear the cache:
    processEngineConfiguration.getDeploymentCache().discardProcessDefinitionCache();

    // if we start an instance of the process, the process will be parsed again:
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());

    // no new definitions were created
    assertThat(managementService.createJobDefinitionQuery().count()).isEqualTo(jobDefinitions.size());

    // the job has the correct definitionId set:
    List<Job> jobs = managementService.createJobQuery().list();
    for (Job job : jobs) {
      assertThat(jobDefinitionIds).contains(job.getJobDefinitionId());
    }

    // delete the deployment
    repositoryService.deleteDeployment(deploymentId, true);
  }

  protected Set<String> getJobDefinitionIds(List<JobDefinition> jobDefinitions) {
    Set<String> definitionIds = new HashSet<>();
    for (JobDefinition definition : jobDefinitions) {
      definitionIds.add(definition.getId());
    }
    return definitionIds;
  }

}
