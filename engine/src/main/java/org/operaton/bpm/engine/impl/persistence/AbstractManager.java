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
package org.operaton.bpm.engine.impl.persistence;

import java.util.concurrent.Callable;

import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.impl.AbstractQuery;
import org.operaton.bpm.engine.impl.cfg.auth.ResourceAuthorizationProvider;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionManager;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionManager;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.entitymanager.DbEntityManager;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSession;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionManager;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionRequirementsDefinitionManager;
import org.operaton.bpm.engine.impl.form.entity.OperatonFormDefinitionManager;
import org.operaton.bpm.engine.impl.history.event.HistoricDecisionInstanceManager;
import org.operaton.bpm.engine.impl.identity.Authentication;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.Session;
import org.operaton.bpm.engine.impl.persistence.entity.*;


/**
 * @author Tom Baeyens
 */
public abstract class AbstractManager implements Session {
  protected static final String ACTIVITY_ID = "activityId";
  protected static final String BATCH_ID = "batchId";
  protected static final String BATCH_SIZE = "batchSize";
  protected static final String CASE_INSTANCE_IDS = "caseInstanceIds";
  protected static final String CURRENT_TIMESTAMP = "currentTimestamp";
  protected static final String ID = "id";
  protected static final String IS_PROCESS_DEFINITION_TENANT_ID_SET = "isProcessDefinitionTenantIdSet";
  protected static final String IS_TENANT_ID_SET = "isTenantIdSet";
  protected static final String JOB_DEFINITION_ID = "jobDefinitionId";
  protected static final String MAX_RESULTS = "maxResults";
  protected static final String MINUTE_FROM = "minuteFrom";
  protected static final String MINUTE_TO = "minuteTo";
  protected static final String REMOVAL_TIME = "removalTime";
  protected static final String PROCESS_DEFINITION_ID = "processDefinitionId";
  protected static final String PROCESS_DEFINITION_KEY = "processDefinitionKey";
  protected static final String PROCESS_DEFINITION_TENANT_ID = "processDefinitionTenantId";
  protected static final String PROCESS_INSTANCE_ID = "processInstanceId";
  protected static final String PROCESS_INSTANCE_IDS = "processInstanceIds";
  protected static final String ROOT_PROCESS_INSTANCE_ID = "rootProcessInstanceId";
  protected static final String SUSPENSION_STATE = "suspensionState";
  protected static final String TASK_CASE_INSTANCE_IDS = "taskCaseInstanceIds";
  protected static final String TASK_ID = "taskId";
  protected static final String TASK_PROCESS_INSTANCE_IDS = "taskProcessInstanceIds";
  protected static final String TENANT_ID = "tenantId";

  public void insert(DbEntity dbEntity) {
    getDbEntityManager().insert(dbEntity);
  }

  public void delete(DbEntity dbEntity) {
    getDbEntityManager().delete(dbEntity);
  }

  protected DbEntityManager getDbEntityManager() {
    return getSession(DbEntityManager.class);
  }

  protected DbSqlSession getDbSqlSession() {
    return getSession(DbSqlSession.class);
  }

  protected <T> T getSession(Class<T> sessionClass) {
    return Context.getCommandContext().getSession(sessionClass);
  }

  protected DeploymentManager getDeploymentManager() {
    return getSession(DeploymentManager.class);
  }

  protected ResourceManager getResourceManager() {
    return getSession(ResourceManager.class);
  }

  protected ByteArrayManager getByteArrayManager() {
    return getSession(ByteArrayManager.class);
  }

  protected ProcessDefinitionManager getProcessDefinitionManager() {
    return getSession(ProcessDefinitionManager.class);
  }

  protected CaseDefinitionManager getCaseDefinitionManager() {
    return getSession(CaseDefinitionManager.class);
  }

  protected DecisionDefinitionManager getDecisionDefinitionManager() {
    return getSession(DecisionDefinitionManager.class);
  }

  protected DecisionRequirementsDefinitionManager getDecisionRequirementsDefinitionManager() {
    return getSession(DecisionRequirementsDefinitionManager.class);
  }

  protected OperatonFormDefinitionManager getOperatonFormDefinitionManager() {
    return getSession(OperatonFormDefinitionManager.class);
  }

  protected HistoricDecisionInstanceManager getHistoricDecisionInstanceManager() {
    return getSession(HistoricDecisionInstanceManager.class);
  }

  protected CaseExecutionManager getCaseInstanceManager() {
    return getSession(CaseExecutionManager.class);
  }

  protected CaseExecutionManager getCaseExecutionManager() {
    return getSession(CaseExecutionManager.class);
  }

  protected ExecutionManager getProcessInstanceManager() {
    return getSession(ExecutionManager.class);
  }

  protected TaskManager getTaskManager() {
    return getSession(TaskManager.class);
  }

  protected TaskReportManager getTaskReportManager() {
    return getSession(TaskReportManager.class);
  }

  protected IdentityLinkManager getIdentityLinkManager() {
    return getSession(IdentityLinkManager.class);
  }

  protected VariableInstanceManager getVariableInstanceManager() {
    return getSession(VariableInstanceManager.class);
  }

  protected HistoricProcessInstanceManager getHistoricProcessInstanceManager() {
    return getSession(HistoricProcessInstanceManager.class);
  }

  protected HistoricCaseInstanceManager getHistoricCaseInstanceManager() {
    return getSession(HistoricCaseInstanceManager.class);
  }

  protected HistoricDetailManager getHistoricDetailManager() {
    return getSession(HistoricDetailManager.class);
  }

  protected HistoricVariableInstanceManager getHistoricVariableInstanceManager() {
    return getSession(HistoricVariableInstanceManager.class);
  }

  protected HistoricActivityInstanceManager getHistoricActivityInstanceManager() {
    return getSession(HistoricActivityInstanceManager.class);
  }

  protected HistoricCaseActivityInstanceManager getHistoricCaseActivityInstanceManager() {
    return getSession(HistoricCaseActivityInstanceManager.class);
  }

  protected HistoricTaskInstanceManager getHistoricTaskInstanceManager() {
    return getSession(HistoricTaskInstanceManager.class);
  }

  protected HistoricIncidentManager getHistoricIncidentManager() {
    return getSession(HistoricIncidentManager.class);
  }

  protected HistoricIdentityLinkLogManager getHistoricIdentityLinkManager() {
    return getSession(HistoricIdentityLinkLogManager.class);
  }

  protected HistoricJobLogManager getHistoricJobLogManager() {
    return getSession(HistoricJobLogManager.class);
  }

  protected HistoricExternalTaskLogManager getHistoricExternalTaskLogManager() {
    return getSession(HistoricExternalTaskLogManager.class);
  }

  protected JobManager getJobManager() {
    return getSession(JobManager.class);
  }

  protected JobDefinitionManager getJobDefinitionManager() {
    return getSession(JobDefinitionManager.class);
  }

  protected UserOperationLogManager getUserOperationLogManager() {
    return getSession(UserOperationLogManager.class);
  }

  protected EventSubscriptionManager getEventSubscriptionManager() {
    return getSession(EventSubscriptionManager.class);
  }

  protected IdentityInfoManager getIdentityInfoManager() {
    return getSession(IdentityInfoManager.class);
  }

  protected AttachmentManager getAttachmentManager() {
    return getSession(AttachmentManager.class);
  }

  protected ReportManager getHistoricReportManager() {
    return getSession(ReportManager.class);
  }

  protected BatchManager getBatchManager() {
    return getSession(BatchManager.class);
  }

  protected HistoricBatchManager getHistoricBatchManager() {
    return getSession(HistoricBatchManager.class);
  }

  protected TenantManager getTenantManager() {
    return getSession(TenantManager.class);
  }

  // authorizations ///////////////////////////////////////

  protected CommandContext getCommandContext() {
    return Context.getCommandContext();
  }

  protected AuthorizationManager getAuthorizationManager() {
    return getSession(AuthorizationManager.class);
  }

  protected void configureQuery(AbstractQuery<?,?> query, Resource resource) {
    getAuthorizationManager().configureQuery(query, resource);
  }

  protected void checkAuthorization(Permission permission, Resource resource, String resourceId) {
    getAuthorizationManager().checkAuthorization(permission, resource, resourceId);
  }

  public boolean isAuthorizationEnabled() {
    return Context.getProcessEngineConfiguration().isAuthorizationEnabled();
  }

  protected Authentication getCurrentAuthentication() {
    return Context.getCommandContext().getAuthentication();
  }

  protected ResourceAuthorizationProvider getResourceAuthorizationProvider() {
    return Context.getProcessEngineConfiguration()
        .getResourceAuthorizationProvider();
  }

  protected void deleteAuthorizations(Resource resource, String resourceId) {
    getAuthorizationManager().deleteAuthorizationsByResourceId(resource, resourceId);
  }

  protected void deleteAuthorizationsForUser(Resource resource, String resourceId, String userId) {
    getAuthorizationManager().deleteAuthorizationsByResourceIdAndUserId(resource, resourceId, userId);
  }

  protected void deleteAuthorizationsForGroup(Resource resource, String resourceId, String groupId) {
    getAuthorizationManager().deleteAuthorizationsByResourceIdAndGroupId(resource, resourceId, groupId);
  }

  public void saveDefaultAuthorizations(final AuthorizationEntity[] authorizations) {
    if(authorizations != null && authorizations.length > 0) {
      Context.getCommandContext().runWithoutAuthorization((Callable<Void>) () -> {
        AuthorizationManager authorizationManager = getAuthorizationManager();
        for (AuthorizationEntity authorization : authorizations) {

          if (authorization.getId() == null) {
            authorizationManager.insert(authorization);
          } else {
            authorizationManager.update(authorization);
          }

        }
        return null;
      });
    }
  }

  public void deleteDefaultAuthorizations(final AuthorizationEntity[] authorizations) {
    if(authorizations != null && authorizations.length > 0) {
      Context.getCommandContext().runWithoutAuthorization((Callable<Void>) () -> {
        AuthorizationManager authorizationManager = getAuthorizationManager();
        for (AuthorizationEntity authorization : authorizations) {
          authorizationManager.delete(authorization);
        }
        return null;
      });
    }
  }

}
