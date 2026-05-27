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
| `unit-tests` | checkout → Java 17 → `mvn verify -P unit-tests -Dgpg.skip=true` → upload coverage (flag: `unit`) |
| `integration-tests` | checkout → Java 17 → install aptos CLI → start localnet → wait for readiness → `mvn verify -P integration-tests -Dgpg.skip=true` → upload coverage (flag: `integration`) |

Both jobs run on `ubuntu-latest` and execute in parallel. `mvn verify` is used (not `mvn test`) because JaCoCo's `report` goal is bound to the `verify` phase. `-Dgpg.skip=true` skips GPG signing which is also bound to `verify`.

**Localnet startup:**
- Install the Aptos CLI via the `@aptos-labs/aptos-cli@3.0.0` npm wrapper: `npm install --no-save` the package, then `npx aptos --install` with `APTOS_CLI_VERSION` pinned. The wrapper reads `APTOS_CLI_VERSION` to bypass package managers and download the pinned release directly from GitHub Releases, auto-detects the Ubuntu version from `/etc/os-release`, extracts to a tmpdir, moves the binary to `~/.local/bin`, and verifies the SHA256 checksum. Subsequent CLI invocations go through `npx aptos …` so no PATH manipulation is required.
- Start `npx aptos node run-localnet --force-restart --assume-yes` as a background process (faucet on port 8081 is included by default in v4+)
- Poll `http://localhost:8070/` (the localnet's built-in readiness endpoint, which signals when node + faucet are both ready) until HTTP 200, timeout 90s

## Test Separation

Use JUnit 5 `@Tag("integration")` on all network-dependent test classes.

**Classes to annotate with `@Tag("integration")`:**
- `DevnetTest`
- `BasicFundingSigningTests`
- `MultiKeyTests`
- `MultiKeyAccountTests`
- `ModuleFunctionTests`
- `TransactionTests`
- `GasStationTest` (also `@Disabled` — tagged for correctness if ever re-enabled)
- `MoveOptionIntegrationTest` (also `@Disabled` — tagged for correctness if ever re-enabled)

Pure unit tests — no changes needed: `MoveOptionTest`, `AccountGenerationTests`, `Bip39UtilsTests`, `SigningTests`, `OrderlessPayloadTests`, `AndroidMultikeyValidationTest`, `AccountDerivedPathTests`, `KeylessMultiKeyTest`, `LoggingTest`, `LoggingCustomNetworkTest`, `PluginSettingsTest`.

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
- `report` goal (bound to `verify` phase) — generates `target/site/jacoco/jacoco.xml`

**Codecov upload** in each job via `codecov/codecov-action@v5`:
```yaml
- uses: codecov/codecov-action@v5
  with:
    token: ${{ secrets.CODECOV_TOKEN }}
    files: target/site/jacoco/jacoco.xml
    flags: unit          # or: integration
    fail_ci_if_error: ${{ github.event_name == 'push' || github.event.pull_request.head.repo.full_name == github.repository }}
```

`fail_ci_if_error` is `true` for pushes to `main` and PRs from within the repo, `false` for fork PRs (which lack access to `CODECOV_TOKEN`).

Codecov merges the two flagged reports into a unified coverage view.

## Files to Create / Modify

| File | Action |
|---|---|
| `.github/workflows/ci.yml` | Create |
| `pom.xml` | Add JaCoCo plugin + two Maven profiles |
| 8 test class files | Add `@Tag("integration")` (see list above) |
| `src/test/java/com/aptoslabs/japtos/utils/TestConfig.java` | Read `APTOS_NETWORK` system property to override network; expose as `TestConfig.DEFAULT_NETWORK` |
| Integration test classes | Replace hardcoded `AptosConfig.Network.LOCALNET` with `TestConfig.DEFAULT_NETWORK` so the `APTOS_NETWORK` override actually flows through |

## Out of Scope

- `codecov.yml` coverage threshold configuration (can be added later)
- Java version matrix (single Java 17 for now)
- Scheduled or nightly integration test runs
- Caching of the `aptos` CLI binary between runs
