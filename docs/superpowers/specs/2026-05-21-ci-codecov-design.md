---
name: ci-codecov
description: GitHub Actions CI with JaCoCo coverage reporting to Codecov — two parallel jobs (unit tests, integration tests against aptos localnet)
metadata:
  type: project
---

# CI + Codecov Coverage Design

## Overview

Add a GitHub Actions CI pipeline with two parallel jobs: one for unit tests and one for integration tests against a local Aptos node. Both jobs collect JaCoCo coverage and upload to Codecov, which merges the reports.

## Workflow Structure

**File:** `.github/workflows/ci.yml`

**Triggers:** `push` to `main`, `pull_request` on all branches.

**Jobs:**

| Job | Steps |
|---|---|
| `unit-tests` | checkout → Java 17 → `mvn test -P unit-tests` → upload coverage (flag: `unit`) |
| `integration-tests` | checkout → Java 17 → install aptos CLI → start localnet → wait for health → `mvn test -P integration-tests` → upload coverage (flag: `integration`) |

Both jobs run on `ubuntu-latest` and execute in parallel.

**Localnet startup:**
- Install the `aptos` CLI binary from the latest GitHub release via the official install script
- Start `aptos node run-localnet --with-faucet` as a background process
- Poll `http://localhost:8080/v1` until it returns HTTP 200 (with a timeout ~60s) before running tests

## Test Separation

Use JUnit 5 `@Tag("integration")` on all network-dependent test classes.

**Classes to annotate with `@Tag("integration")`:**
- `DevnetTest`
- `BasicFundingSigningTests`
- `MultiKeyTests`
- `MultiKeyAccountTests`
- `LoggingTest`
- `LoggingCustomNetworkTest`
- `ModuleFunctionTests`
- `TransactionTests`
- `PluginSettingsTest`
- `KeylessMultiKeyTest`

Classes that are already `@Disabled` (`GasStationTest`, `MoveOptionIntegrationTest`) are not tagged — they remain disabled regardless.

`MoveOptionTest`, `AccountGenerationTests`, `Bip39UtilsTests`, `SigningTests`, `OrderlessPayloadTests`, `AndroidMultikeyValidationTest`, `AccountDerivedPathTests` are pure unit tests — no changes needed.

## Maven Profiles

Two profiles added to `pom.xml`:

**`unit-tests` profile:**
```xml
<profile>
  <id>unit-tests</id>
  <build>
    <plugins>
      <plugin>
        <!-- JaCoCo: prepare-agent + report goals -->
      </plugin>
      <plugin>
        <!-- Surefire: excludedGroups=integration -->
      </plugin>
    </plugins>
  </build>
</profile>
```

**`integration-tests` profile:**
```xml
<profile>
  <id>integration-tests</id>
  <build>
    <plugins>
      <plugin>
        <!-- JaCoCo: prepare-agent + report goals -->
      </plugin>
      <plugin>
        <!-- Surefire: groups=integration, systemPropertyVariables: APTOS_NETWORK=LOCALNET -->
      </plugin>
    </plugins>
  </build>
</profile>
```

The `APTOS_NETWORK` system property is read by `TestConfig` (or tests directly) to override the default network to `LOCALNET` (`http://127.0.0.1:8080`, faucet `http://127.0.0.1:8081`).

## Coverage — JaCoCo + Codecov

**JaCoCo** added to `<pluginManagement>` (version 0.8.12) and activated in both profiles with:
- `prepare-agent` goal (bound to `initialize` phase) — sets JVM args for instrumentation
- `report` goal (bound to `test` phase) — generates `target/site/jacoco/jacoco.xml`

**Codecov upload** in each job via `codecov/codecov-action@v5`:
```yaml
- uses: codecov/codecov-action@v5
  with:
    token: ${{ secrets.CODECOV_TOKEN }}
    files: target/site/jacoco/jacoco.xml
    flags: unit          # or: integration
    fail_ci_if_error: true
```

Codecov merges the two flagged reports into a unified coverage view.

## Files to Create / Modify

| File | Action |
|---|---|
| `.github/workflows/ci.yml` | Create |
| `pom.xml` | Add JaCoCo plugin + two Maven profiles |
| 10 test class files | Add `@Tag("integration")` |
| `src/test/java/com/aptoslabs/japtos/utils/TestConfig.java` | Read `APTOS_NETWORK` system property to override network |

## Out of Scope

- `codecov.yml` coverage threshold configuration (can be added later)
- Java version matrix (single Java 17 for now)
- Scheduled or nightly integration test runs
- Caching of the `aptos` CLI binary between runs
