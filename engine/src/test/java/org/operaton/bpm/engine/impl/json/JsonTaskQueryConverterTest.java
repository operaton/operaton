/*
 * Copyright 2026 the Operaton contributors.
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

package org.operaton.bpm.engine.impl.json;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.TaskQueryImpl;
import org.operaton.bpm.engine.task.TaskQuery;

import static org.assertj.core.api.Assertions.assertThat;

class JsonTaskQueryConverterTest {

  private JsonTaskQueryConverter converter;

  @BeforeEach
  void setUp() {
    converter = new JsonTaskQueryConverter();
  }

  @Test
  void shouldConvertEmptyJsonObject() {
    JsonObject json = new JsonObject();
    TaskQuery query = converter.toObject(json);
    assertThat(query).isNotNull();
    assertThat(((TaskQueryImpl) query).getTaskId()).isNull();
  }

  @Test
  void shouldConvertSimpleFields() {
    JsonObject json = new JsonObject();
    json.addProperty(JsonTaskQueryConverter.TASK_ID, "task-id");
    json.addProperty(JsonTaskQueryConverter.NAME, "task-name");
    json.addProperty(JsonTaskQueryConverter.PRIORITY, 50);

    TaskQuery query = converter.toObject(json);
    TaskQueryImpl queryImpl = (TaskQueryImpl) query;

    assertThat(queryImpl.getTaskId()).isEqualTo("task-id");
    assertThat(queryImpl.getName()).isEqualTo("task-name");
    assertThat(queryImpl.getPriority()).isEqualTo(50);
  }

  @Test
  void shouldConvertOrQueries() {
    JsonObject json = new JsonObject();
    json.addProperty(JsonTaskQueryConverter.NAME, "main-query");

    JsonObject orJson = new JsonObject();
    orJson.addProperty(JsonTaskQueryConverter.ASSIGNEE, "assignee");

    com.google.gson.JsonArray orQueries = new com.google.gson.JsonArray();
    orQueries.add(orJson);
    json.add(JsonTaskQueryConverter.OR_QUERIES, orQueries);

    TaskQuery query = converter.toObject(json);
    TaskQueryImpl queryImpl = (TaskQueryImpl) query;

    assertThat(queryImpl.getName()).isEqualTo("main-query");
    assertThat(queryImpl.getQueries()).hasSize(2);
    assertThat(queryImpl.getQueries().get(1).getAssignee()).isEqualTo("assignee");
    assertThat(queryImpl.getQueries().get(1).isOrQueryActive()).isTrue();
  }

  @Test
  void shouldConvertExpressions() {
    JsonObject json = new JsonObject();
    json.addProperty(JsonTaskQueryConverter.ASSIGNEE + "Expression", "${myExpression}");

    TaskQuery query = converter.toObject(json);
    TaskQueryImpl queryImpl = (TaskQueryImpl) query;

    assertThat(queryImpl.getExpressions()).containsEntry(JsonTaskQueryConverter.ASSIGNEE, "${myExpression}");
  }
}
