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
package org.operaton.bpm.engine.impl.cmmn.model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.delegate.VariableListener;
import org.operaton.bpm.engine.impl.cmmn.behavior.CmmnActivityBehavior;
import org.operaton.bpm.engine.impl.core.model.CoreActivity;
import org.operaton.bpm.model.cmmn.instance.CmmnElement;

/**
 * @author Roman Smirnov
 *
 */
@SuppressWarnings("java:S1948")
public class CmmnActivity extends CoreActivity {

  @Serial private static final long serialVersionUID = 1L;

  protected List<CmmnActivity> activities = new ArrayList<>();
  private Map<String, CmmnActivity> namedActivities = new HashMap<>();

  private CmmnElement cmmnElement;

  private CmmnActivityBehavior activityBehavior;

  protected CmmnCaseDefinition caseDefinition;

  private CmmnActivity parent;

  private List<CmmnSentryDeclaration> sentries = new ArrayList<>();
  private Map<String, CmmnSentryDeclaration> sentryMap = new HashMap<>();

  private List<CmmnSentryDeclaration> entryCriteria = new ArrayList<>();
  private List<CmmnSentryDeclaration> exitCriteria = new ArrayList<>();

  // eventName => activity id => variable listeners
  private Map<String, Map<String, List<VariableListener<?>>>> resolvedVariableListeners;
  private Map<String, Map<String, List<VariableListener<?>>>> resolvedBuiltInVariableListeners;

  public CmmnActivity(String id, CmmnCaseDefinition caseDefinition) {
    super(id);
    this.caseDefinition = caseDefinition;
  }

  // create a new activity ///////////////////////////////////////

  @Override
  public CmmnActivity createActivity(String activityId) {
    CmmnActivity activity = new CmmnActivity(activityId, caseDefinition);
    if (activityId!=null) {
      namedActivities.put(activityId, activity);
    }
    activity.setParent(this);
    activities.add(activity);
    return activity;
  }

  // activities ////////////////////////////////////////////////

  @Override
  public List<CmmnActivity> getActivities() {
    return activities;
  }

  @Override
  public CmmnActivity findActivity(String activityId) {
    return (CmmnActivity) super.findActivity(activityId);
  }

  // child activity ////////////////////////////////////////////

  @Override
  public CmmnActivity getChildActivity(String activityId) {
    return namedActivities.get(activityId);
  }

  // behavior //////////////////////////////////////////////////

  @Override
  public CmmnActivityBehavior getActivityBehavior() {
    return activityBehavior;
  }

  public void setActivityBehavior(CmmnActivityBehavior behavior) {
    this.activityBehavior = behavior;
  }

  // parent ////////////////////////////////////////////////////

  public CmmnActivity getParent() {
    return this.parent;
  }

  public void setParent(CmmnActivity parent) {
    this.parent = parent;
  }

  // case definition

  public CmmnCaseDefinition getCaseDefinition() {
    return caseDefinition;
  }

  public void setCaseDefinition(CmmnCaseDefinition caseDefinition) {
    this.caseDefinition = caseDefinition;
  }

  // cmmn element

  public CmmnElement getCmmnElement() {
    return cmmnElement;
  }

  public void setCmmnElement(CmmnElement cmmnElement) {
    this.cmmnElement = cmmnElement;
  }

  // sentry

  public List<CmmnSentryDeclaration> getSentries() {
    return sentries;
  }

  public CmmnSentryDeclaration getSentry(String sentryId) {
    return sentryMap.get(sentryId);
  }

  public void addSentry(CmmnSentryDeclaration sentry) {
    sentryMap.put(sentry.getId(), sentry);
    sentries.add(sentry);
  }

  // entryCriteria

  public List<CmmnSentryDeclaration> getEntryCriteria() {
    return entryCriteria;
  }

  public void setEntryCriteria(List<CmmnSentryDeclaration> entryCriteria) {
    this.entryCriteria = entryCriteria;
  }

  public void addEntryCriteria(CmmnSentryDeclaration entryCriteria) {
    this.entryCriteria.add(entryCriteria);
  }

  // exitCriteria

  public List<CmmnSentryDeclaration> getExitCriteria() {
    return exitCriteria;
  }

  public void setExitCriteria(List<CmmnSentryDeclaration> exitCriteria) {
    this.exitCriteria = exitCriteria;
  }

  public void addExitCriteria(CmmnSentryDeclaration exitCriteria) {
    this.exitCriteria.add(exitCriteria);
  }

  // variable listeners

  /**
   * Returns a map of all variable listeners defined on this activity or any of
   * its parents activities. The map's key is the id of the respective activity
   * the listener is defined on.
   */
  public Map<String, List<VariableListener<?>>> getVariableListeners(String eventName, boolean includeCustomListeners) {
    Map<String, Map<String, List<VariableListener<?>>>> listenerCache = getVariableListenerCache(includeCustomListeners);

    return listenerCache.computeIfAbsent(eventName, k -> resolveVariableListeners(eventName, includeCustomListeners));
  }

  protected Map<String, Map<String, List<VariableListener<?>>>> getVariableListenerCache(boolean includeCustomListeners) {
    if (includeCustomListeners) {
      if (resolvedVariableListeners == null) {
        resolvedVariableListeners = new HashMap<>();
      }
      return resolvedVariableListeners;
    } else {
      if (resolvedBuiltInVariableListeners == null) {
        resolvedBuiltInVariableListeners = new HashMap<>();
      }
      return resolvedBuiltInVariableListeners;
    }
  }

  protected Map<String, List<VariableListener<?>>> resolveVariableListeners(String eventName, boolean includeCustomListeners) {
    Map<String, List<VariableListener<?>>> resolvedListenersForEvent = new HashMap<>();
    CmmnActivity currentActivity = this;

    while (currentActivity != null) {
      List<VariableListener<?>> localListeners = getVariableListenersLocal(currentActivity, eventName, includeCustomListeners);

      if (localListeners != null && !localListeners.isEmpty()) {
        resolvedListenersForEvent.put(currentActivity.getId(), localListeners);
      }

      currentActivity = currentActivity.getParent();
    }
    return resolvedListenersForEvent;
  }

  protected List<VariableListener<?>> getVariableListenersLocal(CmmnActivity activity, String eventName, boolean includeCustomListeners) {
    if (includeCustomListeners) {
      return activity.getVariableListenersLocal(eventName);
    } else {
      return activity.getBuiltInVariableListenersLocal(eventName);
    }
  }
}
