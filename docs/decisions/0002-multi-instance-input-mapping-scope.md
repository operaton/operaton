---
status: "Proposed"
date: 2026-10-09
---

# Multi-instance input mapping evaluation scope

## Context and Problem Statement

Input parameters evaluate against the parent variable scope and write their results to the activity's local scope.
Multi-instance activities can store their collection element and `loopCounter` on that local execution. Consequently,
their input parameters cannot always read the current iteration's variables, as reported in
[issue #3814](https://github.com/operaton/operaton/issues/3814).

Changing every input parameter to evaluate against the activity execution would also expose unrelated local variables
and earlier input parameter results. A narrowly scoped correction must preserve ordinary parent-scope lookup,
parameter order, and the existing destination of explicit variable writes in expressions and scripts.

## Decision Drivers

- Make current iteration variables available to expression and script input parameters, including through `execution`.
- Preserve parent-scope evaluation for other variables and for activities without multi-instance characteristics.
- Preserve null shadowing, nested multi-instance isolation, and the order of parameter evaluation and assignment.
- Avoid temporarily changing shared execution state or the behavior of general variable-scope operations.

## Considered Options

1. Evaluate input parameters directly against the activity execution.
2. Evaluate all input values against that execution before assigning any results.
3. Temporarily overlay iteration variables on the actual parent execution.
4. Use an explicit execution adapter for multi-instance input mapping evaluation.

## Decision Outcome

Chosen option: "Use an explicit execution adapter for multi-instance input mapping evaluation".

Create one adapter for a multi-instance activity's input mappings when iteration variables are local to the execution
receiving the mapping results. Where the parent already provides the iteration context, keep the normal mapping path.
Capture the local collection element and `loopCounter` as typed values before evaluating the first parameter.
Read these captured values before falling back to the live parent execution; test presence separately from value so
that a null iteration element still shadows an identically named parent variable. Keep this snapshot for the entire
input mapping operation so that an input parameter named after an iteration variable does not change the source of
subsequent input parameters.
Deserialize captured values only when requested so an unused serialized element does not introduce a new failure.

The adapter implements `DelegateExecution`, forwards execution metadata and variable mutations to the original parent,
and exposes the same evaluation view through expression and script `execution` bindings. Parameters retain their
existing evaluate-then-assign order, and assignment still writes to the real activity execution. The normal input
mapping path and output mappings remain unchanged.

### Consequences

- Expression and script mappings can read the current iteration element and index without modifying execution trees
  or persistent variables.
- Unrelated parent-variable changes made during evaluation remain visible to subsequent parameters. Earlier mapping
  results do not become a new source of input values.
- A parent variable with the same name as an available iteration variable is intentionally shadowed in multi-instance
  input mappings, including when the iteration value is null. Models relying on the previous parent value can observe
  changed results.
- The `execution` value in affected mappings implements the supported `DelegateExecution` API and preserves the
  parent's metadata and mutation target. Code relying on concrete `ExecutionEntity` casts or object identity must be
  reviewed; the adapter does not promise those internal implementation details.
- The adapter adds delegation methods that must stay aligned with the execution API. This localized maintenance cost
  avoids changing core variable lookup or introducing temporary state visible to reentrant engine calls.
- There are no database schema, BPMN model format, or public execution API changes.

### Confirmation

Verify sequential and parallel collection-based mappings, expression and script access through `execution`, a null
element shadowing a parent value, same-name input parameters, parent-variable lookup and mutation, and nested
multi-instance activities. Run the existing input/output mapping tests to confirm ordinary mapping compatibility.

## Pros and Cons of the Options

- Direct activity evaluation is small but makes mappings depend on earlier mapping results and changes explicit
  execution-write targets.
- Two-phase evaluation avoids newly assigned results during evaluation but changes assignment timing, execution
  metadata, and write targets; unrelated preexisting local variables are still visible.
- A temporary overlay preserves the actual parent object but affects reentrant lookups and can change write routing
  because variable mutation itself consults variable-presence methods.
- An explicit adapter isolates the corrected read view and preserves write routing, at the cost of delegation code
  and the documented change to the concrete execution object in affected mappings.
