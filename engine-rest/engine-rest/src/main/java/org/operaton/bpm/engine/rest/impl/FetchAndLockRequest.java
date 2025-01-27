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
package org.operaton.bpm.engine.rest.impl;

import org.operaton.bpm.engine.impl.identity.Authentication;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.rest.dto.externaltask.FetchExternalTasksExtendedDto;

import jakarta.ws.rs.container.AsyncResponse;
import java.util.Date;

/**
 * @author Tassilo Weidner
 */
public class FetchAndLockRequest {

  protected Date requestTime = ClockUtil.getCurrentTime();
  protected FetchExternalTasksExtendedDto dto;
  protected AsyncResponse asyncResponse;
  protected String processEngineName;
  protected Authentication authentication;

  public Date getRequestTime() {
    return requestTime;
  }

  public FetchAndLockRequest setRequestTime(Date requestTime) {
    this.requestTime = requestTime;
    return this;
  }

  public FetchExternalTasksExtendedDto getDto() {
    return dto;
  }

  public FetchAndLockRequest setDto(FetchExternalTasksExtendedDto dto) {
    this.dto = dto;
    return this;
  }

  public AsyncResponse getAsyncResponse() {
    return asyncResponse;
  }

  public FetchAndLockRequest setAsyncResponse(AsyncResponse asyncResponse) {
    this.asyncResponse = asyncResponse;
    return this;
  }

  public String getProcessEngineName() {
    return processEngineName;
  }

  public FetchAndLockRequest setProcessEngineName(String processEngineName) {
    this.processEngineName = processEngineName;
    return this;
  }

  public Authentication getAuthentication() {
    return authentication;
  }

  public FetchAndLockRequest setAuthentication(Authentication authentication) {
    this.authentication = authentication;
    return this;
  }

  public long getTimeoutTimestamp() {
    long reqTime = getRequestTime().getTime();
    long asyncResponseTimeout = getDto().getAsyncResponseTimeout();
    return reqTime + asyncResponseTimeout;
  }

  @Override
  public String toString() {
    return "FetchAndLockRequest [requestTime=" + requestTime + ", dto=" + dto + ", asyncResponse=" + asyncResponse + ", processEngineName=" + processEngineName
        + ", authentication=" + authentication + "]";
  }

}
