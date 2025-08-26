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

import java.io.Serial;

import org.operaton.bpm.engine.identity.Tenant;
import org.operaton.bpm.engine.identity.TenantQuery;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

public abstract class TenantQueryImpl extends AbstractQuery<TenantQuery, Tenant> implements TenantQuery {

  @Serial private static final long serialVersionUID = 1L;
  protected String id;
  protected String[] ids;
  protected String name;
  protected String nameLike;
  protected String userId;
  protected String groupId;
  protected boolean includingGroups;

  protected TenantQueryImpl() {
  }

  protected TenantQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public TenantQuery tenantId(String id) {
    ensureNotNull("tenant ud", id);
    this.id = id;
    return this;
  }

  @Override
  public TenantQuery tenantIdIn(String... ids) {
    ensureNotNull("tenant ids", (Object[]) ids);
    this.ids = ids;
    return this;
  }

  @Override
  public TenantQuery tenantName(String name) {
    ensureNotNull("tenant name", name);
    this.name = name;
    return this;
  }

  @Override
  public TenantQuery tenantNameLike(String nameLike) {
    ensureNotNull("tenant name like", nameLike);
    this.nameLike = nameLike;
    return this;
  }

  @Override
  public TenantQuery userMember(String userId) {
    ensureNotNull("user id", userId);
    this.userId = userId;
    return this;
  }

  @Override
  public TenantQuery groupMember(String groupId) {
    ensureNotNull("group id", groupId);
    this.groupId = groupId;
    return this;
  }

  @Override
  public TenantQuery includingGroupsOfUser(boolean includingGroups) {
    this.includingGroups = includingGroups;
    return this;
  }

  //sorting ////////////////////////////////////////////////////////

  @Override
  public TenantQuery orderByTenantId() {
    return orderBy(TenantQueryProperty.GROUP_ID);
  }

  @Override
  public TenantQuery orderByTenantName() {
    return orderBy(TenantQueryProperty.NAME);
  }

  //getters ////////////////////////////////////////////////////////

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getNameLike() {
    return nameLike;
  }

  public String[] getIds() {
    return ids;
  }

  public String getUserId() {
    return userId;
  }

  public String getGroupId() {
    return groupId;
  }

  public boolean isIncludingGroups() {
    return includingGroups;
  }

}
