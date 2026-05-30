# InterviewTemplate

Small Java 21 template for live coding interviews.

The project is intentionally plain Java: no Spring, no configuration files, no
test doubles framework by default. It is meant to be opened in IntelliJ and used
as a fast starting point for TDD-style mini projects or algorithmic tasks.

## Requirements

- JDK 21 or newer
- Maven 3.9+

## Current Structure

```text
src/main/java/interview/Main.java
src/main/java/interview/Solution.java
src/test/java/interview/SolutionTest.java
```

- `Solution` is the first class to replace with the task implementation.
- `SolutionTest` contains quick examples for parameterized tests, edge cases,
  and exception assertions.
- `Main` is only for a fast manual console run.
- For mini projects, create a focused package under `interview`, for example
  `interview.loadbalancer` or `interview.transfer`.

## Build

```shell
mvn test
mvn package
```

## Live Coding Flow

1. Clarify functional requirements and assumptions.
2. State the simplest plan before coding.
3. Write the first failing test for the current stage.
4. Implement the smallest working solution.
5. Refactor only after tests pass.
6. Repeat for the next stage.

Useful test buckets:

- happy path
- invalid input
- duplicates/idempotency
- boundary values
- concurrency/race conditions, if the task is stateful
