import { describe, it, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  DELETE: vi.fn(),
  GET_TEXT: vi.fn(),
}));

import { GET, DELETE, GET_TEXT } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import deployment from "./deployment.js";

describe("api/resources/deployment", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("all() GETs deployments sorted by deployment time", () => {
    deployment.all(state);
    expect_api_call(GET, {
      url: "/deployment?sortBy=deploymentTime&sortOrder=desc",
      state,
      signal: state.api.deployment.all,
    });
  });

  it("resources() GETs /deployment/:id/resources", () => {
    deployment.resources(state, "dep-1");
    expect_api_call(GET, {
      url: "/deployment/dep-1/resources",
      state,
      signal: state.api.deployment.resources,
    });
  });

  it("resource() GET_TEXTs the resource data endpoint", () => {
    deployment.resource(state, "dep-1", "res-1");
    expect_api_call(GET_TEXT, {
      url: "/deployment/dep-1/resources/res-1/data",
      state,
      signal: state.api.deployment.resource,
    });
  });

  it("delete() DELETEs /deployment/:id with empty query for default params", () => {
    deployment.delete(state, "dep-1");
    expect_api_call(DELETE, {
      url: "/deployment/dep-1?",
      body: null,
      state,
      signal: state.api.deployment.delete,
    });
  });

  it("delete() encodes provided params into the query string", () => {
    deployment.delete(state, "dep-1", {
      cascade: true,
      skipCustomListeners: true,
    });
    expect_api_call(DELETE, {
      url: "/deployment/dep-1?cascade=true&skipCustomListeners=true",
      body: null,
      state,
      signal: state.api.deployment.delete,
    });
  });
});
