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
package org.operaton.bpm.client.spring.boot.starter.impl;

import java.io.InputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ssl.pem.PemContent;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import org.operaton.bpm.client.interceptor.auth.AzureWorkloadIdentityAssertionProvider;
import org.operaton.bpm.client.interceptor.auth.BasicAuthProvider;
import org.operaton.bpm.client.interceptor.auth.ClientAssertionProvider;
import org.operaton.bpm.client.interceptor.auth.JjwtClientAssertionProvider;
import org.operaton.bpm.client.interceptor.auth.OAuth2ClientCredentialsProvider;
import org.operaton.bpm.client.spring.boot.starter.BasicAuthProperties;
import org.operaton.bpm.client.spring.boot.starter.ClientProperties;
import org.operaton.bpm.client.spring.boot.starter.OAuth2Properties;
import org.operaton.bpm.client.spring.boot.starter.OAuth2Properties.AssertionProperties;
import org.operaton.bpm.client.spring.boot.starter.OAuth2Properties.AssertionProperties.AssertionType;
import org.operaton.bpm.client.spring.impl.client.ClientConfiguration;
import org.operaton.bpm.client.spring.impl.client.ClientFactory;

public class PropertiesAwareClientFactory extends ClientFactory implements ResourceLoaderAware {

  protected ClientProperties clientProperties;
  protected ResourceLoader resourceLoader = new DefaultResourceLoader();

  @Autowired
  public PropertiesAwareClientFactory(ClientProperties clientProperties) {
    this.clientProperties = clientProperties;
  }

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @Override
  public void afterPropertiesSet() {
    applyPropertiesFrom(clientProperties);
    addBasicAuthInterceptor();
    addOAuth2Interceptor();
    super.afterPropertiesSet();
  }

  protected void addBasicAuthInterceptor() {
    BasicAuthProperties basicAuth = clientProperties.getBasicAuth();
    if (basicAuth != null) {

      String username = basicAuth.getUsername();
      String password = basicAuth.getPassword();
      BasicAuthProvider basicAuthProvider = new BasicAuthProvider(username, password);

      getRequestInterceptors().add(basicAuthProvider);
    }
  }

  protected void addOAuth2Interceptor() {
    OAuth2Properties oauth2 = clientProperties.getOauth2();
    if (oauth2 == null) {
      return;
    }

    OAuth2ClientCredentialsProvider.Builder builder = OAuth2ClientCredentialsProvider.builder()
        .tokenUri(oauth2.getTokenUri())
        .clientId(oauth2.getClientId())
        .scope(oauth2.getScope())
        .audience(oauth2.getAudience())
        .resource(oauth2.getResource())
        .additionalParameters(oauth2.getAdditionalParameters())
        .expiryBuffer(Duration.ofSeconds(oauth2.getExpiryBufferSeconds()));

    if (oauth2.getClientSecret() != null && !oauth2.getClientSecret().isBlank()) {
      builder.clientSecret(oauth2.getClientSecret());
    } else {
      builder.clientAssertionProvider(buildAssertionProvider(oauth2));
    }

    getRequestInterceptors().add(builder.build());
  }

  protected ClientAssertionProvider buildAssertionProvider(OAuth2Properties oauth2) {
    AssertionProperties assertion = oauth2.getAssertion();
    if (assertion == null || assertion.getType() == null) {
      throw new IllegalStateException(
          "operaton.bpm.client.oauth2.assertion.type must be set when no clientSecret is configured. "
              + "Valid values: JJWT, AZURE_WORKLOAD_IDENTITY");
    }

    AssertionType type = assertion.getType();
    if (type == AssertionType.AZURE_WORKLOAD_IDENTITY) {
      return new AzureWorkloadIdentityAssertionProvider();
    }

    String keyLocation = assertion.getKeyLocation();
    if (keyLocation == null || keyLocation.isBlank()) {
      throw new IllegalStateException(
          "operaton.bpm.client.oauth2.assertion.key-location must be set for assertion type " + type);
    }

    Resource resource = resourceLoader.getResource(keyLocation);
    PrivateKey privateKey;
    try (InputStream inputStream = resource.getInputStream()) {
      privateKey = PemContent.load(inputStream).getPrivateKey();
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load OAuth2 client assertion private key from '" + keyLocation + "'", e);
    }

    if (privateKey == null) {
      throw new IllegalStateException("No private key found in PEM content at '" + keyLocation + "'");
    }
    if (!(privateKey instanceof RSAPrivateKey) && !(privateKey instanceof ECPrivateKey)) {
      throw new IllegalStateException(
          "Unsupported private key type '" + privateKey.getAlgorithm()
              + "' (" + privateKey.getClass().getName() + ") loaded from '" + keyLocation
              + "'. Expected RSA or EC key.");
    }

    return new JjwtClientAssertionProvider(oauth2.getClientId(), oauth2.getTokenUri(), privateKey);
  }

  public void applyPropertiesFrom(ClientProperties clientConfigurationProps) {
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    if (clientConfigurationProps.getBaseUrl() != null) {
      clientConfiguration.setBaseUrl(clientConfigurationProps.getBaseUrl());
    }
    if (clientConfigurationProps.getWorkerId() != null) {
      clientConfiguration.setWorkerId(clientConfigurationProps.getWorkerId());
    }
    if (clientConfigurationProps.getMaxTasks() != null) {
      clientConfiguration.setMaxTasks(clientConfigurationProps.getMaxTasks());
    }
    if (clientConfigurationProps.getOrderByCreateTime() != null) {
      clientConfiguration.setOrderByCreateTime(clientConfigurationProps.getOrderByCreateTime());
    }
    if (clientConfigurationProps.getUseCreateTime() != null) {
      clientConfiguration.setUseCreateTime(clientConfigurationProps.getUseCreateTime());
    }
    if (clientConfigurationProps.getUsePriority() != null && Boolean.FALSE.equals(clientConfigurationProps.getUsePriority())) {
      clientConfiguration.setUsePriority(false);
    }
    if (clientConfigurationProps.getDefaultSerializationFormat() != null) {
      clientConfiguration.setDefaultSerializationFormat(clientConfigurationProps.getDefaultSerializationFormat());
    }
    if (clientConfigurationProps.getDateFormat() != null) {
      clientConfiguration.setDateFormat(clientConfigurationProps.getDateFormat());
    }
    if (clientConfigurationProps.getLockDuration() != null) {
      clientConfiguration.setLockDuration(clientConfigurationProps.getLockDuration());
    }
    if (clientConfigurationProps.getAsyncResponseTimeout() != null) {
      clientConfiguration.setAsyncResponseTimeout(clientConfigurationProps.getAsyncResponseTimeout());
    }
    if (clientConfigurationProps.getDisableAutoFetching() != null &&
        clientConfigurationProps.getDisableAutoFetching()) {
      clientConfiguration.setDisableAutoFetching(true);
    }
    if (clientConfigurationProps.getDisableBackoffStrategy() != null &&
        clientConfigurationProps.getDisableBackoffStrategy()) {
      clientConfiguration.setDisableBackoffStrategy(true);
    }
    setClientConfiguration(clientConfiguration);
  }

}
