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
package org.operaton.bpm.integrationtest.functional.ejb.beans;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;

import jakarta.ejb.ApplicationException;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;

/**
 * A SLSB acting as a {@link JavaDelegate}
 *
 * @author Daniel Meyer
 *
 */
@Named("SLSBThrowExceptionDelegate")
@Stateless
public class SLSBThrowExceptionDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    throw new MyException("error");
  }

  @ApplicationException
  public static class MyException extends RuntimeException{

    private static final long serialVersionUID = 826202870386719558L;

    public MyException(String string) {
      super(string);
    }

  }
}
