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
package org.operaton.bpm.run.test.config.identity;

import org.operaton.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin;
import org.operaton.bpm.run.OperatonApp;
import org.operaton.bpm.run.property.OperatonBpmRunLdapProperties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = {OperatonApp.class})
@ActiveProfiles(profiles = {"test-auth-disabled", "test-ldap-enabled", "test-ldap-auth-exception"})
class LdapConfigurationTest {

  @Autowired
  OperatonBpmRunLdapProperties props;

  @Autowired
  LdapIdentityProviderPlugin plugin;

  @Test
  void shouldPickUpConfiguration() {
    assertThat(props.isEnabled()).isTrue();
    assertThat(props.getServerUrl()).isEqualTo(plugin.getServerUrl());
    assertThat(props.getManagerDn()).isEqualTo(plugin.getManagerDn());
    assertThat(props.getManagerPassword()).isEqualTo(plugin.getManagerPassword());
    assertThat(props.getBaseDn()).isEqualTo(plugin.getBaseDn());
    assertThat(props.getUserSearchBase()).isEqualTo(plugin.getUserSearchBase());
    assertThat(props.getUserIdAttribute()).isEqualTo(plugin.getUserIdAttribute());
    assertThat(props.getUserFirstnameAttribute()).isEqualTo(plugin.getUserFirstnameAttribute());
    assertThat(props.getUserLastnameAttribute()).isEqualTo(plugin.getUserLastnameAttribute());
    assertThat(props.getUserEmailAttribute()).isEqualTo(plugin.getUserEmailAttribute());
    assertThat(props.getUserPasswordAttribute()).isEqualTo(plugin.getUserPasswordAttribute());
    assertThat(props.getGroupSearchBase()).isEqualTo(plugin.getGroupSearchBase());
    assertThat(props.getGroupSearchFilter()).isEqualTo(plugin.getGroupSearchFilter());
    assertThat(props.getGroupIdAttribute()).isEqualTo(plugin.getGroupIdAttribute());
    assertThat(props.getGroupNameAttribute()).isEqualTo(plugin.getGroupNameAttribute());
    assertThat(props.getGroupTypeAttribute()).isEqualTo(plugin.getGroupTypeAttribute());
    assertThat(props.getGroupMemberAttribute()).isEqualTo(plugin.getGroupMemberAttribute());
    assertThat(props.isSortControlSupported()).isEqualTo(plugin.isSortControlSupported());
    assertThat(props.isUseSsl()).isEqualTo(plugin.isUseSsl());
    assertThat(props.isUsePosixGroups()).isEqualTo(plugin.isUsePosixGroups());
    assertThat(props.isAllowAnonymousLogin()).isEqualTo(plugin.isAllowAnonymousLogin());
    assertThat(props.isAuthorizationCheckEnabled()).isEqualTo(plugin.isAuthorizationCheckEnabled());
    assertThat(props.isAcceptUntrustedCertificates()).isEqualTo(plugin.isAcceptUntrustedCertificates());
    assertThat(props.getInitialContextFactory()).isEqualTo(plugin.getInitialContextFactory());
    assertThat(props.getSecurityAuthentication()).isEqualTo(plugin.getSecurityAuthentication());
    assertThat(props.isPasswordCheckCatchAuthenticationException()).isEqualTo(plugin.isPasswordCheckCatchAuthenticationException());
  }

  @Test
  void shouldNotIncludeSensitiveConnectionPropertiesInToString() {
    assertThat(props.toString()).doesNotContain(
        "http://foo.bar",
        "managerdn",
        "managerpw");
  }
}
