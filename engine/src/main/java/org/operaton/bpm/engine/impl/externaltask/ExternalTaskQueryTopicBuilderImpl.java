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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryTopicBuilder;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.impl.QueryOrderingProperty;
import org.operaton.bpm.engine.impl.cmd.FetchExternalTasksCmd;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;

/**
 * @author Thorben Lindhauer
 * @author Christopher Zell
 *
 */
public class ExternalTaskQueryTopicBuilderImpl implements ExternalTaskQueryTopicBuilder {

  protected CommandExecutor commandExecutor;

  protected String workerId;
  protected int maxTasks;
  /**
   * Indicates that priority is enabled.
   */
  protected boolean usePriority;

  protected List<QueryOrderingProperty> orderingProperties;

  protected Map<String, TopicFetchInstruction> instructions;

  protected TopicFetchInstruction currentInstruction;

  /**
   * All args constructor.
   */
  public ExternalTaskQueryTopicBuilderImpl(CommandExecutor commandExecutor,
                                           String workerId,
                                           int maxTasks,
                                           boolean usePriority,
                                           List<QueryOrderingProperty> orderingProperties,
                                           Map<String, TopicFetchInstruction> instructions,
                                           TopicFetchInstruction currentInstruction) {
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
                                           String workerId,
                                           int maxTasks,
                                           boolean usePriority,
                                           List<QueryOrderingProperty> orderingProperties) {
    this(commandExecutor, workerId, maxTasks, usePriority, orderingProperties, new HashMap<>(), null);
  }

  /**
   * Constructor using priority.
   */
  public ExternalTaskQueryTopicBuilderImpl(CommandExecutor commandExecutor,
                                           String workerId,
                                           int maxTasks,
                                           boolean usePriority) {
    this(commandExecutor, workerId, maxTasks, usePriority, new ArrayList<>(), new HashMap<>(), null);
  }

  /**
   * Copy constructor
   */
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
  public ExternalTaskQueryTopicBuilder variables(String... variables) {
    // don't use plain Arrays.asList since this returns an instance of a different list class
    // that is private and may mess mybatis queries up
    if (variables != null) {
      currentInstruction.setVariablesToFetch(new ArrayList<>(Arrays.asList(variables)));
    }
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder variables(List<String> variables) {
    currentInstruction.setVariablesToFetch(variables);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder processInstanceVariableEquals(Map<String, Object> variables) {
    currentInstruction.setFilterVariables(variables);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder processInstanceVariableEquals(String name, Object value) {
    currentInstruction.addFilterVariable(name, value);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder businessKey(String businessKey) {
    currentInstruction.setBusinessKey(businessKey);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder processDefinitionId(String processDefinitionId) {
    currentInstruction.setProcessDefinitionId(processDefinitionId);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder processDefinitionIdIn(String... processDefinitionIds) {
    currentInstruction.setProcessDefinitionIds(processDefinitionIds);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder processDefinitionKey(String processDefinitionKey) {
    currentInstruction.setProcessDefinitionKey(processDefinitionKey);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder processDefinitionKeyIn(String... processDefinitionKeys) {
    currentInstruction.setProcessDefinitionKeys(processDefinitionKeys);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder processDefinitionVersionTag(String processDefinitionVersionTag) {
    currentInstruction.setProcessDefinitionVersionTag(processDefinitionVersionTag);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder withoutTenantId() {
    currentInstruction.setTenantIds(null);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder tenantIdIn(String... tenantIds) {
    currentInstruction.setTenantIds(tenantIds);
    return this;
  }

  protected void submitCurrentInstruction() {
    if (currentInstruction != null) {
      this.instructions.put(currentInstruction.getTopicName(), currentInstruction);
    }
  }

  @Override
  public ExternalTaskQueryTopicBuilder enableCustomObjectDeserialization() {
    currentInstruction.setDeserializeVariables(true);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder localVariables() {
    currentInstruction.setLocalVariables(true);
    return this;
  }

  @Override
  public ExternalTaskQueryTopicBuilder includeExtensionProperties() {
    currentInstruction.setIncludeExtensionProperties(true);
    return this;
  }

}
