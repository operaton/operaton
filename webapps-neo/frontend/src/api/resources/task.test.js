import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";

// Keep the URL/credential builders real (get_tasks uses raw fetch), but stub the
// request wrappers so we can assert call args.
vi.mock("../helper.jsx", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    GET: vi.fn(),
    POST: vi.fn(),
    PUT: vi.fn(() => Promise.resolve()),
    GET_TEXT: vi.fn(),
    GET_SERVER_URL: vi.fn(),
  };
});

// task.js imports engine_rest (circular). In the real app engine_rest is loaded
// first; in isolation we mock it so update_task's re-fetch is observable.
vi.mock("../engine_rest.jsx", () => ({
  default: { task: { get_task: vi.fn() } },
}));

import {
  GET,
  POST,
  PUT,
  GET_TEXT,
  GET_SERVER_URL,
  RESPONSE_STATE,
} from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import engine_rest from "../engine_rest.jsx";
import task from "./task.js";

describe("api/resources/task", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("get_task GETs /task/:id", () => {
    task.get_task(state, "t1");
    expect_api_call(GET, {
      url: "/task/t1",
      state,
      signal: state.api.task.one,
    });
  });

  it("get_process_instance_tasks GETs tasks by process instance", () => {
    task.get_process_instance_tasks(state, "pi1");
    expect_api_call(GET, {
      url: "/task?processInstanceId=pi1",
      state,
      signal: state.api.task.by_process_instance,
    });
  });

  it("get_task_form uses GET_SERVER_URL", () => {
    task.get_task_form(state, "embedded:app:form.html");
    expect_api_call(GET_SERVER_URL, {
      url: "/embedded:app:form.html",
      state,
      signal: state.api.task.form,
    });
  });

  it("get_task_rendered_form uses GET_TEXT", () => {
    task.get_task_rendered_form(state, "t1");
    expect_api_call(GET_TEXT, {
      url: "/task/t1/rendered-form",
      state,
      signal: state.api.task.rendered_form,
    });
  });

  it("get_task_deployed_form / get_task_form_variables GET the right URLs", () => {
    task.get_task_deployed_form(state, "t1");
    expect_api_call(GET, {
      url: "/task/t1/deployed-form",
      state,
      signal: state.api.task.deployed_form,
    });
    task.get_task_form_variables(state, "t1");
    expect_api_call(GET, {
      url: "/task/t1/form-variables",
      state,
      signal: state.api.task.form_variables,
    });
  });

  it("claim_task POSTs the current user id", () => {
    task.claim_task(state, "t1");
    expect_api_call(POST, {
      url: "/task/t1/claim",
      body: { userId: "demo" },
      state,
      signal: state.api.task.claim_result,
    });
  });

  it("unclaim_task POSTs the current user id", () => {
    task.unclaim_task(state, "t1");
    expect_api_call(POST, {
      url: "/task/t1/unclaim",
      body: { userId: "demo" },
      state,
      signal: state.api.task.unclaim_result,
    });
  });

  it("assign_task POSTs the assignee", () => {
    task.assign_task(state, "alice", "t1");
    expect_api_call(POST, {
      url: "/task/t1/assignee",
      body: { userId: "alice" },
      state,
      signal: state.api.task.assign_result,
    });
  });

  it("add_group / delete_group POST candidate identity links", () => {
    task.add_group(state, "t1", "sales");
    expect_api_call(POST, {
      url: "/task/t1/identity-links",
      body: { groupId: "sales", type: "candidate" },
      state,
      signal: state.api.task.add_group,
    });
    task.delete_group(state, "t1", "sales");
    expect_api_call(POST, {
      url: "/task/t1/identity-links/delete",
      body: { groupId: "sales", type: "candidate" },
      state,
      signal: state.api.task.delete_group,
    });
  });

  it("get_identity_links / get_comments GET the right URLs", () => {
    task.get_identity_links(state, "t1");
    expect_api_call(GET, {
      url: "/task/t1/identity-links",
      state,
      signal: state.api.task.identity_links,
    });
    task.get_comments(state, "t1");
    expect_api_call(GET, {
      url: "/task/t1/comment",
      state,
      signal: state.api.task.comment.list,
    });
  });

  it("create_comment POSTs the message", () => {
    task.create_comment(state, "t1", "hello");
    expect_api_call(POST, {
      url: "/task/t1/comment/create",
      body: { message: "hello" },
      state,
      signal: state.api.task.comment.create,
    });
  });

  it("post_task_form submits variables with withVariablesInReturn", () => {
    const data = { amount: { value: 10 } };
    task.post_task_form(state, "t1", data);
    expect_api_call(POST, {
      url: "/task/t1/submit-form",
      body: { variables: data, withVariablesInReturn: true },
      state,
      signal: state.api.task.submit_form,
    });
  });

  describe("update_task", () => {
    it("merges the changeset onto the cached task and re-fetches afterwards", async () => {
      state.api.task.one.value = {
        data: { id: "t1", name: "Old", assignee: null },
      };
      await task.update_task(state, { assignee: "alice" }, "t1");

      expect_api_call(PUT, {
        url: "/task/t1",
        body: { id: "t1", name: "Old", assignee: "alice" },
        state,
        signal: state.api.task.one,
      });
      // After the PUT resolves it re-fetches the task via engine_rest.
      expect(engine_rest.task.get_task).toHaveBeenCalled();
      const [refetch_state, refetch_id] =
        engine_rest.task.get_task.mock.lastCall;
      expect(refetch_state).toBe(state);
      expect(refetch_id).toBe("t1");
    });
  });

  describe("raw fetch endpoints", () => {
    let fetchMock;
    beforeEach(() => {
      fetchMock = vi.fn();
      vi.stubGlobal("fetch", fetchMock);
    });
    afterEach(() => vi.unstubAllGlobals());

    it("get_tasks builds the query string and stores SUCCESS", async () => {
      fetchMock.mockResolvedValue({
        ok: true,
        json: async () => [{ id: "t1" }, { id: "t2" }],
      });
      task.get_tasks(state, "created", "desc", 0, 15, { assignee: "demo" });
      await vi.waitFor(() =>
        expect(state.api.task.list.value.status).toBe(RESPONSE_STATE.SUCCESS),
      );

      const url = fetchMock.mock.calls[0][0];
      expect(url).toContain("/engine-rest/task?");
      expect(url).toContain("sortBy=created");
      expect(url).toContain("sortOrder=desc");
      expect(url).toContain("assignee=demo");
      expect(state.api.task.list.value.data).toEqual([
        { id: "t1" },
        { id: "t2" },
      ]);
    });

    it("get_task_process_definitions fetches by id list and returns json", async () => {
      fetchMock.mockResolvedValue({
        ok: true,
        json: async () => [{ id: "d1" }],
      });
      const result = await task.get_task_process_definitions(state, ["d1"]);
      expect(fetchMock.mock.calls[0][0]).toContain(
        "/process-definition?processDefinitionIdIn=d1",
      );
      expect(result).toEqual([{ id: "d1" }]);
    });
  });
});
