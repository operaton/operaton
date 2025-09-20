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

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.runtime.ProcessInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Josh Long
 * @since 5.3
 */

@SuppressWarnings("unused")
public class Delegate1 implements JavaDelegate, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(Delegate1.class);

    @Autowired
    private ProcessInstance processInstance;

    @Autowired
    private StatefulObject statefulObject;

  @Override
  public void execute(DelegateExecution execution) {

        String pid = this.processInstance.getId();

        LOG.info("the processInstance#id is {}", pid);

    assertThat(statefulObject).as("the 'scopedCustomer' reference can't be null").isNotNull();
        String uuid = UUID.randomUUID().toString();
        statefulObject.setName(uuid);
        LOG.info("the 'uuid' value given to the ScopedCustomer#name property is '{}' in {}", uuid, getClass().getName());

        this.statefulObject.increment();
    }

  @Override
  public void afterPropertiesSet() {
    assertThat(this.processInstance).as("the processInstance must not be null").isNotNull();

    }
}
