# operaton-bpm-sdk-js

Javascript client library for [Operaton](https://github.com/operaton/operaton)

## Install using bower

```sh
bower install operaton-bpm-sdk-js --save
```

## Documentation

See https://docs.operaton.org/manual/latest/reference/embedded-forms/

## Development

The SDK is built and tested through the parent `webapps/frontend` package.
Run these commands from that directory:

```sh
npm ci
npm run build
```

### Testing

```sh
npm run test:sdk
```

This runs every Node SDK spec in `test/client/**/*Spec.js`. The existing
pending resource examples remain visible in the Mocha report. `npm test`
runs this suite followed by the webapp UI suite, which requires Chrome.
The historical SDK browser specs are not wired into the current UI harness.

The REST mock in `test/superagent-mock-config.js` is restored verbatim from
[the parent of the historical removal commit](https://github.com/operaton/operaton/commit/0aa166c85e9ee9cb0863ecae60da587c01753e72),
where it lived at `webapps/camunda-bpm-sdk-js/test/superagent-mock-config.js`.
Its original license and attribution are preserved. The published
`fixturer@0.0.1` implementation and package metadata match the original
Camunda fork at `e17687b7c78c51ff09b7b187a7641e77bfc2745e`; the fixture needs
UUID's legacy callable API. Test-only dependencies are pinned in the parent
package and lockfile.

### Issues

https://app.operaton.com/jira/browse/CAM/component/12351

## Releasing

### Release

To create a release:

```sh
grunt publish:release --setversion='myReleaseVersion'
```

This will update the version, commit and tag it, then publish it to [bower-operaton-bpm-sdk-js](https://github.com/camunda/bower-camunda-bpm-sdk-js)

### Snapshot

To create a snapshot release which just builds current head and publishes it to [bower-operaton-bpm-sdk-js](https://github.com/camunda/bower-camunda-bpm-sdk-js) on a branch named the current version:

```sh
grunt publish:snapshot
```

### Version

If you just want to update the current version:

```sh
grunt publish:version --setversion='myNewVersion'
```

### Available options

- --no-bower -> skip bower release
- --no-write -> dryRun mode

### Examples

- [standalone usage](https://github.com/camunda/camunda-bpm-examples/tree/master/sdk-js)

### Contributing

You are **more than welcome** to take part on the development of this project!

#### Coding

Clone the repository, add, fix or improve and send us a pull request.
But please take care about the commit messages, [our conventions can be found
here](https://github.com/operaton/operaton/blob/main/CONTRIBUTING.md).

#### Coding style guide

In place of a guide, just follow the formatting of existing code :-)

## License

The source files in this repository are made available under the [Apache License Version 2.0](./LICENSE).
