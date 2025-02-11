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
package org.operaton.bpm.engine.test.bpmn.tasklistener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Joram Barrez
 * @author Falko Menge <falko.menge@camunda.com>
 * @author Frederik Heremans
 */
public class CustomTaskAssignmentTest extends PluggableProcessEngineTest {
  
  @Before
  public void setUp() {

    
    identityService.saveUser(identityService.newUser("kermit"));
    identityService.saveUser(identityService.newUser("fozzie"));
    identityService.saveUser(identityService.newUser("gonzo"));
    
    identityService.saveGroup(identityService.newGroup("management"));
    
    identityService.createMembership("kermit", "management");
  }
  
  @After
  public void tearDown() {
    identityService.deleteUser("kermit");
    identityService.deleteUser("fozzie");
    identityService.deleteUser("gonzo");
    identityService.deleteGroup("management");

  }
  
  @Deployment
  @Test
  public void testCandidateGroupAssignment() {
    runtimeService.startProcessInstanceByKey("customTaskAssignment");
    assertThat(taskService.createTaskQuery().taskCandidateGroup("management").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateUser("fozzie").count()).isEqualTo(0);
  }
  
  @Deployment
  @Test
  public void testCandidateUserAssignment() {
    runtimeService.startProcessInstanceByKey("customTaskAssignment");
    assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateUser("fozzie").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateUser("gonzo").count()).isEqualTo(0);
  }
  
  @Deployment
  @Test
  public void testAssigneeAssignment() {
    runtimeService.startProcessInstanceByKey("setAssigneeInListener");
    assertNotNull(taskService.createTaskQuery().taskAssignee("kermit").singleResult());
    assertThat(taskService.createTaskQuery().taskAssignee("fozzie").count()).isEqualTo(0);
    assertThat(taskService.createTaskQuery().taskAssignee("gonzo").count()).isEqualTo(0);
  }
  
  @Deployment
  @Test
  public void testOverwriteExistingAssignments() {
    runtimeService.startProcessInstanceByKey("overrideAssigneeInListener");
    assertNotNull(taskService.createTaskQuery().taskAssignee("kermit").singleResult());
    assertThat(taskService.createTaskQuery().taskAssignee("fozzie").count()).isEqualTo(0);
    assertThat(taskService.createTaskQuery().taskAssignee("gonzo").count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testOverwriteExistingAssignmentsFromVariable() {
    // prepare variables
    Map<String, String> assigneeMappingTable = new HashMap<>();
    assigneeMappingTable.put("fozzie", "gonzo");
   
    Map<String, Object> variables = new HashMap<>();
    variables.put("assigneeMappingTable", assigneeMappingTable);

    // start process instance
    runtimeService.startProcessInstanceByKey("customTaskAssignment", variables);
    
    // check task lists
    assertNotNull(taskService.createTaskQuery().taskAssignee("gonzo").singleResult());
    assertThat(taskService.createTaskQuery().taskAssignee("fozzie").count()).isEqualTo(0);
    assertThat(taskService.createTaskQuery().taskAssignee("kermit").count()).isEqualTo(0);
  }
  
  @Deployment
  @Test
  public void testReleaseTask() {
    runtimeService.startProcessInstanceByKey("releaseTaskProcess");
    
    Task task = taskService.createTaskQuery().taskAssignee("fozzie").singleResult();
    assertNotNull(task);
    String taskId = task.getId();
    
    // Set assignee to null
    taskService.setAssignee(taskId, null);
    
    task = taskService.createTaskQuery().taskAssignee("fozzie").singleResult();
    assertNull(task);
    
    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertNotNull(task);
    assertNull(task.getAssignee());
  }

}
