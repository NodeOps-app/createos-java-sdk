# Contributing

Use Java 17 or newer and Maven 3.9 or newer.

Before opening a pull request, run:

```sh
mvn spotless:apply
mvn verify
```

Code must follow the Google Java Style Guide. Spotless is authoritative for
formatting; Checkstyle covers additional source conventions. Add contract tests
for endpoint paths, request fields, response mapping, errors, retries, and
stream ownership when those behaviors change.

Use Conventional Commits with one of these types: `feat`, `fix`, `docs`,
`style`, `refactor`, `test`, `chore`, or `perf`.
