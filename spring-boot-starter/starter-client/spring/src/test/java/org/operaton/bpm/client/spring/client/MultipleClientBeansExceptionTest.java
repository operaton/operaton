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
package org.operaton.bpm.client.spring.client;

import org.operaton.bpm.client.spring.client.configuration.MultipleClientBeansConfiguration;
import org.operaton.bpm.client.spring.exception.SpringExternalTaskClientException;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
class MultipleClientBeansExceptionTest {

  @Test
  void shouldThrowException() {
    Class<?> clazz = MultipleClientBeansConfiguration.class;
    assertThatThrownBy(() -> new AnnotationConfigApplicationContext(clazz))
        .isInstanceOf(SpringExternalTaskClientException.class)
        .hasMessageContaining("Multiple matching client bean candidates have been found");
  }

}
