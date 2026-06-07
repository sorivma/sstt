# Project Instructions

## Git Flow

- Use `main` only for production-ready code.
- Use `develop` as the integration branch for upcoming work.
- Do not commit directly to `main` or `develop` unless the user explicitly requests it.
- Create feature branches from `develop` using `feature/<short-description>`.
- Create release branches from `develop` using `release/<version>`.
- Create hotfix branches from `main` using `hotfix/<short-description>`.
- Merge completed feature branches back into `develop`.
- Merge release branches into both `main` and `develop`.
- Merge hotfix branches into both `main` and `develop`.
- Keep branch names lowercase and use hyphens between words.
- Before finishing work, run the relevant checks or explain why they could not be run.
- Do not rewrite shared history unless the user explicitly asks for it.
- Do not discard uncommitted user changes.

