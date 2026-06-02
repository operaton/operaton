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
      const nav = container.querySelector("#admin-page > nav");
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
      mockParams = { page_id: "users" };
      // create signal must report SUCCESS for the post-submit has_data branch.
      signal_response(state.api.user.create, { id: "newbie" });
      const { container, getAllByText } = renderPage(state);

      fireEvent.click(getAllByText("admin.user.create")[0]);

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
      mockParams = { page_id: "users" };
      const { container, getAllByText, getByText } = renderPage(state);
      fireEvent.click(getAllByText("admin.user.create")[0]);

      const set = (sel, value) =>
        fireEvent.input(container.querySelector(sel), { target: { value } });
      set("#user-id", "newbie");
      set("#password1", "secret");
      set("#password2", "different");

      fireEvent.submit(container.querySelector("form"));

      expect(engine_rest.user.create).not.toHaveBeenCalled();
      expect(getByText("admin.user.password-mismatch")).toBeTruthy();
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
      mockParams = { page_id: "groups" };
      signal_response(state.api.group.create, { id: "g2" });
      const { container, getAllByText } = renderPage(state);

      fireEvent.click(getAllByText("admin.group.create")[0]);
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
      mockParams = { page_id: "tenants" };
      signal_response(state.api.tenant.create, { id: "t2" });
      const { container, getAllByText } = renderPage(state);

      fireEvent.click(getAllByText("admin.tenant.create")[0]);
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
        target: { value: "carol" },
      });
      fireEvent.submit(container.querySelector("form.authorization-create"));

      expect(engine_rest.authorization.create).toHaveBeenCalled();
      const call = engine_rest.authorization.create.mock.lastCall;
      expect(call[0]).toBe(state);
      expect(call[1].userId).toBe("carol");
      expect(call[1].resourceType).toBe(1);
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

    it("renders the telemetry data from the signal", () => {
      mockParams = { page_id: "system" };
      signal_response(state.api.engine.telemetry, {
        installation: "abc-123",
        product: { name: "Operaton" },
      });
      const { container } = renderPage(state);
      const pre = container.querySelector("pre");
      expect(pre).toBeTruthy();
      expect(pre.textContent).toContain("abc-123");
      expect(pre.textContent).toContain("Operaton");
    });
  });
});
