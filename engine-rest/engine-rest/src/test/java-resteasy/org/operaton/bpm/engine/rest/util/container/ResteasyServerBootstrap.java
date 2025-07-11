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

import java.util.Properties;
import jakarta.ws.rs.core.Application;
import org.jboss.resteasy.plugins.server.netty.NettyJaxrsServer;

public class ResteasyServerBootstrap extends EmbeddedServerBootstrap {

  protected NettyJaxrsServer server;

  public ResteasyServerBootstrap(Application application) {
    super(application);
  }

  @Override
  public void stop() {
    server.stop();
  }

  @Override
  protected void startServerInternal() {
    server.start();
  }

  @Override
  protected void setupServer(Application application) {
    Properties serverProperties = readProperties();
    int port = Integer.parseInt(serverProperties.getProperty(PORT_PROPERTY));

    server = new NettyJaxrsServer();
    server.setRootResourcePath(ROOT_RESOURCE_PATH);
    server.setPort(port);

    server.getDeployment().setApplication(application);
  }

}
