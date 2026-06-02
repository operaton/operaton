import { useContext, useEffect } from 'preact/hooks'
import { useRoute, useLocation } from 'preact-iso'
import { useTranslation } from 'react-i18next'
import { useSignal, useSignalEffect } from '@preact/signals'
import engine_rest, { RequestState } from '../api/engine_rest.jsx'
import { has_data } from '../api/helper.jsx'
import { AppState } from '../state.js'
import { Breadcrumbs } from '../components/Breadcrumbs.jsx'
import { Dialog, ConfirmDialog } from '../components/Dialog.jsx'

const AdminPage = () => {
  const
    { params: { page_id } } = useRoute(),
    { route } = useLocation(),
    [t] = useTranslation()

  useEffect(() => {
    if (page_id === undefined) route('/admin/users')
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page_id])

  const is_selected = (page) => (page_id === page) ? 'selected' : ''

  return <div id="admin-page">
    <nav>
      <ul class="list">
        <li class={is_selected('users')}><a href="/admin/users">{t("admin.users")}</a></li>
        <li class={is_selected('groups')}><a href="/admin/groups">{t("admin.groups")}</a></li>
        <li class={is_selected('tenants')}><a href="/admin/tenants">{t("admin.tenants")}</a></li>
        <li class={is_selected('authorizations')}><a href="/admin/authorizations">{t("admin.authorizations")}</a></li>
        <li class={is_selected('system')}><a href="/admin/system">{t("admin.system")}</a></li>
      </ul>
    </nav>

    {({
      users: <UserPage />,
      groups: <GroupsPage />,
      tenants: <TenantsPage />,
      authorizations: <AuthorizationsPage />,
      system: <SystemPage />,
    })[page_id] ?? <p>{t("common.select-page")}</p>}
  </div>
}

/** Shows the success/error result of an action signal once it has fired. */
const ActionResult = ({ signal, success }) =>
  <RequestState
    signal={signal}
    on_nothing={() => <></>}
    on_success={() => <p class="success">{success}</p>} />

/* ----------------------------------------------------------------- Tenants */

const TenantsPage = () => {
  const { params: { selection_id } } = useRoute()

  return (selection_id === undefined)
    ? <TenantList />
    : <TenantDetails tenant_id={selection_id} />
}

const TenantList = () => {
  const
    state = useContext(AppState),
    { api: { tenant: { list: tenants } } } = state,
    [t] = useTranslation(),
    create_open = useSignal(false)

  useEffect(() => {
    void engine_rest.tenant.all(state)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return <div class="content fade-in">
    <Breadcrumbs paths={[
      { name: t("nav.admin"), route: '/admin' },
      { name: t("admin.tenants") }]} />
    <h2>{t("admin.tenants")}</h2>

    <div class="button-group">
      <button onClick={() => (create_open.value = true)}>{t("admin.tenant.create")}</button>
    </div>
    <TenantCreate open={create_open} />

    <RequestState
      signal={tenants}
      on_success={() => tenants.value.data.length !== 0
        ? <table class="fade-in">
          <thead>
          <tr>
            <th>{t("common.id")}</th>
            <th>{t("common.name")}</th>
          </tr>
          </thead>
          <tbody>
          {tenants.value.data.map((tenant) => (
            <tr key={tenant.id}>
              <td><a href={`/admin/tenants/${tenant.id}`}>{tenant.id}</a></td>
              <td>{tenant.name}</td>
            </tr>
          ))}
          </tbody>
        </table>
        : <p>{t("admin.tenant.no-tenants")}</p>} />
  </div>
}

const TenantCreate = ({ open }) => {
  const
    state = useContext(AppState),
    { api: { tenant: { create: tenant_create } } } = state,
    [t] = useTranslation(),
    form = useSignal({})

  const
    set_value = (k, e) => form.value = { ...form.peek(), [k]: e.currentTarget.value },
    on_submit = e => {
      e.preventDefault()
      void engine_rest.tenant.create(state, form.value).then(() => {
        if (has_data(tenant_create)) {
          engine_rest.tenant.all(state)
          open.value = false
        }
      })
    }

  return <Dialog open={open} title={t("admin.tenant.create-title")}>
    <ActionResult signal={tenant_create} success={t("admin.tenant.success-created")} />
    <form onSubmit={on_submit}>
      <label for="tenant-id">{t("admin.tenant.tenant-id")}</label>
      <input id="tenant-id" type="text" onInput={(e) => set_value('id', e)} required />

      <label for="tenant-name">{t("admin.tenant.tenant-name")}</label>
      <input id="tenant-name" type="text" onInput={(e) => set_value('name', e)} required />

      <div class="button-group">
        <button type="submit">{t("admin.tenant.create")}</button>
        <button type="button" class="secondary" onClick={() => (open.value = false)}>{t("common.cancel")}</button>
      </div>
    </form>
  </Dialog>
}

const TenantDetails = ({ tenant_id }) => {
  const
    state = useContext(AppState),
    { api: { tenant } } = state,
    [t] = useTranslation(),
    delete_open = useSignal(false),
    { route } = useLocation(),
    form = useSignal(null)

  useEffect(() => {
    form.value = null
    void engine_rest.tenant.all(state)
    void engine_rest.tenant.user_members(state, tenant_id)
    void engine_rest.tenant.group_members(state, tenant_id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tenant_id])

  useSignalEffect(() => {
    if (has_data(tenant.list) && form.value === null) {
      const found = tenant.list.value.data.find((tn) => tn.id === tenant_id)
      if (found) form.value = { ...found }
    }
  })

  const
    set_value = (k, e) => form.value = { ...form.peek(), [k]: e.currentTarget.value },
    on_submit = e => {
      e.preventDefault()
      void engine_rest.tenant.update(state, tenant_id, form.value)
    },
    on_delete = () =>
      void engine_rest.tenant.delete(state, tenant_id).then(() => route('/admin/tenants'))

  return <div class="content fade-in">
    <Breadcrumbs paths={[
      { name: t("nav.admin"), route: '/admin' },
      { name: t("admin.tenants"), route: '/admin/tenants' },
      { name: tenant_id }]} />

    <h2>{t("admin.tenant.details")}</h2>

    <h3>{t("admin.tenant.information")}</h3>
    <ActionResult signal={tenant.update} success={t("admin.tenant.success-updated")} />
    {form.value
      ? <form onSubmit={on_submit}>
        <label for="tenant-id">{t("admin.tenant.tenant-id")}</label>
        <input id="tenant-id" type="text" value={form.value.id} disabled />

        <label for="tenant-name">{t("admin.tenant.tenant-name")}</label>
        <input id="tenant-name" type="text" value={form.value.name ?? ''} onInput={(e) => set_value('name', e)} required />

        <div class="button-group">
          <button type="submit">{t("common.save")}</button>
        </div>
      </form>
      : <p>{t("common.loading")}</p>}

    <MemberSection
      title={t("admin.tenant.users")}
      list_signal={tenant.user_members}
      empty={t("admin.tenant.no-users")}
      add_label={t("admin.tenant.add-user")}
      id_label={t("admin.user.user-id")}
      on_add={(member_id) => engine_rest.tenant.add_user(state, tenant_id, member_id)}
      on_remove={(member_id) => engine_rest.tenant.remove_user(state, tenant_id, member_id)}
      refetch={() => engine_rest.tenant.user_members(state, tenant_id)} />

    <MemberSection
      title={t("admin.tenant.groups")}
      list_signal={tenant.group_members}
      empty={t("admin.tenant.no-groups")}
      add_label={t("admin.tenant.add-group")}
      id_label={t("admin.group.group-id")}
      on_add={(member_id) => engine_rest.tenant.add_group(state, tenant_id, member_id)}
      on_remove={(member_id) => engine_rest.tenant.remove_group(state, tenant_id, member_id)}
      refetch={() => engine_rest.tenant.group_members(state, tenant_id)} />

    <h3>{t("admin.danger-zone")}</h3>
    <div class="button-group">
      <button class="danger" onClick={() => (delete_open.value = true)}>{t("admin.tenant.delete")}</button>
    </div>
    <ConfirmDialog open={delete_open} message={t("admin.tenant.confirm-delete")} on_confirm={on_delete} />
  </div>
}

/* ------------------------------------------------------------------ Groups */

const GroupsPage = () => {
  const { params: { selection_id } } = useRoute()

  return (selection_id === undefined)
    ? <GroupsList />
    : <GroupDetails group_id={selection_id} />
}

const GroupsList = () => {
  const
    state = useContext(AppState),
    { api: { group: { list: groups, delete: group_delete } } } = state,
    [t] = useTranslation(),
    create_open = useSignal(false),
    delete_open = useSignal(false),
    pending_delete = useSignal(null)

  useEffect(() => {
    void engine_rest.group.all(state)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const confirm_delete = (group_id) => {
    pending_delete.value = group_id
    delete_open.value = true
  }
  const on_delete = () =>
    void engine_rest.group.delete(state, pending_delete.value).then(() => engine_rest.group.all(state))

  return <div class="content fade-in">
    <Breadcrumbs paths={[
      { name: t("nav.admin"), route: '/admin' },
      { name: t("admin.groups") }]} />
    <h2>{t("admin.groups")}</h2>
    <div class="button-group">
      <button onClick={() => (create_open.value = true)}>{t("admin.group.create")}</button>
    </div>
    <GroupCreate open={create_open} />
    <ActionResult signal={group_delete} success={t("admin.group.success-deleted")} />

    <RequestState
      signal={groups}
      on_success={() => groups.value.data.length !== 0
        ? <table class="fade-in">
          <thead>
          <tr>
            <th>{t("admin.group.group-id")}</th>
            <th>{t("admin.group.group-name")}</th>
            <th>{t("admin.group.group-type")}</th>
            <th>{t("common.action")}</th>
          </tr>
          </thead>
          <tbody>
          {groups.value.data.map((group) => (
            <tr key={group.id}>
              <td><a href={`/admin/groups/${group.id}`}>{group.id}</a></td>
              <td>{group.name}</td>
              <td>{group.type}</td>
              <td><button class="danger" onClick={() => confirm_delete(group.id)}>{t("common.delete")}</button></td>
            </tr>
          ))}
          </tbody>
        </table>
        : <p>{t("admin.group.no-groups")}</p>} />

    <ConfirmDialog open={delete_open} message={t("admin.group.confirm-delete")} on_confirm={on_delete} />
  </div>
}

const GroupCreate = ({ open }) => {
  const
    state = useContext(AppState),
    { api: { group: { create: group_create } } } = state,
    [t] = useTranslation(),
    form = useSignal({})

  const
    set_value = (k, e) => form.value = { ...form.peek(), [k]: e.currentTarget.value },
    on_submit = e => {
      e.preventDefault()
      void engine_rest.group.create(state, form.value).then(() => {
        if (has_data(group_create)) {
          engine_rest.group.all(state)
          open.value = false
        }
      })
    }

  return <Dialog open={open} title={t("admin.group.create")}>
    <ActionResult signal={group_create} success={t("admin.group.success-created")} />
    <form onSubmit={on_submit}>
      <label for="group-id">{t("admin.group.group-id")}</label>
      <input id="group-id" type="text" onInput={(e) => set_value('id', e)} required />

      <label for="group-name">{t("admin.group.group-name")}</label>
      <input id="group-name" type="text" onInput={(e) => set_value('name', e)} required />

      <label for="group-type">{t("admin.group.group-type")}</label>
      <input id="group-type" type="text" onInput={(e) => set_value('type', e)} />

      <div class="button-group">
        <button type="submit">{t("admin.group.create")}</button>
        <button type="button" class="secondary" onClick={() => (open.value = false)}>{t("common.cancel")}</button>
      </div>
    </form>
  </Dialog>
}

const GroupDetails = ({ group_id }) => {
  const
    state = useContext(AppState),
    { api: { group } } = state,
    [t] = useTranslation(),
    { route } = useLocation(),
    delete_open = useSignal(false),
    form = useSignal(null)

  useEffect(() => {
    form.value = null
    void engine_rest.group.all(state)
    void engine_rest.group.members(state, group_id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [group_id])

  useSignalEffect(() => {
    if (has_data(group.list) && form.value === null) {
      const found = group.list.value.data.find((g) => g.id === group_id)
      if (found) form.value = { ...found }
    }
  })

  const
    set_value = (k, e) => form.value = { ...form.peek(), [k]: e.currentTarget.value },
    on_submit = e => {
      e.preventDefault()
      void engine_rest.group.update(state, group_id, form.value)
    },
    on_delete = () =>
      void engine_rest.group.delete(state, group_id).then(() => route('/admin/groups'))

  return <div class="content fade-in">
    <Breadcrumbs paths={[
      { name: t("nav.admin"), route: '/admin' },
      { name: t("admin.groups"), route: '/admin/groups' },
      { name: group_id }]} />

    <h2>{t("admin.group.details")}</h2>

    <h3>{t("admin.group.information")}</h3>
    <ActionResult signal={group.update} success={t("admin.group.success-updated")} />
    {form.value
      ? <form onSubmit={on_submit}>
        <label for="group-id">{t("admin.group.group-id")}</label>
        <input id="group-id" type="text" value={form.value.id} disabled />

        <label for="group-name">{t("admin.group.group-name")}</label>
        <input id="group-name" type="text" value={form.value.name ?? ''} onInput={(e) => set_value('name', e)} required />

        <label for="group-type">{t("admin.group.group-type")}</label>
        <input id="group-type" type="text" value={form.value.type ?? ''} onInput={(e) => set_value('type', e)} />

        <div class="button-group">
          <button type="submit">{t("common.save")}</button>
        </div>
      </form>
      : <p>{t("common.loading")}</p>}

    <MemberSection
      title={t("admin.group.members")}
      list_signal={group.members}
      empty={t("admin.group.no-members")}
      add_label={t("admin.group.add-member")}
      id_label={t("admin.user.user-id")}
      on_add={(member_id) => engine_rest.group.add_user(state, group_id, member_id)}
      on_remove={(member_id) => engine_rest.group.remove_member(state, group_id, member_id)}
      refetch={() => engine_rest.group.members(state, group_id)} />

    <h3>{t("admin.danger-zone")}</h3>
    <div class="button-group">
      <button class="danger" onClick={() => (delete_open.value = true)}>{t("admin.group.delete")}</button>
    </div>
    <ConfirmDialog open={delete_open} message={t("admin.group.confirm-delete")} on_confirm={on_delete} />
  </div>
}

/**
 * A generic membership table: lists members (by id/name/type), supports removing
 * a member (confirm dialog) and adding one by id (dialog). Refetches after each.
 */
const MemberSection = ({ title, list_signal, empty, add_label, id_label, on_add, on_remove, refetch }) => {
  const
    [t] = useTranslation(),
    add_open = useSignal(false),
    remove_open = useSignal(false),
    pending_remove = useSignal(null),
    new_id = useSignal('')

  const
    confirm_remove = (member_id) => {
      pending_remove.value = member_id
      remove_open.value = true
    },
    do_remove = () => void Promise.resolve(on_remove(pending_remove.value)).then(refetch),
    do_add = e => {
      e.preventDefault()
      void Promise.resolve(on_add(new_id.value)).then(() => {
        new_id.value = ''
        add_open.value = false
        refetch()
      })
    }

  return <>
    <h3>{title}</h3>
    <RequestState
      signal={list_signal}
      on_success={() => list_signal.value.data.length !== 0
        ? <table>
          <thead>
          <tr>
            <th>{t("common.id")}</th>
            <th>{t("common.name")}</th>
            <th>{t("common.action")}</th>
          </tr>
          </thead>
          <tbody>
          {list_signal.value.data.map((member) => (
            <tr key={member.id}>
              <td>{member.id}</td>
              <td>{member.name}</td>
              <td><button class="danger" onClick={() => confirm_remove(member.id)}>{t("common.remove")}</button></td>
            </tr>
          ))}
          </tbody>
        </table>
        : <p>{empty}</p>} />

    <div class="button-group">
      <button onClick={() => (add_open.value = true)}>{add_label}</button>
    </div>

    <Dialog open={add_open} title={add_label}>
      <form onSubmit={do_add}>
        <label for="member-id">{id_label}</label>
        <input id="member-id" type="text" value={new_id.value}
               onInput={(e) => (new_id.value = e.currentTarget.value)} required />
        <div class="button-group">
          <button type="submit">{t("common.save")}</button>
          <button type="button" class="secondary" onClick={() => (add_open.value = false)}>{t("common.cancel")}</button>
        </div>
      </form>
    </Dialog>

    <ConfirmDialog open={remove_open} confirm_label={t("common.remove")}
                   message={t("admin.confirm-remove-member")} on_confirm={do_remove} />
  </>
}

/* ----------------------------------------------------------------- System */

const SystemPage = () => {
  const { api: { engine: { telemetry } } } = useContext(AppState),
    state = useContext(AppState),
    [t] = useTranslation()

  useEffect(() => {
    void engine_rest.engine.telemetry(state)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return <div class="content fade-in">
    <Breadcrumbs paths={[
      { name: t("nav.admin"), route: '/admin' },
      { name: t("admin.system") }]} />
    <h2>{t("admin.system")}</h2>
    <RequestState
      signal={telemetry}
      on_success={() => <pre class="fade-in">{telemetry.value !== undefined ? JSON.stringify(telemetry.value?.data, null, 2) : ''} </pre>}
    />
  </div>
}

/* ------------------------------------------------------------------ Users */

const UserPage = () => {
  const { params: { selection_id } } = useRoute()

  return (selection_id === undefined)
    ? <UserList />
    : <UserDetails user_id={selection_id} />
}

const UserList = () => {
  const
    state = useContext(AppState),
    { api: { user: { list: users } } } = state,
    [t] = useTranslation(),
    create_open = useSignal(false)

  useEffect(() => {
    void engine_rest.user.all(state)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return <div class="content fade-in">
    <Breadcrumbs paths={[
      { name: t("nav.admin"), route: '/admin' },
      { name: t("admin.users") }]} />
    <h2>{t("admin.users")}</h2>
    <div class="button-group">
      <button onClick={() => (create_open.value = true)}>{t("admin.user.create")}</button>
    </div>
    <UserCreate open={create_open} />

    <table class="fade-in">
      <thead>
      <tr>
        <th>{t("common.id")}</th>
        <th>{t("admin.user.first-name")}</th>
        <th>{t("admin.user.last-name")}</th>
        <th>{t("admin.user.email")}</th>
      </tr>
      </thead>
      <tbody>
      <RequestState
        signal={users}
        on_success={() => users.value?.data.map(({ id, firstName, lastName, email }) => (
          <tr key={id}>
            <td><a href={`/admin/users/${id}`}>{id}</a></td>
            <td>{firstName}</td>
            <td>{lastName}</td>
            <td>{email}</td>
          </tr>
        )) ?? <tr>
          <td>{t("admin.user.no-users")}</td>
        </tr>} />
      </tbody>
    </table>
  </div>
}

const UserCreate = ({ open }) => {
  const
    state = useContext(AppState),
    { api: { user: { create: user_create } } } = state,
    [t] = useTranslation(),
    form = useSignal({ profile: {}, credentials: {} }),
    password_repeat = useSignal(''),
    mismatch = useSignal(false)

  const
    set_value = (k1, k2, e) => form.value = { ...form.peek(), [k1]: { ...form.peek()[k1], [k2]: e.currentTarget.value } },
    set_p = (k, e) => set_value('profile', k, e),
    set_c = (k, e) => set_value('credentials', k, e),
    on_submit = e => {
      e.preventDefault()
      if (form.value.credentials.password !== password_repeat.value) {
        mismatch.value = true
        return
      }
      mismatch.value = false
      void engine_rest.user.create(state, form.value).then(() => {
        if (has_data(user_create)) {
          engine_rest.user.all(state)
          open.value = false
        }
      })
    }

  return <Dialog open={open} title={t("admin.user.create")}>
    <ActionResult signal={user_create} success={t("admin.user.success-created")} />
    {mismatch.value ? <p class="error">{t("admin.user.password-mismatch")}</p> : null}
    <form onSubmit={on_submit}>
      <label for="user-id">{t("admin.user.user-id")}</label>
      <input id="user-id" type="text" onInput={(e) => set_p('id', e)} required />

      <label for="password1">{t("admin.user.password")}</label>
      <input id="password1" type="password" onInput={(e) => set_c('password', e)} required />

      <label for="password2">{t("admin.user.password-repeated")}</label>
      <input id="password2" type="password" onInput={(e) => (password_repeat.value = e.currentTarget.value)} required />

      <label for="first-name">{t("admin.user.first-name")}</label>
      <input id="first-name" type="text" onInput={(e) => set_p('firstName', e)} required />

      <label for="last-name">{t("admin.user.last-name")}</label>
      <input id="last-name" type="text" onInput={(e) => set_p('lastName', e)} required />

      <label for="email">{t("admin.user.email")}</label>
      <input id="email" type="email" onInput={(e) => set_p('email', e)} required />

      <div class="button-group">
        <button type="submit">{t("admin.user.create")}</button>
        <button type="button" class="secondary" onClick={() => (open.value = false)}>{t("common.cancel")}</button>
      </div>
    </form>
  </Dialog>
}

const UserDetails = ({ user_id }) => {
  const
    state = useContext(AppState),
    [t] = useTranslation(),
    { route } = useLocation(),
    delete_open = useSignal(false)

  useEffect(() => {
    void engine_rest.user.profile.get(state, user_id)
    void engine_rest.group.by_member(state, user_id)
    void engine_rest.tenant.by_member(state, user_id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user_id])

  const on_delete = () =>
    void engine_rest.user.delete(state, user_id).then(() => route('/admin/users'))

  return <div class="content fade-in">
    <Breadcrumbs paths={[
      { name: t("nav.admin"), route: '/admin' },
      { name: t("admin.users"), route: '/admin/users' },
      { name: user_id }]} />

    <h2>{t("admin.user.details")}</h2>

    <h3>{t("admin.group.profile")}</h3>
    <UserProfile user_id={user_id} />
    <UserPassword user_id={user_id} />
    <UserGroups user_id={user_id} />
    <UserTenants />

    <h3>{t("admin.danger-zone")}</h3>
    <div class="button-group">
      <button class="danger" onClick={() => (delete_open.value = true)}>{t("admin.user.delete")}</button>
    </div>
    <ConfirmDialog open={delete_open} message={t("admin.user.confirm-delete")} on_confirm={on_delete} />
  </div>
}

const UserProfile = ({ user_id }) => {
  const
    state = useContext(AppState),
    { api: { user: { profile, update } } } = state,
    [t] = useTranslation(),
    form = useSignal(null)

  useSignalEffect(() => {
    if (has_data(profile)) {
      const { firstName, lastName, email } = profile.value.data
      form.value = { firstName: firstName ?? '', lastName: lastName ?? '', email: email ?? '' }
    }
  })

  const
    set_value = (k, e) => form.value = { ...form.peek(), [k]: e.currentTarget.value },
    on_submit = e => {
      e.preventDefault()
      void engine_rest.user.profile.update(state, user_id, form.value)
    }

  return form.value
    ? <>
      <ActionResult signal={update} success={t("admin.user.success-updated")} />
      <form onSubmit={on_submit}>
        <label for="first-name">{t("admin.user.first-name")}</label>
        <input id="first-name" value={form.value.firstName} onInput={(e) => set_value('firstName', e)} />

        <label for="last-name">{t("admin.user.last-name")}</label>
        <input id="last-name" value={form.value.lastName} onInput={(e) => set_value('lastName', e)} />

        <label for="email">{t("admin.user.email")}</label>
        <input id="email" type="email" value={form.value.email} onInput={(e) => set_value('email', e)} />

        <div class="button-group">
          <button type="submit">{t("admin.user.update-profile")}</button>
        </div>
      </form>
    </>
    : <p>{t("common.loading")}</p>
}

const UserPassword = ({ user_id }) => {
  const
    state = useContext(AppState),
    { api: { user: { credentials } } } = state,
    [t] = useTranslation(),
    password = useSignal(''),
    password_repeat = useSignal(''),
    mismatch = useSignal(false)

  const on_submit = e => {
    e.preventDefault()
    if (password.value !== password_repeat.value) {
      mismatch.value = true
      return
    }
    mismatch.value = false
    void engine_rest.user.credentials_update(state, user_id, { password: password.value })
  }

  return <>
    <h3>{t("admin.user.password")}</h3>
    <ActionResult signal={credentials} success={t("admin.user.password-success")} />
    {mismatch.value ? <p class="error">{t("admin.user.password-mismatch")}</p> : null}
    <form onSubmit={on_submit}>
      <label for="new-password">{t("admin.user.new-password")}</label>
      <input id="new-password" type="password" value={password.value}
             onInput={(e) => (password.value = e.currentTarget.value)} required />

      <label for="new-password-repeat">{t("admin.user.new-password-repeat")}</label>
      <input id="new-password-repeat" type="password" value={password_repeat.value}
             onInput={(e) => (password_repeat.value = e.currentTarget.value)} required />

      <div class="button-group">
        <button type="submit">{t("admin.user.change-password")}</button>
      </div>
    </form>
  </>
}

const UserGroups = ({ user_id }) => {
  const
    state = useContext(AppState),
    { api: { user: { group: { list: user_groups } } } } = state,
    [t] = useTranslation(),
    add_open = useSignal(false),
    remove_open = useSignal(false),
    pending_remove = useSignal(null),
    new_group = useSignal('')

  const
    refetch = () => engine_rest.group.by_member(state, user_id),
    confirm_remove = (group_id) => {
      pending_remove.value = group_id
      remove_open.value = true
    },
    do_remove = () =>
      void engine_rest.group.remove_member(state, pending_remove.value, user_id).then(refetch),
    do_add = e => {
      e.preventDefault()
      void engine_rest.group.add_user(state, new_group.value, user_id).then(() => {
        new_group.value = ''
        add_open.value = false
        refetch()
      })
    }

  return <>
    <h3>{t("admin.groups")}</h3>
    <RequestState
      signal={user_groups}
      on_success={() => user_groups.value.data.length !== 0
        ? <table>
          <caption class="screen-hidden">{t("admin.group.user-groups")}</caption>
          <thead>
          <tr>
            <th>{t("common.id")}</th>
            <th>{t("common.name")}</th>
            <th>{t("common.type")}</th>
            <th>{t("common.action")}</th>
          </tr>
          </thead>
          <tbody>
          {user_groups.value.data.map((group) => (
            <tr key={group.id}>
              <td>{group.id}</td>
              <td>{group.name}</td>
              <td>{group.type}</td>
              <td><button class="danger" onClick={() => confirm_remove(group.id)}>{t("admin.user.remove-from-group")}</button></td>
            </tr>
          ))}
          </tbody>
        </table>
        : <p>{t("admin.group.no-groups")}</p>} />

    <div class="button-group">
      <button onClick={() => (add_open.value = true)}>{t("admin.user.add-to-group")}</button>
    </div>

    <Dialog open={add_open} title={t("admin.user.add-to-group")}>
      <form onSubmit={do_add}>
        <label for="add-group-id">{t("admin.group.group-id")}</label>
        <input id="add-group-id" type="text" value={new_group.value}
               onInput={(e) => (new_group.value = e.currentTarget.value)} required />
        <div class="button-group">
          <button type="submit">{t("common.save")}</button>
          <button type="button" class="secondary" onClick={() => (add_open.value = false)}>{t("common.cancel")}</button>
        </div>
      </form>
    </Dialog>

    <ConfirmDialog open={remove_open} confirm_label={t("common.remove")}
                   message={t("admin.user.confirm-remove-from-group")} on_confirm={do_remove} />
  </>
}

const UserTenants = () => {
  const
    { api: { tenant: { by_member: tenants } } } = useContext(AppState),
    [t] = useTranslation()

  return <>
    <h3>{t("admin.tenants")}</h3>
    <RequestState
      signal={tenants}
      on_success={() => tenants.value.data.length !== 0
        ? <table>
          <thead>
          <tr>
            <th>{t("common.id")}</th>
            <th>{t("common.name")}</th>
          </tr>
          </thead>
          <tbody>
          {tenants.value.data.map((tenant) => (
            <tr key={tenant.id}>
              <td>{tenant.id}</td>
              <td>{tenant.name}</td>
            </tr>
          ))}
          </tbody>
        </table>
        : <p>{t("admin.user.no-tenants")}</p>} />
  </>
}

/* --------------------------------------------------------- Authorizations */

const AuthorizationsPage = () => {
  const
    { params: { selection_id, sub_selection_id } } = useRoute(),
    resource_type = selection_id === 'resource-type' ? sub_selection_id : undefined,
    state = useContext(AppState),
    [t] = useTranslation(),
    show_create = useSignal(false)

  useEffect(() => {
    if (resource_type !== undefined) void engine_rest.authorization.all(state, resource_type)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [resource_type])

  const resource = authorization_resources.find(
    ({ resource_type: rt }) => rt.toString() === resource_type)

  return <div class="content fade-in">
    <Breadcrumbs paths={resource_type
      ? [
        { name: t("nav.admin"), route: '/admin' },
        { name: t("admin.authorizations"), route: '/admin/authorizations' },
        { name: resource ? t(resource.nameKey) : resource_type }]
      : [
        { name: t("nav.admin"), route: '/admin' },
        { name: t("admin.authorizations") }]} />

    {!resource_type
      ? <>
        <h2>{t("admin.authorizations")}</h2>
        <ul class="link-list">
          {authorization_resources.map(({ nameKey, resource_type }) =>
            <li key={resource_type}>
              <a href={`/admin/authorizations/resource-type/${resource_type}`}>{t(nameKey)}</a>
            </li>)}
        </ul>
      </>
      : <>
        <h2>{resource ? t(resource.nameKey) : ''} {t("admin.authorization.title")}</h2>
        {resource ? <p><small>{t("admin.authorization.resource-id")}: {resource.resource_id}</small></p> : null}

        <div class="button-group">
          <button onClick={() => (show_create.value = !show_create.value)}>
            {!show_create.value ? t("admin.authorization.create") : t("admin.authorization.cancel-create")}
          </button>
        </div>

        {show_create.value
          ? <AuthorizationCreate resource={resource} resource_type={resource_type}
                                 on_done={() => (show_create.value = false)} />
          : null}

        <table class="fade-in auth-table">
          <thead>
          <tr>
            <th>{t("common.type")}</th>
            <th>{t("admin.authorization.user-group")}</th>
            <th>{t("admin.authorization.permissions")}</th>
            <th>{t("admin.authorization.resource-id")}</th>
            <th>{t("common.action")}</th>
          </tr>
          </thead>
          <tbody>
          <RequestState
            signal={state.api.authorization.all}
            on_success={() => state.api.authorization.all.value.data.map((authorization) =>
              <AuthorizationResourceRow key={authorization.id} authorization={authorization} />)} />
          </tbody>
        </table>
      </>}
  </div>
}

const AuthorizationCreate = ({ resource, resource_type, on_done }) => {
  const
    state = useContext(AppState),
    { api: { authorization: { create } } } = state,
    [t] = useTranslation(),
    form = useSignal({ type: 1, userId: '', permissions: [], resourceId: '*' })

  const
    set_value = (k, e) => form.value = { ...form.peek(), [k]: e.currentTarget.value },
    toggle_permission = (name, checked) => {
      const current = form.peek().permissions
      form.value = {
        ...form.peek(),
        permissions: checked ? [...current, name] : current.filter((p) => p !== name),
      }
    },
    on_submit = e => {
      e.preventDefault()
      const { type, userId, permissions, resourceId } = form.value
      void engine_rest.authorization.create(state, {
        type: Number(type),
        permissions,
        userId: userId || '*',
        resourceType: Number(resource_type),
        resourceId,
      }).then(() => {
        if (has_data(create)) {
          engine_rest.authorization.all(state, resource_type)
          on_done()
        }
      })
    }

  return <form onSubmit={on_submit} class="authorization-create">
    <ActionResult signal={create} success={t("admin.authorization.success-created")} />

    <label for="auth-type">{t("common.type")}</label>
    <select id="auth-type" onInput={(e) => set_value('type', e)}>
      <option value="0">{t("admin.authorization.global")}</option>
      <option value="1" selected>{t("admin.authorization.allow")}</option>
      <option value="2">{t("admin.authorization.deny")}</option>
    </select>

    <label for="auth-user">{t("admin.authorization.user-group")}</label>
    <input id="auth-user" type="text" value={form.value.userId}
           onInput={(e) => set_value('userId', e)} placeholder="*" />

    <fieldset>
      <legend>{t("admin.authorization.available-permissions")}</legend>
      {(resource?.permissions ?? []).map((name) => (
        <label key={name}>
          <input type="checkbox" value={name}
                 onInput={(e) => toggle_permission(name, e.currentTarget.checked)} />
          {humanize_permission(name)}
        </label>
      ))}
    </fieldset>

    <label for="auth-resource-id">{t("admin.authorization.resource-id")}</label>
    <input id="auth-resource-id" type="text" value={form.value.resourceId}
           onInput={(e) => set_value('resourceId', e)}
           placeholder={resource?.resource_id} required />

    <div class="button-group">
      <button type="submit">{t("common.save")}</button>
      <button type="button" class="secondary" onClick={on_done}>{t("common.cancel")}</button>
    </div>
  </form>
}

const AuthorizationResourceRow = ({ authorization }) => {
  const
    { permissions: perms, type, groupId, userId, resourceId, id } = authorization,
    state = useContext(AppState),
    [t] = useTranslation(),
    is_edit = useSignal(false),
    delete_open = useSignal(false),
    form = useSignal(authorization),
    resource = authorization_resources.find((r) => r.resource_type === authorization.resourceType),
    available_permissions = resource?.permissions ?? []

  const
    type_label = { 0: t("admin.authorization.global-display"), 1: t("admin.authorization.allow-display"), 2: t("admin.authorization.deny-display") }[type],
    form_id = `authorization_edit_${id}`,
    set_value = (k, e) => form.value = { ...form.peek(), [k]: e.currentTarget.value },
    set_null = (k) => form.value = { ...form.peek(), [k]: null },
    on_submit = e => {
      e.preventDefault()
      void engine_rest.authorization.update(state, id, form.value).then(() => {
        is_edit.value = false
      })
    },
    on_delete = () =>
      void engine_rest.authorization.delete(state, id).then(() =>
        engine_rest.authorization.all(state, form.peek().resourceType))

  return <tr>
    {!is_edit.value
      ? <>
        <td>{type_label}</td>
        <td>{userId || groupId}</td>
        <td>{perms.toString()}</td>
        <td>{resourceId}</td>
        <td>
          <div class="button-group">
            <button onClick={() => (is_edit.value = true)}>{t("common.edit")}</button>
            <button class="danger" onClick={() => (delete_open.value = true)}>{t("common.delete")}</button>
            <ConfirmDialog open={delete_open} message={t("admin.authorization.confirm-delete")} on_confirm={on_delete} />
          </div>
        </td>
      </>
      : <>
        <td>{type_label}</td>
        <td>
          <form id={form_id} onSubmit={on_submit}>
            {groupId
              ? <input name="groupId" value={form.value.groupId}
                       onInput={(e) => { set_value('groupId', e); set_null('userId') }} />
              : <input name="userId" value={form.value.userId}
                       onInput={(e) => { set_value('userId', e); set_null('groupId') }} />}
          </form>
        </td>
        <td>
          <select multiple form={form_id} aria-label={t("admin.authorization.permissions")}
                  onChange={(e) =>
                    (form.value = { ...form.peek(), permissions: Array.from(e.currentTarget.selectedOptions).map((o) => o.value) })}>
            {available_permissions.map((name) => (
              <option key={name} value={name} selected={form.value.permissions?.includes(name)}>{humanize_permission(name)}</option>
            ))}
          </select>
        </td>
        <td>
          <input form={form_id} name="resourceId" value={form.value.resourceId}
                 onInput={(e) => set_value('resourceId', e)}
                 placeholder={resource?.resource_id} title={resource?.resource_id} />
        </td>
        <td>
          <div class="button-group">
            <button onClick={() => (is_edit.value = false)}>{t("common.cancel")}</button>
            <button form={form_id} type="submit">{t("common.save")}</button>
          </div>
        </td>
      </>}
  </tr>
}

// Turn a permission enum name into a readable label, e.g.
// CREATE_BATCH_SET_VARIABLES -> "Create batch set variables".
const humanize_permission = (name) =>
  name.charAt(0) + name.slice(1).toLowerCase().replaceAll('_', ' ')

// Permissions valid per resource, as the Camunda 7 REST API expects them
// (basic CRUD subset + resource-specific additional permissions). "ALL" grants
// every permission for the resource.
const CRUD = ['ALL', 'READ', 'UPDATE', 'CREATE', 'DELETE']

const authorization_resources = [
  { id: 'application', nameKey: 'admin.authorization-resources.application', resource_type: 0, resource_id: 'admin/cockpit/tasklist/*', permissions: ['ALL', 'ACCESS'] },
  { id: 'authorization', nameKey: 'admin.authorization-resources.authorization', resource_type: 4, resource_id: 'Authorization ID', permissions: CRUD },
  { id: 'batch', nameKey: 'admin.authorization-resources.batch', resource_type: 13, resource_id: 'Batch ID', permissions: ['ALL', 'READ', 'UPDATE', 'CREATE', 'DELETE', 'READ_HISTORY', 'DELETE_HISTORY', 'CREATE_BATCH_MIGRATE_PROCESS_INSTANCES', 'CREATE_BATCH_MODIFY_PROCESS_INSTANCES', 'CREATE_BATCH_RESTART_PROCESS_INSTANCES', 'CREATE_BATCH_DELETE_RUNNING_PROCESS_INSTANCES', 'CREATE_BATCH_DELETE_FINISHED_PROCESS_INSTANCES', 'CREATE_BATCH_DELETE_DECISION_INSTANCES', 'CREATE_BATCH_SET_JOB_RETRIES', 'CREATE_BATCH_SET_EXTERNAL_TASK_RETRIES', 'CREATE_BATCH_UPDATE_PROCESS_INSTANCES_SUSPEND', 'CREATE_BATCH_SET_REMOVAL_TIME', 'CREATE_BATCH_SET_VARIABLES', 'CREATE_BATCH_CORRELATE_MESSAGES'] },
  { id: 'decision_definition', nameKey: 'admin.authorization-resources.decision-definition', resource_type: 10, resource_id: 'Decision Definition Key', permissions: ['ALL', 'READ', 'UPDATE', 'CREATE_INSTANCE', 'READ_HISTORY', 'DELETE_HISTORY'] },
  { id: 'decision_requirements_definition', nameKey: 'admin.authorization-resources.decision-requirements-definition', resource_type: 14, resource_id: 'Decision Requirements Definition Key', permissions: ['ALL', 'READ'] },
  { id: 'deployment', nameKey: 'admin.authorization-resources.deployment', resource_type: 9, resource_id: 'Deployment ID', permissions: ['ALL', 'READ', 'CREATE', 'DELETE'] },
  { id: 'filter', nameKey: 'admin.authorization-resources.filter', resource_type: 5, resource_id: 'Filter ID', permissions: CRUD },
  { id: 'group', nameKey: 'admin.authorization-resources.group', resource_type: 2, resource_id: 'Group ID', permissions: CRUD },
  { id: 'group_membership', nameKey: 'admin.authorization-resources.group-membership', resource_type: 3, resource_id: 'Group ID', permissions: ['ALL', 'CREATE', 'DELETE'] },
  { id: 'process_definition', nameKey: 'admin.authorization-resources.process-definition', resource_type: 6, resource_id: 'Process Definition Key', permissions: ['ALL', 'READ', 'UPDATE', 'CREATE_INSTANCE', 'READ_INSTANCE', 'UPDATE_INSTANCE', 'DELETE_INSTANCE', 'MIGRATE_INSTANCE', 'RETRY_JOB', 'SUSPEND', 'SUSPEND_INSTANCE', 'UPDATE_INSTANCE_VARIABLE', 'READ_TASK', 'UPDATE_TASK', 'TASK_WORK', 'TASK_ASSIGN', 'UPDATE_TASK_VARIABLE', 'READ_HISTORY', 'DELETE_HISTORY', 'UPDATE_HISTORY'] },
  { id: 'process_instance', nameKey: 'admin.authorization-resources.process-instance', resource_type: 8, resource_id: 'Process Instance ID', permissions: ['ALL', 'READ', 'CREATE', 'UPDATE', 'DELETE', 'RETRY_JOB', 'SUSPEND', 'UPDATE_VARIABLE'] },
  { id: 'task', nameKey: 'admin.authorization-resources.task', resource_type: 7, resource_id: 'Task ID', permissions: ['ALL', 'READ', 'UPDATE', 'CREATE', 'DELETE', 'TASK_WORK', 'TASK_ASSIGN', 'READ_VARIABLE', 'UPDATE_VARIABLE'] },
  { id: 'historic_task', nameKey: 'admin.authorization-resources.historic-task', resource_type: 19, resource_id: 'Historic Task ID', permissions: ['ALL', 'READ', 'READ_VARIABLE'] },
  { id: 'historic_process_instance', nameKey: 'admin.authorization-resources.historic-process-instance', resource_type: 20, resource_id: 'Historic Process Instance ID', permissions: ['ALL', 'READ'] },
  { id: 'tenant', nameKey: 'admin.authorization-resources.tenant', resource_type: 11, resource_id: 'Tenant ID', permissions: CRUD },
  { id: 'tenant_membership', nameKey: 'admin.authorization-resources.tenant-membership', resource_type: 12, resource_id: 'Tenant ID', permissions: ['ALL', 'CREATE', 'DELETE'] },
  { id: 'user', nameKey: 'admin.authorization-resources.user', resource_type: 1, resource_id: 'User ID', permissions: CRUD },
  { id: 'report', nameKey: 'admin.authorization-resources.report', resource_type: 15, resource_id: 'Report ID', permissions: CRUD },
  { id: 'dashboard', nameKey: 'admin.authorization-resources.dashboard', resource_type: 16, resource_id: 'Dashboard', permissions: CRUD },
  { id: 'user_operation_log_category', nameKey: 'admin.authorization-resources.user-operation-log', resource_type: 17, resource_id: 'User Operation Log Category', permissions: ['ALL', 'READ', 'UPDATE', 'DELETE'] },
  { id: 'system', nameKey: 'admin.authorization-resources.system', resource_type: 21, resource_id: '* resources do not support individual resource ids. You have to use them with a wildcard id (*).', permissions: ['ALL', 'READ'] },
]

export { AdminPage }
