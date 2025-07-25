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
package org.operaton.bpm.engine.impl.identity;

import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.GroupQuery;
import org.operaton.bpm.engine.identity.NativeUserQuery;
import org.operaton.bpm.engine.identity.Tenant;
import org.operaton.bpm.engine.identity.TenantQuery;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.identity.UserQuery;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.Session;

/**
 * <p>SPI interface for read-only identity Service Providers.</p>
 *
 * <p>This interface provides access to a read-only user / group
 * repository</p>
 *
 *
 * @author Daniel Meyer
 *
 */
public interface ReadOnlyIdentityProvider extends Session {

  // users ////////////////////////////////////////

  /**
   * @return a {@link User} object for the given user id or null if no such user exists.
   * @throws IdentityProviderException in case an error occurs
   */
  User findUserById(String userId);


  /**
   * @return a {@link UserQuery} object which can be used for querying for users.
   * @throws IdentityProviderException in case an error occurs
   */
  UserQuery createUserQuery();

  /**
   * @return a {@link UserQuery} object which can be used in the current command context
   * @throws IdentityProviderException in case an error occurs
   */
  UserQuery createUserQuery(CommandContext commandContext);

  /**
   * Creates a {@link NativeUserQuery} that allows to select users with native queries.
   * @return NativeUserQuery
   */
  NativeUserQuery createNativeUserQuery();

  /**
   * @return 'true' if the password matches the
   * @throws IdentityProviderException in case an error occurs
   */
  boolean checkPassword(String userId, String password);

  // groups //////////////////////////////////////

  /**
   * @return a {@link Group} object for the given group id or null if no such group exists.
   * @throws IdentityProviderException in case an error occurs
   */
  Group findGroupById(String groupId);

  /**
   * @return a {@link GroupQuery} object which can be used for querying for groups.
   * @throws IdentityProviderException in case an error occurs
   */
  GroupQuery createGroupQuery();

  /**
   * @return a {@link GroupQuery} object which can be used for querying for groups and can be reused in the current command context.
   * @throws IdentityProviderException in case an error occurs
   */
  GroupQuery createGroupQuery(CommandContext commandContext);

  // tenants //////////////////////////////////////

  /**
   * @return a {@link Tenant} object for the given id or null if no such tenant
   *         exists.
   * @throws IdentityProviderException
   *           in case an error occurs
   */
  Tenant findTenantById(String tenantId);

  /**
   * @return a {@link TenantQuery} object which can be used for querying for
   *         tenants.
   * @throws IdentityProviderException
   *           in case an error occurs
   */
  TenantQuery createTenantQuery();

  /**
   * @return a {@link TenantQuery} object which can be used for querying for
   *         tenants and can be reused in the current command context.
   * @throws IdentityProviderException
   *           in case an error occurs
   */
  TenantQuery createTenantQuery(CommandContext commandContext);

}
