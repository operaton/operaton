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

import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.identity.UserQuery;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author Joram Barrez
 */
public abstract class UserQueryImpl extends AbstractQuery<UserQuery, User> implements UserQuery {

  @Serial private static final long serialVersionUID = 1L;
  protected String id;
  protected String[] ids;
  protected String firstName;
  protected String firstNameLike;
  protected String lastName;
  protected String lastNameLike;
  protected String email;
  protected String emailLike;
  protected String groupId;
  protected String procDefId;
  protected String tenantId;

  protected UserQueryImpl() {
  }

  protected UserQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public UserQuery userId(String id) {
    ensureNotNull("Provided id", id);
    this.id = id;
    return this;
  }

  @Override
  public UserQuery userIdIn(String... ids) {
    ensureNotNull("Provided ids", ids);
    this.ids = ids;
    return this;
  }

  @Override
  public UserQuery userFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  @Override
  public UserQuery userFirstNameLike(String firstNameLike) {
    ensureNotNull("Provided firstNameLike", firstNameLike);
    this.firstNameLike = firstNameLike;
    return this;
  }

  @Override
  public UserQuery userLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  @Override
  public UserQuery userLastNameLike(String lastNameLike) {
    ensureNotNull("Provided lastNameLike", lastNameLike);
    this.lastNameLike = lastNameLike;
    return this;
  }

  @Override
  public UserQuery userEmail(String email) {
    this.email = email;
    return this;
  }

  @Override
  public UserQuery userEmailLike(String emailLike) {
    ensureNotNull("Provided emailLike", emailLike);
    this.emailLike = emailLike;
    return this;
  }

  @Override
  public UserQuery memberOfGroup(String groupId) {
    ensureNotNull("Provided groupId", groupId);
    this.groupId = groupId;
    return this;
  }

  @Override
  public UserQuery potentialStarter(String procDefId) {
    ensureNotNull("Provided processDefinitionId", procDefId);
    this.procDefId = procDefId;
    return this;

  }

  @Override
  public UserQuery memberOfTenant(String tenantId) {
    ensureNotNull("Provided tenantId", tenantId);
    this.tenantId = tenantId;
    return this;
  }

  //sorting //////////////////////////////////////////////////////////

  @Override
  public UserQuery orderByUserId() {
    return orderBy(UserQueryProperty.USER_ID);
  }

  @Override
  public UserQuery orderByUserEmail() {
    return orderBy(UserQueryProperty.EMAIL);
  }

  @Override
  public UserQuery orderByUserFirstName() {
    return orderBy(UserQueryProperty.FIRST_NAME);
  }

  @Override
  public UserQuery orderByUserLastName() {
    return orderBy(UserQueryProperty.LAST_NAME);
  }

  //getters //////////////////////////////////////////////////////////

  public String getId() {
    return id;
  }
  public String[] getIds() {
    return ids;
  }
  public String getFirstName() {
    return firstName;
  }
  public String getFirstNameLike() {
    return firstNameLike;
  }
  public String getLastName() {
    return lastName;
  }
  public String getLastNameLike() {
    return lastNameLike;
  }
  public String getEmail() {
    return email;
  }
  public String getEmailLike() {
    return emailLike;
  }
  public String getGroupId() {
    return groupId;
  }
  public String getTenantId() {
    return tenantId;
  }
}
