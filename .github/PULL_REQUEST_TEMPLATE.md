## What

<!-- 1-3 bullets describing what changed. Be specific. -->
-

## Why

<!-- Motivation and context. Link the issue this resolves. -->
Closes #

## How

<!-- Key technical decisions or non-obvious implementation details. Skip if self-evident from the diff. -->

## Test plan

- [ ] <!-- Manual step or automated test that validates the main flow -->
- [ ] Edge cases covered: <!-- list them -->
- [ ] Tested on Android
- [ ] Tested on iOS (if applicable)
- [ ] Tested on desktop / web (if applicable)

## Screenshots

<!-- Delete this section if there are no UI changes. -->

| Before | After |
|--------|-------|
|        |       |

## Checklist

- [ ] Fulfills the task / issue scope (no out-of-scope changes)
- [ ] Unit tests added or updated
- [ ] All tests pass (`./gradlew :core:allTests`)
- [ ] Lint passes (`./gradlew ktlintCheck`)
- [ ] Logic placed in the right module (`core` for game rules, `ui` for presentation; `commonMain` when possible)
- [ ] `core` still has no Compose, Firebase or platform dependencies
- [ ] Documentación actualizada (README / docs)
- [ ] No new technical debt introduced without justification
