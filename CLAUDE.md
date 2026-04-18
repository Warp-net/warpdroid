# Development Guidelines

## Documentation
Before making any code changes, you must read the relevant official documentation on the website https://warpnet.site/docs.
Requirements:
- First find and read the relevant documentation pages, then act.
- If the documentation is unclear or conflicts with the codebase rely on codebase.
- If the documentation is unclear or conflicts with the codebase notify about this discrepancy.

## Code Changes
- Make the smallest possible changes required to solve the task.
- Avoid refactoring or unrelated edits.

## AI-generated Comments
- Validate all comments and suggestions from Codex and Copilot:
    - Ensure correctness.
    - Ensure relevance.
    - Discard low-value or incorrect suggestions.

## Versioning
- Increment the patch version in the `version` file on every commit. Create an according git tag.