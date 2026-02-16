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
package org.operaton.bpm.engine.rest.util.container;

import java.net.BindException;
import java.util.concurrent.TimeUnit;
import jakarta.ws.rs.core.Application;
import org.awaitility.core.ConditionTimeoutException;
import static org.awaitility.Awaitility.await;

public abstract class EmbeddedServerBootstrap extends AbstractServerBootstrap {

  protected Application application;

  protected abstract void setupServer(Application application);
  protected abstract void startServerInternal() throws Exception;

  protected EmbeddedServerBootstrap(Application application) {
    this.application = application;
    setupServer(application);
  }

  @Override
  protected void startServer(int startUpRetries) {
    try {
      await()
        .atMost((startUpRetries + 1) * 500L, TimeUnit.MILLISECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .ignoreExceptionsMatching(e -> e instanceof BindException || e.getCause() instanceof BindException)
        .until(() -> {
          try {
            startServerInternal();
            return true;
          } catch (Exception e) {
            if (e instanceof BindException || e.getCause() instanceof BindException) {
              stop();
              setupServer(application);
              throw e;
            }
            throw new ServerBootstrapException(e);
          }
        });
    } catch (ConditionTimeoutException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Exception) {
        throw new ServerBootstrapException((Exception) cause);
      }
      throw new ServerBootstrapException(e);
    }
  }
}
