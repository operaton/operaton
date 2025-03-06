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
package org.operaton.bpm.container.impl.ejb;

import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import org.operaton.bpm.ProcessApplicationService;
import org.operaton.bpm.application.ProcessApplicationInfo;

/**
 * <p>Exposes the {@link ProcessApplicationService} as EJB inside the container.</p>
 *
 * @author Daniel Meyer
 *
 */
@Stateless(name="ProcessApplicationService", mappedName="ProcessApplicationService")
@Local(ProcessApplicationService.class)
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class EjbProcessApplicationService implements ProcessApplicationService {

  @EJB
  protected EjbBpmPlatformBootstrap ejbBpmPlatform;

  /** the processApplicationServiceDelegate */
  protected ProcessApplicationService processApplicationServiceDelegate;

  @PostConstruct
  protected void initProcessEngineServiceDelegate() {
    processApplicationServiceDelegate = ejbBpmPlatform.getProcessApplicationService();
  }

  public Set<String> getProcessApplicationNames() {
    return processApplicationServiceDelegate.getProcessApplicationNames();
  }

  public ProcessApplicationInfo getProcessApplicationInfo(String processApplicationName) {
    return processApplicationServiceDelegate.getProcessApplicationInfo(processApplicationName);
  }

}
