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
package org.operaton.bpm.engine.rest.dto.batch;

import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.batch.BatchQuery;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BatchQueryDtoTest {
    @Mock
    BatchQuery query;

    @Mock
    MultivaluedMap<String, String> queryParameters;

    @InjectMocks
    BatchQueryDto dto;

    @Test
    void applyFilters_batchId_whenValueIsGiven() {
        // given
        dto.setBatchId("12345");

        // when
        dto.applyFilters(query);

        // th
        verify(query).batchId("12345");
    }

    @Test
    void applyFilters_batchId_whenValueIsNotSet() {
        // when
        dto.applyFilters(query);

        // th
        verify(query, never()).batchId(any());
    }

    @Test
    void applyFilters_type_whenValueIsGiven() {
        // given
        dto.setType("12345");

        // when
        dto.applyFilters(query);

        // then
        verify(query).type("12345");
    }

    @Test
    void applyFilters_type_whenValueIsNotSet() {
        // when
        dto.applyFilters(query);

        // then
        verify(query, never()).type(any());
    }

    @Test
    void applyFilters_withoutTenantId_whenValueIsTrue() {
        // given
        dto.setWithoutTenantId(true);

        // when
        dto.applyFilters(query);

        // then
        verify(query).withoutTenantId();
    }

    @Test
    void applyFilters_withoutTenantId_whenValueIsFalse() {
        // when
        dto.applyFilters(query);

        // then
        verify(query, never()).withoutTenantId();
    }

    @Test
    void applyFilters_tenantIds_whenValueIsGiven() {
        // given
        var tenantIds = List.of("tenant1", "tenant2");
        dto.setTenantIdIn(tenantIds);

        // when
        dto.applyFilters(query);

        // then
        verify(query).tenantIdIn("tenant1", "tenant2");
    }

    @Test
    void applyFilters_tenantIds_whenValueIsNotSet() {
        // when
        dto.applyFilters(query);

        // then
        verify(query, never()).tenantIdIn(any());
    }

    @Test
    void applyFilters_suspended_whenValueIsTrue() {
        // given
        dto.setSuspended(true);

        // when
        dto.applyFilters(query);

        // then
        verify(query).suspended();
    }

    @Test
    void applyFilters_suspended_whenValueIsFalse() {
        // given
        dto.setSuspended(false);

        // when
        dto.applyFilters(query);

        // then
        verify(query).active();
    }

    @Test
    void applyFilters_suspended_whenValueIsNotSet() {
        // when
        dto.applyFilters(query);

        // then
        verify(query, never()).suspended();
        verify(query, never()).active();
    }

    @Test
    void applySortBy_whenSortByIs_batchId() {
        // given
        dto.setSortBy("batchId");

        // when
        dto.applySortBy(query, "batchId", null, null);

        // then
        verify(query).orderById();
    }

    @Test
    void applySortBy_whenSortByIs_tenantId () {
        // given
        dto.setSortBy("tenantId");

        // when
        dto.applySortBy(query, "tenantId", null, null);

        // then
        verify(query).orderByTenantId();
    }
}
