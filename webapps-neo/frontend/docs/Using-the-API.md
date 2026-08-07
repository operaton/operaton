# Using the API

The Operaton API is implemented in [`/src/api/`](../src/api/), with [`engine_rest.jsx`](../src/api/engine_rest.jsx) as its core.

Currently, we can refer to the [Camunda 7 REST API](https://docs.camunda.org/rest/camunda-bpm-platform/7.23-SNAPSHOT/)
to see which endpoints are available.

To get an initial overview on how we implement this API, read this document first.
For detailed documentation on the implemented endpoints, also check out the doc-strings on the functions themselves
on the shown functions below, have a look at the docstrings inside the file.

## Using the implemented endpoints in your JSX code 

We want to adhere to a common pattern when extending and using the API:

```jsx
import engine_rest, { RequestState } from '../api/engine_rest.jsx'
import { AppState } from '../state.js'

const ParentComponent = () => {
    void engine_rest.process_definition.list(state) // [1]
    
    return <ChildComponent />
}

const ChildComponent = () => {
    const
        { api: { directory: { some_value } } } = useContext(AppState) // [2]

    return <div>
        <h1>Some data</h1>
        <RequestState {/* [3.1] */}
            signl={some_value} {/* [3.2] */}
            on_success={() => // [3.3]
                some_value.value?.data?.map(process =>
                    <SomeItem key={process.id} {...process} />)
            } />
    </div>
}
```

1. We need to initiate the fetching of the resource in a parent component by utilising `engine_rest`. This prevents re-rendering and enables Preact to manage the state properly
2. In the child component we extract the required signal from the global `AppState` state object to use it in (3)
3. `<RequestState ... />`
   1. The `ReqestState` component checks the state of a given signal and displays loading, error or success values.
   2. The `signl` parameter gets a reference to the signal from our global state to listen to
   3. The `on_success` parameter requires a function which then displays a custom result, using the state value defined in (2) for the `signl` parameter

**Note**: the `<RequestState />` component has additional optional parameters for `on_error` and `on_loading` if a custom
message needs to be shown. They require, like `on_success`, an anonymous function to work properly.


## Extending the API

We use generic wrappers around the standard JavaScript `fetch` function, defined in the [`helper.jsx`](../api/helper.jsx) file:

- GET
- POST
- PUT
- DELETE

```js
export const some_api_function = (state) =>
    GET('/foo/bar', state, state.api.directory.some_value)
```

For `GET` we only need the `state` as a parameter to set the resulting value.

```js
const some_other_api_function = (state, an_id) =>
    POST(`/foo/${an_id}/bar`, { id: state.some.value.id }, state, state.api.directory.some_other_value)
```

The other methods additionally can use an object representing the `body` of an HTTP request.