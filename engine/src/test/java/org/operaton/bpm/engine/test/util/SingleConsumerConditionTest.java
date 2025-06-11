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
package org.operaton.bpm.engine.test.util;

import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.operaton.bpm.engine.impl.util.SingleConsumerCondition;

class SingleConsumerConditionTest {

  @Test
  @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
  void shouldNotBlockIfSignalAvailable() {
    SingleConsumerCondition condition = new SingleConsumerCondition(Thread.currentThread());

    // given
    condition.signal();

    // then
    condition.await(100000);
  }

  @Test
  @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
  void shouldNotBlockIfSignalAvailableDifferentThread() throws InterruptedException {

    final SingleConsumerCondition condition = new SingleConsumerCondition(Thread.currentThread());

    Thread consumer = new Thread() {
      @Override
      public void run() {
        condition.signal();
      }
    };

    consumer.start();
    consumer.join();

    // then
    condition.await(100000);
  }

  @Test
  void cannotAwaitFromDifferentThread() {
    // given
    SingleConsumerCondition condition = new SingleConsumerCondition(new Thread());

    // when then
    try {
      condition.await(0);
      fail("expected exception");
    }
    catch (RuntimeException e) {
      // expected
    }
  }

  @Test
  void cannotCreateWithNull() {
    try {
      new SingleConsumerCondition(null);
      fail("expected exception");
    }
    catch (IllegalArgumentException e) {
      // expected
    }
  }

}
