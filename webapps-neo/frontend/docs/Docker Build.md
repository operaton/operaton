# Docker Build

To make the web apps more available, we provide docker images as standalone 
version which is able to connect to any Operaton and Camunda 7 backend where 
the REST API is enabled.

The build process depends on the following files:

- `Dockerfile`: Complies the app with node and copies the resulting static files in a nginx container to create a lightweight image
- `.env.production`: Defines placeholder environment variables in the compiled for the `docker build` step
- `.env.sh`: Script for replacing the environment variables of the compiled vite app. Gets executed before `docker run`


## Creating the image

1. Install docker on your device
2. Clone the repository
3. Run `docker build . -t "operaton-webapps-standalone:{a-version-number}`

Make sure the version numbers is higher than in the previous release, if you 
want to make it public.

## Deploying to Docker Hub with GitHub Actions

TBD

## Running the image

In a terminal run 

```bash
docker run -p {a-port}:80 --env-file {a-env-file} operaton-webapps-standalone:{a-version}
```

Replace:

- {a-port} with your desired port. If you want to run on :80, remove `-p {a-port}:80` from the command
- {a-env-file} either use a `.env` file or supply the environment variables with `-e` one-by-one
- {a-version} choose a version tag – either from docker hub, if available, or the version you defined while creating the image locally {a-version-number}

## Serving from a sub-path

By default the image serves the app from `/`. Set
`DOCKER_RUN_PLACEHOLDER_APPLICATION_PATH` to serve it from somewhere else:

```bash
docker run -p 8080:80 \
  -e ASSET_DIR=/var/www/html \
  -e APP_PREFIX=DOCKER_RUN_PLACEHOLDER_ \
  -e DOCKER_RUN_PLACEHOLDER_APPLICATION_PATH=/app-neo \
  operaton-webapps-standalone:{a-version}
```

`env.sh` then rewrites the `<base href>` in `index.html` and the location blocks
in `nginx.conf` before nginx starts. `/` redirects to the application path, and
the bundle keeps living at the web root — the prefix is stripped by nginx rather
than baked into the file layout, so the image is the same either way.