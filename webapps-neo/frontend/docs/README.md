# Technical Overview of the Operaton Web App

## Technical Decisions

- We use Preact as the single page application framework
  - We use Signals for state management
  - We use vanilla CSS for styling
  - We MAYBE use JSX files for inline HTML (and therefore depend on )
  

## Structure

### Pages

Inside the `src/pages` folder you'll find the main screens as seen in the main navigation bar of the app.

### CSS Styling

We use vanilla CSS to style all our elements in `src/css`.

- `main.css` defines global style for native HTML elements
- `vars.css` contains all the variables as core of our styling system
- `layout.css` takes care of the layout grid for all pages
- `fonts.css` imports fonts
- `form.css` defines all styles for form components in
- `components.css` / `components/` contains the styling for custom components which are not part of the native HTML elements