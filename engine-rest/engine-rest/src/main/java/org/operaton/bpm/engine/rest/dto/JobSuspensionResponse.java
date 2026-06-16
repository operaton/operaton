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

public class JobSuspensionResponse extends JobOperationResponse {

  public JobSuspensionResponse() {
  }

  public JobSuspensionResponse(String jobId, ResponseStatus status, String errorMessage) {
    super(jobId, status, errorMessage);
  }
}
