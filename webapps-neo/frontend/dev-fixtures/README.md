# dev-fixtures

Synthetic processes and a load-generating bot for local Operaton development.
The aim is twofold:

- expose every BPMN 2.0 feature you'd plausibly meet in real projects, so the
  web-app code paths get exercised, and
- run thousands of instances on demand, so the dashboard, diagrams, and queries
  feel real instead of artisanal.

```
dev-fixtures/
├── processes/                    BPMN 2.0 files
│   ├── order-fulfillment.bpmn        service tasks · gateways · user task
│   ├── insurance-claim.bpmn          subprocess · boundary timer + error · DMN · compensation
│   ├── loan-approval.bpmn            message start · multi-instance · call activity · signal
│   ├── document-review.bpmn          event-based gateway · timer/message/signal race
│   └── risk-assessment.bpmn          (called by loan-approval)
├── rules/
│   └── claim-routing.dmn             called by insurance-claim
└── bot/                          Node 20+ load generator and CLIs
    ├── deploy.js                     deploy all *.bpmn / *.dmn
    ├── bot.js                        long-running worker bot
    ├── spawn.js                      manual instance creation
    ├── stress.js                     load profiles
    ├── config.json                   tunable knobs
    └── lib/                          internals (engine client, RNG, topics)
```

## Quickstart — pick a flavour

### A. All-in-one Docker (engine + bot + control panel)

```sh
# in the repo root
docker compose -f docker-compose.dev-fixtures.yaml up --build
```

Then:

- Engine + REST → <http://localhost:8084>
- Web app dev server (separate, `npm run dev`) → <http://localhost:5173>
- **Control panel** → <http://localhost:3001>  (buttons for deploy / bot / spawn / stress)

### B. Engine in Docker, bot on the host (faster iteration on the bot)

```sh
docker compose up -d                   # the existing engine-only compose
cd dev-fixtures/bot
npm run deploy
npm run bot                            # auto-spawns + workers + dispatcher
# or
npm run bot -- --no-spawner            # workers only, queue maintenance mode
# or
npm run bot -- --auto-deploy           # deploy + run, in one step
# or
npm run server                         # the same control panel as in (A), at :3000
```

## Spawning specific scenarios

```sh
# One Order Fulfillment with defaults
npm run spawn -- --process orderFulfillment

# Ten Loan Approval applications (started via message)
npm run spawn -- --message loan-application-received --count 10

# A specific scenario via vars
npm run spawn -- --process insuranceClaim --vars claimType=health,amount=12000
```

`--vars` accepts comma-separated `key=value`. Values that look like integers,
floats or `true`/`false` are typed accordingly; everything else is a string.

## Stress testing

Default profile is `default` (1000 instances, rampup):

```sh
npm run stress
```

Presets and overrides:

| flag                         | effect                                                  |
| ---------------------------- | ------------------------------------------------------- |
| `--preset tiny`              | 50 instances                                            |
| `--preset small`             | 100                                                     |
| `--preset default`           | 1000  *(default)*                                       |
| `--preset big`               | 10 000                                                  |
| `--preset huge`              | 100 000  (bring snacks)                                 |
| `--count N`                  | custom count, implies `--preset custom`                 |
| `--mode burst`               | fire as fast as possible, then exit                     |
| `--mode rampup` *(default)*  | linearly increase rate over `--duration`                |
| `--mode soak`                | keep ~N concurrent for `--duration`, topping up         |
| `--duration 10m`             | rampup/soak window (`s`/`m`/`h` suffix)                 |
| `--rate 60/min`              | spawn-rate cap                                          |
| `--process orderFulfillment` | restrict to one process key (default: weighted mix)     |

Examples:

```sh
npm run stress -- --preset big --mode burst
npm run stress -- --count 5000 --mode rampup --duration 15m
npm run stress -- --preset big --mode soak --duration 1h
```

## Reproducibility

Every random choice is forked off `config.seed`. Same seed + same code = same
sequence of process starts, completion delays, failure injections. Change the
seed in `config.json` if you want a fresh shuffle. The `spawn` and `stress`
CLIs xor the seed with `Date.now()` so manual runs don't collide with the bot's
deterministic stream.

## Control panel

`server.js` exposes the bot as a tiny HTTP service with a one-page UI under
`bot/public/`. Buttons map onto the CLIs:

| Button         | What runs                       |
| -------------- | ------------------------------- |
| Deploy         | `node deploy.js`                |
| Start bot      | `node bot.js [--auto-deploy]`   |
| Stop bot       | `SIGTERM` to the bot child      |
| Spawn          | `node spawn.js …`               |
| Stress         | `node stress.js …`              |
| Cancel stress  | `SIGTERM` to the stress child   |

Recent jobs and their stdout are kept in memory (max 50) so you can scroll
back. Output for any job: `GET /api/jobs/<id>`.

## Environment variables

These override `config.json` (handy in containers):

| var                              | overrides           |
| -------------------------------- | ------------------- |
| `DEV_FIXTURES_ENGINE_URL`        | `engine.url`        |
| `DEV_FIXTURES_ENGINE_USERNAME`   | `engine.auth.username` |
| `DEV_FIXTURES_ENGINE_PASSWORD`   | `engine.auth.password` |
| `DEV_FIXTURES_SEED`              | `seed`              |
| `PORT`                           | server port (3000)  |

## Knobs in `config.json`

```jsonc
{
  "engine": { "url": "http://localhost:8084/engine-rest", "auth": { "username": "demo", "password": "demo" } },
  "seed": 42,
  "businessKeyPrefix": "loadgen-",                    // every started instance carries this prefix
  "spawner": {
    "instancesPerMinute": 60,                          // bot's auto-spawn rate
    "weights": { "orderFulfillment": 50, "loanApproval": 25, ... }
  },
  "userTaskCompleter": {
    "pollIntervalMs": 2000,
    "completionDelayMedianMs": 8000,                   // log-normal sample around this median
    "completionDelaySigma": 1.4,                       // wider = more variance
    "stallProbability": 0.05                           // % of tasks the bot leaves alone
  },
  "externalTask": {
    "lockDurationMs": 30000,
    "maxTasksPerFetch": 20,
    "pollIntervalMs": 500,
    "failureRatePerTopic": { "default": 0.01, "credit-check": 0.02, "charge-payment": 0.03 }
  },
  "messageDispatcher": {
    "intervalMs": 5000,
    "messages": {
      "loan-application-received": { "weight": 1, "vars": { "amount": "random:int:1000:50000", "credit": "random:enum:good,fair,poor" } }
    }
  }
}
```

`random:` value-spec mini-language:

- `random:int:lo:hi`
- `random:float:lo:hi`
- `random:enum:a,b,c`
- `random:bool[:p]`

Anything else is passed through as a literal value.

## Cleaning up

The bot tags every instance with `loadgen-...` in its business key, so you can
filter generated noise in the dashboard. To wipe everything generated:

```sh
# All running instances (any process)
curl -u demo:demo -X POST http://localhost:8084/engine-rest/process-instance/delete \
  -H 'Content-Type: application/json' \
  -d '{"processInstanceQuery":{}}'
```

Add `processDefinitionKey` to the query if you want to be more surgical.

## What this purposefully does *not* cover (yet)

- Ad-hoc subprocesses, transactions with cancel boundaries, complex gateways —
  rarely used, easy to add when needed.
- Tenant or authorization scenarios — orthogonal, separate concern.
- Migrations — the existing migration page already handles that.
