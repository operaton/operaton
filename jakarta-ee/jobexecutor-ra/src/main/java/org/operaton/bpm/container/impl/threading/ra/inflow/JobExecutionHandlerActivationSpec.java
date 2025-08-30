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
package org.operaton.bpm.container.impl.threading.ra.inflow;

import java.io.Serial;
import java.io.Serializable;
import jakarta.resource.spi.Activation;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;


/**
 *
 * @author Daniel Meyer
 */
@Activation(
  messageListeners = {JobExecutionHandler.class}
)
public class JobExecutionHandlerActivationSpec implements ActivationSpec, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private transient ResourceAdapter ra;
  /** Please check #CAM-9811  */
  private String dummyPojo;

  @Override
  public void validate() throws InvalidPropertyException {
    // nothing to do (the endpoint has no activation properties)
  }

  @Override
  public ResourceAdapter getResourceAdapter() {
    return ra;
  }

  @Override
  public void setResourceAdapter(ResourceAdapter ra) {
    this.ra = ra;
  }

  public void setDummyPojo(String dummyPojo) {
    this.dummyPojo = dummyPojo;
  }

  public String getDummyPojo() {
    return dummyPojo;
  }

}
