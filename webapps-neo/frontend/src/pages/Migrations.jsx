import { signal, useSignal } from "@preact/signals";
import { useContext, useEffect, useMemo } from "preact/hooks";
import { useTranslation } from "react-i18next";
import engine_rest, { RequestState } from "../api/engine_rest.jsx";
import { AppState } from "../state.js";
import { createContext } from "preact";
import ReactBpmn from "react-bpmn";
import BpmnModdle from "bpmn-moddle";
import { useLocation } from "preact-iso/router";
import { has_data } from "../api/helper.jsx";
import {
  VARIABLE_TYPES,
  QUERY_FIELDS_KEYS,
  build_migration_plan,
  build_migration_plan_with_variables,
  update_mapping,
  add_variable,
  remove_variable,
  update_variable,
  add_query_row,
  remove_query_row,
  update_query_row,
  build_query,
  add_query_params_abstract,
} from "./migration_helpers.js";

const create_migration_state = () => {
  const source = signal(null),
    source_diagram = signal(null),
    source_activities = signal(null),
    target = signal(null),
    target_diagram = signal(null),
    target_activities = signal(null),
    mappings = signal({}),
    selected_process_instances = signal({}),
    variables = signal([]),
    process_instance_query = signal([]);

  return {
    source,
    source_diagram,
    source_activities,
    target,
    target_diagram,
    target_activities,
    mappings,
    selected_process_instances,
    variables,
    process_instance_query,
  };
};

const MigrationState = createContext(undefined);

const MigrationsPage = () => {
  const state = useContext(AppState),
    migration_state = useMemo(() => create_migration_state(), []);

  useEffect(() => {
    void engine_rest.process_definition.list(state);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <MigrationState.Provider value={migration_state}>
      <main>
        <ProcessSelection />
      </main>
    </MigrationState.Provider>
  );
};

const bpmnModdle = new BpmnModdle();

/**
 * Takes the XML representation of a BPMN diagram, gets all the activities and
 * filters for non-immediate and mappable activity types.
 */
const find_activities_of_diagram = (diagram_signal, activities_signal) =>
  bpmnModdle
    .fromXML(diagram_signal.value.data?.bpmn20Xml)
    .then(({ rootElement: definitions }) => {
      return (activities_signal.value = {
        status: "SUCCESS",
        data: definitions.rootElements
          .find(({ $type }) => $type === "bpmn:Process")
          .flowElements.filter(
            ({ isImmediate, $type }) =>
              !(
                isImmediate ||
                $type === "bpmn:SequenceFlow" ||
                $type === "bpmn:DataStoreReference"
              ),
          ),
      });
    })
    .catch((error) => (activities_signal.value = { status: "ERROR", error }));

const validate = (state, migration_state) => {
  if (
    migration_state.mappings.peek() !== null &&
    state.api.migration.generate.peek() !== null &&
    state.api.migration.generate.peek().data !== null
  ) {
    engine_rest.migration.validate(
      state,
      build_migration_plan(
        state.api.migration.generate.peek().data,
        migration_state.mappings.peek(),
      ),
    );
  }
};

const generate_abstract = (migration_state, state) =>
  migration_state.source.value !== null && migration_state.target.value !== null
    ? engine_rest.migration
        .generate(
          state,
          migration_state.source.value,
          migration_state.target.value,
        )
        .then(
          () =>
            (migration_state.mappings.value = Object.fromEntries(
              state.api.migration.generate.value.data.instructions.map(
                (instruction) => [
                  instruction.sourceActivityIds[0],
                  instruction.targetActivityIds[0],
                ],
              ),
            )),
        )
    : null;

const ProcessSelection = () => {
  const state = useContext(AppState),
    {
      api: {
        process: {
          definition: { list },
        },
      },
    } = state,
    migration_state = useContext(MigrationState),
    { route, url, query, path } = useLocation(),
    [t] = useTranslation(),
    add_query_params = (name, value) =>
      add_query_params_abstract(query, url, route, path, name, value),
    generate = () => generate_abstract(migration_state, state),
    execute_form_data = useSignal({
      async: false,
      skip_io_mappings: false,
      skip_custom_listeners: false,
    });

  // All fetching/generation is driven from the URL query params (the single
  // source of truth) so nothing mutates signals or fetches during render.
  useEffect(() => {
    if (query.source === undefined || query.target === undefined) {
      state.api.migration.validation.value = null;
      state.api.migration.generate.value = null;
      state.api.process.instance.by_defintion_id.value = null;
    }

    if (query.source) {
      migration_state.source.value = query.source;
      void engine_rest.process_definition
        .diagram(state, query.source, migration_state.source_diagram)
        .then(() =>
          find_activities_of_diagram(
            migration_state.source_diagram,
            migration_state.source_activities,
          ),
        );
      void engine_rest.process_instance.by_defintion_id(state, query.source);
    }

    if (query.target) {
      migration_state.target.value = query.target;
      void engine_rest.process_definition
        .diagram(state, query.target, migration_state.target_diagram)
        .then(() =>
          find_activities_of_diagram(
            migration_state.target_diagram,
            migration_state.target_activities,
          ),
        );
    }

    if (query.source && query.target) {
      const generation = generate();
      if (generation) generation.then(() => validate(state, migration_state));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query.source, query.target]);

  // Execution is only allowed once a valid, non-empty mapping plan exists.
  const has_validation_errors =
      has_data(state.api.migration.validation) &&
      state.api.migration.validation.value.data.instructionReports.length > 0,
    can_execute =
      has_data(state.api.migration.generate) &&
      Object.keys(migration_state.mappings.value).length > 0 &&
      !has_validation_errors;

  return (
    <div id="migration">
      <h1 class="screen-hidden">{t("migrations.title")}</h1>

      <form
        class="migration-steps"
        onSubmit={(event) => {
          event.preventDefault();
          const migration_plan = build_migration_plan_with_variables(
              state.api.migration.generate.peek().data,
              migration_state.mappings.peek(),
              migration_state.variables.peek(),
            ),
            selected_ids = Object.entries(
              migration_state.selected_process_instances.peek(),
            )
              .filter(([, v]) => v)
              .map(([k]) => k),
            query_obj = build_query(
              migration_state.process_instance_query.peek(),
            ),
            has_query = Object.keys(query_obj).length > 0;
          engine_rest.migration.execute(
            state,
            migration_plan,
            selected_ids.length > 0 ? selected_ids : null,
            has_query ? query_obj : null,
            execute_form_data.peek().skip_custom_listeners,
            execute_form_data.peek().skip_io_mappings,
            execute_form_data.peek().async,
          );
        }}
      >
        <section class="migration-step">
          <h2>
            <span class="step-number">1</span> {t("migrations.step-1-select")}
          </h2>
          <div class="two-col">
            <div>
              <label>{t("migrations.source")} </label>
              <select
                onChange={(e) => add_query_params("source", e.target.value)}
              >
                <option
                  value=""
                  disabled
                  selected={migration_state.source.value === null}
                >
                  {t("migrations.select-source")}
                </option>
                <RequestState
                  signal={list}
                  on_success={() =>
                    list.value.data.map(({ id, definition }) => (
                      <option
                        key={id}
                        disabled={migration_state.target.value === id}
                        selected={migration_state.source.value === id}
                        value={id}
                      >
                        {definition.name} – v{definition.version}
                      </option>
                    ))
                  }
                />
              </select>
            </div>

            <div>
              <label>{t("migrations.target")} </label>
              <select
                onChange={(e) => add_query_params("target", e.target.value)}
              >
                <option
                  value=""
                  disabled
                  selected={migration_state.target.value === null}
                >
                  {t("migrations.select-target")}
                </option>
                <RequestState
                  signal={list}
                  on_success={() =>
                    list.value.data.map(({ id, definition }) => (
                      <option
                        key={id}
                        disabled={migration_state.source.value === id}
                        selected={migration_state.target.value === id}
                        value={id}
                      >
                        {definition.name} – v{definition.version}
                      </option>
                    ))
                  }
                />
              </select>
            </div>
          </div>

          <Diagrams />
        </section>

        <section class="migration-step">
          <h2>
            <span class="step-number">2</span> {t("migrations.step-2-map")}
          </h2>
          <RequestState
            signal={migration_state.target_activities}
            on_nothing={() => (
              <p>
                <small>{t("migrations.select-definitions-hint")}</small>
              </p>
            )}
            on_success={() => <Mappings />}
          />
        </section>

        <section class="migration-step">
          <h2>
            <span class="step-number">3</span>{" "}
            {t("migrations.step-3-instances")}
          </h2>
          <ProcessInstanceSelection />

          <RequestState
            signal={[
              state.api.process.instance.by_defintion_id,
              state.api.migration.validation,
            ]}
            on_nothing={() => <></>}
            on_success={() =>
              has_validation_errors ? (
                <></>
              ) : (
                <>
                  <ProcessInstanceQuery />
                  <Variables />
                </>
              )
            }
          />
        </section>

        <section class="migration-step" id="execute">
          <h2>
            <span class="step-number">4</span> {t("migrations.step-4-execute")}
          </h2>

          <fieldset class="execute-options">
            <legend>{t("migrations.options")}</legend>
            <div>
              <input
                type="checkbox"
                id="async"
                name="async"
                onInput={(e) =>
                  (execute_form_data.value = {
                    ...execute_form_data.peek(),
                    async: e.currentTarget.checked,
                  })
                }
              />
              <label for="async">{t("migrations.async")}</label>
            </div>

            <div>
              <input
                type="checkbox"
                id="skip_custom_listeners"
                name="skip_custom_listeners"
                onInput={(e) =>
                  (execute_form_data.value = {
                    ...execute_form_data.peek(),
                    skip_custom_listeners: e.currentTarget.checked,
                  })
                }
              />
              <label for="skip_custom_listeners">
                {t("migrations.skip-custom-listeners")}
              </label>
            </div>

            <div>
              <input
                type="checkbox"
                id="skip_io_mappings"
                name="skip_io_mappings"
                onInput={(e) =>
                  (execute_form_data.value = {
                    ...execute_form_data.peek(),
                    skip_io_mappings: e.currentTarget.checked,
                  })
                }
              />
              <label for="skip_io_mappings">
                {t("migrations.skip-io-mappings")}
              </label>
            </div>
          </fieldset>

          <RequestState
            signal={state.api.migration.execution}
            on_nothing={() => <></>}
            on_success={() => (
              <p class="success">
                {execute_form_data.value.async
                  ? t("migrations.async-batch-created")
                  : t("migrations.success")}
              </p>
            )}
            on_error={
              <p class="error">
                <strong>{t("migrations.failed")} </strong>
                {state.api.migration.execution.value?.error?.message ??
                  t("migrations.unknown-error")}
              </p>
            }
          />

          <div class="button-group">
            <button type="submit" disabled={!can_execute}>
              {t("migrations.execute")}
            </button>
          </div>
          {!can_execute && (
            <small class="execute-disabled-hint">
              {t("migrations.execute-disabled-hint")}
            </small>
          )}
        </section>
      </form>
    </div>
  );
};

const Diagrams = () => {
  const migration_state = useContext(MigrationState),
    [t] = useTranslation();

  return (
    <div class="migration-diagrams two-col">
      <div>
        <h3 class="screen-hidden">{t("migrations.source")}</h3>
        <RequestState
          signal={migration_state.source_diagram}
          on_nothing={() => (
            <p>
              <small>{t("migrations.select-source-definition")}</small>
            </p>
          )}
          on_success={() => (
            <ReactBpmn
              diagramXML={migration_state.source_diagram.value.data?.bpmn20Xml}
            />
          )}
        />
      </div>
      <div>
        <h3 class="screen-hidden">{t("migrations.target")}</h3>
        <RequestState
          signal={migration_state.target_diagram}
          on_nothing={() => (
            <p>
              <small>{t("migrations.select-target-definition")}</small>
            </p>
          )}
          on_success={() => (
            <ReactBpmn
              diagramXML={migration_state.target_diagram.value.data?.bpmn20Xml}
            />
          )}
        />
      </div>
    </div>
  );
};

const handle_mapping_change = (e, source_activity, migration_state, state) => {
  migration_state.mappings.value = update_mapping(
    e.target.value,
    source_activity.id,
    migration_state.mappings.peek(),
  );
  validate(state, migration_state);
};

const generate_mapping_rows = (migration_state, state, t) => (
  <RequestState
    signal={[
      migration_state.target_activities,
      migration_state.source_activities,
    ]}
    on_success={() =>
      migration_state.source_activities.value.data.map((source_activity) => (
        <tr key={source_activity.id}>
          <td>{source_activity.name}</td>
          <td>
            <select
              onChange={(e) =>
                handle_mapping_change(
                  e,
                  source_activity,
                  migration_state,
                  state,
                )
              }
            >
              <option value="">{t("migrations.do-not-map")}</option>

              {migration_state.target_activities.value.data.map(
                (target_activity) => (
                  <option
                    key={target_activity.id}
                    value={target_activity.id}
                    selected={
                      migration_state.mappings.value[source_activity.id] ===
                      target_activity.id
                    }
                  >
                    {target_activity.name} ({target_activity.id})
                  </option>
                ),
              )}
            </select>
          </td>
          <td>
            <RequestState
              signal={state.api.migration.validation}
              on_nothing={() => <p>{t("migrations.not-validated")}</p>}
              on_success={() =>
                state.api.migration.validation.value.data.instructionReports.some(
                  (report) =>
                    report.instruction.sourceActivityIds[0] ===
                    source_activity.id,
                ) ? (
                  <p>✗ {t("common.no")}</p>
                ) : (
                  <p>✓ {t("common.yes")}</p>
                )
              }
            />
          </td>
        </tr>
      ))
    }
  />
);

const Mappings = () => {
  const state = useContext(AppState),
    migration_state = useContext(MigrationState),
    [t] = useTranslation();

  return (
    <>
      <h3 class="screen-hidden">{t("migrations.mappings")}</h3>

      <RequestState
        signal={state.api.migration.generate}
        on_nothing={() => (
          <small>{t("migrations.select-definitions-hint")}</small>
        )}
        on_success={() => (
          <table>
            <thead>
              <tr>
                <th scope="column">{t("migrations.source-activity")}</th>
                <th scope="column">{t("migrations.target-activity")}</th>
                <th scope="column">{t("migrations.valid")}</th>
              </tr>
            </thead>
            <tbody>{generate_mapping_rows(migration_state, state, t)}</tbody>
          </table>
        )}
      />

      <RequestState
        signal={state.api.migration.validation}
        on_nothing={() => <></>}
        on_success={() => (
          <div aria-live="polite">
            {state.api.migration.validation.value.data.instructionReports
              .length > 0 ? (
              <>
                <h3>{t("migrations.validation-errors")}</h3>
                <ul>
                  {state.api.migration.validation.value.data.instructionReports.map(
                    (report) =>
                      report.failures.map((failure) => (
                        <li key={failure.toString()}>{failure}</li>
                      )),
                  )}
                </ul>
              </>
            ) : null}
          </div>
        )}
      />
    </>
  );
};

const Variables = () => {
  const migration_state = useContext(MigrationState),
    [t] = useTranslation();

  return (
    <>
      <h3>{t("migrations.variables")}</h3>
      {migration_state.variables.value.length > 0 && (
        <table>
          <thead>
            <tr>
              <th scope="column">{t("common.name")}</th>
              <th scope="column">{t("common.type")}</th>
              <th scope="column">{t("common.value")}</th>
              <th scope="column">{t("common.actions")}</th>
            </tr>
          </thead>
          <tbody>
            {migration_state.variables.value.map((variable, index) => (
              <tr key={index}>
                <td>
                  <input
                    type="text"
                    value={variable.name}
                    placeholder={t("migrations.variable-name")}
                    onInput={(e) =>
                      update_variable(
                        migration_state,
                        index,
                        "name",
                        e.currentTarget.value,
                      )
                    }
                  />
                </td>
                <td>
                  <select
                    value={variable.type}
                    onChange={(e) =>
                      update_variable(
                        migration_state,
                        index,
                        "type",
                        e.target.value,
                      )
                    }
                  >
                    {VARIABLE_TYPES.map((type) => (
                      <option key={type} value={type}>
                        {type}
                      </option>
                    ))}
                  </select>
                </td>
                <td>
                  <input
                    type="text"
                    value={variable.value}
                    placeholder={t("common.value")}
                    onInput={(e) =>
                      update_variable(
                        migration_state,
                        index,
                        "value",
                        e.currentTarget.value,
                      )
                    }
                  />
                </td>
                <td>
                  <button
                    type="button"
                    onClick={() => remove_variable(migration_state, index)}
                  >
                    {t("common.remove")}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <div class="button-group">
        <button type="button" onClick={() => add_variable(migration_state)}>
          {t("migrations.add-variable")}
        </button>
      </div>
    </>
  );
};

const ProcessInstanceQuery = () => {
  const migration_state = useContext(MigrationState),
    [t] = useTranslation();

  return (
    <>
      <h3>{t("migrations.process-instance-query")}</h3>
      <small>
        {t("migrations.query-hint-prefix")}{" "}
        <code>processDefinitionId: "source process definition ID"</code>{" "}
        {t("migrations.query-hint-suffix")}
      </small>
      {migration_state.process_instance_query.value.length > 0 && (
        <table>
          <thead>
            <tr>
              <th scope="column">{t("migrations.filter")}</th>
              <th scope="column">{t("common.value")}</th>
              <th scope="column">{t("common.actions")}</th>
            </tr>
          </thead>
          <tbody>
            {migration_state.process_instance_query.value.map((row, index) => {
              const field_def = QUERY_FIELDS_KEYS.find(
                (f) => f.name === row.field,
              );
              return (
                <tr key={index}>
                  <td>
                    <select
                      value={row.field}
                      onChange={(e) =>
                        update_query_row(
                          migration_state,
                          index,
                          "field",
                          e.target.value,
                        )
                      }
                    >
                      {QUERY_FIELDS_KEYS.map(({ name, labelKey }) => (
                        <option key={name} value={name}>
                          {t(labelKey)}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    {field_def?.type === "boolean" ? (
                      <select
                        value={row.value}
                        onChange={(e) =>
                          update_query_row(
                            migration_state,
                            index,
                            "value",
                            e.target.value,
                          )
                        }
                      >
                        <option value="true">true</option>
                        <option value="false">false</option>
                      </select>
                    ) : (
                      <input
                        type="text"
                        value={row.value}
                        placeholder={
                          field_def?.type === "array" ? "value1, value2" : ""
                        }
                        onInput={(e) =>
                          update_query_row(
                            migration_state,
                            index,
                            "value",
                            e.target.value,
                          )
                        }
                      />
                    )}
                  </td>
                  <td>
                    <button
                      type="button"
                      onClick={() => remove_query_row(migration_state, index)}
                    >
                      {t("common.remove")}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
      <div class="button-group">
        <button type="button" onClick={() => add_query_row(migration_state)}>
          {t("migrations.add-filter")}
        </button>
      </div>
    </>
  );
};

const ProcessInstanceSelection = () => {
  const state = useContext(AppState),
    migration_state = useContext(MigrationState),
    [t] = useTranslation(),
    instances = state.api.process.instance.by_defintion_id,
    has_errors = () =>
      state.api.migration.validation.value.data.instructionReports.length > 0,
    set_all = (checked) =>
      (migration_state.selected_process_instances.value = checked
        ? Object.fromEntries(instances.value.data.map(({ id }) => [id, true]))
        : {});

  return (
    <RequestState
      signal={[instances, state.api.migration.validation]}
      on_nothing={() => <small>{t("migrations.define-mappings-hint")}</small>}
      on_success={() =>
        has_errors() ? (
          <p>{t("migrations.invalid-mappings")}</p>
        ) : (
          <>
            <h3>{t("migrations.select-instances")}</h3>
            {instances.value.data.length === 0 ? (
              <p>{t("migrations.no-processes-for-migration")}</p>
            ) : (
              <>
                <div class="instance-toolbar">
                  <small>
                    {t("migrations.instance-count", {
                      n: instances.value.data.length,
                    })}
                  </small>
                  <span class="button-group">
                    <button type="button" onClick={() => set_all(true)}>
                      {t("migrations.select-all")}
                    </button>
                    <button type="button" onClick={() => set_all(false)}>
                      {t("migrations.clear-selection")}
                    </button>
                  </span>
                </div>
                <p>
                  <small>{t("migrations.migrate-all-hint")}</small>
                </p>
                <ul class="instance-list">
                  {instances.value.data.map(({ id }) => (
                    <li key={id}>
                      <label>
                        <input
                          type="checkbox"
                          checked={
                            !!migration_state.selected_process_instances.value[
                              id
                            ]
                          }
                          onChange={(e) =>
                            (migration_state.selected_process_instances.value =
                              {
                                ...migration_state.selected_process_instances.peek(),
                                [id]: e.target.checked,
                              })
                          }
                        />
                        {id}
                      </label>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </>
        )
      }
    />
  );
};

export { MigrationsPage };
