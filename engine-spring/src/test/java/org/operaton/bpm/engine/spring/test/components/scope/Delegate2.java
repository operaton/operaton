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

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Josh Long
 * @since 5,3
 */

@SuppressWarnings("unused")
public class Delegate2 implements JavaDelegate {

  private static final Logger LOG = Logger.getLogger(Delegate2.class.getName());

	@Autowired private StatefulObject statefulObject;

  @Override
  public void execute(DelegateExecution execution) {

		this.statefulObject.increment();

    assertThat(this.statefulObject).as("the 'scopedCustomer' reference can't be null").isNotNull();
    assertThat(this.statefulObject.getName()).as("the 'scopedCustomer.name' property should be non-null, since it was set in a previous delegate bound to this very thread").isNotNull();
		LOG.info( "the 'uuid' value retrieved from the ScopedCustomer#name property is '" +  this.statefulObject.getName()+ "' in "+getClass().getName());
	}
}
