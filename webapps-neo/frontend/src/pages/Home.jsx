import { useContext } from "preact/hooks";
import engine_rest from "../api/engine_rest.jsx";
import { AppState } from "../state.js";

export const Home = () => (
  <div class="p-3">
    <h2>Welcome to Operaton!</h2>
    <p>
      You can find general information on operaton on <a href="https://operaton.org">operaton.org</a>.
    </p>

    <h3>Help</h3>
    <p>If you are not sure how to use Operaton or its web apps, start by taking a look at our help pages:</p>
    <ul>
      <li>
        <a href="/help">Help Page</a>
      </li>
      <li>
        <a href="http://operaton.org/getting-started">Getting Started</a>
      </li>
    </ul>

    <h3>Web Apps</h3>
    <p>You are currently using the integrated web apps of Operaton.</p>

    <h3>Community</h3>
    <p>
      To visit the community forum to discuss features and find help. If you like to contribute to the project, check
      out our public git repository:
    </p>
    <ul>
      <li>
        <a href="https://forum.operaton.org">Forum</a>
      </li>
      <li>
        <a href="https://github.com/operaton/operaton">GitHub</a>
      </li>
    </ul>

    <h4>Reporting a bug</h4>
    <p>If you encountered an error while using Operaton, please create a ticket to let us know:</p>
    <ul>
      <li>
        <a href="https://github.com/operaton/operaton/issues">Create issues on GitHub</a>
      </li>
    </ul>

    <h4>Requesting features</h4>
    <p>
      If you miss some functionality while using Operaton, you can communicate this as well. Please keep in mind, that
      features which a very specific to an organization might be better added as an extension, not as part of the
      official community code.
    </p>
    <ul>
      <li>
        <a href="https://forum.operaton.org">Discuss feature ideas on the forum</a>
      </li>
      <li>
        <a href="https://github.com/operaton/operaton/issues">Create a feature request on GitHub</a>
      </li>
    </ul>
  </div>
);
