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
package org.operaton.bpm.engine.rest.dto.authorization;

import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.authorization.AuthorizationQuery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AuthorizationQueryDtoTest {
    @Mock
    AuthorizationQuery query;

    @Mock
    MultivaluedMap<String, String> queryParameters;

    @InjectMocks
    AuthorizationQueryDto dto;

    @Test
    void applyFilters_authorizationId_whenValueIsGiven() {
        // given
        dto.setId("12345");

        // when
        dto.applyFilters(query);

        // then
        verify(query).authorizationId("12345");
    }

    @Test
    void applyFilters_authorizationId_whenValueIsNotSet() {
        // when
        dto.applyFilters(query);

        // then
        verify(query, never()).authorizationId(any());
    }

    @Test
    void applyFilters_authorizationType_whenValueIsGiven() {
        // given
        dto.setType(12345);

        // when
        dto.applyFilters(query);

        // then
        verify(query).authorizationType(12345);
    }

    @Test
    void applyFilters_authorizationType_whenValueIsNotSet() {
        // when
        dto.applyFilters(query);

        //then
        verify(query, never()).authorizationType(any());
    }

    @Test
    void applyFilters_userIds_whenValueIsGiven() {
        // given
        var userIds = new String[]{"user1", "user2"};
        dto.setUserIdIn(userIds);

        // when
        dto.applyFilters(query);

        // then
        verify(query).userIdIn("user1", "user2");
    }

    @Test
    void applyFilters_userIds_whenValueIsNotSet() {
        // when
        dto.applyFilters(query);

        // then
        verify(query, never()).userIdIn(any());
    }

    @Test
    void applyFilters_groupIds_whenValueIsGiven() {
        // given
        var groupIds = new String[]{"group1", "group2"};
        dto.setGroupIdIn(groupIds);

        // when
        dto.applyFilters(query);

        // then
        verify(query).groupIdIn("group1", "group2");
    }

    @Test
    void applyFilters_groupIds_whenValueIsNotSet() {
        // when
        dto.applyFilters(query);

        // then
        verify(query, never()).groupIdIn(any());
    }

    @Test
    void applyFilters_resourceType_whenValueIsGiven() {
        // given
        dto.setResourceType(12345);

        // when
        dto.applyFilters(query);

        // then
        verify(query).resourceType(12345);
    }

    @Test
    void applyFilters_resourceType_whenValueIsNotSet() {
        // when
        dto.applyFilters(query);

        // then
        verify(query, never()).resourceType(any());
    }

    @Test
    void applyFilters_resourceId_whenValueIsGiven() {
        // given
        dto.setResourceId("12345");

        // when
        dto.applyFilters(query);

        // then
        verify(query).resourceId("12345");
    }

    @Test
    void applyFilters_resourceId_whenValueIsNotGiven() {
        // when
        dto.applyFilters(query);

        // then
        verify(query, never()).resourceId(any());
    }

    @Test
    void applySortBy_whenSortByIs_resourceId() {
        // given
        dto.setSortBy("resourceId");

        // when
        dto.applySortBy(query, "resourceId", null, null);

        // then
        verify(query).orderByResourceId();
    }

    @Test
    void applySortBy_whenSortByIs_resourceType() {
        // given
        dto.setSortBy("resourceType");

        //when
        dto.applySortBy(query, "resourceType", null, null);

        // then
        verify(query).orderByResourceType();
    }
}
