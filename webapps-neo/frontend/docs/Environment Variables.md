# Environment Variables

We use Vite as a build tool: https://vite.dev/guide/env-and-mode  

## Overview on our Env Vars

### VITE_BACKEND

A list of possible backends a user can switch between when using the
application.
The data is a JSON list with objects consisting of a `name` and `url` string. If you use Camunda 7 (C7) as a backend, you need to additionally set `c7_mode` to true. 

```JSON
[
  {
    "name": "Operaton Local Dev",
    "url": "http://localhost:8080"
  },
  {
    "name": "C7 Prod",
    "url": "https://processes.example.com",
    "c7_mode": true
  }
]
```


The URL string has the following structure:  
`{http|https}` + `://` + `{your.domain}` + `{port|_}`

E.g.:

- `http://localhost:8080`
- `https://operaton.example.com`

The resulting entry in the `.env`-files can look like the following

```properties
# .env.development
VITE_BACKEND=[{"name": "Dev Operaton", "url": "http://localhost:8084"}, {"name": "Dev c7", "url": "http://localhost:8088", "c7_mode": true}]
```

> **Important**: This config works with the `docker-compose.yaml` setup and is also 
> already set in `.env.development`. You can use the `.env.development.local` file 
> to change this config to you preferences. For IntelliJ users this is also provided 
> as run configuration.

