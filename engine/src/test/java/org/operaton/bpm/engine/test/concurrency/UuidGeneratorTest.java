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
package org.operaton.bpm.engine.test.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

import com.fasterxml.uuid.EthernetAddress;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Daniel Meyer
 *
 */
class UuidGeneratorTest {

  private static final int THREAD_COUNT = 10;
  private static final int LOOP_COUNT = 10000;

  @Test
  void testMultithreaded() throws InterruptedException {
    final List<Thread> threads = new ArrayList<>();

    final TimeBasedGenerator timeBasedGenerator = Generators.timeBasedGenerator(EthernetAddress.fromInterface());
    final ConcurrentSkipListSet<String> generatedIds = new ConcurrentSkipListSet<>();
    final ConcurrentSkipListSet<String> duplicatedIds = new ConcurrentSkipListSet<>();

    for (int i = 0; i < THREAD_COUNT; i++) {
      Thread thread = new Thread(() -> {
        for (int j = 0;j < LOOP_COUNT;j++) {

          String id = timeBasedGenerator.generate().toString();
          boolean wasAdded = generatedIds.add(id);
          if (!wasAdded) {
            duplicatedIds.add(id);
          }
        }
      });
      threads.add(thread);
      thread.start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    assertThat(generatedIds).hasSize(THREAD_COUNT * LOOP_COUNT);
    assertThat(duplicatedIds).isEmpty();
  }
}
