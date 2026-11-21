#!/usr/bin/env node

const {dirname, join} = require("path");

const {
    readFileSync,
    writeFileSync
} = require("fs");

const SPEC_PATH = join(__dirname, "target", "dependency", "openapi.json");

// Redoc 3 no longer offers server-side rendering, the page is rendered in the browser by the standalone bundle
async function generateDocs() {
    const spec = JSON.parse(readFileSync(SPEC_PATH, "utf8"));

    const redocJs = readFileSync(
        join(dirname(require.resolve("redoc/package.json")), "bundles", "redoc.standalone.js")
    ).toString().replace(/<\/script/gi, "<\\/script");

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

    <script type="module">${redocJs}
    window.Redoc = {init};
    </script>
</head>

<body>
<div id="redoc"></div>
<script type="module">
    const spec = ${JSON.stringify(spec).replace(/</g, "\\u003c")};
    window.Redoc.init(spec, {}, document.getElementById('redoc'));
</script>
</body>
</html>`

    writeFileSync(join(__dirname, 'target', 'index.html'), page);
}

generateDocs();
