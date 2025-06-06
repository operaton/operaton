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

import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(OperatonBpmRunProperties.PREFIX)
public class OperatonBpmRunProperties {

  public static final String PREFIX = OperatonBpmProperties.PREFIX + ".run";

  @NestedConfigurationProperty
  protected OperatonBpmRunAuthenticationProperties auth = new OperatonBpmRunAuthenticationProperties();

  @NestedConfigurationProperty
  protected OperatonBpmRunCorsProperty cors = new OperatonBpmRunCorsProperty();

  @NestedConfigurationProperty
  protected OperatonBpmRunLdapProperties ldap = new OperatonBpmRunLdapProperties();

  @NestedConfigurationProperty
  protected List<OperatonBpmRunProcessEnginePluginProperty> processEnginePlugins = new ArrayList<>();

  @NestedConfigurationProperty
  protected OperatonBpmRunRestProperties rest = new OperatonBpmRunRestProperties();

  @NestedConfigurationProperty
  protected OperatonBpmRunDeploymentProperties deployment = new OperatonBpmRunDeploymentProperties();

  protected OperatonBpmRunAdministratorAuthorizationProperties adminAuth
      = new OperatonBpmRunAdministratorAuthorizationProperties();

  public OperatonBpmRunAuthenticationProperties getAuth() {
    return auth;
  }

  public void setAuth(OperatonBpmRunAuthenticationProperties auth) {
    this.auth = auth;
  }

  public OperatonBpmRunCorsProperty getCors() {
    return cors;
  }

  public void setCors(OperatonBpmRunCorsProperty cors) {
    this.cors = cors;
  }

  public OperatonBpmRunLdapProperties getLdap() {
    return ldap;
  }

  public void setLdap(OperatonBpmRunLdapProperties ldap) {
    this.ldap = ldap;
  }

  public OperatonBpmRunAdministratorAuthorizationProperties getAdminAuth() {
    return adminAuth;
  }

  public void setAdminAuth(OperatonBpmRunAdministratorAuthorizationProperties adminAuth) {
    this.adminAuth = adminAuth;
  }

  public List<OperatonBpmRunProcessEnginePluginProperty> getProcessEnginePlugins() {
    return processEnginePlugins;
  }

  public void setProcessEnginePlugins(List<OperatonBpmRunProcessEnginePluginProperty> processEnginePlugins) {
    this.processEnginePlugins = processEnginePlugins;
  }

  public OperatonBpmRunRestProperties getRest() {
    return rest;
  }

  public void setRest(OperatonBpmRunRestProperties rest) {
    this.rest = rest;
  }

  public OperatonBpmRunDeploymentProperties getDeployment() {
    return deployment;
  }

  public void setDeployment(OperatonBpmRunDeploymentProperties deployment) {
    this.deployment = deployment;
  }


  @Override
  public String toString() {
    return "OperatonBpmRunProperties [" +
        "auth=" + auth +
        ", cors=" + cors +
        ", ldap=" + ldap +
        ", adminAuth=" + adminAuth +
        ", plugins=" + processEnginePlugins +
        ", rest=" + rest +
        ", deployment=" + deployment +
        "]";
  }
}
