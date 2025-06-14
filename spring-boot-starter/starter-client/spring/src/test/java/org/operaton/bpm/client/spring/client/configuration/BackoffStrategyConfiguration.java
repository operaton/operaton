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
package org.operaton.bpm.client.spring.client.configuration;

import org.operaton.bpm.client.backoff.BackoffStrategy;
import org.operaton.bpm.client.task.ExternalTask;

import java.util.List;

import org.springframework.context.annotation.Bean;

public class BackoffStrategyConfiguration {

  @Bean
  @SuppressWarnings("unused")
  public BackoffStrategy backoffStrategy() {
    return new BackoffStrategy() {
      @Override
      @SuppressWarnings("java:S1186")
      public void reconfigure(List<ExternalTask> list) {
      }

      @Override
      public long calculateBackoffTime() {
        return 0;
      }
    };
  }

}
