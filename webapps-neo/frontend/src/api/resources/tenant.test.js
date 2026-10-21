import { describe, it, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  encode_id: (id) => encodeURIComponent(id ?? ""),
  GET_LIST: vi.fn(),
  PAGE_SIZE: 50,
  GET: vi.fn(),
  POST: vi.fn(),
  PUT: vi.fn(),
  DELETE: vi.fn(),
  resolve_user: (state, user_name) => user_name ?? state.auth.user.id.value,
}));

import { GET, POST, PUT, DELETE, GET_LIST } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import tenant from "./tenant.js";

describe("api/resources/tenant", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("all() asks for the first page, sorted by id", () => {
    tenant.all(state);
    expect(GET_LIST).toHaveBeenCalled();
    const [url, , signal] = GET_LIST.mock.lastCall;
    expect(url).toBe(
      "/tenant?firstResult=0&maxResults=50&sortBy=id&sortOrder=asc",
    );
    expect(signal).toBe(state.api.tenant.list);
  });

  it("all() puts a search query into the request", () => {
    tenant.all(state, { nameLike: "Baden%" });
    expect(GET_LIST.mock.lastCall[0]).toContain("nameLike=Baden%25");
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

  it("by_member() defaults to the signed-in user when no user given", () => {
    state.auth.user.id.value = "carol";
    tenant.by_member(state);
    expect_api_call(GET, {
      url: "/tenant?userMember=carol&maxResults=50&firstResult=0",
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

  it("escapes a slash in the tenant id", () => {
    tenant.delete(state, "de/bw");
    expect_api_call(DELETE, {
      url: "/tenant/de%2Fbw",
      body: {},
      state,
      signal: state.api.tenant.delete,
    });
  });
});
