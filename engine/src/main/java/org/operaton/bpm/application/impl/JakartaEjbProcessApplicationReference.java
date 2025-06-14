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
package org.operaton.bpm.application.impl;

import jakarta.ejb.EJBException;
import org.operaton.bpm.application.ProcessApplicationInterface;
import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.application.ProcessApplicationUnavailableException;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;

/**
 * <p>A reference to an EJB process application.</p>
 *
 * <p>An EJB process application is an EJB Session Bean that can be looked up in JNDI.</p>
 *
 * @author Daniel Meyer
 *
 */
public class JakartaEjbProcessApplicationReference implements ProcessApplicationReference {

  private static final ProcessApplicationLogger LOG = ProcessEngineLogger.PROCESS_APPLICATION_LOGGER;

  /** this is an EjbProxy and can be cached */
  protected ProcessApplicationInterface selfReference;

  /** the name of the process application */
  protected String processApplicationName;

  public JakartaEjbProcessApplicationReference(ProcessApplicationInterface selfReference, String name) {
    this.selfReference = selfReference;
    this.processApplicationName = name;
  }

  @Override
  public String getName() {
    return processApplicationName;
  }

  @Override
  public ProcessApplicationInterface getProcessApplication() throws ProcessApplicationUnavailableException {
    try {
      // check whether process application is still deployed
      selfReference.getName();
    } catch (EJBException e) {
      throw LOG.processApplicationUnavailableException(processApplicationName, e);
    }
    return selfReference;
  }

  public void processEngineStopping(ProcessEngine processEngine) {
    // do nothing.
  }

}
