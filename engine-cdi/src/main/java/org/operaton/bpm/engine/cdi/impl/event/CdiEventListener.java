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
package org.operaton.bpm.engine.cdi.impl.event;

import java.lang.annotation.Annotation;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.cdi.BusinessProcessEvent;
import org.operaton.bpm.engine.cdi.impl.util.BeanManagerLookup;
import org.operaton.bpm.engine.delegate.ExecutionListener;

/**
 * Generic {@link ExecutionListener} publishing events using the CDI event
 * infrastructure.
 *
 * @author Daniel Meyer
 */
public class CdiEventListener extends AbstractCdiEventListener {

  @Inject
  Event<BusinessProcessEvent> event;

  @Override
  protected void fireEvent(BusinessProcessEvent businessProcessEvent, Annotation[] qualifiers) {
    event.select(qualifiers).fire(businessProcessEvent);
  }

  protected BeanManager getBeanManager() {
    BeanManager bm = BeanManagerLookup.getBeanManager();
    if (bm == null) {
      throw new ProcessEngineException("No cdi bean manager available, cannot publish event.");
    }
    return bm;
  }
}
