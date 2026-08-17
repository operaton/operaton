import { useLocation, useRoute } from 'preact-iso'
import { useTranslation } from 'react-i18next'
import { AppState } from '../state.js'
import engine_rest, { RequestState } from '../api/engine_rest.jsx'
import { useContext, useEffect } from 'preact/hooks'
import { useComputed, useSignal } from '@preact/signals'

const AccountPage = () => {
  const
    { params: { page_id } } = useRoute(),
    { route } = useLocation(),
    [t] = useTranslation()

  useEffect(() => {
    if (page_id === undefined) {
      route('/account/profile', true)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page_id])

  const current = (page) => (page_id === page) ? 'page' : undefined

  return <main id="content" class="account-page">
    <nav aria-label={t("nav.account")}>
      <menu class="list">
        <li><a href="/account/profile" aria-current={current('profile')}>{t("account.profile")}</a></li>
        <li><a href="/account/account" aria-current={current('account')}>{t("account.password")}</a></li>
        <li><a href="/account/groups" aria-current={current('groups')}>{t("admin.groups")}</a></li>
        <li><a href="/account/tenants" aria-current={current('tenants')}>{t("admin.tenants")}</a></li>
        <li><a href="/account/settings" aria-current={current('settings')}>{t("account.settings")}</a></li>
      </menu>
    </nav>

    {({
      profile: <ProfileAccountPage />,
      account: <AccountAccountPage />,
      groups: <GroupAccountPage />,
      tenants: <TenantsAccountPage />,
      settings: <SettingsAccountPage />,
    })[page_id] ?? <p>{t("common.select-page")}</p>}
  </main>
}

const ProfileAccountPage = () => {
  const
    { params: { selection_id } } = useRoute(),
    state = useContext(AppState)

  useEffect(() => {
    // The signal starts out holding the login placeholder ({ id }), not a
    // response, so check for fetched data rather than for any value at all.
    if (!state.api.user.profile.value?.data) {
      void engine_rest.user.profile.get(state)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])
  return <RequestState
    signal={state.api.user.profile}
    on_success={() => selection_id === 'edit' ? <ProfileEditPage /> : <ProfileDetails />
    }
  />
}

const ProfileEditPage = () => {
  const
    state = useContext(AppState),
    { profile, update } = state.api.user,
    // Ephemeral edit buffer — local UI state, seeded from the fetched profile
    // (which the parent RequestState guarantees is loaded before we mount).
    edit = useSignal({ ...profile.value?.data }),
    [t] = useTranslation()

  // Re-seed when the profile refetches; drop any stale update feedback on mount
  // so a previous session's success/error banner can't flash.
  useEffect(() => {
    edit.value = { ...profile.value?.data }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [profile.value?.data])
  useEffect(() => {
    update.value = null
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const
    set_value = (k, v) => edit.value = { ...edit.peek(), [k]: v.currentTarget.value },
    on_submit = e => {
      e.preventDefault()
      engine_rest.user.profile.update(state, null, edit.value).then(() => engine_rest.user.profile.get(state))
    }

  return <section>
    <h2>{t("account.edit-profile")}</h2>
    <div aria-live="polite">
      {update.value != null
        ? <RequestState
            signal={update}
            on_success={() => <p class="success">{t("account.success-updated")}</p>}
            on_error={() => <p class="error">{t("common.error")} {update.value?.message}</p>} />
        : ''}
    </div>


    <form onSubmit={on_submit}>
      <label for="first-name">{t("account.first-name")}</label>
      <input id="first-name" type="text" value={edit.value.firstName}
             onInput={(e) => set_value('firstName', e)} required />

      <label for="last-name">{t("account.last-name")}</label>
      <input id="last-name" type="text" value={edit.value.lastName}
             onInput={(e) => set_value('lastName', e)} required />

      <label for="email">{t("account.email")}</label>
      <input id="email" type="email" value={edit.value.email}
             onInput={(e) => set_value('email', e)} required />

      <div class="button-group">
        <a href="/account/profile" class="button secondary">{t("common.cancel")}</a>
        <button type="submit">{t("account.update-profile")}</button>
      </div>
    </form>
  </section>
}

const ProfileDetails = () => {
  const
    state = useContext(AppState),
    { api: { user: { profile }} } = state,
    [t] = useTranslation()

  return <section>
    <h2>{t("account.profile")}</h2>
    <dl>
      <dt>{t("account.first-name")}</dt>
      <dd>{profile.value.data.firstName}</dd>
      <dt>{t("account.last-name")}</dt>
      <dd>{profile.value.data.lastName}</dd>
      <dt>{t("account.email")}</dt>
      <dd>{profile.value.data.email}</dd>
    </dl>
    <a href={`/admin/users/${profile.value.data.id}`} class="button">{t("account.edit")}</a>
  </section>
}

const AccountAccountPage = () => {
  const
    state = useContext(AppState),
    [t] = useTranslation(),
    // local state
    old_password = useSignal(''),
    password = useSignal(''),
    password_repeat = useSignal(''),
    // computed local state
    is_change_pw_button_disabled = useComputed(() => password.value !== password_repeat.value || !old_password.value || !password.value || !password_repeat.value),
    show_repeated_pw_hint = useComputed(() => password.value !== password_repeat.value),
    // form handlers
    on_submit = e => {
      e.preventDefault()
      void engine_rest.user.credentials_update(state, null, {
        authenticatedUserPassword: old_password.value,
        password: password.value,
      })
    }

  return <section>
    <h2>{t("account.change-password")}</h2>
    <form onSubmit={on_submit}>
      <label for="old-pw">{t("account.old-password")}</label>
      <input id="old-pw" type="password" onInput={(e) => old_password.value = e.currentTarget.value} required />

      <label for="new-pw">{t("account.new-password")}</label>
      <input id="new-pw" type="password" onInput={(e) => password.value = e.currentTarget.value} required />

      <label for="new-pw-repeat">{t("account.new-password-repeat")}</label>
      <input id="new-pw-repeat" type="password" onInput={(e) => password_repeat.value = e.currentTarget.value}
             required />

      <div class="button-group">
        <div aria-live="assertive">
          {show_repeated_pw_hint.value && <div class="danger">{t("account.passwords-must-match")}</div>}
        </div>
        <button type="submit" disabled={is_change_pw_button_disabled.value}>{t("account.change-password")}</button>
      </div>
    </form>
  </section>
}

const GroupAccountPage = () => {
  const
    state = useContext(AppState),
    { api: { user: { group: { list: groups } } } } = state,
    [t] = useTranslation()

  useEffect(() => {
    if (!groups.value) {
      void engine_rest.group.by_member(state, null)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return <section>
    <h2>{t("account.your-groups")}</h2>
    <RequestState
      signal={groups}
      on_success={() =>
        groups.value?.data.length > 0 ? <table>
            <thead>
            <tr>
              <th>{t("account.group-id")}</th>
              <th>{t("account.group-name")}</th>
              <th>{t("account.group-type")}</th>
            </tr>
            </thead>
            <tbody>
            {groups.value.data.map((group) => (
              <tr key={group.id}>
                <td><a href={`/admin/groups/${group.id}`}>{group.id}</a></td>
                <td>{group.name}</td>
                <td>{group.type}</td>
              </tr>
            ))}
            </tbody>
          </table>
          : <p>{t("account.no-groups")}</p>} />
  </section>
}

const TenantsAccountPage = () => {
  const
    { params: { selection_id } } = useRoute(),
    state = useContext(AppState),
    { api: { tenant: { list: tenants, by_member: user_tenants }} } = state,
    [t] = useTranslation(),
    // computed local state
    tenants_without_user_tenants = useComputed(() => tenants.value?.data?.filter(tenant => !user_tenants.value?.data?.map(user_tenant => user_tenant.id).includes(tenant.id))),
    //button handlers
    handle_add_tenant = (tenant_id) => engine_rest.tenant.add_user(state, tenant_id, null).then(() => engine_rest.tenant.by_member(state, null)),
    handle_remove_tenant = (tenant_id) => engine_rest.tenant.delete(state, tenant_id, null).then(() => engine_rest.tenant.by_member(state, null))

  useEffect(() => {
    if (!user_tenants.value) {
      void engine_rest.tenant.by_member(state)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (selection_id === 'add') {
      void engine_rest.tenant.all(state)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selection_id])

  // Add-tenant is its own route (/account/tenants/add) rather than a JS-toggled dialog,
  // so the picker is linkable and survives a reload.
  if (selection_id === 'add') {
    return <section>
      <h2>{t("account.add-tenants")}</h2>
      {tenants_without_user_tenants.value?.length > 0 ? <table>
          <thead>
          <tr>
            <th scope="col">{t("account.tenant-id")}</th>
            <th scope="col">{t("account.tenant-name")}</th>
            <th scope="col">{t("common.action")}</th>
          </tr>
          </thead>
          <tbody>
          {tenants_without_user_tenants.value.map((tenant) => (
            <tr key={tenant.id}>
              <td><a href={`/admin/tenants/${tenant.id}`}>{tenant.id}</a></td>
              <td class="fill">{tenant.name}</td>
              <td><button type="button" class="link" onClick={() => handle_add_tenant(tenant.id)}>{t("account.add")}</button></td>
            </tr>
          ))}
          </tbody>
        </table>
        : <p>{t("account.no-additional-tenants")}</p>
      }
      <div class="button-group">
        <a href="/account/tenants" class="button secondary">{t("common.back")}</a>
      </div>
    </section>
  }

  return <section>
    <h2>{t("account.your-tenants")}</h2>
    {user_tenants.value?.data?.length > 0 ? <table>
        <thead>
        <tr>
          <th scope="col">{t("account.tenant-id")}</th>
          <th scope="col">{t("account.tenant-name")}</th>
          <th scope="col">{t("common.action")}</th>
        </tr>
        </thead>
        <tbody>
        {user_tenants.value.data.map((tenant) => (
          <tr key={tenant.id}>
            <td><a href={`/admin/tenants/${tenant.id}`}>{tenant.id}</a></td>
            <td>{tenant.name}</td>
            <td><button type="button" class="link" onClick={() => handle_remove_tenant(tenant.id)}>{t("common.remove")}</button></td>
          </tr>
        ))}
        </tbody>
      </table>
      : <p>{t("account.no-tenants")}</p>
    }
    <div class="button-group">
      <a href="/account/tenants/add" class="button primary">{t("account.add-tenants")}</a>
    </div>
  </section>
}

const LANGUAGES = [
  { code: 'en-US', label: 'English' },
  { code: 'de-DE', label: 'Deutsch' },
  { code: 'fr-FR', label: 'Français' },
  { code: 'es-ES', label: 'Español' },
  { code: 'nl-NL', label: 'Nederlands' },
]

const SettingsAccountPage = () => {
  const [t, i18n] = useTranslation()

  const change_language = (e) => {
    i18n.changeLanguage(e.currentTarget.value)
  }

  return <section>
    <h2>{t("account.settings")}</h2>

    <label for="language-select">{t("account.language")}</label>
    <p>{t("account.language-hint")}</p>
    <select id="language-select" value={i18n.language} onChange={change_language}>
      {LANGUAGES.map(({ code, label }) =>
        <option key={code} value={code}>{label}</option>
      )}
    </select>
  </section>
}

export {
  AccountPage
}
