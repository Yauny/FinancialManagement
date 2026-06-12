# Repository Guidelines

A contributor guide for the FinancialManagement project — a personal finance management system with a Spring Boot backend and React frontend.

## Project Structure

```
backend/                         # Spring Boot 3.3 (Java 17)
├── src/main/java/com/fm/
│   ├── FinancialManagementApplication.java   # Entry point
│   ├── account/                # Account bounded context
│   │   ├── controller/
│   │   ├── service/
│   │   └── repository/         # MyBatis Plus data access
│   └── common/                 # Shared utilities, config, constants
├── src/main/resources/
│   ├── application.yml          # Base config
│   └── db/init.sql             # Database schema
frontend/                       # React + Ant Design (planned)
```

Backend package structure follows `<group>.<context>/` — currently `com.fm.account/` for the account domain. Each context follows **Controller → Service → Repository** layers.

## Build & Run

```bash
# Backend
cd backend
mvn clean compile              # Compile
mvn spring-boot:run           # Run locally (port 8080)
mvn test                       # Run tests (H2 in-memory)

# Frontend (once scaffolded)
cd frontend
npm install
npm start                      # Run on port 3000
```

## Coding Conventions

This project follows strict layered architecture. Key rules from `rules/`:

| Rule File | Key Points |
|-----------|-----------|
| `rules/coding-style.md` | Prefer immutability; files ≤800 lines; explicit error handling |
| `rules/project-structure.md` | `IXxxService` for repository layer; `XxxService` for business layer; no cross-layer imports |
| `rules/api-design.md` | Full POST + JSON body; all logs must include `traceId` and `[methodName]` prefix |
| `rules/testing.md` | TDD workflow; 80% minimum coverage |
| `rules/security.md` | No hardcoded secrets; SQL via ORM Wrappers only; no `Access-Control-Allow-Origin: *` |

**Immutability is critical**: never mutate existing objects. Always return new copies.

**Naming patterns**:
- `XxxDO` — table entity (`repository/entity/`)
- `XxxBO` — internal service DTO (`service/bo/`)
- `XxxReqVO` / `XxxRespVO` — HTTP request/response (`controller/vo/`)
- `IAccountService` — repository layer interface
- `AccountService` — business layer service

## Testing Guidelines

- Test framework: **Spring Boot Test** with JUnit
- Test database: **H2 in-memory** (configured in `pom.xml`)
- Naming: `XxxServiceTest.java`, `XxxControllerTest.java`
- Coverage target: **80%**
- Follow TDD: write failing test first → implement minimal code → refactor

## Commit & Pull Request

Since this repo has a single initial commit, no formal commit convention is established yet. Follow conventional commits informally:

```
feat: add transaction CRUD endpoints
fix: validate required fields in AccountController
docs: update API design rules
```

PR requirements:
- Title describes the change
- Link any related issues
- Tests included for new functionality
- No hardcoded secrets or sensitive data in logs

---

# Project Rules

When working in this project, **you must** follow these rules:

| Rule File | Purpose |
|-----------|---------|
| `rules/coding-style.md` | Immutability, file organization, error handling, input validation |
| `rules/project-structure.md` | Package organization, naming conventions, layer hierarchy |
| `rules/api-design.md` | Controller standards, VO naming, log format |
| `rules/testing.md` | Testing standards |
| `rules/security.md` | Security standards |

### Key Rules Summary

**Coding Style**:
- Prefer creating new objects over mutating existing ones (immutability pattern)
- Files target 200-400 lines, hard cap at 800 lines
- Explicit error handling at every layer

**Project Structure**:
- Single Maven module + multiple bounded contexts, organized by `group.context/`
- Strict **Controller → Service → Repository** layering
- Repository layer uses `I*Service` prefix; business layer uses `XxxService`

**API Design**:
- All POST + JSON body request/response
- All logs must include `traceId` and `[methodName]` prefix
- Controller only: parse request → validate params → call service → build response

---

# Autonomous Development Loop

### 1. PROGRESS.md Driven

Maintain `PROGRESS.md` in the project root as the development "status panel":

```markdown
# Project Progress

## Current Phase
[Phase name]

## Completed
- [x] Completed item 1
- [x] Completed item 2

## In Progress
- [ ] In-progress item (currently working on)

## Pending
- [ ] Pending item 1
- [ ] Pending item 2
```

**Rules**:
- Update PROGRESS.md first when starting work
- Mark completed modules immediately and move to the next
- When blocked, record the issue and continue with the next item

### 2. Verify First

Verify each module **immediately after completion**, not at the end:

```
1. Write code → mvn compile to check syntax errors
2. Start service → verify startup succeeds
3. Call API → verify functionality works
4. Pass → record in PROGRESS.md → move to next
```

**Forbidden**: writing all code then compiling together (problems pile up, hard to locate)

### 3. Quick Fix Loop

When发现问题:

```
发现问题 → 分析原因 → 修复 → 验证 → 通过则继续
                        ↓ 未通过
                   重新分析 → 修复 → 验证
```

### 4. 5-Times-Bail Rule

If the **same problem is not solved after 5 attempts, immediately skip**:

- Record the issue in PROGRESS.md's "Blocked Items"
- Continue with other features
- Handle later or ask for user assistance

### 5. End-to-End Testing

After all features are complete, perform one complete test from the **user's perspective**:

- Start the service
- Starting from the first API, call through the business flow
- Verify data flow, return values, and exception handling
- Ensure the entire chain is smooth

---

# Using Extension Capabilities

### Context7 MCP - Query Latest Docs

When needing to query latest usage of a library/framework:

```
/context7 查询 Spring Boot 3.4 的新特性
/context7 MyBatis Plus 3.5 的用法变化
```

### Firecrawl MCP - Web Search

When needing to search for latest tech articles, solutions:

```
/firecrawl-search 搜索 React 18 的最佳实践
/firecrawl-scrape 抓取某技术文档页面
```

### Frontend Design Skill - Frontend Beautification

When doing frontend development, activate the skill:

```
使用 frontend-design 技能美化这个界面
```

### Firecrawl Skill Series

| Skill | Purpose |
|-------|---------|
| `firecrawl-scrape` | Extract markdown from URL |
| `firecrawl-search` | Search and get full page content |
| `firecrawl-crawl` | Batch scrape entire website |
| `firecrawl-interact` | Interactive browser operations |

---

# Workflow

1. **Start**: Read relevant rule files, update PROGRESS.md
2. **Code**: Follow rules, small-step progression
3. **Verify**: Compile and run immediately after each module
4. **Blocked**: Skip after 5 attempts, record the issue
5. **Query**: Use Context7 for unfamiliar APIs, Firecrawl for solutions
6. **Complete**: End-to-end test to ensure the entire chain is smooth

---

# When Rules Are Applied

AI automatically reads files in `rules/` and applies them at:
- Checking naming and structure when adding code
- Checking for rule violations when modifying code
- Checking coverage requirements when writing tests
- Maintaining PROGRESS.md as development status panel

---

# Key Notes

- Single Maven module with multiple bounded contexts (no submodules)
- All controller endpoints are **POST-only** with JSON body
- User context comes from the request; no JWT parsing in controllers
- Sensitive values (appId, tokens) must come from configuration, never hardcoded
- Scheduled tasks go in `task/` package, not scattered in services
