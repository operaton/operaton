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
package org.operaton.bpm.client.spring.impl.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;

import org.operaton.bpm.client.ExternalTaskClient;
import org.operaton.bpm.client.ExternalTaskClientBuilder;
import org.operaton.bpm.client.backoff.BackoffStrategy;
import org.operaton.bpm.client.interceptor.ClientRequestInterceptor;
import org.operaton.bpm.client.spring.exception.SpringExternalTaskClientException;
import org.operaton.bpm.client.spring.impl.client.util.ClientLoggerUtil;
import org.operaton.bpm.client.spring.impl.util.LoggerUtil;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.operaton.bpm.client.spring.annotation.EnableExternalTaskClient.STRING_ORDER_BY_ASC_VALUE;
import static org.operaton.bpm.client.spring.annotation.EnableExternalTaskClient.STRING_ORDER_BY_DESC_VALUE;

public class ClientFactory
  implements FactoryBean<ExternalTaskClient>, InitializingBean {

  protected static final ClientLoggerUtil LOG = LoggerUtil.CLIENT_LOGGER;

  protected ClientConfiguration clientConfiguration;

  protected BackoffStrategy backoffStrategy;
  protected List<ClientRequestInterceptor> requestInterceptors = new ArrayList<>();

  protected ExternalTaskClient client;

  protected PropertyResolver propertyResolver;

  @Override
  public ExternalTaskClient getObject() {
    if (client == null) {
      initClient();
    }

    LOG.bootstrapped();

    return client;
  }

  private void initClient() {
    ExternalTaskClientBuilder clientBuilder = ExternalTaskClient.create();

    ofNullable(clientConfiguration.getBaseUrl())
      .map(this::resolve)
      .ifPresent(clientBuilder::baseUrl);

    ofNullable(clientConfiguration.getWorkerId())
      .map(this::resolve)
      .ifPresent(clientBuilder::workerId);

    addClientRequestInterceptors(clientBuilder);

    ofNullable(clientConfiguration.getMaxTasks())
      .ifPresent(clientBuilder::maxTasks);

    if (clientConfiguration.getUsePriority() != null && Boolean.FALSE.equals(clientConfiguration.getUsePriority())) {
      clientBuilder.usePriority(false);
    }

    ofNullable(clientConfiguration.getDefaultSerializationFormat())
      .map(this::resolve)
      .ifPresent(clientBuilder::defaultSerializationFormat);

    ofNullable(clientConfiguration.getDateFormat())
      .map(this::resolve)
      .ifPresent(clientBuilder::dateFormat);

    ofNullable(clientConfiguration.getAsyncResponseTimeout())
      .ifPresent(clientBuilder::asyncResponseTimeout);

    ofNullable(clientConfiguration.getLockDuration())
      .ifPresent(clientBuilder::lockDuration);

    if (TRUE.equals(clientConfiguration.getDisableAutoFetching())) {
      clientBuilder.disableAutoFetching();
    }

    if (TRUE.equals(clientConfiguration.getDisableBackoffStrategy())) {
      clientBuilder.disableBackoffStrategy();
    }

    if (backoffStrategy != null) {
      clientBuilder.backoffStrategy(backoffStrategy);
    }

    tryConfigureCreateTimeOrder(clientBuilder);

    client = clientBuilder.build();
  }

  protected void addClientRequestInterceptors(ExternalTaskClientBuilder taskClientBuilder) {
    requestInterceptors.forEach(taskClientBuilder::addInterceptor);
  }

  protected void tryConfigureCreateTimeOrder(ExternalTaskClientBuilder builder) {
    checkForCreateTimeMisconfiguration();

    if (isUseCreateTimeEnabled()) {
      builder.orderByCreateTime().desc();
      return;
    }

    if (isOrderByCreateTimeEnabled()) {
      handleOrderByCreateTimeConfig(builder);
    }
  }

  protected void handleOrderByCreateTimeConfig(ExternalTaskClientBuilder builder) {
    String orderByCreateTime = clientConfiguration.getOrderByCreateTime();

    if (STRING_ORDER_BY_ASC_VALUE.equals(orderByCreateTime)) {
      builder.orderByCreateTime().asc();
      return;
    }

    if (STRING_ORDER_BY_DESC_VALUE.equals(orderByCreateTime)) {
      builder.orderByCreateTime().desc();
      return;
    }

    throw new SpringExternalTaskClientException("Invalid value " + clientConfiguration.getOrderByCreateTime()
        + ". Please use either \"asc\" or \"desc\" value for configuring \"orderByCreateTime\" on the client");
  }

  protected boolean isOrderByCreateTimeEnabled() {
    return clientConfiguration.getOrderByCreateTime() != null;
  }

  protected boolean isUseCreateTimeEnabled() {
    return TRUE.equals(clientConfiguration.getUseCreateTime());
  }

  protected void checkForCreateTimeMisconfiguration() {
    if (isUseCreateTimeEnabled() && isOrderByCreateTimeEnabled()) {
      throw new SpringExternalTaskClientException(
          "Both \"useCreateTime\" and \"orderByCreateTime\" are enabled. Please use one or the other");
    }
  }

  @Autowired(required = false)
  public void setRequestInterceptors(List<ClientRequestInterceptor> requestInterceptors) {
    if (requestInterceptors != null) {
      this.requestInterceptors.addAll(requestInterceptors);
      LOG.requestInterceptorsFound(this.requestInterceptors.size());
    }
  }

  @Autowired(required = false)
  public void setClientBackoffStrategy(BackoffStrategy backoffStrategy) {
    this.backoffStrategy = backoffStrategy;
    LOG.backoffStrategyFound();
  }

  @Override
  public Class<ExternalTaskClient> getObjectType() {
    return ExternalTaskClient.class;
  }

  @Override
  public void afterPropertiesSet() {
    // no-op
  }

  @SuppressWarnings("unused")
  public ClientConfiguration getClientConfiguration() {
    return clientConfiguration;
  }

  public void setClientConfiguration(ClientConfiguration clientConfiguration) {
    this.clientConfiguration = clientConfiguration;
  }

  public List<ClientRequestInterceptor> getRequestInterceptors() {
    return requestInterceptors;
  }

  protected void close() {
    if (client != null) {
      client.stop();
    }
  }

  @Autowired(required = false)
  protected void setPropertyConfigurer(PropertySourcesPlaceholderConfigurer configurer) {
    PropertySources appliedPropertySources = configurer.getAppliedPropertySources();
    propertyResolver = new PropertySourcesPropertyResolver(appliedPropertySources);
  }

  protected String resolve(String property) {
    if (propertyResolver == null) {
      return property;
    }

    if (property != null) {
      return propertyResolver.resolvePlaceholders(property);
    } else {
      return null;
    }
  }

}
