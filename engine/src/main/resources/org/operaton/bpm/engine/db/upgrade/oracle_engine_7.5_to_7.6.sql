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

-- AUTHORIZATION --

-- add grant authorizations for group operaton-admin:
INSERT INTO
  ACT_RU_AUTHORIZATION (ID_, TYPE_, GROUP_ID_, RESOURCE_TYPE_, RESOURCE_ID_, PERMS_, REV_)
VALUES
  ('operaton-admin-grant-drd', 1, 'operaton-admin', 14, '*', 2147483647, 1);

-- decision requirements definition --

ALTER TABLE ACT_RE_DECISION_DEF
  ADD DEC_REQ_ID_ NVARCHAR2(64);

ALTER TABLE ACT_RE_DECISION_DEF
  ADD DEC_REQ_KEY_ NVARCHAR2(255);

ALTER TABLE ACT_RU_CASE_SENTRY_PART
  ADD VARIABLE_EVENT_ NVARCHAR2(255);

ALTER TABLE ACT_RU_CASE_SENTRY_PART
  ADD VARIABLE_NAME_ NVARCHAR2(255);

create table ACT_RE_DECISION_REQ_DEF (
    ID_ NVARCHAR2(64) NOT NULL,
    REV_ INTEGER,
    CATEGORY_ NVARCHAR2(255),
    NAME_ NVARCHAR2(255),
    KEY_ NVARCHAR2(255) NOT NULL,
    VERSION_ INTEGER NOT NULL,
    DEPLOYMENT_ID_ NVARCHAR2(64),
    RESOURCE_NAME_ NVARCHAR2(2000),
    DGRM_RESOURCE_NAME_ NVARCHAR2(2000),
    TENANT_ID_ NVARCHAR2(64),
    primary key (ID_)
);

alter table ACT_RE_DECISION_DEF
    add constraint ACT_FK_DEC_REQ
    foreign key (DEC_REQ_ID_)
    references ACT_RE_DECISION_REQ_DEF(ID_);

create index ACT_IDX_DEC_DEF_REQ_ID on ACT_RE_DECISION_DEF(DEC_REQ_ID_);
create index ACT_IDX_DEC_REQ_DEF_TENANT_ID on ACT_RE_DECISION_REQ_DEF(TENANT_ID_);

ALTER TABLE ACT_HI_DECINST
  ADD ROOT_DEC_INST_ID_ NVARCHAR2(64);

ALTER TABLE ACT_HI_DECINST
  ADD DEC_REQ_ID_ NVARCHAR2(64);

ALTER TABLE ACT_HI_DECINST
  ADD DEC_REQ_KEY_ NVARCHAR2(255);

create index ACT_IDX_HI_DEC_INST_ROOT_ID on ACT_HI_DECINST(ROOT_DEC_INST_ID_);
create index ACT_IDX_HI_DEC_INST_REQ_ID on ACT_HI_DECINST(DEC_REQ_ID_);
create index ACT_IDX_HI_DEC_INST_REQ_KEY on ACT_HI_DECINST(DEC_REQ_KEY_);

-- remove not null from ACT_HI_DEC tables --
alter table ACT_HI_DEC_OUT
  modify (
      CLAUSE_ID_ null,
      RULE_ID_ null
  );

alter table ACT_HI_DEC_IN
  modify ( CLAUSE_ID_ null );

-- CAM-5914
create index ACT_IDX_JOB_EXECUTION_ID on ACT_RU_JOB(EXECUTION_ID_);
create index ACT_IDX_JOB_HANDLER on ACT_RU_JOB(HANDLER_TYPE_,HANDLER_CFG_);

ALTER TABLE ACT_RU_EXT_TASK
  ADD ERROR_DETAILS_ID_ NVARCHAR2(64);

alter table ACT_RU_EXT_TASK
  add constraint ACT_FK_EXT_TASK_ERROR_DETAILS
  foreign key (ERROR_DETAILS_ID_)
  references ACT_GE_BYTEARRAY (ID_);

ALTER TABLE ACT_HI_PROCINST
  ADD STATE_ NVARCHAR2(255);

update ACT_HI_PROCINST set STATE_ = 'ACTIVE' where END_TIME_ is null;
update ACT_HI_PROCINST set STATE_ = 'COMPLETED' where END_TIME_ is not null;

-- add indexes on PROC_DEF_KEY_ columns in history tables CAM-6679
create index ACT_IDX_HI_ACT_INST_PROC_KEY on ACT_HI_ACTINST(PROC_DEF_KEY_);
create index ACT_IDX_HI_DETAIL_PROC_KEY on ACT_HI_DETAIL(PROC_DEF_KEY_);
create index ACT_IDX_HI_IDENT_LNK_PROC_KEY on ACT_HI_IDENTITYLINK(PROC_DEF_KEY_);
create index ACT_IDX_HI_INCIDENT_PROC_KEY on ACT_HI_INCIDENT(PROC_DEF_KEY_);
create index ACT_IDX_HI_JOB_LOG_PROC_KEY on ACT_HI_JOB_LOG(PROCESS_DEF_KEY_);
create index ACT_IDX_HI_PRO_INST_PROC_KEY on ACT_HI_PROCINST(PROC_DEF_KEY_);
create index ACT_IDX_HI_TASK_INST_PROC_KEY on ACT_HI_TASKINST(PROC_DEF_KEY_);
create index ACT_IDX_HI_VAR_INST_PROC_KEY on ACT_HI_VARINST(PROC_DEF_KEY_);

-- update the oracle indexes to prevent NULL queries CAM-6680

drop index ACT_IDX_CASE_DEF_TENANT_ID;
drop index ACT_IDX_CASE_EXEC_TENANT_ID;

create index ACT_IDX_CASE_DEF_TENANT_ID on ACT_RE_CASE_DEF(TENANT_ID_, 0);
create index ACT_IDX_CASE_EXEC_TENANT_ID on ACT_RU_CASE_EXECUTION(TENANT_ID_, 0);

drop index ACT_IDX_HI_CAS_I_TENANT_ID;
drop index ACT_IDX_HI_CAS_A_I_TENANT_ID;

create index ACT_IDX_HI_CAS_I_TENANT_ID on ACT_HI_CASEINST(TENANT_ID_, 0);
create index ACT_IDX_HI_CAS_A_I_TENANT_ID on ACT_HI_CASEACTINST(TENANT_ID_, 0);

drop index ACT_IDX_DEC_DEF_TENANT_ID;
drop index ACT_IDX_DEC_REQ_DEF_TENANT_ID;

create index ACT_IDX_DEC_DEF_TENANT_ID on ACT_RE_DECISION_DEF(TENANT_ID_, 0);
create index ACT_IDX_DEC_REQ_DEF_TENANT_ID on ACT_RE_DECISION_REQ_DEF(TENANT_ID_, 0);

drop index ACT_IDX_HI_DEC_INST_TENANT_ID;

create index ACT_IDX_HI_DEC_INST_TENANT_ID on ACT_HI_DECINST(TENANT_ID_, 0);

drop index ACT_IDX_EXT_TASK_TENANT_ID;
drop index ACT_IDX_INC_TENANT_ID;
drop index ACT_IDX_JOBDEF_TENANT_ID;
drop index ACT_IDX_JOB_TENANT_ID;
drop index ACT_IDX_EVENT_SUBSCR_TENANT_ID;
drop index ACT_IDX_VARIABLE_TENANT_ID;
drop index ACT_IDX_TASK_TENANT_ID;
drop index ACT_IDX_EXEC_TENANT_ID;
drop index ACT_IDX_PROCDEF_TENANT_ID;
drop index ACT_IDX_DEPLOYMENT_TENANT_ID;

create index ACT_IDX_EXEC_TENANT_ID on ACT_RU_EXECUTION(TENANT_ID_, 0);
create index ACT_IDX_TASK_TENANT_ID on ACT_RU_TASK(TENANT_ID_, 0);
create index ACT_IDX_EVENT_SUBSCR_TENANT_ID on ACT_RU_EVENT_SUBSCR(TENANT_ID_, 0);
create index ACT_IDX_VARIABLE_TENANT_ID on ACT_RU_VARIABLE(TENANT_ID_, 0);
create index ACT_IDX_INC_TENANT_ID on ACT_RU_INCIDENT(TENANT_ID_, 0);
create index ACT_IDX_JOB_TENANT_ID on ACT_RU_JOB(TENANT_ID_, 0);
create index ACT_IDX_JOBDEF_TENANT_ID on ACT_RU_JOBDEF(TENANT_ID_, 0);
create index ACT_IDX_EXT_TASK_TENANT_ID on ACT_RU_EXT_TASK(TENANT_ID_, 0);
create index ACT_IDX_DEPLOYMENT_TENANT_ID on ACT_RE_DEPLOYMENT(TENANT_ID_, 0);
create index ACT_IDX_PROCDEF_TENANT_ID ON ACT_RE_PROCDEF(TENANT_ID_, 0);

drop index ACT_IDX_HI_PRO_INST_TENANT_ID;
drop index ACT_IDX_HI_ACT_INST_TENANT_ID;
drop index ACT_IDX_HI_TASK_INST_TENANT_ID;
drop index ACT_IDX_HI_DETAIL_TENANT_ID;
drop index ACT_IDX_HI_VAR_INST_TENANT_ID;
drop index ACT_IDX_HI_IDENT_LNK_TENANT_ID;
drop index ACT_IDX_HI_INCIDENT_TENANT_ID;
drop index ACT_IDX_HI_JOB_LOG_TENANT_ID;


create index ACT_IDX_HI_PRO_INST_TENANT_ID on ACT_HI_PROCINST(TENANT_ID_, 0);
create index ACT_IDX_HI_ACT_INST_TENANT_ID on ACT_HI_ACTINST(TENANT_ID_, 0);
create index ACT_IDX_HI_TASK_INST_TENANT_ID on ACT_HI_TASKINST(TENANT_ID_, 0);
create index ACT_IDX_HI_DETAIL_TENANT_ID on ACT_HI_DETAIL(TENANT_ID_, 0);
create index ACT_IDX_HI_IDENT_LNK_TENANT_ID on ACT_HI_IDENTITYLINK(TENANT_ID_, 0);
create index ACT_IDX_HI_VAR_INST_TENANT_ID on ACT_HI_VARINST(TENANT_ID_, 0);
create index ACT_IDX_HI_INCIDENT_TENANT_ID on ACT_HI_INCIDENT(TENANT_ID_, 0);
create index ACT_IDX_HI_JOB_LOG_TENANT_ID on ACT_HI_JOB_LOG(TENANT_ID_, 0);

-- CAM-6725
ALTER TABLE ACT_RU_METER_LOG
 ADD MILLISECONDS_ NUMBER(19,0) DEFAULT 0;

alter table ACT_RU_METER_LOG
  modify ( TIMESTAMP_ null );

CREATE INDEX ACT_IDX_METER_LOG_MS ON ACT_RU_METER_LOG(MILLISECONDS_);
CREATE INDEX ACT_IDX_METER_LOG_REPORT ON ACT_RU_METER_LOG(NAME_, REPORTER_, MILLISECONDS_);
CREATE INDEX ACT_IDX_METER_LOG_NAME_MS ON ACT_RU_METER_LOG(NAME_, MILLISECONDS_);

-- old metric timestamp column
CREATE INDEX ACT_IDX_METER_LOG_TIME ON ACT_RU_METER_LOG(TIMESTAMP_);
