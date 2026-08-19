import { describe, it, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  POST: vi.fn(),
  PUT: vi.fn(),
  DELETE: vi.fn(),
}));

import { GET, POST, PUT, DELETE } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import authorization from "./authorization.js";

describe("api/resources/authorization", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("all() GETs authorizations filtered by resource type", () => {
    authorization.all(state, 7);
    expect_api_call(GET, {
      url: "/authorization?resourceType=7",
      state,
      signal: state.api.authorization.all,
    });
  });

  it("create() POSTs the body to /authorization/create", () => {
    const body = { type: 1, permissions: ["ALL"] };
    authorization.create(state, body);
    expect_api_call(POST, {
      url: "/authorization/create",
      body,
      state,
      signal: state.api.authorization.create,
    });
  });

  it("update() PUTs the body to /authorization/:id", () => {
    const body = { permissions: ["READ"] };
    authorization.update(state, "auth-1", body);
    expect_api_call(PUT, {
      url: "/authorization/auth-1",
      body,
      state,
      signal: state.api.authorization.update,
    });
  });

  it("delete() DELETEs /authorization/:id", () => {
    authorization.delete(state, "auth-1");
    expect_api_call(DELETE, {
      url: "/authorization/auth-1",
      body: {},
      state,
      signal: state.api.authorization.delete,
    });
  });
});
