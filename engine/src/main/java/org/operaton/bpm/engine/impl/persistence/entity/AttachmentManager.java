/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.persistence.entity;

import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperation;
import org.operaton.bpm.engine.impl.persistence.AbstractHistoricManager;
import org.operaton.bpm.engine.task.Attachment;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tom Baeyens
 */
public class AttachmentManager extends AbstractHistoricManager {

    @SuppressWarnings("unchecked")
    public List<Attachment> findAttachmentsByProcessInstanceId(String processInstanceId) {
        checkHistoryEnabled();
        return getDbEntityManager().selectList("selectAttachmentsByProcessInstanceId", processInstanceId);
    }

    @SuppressWarnings("unchecked")
    public List<Attachment> findAttachmentsByTaskId(String taskId) {
        checkHistoryEnabled();
        return getDbEntityManager().selectList("selectAttachmentsByTaskId", taskId);
    }

    public DbOperation addRemovalTimeToAttachmentsByRootProcessInstanceId(String rootProcessInstanceId, Date removalTime,
            Integer batchSize) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(ROOT_PROCESS_INSTANCE_ID, rootProcessInstanceId);
        parameters.put(REMOVAL_TIME, removalTime);
        parameters.put(MAX_RESULTS, batchSize);

        return getDbEntityManager()
                .updatePreserveOrder(AttachmentEntity.class, "updateAttachmentsByRootProcessInstanceId", parameters);
    }

    public DbOperation addRemovalTimeToAttachmentsByProcessInstanceId(String processInstanceId, Date removalTime,
            Integer batchSize) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(PROCESS_INSTANCE_ID, processInstanceId);
        parameters.put(REMOVAL_TIME, removalTime);
        parameters.put(MAX_RESULTS, batchSize);

        return getDbEntityManager()
                .updatePreserveOrder(AttachmentEntity.class, "updateAttachmentsByProcessInstanceId", parameters);

    }

    @SuppressWarnings("unchecked")
    public void deleteAttachmentsByTaskId(String taskId) {
        checkHistoryEnabled();
        List<AttachmentEntity> attachments = getDbEntityManager().selectList("selectAttachmentsByTaskId", taskId);
        for (AttachmentEntity attachment : attachments) {
            String contentId = attachment.getContentId();
            if (contentId != null) {
                getByteArrayManager().deleteByteArrayById(contentId);
            }
            getDbEntityManager().delete(attachment);
        }
    }

    public void deleteAttachmentsByProcessInstanceIds(List<String> processInstanceIds) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(PROCESS_INSTANCE_IDS, processInstanceIds);
        deleteAttachments(parameters);
    }

    public void deleteAttachmentsByTaskProcessInstanceIds(List<String> processInstanceIds) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(TASK_PROCESS_INSTANCE_IDS, processInstanceIds);
        deleteAttachments(parameters);
    }

    public void deleteAttachmentsByTaskCaseInstanceIds(List<String> caseInstanceIds) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(CASE_INSTANCE_IDS, caseInstanceIds);
        deleteAttachments(parameters);
    }

    protected void deleteAttachments(Map<String, Object> parameters) {
        getDbEntityManager().deletePreserveOrder(ByteArrayEntity.class, "deleteAttachmentByteArraysByIds", parameters);
        getDbEntityManager().deletePreserveOrder(AttachmentEntity.class, "deleteAttachmentByIds", parameters);
    }

    public Attachment findAttachmentByTaskIdAndAttachmentId(String taskId, String attachmentId) {
        checkHistoryEnabled();

        Map<String, String> parameters = new HashMap<>();
        parameters.put(TASK_ID, taskId);
        parameters.put(ID, attachmentId);

        return (AttachmentEntity) getDbEntityManager().selectOne("selectAttachmentByTaskIdAndAttachmentId", parameters);
    }

    public DbOperation deleteAttachmentsByRemovalTime(Date removalTime, int minuteFrom, int minuteTo, int batchSize) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(REMOVAL_TIME, removalTime);
        if (minuteTo - minuteFrom + 1 < 60) {
            parameters.put(MINUTE_FROM, minuteFrom);
            parameters.put(MINUTE_TO, minuteTo);
        }
        parameters.put(BATCH_SIZE, batchSize);

        return getDbEntityManager()
                .deletePreserveOrder(AttachmentEntity.class, "deleteAttachmentsByRemovalTime",
                        new ListQueryParameterObject(parameters, 0, batchSize));
    }

}

