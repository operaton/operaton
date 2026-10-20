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
package org.operaton.bpm.engine.impl.externaltask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryTopicBuilder;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.impl.QueryOrderingProperty;
import org.operaton.bpm.engine.impl.cmd.FetchExternalTasksCmd;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;

import static java.util.Objects.requireNonNull;

/**
 * @author Thorben Lindhauer
 * @author Christopher Zell
 *
 */
public class ExternalTaskQueryTopicBuilderImpl implements ExternalTaskQueryTopicBuilder {

  protected CommandExecutor commandExecutor;

  protected @Nullable String workerId;
  protected int maxTasks;
  /**
   * Indicates that priority is enabled.
   */
  protected boolean usePriority;

  protected List<QueryOrderingProperty> orderingProperties;

  protected Map<String, TopicFetchInstruction> instructions;

  protected @Nullable TopicFetchInstruction currentInstruction;

  /**
   * All args constructor.
   */
  public ExternalTaskQueryTopicBuilderImpl(CommandExecutor commandExecutor,
                                           @Nullable String workerId,
                                           int maxTasks,
                                           boolean usePriority,
                                           List<QueryOrderingProperty> orderingProperties,
                                           Map<String, TopicFetchInstruction> instructions,
                                           @Nullable TopicFetchInstruction currentInstruction) {
    this.commandExecutor = commandExecutor;
    this.workerId = workerId;
    this.maxTasks = maxTasks;
    this.usePriority = usePriority;
    this.orderingProperties = orderingProperties;
    this.instructions = instructions;
    this.currentInstruction = currentInstruction;
  }

  /**
   * Constructor using priority & createTime.
   */
  public ExternalTaskQueryTopicBuilderImpl(CommandExecutor commandExecutor,
                                           @Nullable String workerId,
                                           int maxTasks,
                                           boolean usePriority,
                                           List<QueryOrderingProperty> orderingProperties) {
    this(commandExecutor, workerId, maxTasks, usePriority, orderingProperties, new HashMap<>(), null);
  }

  /**
   * Constructor using priority.
   */
  public ExternalTaskQueryTopicBuilderImpl(CommandExecutor commandExecutor,
                                           @Nullable String workerId,
                                           int maxTasks,
                                           boolean usePriority) {
    this(commandExecutor, workerId, maxTasks, usePriority, new ArrayList<>(), new HashMap<>(), null);
  }

  /**
   * Copy constructor
   */
  @SuppressWarnings("unused")
  public ExternalTaskQueryTopicBuilderImpl(ExternalTaskQueryTopicBuilderImpl builder) {
    this(
        builder.commandExecutor,
        builder.workerId,
        builder.maxTasks,
        builder.usePriority,
        builder.orderingProperties,
        builder.instructions,
        builder.currentInstruction
    );
  }

  private TopicFetchInstruction getCurrentInstruction() {
    return requireNonNull(currentInstruction, "No topic specified. Call topic() before setting additional parameters.");
  }

  @Override
  public List<LockedExternalTask> execute() {
    submitCurrentInstruction();
    return commandExecutor.execute(
        new FetchExternalTasksCmd(workerId, maxTasks, instructions, usePriority, orderingProperties));
  }

  @Override
  public ExternalTaskQueryTopicBuilder topic(String topicName, long lockDuration) {
    submitCurrentInstruction();
    currentInstruction = new TopicFetchInstruction(topicName, lockDuration);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder variables(String @Nullable... variables) {
    // don't use plain List.of since this returns an instance of a different list class
    // that is private and may mess mybatis queries up
    if (variables != null) {
      getCurrentInstruction().setVariablesToFetch(new ArrayList<>(List.of(variables)));
    }
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder variables(List<String> variables) {
    getCurrentInstruction().setVariablesToFetch(variables);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder processInstanceVariableEquals(Map<String, Object> variables) {
    getCurrentInstruction().setFilterVariables(variables);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder processInstanceVariableEquals(String name, Object value) {
    getCurrentInstruction().addFilterVariable(name, value);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder businessKey(String businessKey) {
    getCurrentInstruction().setBusinessKey(businessKey);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder processDefinitionId(String processDefinitionId) {
    getCurrentInstruction().setProcessDefinitionId(processDefinitionId);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder processDefinitionIdIn(String... processDefinitionIds) {
    getCurrentInstruction().setProcessDefinitionIds(processDefinitionIds);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder processDefinitionKey(String processDefinitionKey) {
    getCurrentInstruction().setProcessDefinitionKey(processDefinitionKey);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder processDefinitionKeyIn(String... processDefinitionKeys) {
    getCurrentInstruction().setProcessDefinitionKeys(processDefinitionKeys);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder processDefinitionVersionTag(String processDefinitionVersionTag) {
    getCurrentInstruction().setProcessDefinitionVersionTag(processDefinitionVersionTag);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder withoutTenantId() {
    getCurrentInstruction().setTenantIds(null);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder tenantIdIn(String... tenantIds) {
    getCurrentInstruction().setTenantIds(tenantIds);
    return this;
  }

  protected void submitCurrentInstruction() {
    if (currentInstruction != null) {
      this.instructions.put(currentInstruction.getTopicName(), currentInstruction);
    }
  }

  @Override
  public ExternalTaskQueryTopicBuilder enableCustomObjectDeserialization() {
    getCurrentInstruction().setDeserializeVariables(true);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder localVariables() {
    getCurrentInstruction().setLocalVariables(true);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder includeExtensionProperties() {
    getCurrentInstruction().setIncludeExtensionProperties(true);
    return this;
  }

}
