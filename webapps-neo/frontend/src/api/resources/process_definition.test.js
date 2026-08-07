import { describe, it, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  GET_TEXT: vi.fn(),
  POST: vi.fn(),
  PUT: vi.fn(),
  DELETE: vi.fn(),
}));

import { GET, GET_TEXT, POST, PUT, DELETE } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import process_definition from "./process_definition.js";

describe("api/resources/process_definition", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("list() GETs statistics without a filter", () => {
    process_definition.list(state);
    expect_api_call(GET, {
      url: "/process-definition/statistics",
      state,
      signal: state.api.process.definition.list,
    });
  });

  it("list() encodes the nameLike filter", () => {
    process_definition.list(state, "foo bar");
    expect_api_call(GET, {
      url: "/process-definition/statistics?nameLike=%25foo%20bar%25",
      state,
      signal: state.api.process.definition.list,
    });
  });

  it("one() GETs a single process definition", () => {
    process_definition.one(state, "def-1");
    expect_api_call(GET, {
      url: "/process-definition/def-1",
      state,
      signal: state.api.process.definition.one,
    });
  });

  it("called() GETs the static called process definitions", () => {
    process_definition.called(state, "def-1");
    expect_api_call(GET, {
      url: "/process-definition/def-1/static-called-process-definitions",
      state,
      signal: state.api.process.definition.called,
    });
  });

  it("by_deployment_id() GETs filtered by deployment and encodes resource name", () => {
    process_definition.by_deployment_id(state, "dep-1", "my process.bpmn");
    expect_api_call(GET, {
      url: "/process-definition?deploymentId=dep-1&resourceName=my%20process.bpmn",
      state,
      signal: state.api.process.definition.one,
    });
  });

  it("diagram() GETs the xml into the default diagram signal", () => {
    process_definition.diagram(state, "def-1");
    expect_api_call(GET, {
      url: "/process-definition/def-1/xml",
      state,
      signal: state.api.process.definition.diagram,
    });
  });

  it("diagram() uses an explicitly passed signal", () => {
    process_definition.diagram(
      state,
      "def-1",
      state.api.process.definition.one,
    );
    expect_api_call(GET, {
      url: "/process-definition/def-1/xml",
      state,
      signal: state.api.process.definition.one,
    });
  });

  it("statistics() GETs statistics with incidents", () => {
    process_definition.statistics(state, "def-1");
    expect_api_call(GET, {
      url: "/process-definition/def-1/statistics?incidents=true",
      state,
      signal: state.api.process.definition.statistics,
    });
  });

  it("list_startable() GETs startable definitions with the query string", () => {
    process_definition.list_startable(state);
    expect_api_call(GET, {
      url: "/process-definition?latest=true&active=true&startableInTasklist=true&startablePermissionCheck=true&firstResult=0&maxResults=15",
      state,
      signal: state.api.process.definition.list,
    });
  });

  it("start_form() GETs the start form by key", () => {
    process_definition.start_form(state, "myKey");
    expect_api_call(GET, {
      url: "/process-definition/key/myKey/startForm",
      state,
      signal: state.api.process.definition.start_form,
    });
  });

  it("get_deployed_start_form() GETs the deployed start form", () => {
    process_definition.get_deployed_start_form(state, "def-1");
    expect_api_call(GET, {
      url: "/process-definition/def-1/deployed-start-form",
      state,
      signal: state.api.process.definition.deployed_start_form,
    });
  });

  it("rendered_start_form() GET_TEXTs the rendered form", () => {
    process_definition.rendered_start_form(state, "def-1");
    expect_api_call(GET_TEXT, {
      url: "/process-definition/def-1/rendered-form",
      state,
      signal: state.api.process.definition.rendered_form,
    });
  });

  it("submit_form() POSTs the body to submit-form", () => {
    const body = { variables: {} };
    process_definition.submit_form(state, "def-1", body);
    expect_api_call(POST, {
      url: "/process-definition/def-1/submit-form",
      body,
      state,
      signal: state.api.process.definition.submit_form,
    });
  });

  it("activity_instance_statistics() GETs statistics", () => {
    process_definition.activity_instance_statistics(state, "def-1");
    expect_api_call(GET, {
      url: "/process-definition/def-1/statistics",
      state,
      signal: state.api.process.definition.activity_instance_statistics,
    });
  });

  it("suspend() PUTs suspended=true", () => {
    process_definition.suspend(state, "def-1");
    expect_api_call(PUT, {
      url: "/process-definition/def-1/suspended",
      body: { suspended: true, includeProcessInstances: true },
      state,
      signal: state.api.process.definition.suspend,
    });
  });

  it("activate() PUTs suspended=false", () => {
    process_definition.activate(state, "def-1");
    expect_api_call(PUT, {
      url: "/process-definition/def-1/suspended",
      body: { suspended: false, includeProcessInstances: true },
      state,
      signal: state.api.process.definition.suspend,
    });
  });

  it("remove() DELETEs with cascade=true", () => {
    process_definition.remove(state, "def-1");
    expect_api_call(DELETE, {
      url: "/process-definition/def-1?cascade=true",
      body: null,
      state,
      signal: state.api.process.definition.remove,
    });
  });
});
