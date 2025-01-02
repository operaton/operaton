/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.migration.MigrationPlanBuilder;
import org.operaton.bpm.engine.impl.cmd.CreateMigrationPlanCmd;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.migration.MigrationInstructionBuilder;
import org.operaton.bpm.engine.migration.MigrationInstructionsBuilder;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.impl.VariableMapImpl;

/**
 * @author Thorben Lindhauer
 *
 */
public class MigrationPlanBuilderImpl implements MigrationInstructionBuilder, MigrationInstructionsBuilder {

  protected CommandExecutor commandExecutor;

  protected String sourceProcessDefinitionId;
  protected String targetProcessDefinitionId;
  protected List<MigrationInstructionImpl> explicitMigrationInstructions;

  protected boolean mapEqualActivities = false;
  protected boolean updateEventTriggersForGeneratedInstructions = false;
  protected VariableMap variables;

  public MigrationPlanBuilderImpl(CommandExecutor commandExecutor, String sourceProcessDefinitionId,
      String targetProcessDefinitionId) {
    this.commandExecutor = commandExecutor;
    this.sourceProcessDefinitionId = sourceProcessDefinitionId;
    this.targetProcessDefinitionId = targetProcessDefinitionId;
    this.explicitMigrationInstructions = new ArrayList<>();
  }

  @Override
  public MigrationInstructionsBuilder mapEqualActivities() {
    this.mapEqualActivities = true;
    return this;
  }

  @Override
  public MigrationPlanBuilder setVariables(Map<String, ?> variables) {
    if (variables instanceof VariableMapImpl impl) {
      this.variables = impl;
    } else if (variables != null) {
      this.variables = new VariableMapImpl(new HashMap<>(variables));
    }
    return this;
  }

  @Override
  public MigrationInstructionBuilder mapActivities(String sourceActivityId, String targetActivityId) {
    this.explicitMigrationInstructions.add(
      new MigrationInstructionImpl(sourceActivityId, targetActivityId)
    );
    return this;
  }

  @Override
  public MigrationInstructionBuilder updateEventTrigger() {
    explicitMigrationInstructions
      .get(explicitMigrationInstructions.size() - 1)
      .setUpdateEventTrigger(true);
    return this;
  }

  @Override
  public MigrationInstructionsBuilder updateEventTriggers() {
    this.updateEventTriggersForGeneratedInstructions = true;
    return this;
  }

  public String getSourceProcessDefinitionId() {
    return sourceProcessDefinitionId;
  }

  public String getTargetProcessDefinitionId() {
    return targetProcessDefinitionId;
  }

  public boolean isMapEqualActivities() {
    return mapEqualActivities;
  }

  public VariableMap getVariables() {
    return variables;
  }

  public boolean isUpdateEventTriggersForGeneratedInstructions() {
    return updateEventTriggersForGeneratedInstructions;
  }

  public List<MigrationInstructionImpl> getExplicitMigrationInstructions() {
    return explicitMigrationInstructions;
  }

  @Override
  public MigrationPlan build() {
    return commandExecutor.execute(new CreateMigrationPlanCmd(this));
  }

}
