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

import org.operaton.bpm.engine.impl.db.DbEntity;


/**
 * @author Tom Baeyens
 */
public class MembershipEntity implements Serializable, DbEntity {

  @Serial private static final long serialVersionUID = 1L;

  protected UserEntity user;
  protected GroupEntity group;

  /**
   * To handle a MemberhipEntity in the cache, an id is necessary.
   * Even though it is not going to be persisted in the database.
   */
  protected String id;

  @Override
  public Object getPersistentState() {
    // membership is not updatable
    return MembershipEntity.class;
  }

  @Override
  public String getId() {
    // For the sake of Entity caching the id is necessary
    return id;
  }

  @Override
  public void setId(String id) {
    // For the sake of Entity caching the id is necessary
    this.id = id;
  }

  public UserEntity getUser() {
    return user;
  }
  public void setUser(UserEntity user) {
    this.user = user;
  }
  public GroupEntity getGroup() {
    return group;
  }
  public void setGroup(GroupEntity group) {
    this.group = group;
  }

  // required for mybatis
  public String getUserId(){
	  return user.getId();
  }

  // required for mybatis
  public String getGroupId(){
	  return group.getId();
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
           + "[user=%s, group=".formatted(user) + group
           + "]";
  }
}
