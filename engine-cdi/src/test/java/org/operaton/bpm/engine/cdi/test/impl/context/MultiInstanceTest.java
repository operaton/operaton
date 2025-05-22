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
package org.operaton.bpm.engine.cdi.test.impl.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessInstanceWithVariablesImpl;
import org.operaton.bpm.engine.test.Deployment;

/**
 * @author Daniel Meyer
 *
 */
@RunWith(Arquillian.class)
public class MultiInstanceTest extends CdiProcessEngineTestCase {

  @Test
  @Deployment
  public void testParallelMultiInstanceServiceTasks() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);
    businessProcess.setVariable("list", List.of("1", "2"));
    var process = (ProcessInstanceWithVariablesImpl) businessProcess.startProcessByKey("miParallelScriptTask");
    
    assertThat(process.isEnded()).isFalse();
    assertThat(process.isSuspended()).isFalse();
    assertThat(process.getExecutionEntity().isActive()).isTrue();
    assertThat(process.getVariables()).containsEntry("list", List.of("1", "2"));
    assertThat(process.getExecutionEntity().getCurrentActivityId()).isEqualTo("waitState");
  }

}
