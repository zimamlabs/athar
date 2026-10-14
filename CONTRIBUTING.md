# Contributing

Athar is a small, local-first app. Issues and ideas are welcome.

- Work on a branch and open a pull request into `main`; the PR title is the branch name. `main` is protected: the "Tests and builds" check must pass, and history stays linear (squash merge).
- Keep the module rules: features (`feature-*`) depend only on `:core` and `:ui` (`scripts/check-modules.sh`).
- Every text goes in both `StringsEn.kt` and `StringsAr.kt`; a test checks they match.
- New logic comes with a test. Never delete or overwrite a user's file: copy, verify, keep.
- Commit messages describe the change; no tool attribution.
