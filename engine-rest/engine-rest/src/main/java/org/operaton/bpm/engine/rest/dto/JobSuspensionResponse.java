/*
 * Copyright 2025 FINOS
 *
 * The source files in this repository are made available under the Apache License Version 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Fluxnova uses and includes third-party dependencies published under various licenses.
 * By downloading and using Fluxnova artifacts, you agree to their terms and conditions.
 */
package org.operaton.bpm.engine.rest.dto;

public class JobSuspensionResponse {

  private String jobId;
  private ResponseStatus status;
  private String errorMessage;

  public JobSuspensionResponse() {
  }

  public JobSuspensionResponse(String jobId, ResponseStatus status, String errorMessage) {
    this.jobId = jobId;
    this.status = status;
    this.errorMessage = errorMessage;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public ResponseStatus getStatus() {
    return status;
  }

  public void setStatus(ResponseStatus status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
