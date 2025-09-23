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
package org.operaton.bpm.engine.test.assertions.helpers;


import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.test.junit5.DeploymentExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.operaton.bpm.engine.test.assertions.bpmn.AbstractAssertions.reset;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(ProcessEngineExtension.class)
@ExtendWith(DeploymentExtension.class)
public abstract class ProcessAssertTestCase {

  @AfterEach
  public void tearDown() {
    reset();
  }

  protected void expect(Failure fail) {
    expect(fail, AssertionError.class);
  }

  protected void expect(Failure fail, String messageContent) {
    try {
      fail.when();
    } catch (AssertionError e) {
      if (e.getMessage().contains(messageContent)) {
        System.out.println("AssertionError caught with message '%s' and expected content '%s'".formatted(e.getMessage(), messageContent));
        return;
      } else {
        fail("Error message should include '" + messageContent + "' but was: " + e.getMessage());
      }
    }
  }

  @SafeVarargs
  protected final void expect(Failure fail, Class<? extends Throwable>... exception) {
    try {
      fail.when();
    } catch (Throwable e) {
      for (Class<? extends Throwable> t : exception) {
        if (t.isAssignableFrom(e.getClass())) {
          System.out.println(("caught " + e.getClass().getSimpleName() + " of expected type " + t.getSimpleName() + " with message '%s'").formatted(e.getMessage()));
          return;
        }
      }
      throw (RuntimeException) e;
    }
    fail("expected one of " + Lists.newArrayList(exception) + " to be thrown, but did not see any");
  }

}
