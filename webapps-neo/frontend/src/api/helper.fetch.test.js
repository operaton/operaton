import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { signal } from "@preact/signals";
import {
  GET,
  POST,
  PUT,
  DELETE,
  PAGINATED_GET,
  GET_TEXT,
  GET_SERVER_URL,
  RESPONSE_STATE,
} from "./helper.jsx";
import { create_mock_state } from "../test/helpers.js";

const ok_json = (data, status = 200) => ({
  ok: true,
  status,
  json: async () => data,
  text: async () => (typeof data === "string" ? data : JSON.stringify(data)),
});

const not_ok = (status = 500, body = { message: "nope" }) => ({
  ok: false,
  status,
  statusText: "Server Error",
  json: async () => body,
});

describe("api/helper fetch wrappers", () => {
  let state, sig, fetchMock;

  beforeEach(() => {
    state = create_mock_state();
    sig = signal(null);
    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe("GET", () => {
    it("requests the engine-rest URL with an Authorization header and stores SUCCESS", async () => {
      fetchMock.mockResolvedValue(ok_json([{ id: "x" }]));
      await GET("/process-definition", state, sig);

      const [url, opts] = fetchMock.mock.calls[0];
      expect(url).toBe("http://localhost:8080/engine-rest/process-definition");
      expect(opts.headers.get("Authorization")).toMatch(/^Basic /);
      expect(sig.value).toEqual({
        status: RESPONSE_STATE.SUCCESS,
        data: [{ id: "x" }],
      });
    });

    it("stores ERROR with the rejected response on a non-ok status", async () => {
      const response = not_ok(404);
      fetchMock.mockResolvedValue(response);
      await GET("/missing", state, sig);

      expect(sig.value.status).toBe(RESPONSE_STATE.ERROR);
      expect(sig.value.error).toBe(response);
    });
  });

  describe("POST / PUT / DELETE (fetch_with_body)", () => {
    it("POST sends JSON body and method, stores parsed data", async () => {
      fetchMock.mockResolvedValue(ok_json({ id: "new" }));
      await POST("/user/create", { id: "bob" }, state, sig);

      const [url, opts] = fetchMock.mock.calls[0];
      expect(url).toBe("http://localhost:8080/engine-rest/user/create");
      expect(opts.method).toBe("POST");
      expect(opts.body).toBe(JSON.stringify({ id: "bob" }));
      expect(opts.headers.get("Content-Type")).toBe("application/json");
      expect(sig.value).toEqual({
        status: RESPONSE_STATE.SUCCESS,
        data: { id: "new" },
      });
    });

    it("treats a 204 response as 'No Content'", async () => {
      fetchMock.mockResolvedValue(ok_json(null, 204));
      await PUT("/user/bob/profile", {}, state, sig);
      expect(sig.value).toEqual({
        status: RESPONSE_STATE.SUCCESS,
        data: "No Content",
      });
    });

    it("DELETE uses the DELETE method", async () => {
      fetchMock.mockResolvedValue(ok_json(null, 204));
      await DELETE("/user/bob", {}, state, sig);
      expect(fetchMock.mock.calls[0][1].method).toBe("DELETE");
    });

    it("parses the error body into the ERROR state when the response is a Response", async () => {
      // fetch_with_body rejects with the Response, then reads error.json()
      const response = new Response(JSON.stringify({ message: "bad" }), {
        status: 400,
      });
      fetchMock.mockResolvedValue(response);
      await POST("/x", {}, state, sig);

      expect(sig.value.status).toBe(RESPONSE_STATE.ERROR);
      expect(sig.value.error).toEqual({ message: "bad" });
    });
  });

  describe("PAGINATED_GET", () => {
    it("replaces data and sets hasMore=true when a full page is returned", async () => {
      const page = Array.from({ length: 20 }, (_, i) => ({ id: i }));
      fetchMock.mockResolvedValue(ok_json(page));
      await PAGINATED_GET("/history/process-instance", state, sig, 0, 20);

      const [url] = fetchMock.mock.calls[0];
      expect(url).toContain("?firstResult=0&maxResults=20");
      expect(sig.value.status).toBe(RESPONSE_STATE.SUCCESS);
      expect(sig.value.data).toHaveLength(20);
      expect(sig.value.hasMore).toBe(true);
    });

    it("appends and de-dupes when firstResult > 0, and uses & when the url has a query", async () => {
      sig.value = {
        status: RESPONSE_STATE.SUCCESS,
        data: [{ id: 1 }, { id: 2 }],
      };
      fetchMock.mockResolvedValue(ok_json([{ id: 2 }, { id: 3 }]));
      await PAGINATED_GET(
        "/history/process-instance?foo=bar",
        state,
        sig,
        2,
        20,
      );

      expect(fetchMock.mock.calls[0][0]).toContain(
        "?foo=bar&firstResult=2&maxResults=20",
      );
      expect(sig.value.data.map((i) => i.id)).toEqual([1, 2, 3]);
      expect(sig.value.hasMore).toBe(false);
    });
  });

  describe("GET_TEXT", () => {
    it("stores the response text", async () => {
      fetchMock.mockResolvedValue(ok_json("<bpmn/>"));
      await GET_TEXT("/process-definition/x/xml", state, sig);
      expect(sig.value).toEqual({
        status: RESPONSE_STATE.SUCCESS,
        data: "<bpmn/>",
      });
    });
  });

  describe("GET_SERVER_URL", () => {
    it("requests the server root (no /engine-rest) and stores text", async () => {
      fetchMock.mockResolvedValue(ok_json("cookie-page"));
      await GET_SERVER_URL("/operaton/app/", state, sig);
      expect(fetchMock.mock.calls[0][0]).toBe(
        "http://localhost:8080/operaton/app/",
      );
      expect(sig.value).toEqual({
        status: RESPONSE_STATE.SUCCESS,
        data: "cookie-page",
      });
    });
  });
});
