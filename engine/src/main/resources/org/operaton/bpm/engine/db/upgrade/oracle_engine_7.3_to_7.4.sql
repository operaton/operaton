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

-- metrics --

ALTER TABLE ACT_RU_METER_LOG
  ADD REPORTER_ NVARCHAR2(255);

-- job prioritization --

ALTER TABLE ACT_RU_JOB
  ADD PRIORITY_ NUMBER(19,0) DEFAULT 0 NOT NULL;

ALTER TABLE ACT_RU_JOBDEF
  ADD JOB_PRIORITY_ NUMBER(19,0);

ALTER TABLE ACT_HI_JOB_LOG
  ADD JOB_PRIORITY_ NUMBER(19,0) DEFAULT 0 NOT NULL;

-- create decision definition table --
create table ACT_RE_DECISION_DEF (
    ID_ NVARCHAR2(64) NOT NULL,
    REV_ INTEGER,
    CATEGORY_ NVARCHAR2(255),
    NAME_ NVARCHAR2(255),
    KEY_ NVARCHAR2(255) NOT NULL,
    VERSION_ INTEGER NOT NULL,
    DEPLOYMENT_ID_ NVARCHAR2(64),
    RESOURCE_NAME_ NVARCHAR2(2000),
    DGRM_RESOURCE_NAME_ NVARCHAR2(2000),
    primary key (ID_)
);

-- create unique constraint on ACT_RE_DECISION_DEF --
alter table ACT_RE_DECISION_DEF
    add constraint ACT_UNIQ_DECISION_DEF
    unique (KEY_,VERSION_);

-- case sentry part source --

ALTER TABLE ACT_RU_CASE_SENTRY_PART
  ADD SOURCE_ NVARCHAR2(255);

-- create history decision instance table --
create table ACT_HI_DECINST (
    ID_ NVARCHAR2(64) NOT NULL,
    DEC_DEF_ID_ NVARCHAR2(64) NOT NULL,
    DEC_DEF_KEY_ NVARCHAR2(255) NOT NULL,
    DEC_DEF_NAME_ NVARCHAR2(255),
    PROC_DEF_KEY_ NVARCHAR2(255),
    PROC_DEF_ID_ NVARCHAR2(64),
    PROC_INST_ID_ NVARCHAR2(64),
    CASE_DEF_KEY_ NVARCHAR2(255),
    CASE_DEF_ID_ NVARCHAR2(64),
    CASE_INST_ID_ NVARCHAR2(64),
    ACT_INST_ID_ NVARCHAR2(64),
    ACT_ID_ NVARCHAR2(255),
    EVAL_TIME_ TIMESTAMP(6) not null,
    COLLECT_VALUE_ NUMBER(*,10),
    primary key (ID_)
);

-- create history decision input table --
create table ACT_HI_DEC_IN (
    ID_ NVARCHAR2(64) NOT NULL,
    DEC_INST_ID_ NVARCHAR2(64) NOT NULL,
    CLAUSE_ID_ NVARCHAR2(64) NOT NULL,
    CLAUSE_NAME_ NVARCHAR2(255),
    VAR_TYPE_ NVARCHAR2(100),
    BYTEARRAY_ID_ NVARCHAR2(64),
    DOUBLE_ NUMBER(*,10),
    LONG_ NUMBER(19,0),
    TEXT_ NVARCHAR2(2000),
    TEXT2_ NVARCHAR2(2000),
    primary key (ID_)
);

-- create history decision output table --
create table ACT_HI_DEC_OUT (
    ID_ NVARCHAR2(64) NOT NULL,
    DEC_INST_ID_ NVARCHAR2(64) NOT NULL,
    CLAUSE_ID_ NVARCHAR2(64) NOT NULL,
    CLAUSE_NAME_ NVARCHAR2(255),
    RULE_ID_ NVARCHAR2(64) NOT NULL,
    RULE_ORDER_ INTEGER,
    VAR_NAME_ NVARCHAR2(255),
    VAR_TYPE_ NVARCHAR2(100),
    BYTEARRAY_ID_ NVARCHAR2(64),
    DOUBLE_ NUMBER(*,10),
    LONG_ NUMBER(19,0),
    TEXT_ NVARCHAR2(2000),
    TEXT2_ NVARCHAR2(2000),
    primary key (ID_)
);

-- create indexes for historic decision tables
create index ACT_IDX_HI_DEC_INST_ID on ACT_HI_DECINST(DEC_DEF_ID_);
create index ACT_IDX_HI_DEC_INST_KEY on ACT_HI_DECINST(DEC_DEF_KEY_);
create index ACT_IDX_HI_DEC_INST_PI on ACT_HI_DECINST(PROC_INST_ID_);
create index ACT_IDX_HI_DEC_INST_CI on ACT_HI_DECINST(CASE_INST_ID_);
create index ACT_IDX_HI_DEC_INST_ACT on ACT_HI_DECINST(ACT_ID_);
create index ACT_IDX_HI_DEC_INST_ACT_INST on ACT_HI_DECINST(ACT_INST_ID_);
create index ACT_IDX_HI_DEC_INST_TIME on ACT_HI_DECINST(EVAL_TIME_);

create index ACT_IDX_HI_DEC_IN_INST on ACT_HI_DEC_IN(DEC_INST_ID_);
create index ACT_IDX_HI_DEC_IN_CLAUSE on ACT_HI_DEC_IN(DEC_INST_ID_, CLAUSE_ID_);

create index ACT_IDX_HI_DEC_OUT_INST on ACT_HI_DEC_OUT(DEC_INST_ID_);
create index ACT_IDX_HI_DEC_OUT_RULE on ACT_HI_DEC_OUT(RULE_ORDER_, CLAUSE_ID_);

-- add grant authorization for group operaton-admin:
INSERT INTO ACT_RU_AUTHORIZATION (ID_, TYPE_, GROUP_ID_, RESOURCE_TYPE_, RESOURCE_ID_, PERMS_, REV_)
  VALUES ('operaton-admin-grant-decision-definition', 1, 'operaton-admin', 10, '*', 2147483647, 1);

-- external tasks --

create table ACT_RU_EXT_TASK (
  ID_ NVARCHAR2(64) not null,
  REV_ integer not null,
  WORKER_ID_ NVARCHAR2(255),
  TOPIC_NAME_ NVARCHAR2(255),
  RETRIES_ INTEGER,
  ERROR_MSG_ NVARCHAR2(2000),
  LOCK_EXP_TIME_ TIMESTAMP(6),
  SUSPENSION_STATE_ integer,
  EXECUTION_ID_ NVARCHAR2(64),
  PROC_INST_ID_ NVARCHAR2(64),
  PROC_DEF_ID_ NVARCHAR2(64),
  PROC_DEF_KEY_ NVARCHAR2(255),
  ACT_ID_ NVARCHAR2(255),
  ACT_INST_ID_ NVARCHAR2(64),
  primary key (ID_)
);

alter table ACT_RU_EXT_TASK
  add constraint ACT_FK_EXT_TASK_EXE
  foreign key (EXECUTION_ID_)
  references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_EXT_TASK_TOPIC on ACT_RU_EXT_TASK(TOPIC_NAME_);

-- deployment --

ALTER TABLE ACT_RE_DEPLOYMENT
  ADD SOURCE_ NVARCHAR2(255);

ALTER TABLE ACT_HI_OP_LOG
  ADD DEPLOYMENT_ID_ NVARCHAR2(64);

-- job suspension state

ALTER TABLE ACT_RU_JOB
  MODIFY ( SUSPENSION_STATE_ DEFAULT 1 );

  -- relevant for jobs created in Operaton 7.0
UPDATE ACT_RU_JOB
  SET SUSPENSION_STATE_ = 1
  WHERE SUSPENSION_STATE_ IS NULL;

ALTER TABLE ACT_RU_JOB
  MODIFY ( SUSPENSION_STATE_ NOT NULL );
