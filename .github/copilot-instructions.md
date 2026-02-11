# Copilot Instructions for TechEmpower Framework Benchmarks

## Repository Overview

This is the **TechEmpower Framework Benchmarks (TFB)** repository - a comprehensive performance benchmarking suite for web application frameworks across multiple programming languages. The repository contains over 400 framework implementations across 41+ languages, with approximately 1000+ Dockerfiles for testing different configurations.

**Purpose**: Provide representative performance measures for web frameworks through standardized tests including JSON serialization, database operations, ORM usage, template rendering, and more.

**Official Documentation**: https://github.com/TechEmpower/FrameworkBenchmarks/wiki

**Live Results**: https://tfb-status.techempower.com/

**⚠️ Important Note**: This fork is maintained by the huanshankeji organization. Information in this document about the `scripts/` and `toolset/` directories comes from the upstream TechEmpower repository and may not be fully accurate for this fork. When working with these areas, verify behavior against actual code and defer to the repository maintainer for authoritative guidance.

**⚠️ Additional Disclaimer**: Information about the `scripts/` and `toolset/` directories was AI-summarized from upstream source code without human review. It may be incomplete, incompletely summarized, or inaccurate. Always verify against the actual code in these directories and consult the repository maintainer when working with these areas.

## Repository Structure

```
FrameworkBenchmarks/
├── frameworks/          # Framework implementations organized by language
│   ├── Kotlin/         # Each language has its own directory
│   ├── Java/
│   ├── Python/
│   └── ...             # 41+ language directories
├── toolset/            # Python-based test orchestration toolset
│   ├── benchmark/      # Core benchmarking logic
│   ├── test_types/     # Test type definitions (json, db, query, etc.)
│   ├── utils/          # Utilities including docker_helper.py
│   └── run-tests.py    # Main entry point for test execution
├── .github/            # GitHub Actions workflows and templates
│   └── workflows/      # CI/CD workflows
├── tfb                 # Main CLI script (wrapper for Docker-based execution)
└── Dockerfile          # Toolset container definition
```

## Key Concepts

### Framework Tests Structure

Each framework test is organized under `frameworks/<Language>/<Framework>/`:

- **benchmark_config.json**: Defines test configurations, URLs, database types, and metadata
- **\*.dockerfile**: Docker container definitions for building and running the test
- **README.md**: Framework description, test URLs, and important libraries used
- **Source code**: Implementation of various test types

### Test Types

The suite includes these standard test types (defined in `toolset/test_types/`):

1. **JSON**: JSON serialization test
2. **PLAINTEXT**: Plain text response test
3. **DB**: Single database query test
4. **QUERY**: Multiple database queries test
5. **UPDATE**: Database update test
6. **FORTUNE**: Server-side template rendering with database queries
7. **CACHED-QUERY**: Cached query test

Each test type has specific requirements defined in the corresponding module under `toolset/test_types/`.

## How to Work with This Repository

### Running Tests Locally

The primary way to interact with the test suite is through the `./tfb` script:

```bash
# Run a specific test in verify mode
./tfb --mode verify --test <framework-name>

# Example
./tfb --mode verify --test vertx-web-kotlinx

# Create a new framework test (interactive wizard)
./tfb --new
```

**Important**: The `./tfb` script:
- Builds a Docker container from the root `Dockerfile`
- Mounts the repository and Docker socket
- Runs `toolset/run-tests.py` inside the container
- Requires Docker to be installed and running

**⚠️ Known Issue in AI Coding Agent Environments**: 

The `./tfb` command may fail in GitHub Copilot coding agent environments due to SSL certificate verification errors when building the Docker container. The error appears as:

```
[SSL: CERTIFICATE_VERIFY_FAILED] certificate verify failed: self-signed certificate in certificate chain
```

This occurs when pip tries to install Python packages from PyPI during the Docker build process.

**Workaround for AI Agents**:
- **DO NOT attempt to run `./tfb` commands directly in the coding agent environment**
- When test execution is required, document the changes made and **hand over the work to the repository maintainer** to run the tests
- Include clear instructions for the maintainer on which tests to run (e.g., `./tfb --mode verify --test framework-name`)
- **Alternative**: GitHub Codespaces is confirmed to work for running `./tfb` commands without SSL certificate issues

### Adding a New Framework

1. **Use the scaffolding wizard**: `./tfb --new`
2. **Manual approach**:
   - Create directory: `frameworks/<Language>/<FrameworkName>/`
   - Add `benchmark_config.json` with test configuration
   - Create `*.dockerfile` for each test configuration
   - Add `README.md` describing the framework
   - Implement required test endpoints

### benchmark_config.json Structure

```json
{
  "framework": "framework-name",
  "tests": [
    {
      "test-name": {
        "json_url": "/json",          // For JSON test
        "plaintext_url": "/plaintext", // For PLAINTEXT test
        "db_url": "/db",              // For DB test
        "query_url": "/queries?queries=", // For QUERY test
        "update_url": "/updates?queries=", // For UPDATE test
        "fortune_url": "/fortunes",   // For FORTUNE test
        "port": 8080,
        "approach": "Realistic|Stripped", // Implementation approach
        "classification": "Micro|Platform|Fullstack",
        "database": "postgres|mysql|mongodb|None",
        "framework": "framework-name",
        "language": "Language",
        "orm": "Full|Raw|None",
        "platform": "Platform-name",
        "webserver": "None|nginx|etc",
        "os": "Linux",
        "database_os": "Linux",
        "display_name": "display-name",
        "notes": "",
        "versus": "comparison-framework"
      }
    }
  ]
}
```

## Docker and Container Architecture

### Framework Dockerfiles

Each framework test requires one or more Dockerfiles named: `<framework-name>-<test-config>.dockerfile`

**Best Practices**:
- Use multi-stage builds when possible
- Base images should match the language/framework requirements
- Optimize for build time (cache dependencies)
- Expose the correct port (typically 8080)
- Use `CMD` or `ENTRYPOINT` to start the application
- Set appropriate JVM flags, environment variables, or runtime options

**Example Dockerfile patterns**:
```dockerfile
FROM gradle:9.2.1-jdk25

WORKDIR /app
COPY gradle/ gradle/
COPY build.gradle.kts .
# ... copy source code
RUN gradle build

EXPOSE 8080
CMD ["./start-script.sh"]
```

### Docker Helper (toolset/utils/docker_helper.py)

The toolset uses docker-py to manage containers. Key points:
- Builds containers using low-level Docker API
- Mounts volumes for profiling results persistence
- Build timeout is 60 minutes (3600 seconds)
- Use `run_log_dir` as `/profiling-results` volume to persist profiling data

## GitHub Actions CI/CD

### Workflow Trigger (.github/workflows/build.yml)

The CI runs on push and pull requests:

1. **Setup job**: Determines which frameworks changed using `github_actions_diff.py`
2. **Verify job**: Runs tests only for changed frameworks (matrix strategy)
3. Tests can be skipped with `[ci skip]` in commit message
4. Only tests frameworks in the diff to save CI time

### GitHub Actions Diff

The toolset intelligently determines which frameworks to test based on file changes:
- Compares current commit with target branch
- Outputs `github-actions-run-tests <framework-dirs>`
- Prevents unnecessary test runs

## Kotlin Code Style (for Kotlin frameworks)

Per https://github.com/huanshankeji/.github/blob/main/kotlin-code-style.md:

1. **Golf the code**: Prefer `also`, `apply`, `let`, `run` to `if-else` when they shorten code
2. **Avoid single-use variables**: Don't create variables used only once unless it greatly improves readability
3. **Don't refactor existing patterns**: Unless you're a maintainer deciding on a better approach

## Development Instructions (for snapshot dependencies)

Per https://github.com/huanshankeji/.github/blob/main/dev-instructions.md:

When encountering `com.huanshankeji` snapshot dependencies:
1. Ensure dependency project is available locally
2. Switch to `dev` branch (or `main` if dev doesn't exist/work)
3. Run `publishToMavenLocal` in the dependency project
4. Apply recursively for transitive dependencies

## Common Patterns and Conventions

### Framework Implementations

- **Minimize dependencies**: Only include what's needed for benchmarks
- **No test code**: Don't include JUnit tests, local dev configs, or startup scripts
- **Update README.md**: Always update framework README when editing tests
- **Consistent naming**: Framework directories use lowercase with hyphens

### Python Toolset

- Python 3.x (check `.github/workflows/build.yml` for current CI version)
- Uses these key modules:
  - `docker` for container management
  - `colorama` for colored output
  - `psutil` for resource monitoring
  - `requests` for HTTP testing

### Database Configuration

Database containers are managed by the toolset:
- PostgreSQL, MySQL, MongoDB supported
- Configured in `toolset/databases/`
- Connected via Docker network `tfb`

## File Structure Reference

From wiki: https://github.com/TechEmpower/FrameworkBenchmarks/wiki/Codebase-File-Structure

Key files:
- **tfb**: Main entry script
- **Dockerfile**: Toolset container
- **entrypoint.sh**: Container entrypoint that drops privileges
- **toolset/run-tests.py**: Test orchestration
- **toolset/benchmark/benchmarker.py**: Core benchmark logic
- **toolset/benchmark/framework_test.py**: Framework test abstraction
- **toolset/utils/docker_helper.py**: Docker operations
- **toolset/test_types/**: Test type implementations and verifications

## Testing and Verification

### Verify Mode

Tests run in "verify" mode check:
- HTTP response codes (200 OK expected)
- Response headers (Content-Type, Content-Length)
- Response body format and content
- Database query results
- XSS prevention in templates

**Note**: Verify mode also takes significantly less time to complete compared to full benchmark runs, making it ideal for development and validation.

Verification logic is in `toolset/test_types/verifications.py`.

### Running Specific Tests

```bash
# Run specific framework
./tfb --mode verify --test framework-name

# Run specific test type
./tfb --mode verify --test framework-name --type json

# Run with specific database
./tfb --mode verify --test framework-name --database postgres
```

## Common Errors and Solutions

### Error: Docker Build Timeout

**Symptom**: Build exceeds 60 minutes
**Solution**: 
- Optimize Dockerfile (better caching, smaller base images)
- Pre-download dependencies in earlier layers
- Check for hanging processes in build

### Error: Port Already in Use

**Symptom**: Test fails to start due to port conflict
**Solution**:
- CI automatically stops MySQL/PostgreSQL system services
- For local development, ensure port 8080 is available
- Check for orphaned containers: `docker ps -a`

### Error: Module Not Found in Python

**Symptom**: `ModuleNotFoundError` when running toolset
**Solution**:
- Always use `./tfb` script (not direct Python execution)
- The script sets up proper PYTHONPATH and environment

### Error: Framework Not Found

**Symptom**: `./tfb --test <name>` reports framework not found
**Solution**:
- Ensure `benchmark_config.json` exists
- Check framework name matches directory name
- Verify JSON syntax is valid

## Memory Considerations

- **Profiling results persistence**: Mount `run_log_dir` as `/profiling-results` volume
- **Micrometer integration** (Vert.x): Use `MicrometerMetricsFactory` with Vert.x Builder API

## Contributing Guidelines

From https://github.com/TechEmpower/FrameworkBenchmarks/wiki/Development-Contributing-Guide:

1. **Ask questions**: Use discussions or issue #2978 for help
2. **Test locally first**: Always verify with `./tfb --mode verify`
3. **Update documentation**: Keep README.md files current
4. **Follow PR template**: Use provided template in `.github/PULL_REQUEST_TEMPLATE.md`
5. **Minimal changes**: Don't include unnecessary files (tests, local configs)

## Issue Reporting

Use template from `.github/ISSUE_TEMPLATE.md`:
- Include OS and kernel version
- Describe expected vs actual behavior
- Provide reproduction steps
- Attach relevant logs

## Useful Commands

```bash
# Clean Docker images
docker system prune -a

# Create Docker network (if missing)
docker network create tfb

# Build toolset container manually
docker build -t techempower/tfb --build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g) .

# Run toolset container interactively
docker run -it --rm --network tfb -v /var/run/docker.sock:/var/run/docker.sock -v $(pwd):/FrameworkBenchmarks techempower/tfb bash

# Inside container: run specific test
python3 /FrameworkBenchmarks/toolset/run-tests.py --mode verify --test framework-name
```

## Load Testing Tools

- **wrk**: Primary load generation tool (Linux only)
- **Apache Bench (ab)**: Alternative for Windows
- Tests use varying connection counts and pipeline depths
- Plaintext tests use pipeline depth of 16

## Resources

- **Main Repo**: https://github.com/TechEmpower/FrameworkBenchmarks
- **Wiki**: https://github.com/TechEmpower/FrameworkBenchmarks/wiki
- **Live Results**: https://tfb-status.techempower.com/
- **Test Overview**: https://github.com/TechEmpower/FrameworkBenchmarks/wiki/Project-Information-Framework-Tests-Overview
- **Contributing Guide**: https://github.com/TechEmpower/FrameworkBenchmarks/wiki/Development-Contributing-Guide
- **File Structure**: https://github.com/TechEmpower/FrameworkBenchmarks/wiki/Codebase-File-Structure

## Quick Start Summary

1. **Install Docker**
2. **Clone repo**: `git clone https://github.com/TechEmpower/FrameworkBenchmarks.git`
3. **Run a test**: `./tfb --mode verify --test gemini`
4. **Add new test**: `./tfb --new`
5. **Check CI**: Changes trigger tests for modified frameworks only

## Important Notes for AI Coding Agents

- **Always use `./tfb` script** - don't try to run Python directly
- **Docker is required** - all tests run in containers
- **Framework changes trigger CI** - only changed frameworks are tested
- **60-minute build timeout** - optimize Docker builds
- **Minimal file inclusion** - no tests, dev configs, or unnecessary code
- **README.md required** - for new frameworks
- **benchmark_config.json is critical** - defines all test configurations
- **Port 8080 standard** - most frameworks expose this port
- **Verify mode is mandatory** - test all changes before submitting
- **Wiki is authoritative** - check wiki for detailed documentation
