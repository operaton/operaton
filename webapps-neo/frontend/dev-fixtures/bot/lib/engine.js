// Thin wrapper over Operaton's REST API.
// All paths are relative to engine.url; auth is Basic when configured.

import { readFile } from 'node:fs/promises'
import { basename } from 'node:path'

export const make_engine_client = (config) => {
  const { url, auth } = config.engine
  const headers = () => {
    const h = { 'Content-Type': 'application/json' }
    if (auth?.username) {
      h.Authorization =
        'Basic ' +
        Buffer.from(`${auth.username}:${auth.password ?? ''}`).toString('base64')
    }
    return h
  }

  const request = async (method, path, body) => {
    const res = await fetch(`${url}${path}`, {
      method,
      headers: headers(),
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
    if (!res.ok) {
      const text = await res.text().catch(() => '')
      throw Object.assign(
        new Error(`${method} ${path} → ${res.status}: ${text}`),
        { status: res.status, body: text },
      )
    }
    if (res.status === 204) return null
    const ct = res.headers.get('content-type') ?? ''
    return ct.includes('application/json') ? res.json() : res.text()
  }

  return {
    config,

    get: (path) => request('GET', path),
    post: (path, body) => request('POST', path, body),
    put: (path, body) => request('PUT', path, body),
    del: (path, body) => request('DELETE', path, body),

    // Multipart deployment
    deploy: async ({ name, files, source = 'dev-fixtures', deployChangedOnly = true }) => {
      const fd = new FormData()
      fd.append('deployment-name', name)
      fd.append('deployment-source', source)
      fd.append('enable-duplicate-filtering', 'true')
      fd.append('deploy-changed-only', deployChangedOnly ? 'true' : 'false')
      for (const filePath of files) {
        const buf = await readFile(filePath)
        fd.append(basename(filePath), new Blob([buf]), basename(filePath))
      }
      const headers_no_ct = { ...headers() }
      delete headers_no_ct['Content-Type']
      const res = await fetch(`${url}/deployment/create`, {
        method: 'POST',
        headers: headers_no_ct,
        body: fd,
      })
      if (!res.ok) {
        const text = await res.text().catch(() => '')
        throw new Error(`deployment failed → ${res.status}: ${text}`)
      }
      return res.json()
    },
  }
}
