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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.operaton.bpm.engine.impl.db.DbEntity;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.impl.db.HasDbRevision;
import org.operaton.bpm.engine.impl.db.HistoricEntity;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.task.Comment;
import org.operaton.bpm.engine.task.Event;

/**
 * @author Tom Baeyens
 *
 * Implements the deprecated {@link Event} interface only to remain compatible with
 * {@code TaskService#getTaskEvents} (backed by {@code GetTaskEventsCmd}), for as long
 * as that deprecated API still exists.
 */
@SuppressWarnings("removal")
@NullMarked
public class CommentEntity implements Comment, Event, HasDbRevision, DbEntity, HistoricEntity {

  public static final String TYPE_EVENT = "event";
  public static final String TYPE_COMMENT = "comment";

  protected @Nullable String id;

  protected @Nullable String type;
  protected @Nullable String userId;
  protected @Nullable Date time;
  protected @Nullable String taskId;
  protected @Nullable String processInstanceId;
  protected @Nullable String action;
  protected @Nullable String message;
  protected @Nullable String fullMessage;
  protected @Nullable String tenantId;
  protected @Nullable String rootProcessInstanceId;
  protected @Nullable Date removalTime;
  protected int revision;

  @Override
  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<>();
    persistentState.put("message", message);
    return persistentState;
  }

  public byte @Nullable[] getFullMessageBytes() {
    return fullMessage != null ? StringUtil.toByteArray(fullMessage) : null;
  }

  public void setFullMessageBytes(byte@Nullable[] fullMessageBytes) {
    fullMessage = fullMessageBytes != null ? StringUtil.fromBytes(fullMessageBytes) : null;
  }

  public static final String MESSAGE_PARTS_MARKER = "_|_";

  public void setMessage(String[] messageParts) {
    StringBuilder stringBuilder = new StringBuilder();
    for (String part: messageParts) {
      if (part!=null) {
        stringBuilder.append(part.replace(MESSAGE_PARTS_MARKER, " | "));
        stringBuilder.append(MESSAGE_PARTS_MARKER);
      } else {
        stringBuilder.append("null");
        stringBuilder.append(MESSAGE_PARTS_MARKER);
      }
    }
    for (int i=0; i<MESSAGE_PARTS_MARKER.length(); i++) {
      stringBuilder.deleteCharAt(stringBuilder.length()-1);
    }
    message = stringBuilder.toString();
  }

  @Override
  public List<String> getMessageParts() {
    if (message==null) {
      return Collections.emptyList();
    }
    List<String> messageParts = new ArrayList<>();
    StringTokenizer tokenizer = new StringTokenizer(message, MESSAGE_PARTS_MARKER);
    while (tokenizer.hasMoreTokens()) {
      String nextToken = tokenizer.nextToken();
      if ("null".equals(nextToken)) {
        messageParts.add(null);
      } else {
        messageParts.add(nextToken);
      }
    }
    return messageParts;
  }

  // getters and setters //////////////////////////////////////////////////////

  @Override
  public @Nullable String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public @Nullable String getUserId() {
    return userId;
  }

  public void setUserId(@Nullable String userId) {
    this.userId = userId;
  }

  @Override
  public @Nullable String getTaskId() {
    return taskId;
  }

  public void setTaskId(@Nullable String taskId) {
    this.taskId = taskId;
  }

  @Override
  public @Nullable String getMessage() {
    return message;
  }

  public void setMessage(@Nullable String message) {
    this.message = message;
  }

  @Override
  public @Nullable Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }

  @Override
  public @Nullable String getProcessInstanceId() {
    return processInstanceId;
  }

  public void setProcessInstanceId(@Nullable String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public @Nullable String getType() {
    return type;
  }

  public void setType(@Nullable String type) {
    this.type = type;
  }

  @Override
  public @Nullable String getFullMessage() {
    return fullMessage;
  }

  public void setFullMessage(@Nullable String fullMessage) {
    this.fullMessage = fullMessage;
  }

  @Override
  public @Nullable String getAction() {
    return action;
  }

  public void setAction(@Nullable String action) {
    this.action = action;
  }

  public @Nullable String getTenantId() {
    return tenantId;
  }

  public void setTenantId(@Nullable String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public @Nullable String getRootProcessInstanceId() {
    return rootProcessInstanceId;
  }

  public void setRootProcessInstanceId(@Nullable String rootProcessInstanceId) {
    this.rootProcessInstanceId = rootProcessInstanceId;
  }

  @Override
  public @Nullable Date getRemovalTime() {
    return removalTime;
  }

  public void setRemovalTime(@Nullable Date removalTime) {
    this.removalTime = removalTime;
  }

  public String toEventMessage(String message) {
    String eventMessage = message.replaceAll("\\s+", " ");
    if (eventMessage.length() > 163) {
      eventMessage = eventMessage.substring(0, 160) + "...";
    }
    return eventMessage;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
           + "[id=" + id
           + ", type=" + type
           + ", userId=" + userId
           + ", time=" + time
           + ", taskId=" + taskId
           + ", processInstanceId=" + processInstanceId
           + ", rootProcessInstanceId=" + rootProcessInstanceId
           + ", revision= "+ revision
           + ", removalTime=" + removalTime
           + ", action=" + action
           + ", message=" + message
           + ", fullMessage=" + fullMessage
           + ", tenantId=" + tenantId
           + "]";
  }

  @Override
  public void setRevision(int revision) {
    this.revision = revision;
  }

  @Override
  public int getRevision() {
    return revision;
  }

  @Override
  public int getRevisionNext() {
    return revision + 1;
  }
}
