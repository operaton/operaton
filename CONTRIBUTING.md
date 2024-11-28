# How to contribute

* [Ways to contribute](#ways-to-contribute)
* [Browse our issues](#browse-our-issues)
* [Build from source](#build-from-source)
* [Create a pull request](#create-a-pull-request)
* [Contribution checklist](#contribution-checklist)
* [Commit message conventions](#commit-message-conventions)
* [Review process](#review-process)

# Ways to contribute

We would love you to contribute to this project. You can do so in various ways.

If you are unsure about anything, have a question, or just want to talk about the project, please join our [forum](https://forum.operaton.org/) or [Slack channel](https://join.slack.com/t/operaton/shared_invite/zt-2v6umjt92-d2DRmsoR1fqDEVlJB5IkNA).

## File bugs or feature requests

Found a bug in the code or have a feature that you would like to see in the future? [Search our open issues](https://github.com/operaton/operaton/issues) if we have it on the radar already or [create a new issue otherwise](https://github.com/operaton/operaton/issues/new/choose).

Please note, that our main goal after the fork is to provide minimal maintenance and ensure stability. We appreciate feature requests, but might decide not to merge them into the application in the first months.

Try to apply our best practices for creating issues:

* Only Raise an issue if your request requires a code change in Operaton
  * If you have an understanding question or need help building your solution, check out our [user forum](https://forum.operaton.org/).
* Create a high-quality issue:
  * Give enough context so that a person who doesn't know your project can understand your request
  * Be concise, only add what's needed to understand the core of the request
  * If you raise a bug report, describe the steps to reproduce the problem
  * Specify your environment (e.g. Operaton version, Operaton modules you use, ...)
  * Provide code. For a bug report, create a test that reproduces the problem. For feature requests, create mockup code that shows how the feature might look like. Fork our [unit test Github template](https://github.com/operaton/operaton-engine-unittest) to get started quickly.
  * Your time is valuable, so is ours. Please respect that we might not be able to work on your request immediately. We will try to give you feedback as soon as possible. 
    Please help us to understand your request fast by providing as much precise as possible.

## Write code

You can contribute code that fixes bugs and/or implements features. Here is how it works:

1. Select a ticket that you would like to implement. Have a look at [our backlog](https://github.com/operaton/operaton/issues) if you need inspiration. Be aware that some of the issues need good knowledge of the surrounding code.
1. Looking for some low hanging fruits? Check out the [good first issues](https://github.com/operaton/operaton/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22)
1. Tell us in the ticket comments that you want to work on your ticket. This is also the place where you can ask questions.
1. Check your code changes against our [contribution checklist](#contribution-checklist)
1. [Create a pull request](https://github.com/operaton/operaton/pulls). Note that you can already do this before you have finished your implementation if you would like feedback on your work in progress.


# Browse our issues

In this repository, we manage the [issues](https://github.com/operaton/operaton/issues) for the following Operaton code repositories and projects:

* https://github.com/operaton/operaton

We use [labels](https://github.com/operaton/operaton/labels) to mark and group our issues for easier browsing. We define the following label prefixes:

* `idea`: A suggestion for a new feature
* `bug`: Something isn't working
* `enhancement`: New feature or request
* `good first issue`: Good for newcomers
* `help wanted`: Extra attention is needed
* `question`: Further information is requested
* `documentation`: Improvements or additions to documentation
* `refactor`: Code refactoring
* `build`: Changes related to the build system, including Maven configurations, GitHub Actions workflows, etc.
* `dependencies`: Pull requests that update a dependency file
* `qa`: Tests, quality improvements and assurance
* `backport-c7`: Changes backported from Camunda 7
* `backport-cib7`: Changes backported from CIB seven
* `duplicate`: This issue or pull request already exists
* `invalid`: This will not be worked on
* `wontfix`: This will not be worked on

# Build from source

An entire repository can be built by running `mvn clean install` in the root directory.
This will build all sub modules and execute unit tests.
Furthermore, you can restrict the build to just the module you are changing by running the same command in the corresponding directory.
Check the repository's or module's README for additional module-specific instructions.
The `webapps` module requires NodeJS.
You can exclude building them by running `mvn clean install -pl '!webapps,!webapps/assembly,!webapps/assembly-jakarta'`.

Integration tests (e.g. tests that run in an actual application server) are usually not part of the default Maven profiles. If you think they are relevant to your contribution, please ask us in the ticket, on the forum or in your pull request for how to run them. Smaller contributions usually do not need this.

# Create a pull request

In order to show us your code, you can create a pull request on Github. Do this when your contribution is ready for review, or if you have started with your implementation and want some feedback before you continue. It is always easier to help if we can see your work in progress.

A pull request can be submitted as follows: 

1. [Fork the Operaton repository](https://docs.github.com/en/github/getting-started-with-github/fork-a-repo) you are contributing to
1. Commit and push your changes to a branch in your fork
1. [Submit a Pull Request to the Operaton repository](https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request-from-a-fork). As the *base* branch (the one that you contribute to), select `master`. This should also be the default in the Github UI.
1. In the pull request description, reference the github issue that your pull request addresses.

# Contribution checklist

Before submitting your pull request for code review, please go through the following checklist:

1. Is your code formatted according to our code style guidelines?
    * Java: Please check our [Java Code Style Guidelines](https://github.com/camunda/camunda-bpm-platform/wiki/Coding-Style-Java). You can also import [our template and settings files](https://github.com/operaton/operaton/tree/master/settings) into your IDE before you start coding.
    * Javascript: Your code is automatically formatted whenever you commit.
1. Is your code covered by unit tests?
    * Ask us if you are not sure where to write the tests or what kind of tests you should write.
    * Java: Please follow our [testing best practices](https://github.com/camunda/camunda-bpm-platform/wiki/Testing-Best-Practices-Java).
    * Have a look at other tests in the same module for how it works.
    * In rare cases, it is not feasible to write an automated test. Please ask us if you think that is the case for your contribution.
1. Do your commits follow our [commit message conventions](#commit-message-conventions)?

# Commit message conventions

We are following the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification. 
The messages of all commits must conform to the style:

```
<type>(<scope>): <subject>

<body>

<footer>
```

Example:

```
feat(engine): Support BPEL

- implements execution for a really old standard
- BPEL models are mapped to internal ActivityBehavior classes

related to #123
```

Have a look at the [commit history](https://github.com/operaton/operaton/commits/master) for real-life examples. 


## \<type\>

One of the following:

* feat (feature)
* fix (bug fix)
* docs (documentation)
* style (formatting, missing semicolons, …)
* refactor
* test (when adding missing tests)
* chore (maintain)
 
## \<scope\>

The scope is the module that is changed by the commit. E.g. `engine` in the case of https://github.com/operaton/operaton/tree/master/engine.

Candidates:
* engine
* engine-rest
* webapps
* run
* spin
* juel
* tests
* springboot
* quarkus
* client
* plugin

## \<subject\>

A brief summary of the change. Use imperative form (e.g. *implement* instead of *implemented*).  The entire subject line shall not exceed 70 characters.

## \<body\>

A list of bullet points giving a high-level overview of the contribution, e.g. which strategy was used for implementing the feature. Use present tense here (e.g. *implements* instead of *implemented*). A line in the body shall not exceed 80 characters. For small changes, the body can be omitted. 

## \<footer\>

Must be `related to <ticket>` where ticket is the ticket number, e.g. CAM-1234. If the change is related to multiple 
tickets, list them in a comma-separated list such as `related to CAM-1234, CAM-4321`.

Optionally, you can reference the number of the GitHub PR from which the commit is merged. The message footer can then 
look like `related to <ticket>, closes #<pr_number>` such as `related to CAM-1234, closes #567`.

# Review process

We usually check for new community-submitted pull requests once a week. We will then assign a reviewer from our development team and that person will provide feedback as soon as possible. 

Note that due to other responsibilities (our own implementation tasks, releases), feedback can sometimes be a bit delayed. Especially for larger contributions, it can take a bit until we have the time to assess your code properly.

During review we will provide you with feedback and help to get your contribution merge-ready. However, before requesting a review, please go through our [contribution checklist](#contribution-checklist).
