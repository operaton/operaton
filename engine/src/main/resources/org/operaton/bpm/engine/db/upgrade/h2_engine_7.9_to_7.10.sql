--
-- Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
-- under one or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information regarding copyright
-- ownership. Camunda licenses this file to you under the Apache License,
-- Version 2.0; you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     https://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- limit Tasklist start-process options to the permitted subset of deployed processes
ALTER TABLE ACT_RE_PROCDEF
  ADD STARTABLE_ BOOLEAN NOT NULL DEFAULT TRUE;

-- when using the Java/Rest API, I get the information when a historic variable instance
ALTER TABLE ACT_HI_VARINST
  ADD CREATE_TIME_ TIMESTAMP;

-- when using the Java/Rest API, I get the information when a historic attachment has
ALTER TABLE ACT_HI_ATTACHMENT
  ADD CREATE_TIME_ TIMESTAMP;

-- when using the Java/Rest API, I get the information when a historic decision input
ALTER TABLE ACT_HI_DEC_IN
  ADD CREATE_TIME_ TIMESTAMP;

-- when using the Java/Rest API, I get the information when a historic dec out
ALTER TABLE ACT_HI_DEC_OUT
  ADD CREATE_TIME_ TIMESTAMP;

-- historic Process Instaces have root scope context
ALTER TABLE ACT_HI_PROCINST
  ADD ROOT_PROC_INST_ID_ varchar(64);
create index ACT_IDX_HI_PRO_INST_ROOT_PI on ACT_HI_PROCINST(ROOT_PROC_INST_ID_);

-- historic Process Instances contain removal time information
ALTER TABLE ACT_HI_PROCINST
  ADD REMOVAL_TIME_ timestamp;
create index ACT_IDX_HI_PRO_INST_RM_TIME on ACT_HI_PROCINST(REMOVAL_TIME_);

-- include the user who created a batch operation
ALTER TABLE ACT_HI_BATCH
  ADD CREATE_USER_ID_ varchar(255);
ALTER TABLE ACT_RU_BATCH
  ADD CREATE_USER_ID_ varchar(255);

-- hierarchical History Cleanup includes DMN
ALTER TABLE ACT_HI_DECINST
  ADD ROOT_PROC_INST_ID_ varchar(64);
create index ACT_IDX_HI_DEC_INST_ROOT_PI on ACT_HI_DECINST(ROOT_PROC_INST_ID_);

-- hierarchical History Cleanup includes DMN
ALTER TABLE ACT_HI_DECINST
  ADD REMOVAL_TIME_ timestamp;
create index ACT_IDX_HI_DEC_INST_RM_TIME on ACT_HI_DECINST(REMOVAL_TIME_);

-- byte array table contains type and creation date information
ALTER TABLE ACT_GE_BYTEARRAY
  ADD TYPE_ integer;

ALTER TABLE ACT_GE_BYTEARRAY
  ADD CREATE_TIME_ timestamp;

-- add root process instance id to a process instance
ALTER TABLE ACT_RU_EXECUTION
  ADD ROOT_PROC_INST_ID_ varchar(64);

create index ACT_IDX_EXEC_ROOT_PI on ACT_RU_EXECUTION(ROOT_PROC_INST_ID_);

-- add root process instance id to a historic activity instance
ALTER TABLE ACT_HI_ACTINST
  ADD ROOT_PROC_INST_ID_ varchar(64);

create index ACT_IDX_HI_ACTINST_ROOT_PI on ACT_HI_ACTINST(ROOT_PROC_INST_ID_);

-- add root process instance id to a historic task instance
ALTER TABLE ACT_HI_TASKINST
  ADD ROOT_PROC_INST_ID_ varchar(64);

create index ACT_IDX_HI_TASKINST_ROOT_PI on ACT_HI_TASKINST(ROOT_PROC_INST_ID_);

-- add root process instance id to a historic variable instance
ALTER TABLE ACT_HI_VARINST
  ADD ROOT_PROC_INST_ID_ varchar(64);

create index ACT_IDX_HI_VARINST_ROOT_PI on ACT_HI_VARINST(ROOT_PROC_INST_ID_);

-- add root process instance id to a historic detail
ALTER TABLE ACT_HI_DETAIL
  ADD ROOT_PROC_INST_ID_ varchar(64);

create index ACT_IDX_HI_DETAIL_ROOT_PI on ACT_HI_DETAIL(ROOT_PROC_INST_ID_);

-- add root process instance id to a historic comment
ALTER TABLE ACT_HI_COMMENT
  ADD ROOT_PROC_INST_ID_ varchar(64);

create index ACT_IDX_HI_COMMENT_ROOT_PI on ACT_HI_COMMENT(ROOT_PROC_INST_ID_);

-- add root process instance id to a historic attachment
ALTER TABLE ACT_HI_ATTACHMENT
  ADD ROOT_PROC_INST_ID_ varchar(64);

create index ACT_IDX_HI_ATTACHMENT_ROOT_PI on ACT_HI_ATTACHMENT(ROOT_PROC_INST_ID_);

-- add root process instance id to a historic incident
ALTER TABLE ACT_HI_INCIDENT
  ADD ROOT_PROC_INST_ID_ varchar(64);

create index ACT_IDX_HI_INCIDENT_ROOT_PI on ACT_HI_INCIDENT(ROOT_PROC_INST_ID_);

-- add root process instance id to an external task log
ALTER TABLE ACT_HI_EXT_TASK_LOG
  ADD ROOT_PROC_INST_ID_ varchar(64);

create index ACT_HI_EXT_TASK_LOG_ROOT_PI on ACT_HI_EXT_TASK_LOG(ROOT_PROC_INST_ID_);

-- add root process instance id to a historic identity link
ALTER TABLE ACT_HI_IDENTITYLINK
  ADD ROOT_PROC_INST_ID_ varchar(64);

create index ACT_IDX_HI_IDENT_LNK_ROOT_PI on ACT_HI_IDENTITYLINK(ROOT_PROC_INST_ID_);

-- add root process instance id to a job log
ALTER TABLE ACT_HI_JOB_LOG
  ADD ROOT_PROC_INST_ID_ varchar(64);

create index ACT_IDX_HI_JOB_LOG_ROOT_PI on ACT_HI_JOB_LOG(ROOT_PROC_INST_ID_);

-- add root process instance id to a user operation log
ALTER TABLE ACT_HI_OP_LOG
  ADD ROOT_PROC_INST_ID_ varchar(64);

create index ACT_IDX_HI_OP_LOG_ROOT_PI on ACT_HI_OP_LOG(ROOT_PROC_INST_ID_);

-- add root process instance id to a historic decision input and
ALTER TABLE ACT_HI_DEC_IN
  ADD ROOT_PROC_INST_ID_ varchar(64);

create index ACT_IDX_HI_DEC_IN_ROOT_PI on ACT_HI_DEC_IN(ROOT_PROC_INST_ID_);

ALTER TABLE ACT_HI_DEC_OUT
  ADD ROOT_PROC_INST_ID_ varchar(64);

create index ACT_IDX_HI_DEC_OUT_ROOT_PI on ACT_HI_DEC_OUT(ROOT_PROC_INST_ID_);

-- add root process instance id to a historic bytearray
ALTER TABLE ACT_GE_BYTEARRAY
  ADD ROOT_PROC_INST_ID_ varchar(64);

create index ACT_IDX_BYTEARRAY_ROOT_PI on ACT_GE_BYTEARRAY(ROOT_PROC_INST_ID_);

-- removal time of historic entities is provided on start of root process instance
ALTER TABLE ACT_HI_ACTINST
  ADD REMOVAL_TIME_ timestamp;

create index ACT_IDX_HI_ACT_INST_RM_TIME on ACT_HI_ACTINST(REMOVAL_TIME_);

ALTER TABLE ACT_HI_TASKINST
  ADD REMOVAL_TIME_ timestamp;

create index ACT_IDX_HI_TASK_INST_RM_TIME on ACT_HI_TASKINST(REMOVAL_TIME_);

ALTER TABLE ACT_HI_VARINST
  ADD REMOVAL_TIME_ timestamp;

create index ACT_IDX_HI_VARINST_RM_TIME on ACT_HI_VARINST(REMOVAL_TIME_);

ALTER TABLE ACT_HI_DETAIL
  ADD REMOVAL_TIME_ timestamp;

create index ACT_IDX_HI_DETAIL_RM_TIME on ACT_HI_DETAIL(REMOVAL_TIME_);

ALTER TABLE ACT_HI_COMMENT
  ADD REMOVAL_TIME_ timestamp;

create index ACT_IDX_HI_COMMENT_RM_TIME on ACT_HI_COMMENT(REMOVAL_TIME_);

ALTER TABLE ACT_HI_ATTACHMENT
  ADD REMOVAL_TIME_ timestamp;

create index ACT_IDX_HI_ATTACHMENT_RM_TIME on ACT_HI_ATTACHMENT(REMOVAL_TIME_);

ALTER TABLE ACT_HI_INCIDENT
  ADD REMOVAL_TIME_ timestamp;

create index ACT_IDX_HI_INCIDENT_RM_TIME on ACT_HI_INCIDENT(REMOVAL_TIME_);

ALTER TABLE ACT_HI_EXT_TASK_LOG
  ADD REMOVAL_TIME_ timestamp;

create index ACT_HI_EXT_TASK_LOG_RM_TIME on ACT_HI_EXT_TASK_LOG(REMOVAL_TIME_);

ALTER TABLE ACT_HI_IDENTITYLINK
  ADD REMOVAL_TIME_ timestamp;

create index ACT_IDX_HI_IDENT_LINK_RM_TIME on ACT_HI_IDENTITYLINK(REMOVAL_TIME_);

ALTER TABLE ACT_HI_JOB_LOG
  ADD REMOVAL_TIME_ timestamp;

create index ACT_IDX_HI_JOB_LOG_RM_TIME on ACT_HI_JOB_LOG(REMOVAL_TIME_);

ALTER TABLE ACT_HI_OP_LOG
  ADD REMOVAL_TIME_ timestamp;

create index ACT_IDX_HI_OP_LOG_RM_TIME on ACT_HI_OP_LOG(REMOVAL_TIME_);

ALTER TABLE ACT_HI_DEC_IN
  ADD REMOVAL_TIME_ timestamp;

create index ACT_IDX_HI_DEC_IN_RM_TIME on ACT_HI_DEC_IN(REMOVAL_TIME_);

ALTER TABLE ACT_HI_DEC_OUT
  ADD REMOVAL_TIME_ timestamp;

create index ACT_IDX_HI_DEC_OUT_RM_TIME on ACT_HI_DEC_OUT(REMOVAL_TIME_);

ALTER TABLE ACT_GE_BYTEARRAY
  ADD REMOVAL_TIME_ timestamp;

create index ACT_IDX_BYTEARRAY_RM_TIME on ACT_GE_BYTEARRAY(REMOVAL_TIME_);

-- removal time of historic batches is provided
ALTER TABLE ACT_HI_BATCH
  ADD REMOVAL_TIME_ timestamp;

create index ACT_HI_BAT_RM_TIME on ACT_HI_BATCH(REMOVAL_TIME_);

-- add job creation timestamp
ALTER TABLE ACT_RU_JOB
  ADD CREATE_TIME_ timestamp;
