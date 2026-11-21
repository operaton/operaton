--
-- Copyright 2026 the Operaton contributors.
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at:
--
--     https://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- https://github.com/operaton/operaton/issues/4030
-- Indexes are built concurrently (changeset runs with runInTransaction=false).
-- A failed concurrent build leaves an INVALID index that 'if not exists' skips on re-run: drop it and re-run.
-- Check for invalid indexes with: select indexrelid::regclass from pg_index where not indisvalid;
create index concurrently if not exists ACT_IDX_TASK_PARENT_TASK_ID on ACT_RU_TASK(PARENT_TASK_ID_);
create index concurrently if not exists ACT_IDX_HI_TASK_PARENT_TASK_ID on ACT_HI_TASKINST(PARENT_TASK_ID_);

-- schema log last: the changeset is not transactional
insert into ACT_GE_SCHEMA_LOG
values ('1500', CURRENT_TIMESTAMP, '7.26.0');
