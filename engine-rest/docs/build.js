#!/usr/bin/env node

const {createElement} = require("react");
const {renderToString} = require("react-dom/server");

const {ServerStyleSheet} = require("styled-components");
const {dirname, join} = require("path");

const {
    readFileSync,
    writeFileSync
} = require("fs");

const {Redoc, createStore, loadAndBundleSpec} = require("redoc");

const SPEC_PATH = "docs/target/dependency/openapi.json";

async function generateDocs() {
    const spec = await loadAndBundleSpec(SPEC_PATH);
    const store = await createStore(spec, null, {});

    const styleSheet = new ServerStyleSheet();
    const html = renderToString(
        styleSheet.collectStyles(createElement(Redoc, {store}))
    );

    const redocJs = readFileSync(
        join(dirname(require.resolve("redoc")), "redoc.standalone.js")
    );

    const state = await store.toJS();

    const operatonVersion = require('./package.json').version;

    const page = `<!DOCTYPE html>
<html>
<head>
    <meta charset="utf8"/>
    <title>Operaton Automation Platform ${operatonVersion} REST API</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta http-equiv="Content-Security-Policy"
          content="default-src 'self'; script-src 'unsafe-inline' blob:; style-src 'unsafe-inline' 'self'; img-src 'self' data:;">
    <link rel="shortcut icon" href="/manual/latest/img/favicon.ico" type="image/x-icon">
    <style>
        @import url('/manual/latest/fonts/IBMPlexSans-Regular.ttf');
        @import url('/manual/latest/fonts/IBMPlexSans-Italic.ttf');

        body {
            padding: 0;
            margin: 0;
        }

        * {
            font-family: 'IBM Plex Sans', Helvetica, Arial, Verdana, sans-serif!important;
        }
    </style>

    ${styleSheet.getStyleTags()}

    <script>${redocJs}</script>
</head>

<body>
<div id="redoc">${html}</div>
<script>
    var container = document.getElementById('redoc');
    Redoc.hydrate(${JSON.stringify(state)}, container);
</script>
</body>
</html>`

    writeFileSync('./target/index.html', page);
}

generateDocs();
