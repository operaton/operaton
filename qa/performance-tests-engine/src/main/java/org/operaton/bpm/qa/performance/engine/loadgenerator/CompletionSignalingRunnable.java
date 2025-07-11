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
package org.operaton.bpm.qa.performance.engine.loadgenerator;

import java.util.concurrent.CountDownLatch;

/**
 * @author Daniel Meyer
 *
 */
public class CompletionSignalingRunnable implements Runnable {

  protected final Runnable delegate;
  protected final CountDownLatch latch;

  public CompletionSignalingRunnable(Runnable delegate, CountDownLatch latch) {
    this.delegate = delegate;
    this.latch = latch;
  }

  @Override
  public void run() {
    try {
        delegate.run();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    finally {
      latch.countDown();
    }
  }

  public static CompletionSignalingRunnable wrap(Runnable runnable, CountDownLatch latch) {
    return new CompletionSignalingRunnable(runnable, latch);
  }
}
