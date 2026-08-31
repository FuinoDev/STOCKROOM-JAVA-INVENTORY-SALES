# GitHub publishing

The application and source are local until a repository is explicitly configured and pushed. Do not treat the local folder as an off-device backup.

## Before publishing

1. Confirm you are working in the Stockroom project root, where pom.xml and Start Stockroom.cmd are located.
2. Run scripts/test.ps1 and review its results.
3. Review staged files. Never include .local, .m2, config/local.properties, exports, database dumps or business data.
4. Choose or create a **private** GitHub repository for this project.
5. Authenticate Git using Git Credential Manager, GitHub CLI or the GitHub integration in your IDE. Do not paste a token into the source, README or remote URL.
6. Configure only this project's origin remote to the repository you chose, then push main.

If a project repository has not been initialized, initialize one inside this folder first. Do not commit or push from C:\ itself.

## Example commands after authentication

Replace YOUR-ACCOUNT and YOUR-REPOSITORY with the actual destination:

```text
git remote add origin https://github.com/YOUR-ACCOUNT/YOUR-REPOSITORY.git
git push -u origin main
```

If origin already exists, inspect it and confirm it is the intended repository rather than blindly replacing it.

The supplied GitHub Actions workflow builds on Java 17 and tests against an isolated PostgreSQL service. Desktop screenshots are skipped in headless CI. Production credentials are not needed for CI.

The runtime distribution ZIP contains the runnable app and instructions. The repository contains the complete source, tests, schema and documentation.
