/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.health;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.operaton.bpm.engine.health.FrontendHealthContributor;
import org.operaton.bpm.engine.health.HealthResult;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DefaultHealthServiceTest {

  @Test
  void shouldBeUpWhenNoDataSourceProvided() throws Exception {
    JobExecutor jobExecutor = mock(JobExecutor.class);
    when(jobExecutor.isActive()).thenReturn(true);
    @SuppressWarnings("unchecked")
    Iterator<Object> it = (Iterator<Object>) Mockito.mock(Iterator.class);
    when(it.hasNext()).thenReturn(true);
    when(jobExecutor.engineIterator()).thenReturn((Iterator) it);

    FrontendHealthContributor frontend = () -> Collections.singletonMap("operational", true);

    DefaultHealthService service = new DefaultHealthService(null, jobExecutor, frontend);
    HealthResult result = service.check();

    assertThat(result.status()).isEqualTo("UP");
    assertThat(result.details()).containsKey("database");
    Map<String, Object> db = (Map<String, Object>) result.details().get("database");
    assertThat(db.get("connected")).isEqualTo(false);
    Map<String, Object> frontendDetails = (Map<String, Object>) result.details().get("frontend");
    assertThat(frontendDetails.get("operational")).isEqualTo(true);
  }

    @Test
    void shouldReportDatabaseUp() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.isValid(anyInt())).thenReturn(true);

        JobExecutor jobExecutor = mock(JobExecutor.class);

        Iterator<?> iterator = Collections.emptyIterator();
        when(jobExecutor.engineIterator()).thenReturn((Iterator<ProcessEngineImpl>) iterator);

        DefaultHealthService service = new DefaultHealthService(ds, jobExecutor, null);
        HealthResult result = service.check();

        assertThat(result.status()).isEqualTo("UP");
        Map<String, Object> db = (Map<String, Object>) result.details().get("database");
        assertThat(db.get("connected")).isEqualTo(true);
    }

  @Test
  void shouldReportDatabaseDownOnException() throws Exception {
    DataSource ds = mock(DataSource.class);
    when(ds.getConnection()).thenThrow(new RuntimeException("boom"));

    DefaultHealthService service = new DefaultHealthService(ds, null, null);
    HealthResult result = service.check();

    assertThat(result.status()).isEqualTo("DOWN");
    Map<String, Object> db = (Map<String, Object>) result.details().get("database");
    assertThat(db.get("connected")).isEqualTo(false);
    assertThat(db).containsKey("error");
  }
}
