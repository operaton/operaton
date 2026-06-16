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

public enum ResponseStatus {

  SUCCESS("success"),
  FAILURE("failure");

  private final String value;

  ResponseStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
