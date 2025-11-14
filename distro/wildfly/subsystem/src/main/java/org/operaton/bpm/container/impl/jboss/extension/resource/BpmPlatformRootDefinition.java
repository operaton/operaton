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
package org.operaton.bpm.container.impl.jboss.extension.resource;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

import org.operaton.bpm.container.impl.jboss.extension.BpmPlatformExtension;
import org.operaton.bpm.container.impl.jboss.extension.handler.BpmPlatformSubsystemAdd;
import org.operaton.bpm.container.impl.jboss.extension.handler.BpmPlatformSubsystemRemove;

public final class BpmPlatformRootDefinition extends SimpleResourceDefinition {

  public static final BpmPlatformRootDefinition INSTANCE = new BpmPlatformRootDefinition();

  private BpmPlatformRootDefinition() {
    super(new Parameters(BpmPlatformExtension.SUBSYSTEM_PATH,
        BpmPlatformExtension.getResourceDescriptionResolver())
        .setAddHandler(BpmPlatformSubsystemAdd.INSTANCE)
        .setRemoveHandler(BpmPlatformSubsystemRemove.INSTANCE));
  }

  @Override
  public void registerOperations(ManagementResourceRegistration resourceRegistration) {
    super.registerOperations(resourceRegistration);

    resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
  }

  @Override
  public void registerChildren(ManagementResourceRegistration resourceRegistration) {
    resourceRegistration.registerSubModel(JobExecutorDefinition.INSTANCE);
    resourceRegistration.registerSubModel(ProcessEngineDefinition.INSTANCE);
  }
}
