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
package org.operaton.bpm.engine.impl.incident;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.runtime.Incident;
import org.junit.jupiter.api.Test;
import org.mockito.internal.verification.Times;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CompositeIncidentHandlerTest {

  @Test
  public void shouldUseCompositeIncidentHandlerWithMainIncidentHandlerAddNullHandler() {
    CompositeIncidentHandler compositeIncidentHandler = new CompositeIncidentHandler(new DefaultIncidentHandler(""));

    assertThatThrownBy(() -> compositeIncidentHandler.add(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("Incident handler is null");
  }

  @Test
  public void shouldUseCompositeIncidentHandlerArgumentConstructorWithNullMainHandler() {
    assertThatThrownBy(() -> new CompositeIncidentHandler(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("Incident handler is null");
  }

  @Test
  public void shouldUseCompositeIncidentHandlerArgumentConstructorWithNullVarargs() {
    IncidentHandler incidentHandler = null;
    assertThatThrownBy(() -> new CompositeIncidentHandler(null, incidentHandler))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("Incident handlers contains null value");
  }

  @Test
  public void shouldUseCompositeIncidentHandlerArgumentConstructorWithNullList() {
    List<IncidentHandler> incidentHandler = null;
    assertThatThrownBy(() -> new CompositeIncidentHandler(null, incidentHandler))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("Incident handler is null");
  }

  @Test
  public void shouldUseCompositeIncidentHandlerArgumentConstructorWithMainHandlersAndNullVarargValue() {
    IncidentHandler mainIncidentHandler = new DefaultIncidentHandler("failedJob");
    IncidentHandler incidentHandler = null;
    assertThatThrownBy(() -> new CompositeIncidentHandler(mainIncidentHandler, incidentHandler))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("Incident handlers contains null value");
  }

  @Test
  public void shouldUseCompositeIncidentHandlerArgumentConstructorWithMainHandlersAndNullVarargs() {
    IncidentHandler mainIncidentHandler = new DefaultIncidentHandler("failedJob");
    IncidentHandler[] incidentHandler = null;

    assertThatThrownBy(() -> new CompositeIncidentHandler(mainIncidentHandler, incidentHandler))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("Incident handlers is null");
  }

  @Test
  public void shouldUseCompositeIncidentHandlerArgumentConstructorWithMainHandlersAndNullList() {
    IncidentHandler mainIncidentHandler = new DefaultIncidentHandler("failedJob");
    List<IncidentHandler> incidentHandler = null;

    assertThatThrownBy(() -> new CompositeIncidentHandler(mainIncidentHandler, incidentHandler))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("Incident handlers is null");
  }

  @Test
  public void shouldUseCompositeIncidentHandlerArgumentConstructorWithMainHandlersAndListWithNulls() {
    IncidentHandler mainIncidentHandler = new DefaultIncidentHandler("failedJob");

    List<IncidentHandler> incidentHandler = new ArrayList<>();
    incidentHandler.add(null);
    incidentHandler.add(null);

    assertThatThrownBy(() -> new CompositeIncidentHandler(mainIncidentHandler, incidentHandler))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("Incident handler is null");
  }

  @Test
  public void shouldUseCompositeIncidentHandlerWithAnotherIncidentType() {
    CompositeIncidentHandler compositeIncidentHandler = new CompositeIncidentHandler(new DefaultIncidentHandler("failedJob"));
    DefaultIncidentHandler incidentHandler = new DefaultIncidentHandler("failedExternalTask");

    assertThatThrownBy(() -> compositeIncidentHandler.add(incidentHandler))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Incorrect incident type handler in composite handler with type: failedJob");  }

  @Test
  public void shouldCallAllHandlersWhenCreatingIncident() {
    IncidentHandler mainHandler = mock(IncidentHandler.class);
    Incident incident = mock(Incident.class);

    when(mainHandler.getIncidentHandlerType()).thenReturn("failedJob");
    when(mainHandler.handleIncident(any(), any())).thenReturn(incident);

    CompositeIncidentHandler compositeIncidentHandler = new CompositeIncidentHandler(mainHandler);

    IncidentHandler subHandler = mock(IncidentHandler.class);

    when(subHandler.getIncidentHandlerType()).thenReturn("failedJob");
    when(subHandler.handleIncident(any(), any())).thenReturn(null);

    compositeIncidentHandler.add(subHandler);
    compositeIncidentHandler.add(subHandler);
    compositeIncidentHandler.add(subHandler);

    IncidentContext incidentContext = mock(IncidentContext.class);

    Incident result = compositeIncidentHandler.handleIncident(incidentContext, "Incident message");

    assertThat(result)
      .isNotNull()
      .isEqualTo(incident);

    verify(mainHandler).handleIncident(incidentContext, "Incident message");
    verify(subHandler, new Times(3)).handleIncident(incidentContext, "Incident message");
  }

  @Test
  public void shouldCallAllHandlersWhenDeletingIncident() {
    IncidentHandler mainHandler = mock(IncidentHandler.class);

    when(mainHandler.getIncidentHandlerType()).thenReturn("failedJob");

    CompositeIncidentHandler compositeIncidentHandler = new CompositeIncidentHandler(mainHandler);

    IncidentHandler subHandler = mock(IncidentHandler.class);
    when(subHandler.getIncidentHandlerType()).thenReturn("failedJob");
    compositeIncidentHandler.add(subHandler);
    compositeIncidentHandler.add(subHandler);
    compositeIncidentHandler.add(subHandler);

    IncidentContext incidentContext = mock(IncidentContext.class);

    compositeIncidentHandler.deleteIncident(incidentContext);

    verify(mainHandler).deleteIncident(incidentContext);
    verify(subHandler, new Times(3)).deleteIncident(incidentContext);
  }

  @Test
  public void shouldCallAllHandlersWhenResolvingIncident() {
    IncidentHandler mainHandler = mock(IncidentHandler.class);

    when(mainHandler.getIncidentHandlerType()).thenReturn("failedJob");

    CompositeIncidentHandler compositeIncidentHandler = new CompositeIncidentHandler(mainHandler);

    IncidentHandler subHandler = mock(IncidentHandler.class);

    when(subHandler.getIncidentHandlerType()).thenReturn("failedJob");

    compositeIncidentHandler.add(subHandler);
    compositeIncidentHandler.add(subHandler);
    compositeIncidentHandler.add(subHandler);

    IncidentContext incidentContext = mock(IncidentContext.class);

    compositeIncidentHandler.resolveIncident(incidentContext);

    verify(mainHandler).resolveIncident(incidentContext);
    verify(subHandler, new Times(3)).resolveIncident(incidentContext);
  }
}

