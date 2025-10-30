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
package org.operaton.bpm.spring.boot.starter.security.oauth2.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.context.TestPropertySource;

import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.spring.boot.starter.security.oauth2.AbstractSpringSecurityIT;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource("/oauth2-mock.properties")
class OAuth2GrantedAuthoritiesMapperIT extends AbstractSpringSecurityIT {

  protected static final String GROUP_NAME_ATTRIBUTE = "groupName";

  @Autowired
  private OAuth2GrantedAuthoritiesMapper authoritiesMapper;

  @RegisterExtension
  ProcessEngineLoggingExtension logger = new ProcessEngineLoggingExtension()
      .watch(OAuth2GrantedAuthoritiesMapper.class.getCanonicalName());

  @Test
  void testMappingNotOauth2Authorities() {
    // given
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("USER"));
    // when
    Collection<? extends GrantedAuthority> mappedAuthorities = authoritiesMapper.mapAuthorities(authorities);
    // then
    assertThat(mappedAuthorities).isEmpty();
  }

  @Test
  void testMappingGroupNotAvailable() {
    // given
    List<GrantedAuthority> authorities = List.of(new OAuth2UserAuthority("USER", Map.of("name", "name")));
    // when
    Collection<? extends GrantedAuthority> mappedAuthorities = authoritiesMapper.mapAuthorities(authorities);
    // then
    assertThat(mappedAuthorities).isEmpty();
    String expectedWarn = "Attribute " + GROUP_NAME_ATTRIBUTE + " is not available";
    assertThat(logger.getFilteredLog(expectedWarn)).hasSize(1);
  }

  @Test
  void testMappingSingleAuthority() {
    // given
    List<GrantedAuthority> authorities = List.of(new OAuth2UserAuthority("USER", Map.of(GROUP_NAME_ATTRIBUTE, "group")));
    // when
    Collection<? extends GrantedAuthority> mappedAuthorities = authoritiesMapper.mapAuthorities(authorities);
    // then
    assertThat(mappedAuthorities).hasSize(1);
    assertThat(mappedAuthorities.iterator().next().getAuthority()).isEqualTo("group");
  }

  @Test
  void testMappingMultipleAuthorities() {
    // given
    List<GrantedAuthority> authorities = List.of(new OAuth2UserAuthority("USER", Map.of(GROUP_NAME_ATTRIBUTE, List.of("group1", "group2"))));
    // when
    Collection<? extends GrantedAuthority> mappedAuthorities = authoritiesMapper.mapAuthorities(authorities);
    // then
    assertThat(mappedAuthorities).hasSize(2);
    List<String> groups = mappedAuthorities.stream().map(GrantedAuthority::getAuthority).toList();
    assertThat(groups).contains("group1", "group2");
  }


  @Test
  void testMappingUnsupportedType() {
    // given
    Object object = new Object();
    List<GrantedAuthority> authorities = List.of(new OAuth2UserAuthority("USER", Map.of(GROUP_NAME_ATTRIBUTE, object)));
    // when
    Collection<? extends GrantedAuthority> mappedAuthorities = authoritiesMapper.mapAuthorities(authorities);
    // then
    assertThat(mappedAuthorities).isEmpty();
    String expectedError = "Could not map granted authorities, unsupported group attribute type: " + object.getClass();
    assertThat(logger.getFilteredLog(expectedError)).hasSize(1);
  }


}
