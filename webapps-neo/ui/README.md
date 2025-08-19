# Operaton BPM Web Apps

[Forum](https://forum.operaton.org) | [Slack](https://operaton.org/chat) | [Website](https://operaton.org)

The accompanying web apps for the [Operaton](https://github.com/operaton/operaton) an open-source BPMN system.

![operaton process page](/docs/screenshots/operaton-processes-page.png)

## First use

You require the following software to run the app

- [Preact & Vite Setup](https://preactjs.com/guide/v10/getting-started#create-a-vite-powered-preact-app)
  - An up-to-date version of `node.js` 
- An Operaton API for the backend to consume
  - With Docker Compose run `docker compose up -d`
  - Or with Maven ([example](https://github.com/javahippie/operaton-spring-boot-example))
- Create a `.env.development.local` file in the root of this project and configure your backend as shown [here](./docs/Environment%20Variables.md)

## Running the app

- `npm run dev` - Starts a dev server at http://127.0.0.1:5173
  - **IMPORTANT:** instead of localhost use http://127.0.0.1:5173 to avoid CORS issues! 
- `npm run build` - Builds for production, emitting to `dist/`
- `npm run preview` - Starts a server at http://localhost:4173/ (maybe 127.0.0.1 as well to avoid CORS, not tested yet) to test production build locally

## Documentation

Documentation for the Operaton web apps can currently be found inside the [`/docs`](./docs/) folder.

## Contributing

Please refer to the [Contributing.md](docs/Contributing.md) file for detailed information.
As well have a look at [Coding Conventions.md](docs/Coding%20Conventions.md).

### Join the community

Sometimes the GitHub issues aren't the best place to discuss some topics. Hence we provide a forum and a Slack channel as well:

- [forum.operaton.org](https://forum.operaton.org)
- [Slack](https://operaton.org/chat)

## Screenshots

![operaton process page](/docs/screenshots/operaton-tasks-page.png)
![operaton process page](/docs/screenshots/operaton-global-search.png)

## License

The source files in this repository are made available under the Apache License Version 2.0.

OPERATON uses and includes third-party dependencies published under various licenses. By downloading and using OPERATON artifacts, you agree to their terms and conditions. Refer to our license-book.txt for an overview of third-party libraries and particularly important third-party licenses we want to make you aware of.
