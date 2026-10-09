# Ad-hoc subprocess implementation provenance

The implementation continues Operaton PR [#3207](https://github.com/operaton/operaton/pull/3207)
and issue [#1010](https://github.com/operaton/operaton/issues/1010). Julian Haupt's work and
its commit history are retained, including its sequential ordering, discovery, completion
context, and repeat-activation extensions.

## Existing import

The initial import in Operaton commit `2b3adab0e4` derives from
[Fluxnova commit db14f6e71fba096408c560672cf96475c6c5516b](https://github.com/finos/fluxnova-bpm-platform/commit/db14f6e71fba096408c560672cf96475c6c5516b),
original author Malavade, Harish <harish.malavade@fmr.com>.
The FINOS Apache-2.0 headers restored by Operaton commit `c4eaf485fd` remain intact.
The applicable FINOS and Camunda copyright/inclusion notices from the source NOTICE are
retained in Operaton's root NOTICE. New Operaton-authored files use the Operaton
contributors' Apache-2.0 header, as required by CONTRIBUTING.md.

## Continuation research

The lifecycle regression scenarios were informed by:

- [Fluxnova PR #224](https://github.com/finos/fluxnova-bpm-platform/pull/224), merge
  `830371465790c3fd15476a1d62dde07c947754ba`; implementation by Chris Miller
  <miller.chris.david@gmail.com> in `b8dcf48d`, `8b14c6c5`, and `7f408de0`.
  Its fixes identify scope pruning, canceled child removal, and completion before
  outgoing transitions as relevant failure modes.
- [CIB seven PR #476](https://github.com/cibseven/cibseven/pull/476), squash
  `5e9a113494fdeff5ffff8cefad6127b45b9dfe0e` by dmitrymalk.
  Its work identifies latched-completion activation, two-phase batch activation,
  multi-instance wrappers, and migration validation as relevant failure modes.

Both source revisions carry Apache License 2.0 in their root LICENSE files; these
files were inspected before comparing the implementations. The continuation's runtime
and migration changes are independently adapted to Operaton's existing scope-local
state and PVM lifecycle, rather than a wholesale port. In particular, CIB's active
migration prohibition and sequential-ordering prohibition are not adopted.

New source copied from either project in future changes must retain its applicable
headers/NOTICE and use the actual source commit and original author in the backport
commit body. Merge authors must not be substituted for implementation authors.

## Enabled-state scheduler and migration

The persistent enabled-state scheduler follows BPMN 2.0.2 section 13.3.5 and is
implemented against Operaton's existing PVM, execution persistence and migration
contracts. Its new regression tests are authored for these contracts. Zeebe and
Flowable were studied for behavioral comparison only; no source, translated code,
tests or comments from those implementations are included in this continuation.
See [the comparison](ad-hoc-engine-comparison.md) for version-specific references.

## Validation prerequisites

The continuation includes the null-handling repairs submitted separately in
[Operaton PR #3819](https://github.com/operaton/operaton/pull/3819).
It also includes the two-line offline XML test repair from
[Operaton PR #3820](https://github.com/operaton/operaton/pull/3820): the Neo mapping
XML test resolves the existing MyBatis DTD through MyBatis' bundled entity resolver.
This preserves the XML assertions and avoids a network lookup during validation;
it does not change runtime XML parsing. These overlapping prerequisite changes
are identified separately so they can be removed from the review after merging upstream.
