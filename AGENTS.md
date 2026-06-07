# Project Instructions

## Git Flow

- Use `main` only for production-ready code.
- Use `develop` as the integration branch for upcoming work.
- Do not commit directly to `main` or `develop` unless the user explicitly requests it.
- After completing work on a feature branch, automatically open a pull request into `develop`.
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

## MVP Architecture

- Build the MVP as a modular monolith, not as distributed microservices.
- Split the backend into modules from the start.
- Use a feature-slice architecture as the primary backend reference: each feature owns its domain model, use cases, persistence adapters, web controllers, forms, and templates where practical.
- Prefer vertical feature boundaries over technical-layer packages as the top-level structure.
- Keep shared code small and explicit; move code to shared modules only when at least two feature slices genuinely need it.
- Treat services named in `DESIGN.md` as internal modules/features for the MVP, not as separately deployable services.
- Keep AI integration behind ports/interfaces so the core feature logic does not depend directly on a specific LLM provider or framework.
- Store the MVP context graph in PostgreSQL through a relations table before introducing a dedicated graph database.
- Use PostgreSQL as the primary database for MVP. Add pgvector only when semantic search/RAG becomes part of the implemented scope.

## Backend Feature Slices

- `identity`: users, authentication, authorization, security.
- `academics`: subjects, teachers, semesters, academic metadata.
- `tasks`: tasks, statuses, priorities, kanban, deadlines, task steps.
- `sources`: source messages, manual input, processing history, extraction workflow entrypoints.
- `materials`: uploaded learning materials, file metadata, summaries.
- `contextgraph`: relations between tasks, subjects, teachers, materials, and topics.
- `assistant`: chat, daily recommendations, task Q&A, planning requests.
- `ai`: LLM, embeddings, structured output parsing, agent adapters, AI provider configuration.
- `common`: identifiers, time utilities, errors, pagination, cross-feature primitives.

## Web UI

- Build the MVP UI with server-side rendered templates.
- Prefer Spring MVC + Thymeleaf for pages and forms.
- Use HTMX for partial updates such as kanban status changes, source-message processing, assistant chat updates, and generated task plans.
- Use small, local JavaScript only where templates and HTMX are not enough.
- Do not introduce React/Next.js for MVP unless the user explicitly changes this direction.
