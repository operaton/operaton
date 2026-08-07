import { useContext, useEffect } from "preact/hooks";
import { useSignal } from "@preact/signals";
import { useTranslation } from "react-i18next";
import { useRoute } from "preact-iso";
import { AppState } from "../state.js";
import engine_rest, { RequestState } from "../api/engine_rest.jsx";
import { ConfirmDialog } from "../components/Dialog.jsx";

const BatchesPage = () => {
  const state = useContext(AppState),
    {
      params: { batch_id },
    } = useRoute(),
    [t] = useTranslation();

  // Load the batch list on mount.
  useEffect(() => {
    void engine_rest.batch.all(state);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Load the selected batch's statistics; clear on navigation.
  useEffect(() => {
    if (batch_id) {
      void engine_rest.batch.one(state, batch_id);
    }
    return () => {
      state.api.batch.one.value = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [batch_id]);

  return (
    <main id="content" class="batches fade-in">
      <BatchesList />
      {batch_id ? (
        <BatchDetails />
      ) : (
        <div class="batch-empty">{t("batches.select-batch")}</div>
      )}
    </main>
  );
};

const Progress = ({ batch }) => {
  const total = batch.totalJobs ?? 0,
    completed = batch.completedJobs ?? 0;
  return (
    <>
      <progress value={completed} max={total || 1} /> {completed}/{total}
    </>
  );
};

const BatchesList = () => {
  const state = useContext(AppState),
    { params } = useRoute(),
    [t] = useTranslation();

  return (
    <div>
      <header>
        <h1>{t("batches.title")}</h1>
        <button
          type="button"
          class="secondary"
          onClick={() => engine_rest.batch.all(state)}
        >
          {t("batches.refresh")}
        </button>
      </header>
      <RequestState
        signal={state.api.batch.list}
        on_success={() => {
          const rows = state.api.batch.list.value?.data ?? [];
          if (rows.length === 0)
            return <p class="info-box">{t("batches.empty")}</p>;
          return (
            <table>
              <thead>
                <tr>
                  <th>{t("batches.id")}</th>
                  <th>{t("batches.type")}</th>
                  <th>{t("batches.progress")}</th>
                  <th class="num">{t("batches.failed-jobs")}</th>
                  <th>{t("batches.state")}</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((batch) => (
                  <tr
                    key={batch.id}
                    aria-selected={params.batch_id === batch.id}
                  >
                    <th scope="row">
                      <a href={`/batches/${batch.id}`}>
                        {batch.id.substring(0, 8)}
                      </a>
                    </th>
                    <td>{batch.type}</td>
                    <td>
                      <Progress batch={batch} />
                    </td>
                    <td class="num">{batch.failedJobs ?? 0}</td>
                    <td>
                      {batch.suspended
                        ? t("batches.suspended")
                        : t("batches.running")}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          );
        }}
      />
    </div>
  );
};

const BatchDetails = () => {
  const state = useContext(AppState),
    {
      params: { batch_id },
    } = useRoute(),
    [t] = useTranslation(),
    confirm_delete = useSignal(false);

  const reload = () => {
    void engine_rest.batch.one(state, batch_id);
    void engine_rest.batch.all(state);
  };

  const toggle_suspended = async (suspended) => {
    await engine_rest.batch.set_suspended(state, batch_id, suspended);
    reload();
  };

  const remove = async () => {
    await engine_rest.batch.delete(state, batch_id);
    void engine_rest.batch.all(state);
  };

  return (
    <div id="batch-details">
      <RequestState
        signal={state.api.batch.one}
        on_nothing={() => <p class="info-box">{t("batches.select-batch")}</p>}
        on_success={() => {
          const batch = state.api.batch.one.value?.data?.[0];
          if (!batch) return <p class="info-box">{t("batches.empty")}</p>;
          return (
            <div>
              <header>
                <h1>{batch.id}</h1>
              </header>
              <dl>
                <dt>{t("batches.type")}</dt>
                <dd>{batch.type}</dd>
                <dt>{t("batches.progress")}</dt>
                <dd>
                  <Progress batch={batch} />
                </dd>
                <dt>{t("batches.total-jobs")}</dt>
                <dd>{batch.totalJobs ?? 0}</dd>
                <dt>{t("batches.remaining-jobs")}</dt>
                <dd>{batch.remainingJobs ?? 0}</dd>
                <dt>{t("batches.failed-jobs")}</dt>
                <dd>{batch.failedJobs ?? 0}</dd>
                <dt>{t("batches.created-by")}</dt>
                <dd>{batch.createUserId ?? "—"}</dd>
                <dt>{t("batches.state")}</dt>
                <dd>
                  {batch.suspended
                    ? t("batches.suspended")
                    : t("batches.running")}
                </dd>
              </dl>
              <div class="button-group">
                {batch.suspended ? (
                  <button type="button" onClick={() => toggle_suspended(false)}>
                    {t("batches.resume")}
                  </button>
                ) : (
                  <button type="button" onClick={() => toggle_suspended(true)}>
                    {t("batches.suspend")}
                  </button>
                )}
                <button
                  type="button"
                  class="danger"
                  onClick={() => (confirm_delete.value = true)}
                >
                  {t("batches.delete")}
                </button>
              </div>
              <ConfirmDialog
                open={confirm_delete}
                message={t("batches.confirm-delete")}
                confirm_label={t("batches.delete")}
                on_confirm={remove}
              />
            </div>
          );
        }}
      />
    </div>
  );
};

export { BatchesPage };
