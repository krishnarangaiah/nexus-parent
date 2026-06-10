# Nexus — Remote Agent Management Platform

Nexus is a Spring Boot–based platform for registering, monitoring, and remotely executing scripts on distributed agents. It follows a hub-and-spoke architecture: a central web server (`nexus-web`) exposes a browser UI and a WebSocket endpoint; lightweight agent processes (`nexus-agent`) connect to that endpoint and execute commands on demand.

---

## Table of Contents

- [Project Structure](#project-structure)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Build](#build)
- [Run Configuration](#run-configuration)
  - [nexus-web (Server)](#nexus-web-server)
  - [nexus-agent (Client)](#nexus-agent-client)
- [Usage](#usage)
  - [1. Register an Agent](#1-register-an-agent)
  - [2. Start an Agent](#2-start-an-agent)
  - [3. Manage Command Scripts](#3-manage-command-scripts)
  - [4. Execute a Command](#4-execute-a-command)
  - [5. View Execution History](#5-view-execution-history)
- [Key Configuration Properties](#key-configuration-properties)
  - [nexus-web](#nexus-web)
  - [nexus-agent](#nexus-agent)
- [Database](#database)
- [WebSocket Topics](#websocket-topics)
- [Modules Overview](#modules-overview)

---

## Project Structure

```
nexus-parent/          ← Maven parent POM (dependency management)
├── nexus-core/        ← Shared DTOs, protocol definitions, and common utilities
├── nexus-web/         ← Spring Boot web server (UI + WebSocket hub)
└── nexus-agent/       ← Standalone agent process (connects back to nexus-web)
```

---

## Features

| Feature | Description |
|---|---|
| **Agent Registration** | Pre-register agents in the UI with a UUID before they connect |
| **Real-time Connectivity** | Agents connect over plain WebSocket; connection status is reflected live |
| **Heartbeat Monitoring** | Agents emit periodic heartbeats carrying CPU and memory metrics |
| **Groovy Script Execution** | Author Groovy scripts in the UI and dispatch them to one or many agents |
| **Command Console** | Browser-based console to select agents, pick a script, and fire execution |
| **Execution History** | Full audit trail of every command dispatched — status, output, duration |
| **Dashboard** | KPI summary: total/online agents, active scripts, today's executions |
| **Agent Groups** | Logically group agents for easier management |
| **User Management** | Built-in user/role model with optional login module |
| **Session Persistence** | Spring Session backed by JDBC for distributed-friendly sessions |
| **Async Logging** | Log4j2 with LMAX Disruptor for high-throughput async logging |

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17, Groovy 4 |
| Framework | Spring Boot 3.2 |
| Web / UI | Thymeleaf, Bootstrap 5, jQuery, D3.js |
| WebSocket | Spring WebSocket (STOMP + SockJS) · Java WebSocket API (Tyrus client) |
| Persistence | Spring Data JPA + H2 (file-based) |
| Session | Spring Session JDBC |
| Logging | Log4j2 + LMAX Disruptor |
| Build | Maven 3 (multi-module) |

---

## Prerequisites

- **Java 17+**
- **Maven 3.8+**

---

## Build

Build all modules from the project root:

```bash
mvn clean install
```

To skip tests:

```bash
mvn clean install -DskipTests
```

The build produces two executable JARs:

| Module | Artifact |
|---|---|
| `nexus-web` | `nexus-web/target/nexus-web-1.0-SNAPSHOT.jar` |
| `nexus-agent` | `nexus-agent/target/nexus-agent-1.0-SNAPSHOT.jar` |

---

## Run Configuration

### nexus-web (Server)

Start the web server (default port **9000**):

```bash
java -jar nexus-web/target/nexus-web-1.0-SNAPSHOT.jar
```

Open the UI in a browser:

```
http://localhost:9000
```

The H2 console is available at:

```
http://localhost:9000/h2-console
```

(JDBC URL: `jdbc:h2:file:~/jpadb`, username: `sa`, no password)

---

### nexus-agent (Client)

Each agent **must** be assigned a UUID that is pre-registered in the `nexus-web` database. Pass the UUID as a command-line argument:

```bash
java -jar nexus-agent/target/nexus-agent-1.0-SNAPSHOT.jar \
  --agent.uuid=<YOUR-UUID>
```

Override the server URL if `nexus-web` is not running locally:

```bash
java -jar nexus-agent/target/nexus-agent-1.0-SNAPSHOT.jar \
  --agent.uuid=<YOUR-UUID> \
  --agent.websocket.url=ws://<HOST>:9000/ws-plain
```

The agent is a non-web Spring Boot application. It connects via WebSocket, registers itself with its hostname, OS, and Java version, then waits for commands.

---

## Usage

### 1. Register an Agent

Before starting an agent process, register its UUID in the UI:

1. Navigate to **Monitoring → Agents**.
2. Click **Create Agent**.
3. Fill in a UUID (e.g. generated with `uuidgen`) and a display name.
4. Submit — the agent is now allowed to connect.

### 2. Start an Agent

Use the UUID from step 1:

```bash
java -jar nexus-agent/target/nexus-agent-1.0-SNAPSHOT.jar \
  --agent.uuid=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

The agent will appear as **ONLINE** in the Agents list once it registers.

### 3. Manage Command Scripts

1. Navigate to **Command → Scripts**.
2. Click **Create Script** and write a Groovy script.
3. Mark it **active** to make it available in the console.

### 4. Execute a Command

1. Navigate to **Command → Console**.
2. Select one or more **online** agents.
3. Choose a script from the dropdown.
4. Click **Execute** — results are returned asynchronously.

### 5. View Execution History

Navigate to **Command → History** to see all past executions with status, output, error output, exit code, and duration.

---

## Key Configuration Properties

### nexus-web

File: `nexus-web/src/main/resources/application.properties`

| Property | Default | Description |
|---|---|---|
| `server.port` | `9000` | HTTP port |
| `app.name` | `Nexus` | Application display name |
| `app.admin.appUser` | `system` | Default admin username |
| `app.admin.password` | `system` | Default admin password |
| `app.user.login.module` | `false` | Enable/disable the login screen |
| `spring.datasource.url` | `jdbc:h2:file:~/jpadb` | H2 database file path |
| `spring.h2.console.enabled` | `true` | Enable H2 browser console |
| `spring.session.store-type` | `jdbc` | Session persistence backend |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Schema validation mode |

### nexus-agent

File: `nexus-agent/src/main/resources/application.properties`

| Property | Default | Description |
|---|---|---|
| `agent.uuid` | *(required)* | UUID of this agent — must match a pre-registered record |
| `agent.websocket.url` | `ws://localhost:9000/ws-plain` | WebSocket endpoint of nexus-web |
| `agent.heartbeat.interval-ms` | `30000` | Heartbeat interval in milliseconds |
| `agent.reconnect.delay-ms` | `5000` | Reconnect delay after disconnection |
| `agent.version` | `1.0.0` | Agent version reported to server |

The `agent.uuid` can also be supplied via the environment variable `AGENT_UUID`:

```bash
export AGENT_UUID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
java -jar nexus-agent/target/nexus-agent-1.0-SNAPSHOT.jar
```

---

## Database

Nexus uses an **H2 file-based database** (`~/jpadb`) by default. The schema is managed by JPA (`ddl-auto=validate`), so the tables must exist before startup. On first run, set `ddl-auto=update` or `create` to bootstrap the schema, then revert to `validate`.

Session data is persisted via Spring Session JDBC (`spring.session.jdbc.initialize-schema=always`).

---

## WebSocket Topics

| Topic / Endpoint | Direction | Description |
|---|---|---|
| `/ws-plain` | Agent → Server | Plain WebSocket endpoint for agent connections |
| `/ws` (STOMP/SockJS) | Browser ↔ Server | STOMP broker for UI real-time updates |
| `/topic/dashboard` | Server → Browser | Dashboard metric pushes |
| `/topic/agent/*` | Server → Browser | Per-agent status updates |

---

## Modules Overview

### nexus-core

Shared library consumed by both `nexus-web` and `nexus-agent`. Contains:

- **DTOs**: `CommandRequest`, `CommandResponse`, `HeartbeatMessage`, `RegistrationRequest`
- **Protocol**: `NexusProtocol` (JSON message builder/parser), `MessageType` constants
- **Ontology**: `CommunicationProtocol`

### nexus-web

Spring Boot web application. Contains:

- **Controllers**: Dashboard, Agent Monitoring, Command Console, User, Admin
- **Services**: `AgentService`, `CommandScriptService`, `CommandExecutionService`, `UserService`
- **WebSocket**: `AgentConnectionManager`, `AgentWsController`, `DashboardWsController`, publishers
- **Entities**: `Agent`, `AgentGroup`, `CommandScript`, `CommandExecution`, `AppUser`

### nexus-agent

Standalone Spring Boot application (no web server). Contains:

- **`AgentRunner`**: Main WebSocket connection/reconnection loop
- **`CommandDispatcher`**: Routes commands to `CommandHandler` implementations (including Groovy script execution)
- **Built-in handlers**: `LsCommandHandler`, `PwdCommandHandler`
