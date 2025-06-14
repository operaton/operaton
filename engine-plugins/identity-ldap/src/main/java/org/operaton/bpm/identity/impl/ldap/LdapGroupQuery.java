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
package org.operaton.bpm.identity.impl.ldap;

import java.util.List;

import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.impl.GroupQueryImpl;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;

/**
 * @author Daniel Meyer
 *
 */
public class LdapGroupQuery extends GroupQueryImpl {

  private static final long serialVersionUID = 1L;

  public LdapGroupQuery() {
    super();
  }

  public LdapGroupQuery(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  // execute queries ////////////////////////////

  public long executeCount(CommandContext commandContext) {
    final LdapIdentityProviderSession identityProvider = getLdapIdentityProvider(commandContext);
    return identityProvider.findGroupCountByQueryCriteria(this);
  }

  public List<Group> executeList(CommandContext commandContext, Page page) {
    final LdapIdentityProviderSession identityProvider = getLdapIdentityProvider(commandContext);
    return identityProvider.findGroupByQueryCriteria(this);
  }

  protected LdapIdentityProviderSession getLdapIdentityProvider(CommandContext commandContext) {
    return (LdapIdentityProviderSession) commandContext.getReadOnlyIdentityProvider();
  }

}
