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
package org.operaton.bpm.engine.rest.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.migration.MigratingProcessInstanceValidationException;
import org.operaton.bpm.engine.migration.MigrationPlanValidationException;
import org.operaton.bpm.engine.rest.dto.AuthorizationExceptionDto;
import org.operaton.bpm.engine.rest.dto.ExceptionDto;
import org.operaton.bpm.engine.rest.dto.ParseExceptionDto;
import org.operaton.bpm.engine.rest.dto.migration.MigratingProcessInstanceValidationExceptionDto;
import org.operaton.bpm.engine.rest.dto.migration.MigrationPlanValidationExceptionDto;

/**
 * @author Svetlana Dorokhova.
 */
public class ExceptionHandlerHelper {

  protected static final ExceptionLogger LOGGER = ExceptionLogger.REST_LOGGER;
  protected static final ExceptionHandlerHelper INSTANCE = new ExceptionHandlerHelper();

  private ExceptionHandlerHelper() {
  }

  public static ExceptionHandlerHelper getInstance(){
    return INSTANCE;
  }

  public Response getResponse(Throwable throwable) {
    LOGGER.log(throwable);

    Response.Status responseStatus = getStatus(throwable);
    ExceptionDto exceptionDto = fromException(throwable);

    return Response
        .status(responseStatus)
        .entity(exceptionDto)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .build();
  }

  protected void provideExceptionCode(Throwable throwable, ExceptionDto exceptionDto) {
    Integer code = null;
    if (throwable instanceof ProcessEngineException) {
      code = getCode(throwable);

    } else if (throwable instanceof RestException) {
      Throwable cause = throwable.getCause();
      if (cause instanceof ProcessEngineException) {
        code = getCode(cause);

      }
    }

    if (code != null) {
      exceptionDto.setCode(code);
    }
  }

  protected Integer getCode(Throwable throwable) {
    ProcessEngineException pex = (ProcessEngineException) throwable;
    return pex.getCode();
  }

  public ExceptionDto fromException(Throwable e) {
    ExceptionDto exceptionDto;
    if (e instanceof MigratingProcessInstanceValidationException exception) {
      exceptionDto = MigratingProcessInstanceValidationExceptionDto.from(exception);
    } else if (e instanceof MigrationPlanValidationException exception) {
      exceptionDto = MigrationPlanValidationExceptionDto.from(exception);
    } else if (e instanceof AuthorizationException exception) {
      exceptionDto = AuthorizationExceptionDto.fromException(exception);
    } else if (e instanceof ParseException exception){
      exceptionDto = ParseExceptionDto.fromException(exception);
    } else {
      exceptionDto = ExceptionDto.fromException(e);
    }
    provideExceptionCode(e, exceptionDto);

    return exceptionDto;
  }

  public Response.Status getStatus(Throwable exception) {
    Response.Status responseStatus = Response.Status.INTERNAL_SERVER_ERROR;

    if (exception instanceof ProcessEngineException engineException) {
      responseStatus = getStatus(engineException);
    }
    else if (exception instanceof RestException restException) {
      responseStatus = getStatus(restException);
    }
    else if (exception instanceof WebApplicationException applicationException) {
      //we need to check this, as otherwise the logic for processing WebApplicationException will be overridden
      final int statusCode = applicationException.getResponse().getStatus();
      responseStatus = Response.Status.fromStatusCode(statusCode);
    }
    return responseStatus;
  }

  public Response.Status getStatus(ProcessEngineException exception) {
    Response.Status responseStatus = Response.Status.INTERNAL_SERVER_ERROR;

    // provide custom handling of authorization exception
    if (exception instanceof AuthorizationException) {
      responseStatus = Response.Status.FORBIDDEN;
    }
    else if (exception instanceof MigrationPlanValidationException
      || exception instanceof MigratingProcessInstanceValidationException
      || exception instanceof BadUserRequestException
      || exception instanceof ParseException) {
      responseStatus = Response.Status.BAD_REQUEST;
    }
    return responseStatus;
  }

  public Response.Status getStatus(RestException exception) {
    if (exception.getStatus() != null) {
      return exception.getStatus();
    }
    return Response.Status.INTERNAL_SERVER_ERROR;
  }
}
