/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import org.operaton.bpm.identity.impl.scim.plugin.ScimIdentityProviderPlugin;

public class OperatonBpmRunScimProperties extends ScimIdentityProviderPlugin {

  public static final String PREFIX = OperatonBpmRunProperties.PREFIX + ".scim";

  boolean enabled = true;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String toString() {
    return "OperatonBpmRunScimProperty [enabled=" + enabled +
        ", scimVersion=" + scimVersion +
        ", serverUrl=******" +
        ", authenticationType=******" +
        ", username=******" +
        ", password=******" +
        ", bearerToken=******" +
        ", oauth2TokenUrl=******" +
        ", oauth2ClientId=******" +
        ", oauth2ClientSecret=******" +
        ", oauth2Scope=******" +
        ", usersEndpoint=" + usersEndpoint +
        ", groupsEndpoint=" + groupsEndpoint +
        ", userBaseFilter=" + userBaseFilter +
        ", groupBaseFilter=" + groupBaseFilter +
        ", userIdAttribute=" + userIdAttribute +
        ", userFirstnameAttribute=" + userFirstnameAttribute +
        ", userLastnameAttribute=" + userLastnameAttribute +
        ", userPasswordAttribute=" + userPasswordAttribute +
        ", userEmailsAttribute=" + userEmailsAttribute +
        ", groupIdAttribute=" + groupIdAttribute +
        ", groupNameAttribute=" + groupNameAttribute +
        ", groupMembersAttribute=" + groupMembersAttribute +
        ", allowModifications=" + allowModifications +
        ", connectionTimeout=" + connectionTimeout +
        ", acceptUntrustedCertificates=" + acceptUntrustedCertificates +
        ", pageSize=" + pageSize +
        ", authorizationCheckEnabled=" + authorizationCheckEnabled +
        ", userAuthenticationEnabled=" + userAuthenticationEnabled +
        ", userAuthenticationProtocol=" + userAuthenticationProtocol +
        ", userAuthenticationUrl=" + userAuthenticationUrl + "]";
  }
}
