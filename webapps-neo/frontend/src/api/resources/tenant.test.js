import { describe, it, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  POST: vi.fn(),
  PUT: vi.fn(),
  DELETE: vi.fn(),
}));

import { GET, POST, PUT, DELETE } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import tenant from "./tenant.js";

describe("api/resources/tenant", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("all() GETs the paged tenant list", () => {
    tenant.all(state);
    expect_api_call(GET, {
      url: "/tenant?firstResult=0&maxResults=50&sortBy=id&sortOrder=asc",
      state,
      signal: state.api.tenant.list,
    });
  });

  it("create() POSTs the tenant to /tenant/create", () => {
    const body = { id: "acme" };
    tenant.create(state, body);
    expect_api_call(POST, {
      url: "/tenant/create",
      body,
      state,
      signal: state.api.tenant.create,
    });
  });

  it("update() PUTs the tenant to /tenant/:id", () => {
    const body = { name: "Acme" };
    tenant.update(state, "acme", body);
    expect_api_call(PUT, {
      url: "/tenant/acme",
      body,
      state,
      signal: state.api.tenant.update,
    });
  });

  it("delete() DELETEs /tenant/:id", () => {
    tenant.delete(state, "acme");
    expect_api_call(DELETE, {
      url: "/tenant/acme",
      body: {},
      state,
      signal: state.api.tenant.delete,
    });
  });

  it("by_member() defaults to demo when no user given", () => {
    tenant.by_member(state);
    expect_api_call(GET, {
      url: "/tenant?userMember=demo&maxResults=50&firstResult=0",
      state,
      signal: state.api.tenant.by_member,
    });
  });

  it("by_member() uses the given user", () => {
    tenant.by_member(state, "alice");
    expect_api_call(GET, {
      url: "/tenant?userMember=alice&maxResults=50&firstResult=0",
      state,
      signal: state.api.tenant.by_member,
    });
  });

  it("user_members() GETs /user filtered by memberOfTenant", () => {
    tenant.user_members(state, "acme");
    expect_api_call(GET, {
      url: "/user?memberOfTenant=acme&firstResult=0&maxResults=50",
      state,
      signal: state.api.tenant.user_members,
    });
  });

  it("group_members() GETs /group filtered by memberOfTenant", () => {
    tenant.group_members(state, "acme");
    expect_api_call(GET, {
      url: "/group?memberOfTenant=acme&firstResult=0&maxResults=50",
      state,
      signal: state.api.tenant.group_members,
    });
  });

  it("add_user() PUTs to /tenant/:id/user-members/:user with body", () => {
    tenant.add_user(state, "acme", "alice");
    expect_api_call(PUT, {
      url: "/tenant/acme/user-members/alice",
      body: { id: "acme", userId: "alice" },
      state,
      signal: state.api.tenant.add_user,
    });
  });

  it("remove_user() DELETEs /tenant/:id/user-members/:user with body", () => {
    tenant.remove_user(state, "acme", "alice");
    expect_api_call(DELETE, {
      url: "/tenant/acme/user-members/alice",
      body: { id: "acme", userId: "alice" },
      state,
      signal: state.api.tenant.remove_user,
    });
  });

  it("add_group() PUTs to /tenant/:id/group-members/:group with body", () => {
    tenant.add_group(state, "acme", "admins");
    expect_api_call(PUT, {
      url: "/tenant/acme/group-members/admins",
      body: { id: "acme", groupId: "admins" },
      state,
      signal: state.api.tenant.add_group,
    });
  });

  it("remove_group() DELETEs /tenant/:id/group-members/:group with body", () => {
    tenant.remove_group(state, "acme", "admins");
    expect_api_call(DELETE, {
      url: "/tenant/acme/group-members/admins",
      body: { id: "acme", groupId: "admins" },
      state,
      signal: state.api.tenant.remove_group,
    });
  });
});
