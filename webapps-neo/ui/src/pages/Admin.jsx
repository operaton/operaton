import { useContext } from 'preact/hooks'
import { useRoute, useLocation } from 'preact-iso'
import engine_rest, { RequestState } from '../api/engine_rest.jsx'
import { AppState } from '../state.js'
import { Breadcrumbs } from '../components/Breadcrumbs.jsx'
import { signal, useSignal } from '@preact/signals'
import authorization from '../api/resources/authorization.js'

const AdminPage = () => {
  const
    { params: { page_id } } = useRoute(),
    { route } = useLocation(),
    state = useContext(AppState)

  if (page_id === undefined) {
    route('/admin/users')
  }
  if (page_id === 'system') {
    void engine_rest.engine.telemetry(state)
  }
  if (page_id === 'groups') {
    void engine_rest.group.all(state)
  }
  if (page_id === 'tenants') {
    void engine_rest.tenant.all(state)
  }

  const is_selected = (page) => (page_id === page) ? 'selected' : ''

  return <div id="admin-page">
    <nav>
      <ul class="list">
        <li class={is_selected('users')}><a href="/admin/users">Users</a></li>
        <li class={is_selected('groups')}><a href="/admin/groups">Groups</a></li>
        <li class={is_selected('tenants')}><a href="/admin/tenants">Tenants</a></li>
        <li class={is_selected('authorizations')}><a href="/admin/authorizations">Authorizations</a></li>
        <li class={is_selected('system')}><a href="/admin/system">System</a></li>
      </ul>
    </nav>


    {({
      users: <UserPage />,
      groups: <GroupsPage />,
      tenants: <TenantsPage />,
      authorizations: <AuthorizationsPage />,
      system: <SystemPage />,
    })[page_id] ?? <p>Select Page</p>}

  </div>
}

const TenantsPage = () => {
  const
    { params: { selection_id } } = useRoute()

  return (selection_id === 'new')
    ? <TenantCreate />
    : (selection_id === undefined)
      ? <TenantList />
      : <TenantDetails tenant_id={selection_id} />
}

const TenantDetails = (tenant_id) => {
  const
    state = useContext(AppState)

  void engine_rest.user.profile.get(state, tenant_id.value)
  void engine_rest.group.by_member(state, tenant_id.value)
  void engine_rest.tenant.by_member(state, tenant_id.value)

  return <div class="content fade-in">
    <Breadcrumbs paths={[
      { name: 'Admin', route: '/admin' },
      { name: 'Tenant', route: '/admin/tenants' },
      { name: 'Details' }]} />

    <h2>Tenant Details</h2>

    <h3>Information</h3>
    <h3>Groups</h3>
    <h3>Users</h3>
    <h3>Danger Zone</h3>
  </div>
}

const TenantCreate = () => {
  const
    state = useContext(AppState),
    { api: { tenant: { create: tenant_create } } } = state,
    form_tenant = signal({ profile: {}, credentials: {} })

  const set_value = (k, v) => form_tenant.value[k] = v.currentTarget.value

  const on_submit = e => {
    e.preventDefault()
    console.log(tenant_create.value)
    void engine_rest.tenant.create(state, form_tenant.value)
    // e.currentTarget.reset(); // Clear the inputs to prepare for the next submission
  }

  return <div>
    <h2>Create New Tenant</h2>
    <RequestState
      signal={tenant_create}
      on_nothing={() => <></>}
      on_success={() => <p className="success">Successfully created new tenant.</p>}
      // on_error={() => <p className="error">Error: {user_create.value.error.message}</p>}
    />

    <form onSubmit={on_submit}>
      <label for="tenant-id">Tenant ID</label>
      <input id="tenant-id" type="text" onInput={(e) => set_value('id', e)} required />

      <label for="tenant-name">Tenant Name</label>
      <input id="tenant-name" type="text" onInput={(e) => set_value('name', e)} required />

      <div class="button-group">
        <button type="submit">Create New User</button>
        <a href="/admin/users" class="button secondary">Cancel</a>
      </div>
    </form>
  </div>
}

const TenantList = () => {
  const
    state = useContext(AppState),
    { api: { tenant: { list: tenants } } } = state

  return <div>
    <Breadcrumbs paths={[
      { name: 'Admin', route: '/admin' },
      { name: 'Tenants' }]} />
    <h2>Tenants</h2>

    <a href="/admin/tenants/new" class="button">Create new tenant</a>

    <RequestState
      signal={tenants}
      on_success={() => tenants.value.data.length !== 0
        ? <table class="fade-in">
          <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
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
        : <p>No tenants</p>} />
  </div>
}

const GroupsPage = () => {
  const
    state = useContext(AppState),
    { api: { group: { list: groups } } } = state,
    { params: { selection_id } } = useRoute()
  // //computed local state
  // // groups_without_user_groups = useComputed(() => groups.value?.data?.filter(group => !groups.value?.data?.map(user_group => user_group.id).includes(group.id))),
  // // dialog functions
  // // close_add_group_dialog = () => document.getElementById('add-group-dialog').close(),
  // // show_add_group_dialog = () => {
  // //   void api.group.all(state)
  // //   document.getElementById('add-group-dialog').showModal()
  // // },
  // // button handler
  // // handle_add_group = (group_id) => api.group.create(state, group_id).then(() => api.group.all(state, null)),
  // // handle_remove_group = (group_id) => api.group.delete(state, group_id).then(() => api.group.all(state, null))
  //
  // // if (!groups.value) {
  // //   void api.group.all(state)
  // // }


  return (selection_id === 'new')
    ? <GroupCreate />
    : (selection_id === undefined)
      ? <GroupsList />
      : <GroupDetails user_id={selection_id} />
}

const GroupCreate = () => {
  // https://preactjs.com/guide/v10/forms/
  const
    state = useContext(AppState),
    { api: { group: { create: group_create } } } = state,
    form_group = useSignal({})

  const set_value = (k, v) => form_group.value[k] = v.currentTarget.value


  const on_submit = e => {
    e.preventDefault()
    void engine_rest.group.create(state, form_group.value)
    // e.currentTarget.reset(); // Clear the inputs to prepare for the next submission
  }

  return <div>
    <h2>Create New Group</h2>
    <RequestState
      signal={group_create}
      on_nothing={() => <></>}
      on_success={() => <p className="success">Successfully created new group.</p>}
      // on_error={() => <p className="error">Error: {user_create.value.error.message}</p>}
    />

    <form onSubmit={on_submit}>
      <label for="group-id">Group ID</label>
      <input id="group-id" type="text" onInput={(e) => set_value('id', e)} required />

      <label for="group-name"> Group Name</label>
      <input id="group-name" type="text" onInput={(e) => set_value('groupName', e)} required />

      <label for="group-type">Group Type</label>
      <input id="group-type" type="text" onInput={(e) => set_value('groupType', e)} required />

      <div class="button-group">
        <button type="submit">Create New Group</button>
        <a href="/admin/groups" class="button secondary">Cancel</a>
      </div>
    </form>
  </div>
}

const GroupsList = () => {
  const
    { api: { group: { list: groups } } } = useContext(AppState)

  return <div>
    <Breadcrumbs paths={[
      { name: 'Admin', route: '/admin' },
      { name: 'Groups' }]} />
    <h2>Groups</h2>
    <a href="/admin/groups/new">Create New Group</a>
    <RequestState
      signal={groups}
      on_success={() => groups.value !== null ? <table class="fade-in">
          <thead>
          <tr>
            <th>Group ID</th>
            <th>Group Name</th>
            <th>Group Type</th>
            <th>Action</th>
          </tr>
          </thead>
          <tbody>
          {groups.value.data.map((group) => (
            <tr key={group.id}>
              <td><a href={`/admin/groups/${group.id}`}>{group.id}</a></td>
              <td>{group.name}</td>
              <td>{group.type}</td>
              {/*<td><a onClick={() => handle_remove_group(group.id)}>Remove</a></td>*/}
            </tr>
          ))}
          </tbody>
        </table>
        : <p>User is currently not a member of any group.</p>} />
  </div>
}

const GroupDetails = (user_id) => {
  const
    state = useContext(AppState)

  void engine_rest.user.profile.get(state, user_id.value)
  void engine_rest.group.by_member(state, user_id.value)
  void engine_rest.tenant.by_member(state, user_id.value)

  return <div class="content fade-in">
    <Breadcrumbs paths={[
      { name: 'Admin', route: '/admin' },
      { name: 'Groups', route: '/admin/groups' },
      { name: 'Details' }]} />

    <h2>Group Details</h2>

    <h3>Profile</h3>
    <UserProfile />
    <UserPassword />
    <UserGroups />
    <h3>Tenants</h3>
    <h3>Danger Zone</h3>
  </div>
}

const SystemPage = () => {
  const { api: { engine: { telemetry } } } = useContext(AppState)

  return <div>
    <Breadcrumbs paths={[
      { name: 'Admin', route: '/admin' },
      { name: 'System' }]} />
    <h2>System</h2>
    <RequestState
      signal={telemetry}
      on_success={() => <pre class="fade-in">{telemetry.value !== undefined ? JSON.stringify(telemetry.value?.data, null, 2) : ''} </pre>}
    />
  </div>
}

// const JsonToText = (json) => {
//   return
// }

const UserPage = () => {
  const
    state = useContext(AppState),
    { params: { selection_id } } = useRoute()

  // selection_id === undefined ? void api.get_users(state) : null
  selection_id === undefined ? void engine_rest.user.all(state) : null

  return (selection_id === 'new')
    ? <UserCreate />
    : (selection_id === undefined)
      ? <UserList />
      : <UserDetails user_id={selection_id} />
}

const UserList = () => {
  const { api: { user: { list: users } } } = useContext(AppState)

  return <div className="content">
    <Breadcrumbs paths={[
      { name: 'Admin', route: '/admin' },
      { name: 'Users' }]} />
    <h2>Users</h2>
    <a href="/admin/users/new">Create New User</a>
    <table class="fade-in">
      <thead>
      <tr>
        <th>ID</th>
        <th>First Name</th>
        <th>Last Name</th>
        <th>Email</th>
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
          <td>No Users found</td>
        </tr>} />
      </tbody>
    </table>
  </div>
}

const UserDetails = (user_id) => {
  const
    state = useContext(AppState)

  void engine_rest.user.profile.get(state, user_id.value)
  void engine_rest.group.by_member(state, user_id.value)
  void engine_rest.tenant.by_member(state, user_id.value)

  return <div class="content fade-in">
    <Breadcrumbs paths={[
      { name: 'Admin', route: '/admin' },
      { name: 'Users', route: '/admin/users' },
      { name: 'Details' }]} />

    <h2>User Details</h2>

    <h3>Profile</h3>
    <UserProfile />
    <UserPassword />
    <UserGroups />
    <h3>Tenants</h3>
    <h3>Danger Zone</h3>
  </div>
}

const UserGroups = () => {
  const { api: { user: { group: { list: user_groups } } } } = useContext(AppState)

  return <>
    <h3>Groups</h3>
    <RequestState
      signal={user_groups}
      on_success={() =>
        <table>
          <caption class="screen-hidden">User Groups</caption>
          <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Type</th>
            <th>Action</th>
          </tr>
          </thead>
          <tbody>
          {user_groups.value.data.map(group => <tr key={group.id}>
            <td>{group.id}</td>
            <td>{group.name}</td>
            <td>{group.type}</td>
            <td>Remove from group</td>
          </tr>)}
          </tbody>
        </table>
      } />
    <button>Add to group</button>
  </>
}

const UserProfile = () => {
  /** @namespace user_profile.value.data.firstName **/
  /** @namespace user_profile.value.data.lastName **/
  const
    { api: { user: { profile } } } = useContext(AppState)

  return <>{profile.value?.data
    ? <form>
      <label for="first-name">First Name </label>
      <input id="first-name" value={profile.value.data.firstName ?? ''} />

      <label for="last-name">Last Name</label>
      <input id="last-name" value={profile.value.data.lastName ?? ''} />

      <label for="email">Email</label>
      <input id="email" type="email" value={profile.value.data.email ?? ''} />


      <div class="button-group">
        <button type="submit">Update Profile</button>
      </div>
    </form>
    : <p>Loading...</p>
  }</>
}

const UserPassword = () => {

  return <>
    <h3>Password</h3>
    <form>
      <label for="new-password">New Password</label>
      <input id="new-password" type="password" placeholder="* * * * * * * * *" />

      <label for="new-password-repeat">New Password (repeat)</label>
      <input id="new-password-repeat" type="password" placeholder="* * * * * * * * *" />

      <div class="button-group">
        <button type="submit">Change Password</button>
      </div>
    </form>
  </>
}

const UserCreate = () => {
  // https://preactjs.com/guide/v10/forms/
  const
    state = useContext(AppState),
    { api: { user: { create: user_create } } } = state,
    form_user = signal({ profile: {}, credentials: {} })

  const set_value = (k1, k2, v) => form_user.value[k1][k2] = v.currentTarget.value
  const set_p_value = (k, v) => set_value('profile', k, v)
  const set_c_value = (k, v) => set_value('credentials', k, v)


  const on_submit = e => {
    e.preventDefault()
    console.log(user_create.value)
    void engine_rest.user.create(state, form_user.value)
    // e.currentTarget.reset(); // Clear the inputs to prepare for the next submission
  }

  return <div>
    <h2>Create New User</h2>
    <RequestState
      signal={user_create}
      on_nothing={() => <></>}
      on_success={() => <p className="success">Successfully created new user.</p>}
      // on_error={() => <p className="error">Error: {user_create.value.error.message}</p>}
    />

    <form onSubmit={on_submit}>
      <label for="user-id">User ID</label>
      <input id="user-id" type="text" onInput={(e) => set_p_value('id', e)} required />

      <label for="password1">Password</label>
      <input id="password1" type="password" onInput={(e) => set_c_value('password', e)} required />

      <label for="password2"> Password (repeated)</label>
      <input id="password2" type="password" onInput={(e) => set_c_value('password', e)} />

      <label for="first-name"> First Name</label>
      <input id="first-name" type="text" onInput={(e) => set_p_value('firstName', e)} required />

      <label for="last-name">Last Name</label>
      <input id="last-name" type="text" onInput={(e) => set_p_value('lastName', e)} required />

      <label for="email">Email</label>
      <input id="email" type="email" onInput={(e) => set_p_value('email', e)} required />

      <div class="button-group">
        <button type="submit">Create New User</button>
        <a href="/admin/users" class="button secondary">Cancel</a>
      </div>
    </form>
  </div>
}

const AuthorizationsPage = () => {
  const
    { query: { resource_type } } = useRoute(),
    state = useContext(AppState),
    show_create_authorization = useSignal(false)

  if (resource_type !== undefined || state.api.authorization.all.value === null) {
    void engine_rest.authorization.all(state, resource_type)
  }

  return <div>
    <Breadcrumbs paths={[
      { name: 'Admin', route: '/admin' },
      { name: 'Authorizations' }]} />

    <div class="row">
      <ul class="list">
        {authorization_resources.map(({ name, resource_type }) =>
          <li key={resource_type}>
            <a href={`/admin/authorizations?resource_type=${resource_type}`}
               onClick={() => engine_rest.authorization.all(state, resource_type)}>
              {name}
            </a>
          </li>)}
      </ul>
      {resource_type
        ? <div>
          <h3>
            {(resource_type !== undefined && resource_type !== null)
              ? authorization_resources.find(({ resource_type: resource_type_ }) => resource_type_.toString() === resource_type).name
              : ''} Authorization
          </h3>

          <button onClick={() => show_create_authorization.value = !show_create_authorization.value}>
            {!show_create_authorization.value
              ? 'Create new authorization'
              : 'Cancel creating new authorization'}
          </button>

          <table class="fade-in">
            <thead>
            <tr>
              <th>Type</th>
              <th>User / Group</th>
              <th>Permissions</th>
              <th>Resource ID</th>
              <th>Action</th>
            </tr>
            </thead>
            <tbody>
            {show_create_authorization.value
              ? <tr>
                <td>
                  <form id="create-authorization-form" onSubmit={null}>
                    <select>
                      <option value="gloabl">GLOBAL</option>
                      <option value="allow">ALLOW</option>
                      <option value="deny">DENY</option>
                    </select>
                  </form>
                </td>
                <td>
                  <input
                    id=""
                    name=""
                    onInput={(e) => {}} />
                </td>
                <td>
                  <fieldset>
                    <legend>Available Permissions</legend>
                    <label>
                      Create
                      <input type="checkbox" value="" />
                    </label>
                  </fieldset>
                </td>
                <td>
                  <input
                    type="text"
                    id=""
                    name=""
                    onInput={(e) => {}} />
                </td>
                <td class="button-group">
                  <button onClick={() => null}>Cancel</button>
                  <button form="create-authorization-form" type="submit">Save</button>
                </td>
              </tr>
              : ''
            }
            <RequestState
              signal={state.api.authorization.all}
              on_success={() => <AuthorizationResourceRows authorizations={state.api.authorization.all.value.data} />} />
            </tbody>
          </table>
        </div>
        : <p class="info-box">Select a authorization resource</p>}
        </div>
    </div>
    }

const AuthorizationResourceRows = ({ authorizations }) =>
  authorizations.map(AuthorizationResourceRow)

const AuthorizationResourceRow = (authorization) => {
  const
    { permissions, type, groupId, userId, resourceId, id } = authorization,
    state = useContext(AppState),
    is_edit = useSignal(false),
    is_deleted = useSignal(false),
    form_authorization = signal(authorization),
    form_id = `authorization_edit_${id}`,
    set_value = (k, v) => form_authorization.value[k] = v.currentTarget.value,
    set_null = (k) => form_authorization.value[k] = null


  const
    on_submit = e => {
      e.preventDefault()
      void engine_rest.authorization.update(state, id, form_authorization.value)
      // e.currentTarget.reset(); // Clear the inputs to prepare for the next submission
    },
    dialog_id = `delete_authorization_dialog_${id}`,
    show_delete_dialog = () =>
      document.getElementById(dialog_id).showModal(),
    delete_authorization = () => {
      void engine_rest.authorization.delete(state, id)
      is_deleted.value = true
      document.getElementById(dialog_id).close()
    }

  return <>{!is_deleted.value
    ? <tr key={id}>
      {!is_edit.value
        ? <>
          <td>{{ 0: 'Global', 1: 'Allow', 2: 'Deny' }[type]}</td>
          {/* (0=global, 1=grant, 2=revoke)*/}
          <td>{userId || groupId}</td>
          <td>{permissions.toString()}</td>
          <td>{resourceId}</td>
          <td className="button-group">
            <button onClick={() => is_edit.value = true}>Edit</button>

            <button onClick={() => show_delete_dialog()}>
              Delete
            </button>
          </td>
        </>
        : <>
          <td>{{ 0: 'Global', 1: 'Allow', 2: 'Deny' }[type]}</td>
          {/* (0=global, 1=grant, 2=revoke)*/}
          <td>
            <form id={form_id} onSubmit={on_submit}>
              {groupId
                ? <input name="groupId" value={groupId}
                         onInput={(e) => {
                           set_value('groupId', e)
                           set_null('userId')
                         }} />
                : <input name="userId" value={userId}
                         onInput={(e) => {
                           set_value('userId', e)
                           set_null('groupId')
                         }} />}
            </form>
          </td>
          <td>{permissions.toString()}</td>
          <td>
            <input form={form_id} name="resourceId" value={resourceId} onInput={(e) => set_value('resourceId', e)} />
          </td>
          <td class="button-group">
            <button onClick={() => is_edit.value = false}>Cancel</button>
            <button form={form_id} type="submit">Save</button>
          </td>
        </>
      }
    </tr>
    : ''
  }

    <dialog id={dialog_id}>
      Do you really want to delete this authorization?

      <div class="button-group">
        <button class="danger" onClick={delete_authorization}>Delete</button>
        <button onClick={() => document.getElementById(dialog_id).close()}>Cancel</button>
      </div>
    </dialog>
  </>

}

const permissions = {
  READ: 0,
  UPDATE: 1,
  CREATE: 2,
  DELETE: 3,
  ACCESS: 4

  // Task Assign
  // Task Work
  // Read Variable
  // Update Variable
}

const authorization_resources = [
  { id: 'application', name: 'Application', resource_type: 0, resource_id: 'admin/cockpit/tasklist/*', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'authorization', name: 'Authorization', resource_type: 4, resource_id: 'Authorization ID', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'batch', name: 'Batch', resource_type: 13, resource_id: 'Batch ID', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'decision_definition', name: 'Decision Definition', resource_type: 10, resource_id: 'Decision Definition Key', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'decision_requirements_definition', name: 'Decision Requirements Definition', resource_type: 14, resource_id: 'Decision Requirements Definition Key', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'deployment', name: 'Deployment', resource_type: 9, resource_id: 'Deployment ID', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'filter', name: 'Filter', resource_type: 5, resource_id: 'Filter ID', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'group', name: 'Group', resource_type: 2, resource_id: 'Group ID', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'group_membership', name: 'Group Membership', resource_type: 3, resource_id: 'Group ID', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'process_definition', name: 'Process Definition', resource_type: 6, resource_id: 'Process Definition Key', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'process_instance', name: 'Process Instance', resource_type: 8, resource_id: 'Process Instance ID', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'task', name: 'Task', resource_type: 7, resource_id: 'Task ID', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'historic_task', name: 'Historic Task', resource_type: 19, resource_id: 'Historic Task ID', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'historic_process_instance', name: 'Historic Process Instance', resource_type: 20, resource_id: 'Historic Process Instance ID', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'tenant', name: 'Tenant', resource_type: 11, resource_id: 'Tenant ID', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'tenant_membership', name: 'Tenant Membership', resource_type: 12, resource_id: 'Tenant ID', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'user', name: 'User', resource_type: 1, resource_id: 'User ID', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'report', name: 'Report', resource_type: 15, resource_id: 'Report ID', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'dashboard', name: 'Dashboard', resource_type: 16, resource_id: 'Dashboard', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'user_operation_log_category', name: 'User Operation Log Category', resource_type: 17, resource_id: 'User Operation Log Category', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
  { id: 'system', name: 'System', resource_type: 21, resource_id: '* resources do not support individual resource ids. You have to use them with a wildcard id (*).', permission: [permissions.READ, permissions.UPDATE, permissions.CREATE, permissions.DELETE] },
]

export { AdminPage }
