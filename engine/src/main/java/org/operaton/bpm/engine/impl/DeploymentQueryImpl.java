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
package org.operaton.bpm.engine.impl;

import java.util.Date;
import java.util.List;

import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.util.CompareUtil;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentQuery;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Ingo Richtsmeier
 */
public class DeploymentQueryImpl extends AbstractQuery<DeploymentQuery, Deployment> implements DeploymentQuery {

  protected String deploymentId;
  protected String name;
  protected String nameLike;
  protected boolean sourceQueryParamEnabled;
  protected String source;
  protected Date deploymentBefore;
  protected Date deploymentAfter;

  protected boolean isTenantIdSet;
  protected String[] tenantIds;
  protected boolean includeDeploymentsWithoutTenantId;

  public DeploymentQueryImpl() {
  }

  public DeploymentQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public DeploymentQueryImpl deploymentId(String deploymentId) {
    ensureNotNull("Deployment id", deploymentId);
    this.deploymentId = deploymentId;
    return this;
  }

  @Override
  public DeploymentQueryImpl deploymentName(String deploymentName) {
    ensureNotNull("deploymentName", deploymentName);
    this.name = deploymentName;
    return this;
  }

  @Override
  public DeploymentQueryImpl deploymentNameLike(String nameLike) {
    ensureNotNull("deploymentNameLike", nameLike);
    this.nameLike = nameLike;
    return this;
  }

  @Override
  public DeploymentQuery deploymentSource(String source) {
    sourceQueryParamEnabled = true;
    this.source = source;
    return this;
  }

  @Override
  public DeploymentQuery deploymentBefore(Date before) {
    ensureNotNull("deploymentBefore", before);
    this.deploymentBefore = before;
    return this;
  }

  @Override
  public DeploymentQuery deploymentAfter(Date after) {
    ensureNotNull("deploymentAfter", after);
    this.deploymentAfter = after;
    return this;
  }

  @Override
  public DeploymentQuery tenantIdIn(String... tenantIds) {
    ensureNotNull("tenantIds", (Object[]) tenantIds);
    this.tenantIds = tenantIds;
    isTenantIdSet = true;
    return this;
  }

  @Override
  public DeploymentQuery withoutTenantId() {
    isTenantIdSet = true;
    this.tenantIds = null;
    return this;
  }

  @Override
  public DeploymentQuery includeDeploymentsWithoutTenantId() {
    this.includeDeploymentsWithoutTenantId  = true;
    return this;
  }

  @Override
  protected boolean hasExcludingConditions() {
    return super.hasExcludingConditions() || CompareUtil.areNotInAscendingOrder(deploymentAfter, deploymentBefore);
  }

  //sorting ////////////////////////////////////////////////////////

  @Override
  public DeploymentQuery orderByDeploymentId() {
    return orderBy(DeploymentQueryProperty.DEPLOYMENT_ID);
  }

  @Override
  public DeploymentQuery orderByDeploymentTime() {
    return orderBy(DeploymentQueryProperty.DEPLOY_TIME);
  }

  @Override
  public DeploymentQuery orderByDeploymentName() {
    return orderBy(DeploymentQueryProperty.DEPLOYMENT_NAME);
  }

  @Override
  public DeploymentQuery orderByTenantId() {
    return orderBy(DeploymentQueryProperty.TENANT_ID);
  }

  //results ////////////////////////////////////////////////////////

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    return commandContext
      .getDeploymentManager()
      .findDeploymentCountByQueryCriteria(this);
  }

  @Override
  public List<Deployment> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    return commandContext
      .getDeploymentManager()
      .findDeploymentsByQueryCriteria(this, page);
  }

  //getters ////////////////////////////////////////////////////////

  public String getDeploymentId() {
    return deploymentId;
  }

  public String getName() {
    return name;
  }

  public String getNameLike() {
    return nameLike;
  }

  public boolean isSourceQueryParamEnabled() {
    return sourceQueryParamEnabled;
  }

  public String getSource() {
    return source;
  }

  public Date getDeploymentBefore() {
    return deploymentBefore;
  }

  public Date getDeploymentAfter() {
    return deploymentAfter;
  }
}
