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
package org.operaton.bpm.engine.test.bpmn.event.timer;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;


/**
 * Test timer expression according to act-865
 * 
 * @author Saeid Mirzaei
 */

public class TimeExpressionTest extends PluggableProcessEngineTest {
	
	  
	  private Date testExpression(String timeExpression) {
		    // Set the clock fixed
		    HashMap<String, Object> variables1 = new HashMap<>();
		    variables1.put("dueDate", timeExpression);
		  
		    // After process start, there should be timer created    
		    ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("intermediateTimerEventExample", variables1);
      assertThat(managementService.createJobQuery().processInstanceId(pi1.getId()).count()).isEqualTo(1);


		    List<Job> jobs = managementService.createJobQuery().executable().list();
      assertThat(jobs.size()).isEqualTo(1);
		    return jobs.get(0).getDuedate();
	  }
	  
	  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testExpression.bpmn20.xml"})	  
  @Test
	  public void testTimeExpressionComplete() {
		    Date dt = new Date();
		    
		    Date dueDate = testExpression(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(dt));
      assertThat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(dueDate)).isEqualTo(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(dt));		    	  
	  }
	  
	  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testExpression.bpmn20.xml"})	  
  @Test
	  public void testTimeExpressionWithoutSeconds() {
		    Date dt = new Date();
		    
		    Date dueDate = testExpression(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").format(dt));
      assertThat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").format(dueDate)).isEqualTo(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").format(dt));
	  }
	  
	  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testExpression.bpmn20.xml"})	 
  @Test
	  public void testTimeExpressionWithoutMinutes() {
		    Date dt = new Date();

		    Date dueDate = testExpression(new SimpleDateFormat("yyyy-MM-dd'T'HH").format(new Date()));
      assertThat(new SimpleDateFormat("yyyy-MM-dd'T'HH").format(dueDate)).isEqualTo(new SimpleDateFormat("yyyy-MM-dd'T'HH").format(dt));
	  }
	  
	  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testExpression.bpmn20.xml"})	  
  @Test
	  public void testTimeExpressionWithoutTime() {
		    Date dt = new Date();

		    Date dueDate = testExpression(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
      assertThat(new SimpleDateFormat("yyyy-MM-dd").format(dueDate)).isEqualTo(new SimpleDateFormat("yyyy-MM-dd").format(dt));
	  }
	
	  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testExpression.bpmn20.xml"})	  
  @Test
	  public void testTimeExpressionWithoutDay() {
		    Date dt = new Date();

		    Date dueDate = testExpression(new SimpleDateFormat("yyyy-MM").format(new Date()));
      assertThat(new SimpleDateFormat("yyyy-MM").format(dueDate)).isEqualTo(new SimpleDateFormat("yyyy-MM").format(dt));
	  }
	  
	  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testExpression.bpmn20.xml"})	  
  @Test
	  public void testTimeExpressionWithoutMonth() {
		    Date dt = new Date();
		    
		    Date dueDate = testExpression(new SimpleDateFormat("yyyy").format(new Date()));
      assertThat(new SimpleDateFormat("yyyy").format(dueDate)).isEqualTo(new SimpleDateFormat("yyyy").format(dt));
	  }
}
