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

  const is_selected = (page) => (page_id === page) ? 'selected' : ''

  return <div id="account-page">
    <nav>
      <ul class="list">
        <li class={is_selected('profile')}><a href="/account/profile">{t("account.profile")}</a></li>
        <li class={is_selected('account')}><a href="/account/account">{t("account.password")}</a></li>
        <li class={is_selected('groups')}><a href="/account/groups">{t("admin.groups")}</a></li>
        <li class={is_selected('tenants')}><a href="/account/tenants">{t("admin.tenants")}</a></li>
        <li class={is_selected('settings')}><a href="/account/settings">{t("account.settings")}</a></li>
      </ul>
    </nav>

    {({
      profile: <ProfileAccountPage />,
      account: <AccountAccountPage />,
      groups: <GroupAccountPage />,
      tenants: <TenantsAccountPage />,
      settings: <SettingsAccountPage />,
    })[page_id] ?? <p>{t("common.select-page")}</p>}
  </div>
}

const ProfileAccountPage = () => {
  const
    { params: { selection_id } } = useRoute(),
    state = useContext(AppState)

  useEffect(() => {
    if (!state.api.user.profile.value) {
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
    { user_profile, user_profile_edit, user_profile_edit_response } = state,
    [t] = useTranslation()

  useEffect(() => {
    user_profile_edit.value = { ...user_profile.value?.data }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user_profile.value?.data])

  const
    set_value = (k, v) => user_profile_edit.value = { ...user_profile_edit.peek(), [k]: v.currentTarget.value },
    on_submit = e => {
      e.preventDefault()
      engine_rest.user.profile.update(state).then(() => engine_rest.user.profile.get(state))
    }

  return <section>
    <h2>{t("account.edit-profile")}</h2>
    {user_profile_edit_response.value !== undefined
      ? <>
        {user_profile_edit_response.value?.status}
        <RequestState
          signal={user_profile_edit_response}
          on_success={() => <p className="success">{t("account.success-updated")}</p>}
          on_error={() => <p className="error">{t("common.error")} {user_profile_edit_response.value?.message}</p>} />
      </>
      : ''}


    <form onSubmit={on_submit}>
      <label for="first-name">{t("account.first-name")}</label>
      <input id="first-name" type="text" value={user_profile_edit.value.firstName}
             onInput={(e) => set_value('firstName', e)} required />

      <label for="last-name">{t("account.last-name")}</label>
      <input id="last-name" type="text" value={user_profile_edit.value.lastName}
             onInput={(e) => set_value('lastName', e)} required />

      <label for="email">{t("account.email")}</label>
      <input id="email" type="email" value={user_profile_edit.value.email}
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
    // , user_credentials, user_credentials_response, user_unlock_response
    { api: { user: { credentials, unlock } } } = state,
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

      <div className="button-group">
        {show_repeated_pw_hint.value && <div class="danger">{t("account.passwords-must-match")}</div>}
          <div class="danger">{t("account.password-change-failed")}</div>
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
    state = useContext(AppState),
    { api: { tenant: { list: tenants, by_member: user_tenants }} } = state,
    [t] = useTranslation(),
    // computed local state
    tenants_without_user_tenants = useComputed(() => tenants.value?.data?.filter(tenant => !user_tenants.value?.data?.map(user_tenant => user_tenant.id).includes(tenant.id))),
    //dialog functions
    close_add_tenant_dialog = () => document.getElementById('add-tenant-dialog').close(),
    show_add_tenant_dialog = () => {
      void engine_rest.tenant.by_member(state)
      document.getElementById('add-tenant-dialog').showModal()
    },
    //button handlers
    handle_add_tenant = (tenant_id) => engine_rest.tenant.add_user(state, tenant_id, null).then(() => engine_rest.tenant.by_member(state, null)),
    handle_remove_tenant = (tenant_id) => engine_rest.tenant.delete(state, tenant_id, null).then(() => engine_rest.tenant.by_member(state, null))

  useEffect(() => {
    if (!user_tenants.value) {
      void engine_rest.tenant.by_member(state)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return <section>
    <h2>{t("account.your-tenants")}</h2>
    {user_tenants.value?.data?.length > 0 ? <table>
        <thead>
        <tr>
          <th>{t("account.tenant-id")}</th>
          <th>{t("account.tenant-name")}</th>
          <th>{t("common.action")}</th>
        </tr>
        </thead>
        <tbody>
        {user_tenants.value.data.map((tenant) => (
          <tr key={tenant.id}>
            <td><a href={`/admin/tenants/${tenant.id}`}>{tenant.id}</a></td>
            <td>{tenant.name}</td>
            <td><a onClick={() => handle_remove_tenant(tenant.id)}>{t("common.remove")}</a></td>
          </tr>
        ))}
        </tbody>
      </table>
      : <p>{t("account.no-tenants")}</p>
    }
    <br />
    {/*<button class="primary" onClick={show_add_tenant_dialog}>Add Tenant +</button>*/}
    <dialog id="add-tenant-dialog" className="fade-in">
      <h2>{t("account.add-tenants")}</h2>
      {tenants_without_user_tenants.value?.length > 0 ? <table>
          <thead>
          <tr>
            <th>{t("account.tenant-id")}</th>
            <th>{t("account.tenant-name")}</th>
            <th>{t("common.action")}</th>
          </tr>
          </thead>
          <tbody>
          {tenants_without_user_tenants.value.map((tenant) => (
            <tr key={tenant.id}>
              <td><a href={`/admin/tenants/${tenant.id}`}>{tenant.id}</a></td>
              <td class="fill">{tenant.name}</td>
              <td><a onClick={() => handle_add_tenant(tenant.id)}>{t("account.add")}</a></td>
            </tr>
          ))}
          </tbody>
        </table>
        : <p>{t("account.no-additional-tenants")}</p>
      }
      <br />
      <div className="button-group">
        <button onClick={close_add_tenant_dialog}>{t("common.close")}</button>
      </div>
    </dialog>
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
