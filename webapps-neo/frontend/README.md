# Operaton BPM Web Apps

[Forum](https://forum.operaton.org) | [Slack](https://operaton.org/chat) | [Website](https://operaton.org)

The accompanying web apps for the [Operaton](https://github.com/operaton/operaton) an open-source BPMN system.

![operaton process page](/docs/screenshots/operaton-processes-page.png)

## First use

You require the following software to run the app

- [Preact & Vite Setup](https://preactjs.com/guide/v10/getting-started#create-a-vite-powered-preact-app)
  - An up-to-date version of `node.js` 
- An Operaton API for the backend to consume
  - With Docker Compose run `docker compose up -d`, or
  - With Podman Compose (needs to be [installed on top of Podman](https://github.com/containers/podman-compose)) run `podman compose up -d`
  - Or with Maven ([example](https://github.com/javahippie/operaton-spring-boot-example))
- Create a `.env.development.local` file in the root of this project and configure your backend as shown [here](./docs/Environment%20Variables.md)

## Running the app

- `npm run dev` - Starts a dev server at http://127.0.0.1:5173
  - **IMPORTANT:** instead of localhost use http://127.0.0.1:5173 to avoid CORS issues! 
  - Login with `demo`/`demo` as user/password
- `npm run build` - Builds for production, emitting to `dist/`
- `npm run preview` - Starts a server at http://localhost:4173/ (maybe 127.0.0.1 as well to avoid CORS, not tested yet) to test production build locally

## Dev fixtures

To populate the engine with realistic data — synthetic BPMN/DMN processes plus
a load-generating bot that can spawn anywhere from a handful to 100 000
instances — see [`dev-fixtures/README.md`](./dev-fixtures/README.md). The
quickest path is `docker compose -f docker-compose.dev-fixtures.yaml up --build`,
which brings up the engine, the bot, and a control panel at
<http://localhost:3001> with buttons for deploy / spawn / stress.

## Testing

Unit and component tests use [Vitest](https://vitest.dev/) with
[`@testing-library/preact`]; browser end-to-end tests use
[Playwright](https://playwright.dev/).

- `npx vitest run` - Runs all unit/component tests once
- `npm test` - Runs the unit/component tests in watch mode
- `npx vitest run src/path/to/file.test.js` - Runs a single test file
- `npm run test:coverage` - Runs the unit/component tests with a coverage report
- `npm run test:e2e` - Runs the Playwright end-to-end tests

The e2e tests need the backend running (`docker compose up`, or `podman compose
up`) and drive the dev server, which they start automatically. They log in with
`demo`/`demo`. Use `npx playwright test --ui` to watch the flows interactively.

## Documentation

Documentation for the Operaton web apps can currently be found inside the [`/docs`](./docs/) folder.

## Contributing

Please refer to the [Contributing.md](docs/Contributing.md) file for detailed information.
As well have a look at [Coding Conventions.md](docs/Coding%20Conventions.md).

### Join the community

Sometimes the GitHub issues aren't the best place to discuss some topics. Hence we provide a forum and a Slack channel as well:

- [forum.operaton.org](https://forum.operaton.org)
- [Slack](https://operaton.org/chat) (#webapps channel)

## Screenshots

![operaton process page](/docs/screenshots/operaton-tasks-page.png)
![operaton process page](/docs/screenshots/operaton-global-search.png)

## License

The source files in this repository are made available under the Apache License Version 2.0.

OPERATON uses and includes third-party dependencies published under various licenses. By downloading and using OPERATON artifacts, you agree to their terms and conditions. Refer to our license-book.txt for an overview of third-party libraries and particularly important third-party licenses we want to make you aware of.
