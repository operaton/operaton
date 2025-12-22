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
package org.operaton.bpm.engine.impl.persistence.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.engine.identity.Tenant;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.HasDbRevision;

public class TenantEntity implements Tenant, Serializable, DbEntity, HasDbRevision {

  @Serial private static final long serialVersionUID = 1L;

  protected String id;
  protected String name;

  protected int revision;

  public TenantEntity() {
  }

  public TenantEntity(String id) {
    this.id = id;
  }

  @Override
  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<>();
    persistentState.put("name", name);
    return persistentState;
  }

  @Override
  public int getRevisionNext() {
    return revision + 1;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public int getRevision() {
    return revision;
  }

  @Override
  public void setRevision(int revision) {
    this.revision = revision;
  }

  @Override
  public String toString() {
    return "TenantEntity [id=" + id + ", name=" + name + ", revision=" + revision + "]";
  }

}
