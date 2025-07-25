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

-- set datetime precision --

ALTER TABLE ACT_HI_CASEINST
  MODIFY COLUMN CREATE_TIME_ datetime(3) not null;

ALTER TABLE ACT_HI_CASEINST
  MODIFY COLUMN CLOSE_TIME_ datetime(3);

ALTER TABLE ACT_HI_CASEACTINST
  MODIFY COLUMN CREATE_TIME_ datetime(3) not null;

ALTER TABLE ACT_HI_CASEACTINST
  MODIFY COLUMN END_TIME_ datetime(3);

ALTER TABLE ACT_HI_DECINST
  MODIFY COLUMN EVAL_TIME_ datetime(3) not null;

ALTER TABLE ACT_RU_TASK
  MODIFY COLUMN DUE_DATE_ datetime(3);

ALTER TABLE ACT_RU_TASK
  MODIFY COLUMN FOLLOW_UP_DATE_ datetime(3);

ALTER TABLE ACT_HI_PROCINST
  MODIFY COLUMN START_TIME_ datetime(3) not null;

ALTER TABLE ACT_HI_PROCINST
  MODIFY COLUMN END_TIME_ datetime(3);

ALTER TABLE ACT_HI_ACTINST
  MODIFY COLUMN START_TIME_ datetime(3) not null;

ALTER TABLE ACT_HI_ACTINST
  MODIFY COLUMN END_TIME_ datetime(3);

ALTER TABLE ACT_HI_TASKINST
  MODIFY COLUMN START_TIME_ datetime(3) not null;

ALTER TABLE ACT_HI_TASKINST
  MODIFY COLUMN END_TIME_ datetime(3);

ALTER TABLE ACT_HI_TASKINST
  MODIFY COLUMN DUE_DATE_ datetime(3);

ALTER TABLE ACT_HI_TASKINST
  MODIFY COLUMN FOLLOW_UP_DATE_ datetime(3);

ALTER TABLE ACT_HI_DETAIL
  MODIFY COLUMN TIME_ datetime(3) not null;

ALTER TABLE ACT_HI_COMMENT
  MODIFY COLUMN TIME_ datetime(3) not null;

-- set timestamp precision --

ALTER TABLE ACT_RE_DEPLOYMENT
  MODIFY COLUMN DEPLOY_TIME_ timestamp(3);

ALTER TABLE ACT_RU_JOB
  MODIFY COLUMN LOCK_EXP_TIME_ timestamp(3) NULL;

ALTER TABLE ACT_RU_JOB
  MODIFY COLUMN DUEDATE_ timestamp(3) NULL;

ALTER TABLE ACT_RU_TASK
  MODIFY COLUMN CREATE_TIME_ timestamp(3);

ALTER TABLE ACT_RU_EVENT_SUBSCR
  MODIFY COLUMN CREATED_ timestamp(3) NOT NULL;

ALTER TABLE ACT_RU_INCIDENT
  MODIFY COLUMN INCIDENT_TIMESTAMP_ timestamp(3) NOT NULL;

ALTER TABLE ACT_RU_METER_LOG
  MODIFY COLUMN TIMESTAMP_ timestamp(3) NOT NULL;

ALTER TABLE ACT_RU_EXT_TASK
  MODIFY COLUMN LOCK_EXP_TIME_ timestamp(3) NULL;

ALTER TABLE ACT_HI_OP_LOG
  MODIFY COLUMN TIMESTAMP_ timestamp(3) NOT NULL;

ALTER TABLE ACT_HI_INCIDENT
  MODIFY COLUMN CREATE_TIME_ timestamp(3) NOT NULL;

ALTER TABLE ACT_HI_INCIDENT
  MODIFY COLUMN END_TIME_ timestamp(3) NULL;

ALTER TABLE ACT_HI_JOB_LOG
  MODIFY COLUMN TIMESTAMP_ timestamp(3) NOT NULL;

ALTER TABLE ACT_HI_JOB_LOG
  MODIFY COLUMN JOB_DUEDATE_ timestamp(3) NULL;

-- semantic version --

ALTER TABLE ACT_RE_PROCDEF
  ADD VERSION_TAG_ varchar(64);

create index ACT_IDX_PROCDEF_VER_TAG on ACT_RE_PROCDEF(VERSION_TAG_);

-- tenant id --

ALTER TABLE ACT_RE_DEPLOYMENT
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_DEPLOYMENT_TENANT_ID on ACT_RE_DEPLOYMENT(TENANT_ID_);

ALTER TABLE ACT_RE_PROCDEF
  ADD TENANT_ID_ varchar(64);

ALTER TABLE ACT_RE_PROCDEF
   DROP INDEX ACT_UNIQ_PROCDEF;

create index ACT_IDX_PROCDEF_TENANT_ID ON ACT_RE_PROCDEF(TENANT_ID_);

ALTER TABLE ACT_RU_EXECUTION
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_EXEC_TENANT_ID on ACT_RU_EXECUTION(TENANT_ID_);

ALTER TABLE ACT_RU_TASK
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_TASK_TENANT_ID on ACT_RU_TASK(TENANT_ID_);

ALTER TABLE ACT_RU_VARIABLE
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_VARIABLE_TENANT_ID on ACT_RU_VARIABLE(TENANT_ID_);

ALTER TABLE ACT_RU_EVENT_SUBSCR
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_EVENT_SUBSCR_TENANT_ID on ACT_RU_EVENT_SUBSCR(TENANT_ID_);

ALTER TABLE ACT_RU_JOB
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_JOB_TENANT_ID on ACT_RU_JOB(TENANT_ID_);

ALTER TABLE ACT_RU_JOBDEF
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_JOBDEF_TENANT_ID on ACT_RU_JOBDEF(TENANT_ID_);

ALTER TABLE ACT_RU_INCIDENT
  ADD TENANT_ID_ varchar(64);

ALTER TABLE ACT_RU_IDENTITYLINK
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_INC_TENANT_ID on ACT_RU_INCIDENT(TENANT_ID_);

ALTER TABLE ACT_RU_EXT_TASK
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_EXT_TASK_TENANT_ID on ACT_RU_EXT_TASK(TENANT_ID_);

ALTER TABLE ACT_RE_DECISION_DEF
       DROP INDEX ACT_UNIQ_DECISION_DEF;

ALTER TABLE ACT_RE_DECISION_DEF
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_DEC_DEF_TENANT_ID on ACT_RE_DECISION_DEF(TENANT_ID_);

ALTER TABLE ACT_RE_CASE_DEF
       DROP INDEX ACT_UNIQ_CASE_DEF;

ALTER TABLE ACT_RE_CASE_DEF
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_CASE_DEF_TENANT_ID on ACT_RE_CASE_DEF(TENANT_ID_);

ALTER TABLE ACT_GE_BYTEARRAY
  ADD TENANT_ID_ varchar(64);

ALTER TABLE ACT_RU_CASE_EXECUTION
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_CASE_EXEC_TENANT_ID on ACT_RU_CASE_EXECUTION(TENANT_ID_);

ALTER TABLE ACT_RU_CASE_SENTRY_PART
  ADD TENANT_ID_ varchar(64);

-- user on historic decision instance --

ALTER TABLE ACT_HI_DECINST
  ADD USER_ID_ varchar(255);

-- tenant id on history --

ALTER TABLE ACT_HI_PROCINST
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_HI_PRO_INST_TENANT_ID on ACT_HI_PROCINST(TENANT_ID_);

ALTER TABLE ACT_HI_ACTINST
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_HI_ACT_INST_TENANT_ID on ACT_HI_ACTINST(TENANT_ID_);

ALTER TABLE ACT_HI_TASKINST
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_HI_TASK_INST_TENANT_ID on ACT_HI_TASKINST(TENANT_ID_);

ALTER TABLE ACT_HI_VARINST
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_HI_VAR_INST_TENANT_ID on ACT_HI_VARINST(TENANT_ID_);

ALTER TABLE ACT_HI_DETAIL
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_HI_DETAIL_TENANT_ID on ACT_HI_DETAIL(TENANT_ID_);

ALTER TABLE ACT_HI_INCIDENT
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_HI_INCIDENT_TENANT_ID on ACT_HI_INCIDENT(TENANT_ID_);

ALTER TABLE ACT_HI_JOB_LOG
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_HI_JOB_LOG_TENANT_ID on ACT_HI_JOB_LOG(TENANT_ID_);

ALTER TABLE ACT_HI_COMMENT
  ADD TENANT_ID_ varchar(64);

ALTER TABLE ACT_HI_ATTACHMENT
  ADD TENANT_ID_ varchar(64);

ALTER TABLE ACT_HI_OP_LOG
  ADD TENANT_ID_ varchar(64);

ALTER TABLE ACT_HI_DEC_IN
  ADD TENANT_ID_ varchar(64);

ALTER TABLE ACT_HI_DEC_OUT
  ADD TENANT_ID_ varchar(64);

ALTER TABLE ACT_HI_DECINST
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_HI_DEC_INST_TENANT_ID on ACT_HI_DECINST(TENANT_ID_);

ALTER TABLE ACT_HI_CASEINST
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_HI_CAS_I_TENANT_ID on ACT_HI_CASEINST(TENANT_ID_);

ALTER TABLE ACT_HI_CASEACTINST
  ADD TENANT_ID_ varchar(64);

create index ACT_IDX_HI_CAS_A_I_TENANT_ID on ACT_HI_CASEACTINST(TENANT_ID_);

-- AUTHORIZATION --

-- add grant authorizations for group operaton-admin:
INSERT INTO
  ACT_RU_AUTHORIZATION (ID_, TYPE_, GROUP_ID_, RESOURCE_TYPE_, RESOURCE_ID_, PERMS_, REV_)
VALUES
  ('operaton-admin-grant-tenant', 1, 'operaton-admin', 11, '*', 2147483647, 1),
  ('operaton-admin-grant-tenant-membership', 1, 'operaton-admin', 12, '*', 2147483647, 1),
  ('operaton-admin-grant-batch', 1, 'operaton-admin', 13, '*', 2147483647, 1);

-- tenant table

create table ACT_ID_TENANT (
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_ID_TENANT_MEMBER (
    ID_ varchar(64) not null,
    TENANT_ID_ varchar(64) not null,
    USER_ID_ varchar(64),
    GROUP_ID_ varchar(64),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

alter table ACT_ID_TENANT_MEMBER
    add constraint ACT_UNIQ_TENANT_MEMB_USER
    unique (TENANT_ID_, USER_ID_);

alter table ACT_ID_TENANT_MEMBER
    add constraint ACT_UNIQ_TENANT_MEMB_GROUP
    unique (TENANT_ID_, GROUP_ID_);

alter table ACT_ID_TENANT_MEMBER
    add constraint ACT_FK_TENANT_MEMB
    foreign key (TENANT_ID_)
    references ACT_ID_TENANT (ID_);

alter table ACT_ID_TENANT_MEMBER
    add constraint ACT_FK_TENANT_MEMB_USER
    foreign key (USER_ID_)
    references ACT_ID_USER (ID_);

alter table ACT_ID_TENANT_MEMBER
    add constraint ACT_FK_TENANT_MEMB_GROUP
    foreign key (GROUP_ID_)
    references ACT_ID_GROUP (ID_);

--  BATCH --

-- remove not null from job definition table --
alter table ACT_RU_JOBDEF
	modify PROC_DEF_ID_ varchar(64),
	modify PROC_DEF_KEY_ varchar(255),
	modify ACT_ID_ varchar(255);

create table ACT_RU_BATCH (
  ID_ varchar(64) not null,
  REV_ integer not null,
  TYPE_ varchar(255),
  TOTAL_JOBS_ integer,
  JOBS_CREATED_ integer,
  JOBS_PER_SEED_ integer,
  INVOCATIONS_PER_JOB_ integer,
  SEED_JOB_DEF_ID_ varchar(64),
  BATCH_JOB_DEF_ID_ varchar(64),
  MONITOR_JOB_DEF_ID_ varchar(64),
  SUSPENSION_STATE_ integer,
  CONFIGURATION_ varchar(255),
  TENANT_ID_ varchar(64),
  primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_BATCH (
    ID_ varchar(64) not null,
    TYPE_ varchar(255),
    TOTAL_JOBS_ integer,
    JOBS_PER_SEED_ integer,
    INVOCATIONS_PER_JOB_ integer,
    SEED_JOB_DEF_ID_ varchar(64),
    MONITOR_JOB_DEF_ID_ varchar(64),
    BATCH_JOB_DEF_ID_ varchar(64),
    TENANT_ID_  varchar(64),
    START_TIME_ datetime(3) not null,
    END_TIME_ datetime(3),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create table ACT_HI_IDENTITYLINK (
    ID_ varchar(64) not null,
    TIMESTAMP_ timestamp(3) not null,
    TYPE_ varchar(255),
    USER_ID_ varchar(255),
    GROUP_ID_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    OPERATION_TYPE_ varchar(64),
    ASSIGNER_ID_ varchar(64),
    PROC_DEF_KEY_ varchar(255),
    TENANT_ID_ varchar(64),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_HI_IDENT_LNK_USER on ACT_HI_IDENTITYLINK(USER_ID_);
create index ACT_IDX_HI_IDENT_LNK_GROUP on ACT_HI_IDENTITYLINK(GROUP_ID_);
create index ACT_IDX_HI_IDENT_LNK_TENANT_ID on ACT_HI_IDENTITYLINK(TENANT_ID_);

create index ACT_IDX_JOB_JOB_DEF_ID on ACT_RU_JOB(JOB_DEF_ID_);
create index ACT_IDX_HI_JOB_LOG_JOB_DEF_ID on ACT_HI_JOB_LOG(JOB_DEF_ID_);

create index ACT_IDX_BATCH_SEED_JOB_DEF ON ACT_RU_BATCH(SEED_JOB_DEF_ID_);
alter table ACT_RU_BATCH
    add constraint ACT_FK_BATCH_SEED_JOB_DEF
    foreign key (SEED_JOB_DEF_ID_)
    references ACT_RU_JOBDEF (ID_);

create index ACT_IDX_BATCH_MONITOR_JOB_DEF ON ACT_RU_BATCH(MONITOR_JOB_DEF_ID_);
alter table ACT_RU_BATCH
    add constraint ACT_FK_BATCH_MONITOR_JOB_DEF
    foreign key (MONITOR_JOB_DEF_ID_)
    references ACT_RU_JOBDEF (ID_);

create index ACT_IDX_BATCH_JOB_DEF ON ACT_RU_BATCH(BATCH_JOB_DEF_ID_);
alter table ACT_RU_BATCH
    add constraint ACT_FK_BATCH_JOB_DEF
    foreign key (BATCH_JOB_DEF_ID_)
    references ACT_RU_JOBDEF (ID_);

-- TASK PRIORITY --

ALTER TABLE ACT_RU_EXT_TASK
  ADD PRIORITY_ bigint NOT NULL DEFAULT 0;

create index ACT_IDX_EXT_TASK_PRIORITY ON ACT_RU_EXT_TASK(PRIORITY_);

-- HI OP PROC INDECIES --

create index ACT_IDX_HI_OP_LOG_PROCINST on ACT_HI_OP_LOG(PROC_INST_ID_);
create index ACT_IDX_HI_OP_LOG_PROCDEF on ACT_HI_OP_LOG(PROC_DEF_ID_);

-- JOB_DEF_ID_ on INCIDENTS --
ALTER TABLE ACT_RU_INCIDENT
  ADD JOB_DEF_ID_ varchar(64);

create index ACT_IDX_INC_JOB_DEF on ACT_RU_INCIDENT(JOB_DEF_ID_);
alter table ACT_RU_INCIDENT
    add constraint ACT_FK_INC_JOB_DEF
    foreign key (JOB_DEF_ID_)
    references ACT_RU_JOBDEF (ID_);

ALTER TABLE ACT_HI_INCIDENT
  ADD JOB_DEF_ID_ varchar(64);

-- BATCH_ID_ on ACT_HI_OP_LOG --
ALTER TABLE ACT_HI_OP_LOG
  ADD BATCH_ID_ varchar(64);
