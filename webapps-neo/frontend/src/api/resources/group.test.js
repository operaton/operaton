import { describe, it, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  POST: vi.fn(),
  PUT: vi.fn(),
  DELETE: vi.fn(),
}));

import { GET, POST, PUT, DELETE } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import group from "./group.js";

describe("api/resources/group", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("all() POSTs the paged query to /group", () => {
    group.all(state);
    expect_api_call(POST, {
      url: "/group",
      body: { firstResult: 0, maxResults: 50, sortBy: "id", sortOrder: "asc" },
      state,
      signal: state.api.group.list,
    });
  });

  it("create() POSTs the group to /group/create", () => {
    const body = { id: "admins" };
    group.create(state, body);
    expect_api_call(POST, {
      url: "/group/create",
      body,
      state,
      signal: state.api.group.create,
    });
  });

  it("update() PUTs the group to /group/:id", () => {
    const body = { name: "Admins" };
    group.update(state, "admins", body);
    expect_api_call(PUT, {
      url: "/group/admins",
      body,
      state,
      signal: state.api.group.update,
    });
  });

  it("delete() DELETEs /group/:id", () => {
    group.delete(state, "admins");
    expect_api_call(DELETE, {
      url: "/group/admins",
      body: {},
      state,
      signal: state.api.group.delete,
    });
  });

  it("by_member() POSTs to /group with the given member", () => {
    group.by_member(state, "alice");
    expect_api_call(POST, {
      url: "/group",
      body: { member: "alice", firstResult: 0, maxResults: 50 },
      state,
      signal: state.api.user.group.list,
    });
  });

  it("by_member() falls back to state.auth.user.id when no member given", () => {
    state.auth.user.id.value = "bob";
    group.by_member(state);
    expect_api_call(POST, {
      url: "/group",
      body: { member: "bob", firstResult: 0, maxResults: 50 },
      state,
      signal: state.api.user.group.list,
    });
  });

  it("members() GETs /user filtered by memberOfGroup", () => {
    group.members(state, "admins");
    expect_api_call(GET, {
      url: "/user?memberOfGroup=admins&firstResult=0&maxResults=50",
      state,
      signal: state.api.group.members,
    });
  });

  it("add_user() PUTs to /group/:id/members/:user with body", () => {
    group.add_user(state, "admins", "alice");
    expect_api_call(PUT, {
      url: "/group/admins/members/alice",
      body: { id: "admins", userId: "alice" },
      state,
      signal: state.api.group.add_user,
    });
  });

  it("remove_member() DELETEs /group/:id/members/:user with body", () => {
    group.remove_member(state, "admins", "alice");
    expect_api_call(DELETE, {
      url: "/group/admins/members/alice",
      body: { id: "admins", userId: "alice" },
      state,
      signal: state.api.group.remove_member,
    });
  });
});
