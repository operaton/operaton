# Accessibility

We want the front-end of Operaton, its web app, be as accessible as possible.
There is no perfect accessibility, yet we strive to implement as many
accessibility features as possible and a solid baseline of accessibility.

This file presents a collection of references for implementing accessibility on
the web and specific issues concerning the Operaton front-end applications.

## Knowledge Sources

Official site for web standards: https://www.w3.org/WAI/ARIA/apg/
Mozilla MDN: https://developer.mozilla.org/en-US/docs/Web/Accessibility

Getting started & basics:

- https://www.w3.org/WAI/ARIA/apg/practices/landmark-regions/
- https://www.w3.org/WAI/ARIA/apg/practices/names-and-descriptions/

### Keyboard Navigation:

- https://www.w3.org/WAI/ARIA/apg/practices/keyboard-interface/
- https://developer.mozilla.org/en-US/docs/Learn/Tools_and_testing/Client-side_JavaScript_frameworks/React_accessibility

## Issues

### Split Layout

The split layout pattern can be used when presenting the user with the processes
or tasks page.
An issue is the correct implementation, which isn't properly defined (see GitHub
issues for more information).

https://www.w3.org/WAI/ARIA/apg/patterns/windowsplitter/
https://github.com/w3c/aria-practices/issues/130
https://github.com/w3c/aria-practices/issues/129

An alternative would be a layout, which is controlled by buttons presenting only
a select amount of states (hidden, min, max). This leads to less user
adjustment, but can circumvent the accessibility and some UX issues.

## Testing

Accessibility testing shall be part of automated browser tests. This is
important for checking features like keyboard navigation.

Additionally, we want to use pa11y-ci in the build pipeline to statically test
the resulting HTML/CSS according to the latest WCAG guidelines.

Lastly, it is important to manually test the web apps. This means using
assistive technology like screen-readers. If possible we have a test group with 
personal experience using assistive tools, who can provide feedback as users.   