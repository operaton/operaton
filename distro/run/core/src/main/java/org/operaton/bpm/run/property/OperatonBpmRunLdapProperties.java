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
package org.operaton.bpm.run.property;

import org.operaton.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin;

public class OperatonBpmRunLdapProperties extends LdapIdentityProviderPlugin {

  public static final String PREFIX = OperatonBpmRunProperties.PREFIX + ".ldap";

  boolean enabled = true;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String toString() {
    return "OperatonBpmRunLdapProperty [enabled=%s, initialContextFactory=%s, securityAuthentication=%s, contextProperties=%s, serverUrl=******, managerDn=******, managerPassword=******, baseDn=%s, userDnPattern=%s, userSearchBase=%s, userSearchFilter=%s, groupSearchBase=%s, groupSearchFilter=%s, userIdAttribute=%s, userFirstnameAttribute=%s, userLastnameAttribute=%s, userEmailAttribute=%s, userPasswordAttribute=%s, groupIdAttribute=%s, groupNameAttribute=%s, groupTypeAttribute=%s, groupMemberAttribute=%s, sortControlSupported=%s, useSsl=%s, usePosixGroups=%s, allowAnonymousLogin=%s, authorizationCheckEnabled=%s, passwordCheckCatchAuthenticationException=%s]"
        .formatted(enabled, initialContextFactory, securityAuthentication, contextProperties, baseDn, userDnPattern, userSearchBase, userSearchFilter, groupSearchBase, groupSearchFilter, userIdAttribute, userFirstnameAttribute, userLastnameAttribute, userEmailAttribute, userPasswordAttribute, groupIdAttribute, groupNameAttribute, groupTypeAttribute, groupMemberAttribute, sortControlSupported, useSsl, usePosixGroups, allowAnonymousLogin, authorizationCheckEnabled, passwordCheckCatchAuthenticationException);
  }
}
