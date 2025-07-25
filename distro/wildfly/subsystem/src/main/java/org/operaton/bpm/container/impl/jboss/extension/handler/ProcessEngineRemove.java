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
package org.operaton.bpm.container.impl.jboss.extension.handler;

import org.operaton.bpm.container.impl.jboss.service.ServiceNames;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;


/**
 * Provides the description and the implementation of the process-engine#remove operation.
 *
 * @author Daniel Meyer
 * @author Christian Lipphardt
 */
public class ProcessEngineRemove extends AbstractRemoveStepHandler {

  public static final ProcessEngineRemove INSTANCE = new ProcessEngineRemove();

  @Override
  protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
    String suffix = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();
    ServiceName name = ServiceNames.forManagedProcessEngine(suffix);
    context.removeService(name);
  }

}
