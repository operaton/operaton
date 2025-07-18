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

-- add new column to historic activity instance table --
alter table ACT_HI_ACTINST
    add ACT_INST_STATE_ integer;

-- add follow-up date to tasks --
alter table ACT_RU_TASK
    add FOLLOW_UP_DATE_ timestamp;
alter table ACT_HI_TASKINST
    add FOLLOW_UP_DATE_ timestamp;

-- add JOBDEF table --
create table ACT_RU_JOBDEF (
    ID_ varchar(64) not null,
    REV_ integer,
    PROC_DEF_ID_ varchar(64) not null,
    PROC_DEF_KEY_ varchar(255) not null,
    ACT_ID_ varchar(255) not null,
    JOB_TYPE_ varchar(255) not null,
    JOB_CONFIGURATION_ varchar(255),
    SUSPENSION_STATE_ integer,
    primary key (ID_)
);

-- add new columns to job table --
alter table ACT_RU_JOB
    add PROCESS_DEF_ID_ varchar(64);

alter table ACT_RU_JOB
    add PROCESS_DEF_KEY_ varchar(64);

alter table ACT_RU_JOB
    add SUSPENSION_STATE_ integer;

alter table ACT_RU_JOB
    add JOB_DEF_ID_ varchar(64);

-- update job table with values from execution table --

UPDATE
    ACT_RU_JOB J
SET
    PROCESS_DEF_ID_  = (
        SELECT
            PI.PROC_DEF_ID_
        FROM
            ACT_RU_EXECUTION PI
        WHERE
            PI.ID_ = J.PROCESS_INSTANCE_ID_
    ),
    SUSPENSION_STATE_  = (
        SELECT
            PI.SUSPENSION_STATE_
        FROM
            ACT_RU_EXECUTION PI
        WHERE
            PI.ID_ = J.PROCESS_INSTANCE_ID_
    );

UPDATE
    ACT_RU_JOB J
SET
    PROCESS_DEF_KEY_  = (
        SELECT
            PD.KEY_
        FROM
            ACT_RE_PROCDEF PD
        WHERE
            PD.ID_ = J.PROCESS_DEF_ID_
    );

-- add Hist OP Log table --

create table ACT_HI_OP_LOG (
    ID_ varchar(64) not null,
    PROC_DEF_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    USER_ID_ varchar(255),
    TIMESTAMP_ timestamp not null,
    OPERATION_TYPE_ varchar(64),
    OPERATION_ID_ varchar(64),
    ENTITY_TYPE_ varchar(30),
    PROPERTY_ varchar(64),
    ORG_VALUE_ varchar(4000),
    NEW_VALUE_ varchar(4000),
    primary key (ID_)
);


-- add new column to ACT_HI_VARINST --

alter table ACT_HI_VARINST
    add ACT_INST_ID_ varchar(64);

alter table ACT_HI_DETAIL
    add VAR_INST_ID_ varchar(64);

alter table ACT_HI_TASKINST
    add ACT_INST_ID_ varchar(64);

-- set cached entity state to 63 on all executions --

UPDATE
    ACT_RU_EXECUTION
SET
    CACHED_ENT_STATE_ = 63;

-- add new table ACT_HI_INCIDENT --

create table ACT_HI_INCIDENT (
  ID_ varchar(64) not null,
  PROC_DEF_ID_ varchar(64),
  PROC_INST_ID_ varchar(64),
  EXECUTION_ID_ varchar(64),
  CREATE_TIME_ timestamp not null,
  END_TIME_ timestamp,
  INCIDENT_MSG_ varchar(4000),
  INCIDENT_TYPE_ varchar(255) not null,
  ACTIVITY_ID_ varchar(255),
  CAUSE_INCIDENT_ID_ varchar(64),
  ROOT_CAUSE_INCIDENT_ID_ varchar(64),
  CONFIGURATION_ varchar(255),
  INCIDENT_STATE_ integer,
  primary key (ID_)
);

-- update ACT_RU_VARIABLE table --

-- add new column --

ALTER TABLE ACT_RU_VARIABLE
    add VAR_SCOPE_ varchar(64);

-- migrate execution variables --

UPDATE
  ACT_RU_VARIABLE V

SET
  VAR_SCOPE_ = V.EXECUTION_ID_

WHERE
  V.EXECUTION_ID_ is not null AND
  V.TASK_ID_ is null;

-- migrate task variables --

UPDATE
  ACT_RU_VARIABLE V

SET
  VAR_SCOPE_ = V.TASK_ID_

WHERE
  V.TASK_ID_ is not null;

-- set VAR_SCOPE_ not null --

ALTER TABLE ACT_RU_VARIABLE
    ALTER COLUMN VAR_SCOPE_ SET NOT NULL;

-- reorg ACT_RU_VARIABLE --

Call Sysproc.admin_cmd ('REORG TABLE ACT_RU_VARIABLE');

-- add unique index --

create unique index ACT_UNIQ_VARIABLE on ACT_RU_VARIABLE(VAR_SCOPE_,NAME_);

-- indexes for concurrency problems - https://app.camunda.com/jira/browse/CAM-1646 --
create index ACT_IDX_EXECUTION_PROC on ACT_RU_EXECUTION(PROC_DEF_ID_);
create index ACT_IDX_EXECUTION_PARENT on ACT_RU_EXECUTION(PARENT_ID_);
create index ACT_IDX_EXECUTION_SUPER on ACT_RU_EXECUTION(SUPER_EXEC_);
create index ACT_IDX_EVENT_SUBSCR_EXEC on ACT_RU_EVENT_SUBSCR(EXECUTION_ID_);
create index ACT_IDX_BA_DEPLOYMENT on ACT_GE_BYTEARRAY(DEPLOYMENT_ID_);
create index ACT_IDX_IDENT_LNK_TASK on ACT_RU_IDENTITYLINK(TASK_ID_);
create index ACT_IDX_INCIDENT_EXEC on ACT_RU_INCIDENT(EXECUTION_ID_);
create index ACT_IDX_INCIDENT_PROCINST on ACT_RU_INCIDENT(PROC_INST_ID_);
create index ACT_IDX_INCIDENT_PROC_DEF_ID on ACT_RU_INCIDENT(PROC_DEF_ID_);
create index ACT_IDX_INCIDENT_CAUSE on ACT_RU_INCIDENT(CAUSE_INCIDENT_ID_);
create index ACT_IDX_INCIDENT_ROOT_CAUSE on ACT_RU_INCIDENT(ROOT_CAUSE_INCIDENT_ID_);
create index ACT_IDX_JOB_EXCEPTION_STACK on ACT_RU_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_VARIABLE_BA on ACT_RU_VARIABLE(BYTEARRAY_ID_);
create index ACT_IDX_VARIABLE_EXEC on ACT_RU_VARIABLE(EXECUTION_ID_);
create index ACT_IDX_VARIABLE_PROCINST on ACT_RU_VARIABLE(PROC_INST_ID_);
create index ACT_IDX_TASK_EXEC on ACT_RU_TASK(EXECUTION_ID_);
create index ACT_IDX_TASK_PROCINST on ACT_RU_TASK(PROC_INST_ID_);
create index ACT_IDX_TASK_PROC_DEF_ID on ACT_RU_TASK(PROC_DEF_ID_);
