/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
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
package org.operaton.bpm.client.spring.impl.client;

import org.operaton.bpm.client.spring.annotation.EnableExternalTaskClient;
import org.operaton.bpm.client.spring.annotation.ExternalTaskSubscription;
import static org.operaton.bpm.client.spring.annotation.EnableExternalTaskClient.INT_NULL_VALUE;
import static org.operaton.bpm.client.spring.annotation.ExternalTaskSubscription.LONG_NULL_VALUE;

public class ClientConfiguration {

  protected String baseUrl;
  protected String workerId;
  protected Integer maxTasks;
  protected Boolean usePriority;
  protected Boolean useCreateTime;
  protected String orderByCreateTime;
  protected String defaultSerializationFormat;
  protected String dateFormat;
  protected Long asyncResponseTimeout;
  protected Long lockDuration;
  protected Boolean disableAutoFetching;
  protected Boolean disableBackoffStrategy;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Integer getMaxTasks() {
    return maxTasks;
  }

  public void setMaxTasks(Integer maxTasks) {
    this.maxTasks = maxTasks;
  }

  public String getWorkerId() {
    return workerId;
  }

  public void setWorkerId(String workerId) {
    this.workerId = workerId;
  }

  public Long getAsyncResponseTimeout() {
    return asyncResponseTimeout;
  }

  public void setAsyncResponseTimeout(Long asyncResponseTimeout) {
    this.asyncResponseTimeout = asyncResponseTimeout;
  }

  public Long getLockDuration() {
    return lockDuration;
  }

  public void setLockDuration(Long lockDuration) {
    this.lockDuration = lockDuration;
  }

  public String getDateFormat() {
    return dateFormat;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public String getDefaultSerializationFormat() {
    return defaultSerializationFormat;
  }

  public void setDefaultSerializationFormat(String defaultSerializationFormat) {
    this.defaultSerializationFormat = defaultSerializationFormat;
  }

  public Boolean getUsePriority() {
    return usePriority;
  }

  public void setUsePriority(Boolean usePriority) {
    this.usePriority = usePriority;
  }

  public String getOrderByCreateTime() {
    return orderByCreateTime;
  }

  public void setOrderByCreateTime(String orderByCreateTime) {
    this.orderByCreateTime = orderByCreateTime;
  }

  public Boolean getUseCreateTime() {
    return useCreateTime;
  }

  public void setUseCreateTime(Boolean useCreateTime) {
    this.useCreateTime = useCreateTime;
  }

  public Boolean getDisableAutoFetching() {
    return disableAutoFetching;
  }

  public void setDisableAutoFetching(Boolean disableAutoFetching) {
    this.disableAutoFetching = disableAutoFetching;
  }

  public Boolean getDisableBackoffStrategy() {
    return disableBackoffStrategy;
  }

  public void setDisableBackoffStrategy(Boolean disableBackoffStrategy) {
    this.disableBackoffStrategy = disableBackoffStrategy;
  }

  public void fromAnnotation(EnableExternalTaskClient annotation) {
    String annotatedBaseUrl = annotation.baseUrl();
    setBaseUrl(isNull(annotatedBaseUrl) ? null : annotatedBaseUrl);

    int annotatedMaxTasks = annotation.maxTasks();
    setMaxTasks(isNull(annotatedMaxTasks) ? null : annotatedMaxTasks);

    String annotatedWorkerId = annotation.workerId();
    setWorkerId(isNull(annotatedWorkerId) ? null : annotatedWorkerId);

    setUsePriority(annotation.usePriority());

    setUseCreateTime(annotation.useCreateTime());
    configureOrderByCreateTime(annotation);

    long annotatedAsyncResponseTimeout = annotation.asyncResponseTimeout();
    setAsyncResponseTimeout(isNull(annotatedAsyncResponseTimeout) ? null : annotatedAsyncResponseTimeout);

    setDisableAutoFetching(annotation.disableAutoFetching());

    setDisableBackoffStrategy(annotation.disableBackoffStrategy());

    long annotatedLockDuration = annotation.lockDuration();
    setLockDuration(isNull(annotatedLockDuration) ? null : annotatedLockDuration);

    String annotatedDateFormat = annotation.dateFormat();
    setDateFormat(isNull(annotatedDateFormat) ? null : annotatedDateFormat);

    String serializationFormat = annotation.defaultSerializationFormat();
    setDefaultSerializationFormat(isNull(serializationFormat) ? null : serializationFormat);
  }

  protected void configureOrderByCreateTime(EnableExternalTaskClient annotation) {
    if (EnableExternalTaskClient.STRING_NULL_VALUE.equals(annotation.orderByCreateTime())) {
      setOrderByCreateTime(null);
    } else {
      setOrderByCreateTime(annotation.orderByCreateTime());
    }
  }

  protected static boolean isNull(String value) {
    return ExternalTaskSubscription.STRING_NULL_VALUE.equals(value);
  }

  protected static boolean isNull(long value) {
    return LONG_NULL_VALUE == value;
  }

  protected static boolean isNull(int value) {
    return INT_NULL_VALUE == value;
  }

}
