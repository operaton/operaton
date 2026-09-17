import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup, fireEvent } from "@testing-library/preact";

// Spy all engine_rest API functions but keep RequestState/RESPONSE_STATE real.
vi.mock("../api/engine_rest.jsx", async (importOriginal) => {
  const actual = await importOriginal();
  const spyify = (o) =>
    Object.fromEntries(
      Object.entries(o).map(([k, v]) => [
        k,
        typeof v === "function"
          ? vi.fn()
          : v && typeof v === "object"
            ? spyify(v)
            : v,
      ]),
    );
  return { ...actual, default: spyify(actual.default) };
});

let mockParams = {};
const routeFn = vi.fn();
vi.mock("preact-iso", () => ({
  useRoute: () => ({ params: mockParams }),
  useLocation: () => ({ route: routeFn, path: "/admin" }),
}));

import { RESPONSE_STATE } from "../api/helper.jsx";
import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { AdminPage } from "./Admin.jsx";
import { create_mock_state, signal_response } from "../test/helpers.js";

const renderPage = (state) =>
  render(h(AppState.Provider, { value: state }, h(AdminPage, {})));

// Most create/delete handlers chain `.then(...)`, so the mocked fns must resolve.
const resolve_all = () => {
  const walk = (o) => {
    for (const v of Object.values(o)) {
      if (typeof v === "function" && v.mockResolvedValue)
        v.mockResolvedValue(undefined);
      else if (v && typeof v === "object") walk(v);
    }
  };
  walk(engine_rest);
};

describe("AdminPage", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockParams = {};
    routeFn.mockClear();
    resolve_all();
    // The permission probes answer "allowed" unless a test says otherwise.
    engine_rest.authorization.may.mockResolvedValue(true);
  });
  afterEach(cleanup);

  describe("landing / navigation", () => {
    it("redirects to the users sub-page when no page_id is set", () => {
      mockParams = {};
      renderPage(state);
      expect(routeFn).toHaveBeenCalledWith("/admin/users");
    });

    it("renders the admin sub-navigation links", () => {
      mockParams = { page_id: "users" };
      const { container } = renderPage(state);
      const nav = container.querySelector(".admin-page > nav");
      const href = (text) =>
        Array.from(nav.querySelectorAll("a"))
          .find((a) => a.textContent === text)
          ?.getAttribute("href");
      expect(href("admin.users")).toBe("/admin/users");
      expect(href("admin.groups")).toBe("/admin/groups");
      expect(href("admin.tenants")).toBe("/admin/tenants");
      expect(href("admin.authorizations")).toBe("/admin/authorizations");
      expect(href("admin.system")).toBe("/admin/system");
    });
  });

  describe("navigation follows the permissions", () => {
    it("asks the engine which sections the user may reach", () => {
      mockParams = { page_id: "users" };
      renderPage(state);
      expect(engine_rest.authorization.sections).toHaveBeenCalled();
      expect(engine_rest.authorization.sections.mock.lastCall[0]).toBe(state);
    });

    it("hides a section the user has no permission for", () => {
      mockParams = { page_id: "users" };
      state.api.authorization.sections.value = {
        users: true,
        groups: true,
        tenants: true,
        authorizations: false,
        system: false,
      };
      const { queryByText, getAllByText } = renderPage(state);
      expect(queryByText("admin.authorizations")).toBeNull();
      expect(queryByText("admin.system")).toBeNull();
      expect(getAllByText("admin.users").length).toBeGreaterThan(0);
    });

    it("shows every section until the answers arrive", () => {
      mockParams = { page_id: "users" };
      const { getByText } = renderPage(state);
      expect(getByText("admin.system")).toBeTruthy();
      expect(getByText("admin.authorizations")).toBeTruthy();
    });
  });

  describe("Users", () => {
    it("fetches the user list on mount", () => {
      mockParams = { page_id: "users" };
      renderPage(state);
      expect(engine_rest.user.all).toHaveBeenCalled();
      expect(engine_rest.user.all.mock.lastCall[0]).toBe(state);
    });

    it("renders the users from the signal with detail links", () => {
      mockParams = { page_id: "users" };
      signal_response(state.api.user.list, [
        {
          id: "jdoe",
          firstName: "Jane",
          lastName: "Doe",
          email: "jane@example.com",
        },
      ]);
      const { getByText } = renderPage(state);
      expect(getByText("jdoe").getAttribute("href")).toBe("/admin/users/jdoe");
      expect(getByText("Jane")).toBeTruthy();
      expect(getByText("jane@example.com")).toBeTruthy();
    });

    it("submits the create-user form via engine_rest.user.create", () => {
      mockParams = { page_id: "users", selection_id: "create" };
      // create signal must report SUCCESS for the post-submit has_data branch.
      signal_response(state.api.user.create, { id: "newbie" });
      const { container } = renderPage(state);

      const set = (sel, value) =>
        fireEvent.input(container.querySelector(sel), { target: { value } });
      set("#user-id", "newbie");
      set("#password1", "secret");
      set("#password2", "secret");
      set("#first-name", "New");
      set("#last-name", "Bie");
      set("#email", "new@example.com");

      fireEvent.submit(container.querySelector("form"));

      expect(engine_rest.user.create).toHaveBeenCalled();
      const call = engine_rest.user.create.mock.lastCall;
      expect(call[0]).toBe(state);
      expect(call[1].profile.id).toBe("newbie");
      expect(call[1].credentials.password).toBe("secret");
    });

    it("does not submit when the password fields do not match", () => {
      mockParams = { page_id: "users", selection_id: "create" };
      const { container, getByText } = renderPage(state);

      const set = (sel, value) =>
        fireEvent.input(container.querySelector(sel), { target: { value } });
      set("#user-id", "newbie");
      set("#password1", "secret");
      set("#password2", "different");

      fireEvent.submit(container.querySelector("form"));

      expect(engine_rest.user.create).not.toHaveBeenCalled();
      expect(getByText("admin.user.password-mismatch")).toBeTruthy();
    });

    it("confirms a password change with the signed-in user's own password", () => {
      mockParams = { page_id: "users", selection_id: "jdoe" };
      engine_rest.user.credentials_update.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
      });
      const { container } = renderPage(state);

      const set = (sel, value) =>
        fireEvent.input(container.querySelector(sel), { target: { value } });
      set("#new-password", "fresh");
      set("#new-password-repeat", "fresh");
      set("#own-password", "mine");
      fireEvent.submit(container.querySelector("#new-password").form);

      // The engine refuses the request outright without it.
      const call = engine_rest.user.credentials_update.mock.lastCall;
      expect(call[2]).toEqual({
        password: "fresh",
        authenticatedUserPassword: "mine",
      });
    });

    it("hides the create link from someone who may not create users", async () => {
      mockParams = { page_id: "users" };
      engine_rest.authorization.may.mockResolvedValue(false);
      const { queryByText } = renderPage(state);

      await vi.waitFor(() =>
        expect(queryByText("admin.user.create")).toBeNull(),
      );
    });

    it("fetches profile, groups and tenants on the user details page", () => {
      mockParams = { page_id: "users", selection_id: "jdoe" };
      renderPage(state);
      expect(engine_rest.user.profile.get).toHaveBeenCalled();
      expect(engine_rest.group.by_member).toHaveBeenCalled();
      expect(engine_rest.tenant.by_member).toHaveBeenCalled();
      expect(engine_rest.user.profile.get.mock.lastCall[1]).toBe("jdoe");
    });

    it("renders the user profile form from the profile signal", () => {
      mockParams = { page_id: "users", selection_id: "jdoe" };
      signal_response(state.api.user.profile, {
        firstName: "Jane",
        lastName: "Doe",
        email: "jane@example.com",
      });
      const { container } = renderPage(state);
      expect(container.querySelector("#first-name").value).toBe("Jane");
      expect(container.querySelector("#email").value).toBe("jane@example.com");
    });

    it("deletes the user via engine_rest.user.delete and routes back", () => {
      mockParams = { page_id: "users", selection_id: "jdoe" };
      const { getByText } = renderPage(state);
      // open the danger-zone confirm dialog, then confirm
      fireEvent.click(getByText("admin.user.delete"));
      fireEvent.click(getByText("common.delete"));
      expect(engine_rest.user.delete).toHaveBeenCalled();
      expect(engine_rest.user.delete.mock.lastCall[0]).toBe(state);
      expect(engine_rest.user.delete.mock.lastCall[1]).toBe("jdoe");
    });

    it("unlocks a user that the engine locked out", () => {
      mockParams = { page_id: "users", selection_id: "alice" };
      const { getAllByText } = renderPage(state);
      // [0] is the section heading, [1] the button
      fireEvent.click(getAllByText("admin.user.unlock")[1]);
      expect(engine_rest.user.unlock).toHaveBeenCalled();
      expect(engine_rest.user.unlock.mock.lastCall[1]).toBe("alice");
    });
  });

  describe("searching and paging the lists", () => {
    it("runs the typed search against the engine", () => {
      mockParams = { page_id: "users" };
      const { container, getByText } = renderPage(state);
      const inputs = container.querySelectorAll(".identity-search input");
      fireEvent.input(inputs[1], { target: { value: "Al" } });
      fireEvent.click(getByText("common.search"));
      expect(engine_rest.user.all.mock.lastCall[1]).toEqual({
        firstNameLike: "Al",
      });
    });

    it("leaves an empty field out of the query", () => {
      mockParams = { page_id: "users" };
      const { container, getByText } = renderPage(state);
      const inputs = container.querySelectorAll(".identity-search input");
      fireEvent.input(inputs[0], { target: { value: "x" } });
      fireEvent.input(inputs[0], { target: { value: "" } });
      fireEvent.click(getByText("common.search"));
      expect(engine_rest.user.all.mock.lastCall[1]).toEqual({});
    });

    it("offers the next page once a full page is on screen", () => {
      mockParams = { page_id: "users" };
      signal_response(
        state.api.user.list,
        Array.from({ length: 50 }, (_, i) => ({ id: `u${i}` })),
      );
      const { getByText } = renderPage(state);
      fireEvent.click(getByText("common.load-more"));
      const [, query, append] = engine_rest.user.all.mock.lastCall;
      expect(query.firstResult).toBe(50);
      expect(append).toBe(true);
    });

    it("does not offer the next page on a partial page", () => {
      mockParams = { page_id: "users" };
      signal_response(state.api.user.list, [{ id: "alice" }]);
      const { queryByText } = renderPage(state);
      expect(queryByText("common.load-more")).toBeNull();
    });
  });

  describe("Groups", () => {
    it("fetches the group list on mount", () => {
      mockParams = { page_id: "groups" };
      renderPage(state);
      expect(engine_rest.group.all).toHaveBeenCalled();
    });

    it("renders the groups from the signal with detail links", () => {
      mockParams = { page_id: "groups" };
      signal_response(state.api.group.list, [
        { id: "g1", name: "Admins", type: "WORKFLOW" },
      ]);
      const { getByText } = renderPage(state);
      expect(getByText("g1").getAttribute("href")).toBe("/admin/groups/g1");
      expect(getByText("Admins")).toBeTruthy();
    });

    it("submits the create-group form via engine_rest.group.create", () => {
      mockParams = { page_id: "groups", selection_id: "create" };
      signal_response(state.api.group.create, { id: "g2" });
      const { container } = renderPage(state);

      const set = (sel, value) =>
        fireEvent.input(container.querySelector(sel), { target: { value } });
      set("#group-id", "g2");
      set("#group-name", "Reviewers");
      set("#group-type", "WORKFLOW");

      fireEvent.submit(container.querySelector("form"));

      expect(engine_rest.group.create).toHaveBeenCalled();
      const call = engine_rest.group.create.mock.lastCall;
      expect(call[0]).toBe(state);
      expect(call[1]).toEqual({
        id: "g2",
        name: "Reviewers",
        type: "WORKFLOW",
      });
    });

    it("deletes a group from the list via the confirm dialog", () => {
      mockParams = { page_id: "groups" };
      signal_response(state.api.group.list, [
        { id: "g1", name: "Admins", type: "WORKFLOW" },
      ]);
      const { getAllByText } = renderPage(state);
      // [0] = row delete button (opens dialog); [1] = confirm dialog danger button
      fireEvent.click(getAllByText("common.delete")[0]);
      fireEvent.click(getAllByText("common.delete")[1]);
      expect(engine_rest.group.delete).toHaveBeenCalled();
      expect(engine_rest.group.delete.mock.lastCall[1]).toBe("g1");
    });

    it("fetches group + members on the group details page and renders the form", () => {
      mockParams = { page_id: "groups", selection_id: "g1" };
      signal_response(state.api.group.list, [
        { id: "g1", name: "Admins", type: "WORKFLOW" },
      ]);
      const { container } = renderPage(state);
      expect(engine_rest.group.all).toHaveBeenCalled();
      expect(engine_rest.group.members).toHaveBeenCalled();
      expect(container.querySelector("#group-name").value).toBe("Admins");
    });

    it("adds a member from the group details page", () => {
      mockParams = { page_id: "groups", selection_id: "g1" };
      signal_response(state.api.group.list, [{ id: "g1", name: "Admins" }]);
      const { container, getAllByText } = renderPage(state);

      fireEvent.click(getAllByText("admin.group.add-member")[0]);
      fireEvent.input(container.querySelector("#member-id"), {
        target: { value: "alice" },
      });
      fireEvent.submit(container.querySelector("#member-id").closest("form"));

      expect(engine_rest.group.add_user).toHaveBeenCalled();
      const call = engine_rest.group.add_user.mock.lastCall;
      expect(call[1]).toBe("g1");
      expect(call[2]).toBe("alice");
    });
  });

  describe("suggestions when adding a member", () => {
    it("offers the users that are not members of the group yet", () => {
      mockParams = { page_id: "groups", selection_id: "admins" };
      signal_response(state.api.group.members, [
        { id: "alice", name: "Alice" },
      ]);
      signal_response(state.api.user.list, [
        { id: "alice", firstName: "Alice" },
        { id: "bob", firstName: "Bob" },
      ]);
      const { container } = renderPage(state);
      const suggested = [
        ...container.querySelectorAll("#member-candidates option"),
      ].map((option) => option.value);
      expect(suggested).toEqual(["bob"]);
    });

    it("fetches the users it suggests", () => {
      mockParams = { page_id: "groups", selection_id: "admins" };
      renderPage(state);
      expect(engine_rest.user.all).toHaveBeenCalled();
    });
  });

  describe("Tenants", () => {
    it("fetches the tenant list on mount", () => {
      mockParams = { page_id: "tenants" };
      renderPage(state);
      expect(engine_rest.tenant.all).toHaveBeenCalled();
    });

    it("renders the tenants from the signal with detail links", () => {
      mockParams = { page_id: "tenants" };
      signal_response(state.api.tenant.list, [{ id: "t1", name: "Acme" }]);
      const { getByText } = renderPage(state);
      expect(getByText("t1").getAttribute("href")).toBe("/admin/tenants/t1");
      expect(getByText("Acme")).toBeTruthy();
    });

    it("submits the create-tenant form via engine_rest.tenant.create", () => {
      mockParams = { page_id: "tenants", selection_id: "create" };
      signal_response(state.api.tenant.create, { id: "t2" });
      const { container } = renderPage(state);

      fireEvent.input(container.querySelector("#tenant-id"), {
        target: { value: "t2" },
      });
      fireEvent.input(container.querySelector("#tenant-name"), {
        target: { value: "Globex" },
      });
      fireEvent.submit(container.querySelector("form"));

      expect(engine_rest.tenant.create).toHaveBeenCalled();
      const call = engine_rest.tenant.create.mock.lastCall;
      expect(call[0]).toBe(state);
      expect(call[1]).toEqual({ id: "t2", name: "Globex" });
    });

    it("fetches tenant + members on the tenant details page and renders the form", () => {
      mockParams = { page_id: "tenants", selection_id: "t1" };
      signal_response(state.api.tenant.list, [{ id: "t1", name: "Acme" }]);
      const { container } = renderPage(state);
      expect(engine_rest.tenant.all).toHaveBeenCalled();
      expect(engine_rest.tenant.user_members).toHaveBeenCalled();
      expect(engine_rest.tenant.group_members).toHaveBeenCalled();
      expect(container.querySelector("#tenant-name").value).toBe("Acme");
    });

    it("adds a user member from the tenant details page", () => {
      mockParams = { page_id: "tenants", selection_id: "t1" };
      signal_response(state.api.tenant.list, [{ id: "t1", name: "Acme" }]);
      const { container, getAllByText } = renderPage(state);

      fireEvent.click(getAllByText("admin.tenant.add-user")[0]);
      const input = container.querySelector("#member-id");
      fireEvent.input(input, { target: { value: "bob" } });
      fireEvent.submit(input.closest("form"));

      expect(engine_rest.tenant.add_user).toHaveBeenCalled();
      const call = engine_rest.tenant.add_user.mock.lastCall;
      expect(call[1]).toBe("t1");
      expect(call[2]).toBe("bob");
    });

    it("deletes the tenant via the danger-zone confirm dialog", () => {
      mockParams = { page_id: "tenants", selection_id: "t1" };
      signal_response(state.api.tenant.list, [{ id: "t1", name: "Acme" }]);
      const { getByText } = renderPage(state);
      fireEvent.click(getByText("admin.tenant.delete"));
      fireEvent.click(getByText("common.delete"));
      expect(engine_rest.tenant.delete).toHaveBeenCalled();
      expect(engine_rest.tenant.delete.mock.lastCall[1]).toBe("t1");
    });
  });

  describe("Authorizations", () => {
    it("renders the resource-type link list on the landing view", () => {
      mockParams = { page_id: "authorizations" };
      const { getByText } = renderPage(state);
      const link = getByText("admin.authorization-resources.user");
      // user resource_type is 1
      expect(link.getAttribute("href")).toBe(
        "/admin/authorizations/resource-type/1",
      );
      // no fetch until a resource type is selected
      expect(engine_rest.authorization.all).not.toHaveBeenCalled();
    });

    it("fetches authorizations when a resource type is selected", () => {
      mockParams = {
        page_id: "authorizations",
        selection_id: "resource-type",
        sub_selection_id: "1",
      };
      renderPage(state);
      expect(engine_rest.authorization.all).toHaveBeenCalled();
      expect(engine_rest.authorization.all.mock.lastCall[0]).toBe(state);
      expect(engine_rest.authorization.all.mock.lastCall[1]).toBe("1");
    });

    it("renders authorization rows from the signal", () => {
      mockParams = {
        page_id: "authorizations",
        selection_id: "resource-type",
        sub_selection_id: "1",
      };
      signal_response(state.api.authorization.all, [
        {
          id: "a1",
          type: 1,
          userId: "alice",
          permissions: ["READ"],
          resourceType: 1,
          resourceId: "*",
        },
      ]);
      const { getByText } = renderPage(state);
      expect(getByText("alice")).toBeTruthy();
      expect(getByText("READ")).toBeTruthy();
    });

    it("creates an authorization via engine_rest.authorization.create", () => {
      mockParams = {
        page_id: "authorizations",
        selection_id: "resource-type",
        sub_selection_id: "1",
      };
      signal_response(state.api.authorization.create, { id: "a2" });
      const { container, getByText } = renderPage(state);

      fireEvent.click(getByText("admin.authorization.create"));
      fireEvent.input(container.querySelector("#auth-user"), {
        target: { value: "reviewers" },
      });
      fireEvent.submit(container.querySelector("form.authorization-create"));

      expect(engine_rest.authorization.create).toHaveBeenCalled();
      const call = engine_rest.authorization.create.mock.lastCall;
      expect(call[0]).toBe(state);
      // A group by default: the engine accepts a group id in userId without
      // complaint, and the permission then matches nobody.
      expect(call[1].groupId).toBe("reviewers");
      expect(call[1].userId).toBeUndefined();
      expect(call[1].resourceType).toBe(1);
    });

    it("points at the row in the way instead of letting the engine refuse", () => {
      mockParams = {
        page_id: "authorizations",
        selection_id: "resource-type",
        sub_selection_id: "1",
      };
      signal_response(state.api.authorization.all, [
        {
          id: "a1",
          type: 1,
          groupId: "reviewers",
          permissions: ["READ", "UPDATE"],
          resourceType: 1,
          resourceId: "*",
        },
      ]);
      const { container, getByText } = renderPage(state);

      fireEvent.click(getByText("admin.authorization.create"));
      fireEvent.input(container.querySelector("#auth-user"), {
        target: { value: "reviewers" },
      });
      fireEvent.submit(container.querySelector("form.authorization-create"));

      expect(getByText("admin.authorization.already-exists")).toBeTruthy();
      expect(engine_rest.authorization.create).not.toHaveBeenCalled();
    });

    it("keeps the permissions it already had when one is added", () => {
      mockParams = {
        page_id: "authorizations",
        selection_id: "resource-type",
        sub_selection_id: "1",
      };
      signal_response(state.api.authorization.all, [
        {
          id: "a1",
          type: 1,
          groupId: "reviewers",
          permissions: ["READ", "UPDATE"],
          resourceType: 1,
          resourceId: "*",
        },
      ]);
      const { container, getByText } = renderPage(state);

      fireEvent.click(getByText("common.edit"));
      const create_box = Array.from(
        container.querySelectorAll('input[type="checkbox"]'),
      ).find((box) => box.value === "CREATE");
      fireEvent.input(create_box, { target: { checked: true } });
      fireEvent.submit(container.querySelector("form"));

      const call = engine_rest.authorization.update.mock.lastCall;
      expect(call[2].permissions).toEqual(["READ", "UPDATE", "CREATE"]);
    });

    it("grants to a user when the holder is switched to one", () => {
      mockParams = {
        page_id: "authorizations",
        selection_id: "resource-type",
        sub_selection_id: "1",
      };
      signal_response(state.api.authorization.create, { id: "a3" });
      const { container, getByText } = renderPage(state);

      fireEvent.click(getByText("admin.authorization.create"));
      fireEvent.input(container.querySelector("#auth-identity-type"), {
        target: { value: "user" },
      });
      fireEvent.input(container.querySelector("#auth-user"), {
        target: { value: "carol" },
      });
      fireEvent.submit(container.querySelector("form.authorization-create"));

      const call = engine_rest.authorization.create.mock.lastCall;
      expect(call[1].userId).toBe("carol");
      expect(call[1].groupId).toBeUndefined();
    });

    // Ported from the previous admin's authorizations-spec.js:
    // "can change user and group".
    it("turns a user grant into a group grant", () => {
      mockParams = {
        page_id: "authorizations",
        selection_id: "resource-type",
        sub_selection_id: "1",
      };
      signal_response(state.api.authorization.all, [
        {
          id: "a1",
          type: 1,
          userId: "alice",
          groupId: null,
          permissions: ["READ"],
          resourceType: 1,
          resourceId: "*",
        },
      ]);
      const { getByText, getByLabelText } = renderPage(state);

      fireEvent.click(getByText("common.edit"));
      fireEvent.input(getByLabelText("admin.authorization.identity-type"), {
        target: { value: "group" },
      });
      fireEvent.click(getByText("common.save"));

      const call = engine_rest.authorization.update.mock.lastCall;
      expect(call[2].groupId).toBe("alice");
      expect(call[2].userId).toBeNull();
    });

    it("forgets an abandoned edit when the row is cancelled", () => {
      mockParams = {
        page_id: "authorizations",
        selection_id: "resource-type",
        sub_selection_id: "1",
      };
      signal_response(state.api.authorization.all, [
        {
          id: "a1",
          type: 1,
          userId: "alice",
          permissions: ["READ"],
          resourceType: 1,
          resourceId: "*",
        },
      ]);
      const { container, getByText, getByLabelText } = renderPage(state);

      fireEvent.click(getByText("common.edit"));
      fireEvent.input(getByLabelText("admin.authorization.resource-id"), {
        target: { value: "changed" },
      });
      fireEvent.click(getByText("common.cancel"));
      fireEvent.click(getByText("common.edit"));

      expect(getByLabelText("admin.authorization.resource-id").value).toBe("*");
      expect(container.textContent).not.toContain("changed");
    });

    it("deletes an authorization row via the confirm dialog", () => {
      mockParams = {
        page_id: "authorizations",
        selection_id: "resource-type",
        sub_selection_id: "1",
      };
      signal_response(state.api.authorization.all, [
        {
          id: "a1",
          type: 1,
          userId: "alice",
          permissions: ["READ"],
          resourceType: 1,
          resourceId: "*",
        },
      ]);
      const { getAllByText } = renderPage(state);
      // [0] = row delete button (opens dialog); [1] = confirm dialog danger button
      fireEvent.click(getAllByText("common.delete")[0]);
      fireEvent.click(getAllByText("common.delete")[1]);
      expect(engine_rest.authorization.delete).toHaveBeenCalled();
      expect(engine_rest.authorization.delete.mock.lastCall[1]).toBe("a1");
    });
  });

  describe("System / telemetry", () => {
    it("fetches telemetry data on mount", () => {
      mockParams = { page_id: "system" };
      renderPage(state);
      expect(engine_rest.engine.telemetry).toHaveBeenCalled();
      expect(engine_rest.engine.telemetry.mock.lastCall[0]).toBe(state);
    });

    it("names the product, the database and the JDK", () => {
      mockParams = { page_id: "system" };
      signal_response(state.api.engine.telemetry, {
        installation: "abc-123",
        product: {
          name: "Operaton",
          version: "2.2.0",
          edition: "community",
          internals: {
            database: { vendor: "PostgreSQL", version: "16.2" },
            jdk: { vendor: "Eclipse Adoptium", version: "21.0.2" },
            webapps: ["cockpit", "admin"],
          },
        },
      });
      const { getByText, container } = renderPage(state);
      expect(getByText("Operaton")).toBeTruthy();
      expect(getByText("PostgreSQL 16.2")).toBeTruthy();
      expect(getByText("Eclipse Adoptium 21.0.2")).toBeTruthy();
      expect(getByText("cockpit, admin")).toBeTruthy();
      expect(container.textContent).toContain("abc-123");
    });

    it("shows a dash where the engine reported nothing", () => {
      mockParams = { page_id: "system" };
      signal_response(state.api.engine.telemetry, {
        product: { name: "Operaton" },
      });
      const { container } = renderPage(state);
      expect(container.querySelectorAll("td")[1].textContent).toBe("—");
    });

    it("keeps the raw data reachable", () => {
      mockParams = { page_id: "system" };
      signal_response(state.api.engine.telemetry, { installation: "abc-123" });
      const { container } = renderPage(state);
      const raw = container.querySelector("details pre");
      expect(raw.textContent).toContain("abc-123");
    });
  });
});
