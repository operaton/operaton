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
package org.operaton.bpm.engine.rest.dto.converter;

import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.task.DelegationState;

public class DelegationStateConverter extends JacksonAwareStringToTypeConverter<DelegationState> {

  @Override
  public DelegationState convertQueryParameterToType(String value) {
    try {
      return DelegationState.valueOf(value.toUpperCase());

    } catch (IllegalArgumentException e) {
      String message = "Valid values for property 'delegationState' are 'PENDING' or 'RESOLVED', but was '%s'".formatted(value);
      throw new InvalidRequestException(Status.BAD_REQUEST, e, message);

    }
  }
}
