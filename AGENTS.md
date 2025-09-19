# Repository Guidelines

## Project Structure & Module Organization
QuickFIX/J is a multi-module Maven build orchestrated by the root `pom.xml`. Runtime engine code lives in `quickfixj-core` (session management) and `quickfixj-base` (message primitives), while FIX schemas reside in `quickfixj-messages`. Supporting tools such as `quickfixj-codegenerator`, `quickfixj-dictgenerator`, and `quickfixj-class-pruner-maven-plugin` generate codecs and trim distributions; keep changes here isolated and well-commented. Packaging modules like `quickfixj-distribution`, `quickfixj-all`, and the reference `quickfixj-examples` produce bundles users consume. Architecture diagrams are tracked under `src/main/puml`, and `customising-quickfixj.md` summarises extension points.

## Build, Test, and Development Commands
Builds run in parallel by default (`-T 1C` in `.mvn/maven.config`). The module graph declares ordering (e.g., `quickfixj-base` depends on `quickfixj-orchestration`) so parallel builds are reliable. Override threads as needed with `-T`.

Run `./mvnw clean package -Dmaven.javadoc.skip=true -PskipBundlePlugin,minimal-fix-latest` for the full build the CI workflows execute. Use `./mvnw clean package -DskipAT=true -PskipBundlePlugin,minimal-fix-latest` when iterating and you only need to skip the acceptance suite. Execute `./mvnw -pl quickfixj-core -am test` to focus on the core engine plus its dependencies, or append `-Dtest=SessionTest` to target a class. The examples bundle can be run after `./mvnw -pl quickfixj-examples -am package`.

## Coding Style & Naming Conventions
Target Java 21 (compiler release 21). Follow the existing four-space indentation and K&R brace style shown in `quickfixj-core/src/main/java/quickfix/Session.java`. Class names are CamelCase, constants SCREAMING_SNAKE_CASE, and package names all lower-case. Inject loggers with `private static final Logger LOGGER = LoggerFactory.getLogger(...)`. Keep public APIs javadoc’d and prefer immutable collections unless performance demands otherwise.

## Testing Guidelines
Unit and integration tests use JUnit 4 under each module’s `src/test/java`. New features should include coverage in the module they touch plus regression cases for reported bugs. Acceptance tests (labelled `AT` in the `quickfixj-core` tree) run by default; only set `-DskipAT=true` with a rationale in your PR. Add performance checks under `quickfixj-perf-test` only when reproducible locally.

## Commit & Pull Request Guidelines
Commits should be small, imperative, and scoped, e.g. `core: tighten heartbeat timeout handling`. Reference issue numbers where relevant and avoid merge commits in topic branches. Each PR needs a clear problem statement, testing notes (e.g. `./mvnw test`), and mention of any config or schema files touched. Attach screenshots or logs when changes affect distribution artifacts or generated FIX dictionaries.

## Security & Configuration Tips
Review `SECURITY.md` before handling vulnerabilities and never commit real certificates or session settings. Configuration examples belong in `quickfixj-examples` or documentation under `quickfixj-core/src/main/doc`. Confirm dependency upgrades in `quickfixj-distribution` match binary compatibility expectations and call out breaking changes in the PR body.
