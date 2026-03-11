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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

import org.operaton.bpm.container.impl.jboss.extension.BpmPlatformExtension;
import org.operaton.bpm.container.impl.jboss.extension.ModelConstants;
import org.operaton.bpm.container.impl.jboss.extension.SubsystemAttributeDefinitons;
import org.operaton.bpm.container.impl.jboss.extension.handler.JobExecutorAdd;
import org.operaton.bpm.container.impl.jboss.extension.handler.JobExecutorRemove;

public final class JobExecutorDefinition extends SimpleResourceDefinition {

  static final JobExecutorDefinition INSTANCE = new JobExecutorDefinition();

  private JobExecutorDefinition() {
    super(new Parameters(BpmPlatformExtension.JOB_EXECUTOR_PATH,
      BpmPlatformExtension.getResourceDescriptionResolver(ModelConstants.JOB_EXECUTOR))
      .setAddHandler(JobExecutorAdd.INSTANCE)
      .setRemoveHandler(JobExecutorRemove.INSTANCE));
  }

  @Override
  public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
    for (AttributeDefinition attr : SubsystemAttributeDefinitons.JOB_EXECUTOR_ATTRIBUTES) {
      if (!attr.getFlags().contains(AttributeAccess.Flag.RESTART_ALL_SERVICES)) {
        throw new IllegalStateException("Attribute %s was not marked as reload required: %s".formatted(
            attr.getName(), resourceRegistration.getPathAddress()));
      }
      resourceRegistration.registerReadOnlyAttribute(attr, null);
    }
  }

  @Override
  public void registerChildren(ManagementResourceRegistration resourceRegistration) {
    resourceRegistration.registerSubModel(JobAcquisitionDefinition.INSTANCE);
  }

}