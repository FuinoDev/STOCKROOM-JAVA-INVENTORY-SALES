# Third-party notices

Stockroom includes these runtime dependencies:

- **PostgreSQL JDBC Driver 42.7.13** — PostgreSQL JDBC contributors; BSD 2-Clause license. https://jdbc.postgresql.org/about/license/
- **FlatLaf 3.7** — FormDev Software GmbH; Apache License 2.0. https://github.com/JFormDesigner/FlatLaf
- **Checker Qual 3.55.1** — Checker Framework contributors; MIT license for the qualifier artifacts. https://checkerframework.org/

Original license files from the runtime artifacts are preserved under **third-party/** in the application resources and copied into the packaged JAR. These notices do not replace the original licenses.

JUnit is used only for testing and is not included in the runtime JAR. Maven and PostgreSQL are separately installed tools and are not redistributed in the runtime ZIP.
