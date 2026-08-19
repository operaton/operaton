# Contributing

To the Operaton web apps aka. the front-end.

## Getting started

1. Either create your own issue on [GitHub](https://github.com/operaton) or claim an existing one (for an
   easier start look for the `good first issue` tag)
2. Make sure to install all required tooling as stated in
   the [Readme.md](/README.md)
3. Read the rest of this document
4. Read the [Coding Conventions](Coding%20Conventions.md)
5. If necessary, ask for help either
   - in the GitHub issue related to your contribution, or
   - in the [Forum](https://forum.operaton.org), or
   - in the [Chat](https://chat.operaton.org) (TBD)

## Using the old web apps as an example

You can access the old C7 (Camunda 7) web apps when starting the backend with the 
docker-compose file:

- Operaton: `localhost:8084`
- C7: `localhost:8088`

For both, the login information are:

- user: `demo`
- password: `demo`

## Tools and their Documentation

This project uses:

- [Preact](https://preactjs.com/guide/v10/getting-started)
    - [preact-iso (routing)](https://github.com/preactjs/preact-iso)
    - [Preact Signals](https://preactjs.com/guide/v10/signals/)
- [react-bpmn](https://github.com/bpmn-io/react-bpmn)
- [Vite](https://vite.dev/guide/)
- [Operaton API (currently C7 docs)](https://docs.camunda.org/rest/camunda-bpm-platform/7.22/#)

## Directory Structure

Most important folders and files for getting started with development.

- `docs`: project related documentation and information
- `src`: code and resources
    - `assets`: resources
    - `components`: reusable Preact/JavaScript UI components
    - `css`: vanilla CSS for entire project (see below)
    - `helper`: reusable JavaScript code
    - `pages`: a dedicated page for every 'app'
    - `engine_rest.jsx`: all fetch requests to the Operaton REST API
    - `index.js`: Preact main, routing definitions and global app state
    - `state.js`: Global app state definition
- `.env*`: Environment variables

### CSS structure

- `components`: Folder for separate component specific CSS files
- `animation.css`: Everything related to animations
- `components.css`: File for small component specific CSS (could be refactored
  into folder)
- `fonts.css`: Custom fonts and text styling
- `form.css`: Everything related to `<form>` elements and its children
- `layout.css`: General layout and generic layout helper classes
- `normalize.css`: Style overwrite for default HTML tags
- `variables.css`: Custom properties for reuse in other CSS classes 