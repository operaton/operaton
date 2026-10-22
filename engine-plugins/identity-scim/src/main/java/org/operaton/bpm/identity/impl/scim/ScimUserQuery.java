/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
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
package org.operaton.bpm.identity.impl.scim;

import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.UserQueryImpl;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;

import java.util.List;

/**
 * SCIM User Query Implementation.
 */
public class ScimUserQuery extends UserQueryImpl {

  private static final long serialVersionUID = 1L;
  
  private int totalResults = 0;
  
  public int getTotalResults() {
    return totalResults;
  }
  
  public void setTotalResults(int totalResults) {
    this.totalResults = totalResults;
  }

  public ScimUserQuery() {
    super();
  }

  public ScimUserQuery(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    final ScimIdentityProviderReadOnly provider = getScimIdentityProvider(commandContext);
    return provider.findUserCountByQueryCriteria(this);
  }

  @Override
  public List<User> executeList(CommandContext commandContext, Page page) {
    final ScimIdentityProviderReadOnly provider = getScimIdentityProvider(commandContext);
    return provider.findUserByQueryCriteria(this);
  }

  protected ScimIdentityProviderReadOnly getScimIdentityProvider(CommandContext commandContext) {
    return (ScimIdentityProviderReadOnly) commandContext.getReadOnlyIdentityProvider();
  }
}
