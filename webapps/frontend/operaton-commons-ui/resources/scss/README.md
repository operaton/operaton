# Operaton commons styling

## Scaffolding

In this folder you can find 3 sub-folders:

 - `sites`: in which styles for sites like documentation, blog, presence, … are defined
 - `shared`: contains other files which _might_ relevant for web applications or web sites but are not imported by default


## File name convention

Files starting with a `_` are aimed to provide variables or mixins only (importing them will not output anything).


## Best practices

In your project, you should use a `_vars.scss` file which imports the [`shared/_variables.scss`](./shared/_variables.scss) of this project.

```scss
// note: you may need to adapt this path depending on where your `_vars.scss` is
@import "node_modules/operaton-commons-ui/resources/scss/shared/_variables.scss";

// override the default `@main-color` color defined in `shared/_variables.scss`
$main-color: #7fa;

// add custom variables for your project
$custom-variable: 10px;
```

Then, you will have a `styles.scss` (which will probably be compiled as `styles.css`).

```scss
@import "./_vars.scss";

// adapt the path if / as needed
@import "node_modules/operaton-commons-ui/resources/scss/shared/base.scss";
```
