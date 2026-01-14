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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricCaseActivityInstance;
import org.operaton.bpm.engine.history.HistoricCaseInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.HistoricFormProperty;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableUpdate;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.TablePageQueryImpl;
import org.operaton.bpm.engine.impl.batch.BatchEntity;
import org.operaton.bpm.engine.impl.batch.history.HistoricBatchEntity;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionEntity;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseSentryPartEntity;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionEntity;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionRequirementsDefinitionEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricDecisionInputInstanceEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricDecisionInstanceEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricDecisionOutputInstanceEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricDetailEventEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricExternalTaskLogEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricIncidentEventEntity;
import org.operaton.bpm.engine.impl.history.event.UserOperationLogEntryEventEntity;
import org.operaton.bpm.engine.impl.persistence.AbstractManager;
import org.operaton.bpm.engine.impl.util.DatabaseUtil;
import org.operaton.bpm.engine.management.TableMetaData;
import org.operaton.bpm.engine.management.TablePage;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;


/**
 * @author Tom Baeyens
 */
public class TableDataManager extends AbstractManager {

  protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  private static final Map<Class<?>, String> API_TYPE_TO_TABLE_NAME_MAP = new HashMap<>();
  private static final Map<Class<? extends DbEntity>, String> PERSISTENT_OBJECT_TO_TABLE_NAME_MAP = new HashMap<>();

  static {
    // runtime
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(TaskEntity.class, "ACT_RU_TASK");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(ExternalTaskEntity.class, "ACT_RU_EXT_TASK");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(ExecutionEntity.class, "ACT_RU_EXECUTION");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(IdentityLinkEntity.class, "ACT_RU_IDENTITYLINK");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(VariableInstanceEntity.class, "ACT_RU_VARIABLE");

    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(JobEntity.class, "ACT_RU_JOB");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(MessageEntity.class, "ACT_RU_JOB");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(TimerEntity.class, "ACT_RU_JOB");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(JobDefinitionEntity.class, "ACT_RU_JOBDEF");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(BatchEntity.class, "ACT_RU_BATCH");

    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(IncidentEntity.class, "ACT_RU_INCIDENT");

    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(EventSubscriptionEntity.class, "ACT_RU_EVENT_SUBSCR");


    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(MeterLogEntity.class, "ACT_RU_METER_LOG");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(TaskMeterLogEntity.class, "ACT_RU_TASK_METER_LOG");

    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(OperatonFormDefinitionEntity.class, "ACT_RE_CAMFORMDEF");
    // repository
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(DeploymentEntity.class, "ACT_RE_DEPLOYMENT");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(ProcessDefinitionEntity.class, "ACT_RE_PROCDEF");

    // CMMN
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(CaseDefinitionEntity.class, "ACT_RE_CASE_DEF");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(CaseExecutionEntity.class, "ACT_RU_CASE_EXECUTION");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(CaseSentryPartEntity.class, "ACT_RU_CASE_SENTRY_PART");

    // DMN
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(DecisionRequirementsDefinitionEntity.class, "ACT_RE_DECISION_REQ_DEF");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(DecisionDefinitionEntity.class, "ACT_RE_DECISION_DEF");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricDecisionInputInstanceEntity.class, "ACT_HI_DEC_IN");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricDecisionOutputInstanceEntity.class, "ACT_HI_DEC_OUT");

    // history
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(CommentEntity.class, "ACT_HI_COMMENT");

    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricActivityInstanceEntity.class, "ACT_HI_ACTINST");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(AttachmentEntity.class, "ACT_HI_ATTACHMENT");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricProcessInstanceEntity.class, "ACT_HI_PROCINST");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricTaskInstanceEntity.class, "ACT_HI_TASKINST");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricJobLogEventEntity.class, "ACT_HI_JOB_LOG");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricIncidentEventEntity.class, "ACT_HI_INCIDENT");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricBatchEntity.class, "ACT_HI_BATCH");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricExternalTaskLogEntity.class, "ACT_HI_EXT_TASK_LOG");

    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricCaseInstanceEntity.class, "ACT_HI_CASEINST");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricCaseActivityInstanceEntity.class, "ACT_HI_CASEACTINST");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricIdentityLinkLogEntity.class, "ACT_HI_IDENTITYLINK");
    // a couple of stuff goes to the same table
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricFormPropertyEntity.class, "ACT_HI_DETAIL");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricVariableInstanceEntity.class, "ACT_HI_VARINST");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricDetailEventEntity.class, "ACT_HI_DETAIL");

    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(HistoricDecisionInstanceEntity.class, "ACT_HI_DECINST");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(UserOperationLogEntryEventEntity.class, "ACT_HI_OP_LOG");


    // Identity module
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(GroupEntity.class, "ACT_ID_GROUP");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(MembershipEntity.class, "ACT_ID_MEMBERSHIP");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(TenantEntity.class, "ACT_ID_TENANT");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(TenantMembershipEntity.class, "ACT_ID_TENANT_MEMBER");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(UserEntity.class, "ACT_ID_USER");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(IdentityInfoEntity.class, "ACT_ID_INFO");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(AuthorizationEntity.class, "ACT_RU_AUTHORIZATION");


    // general
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(PropertyEntity.class, "ACT_GE_PROPERTY");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(ByteArrayEntity.class, "ACT_GE_BYTEARRAY");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(ResourceEntity.class, "ACT_GE_BYTEARRAY");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(SchemaLogEntryEntity.class, "ACT_GE_SCHEMA_LOG");
    PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.put(FilterEntity.class, "ACT_RU_FILTER");

    // and now the map for the API types (does not cover all cases)
    API_TYPE_TO_TABLE_NAME_MAP.put(Task.class, "ACT_RU_TASK");
    API_TYPE_TO_TABLE_NAME_MAP.put(Execution.class, "ACT_RU_EXECUTION");
    API_TYPE_TO_TABLE_NAME_MAP.put(ProcessInstance.class, "ACT_RU_EXECUTION");
    API_TYPE_TO_TABLE_NAME_MAP.put(ProcessDefinition.class, "ACT_RE_PROCDEF");
    API_TYPE_TO_TABLE_NAME_MAP.put(Deployment.class, "ACT_RE_DEPLOYMENT");
    API_TYPE_TO_TABLE_NAME_MAP.put(Job.class, "ACT_RU_JOB");
    API_TYPE_TO_TABLE_NAME_MAP.put(Incident.class, "ACT_RU_INCIDENT");
    API_TYPE_TO_TABLE_NAME_MAP.put(Filter.class, "ACT_RU_FILTER");


    // history
    API_TYPE_TO_TABLE_NAME_MAP.put(HistoricProcessInstance.class, "ACT_HI_PROCINST");
    API_TYPE_TO_TABLE_NAME_MAP.put(HistoricActivityInstance.class, "ACT_HI_ACTINST");
    API_TYPE_TO_TABLE_NAME_MAP.put(HistoricDetail.class, "ACT_HI_DETAIL");
    API_TYPE_TO_TABLE_NAME_MAP.put(HistoricVariableUpdate.class, "ACT_HI_DETAIL");
    API_TYPE_TO_TABLE_NAME_MAP.put(HistoricFormProperty.class, "ACT_HI_DETAIL");
    API_TYPE_TO_TABLE_NAME_MAP.put(HistoricTaskInstance.class, "ACT_HI_TASKINST");
    API_TYPE_TO_TABLE_NAME_MAP.put(HistoricVariableInstance.class, "ACT_HI_VARINST");


    API_TYPE_TO_TABLE_NAME_MAP.put(HistoricCaseInstance.class, "ACT_HI_CASEINST");
    API_TYPE_TO_TABLE_NAME_MAP.put(HistoricCaseActivityInstance.class, "ACT_HI_CASEACTINST");

    API_TYPE_TO_TABLE_NAME_MAP.put(HistoricDecisionInstance.class, "ACT_HI_DECINST");

    // Identity skipped for the moment as no SQL injection is provided here
  }

  public Map<String, Long> getTableCount() {
    Map<String, Long> tableCount = new HashMap<>();
    try {
      for (String tableName: getDbEntityManager().getTableNamesPresentInDatabase()) {
        tableCount.put(tableName, getTableCount(tableName));
      }
      LOG.countRowsPerProcessEngineTable(tableCount);
    } catch (Exception e) {
      throw LOG.countTableRowsException(e);
    }
    return tableCount;
  }

  protected long getTableCount(String tableName) {
    LOG.selectTableCountForTable(tableName);
    return (Long) getDbEntityManager().selectOne("selectTableCount",
            Collections.singletonMap("tableName", tableName));
  }

  @SuppressWarnings("unchecked")
  public TablePage getTablePage(TablePageQueryImpl tablePageQuery) {

    TablePage tablePage = new TablePage();

    @SuppressWarnings("rawtypes")
    List tableData = getDbEntityManager().selectList("selectTableData", tablePageQuery);

    tablePage.setTableName(tablePageQuery.getTableName());
    tablePage.setTotal(getTableCount(tablePageQuery.getTableName()));
    tablePage.setRows(tableData);
    tablePage.setFirstResult(tablePageQuery.getFirstResult());

    return tablePage;
  }

  public List<Class<? extends DbEntity>> getEntities(String tableName) {
    String databaseTablePrefix = getDbSqlSession().getDbSqlSessionFactory().getDatabaseTablePrefix();
    List<Class<? extends DbEntity>> entities = new ArrayList<>();

    Set<Class<? extends DbEntity>> entityClasses = PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.keySet();
    for (Class<? extends DbEntity> entityClass : entityClasses) {
      String entityTableName = PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.get(entityClass);
      if ((databaseTablePrefix + entityTableName).equals(tableName)) {
        entities.add(entityClass);
      }
    }
    return entities;
  }

  public String getTableName(Class<?> entityClass, boolean withPrefix) {
    String databaseTablePrefix = getDbSqlSession().getDbSqlSessionFactory().getDatabaseTablePrefix();
    String tableName = null;

    if (DbEntity.class.isAssignableFrom(entityClass)) {
      tableName = PERSISTENT_OBJECT_TO_TABLE_NAME_MAP.get(entityClass);
    }
    else {
      tableName = API_TYPE_TO_TABLE_NAME_MAP.get(entityClass);
    }
    if (withPrefix) {
      return databaseTablePrefix + tableName;
    }
    else {
      return tableName;
    }
  }

  public TableMetaData getTableMetaData(String tableName) {
    TableMetaData result = new TableMetaData();
    ResultSet resultSet = null;

    try {
      try {
        result.setTableName(tableName);
        DatabaseMetaData metaData = getDbSqlSession()
            .getSqlSession()
            .getConnection()
            .getMetaData();

        if (DatabaseUtil.checkDatabaseType(DbSqlSessionFactory.POSTGRES)) {
          tableName = tableName.toLowerCase();
        }

        String databaseSchema = getDbSqlSession().getDbSqlSessionFactory().getDatabaseSchema();
        tableName = getDbSqlSession().prependDatabaseTablePrefix(tableName);

        resultSet = metaData.getColumns(null, databaseSchema, tableName, null);
        while(resultSet.next()) {
          String name = resultSet.getString("COLUMN_NAME").toUpperCase();
          String type = resultSet.getString("TYPE_NAME").toUpperCase();
          result.addColumnMetaData(name, type);
        }

      } finally {
        if (resultSet != null) {
          resultSet.close();
        }
      }
    } catch (Exception e) {
      throw LOG.retrieveMetadataException(e);
    }

    if(result.getColumnNames().isEmpty()) {
      // According to API, when a table doesn't exist, null should be returned
      result = null;
    }
    return result;
  }

}
