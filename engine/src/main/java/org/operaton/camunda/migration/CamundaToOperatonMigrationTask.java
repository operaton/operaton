package org.operaton.camunda.migration;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.operaton.bpm.engine.repository.ResourceTypes;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.dmn.Dmn;
import org.operaton.bpm.model.dmn.DmnModelInstance;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;

/**
 * <p>Task for resources in database runtime migration.
 * Do nothing if current database is not on latest version of Camunda 7.
 * Creates new deployments and saves artifacts there.</p>
 */
public class CamundaToOperatonMigrationTask {

  public static final String CAMUNDA_TO_OPERATON_RPOPERTY_NAME = "operaton.migratedFromCamunda";

  private static final int BATCH_SIZE = 100;
  private static final String CAMUNDA_LAST_VERSION = "7.24.0";
  private static final String OPERATON_VERSION_PART = "1.0.0";


  private static final TimeBasedGenerator timeBasedGenerator = Generators.timeBasedGenerator(
      EthernetAddress.fromInterface());

  public void execute(Connection jdbcConnection) {
    try (final Statement statement = jdbcConnection.createStatement();
        final PreparedStatement byteArrayStatement = jdbcConnection.prepareStatement(
            "SELECT ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_, TENANT_ID_, TYPE_, CREATE_TIME_, ROOT_PROC_INST_ID_, "
                + "REMOVAL_TIME_ FROM ACT_GE_BYTEARRAY OFFSET ? LIMIT ? ; ");
        final PreparedStatement deploymentStatement = jdbcConnection.prepareStatement(
            "SELECT ID_, NAME_, DEPLOY_TIME_, SOURCE_, TENANT_ID_ FROM ACT_RE_DEPLOYMENT WHERE ID_ = ?;");
        final PreparedStatement insertDeploymentStatement = jdbcConnection.prepareStatement(
            "INSERT INTO ACT_RE_DEPLOYMENT (ID_, NAME_, DEPLOY_TIME_, SOURCE_, TENANT_ID_) "
                + "VALUES (?, ?, ?, ?, ?); ");
        final PreparedStatement insertResourceStatement = jdbcConnection.prepareStatement(
            "INSERT INTO ACT_GE_BYTEARRAY (ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_, "
                + "TENANT_ID_, TYPE_, CREATE_TIME_, ROOT_PROC_INST_ID_, REMOVAL_TIME) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ");
        final PreparedStatement selectProcessDefinitionStatement = jdbcConnection.prepareStatement("SELECT ID_, REV_, "
            + "category_, name_, key_, version_, deployment_id_, resource_name_, dgrm_resource_name_, has_start_form_key_, suspension_state_, tenant_id_, version_tag_, history_ttl_, startable_ "
            + "FROM ACT_RE_PROCDEF WHERE resource_name_ = ? ORDER BY VERSION_ DESC LIMIT 1;");
        final PreparedStatement insertProcessDefinitionStatement = jdbcConnection.prepareStatement(
            "INSERT INTO ACT_RE_PROCDEF( "
                + "id_, rev_, category_, name_, key_, version_, deployment_id_, resource_name_, dgrm_resource_name_, has_start_form_key_, suspension_state_, tenant_id_, version_tag_, history_ttl_, startable_) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
        final PreparedStatement selectDecisionDefinitionStatement = jdbcConnection.prepareStatement("SELECT id_, rev_, "
            + "category_, name_, key_, version_, deployment_id_, resource_name_, dgrm_resource_name_, dec_req_id_, dec_req_key_, tenant_id_, history_ttl_, version_tag_ "
            + "FROM ACT_RE_DECISION_DEF WHERE resource_name_ = ?;");
        final PreparedStatement insertDecisionDefinitionStatement = jdbcConnection.prepareStatement(
            "INSERT INTO ACT_RE_DECISION_DEF( "
                + "id_, rev_, category_, name_, key_, version_, deployment_id_, resource_name_, dgrm_resource_name_, dec_req_id_, dec_req_key_, tenant_id_, history_ttl_, version_tag_) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
      if (!isCurrentVersionLastCamunda(statement) || isAlreadyMigrated(statement)) {
        return;
      }

      boolean isEnd = false;
      int offset = 0;
      final Map<String, String> updatedDeployments = new HashMap<>();
      while (!isEnd) {
        isEnd = true;
        byteArrayStatement.setInt(1, offset);
        byteArrayStatement.setInt(2, BATCH_SIZE);
        try (final ResultSet byteArrayResultSet = byteArrayStatement.executeQuery()) {

          while (byteArrayResultSet.next()) {
            isEnd = false;
            String id = byteArrayResultSet.getString(1);
            int rev = byteArrayResultSet.getInt(2);
            String name = byteArrayResultSet.getString(3);
            String deploymentId = byteArrayResultSet.getString(4);
            byte[] bytes = byteArrayResultSet.getBytes(5);
            boolean generated = byteArrayResultSet.getBoolean(6);
            String tenantId = byteArrayResultSet.getString(7);
            String type = byteArrayResultSet.getString(8);
            Date createTime = byteArrayResultSet.getDate(9);
            String rootProcInstId = byteArrayResultSet.getString(10);
            String removalTime = byteArrayResultSet.getString(11);

            if (!updatedDeployments.containsKey(deploymentId)) {
              deploymentStatement.setString(1, deploymentId);
              try (final ResultSet deploymentRs = deploymentStatement.executeQuery()) {
                deploymentRs.next();
                String deploymentName = deploymentRs.getString(2);
                String deploymentDeployTime = deploymentRs.getString(3);
                String deploymentSource = deploymentRs.getString(4);
                String deploymentTenantId = deploymentRs.getString(5);

                final String deploymentNewId = insertDeployment(insertDeploymentStatement, deploymentName,
                    deploymentDeployTime, deploymentSource, deploymentTenantId);

                updatedDeployments.put(deploymentId, deploymentNewId);
              }
            }
            final String deploymentNewId = updatedDeployments.get(deploymentId);

            if (Objects.equals(type, ResourceTypes.REPOSITORY.getValue().toString()) && name.endsWith(".bpmn")) {
              byte[] result = transformBytesForBpmn(bytes);

              insertResource(insertResourceStatement, rev, name, deploymentNewId, result, generated, tenantId, type,
                  createTime, rootProcInstId, removalTime);

              updateBpmnProcessDefinitions(selectProcessDefinitionStatement, name, insertProcessDefinitionStatement,
                  deploymentNewId);
            } else if (Objects.equals(type, ResourceTypes.REPOSITORY.getValue().toString()) && name.endsWith(".dmn")) {
              byte[] result = transformBytesForDmn(bytes);

              insertResource(insertResourceStatement, rev, name, deploymentNewId, result, generated, tenantId, type,
                  createTime, rootProcInstId, removalTime);

              updateDmnProcessDefinitions(selectDecisionDefinitionStatement, name, insertDecisionDefinitionStatement,
                  deploymentNewId);
            }
          }

          offset += BATCH_SIZE;
        }
      }

      setMigratedProp(statement);
    } catch (Exception e) {
      // swallow the exception !
    }
  }

  private boolean isAlreadyMigrated(Statement statement) {
    try {
      final ResultSet resultSet = statement.executeQuery(
          "SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_ = '" + CAMUNDA_TO_OPERATON_RPOPERTY_NAME + "'");

      if (!resultSet.next()) {
        return false;
      }

      return resultSet.getBoolean(1);

    } catch (SQLException e) {
      return false;
    }
  }

  private void setMigratedProp(Statement statement) {
    try {
      statement.execute("INSERT INTO ACT_GE_PROPERTY (NAME_, VALUE_, REV_) "
          + "VALUES ('" + CAMUNDA_TO_OPERATON_RPOPERTY_NAME + "', true, 1);");
    } catch (SQLException e) {
      // do nothing
    }
  }

  private void updateDmnProcessDefinitions(PreparedStatement selectDecisionDefinitionStatement,
                                                  String name,
                                                  PreparedStatement insertDecisionDefinitionStatement,
                                                  String deploymentNewId) throws SQLException {
    selectDecisionDefinitionStatement.setString(1, name);
    try (ResultSet decisionDefRS = selectDecisionDefinitionStatement.executeQuery()) {
      while (decisionDefRS.next()) {
        String decisionDefId = decisionDefRS.getString(1);
        Integer decisionDefRevision = decisionDefRS.getInt(2);
        String decisionDefCategory = decisionDefRS.getString(3);
        String decisionDefName = decisionDefRS.getString(4);
        String decisionDefKey = decisionDefRS.getString(5);
        Integer decisionDefVersion = decisionDefRS.getInt(6);
        String decisionDefDeploymentId = decisionDefRS.getString(7);
        String decisionDefResourceName = decisionDefRS.getString(8);
        String decisionDefDgrmResourceName = decisionDefRS.getString(9);
        String decisionDefDecReqId = decisionDefRS.getString(10);
        String decisionDefDecReqKey = decisionDefRS.getString(11);
        String decisionDefTenantId = decisionDefRS.getString(12);
        Integer decisionDefHistoryTtl = decisionDefRS.getInt(13);
        String decisionDefVersionTag = decisionDefRS.getString(14);

        insertDecisionDefinition(insertDecisionDefinitionStatement, deploymentNewId, decisionDefRevision,
            decisionDefCategory, decisionDefName, decisionDefKey, decisionDefVersion, decisionDefResourceName,
            decisionDefDgrmResourceName, decisionDefDecReqId, decisionDefDecReqKey, decisionDefTenantId,
            decisionDefHistoryTtl, decisionDefVersionTag);
      }
    }
  }


  private void updateBpmnProcessDefinitions(PreparedStatement selectProcessDefinitionStatement,
                                                   String name,
                                                   PreparedStatement insertProcessDefinitionStatement,
                                                   String deploymentNewId) throws SQLException {
    int offset = 0;
    boolean isEnd = false;
    selectProcessDefinitionStatement.setString(1, name);
    while (!isEnd) {
      selectProcessDefinitionStatement.setInt(2, offset);
      selectProcessDefinitionStatement.setInt(3, BATCH_SIZE);

      try (ResultSet processDefRS = selectProcessDefinitionStatement.executeQuery()) {
        isEnd = true;
        while (processDefRS.next()) {
          isEnd = false;
          String processDefId = processDefRS.getString(1);
          Integer processDefRevision = processDefRS.getInt(2);
          String processDefCategory = processDefRS.getString(3);
          String processDefName = processDefRS.getString(4);
          String processDefKey = processDefRS.getString(5);
          Integer processDefVersion = processDefRS.getInt(6);
          String processDefDeploymentId = processDefRS.getString(7);
          String processDefResourceName = processDefRS.getString(8);
          String processDefDgrmResourceName = processDefRS.getString(9);
          Boolean processDefHasStartFormKey = processDefRS.getBoolean(10);
          Integer processDefSuspensionState = processDefRS.getInt(11);
          String processDefTenantId = processDefRS.getString(12);
          String processDefVersionTag = processDefRS.getString(13);
          Integer processDefHistoryTtl = processDefRS.getInt(14);
          Boolean processDefStartable = processDefRS.getBoolean(15);

          insertProcessDefinition(insertProcessDefinitionStatement, deploymentNewId, processDefRevision,
              processDefCategory, processDefName, processDefKey, processDefVersion, processDefResourceName,
              processDefDgrmResourceName, processDefHasStartFormKey, processDefSuspensionState, processDefTenantId,
              processDefVersionTag, processDefHistoryTtl, processDefStartable);
        }

      }
      offset += BATCH_SIZE;
    }
  }


  private boolean isCurrentVersionLastCamunda(Statement statement) throws SQLException {
    boolean isOperaton = false;
    boolean isLatestCamunda = false;

    try (ResultSet versionLogRs = statement.executeQuery("SELECT id_, timestamp_, version_ FROM ACT_GE_SCHEMA_LOG")) {
      while (versionLogRs.next()) {
        String entryVersion = versionLogRs.getString("version_");
        if (CAMUNDA_LAST_VERSION.equals(entryVersion)) {
          isLatestCamunda = true;
        } else if (entryVersion != null && entryVersion.contains(OPERATON_VERSION_PART)) {
          isOperaton = true;
        }
      }
    }

    return isLatestCamunda && !isOperaton;
  }

  private byte[] transformBytesForDmn(byte[] bytes) {
    final InputStream inputStream = new ByteArrayInputStream(bytes);

    final DmnModelInstance dmnModelInstance = Dmn.readModelFromStream(inputStream);
    final DmnModelInstance instanceClone = dmnModelInstance.clone();

    final ModelElementInstance documentElement = instanceClone.getDocumentElement();

    final String replacedText = documentElement.getTextContent()
        .replace("org.camunda.bpm", "org.operaton.bpm")
        .replace("org.camunda.spin", "org.operaton.spin")
        .replace("org.camunda.commons", "org.operaton.commons")
        .replace("org.camunda.connect", "org.operaton.connect");
    documentElement.setTextContent(replacedText);
    instanceClone.setDocumentElement(documentElement);
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Dmn.writeModelToStream(outputStream, instanceClone);
    return outputStream.toByteArray();
  }

  private byte[] transformBytesForBpmn(byte[] bytes) {
    final InputStream inputStream = new ByteArrayInputStream(bytes);

    final BpmnModelInstance bpmnModelInstance = Bpmn.readModelFromStream(inputStream);
    final BpmnModelInstance instanceClone = bpmnModelInstance.clone();

    final ModelElementInstance documentElement = instanceClone.getDocumentElement();

    final String replacedText = documentElement.getTextContent()
        .replace("org.camunda.bpm", "org.operaton.bpm")
        .replace("org.camunda.spin", "org.operaton.spin")
        .replace("org.camunda.commons", "org.operaton.commons")
        .replace("org.camunda.connect", "org.operaton.connect");
    documentElement.setTextContent(replacedText);
    instanceClone.setDocumentElement(documentElement);
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Bpmn.writeModelToStream(outputStream, instanceClone);
    return outputStream.toByteArray();
  }

  private void insertResource(PreparedStatement insertResourceStatement,
                              int rev,
                              String name,
                              String deploymentNewId,
                              byte[] result,
                              boolean generated,
                              String tenantId,
                              String type,
                              Date createTime,
                              String rootProcInstId,
                              String removalTime) throws SQLException {
    insertResourceStatement.setString(1, timeBasedGenerator.generate().toString());
    insertResourceStatement.setInt(2, rev);
    insertResourceStatement.setString(3, name);
    insertResourceStatement.setString(4, deploymentNewId);
    insertResourceStatement.setBytes(5, result);
    insertResourceStatement.setBoolean(6, generated);
    insertResourceStatement.setString(7, tenantId);
    insertResourceStatement.setString(8, type);
    insertResourceStatement.setDate(9, createTime);
    insertResourceStatement.setString(10, rootProcInstId);
    insertResourceStatement.setString(11, removalTime);
    insertResourceStatement.execute();
  }

  private String insertDeployment(PreparedStatement insertDeploymentStatement,
                                  String deploymentName,
                                  String deploymentDeployTime,
                                  String deploymentSource,
                                  String deploymentTenantId) throws SQLException {
    final String deploymentNewId = timeBasedGenerator.generate().toString();
    insertDeploymentStatement.setString(1, deploymentNewId);
    insertDeploymentStatement.setString(2, deploymentName);
    insertDeploymentStatement.setString(3, deploymentDeployTime);
    insertDeploymentStatement.setString(4, deploymentSource);
    insertDeploymentStatement.setString(5, deploymentTenantId);
    insertDeploymentStatement.execute();
    return deploymentNewId;
  }

  private void insertDecisionDefinition(PreparedStatement insertDecisionDefinitionStatement,
                                        String deploymentNewId,
                                        Integer decisionDefRevision,
                                        String decisionDefCategory,
                                        String decisionDefName,
                                        String decisionDefKey,
                                        Integer decisionDefVersion,
                                        String decisionDefResourceName,
                                        String decisionDefDgrmResourceName,
                                        String decisionDefDecReqId,
                                        String decisionDefDecReqKey,
                                        String decisionDefTenantId,
                                        Integer decisionDefHistoryTtl,
                                        String decisionDefVersionTag) throws SQLException {
    insertDecisionDefinitionStatement.setString(1, timeBasedGenerator.generate().toString());
    insertDecisionDefinitionStatement.setInt(2, decisionDefRevision);
    insertDecisionDefinitionStatement.setString(3, decisionDefCategory);
    insertDecisionDefinitionStatement.setString(4, decisionDefName);
    insertDecisionDefinitionStatement.setString(5, decisionDefKey);
    insertDecisionDefinitionStatement.setInt(6, 1 + decisionDefVersion);
    insertDecisionDefinitionStatement.setString(7, deploymentNewId);
    insertDecisionDefinitionStatement.setString(8, decisionDefResourceName);
    insertDecisionDefinitionStatement.setString(9, decisionDefDgrmResourceName);
    insertDecisionDefinitionStatement.setString(10, decisionDefDecReqId);
    insertDecisionDefinitionStatement.setString(11, decisionDefDecReqKey);
    insertDecisionDefinitionStatement.setString(12, decisionDefTenantId);
    insertDecisionDefinitionStatement.setInt(13, decisionDefHistoryTtl);
    insertDecisionDefinitionStatement.setString(14, decisionDefVersionTag);
    insertDecisionDefinitionStatement.execute();
  }

  private void insertProcessDefinition(PreparedStatement insertProcessDefinitionStatement,
                                       String deploymentNewId,
                                       Integer processDefRevision,
                                       String processDefCategory,
                                       String processDefName,
                                       String processDefKey,
                                       Integer processDefVersion,
                                       String processDefResourceName,
                                       String processDefDgrmResourceName,
                                       Boolean processDefHasStartFormKey,
                                       Integer processDefSuspensionState,
                                       String processDefTenantId,
                                       String processDefVersionTag,
                                       Integer processDefHistoryTtl,
                                       Boolean processDefStartable) throws SQLException {
    insertProcessDefinitionStatement.setString(1, timeBasedGenerator.generate().toString());
    insertProcessDefinitionStatement.setInt(2, processDefRevision);
    insertProcessDefinitionStatement.setString(3, processDefCategory);
    insertProcessDefinitionStatement.setString(4, processDefName);
    insertProcessDefinitionStatement.setString(5, processDefKey);
    insertProcessDefinitionStatement.setInt(6, 1 + processDefVersion);
    insertProcessDefinitionStatement.setString(7, deploymentNewId);
    insertProcessDefinitionStatement.setString(8, processDefResourceName);
    insertProcessDefinitionStatement.setString(9, processDefDgrmResourceName);
    insertProcessDefinitionStatement.setBoolean(10, processDefHasStartFormKey);
    insertProcessDefinitionStatement.setInt(11, processDefSuspensionState);
    insertProcessDefinitionStatement.setString(12, processDefTenantId);
    insertProcessDefinitionStatement.setString(13, processDefVersionTag);
    insertProcessDefinitionStatement.setInt(14, processDefHistoryTtl);
    insertProcessDefinitionStatement.setBoolean(15, processDefStartable);
    insertProcessDefinitionStatement.execute();
  }


}
