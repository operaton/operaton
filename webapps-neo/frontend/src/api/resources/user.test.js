import { describe, it, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  POST: vi.fn(),
  PUT: vi.fn(),
  DELETE: vi.fn(),
}));

import { GET, POST, PUT, DELETE } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import user from "./user.js";

describe("api/resources/user", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("all() GETs /user into the list signal", () => {
    user.all(state);
    expect_api_call(GET, {
      url: "/user",
      state,
      signal: state.api.user.list,
    });
  });

  it("create() POSTs the user to /user/create", () => {
    const body = { id: "alice" };
    user.create(state, body);
    expect_api_call(POST, {
      url: "/user/create",
      body,
      state,
      signal: state.api.user.create,
    });
  });

  it("delete() DELETEs /user/:name", () => {
    user.delete(state, "alice");
    expect_api_call(DELETE, {
      url: "/user/alice",
      body: {},
      state,
      signal: state.api.user.delete,
    });
  });

  it("count() GETs /user into the count signal", () => {
    user.count(state);
    expect_api_call(GET, {
      url: "/user",
      state,
      signal: state.api.user.count,
    });
  });

  it("profile.get() defaults to demo when no name given", () => {
    user.profile.get(state);
    expect_api_call(GET, {
      url: "/user/demo/profile",
      state,
      signal: state.api.user.profile,
    });
  });

  it("profile.get() uses the given name", () => {
    user.profile.get(state, "alice");
    expect_api_call(GET, {
      url: "/user/alice/profile",
      state,
      signal: state.api.user.profile,
    });
  });

  it("profile.update() PUTs the profile to /user/:name/profile", () => {
    const body = { firstName: "Alice" };
    user.profile.update(state, "alice", body);
    expect_api_call(PUT, {
      url: "/user/alice/profile",
      body,
      state,
      signal: state.api.user.update,
    });
  });

  it("credentials_update() PUTs to /user/:name/credentials", () => {
    const body = { password: "secret" };
    user.credentials_update(state, "alice", body);
    expect_api_call(PUT, {
      url: "/user/alice/credentials",
      body,
      state,
      signal: state.api.user.credentials,
    });
  });

  it("unlock() POSTs to /user/:name/unlock", () => {
    user.unlock(state, "alice");
    expect_api_call(POST, {
      url: "/user/alice/unlock",
      body: {},
      state,
      signal: state.api.user.unlock,
    });
  });
});
