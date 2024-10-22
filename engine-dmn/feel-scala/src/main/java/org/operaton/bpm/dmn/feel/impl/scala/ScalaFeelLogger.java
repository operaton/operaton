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
package org.operaton.bpm.dmn.feel.impl.scala;

import org.operaton.bpm.dmn.feel.impl.FeelException;
import org.operaton.bpm.dmn.feel.impl.scala.spin.SpinValueMapperFactory;
import org.operaton.commons.logging.BaseLogger;

public class ScalaFeelLogger extends BaseLogger {

  public static final String PROJECT_CODE = "FEEL/SCALA";
  public static final String PROJECT_LOGGER = "org.operaton.bpm.dmn.feel.scala";

  public static final ScalaFeelLogger LOGGER = createLogger(ScalaFeelLogger.class,
    PROJECT_CODE, PROJECT_LOGGER, "01");

    /**
   * Logs an error message with the provided id, message template, and throwable.
   *
   * @param id the identifier of the error
   * @param messageTemplate the template of the error message
   * @param t the exception that caused the error
   */
  protected void logError(String id, String messageTemplate, Throwable t) {
    super.logError(id, messageTemplate, t);
  }

    /**
   * Logs an informational message with a specified id and message template, along with a Throwable object.
   *
   * @param id the id of the log message
   * @param messageTemplate the template of the log message
   * @param t the Throwable object to be logged
   */
  protected void logInfo(String id, String messageTemplate, Throwable t) {
    super.logInfo(id, messageTemplate, t);
  }

    /**
   * Logs the detection of a spin value mapper.
   */
  public void logSpinValueMapperDetected() {
    logInfo("001", "Spin value mapper detected");
  }

    /**
   * Creates a FeelException with a specific message and cause related to
   * an instantiation failure of a SpinValueMapper.
   *
   * @param cause the cause of the instantiation failure
   * @return a FeelException with a specific message and cause
   */
  public FeelException spinValueMapperInstantiationException(Throwable cause) {
    return new FeelException(exceptionMessage(
      "002", SpinValueMapperFactory.SPIN_VALUE_MAPPER_CLASS_NAME + " class found " +
        "on class path but cannot be instantiated."), cause);
  }

    /**
   * Creates a new FeelException with a specific message and cause related to a SpinValueMapper access exception.
   *
   * @param cause the cause of the exception
   * @return a new FeelException describing the access exception for SpinValueMapper
   */
  public FeelException spinValueMapperAccessException(Throwable cause) {
    return new FeelException(exceptionMessage(
      "003", SpinValueMapperFactory.SPIN_VALUE_MAPPER_CLASS_NAME + " class found " +
        "on class path but cannot be accessed."), cause);
  }

    /**
   * Constructs a FeelException with a specific error message related to casting SpinValueMapper classes.
   * 
   * @param cause the cause of the exception
   * @param className the class name that the SpinValueMapper could not be cast to
   * @return a new FeelException instance
   */
  public FeelException spinValueMapperCastException(Throwable cause, String className) {
    return new FeelException(exceptionMessage(
      "004", SpinValueMapperFactory.SPIN_VALUE_MAPPER_CLASS_NAME + " class found " +
        "on class path but cannot be cast to " + className), cause);
  }

    /**
   * Creates a new FeelException with a specific error code and message related to a Spin value mapper lookup or registration error.
   *
   * @param cause the Throwable that caused the exception
   * @return a new FeelException with the specified error code, message, and cause
   */
  public FeelException spinValueMapperException(Throwable cause) {
    return new FeelException(exceptionMessage(
      "005", "Error while looking up or registering Spin value mapper", cause));
  }

    /**
   * Creates a FeelException with a specific exception message indicating that only one return value or function is allowed.
   * 
   * @return FeelException with the specified exception message
   */
  public FeelException functionCountExceededException() {
    return new FeelException(exceptionMessage(
      "006", "Only set one return value or a function."));
  }

    /**
   * Creates a FeelException with a specific exception message indicating that a custom function is not available.
   *
   * @return a new FeelException with the exception message "007" and "Custom function not available."
   */
  public FeelException customFunctionNotFoundException() {
    return new FeelException(exceptionMessage(
      "007", "Custom function not available."));
  }

    /**
   * Creates a new FeelException with a specific error message related to evaluation of an expression.
   *
   * @param message the message to include in the exception
   * @return a new FeelException with the formatted error message
   */
  public FeelException evaluationException(String message) {
    return new FeelException(exceptionMessage(
      "008", "Error while evaluating expression: {}", message));
  }

}
