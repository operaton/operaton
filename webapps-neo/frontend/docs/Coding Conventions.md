# Coding Conventions

## Accessibility

The resulting UI needs to be accessible.

- Use semantic HTML where possible
- Adhere to accessibility standards, as explained
  in [Accessibility.md](Accessibility.md)

## Formatting

### Use Standard Style

For consistency across all JavaScript files we use [**standard
**](https://github.com/standard/standard?tab=readme-ov-file).

IntelliJ:

- Go to `Preferences | Editor | Code style | JavaScript`
- Click `Set from...`
- Select `JavaScript Standard Style`

### Tabs

We use 2 spaces for one tab, per indentation. 

IntelliJ:

- Go to `Editor > Code Style > JavaScript > Tabs and Indents`
- Set all three fields to `2`

### When to create new files

- Avoid creating new files, when not necessary
- Create new files for jsx components which you want to reuse 
- Don't create new files for another single use component of your layout code
- Create new files in their appropriate folder (look at the folder structure)

## JavaScript

### General

- Use `const foo = () => ..` instead of `function foo() {}` for everything
    - Prefer directly returning your result, over
      `const foo = () => { ... return ... }`
- Use `_` (underscores) instead of camel case for functions
- Use CamelCase for JSX components (to be discussed)
- Use `class="foo bar"` instead of `className={}`
- Use `a` when changing the state of the web apps aka. changing the route
- Use `button` when changing the state of the data aka. communicating with the back-end
- Never place a `onClick` on a `div` or similar without reason (use `button` instead)
- Define values in groups:

```js
const 
  foo = 1,
  bar = 2
```

### Using the Operaton API

Please have a look at [Using the API](Using-the-API.md) for detailed information.

### Templates

#### View Component

API call:

```js

const Instances = () => {
  const
      state = useContext(AppState),
      { params } = useRoute()

  // call the core, access its result in a child component
  void engine_rest.get_process_instances(state, params.definition_id)

  // use the conditional (ternary) operator inside JSX code blocks
  return !params?.selection_id
          ? (<table class="fade-in">
            <thead>
              <tr>
                <th>ID</th>
                <th>Start Time</th>
                <th>State</th>
                <th>Business Key</th>
              </tr>
            </thead>
            <tbody>
              <InstanceTableRows/>
            </tbody>
          </table>)
          : (<InstanceDetails/>)
}

```

Naming instead of comments:

```js
const ProcessDiagram = () => {
  const
    { process_definition_diagram } = useContext(AppState),
    { params } = useRoute(),
    // describe complex evaluations inside the scope of the function with a good name
    show_diagram =
      process_definition_diagram.value !== null &&
      params.definition_id !== undefined

  return <div id="preview" class="fade-in">
    {show_diagram
      ? <ReactBpmn
          diagramXML={process_definition_diagram.value.bpmn20Xml}
          onLoading={null}
          onShown={null}
          onError={null} />
      : 'Select Process Definition'}
  </div>
}
```

Forms:

```js

const CreateUserPage = () => {
  // https://preactjs.com/guide/v10/forms/
  const
          state = useContext(AppState),
          { user_create, user_create_response } = state

  const set_value = (k1, k2, v) => user_create.value[k1][k2] = v.currentTarget.value
  const set_p_value = (k, v) => set_value('profile', k, v)
  const set_c_value = (k, v) => set_value('credentials', k, v)

  const on_submit = e => {
    e.preventDefault()
    user_create_response.value = engine_rest.create_user(state)
    // e.currentTarget.reset(); // Clear the inputs to prepare for the next submission
  }

  return <div>
    <h2>Create New User</h2>
    {(user_create_response.value !== undefined)
            ? user_create_response.value.success
                    ? <p class="success">Successfully created new user.</p>
                    : <p class="error">Error: {user_create_response.value?.message}</p> : null}
    <form onSubmit={on_submit}>
      <label for="user-id">User ID</label>
      <input id="user-id" type="text" onInput={(e) => set_p_value('id', e)} required/>

      <label for="password1">Password</label>
      <input id="password1" type="password" onInput={(e) => set_c_value('password', e)} required/>

      <label for="password2">Password (repeated)</label>
      <input id="password2" type="password" onInput={(e) => set_c_value('password', e)}/>

      <label for="first-name">First Name</label>
      <input id="first-name" type="text" onInput={(e) => set_p_value('firstName', e)} required/>

      <label for="last-name">Last Name</label>
      <input id="last-name" type="text" onInput={(e) => set_p_value('lastName', e)} required/>

      <label for="email">Email</label>
      <input id="email" type="email" onInput={(e) => set_p_value('email', e)} required/>

      <div class="button-group">
        <button type="submit">Create New User</button>
        <a href="/admin/users" class="button secondary">Cancel</a>
      </div>
    </form>
  </div>
}
```

### engine_rest.jsx

Each endpoint has an identical structure:

```js
// engine_rest.jsx

export const get_process_definitions = (state) =>
  fetch(`${_url_engine_rest(state)}/process-definition/statistics`)
    .then(response => response.json())
    .then(json => state.process_definitions.value = json)
```

```js
// state.js

const createAppState = () => {
  const server = signal(localStorage.getItem("server") || JSON.parse(import.meta.env.VITE_BACKEND)[0])
  const process_definitions = signal(null)
  // ...
  // add your new state signal here

  return {
    server,
    process_definitions,
    // ...
    // make your new state signal available
  }
}
```

- The JS name of the endpoint is always `{method}_{endpoint_name}`
- Prepend the URL string by using the `${_url(state)}` pattern. We need to fetch
  the selected server URL from the environment variable (config)
- Always assign the result to a state value
- Use the `export` keyword with the definition of your function, instead of
  exporting a map at the end. This is to prevent merge conflicts.

```js
const url_params = (definition_id) =>
  new URLSearchParams({
    unfinished: true,
    sortBy: 'startTime',
    sortOrder: 'asc',
    processDefinitionId: definition_id,
  }).toString()

export const get_process_instances = (state, definition_id) =>
  fetch(`${_url_engine_rest(state)}/history/process-instance?${url_params(definition_id)}`)
    .then(response => response.json())
    .then(json => (state.process_instances.value = json))
```

- In case you need params in your URL, use the `URLSearchParams` and keep it
  private (no `export`)
- Place your params object before the corresponding endpoint function

## Preact Specifics

- Use [Signals](https://preactjs.com/guide/v10/signals/) instead of hooks

## CSS

There is currently no proper document to give an overview of the CSS in this project.

If you add CSS you can just dump it in an additional new file and when merging the 
PR it will get cleaned up by the maintainers. 

- Split code in multiple files
- Prefer classless styling to classes
- Prefer cascading styles with few additional classes instead of many single
  classes
- Create reusable CSS, refactor when necessary