# CI + Codecov Coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a GitHub Actions CI pipeline with unit tests and localnet integration tests, both reporting coverage to Codecov.

**Architecture:** Two parallel GitHub Actions jobs — `unit-tests` (no network, always fast) and `integration-tests` (starts an Aptos localnet via the `aptos` CLI, runs tagged tests against it). JaCoCo collects coverage in both jobs; Codecov merges the two flagged uploads.

**Tech Stack:** GitHub Actions, JaCoCo 0.8.12, Maven Surefire 3.3.0 (already in pom.xml), JUnit 5 `@Tag`, Aptos CLI (`aptos node run-local-testnet`), `codecov/codecov-action@v5`

---

## File Map

| File | Action |
|---|---|
| `pom.xml` | Add JaCoCo to `<pluginManagement>`, add `<profiles>` section with `unit-tests` and `integration-tests` profiles |
| `src/test/java/com/aptoslabs/japtos/utils/TestConfig.java` | Replace hardcoded `TESTNET` default with `resolveNetwork()` reading `APTOS_NETWORK` system property |
| `src/test/java/com/aptoslabs/japtos/BasicFundingSigningTests.java` | Add `@Tag("integration")` |
| `src/test/java/com/aptoslabs/japtos/DevnetTest.java` | Add `@Tag("integration")` |
| `src/test/java/com/aptoslabs/japtos/LoggingTest.java` | Add `import org.junit.jupiter.api.Tag;` + `@Tag("integration")` |
| `src/test/java/com/aptoslabs/japtos/LoggingCustomNetworkTest.java` | Add `import org.junit.jupiter.api.Tag;` + `@Tag("integration")` |
| `src/test/java/com/aptoslabs/japtos/ModuleFunctionTests.java` | Add `@Tag("integration")` |
| `src/test/java/com/aptoslabs/japtos/MultiKeyAccountTests.java` | Add `@Tag("integration")` |
| `src/test/java/com/aptoslabs/japtos/MultiKeyTests.java` | Add `@Tag("integration")` |
| `src/test/java/com/aptoslabs/japtos/PluginSettingsTest.java` | Add `import org.junit.jupiter.api.Tag;` + `@Tag("integration")` |
| `src/test/java/com/aptoslabs/japtos/TransactionTests.java` | Add `@Tag("integration")` |
| `.github/workflows/ci.yml` | Create — full CI workflow |

---

## Task 1: Add JaCoCo + Maven profiles to pom.xml

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add JaCoCo to `<pluginManagement>`**

In `pom.xml`, inside `<build><pluginManagement><plugins>`, add JaCoCo after the existing `maven-project-info-reports-plugin` entry:

```xml
        <plugin>
          <groupId>org.jacoco</groupId>
          <artifactId>jacoco-maven-plugin</artifactId>
          <version>0.8.12</version>
        </plugin>
```

The `</plugins>` closing tag that you're inserting before is the one inside `<pluginManagement>`.

- [ ] **Step 2: Add `<profiles>` section**

After the closing `</build>` tag and before the closing `</project>` tag, add:

```xml
  <profiles>

    <profile>
      <id>unit-tests</id>
      <build>
        <plugins>
          <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <executions>
              <execution>
                <id>prepare-agent</id>
                <goals><goal>prepare-agent</goal></goals>
              </execution>
              <execution>
                <id>report</id>
                <phase>test</phase>
                <goals><goal>report</goal></goals>
              </execution>
            </executions>
          </plugin>
          <plugin>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
              <excludedGroups>integration</excludedGroups>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </profile>

    <profile>
      <id>integration-tests</id>
      <build>
        <plugins>
          <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <executions>
              <execution>
                <id>prepare-agent</id>
                <goals><goal>prepare-agent</goal></goals>
              </execution>
              <execution>
                <id>report</id>
                <phase>test</phase>
                <goals><goal>report</goal></goals>
              </execution>
            </executions>
          </plugin>
          <plugin>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
              <groups>integration</groups>
              <systemPropertyVariables>
                <APTOS_NETWORK>LOCALNET</APTOS_NETWORK>
              </systemPropertyVariables>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </profile>

  </profiles>
```

- [ ] **Step 3: Verify unit profile compiles and skips network tests**

```bash
mvn test -P unit-tests 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`. Tests like `DevnetTest`, `MultiKeyTests`, `TransactionTests` should NOT appear in the test output. `AccountGenerationTests`, `Bip39UtilsTests` etc. should run.

Note: This will fail until Task 2 and Task 3 are complete — the `@Tag("integration")` annotations don't exist yet. Skip this step and come back after Task 3 is done.

- [ ] **Step 4: Verify JaCoCo report is generated**

```bash
ls target/site/jacoco/jacoco.xml
```

Expected: file exists.

- [ ] **Step 5: Commit**

```bash
git add pom.xml
git commit -m "build: add JaCoCo coverage and unit/integration Maven profiles"
```

---

## Task 2: Make TestConfig read APTOS_NETWORK system property

**Files:**
- Modify: `src/test/java/com/aptoslabs/japtos/utils/TestConfig.java`

- [ ] **Step 1: Replace the hardcoded DEFAULT_NETWORK field**

In `TestConfig.java`, replace lines 10–11:

```java
    // Default network (can be changed for testing)
    public static final AptosConfig.Network DEFAULT_NETWORK = AptosConfig.Network.TESTNET;
```

With:

```java
    public static final AptosConfig.Network DEFAULT_NETWORK = resolveNetwork();

    private static AptosConfig.Network resolveNetwork() {
        String prop = System.getProperty("APTOS_NETWORK");
        if (prop != null) {
            return AptosConfig.Network.valueOf(prop);
        }
        return AptosConfig.Network.TESTNET;
    }
```

- [ ] **Step 2: Verify it compiles**

```bash
mvn compile test-compile -q
```

Expected: `BUILD SUCCESS` with no output.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/aptoslabs/japtos/utils/TestConfig.java
git commit -m "test: read APTOS_NETWORK system property to override network in tests"
```

---

## Task 3: Annotate network-dependent tests with @Tag("integration")

Nine classes need this annotation. Six already use wildcard `import org.junit.jupiter.api.*` so no new import is needed. Three need an explicit `import org.junit.jupiter.api.Tag;` added.

**Files:** All 9 test class files listed in the File Map.

### Classes with wildcard imports (no import change needed)

For each of these, add `@Tag("integration")` on the line immediately before `public class`:

- [ ] **BasicFundingSigningTests.java** (line 27): change `public class BasicFundingSigningTests` to:
  ```java
  @Tag("integration")
  public class BasicFundingSigningTests {
  ```

- [ ] **DevnetTest.java** (line 27): change:
  ```java
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  public class DevnetTest {
  ```
  to:
  ```java
  @Tag("integration")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  public class DevnetTest {
  ```

- [ ] **ModuleFunctionTests.java** (line 28): add `@Tag("integration")` before `public class ModuleFunctionTests {`

- [ ] **MultiKeyAccountTests.java** (line 35): add `@Tag("integration")` before existing class-level annotations

- [ ] **MultiKeyTests.java** (line 38): add `@Tag("integration")` before:
  ```java
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  public class MultiKeyTests {
  ```
  Result:
  ```java
  @Tag("integration")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  public class MultiKeyTests {
  ```

- [ ] **TransactionTests.java** (line 32): add `@Tag("integration")` before `public class TransactionTests {`

### Classes with specific imports (need import added)

For `LoggingTest`, `LoggingCustomNetworkTest`, and `PluginSettingsTest`, add `import org.junit.jupiter.api.Tag;` alongside the existing JUnit imports, then add `@Tag("integration")` before `public class`.

- [ ] **LoggingTest.java**: Add after `import org.junit.jupiter.api.Test;`:
  ```java
  import org.junit.jupiter.api.Tag;
  ```
  Then add before `public class LoggingTest {`:
  ```java
  @Tag("integration")
  public class LoggingTest {
  ```

- [ ] **LoggingCustomNetworkTest.java**: Add after `import org.junit.jupiter.api.Test;`:
  ```java
  import org.junit.jupiter.api.Tag;
  ```
  Then add `@Tag("integration")` before `public class LoggingCustomNetworkTest {`

- [ ] **PluginSettingsTest.java**: Add after existing imports:
  ```java
  import org.junit.jupiter.api.Tag;
  ```
  Then add `@Tag("integration")` before `public class PluginSettingsTest {`

- [ ] **Step: Verify unit profile excludes all tagged tests**

```bash
mvn verify -P unit-tests -Dgpg.skip=true 2>&1 | grep -E "Tests run:|BUILD"
```

Expected: `BUILD SUCCESS`. None of the tagged integration classes should appear. Unit classes that should run: `AccountGenerationTests`, `Bip39UtilsTests`, `MoveOptionTest`, `SigningTests`, `OrderlessPayloadTests`, `AndroidMultikeyValidationTest`, `AccountDerivedPathTests`, `KeylessMultiKeyTest`, `LoggingTest`, `LoggingCustomNetworkTest`, `PluginSettingsTest`.

- [ ] **Step: Commit**

```bash
git add src/test/java/com/aptoslabs/japtos/
git commit -m "test: tag network-dependent tests with @Tag(\"integration\")"
```

---

## Task 4: Create GitHub Actions CI workflow

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Create directory and workflow file**

```bash
mkdir -p .github/workflows
```

Create `.github/workflows/ci.yml` with this exact content:

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  unit-tests:
    name: Unit Tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - name: Run unit tests
        run: mvn verify -P unit-tests -Dgpg.skip=true

      - name: Upload coverage
        uses: codecov/codecov-action@v5
        with:
          token: ${{ secrets.CODECOV_TOKEN }}
          files: target/site/jacoco/jacoco.xml
          flags: unit
          fail_ci_if_error: ${{ github.event_name == 'push' || github.event.pull_request.head.repo.full_name == github.repository }}

  integration-tests:
    name: Integration Tests
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - uses: actions/setup-node@v4
        with:
          node-version: '22'

      - name: Install Aptos CLI
        env:
          APTOS_CLI_VERSION: '9.3.0'
        run: |
          npm install --no-save @aptos-labs/aptos-cli@3.0.0
          npx aptos --install

      - name: Start localnet
        run: npx aptos node run-localnet --force-restart --assume-yes > /tmp/localnet.log 2>&1 &

      - name: Wait for localnet
        run: |
          echo "Waiting for localnet readiness..."
          for i in {1..45}; do
            if curl -sf http://localhost:8070/ > /dev/null 2>&1; then
              echo "Localnet ready after ${i}x2s"
              exit 0
            fi
            echo "  attempt $i/45..."
            sleep 2
          done
          echo "Localnet failed to start. Log:"
          cat /tmp/localnet.log
          exit 1

      - name: Run integration tests
        run: mvn verify -P integration-tests -Dgpg.skip=true

      - name: Upload coverage
        uses: codecov/codecov-action@v5
        with:
          token: ${{ secrets.CODECOV_TOKEN }}
          files: target/site/jacoco/jacoco.xml
          flags: integration
          fail_ci_if_error: ${{ github.event_name == 'push' || github.event.pull_request.head.repo.full_name == github.repository }}
```

- [ ] **Step 2: Verify YAML is valid**

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/ci.yml'))" && echo "YAML valid"
```

Expected: `YAML valid`

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add GitHub Actions workflow with unit and integration test jobs + Codecov"
```

---

## Task 5: Add CODECOV_TOKEN secret and push

- [ ] **Step 1: Add the secret in GitHub**

Go to the repo's **Settings → Secrets and variables → Actions → New repository secret**.
Name: `CODECOV_TOKEN`
Value: copy from your Codecov dashboard for this repo (Settings → General → Token).

- [ ] **Step 2: Push the branch and open a PR (or push to main)**

```bash
git push origin main
```

Then watch the Actions tab to confirm both jobs run and the coverage uploads succeed.

- [ ] **Step 3: Verify coverage appears on Codecov**

Check `https://app.codecov.io/gh/aptos-labs/japtos` — you should see two flags (`unit`, `integration`) and a merged coverage report.
