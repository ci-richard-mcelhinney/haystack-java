# Hayson (Haystack 4 JSON) work: status

Snapshot taken 2026-10-05 on branch `feature/hayson` (includes everything from the former `claude/eager-thompson` worktree branch).

## Branch state

- `feature/hayson` is about 26 commits ahead of the local `master`.
- `origin/master` has 4 upstream commits that `feature/hayson` does not yet contain: the nested grid encoding fix (#13), the gradle/maven dependency instructions (#14), the jitpack README update, and a merge commit. Merge or rebase these before opening the PR, then re-run the tests.
- Nothing from this work had been pushed when this file was written.

## Build and test

The project uses Gradle, not Maven.

```bash
./gradlew test
```

Current result: 161 tests, 0 failures. `ClientTest` produces no results in the Gradle run. It appears to need a live server and was already broken before this work.

## What is implemented

**Writer** (`io/HHaysonWriter.java`)
- Scalars: Num, Str, Bool, Ref, Date, Time, DateTime, Coord, Symbol, Uri, XStr, Bin, Span, NA, Marker, Remove.
- Containers: List, Dict, Grid.
- Numbers are written at full precision through the private `numToJson()` helper. `toZinc()` rounds to 4 decimal places, so it is not used.
- NaN, INF, -INF and any number with a unit are written as `{"_kind":"number",...}`.

**Reader** (`io/HHaysonReader.java`)
- A custom recursive-descent JSON parser with no external dependencies.
- Layer 1 parses raw JSON into Java types (`String`, `Double`, `Boolean`, `null`, `List`, `Map`).
- Layer 2 converts those into Haystack values, dispatching on `_kind`. It covers every `_kind` the writer emits.
- Registered in `io/HGridFormat.java` for both `application/json` and `application/vnd.haystack+json;version=4`.

**New type**
- `HSpan` is an `HVal` subclass for date or datetime ranges. It is encoded in Hayson as `{"_kind":"xstr","type":"Span","val":"start,end"}`.
- `HNA` was already present and is now wired into the writer.

## Fixes made along the way

- `HNum` rejects NaN with a unit, as the Project Haystack spec requires.
- `HZincWriter.writeBin` wrote `Bin("mime")` twice for version 3 and broke round-tripping. Fixed.
- Re-enabled the commented-out tests in `HBinTest` (`testEncoding`, `testBadBins`) and `ZincGridTest` (v2.0 `Bin(...)` blocks).
- In v2.0 zinc, `parseBinObsolete()` reads tokens, so whitespace is dropped and quoted strings lose their quotes. `Bin(text/html; a=foo; bar="sep")` parses to the mime `text/html;a=foo;bar=sep`.
- Removed dead `HNumber` commented-out code. No `HNumber` class exists.

## Known gaps and loose ends

1. `HSpan.toJson()` throws `UnsupportedOperationException`, like `HBool.toJson()`. This only matters for the old v2 JSON path.
2. `HSpan` only has factories for two `HDate` or two `HDateTime` values. Named ranges such as `today` are not supported.
3. The reader has no tests for malformed JSON, deep nesting or very large numbers.
4. `HNum` does not yet implement every spec detail: the unit name is not validated against a unit database, underscore digit separators are not supported, and scientific-notation parsing was not reviewed.
5. `.claude/` shows as untracked in git. Consider adding it to `.gitignore`.
6. `TokenizerTest` has one commented-out `PST8PDT` datetime case that predates this work.
7. `ClientTest` is not runnable without a server (pre-existing).

## Suggested next steps

1. Merge `origin/master` into `feature/hayson` and re-run `./gradlew test`.
2. Push and open the PR against `master`.
3. Optionally add reader hardening tests (item 3), named-range support for `HSpan` (item 2) and the remaining `HNum` spec items (item 4).

## Working conventions

- Commit messages must not mention Claude or Anthropic.
- Do not push until explicitly asked.
- Keep the JSON reader dependency-free.
