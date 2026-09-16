/*
 * Copyright 2025 the Operaton contributors.
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
package org.operaton.bpm.engine.rest.dto.repository;

public class DeleteDeploymentResponse {

  public static final String SUCCESS = "SUCCESS";
  public static final String FAILURE = "FAILURE";

  protected String deploymentId;
  protected String status;
  protected String errorMessage;

  public DeleteDeploymentResponse() {
  }

  public DeleteDeploymentResponse(String deploymentId, String status, String errorMessage) {
    this.deploymentId = deploymentId;
    this.status = status;
    this.errorMessage = errorMessage;
  }

  public String getDeploymentId() {
    return deploymentId;
  }

  public void setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public boolean hasFailed() {
    return FAILURE.equals(status);
  }

}
