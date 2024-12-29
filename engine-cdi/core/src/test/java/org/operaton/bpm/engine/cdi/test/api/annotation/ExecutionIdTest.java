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
package org.operaton.bpm.engine.cdi.test.api.annotation;

import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.annotation.ExecutionIdLiteral;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.test.Deployment;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import java.util.Set;

import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 
 * @author Daniel Meyer
 */
@ExtendWith(ArquillianExtension.class)
class ExecutionIdTest extends CdiProcessEngineTestCase {

  @Test
  @Deployment
  void executionIdInjectableByName() {
    getBeanInstance(BusinessProcess.class).startProcessByKey("keyOfTheProcess");
    String processInstanceId = (String) getBeanInstance("processInstanceId");
    assertThat(processInstanceId).isNotNull();
    String executionId = (String) getBeanInstance("executionId");
    assertThat(executionId).isNotNull();

    assertThat(executionId).isEqualTo(processInstanceId);
  }

  @Test
  @Deployment
  void executionIdInjectableByQualifier() {
    getBeanInstance(BusinessProcess.class).startProcessByKey("keyOfTheProcess");
    
    Set<Bean<?>> beans = beanManager.getBeans(String.class, new ExecutionIdLiteral());    
    Bean<String> bean = (Bean<java.lang.String>) beanManager.resolve(beans);
    
    CreationalContext<String> ctx = beanManager.createCreationalContext(bean);
    String executionId = (String) beanManager.getReference(bean, String.class, ctx);
    assertThat(executionId).isNotNull();
    
    String processInstanceId = (String) getBeanInstance("processInstanceId");
    assertThat(processInstanceId).isNotNull();

    assertThat(executionId).isEqualTo(processInstanceId);
  }
  
}
