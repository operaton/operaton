---
name: operaton-null-safety
description: Use when fixing Sonar javabugs:S2259 null-pointer-dereference issues, adding JSpecify annotations (@NullMarked, @Nullable, @NonNull) to Java code, or when asked to make Java code null-safe
---

# JSpecify Null Safety

## Overview

Assure null safety by using JSpecify annotations at API boundaries, then guarding call sites.
Use fewest annotations possible: `@NullMarked` scope + `@Nullable` exceptions, not `@NonNull` everywhere.

## Step 0: Verify the dependency FIRST

Before writing any `org.jspecify` import, confirm the dependency is declared:

```bash
grep -rn "jspecify" --include=pom.xml .
```

If absent, add to the module's pom (and version to the parent/bom if the repo manages versions centrally):

```xml
<dependency>
  <groupId>org.jspecify</groupId>
  <artifactId>jspecify</artifactId>
  <version>1.0.0</version>
</dependency>
```

A diff that imports jspecify without this step does not compile. Never skip it.

## Step 1: Analyze nullability

Analyze accessible methods and fields for nullability. The analysis must be 100% sure.

## Step 2: Annotate — the decision order

1. **`@NullMarked`** on the class (or `package-info.java` for a whole package): everything is non-null by default. This is the primary tool — it makes `@NonNull` redundant inside its scope.
2. **`@Nullable`** on the methods/params/fields that genuinely can be null (the ones Sonar's flow points at).
3. **`@NonNull`** only in code *outside* any `@NullMarked` scope where a single signature must be pinned down. If you're writing many `@NonNull`, switch to `@NullMarked` instead.
4. **Never annotate local variables.** Nullness of locals is inferred; annotations there are noise.

## Step 3: Fix the dereference

At each call site of a `@Nullable` method, either:
- **Fail fast** with the codebase's idiom (in Operaton: `EnsureUtil.ensureNotNull("processEngineConfiguration", value)` or a `ProcessEngineException` matching surrounding style), or
- **Handle null** with a real fallback if the domain allows it.

Never `@SuppressWarnings` or mark the Sonar issue "won't fix" to hit zero.

## Step 4: Verify

```bash
mvn -q compile -pl <module>   # must pass before claiming done
```

## Output rules

- Present changes as a reviewable unified diff, one logical change per hunk, with a one-line rationale each.
- Do NOT add Javadoc for annotations — `@Nullable` is self-documenting. Add prose only where the *reason* for nullability is non-obvious.

## Common mistakes

| Mistake | Fix |
|---|---|
| Import jspecify, dependency missing | Step 0 first, always |
| `@Nullable` on locals | Delete — annotate the API boundary instead |
| Blanket `@NonNull` on every member | Use `@NullMarked` on class/package |
| Guard added but nullable *source* left unannotated | Annotate the returning method too, so future callers are warned |
| Javadoc explaining each annotation | Omit unless reason is non-obvious |
