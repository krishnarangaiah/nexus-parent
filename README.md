# Nexus - Next Generation Application Monitoring & Management Platform

## 🎭 Overview

**Nexus** is a sophisticated, enterprise-grade application monitoring and management platform built with Spring Boot and Groovy. It provides real-time system metrics collection, WebSocket-based monitoring, and comprehensive application lifecycle management capabilities. The platform is designed as a multi-module Maven project with a modular architecture that separates concerns between the server, monitoring agents, and shared components.

## 🌟 Key Features

### 1. **Real-Time Agent Monitoring System**
The crown jewel of Nexus is its real-time monitoring infrastructure that creates a living, breathing view of your distributed system landscape.

**How It Works:**
- **Autonomous Monitoring Agents**: Lightweight agents (`nexus-agent`) run alongside your applications, continuously collecting JVM metrics such as thread counts, heap memory usage, and system health indicators
- **WebSocket Communication**: Agents establish persistent WebSocket connections using STOMP protocol for bi-directional, low-latency communication with the central server
- **Live Dashboard**: The central Nexus server aggregates metrics from all registered agents and broadcasts them to web clients in real-time, creating a dynamic dashboard that updates every few seconds
- **Heartbeat Mechanism**: Sophisticated heartbeat tracking ensures you know immediately when an agent becomes unresponsive, with configurable timeout thresholds
- **Resilient Connections**: The agent includes automatic reconnection logic with exponential backoff, ensuring monitoring continuity even during network disruptions

**Theatrical Value**: Imagine a conductor orchestrating a symphony - the Nexus server is that conductor, maintaining perfect harmony with dozens of monitoring agents, each playing their part by reporting their health status in real-time. When one instrument falters, the conductor knows instantly.

### 2. **Distributed Agent Architecture**
The platform employs a scalable, distributed architecture where multiple monitoring agents can be deployed across your infrastructure.

**Components:**
- **Agent Registration**: Create and register new monitoring agents through intuitive web forms or RESTful JSON APIs
- **Agent Lifecycle Management**: Full CRUD operations for agent configuration, including display names, unique identifiers, and metadata
- **Scheduled Broadcasting**: The server periodically broadcasts consolidated agent snapshots to all connected clients, ensuring everyone has a consistent view
- **Session Management**: WebSocket sessions are tracked and managed, with proper cleanup on disconnection

**Theatrical Value**: Think of this as a fleet of reconnaissance drones, each independently surveying its territory, yet all reporting back to mission control with perfect coordination. The system scales horizontally - add more drones as your empire grows.

### 3. **Multi-Protocol API Support**
Nexus embraces both traditional form-based web interactions and modern JSON REST APIs, making it accessible to both human operators and automated systems.

**Dual Endpoint Design:**
- **Form-Based Endpoints**: Traditional POST requests with form data for browser-based interactions, complete with redirect-after-post pattern and flash messages
- **JSON REST Endpoints**: Content-negotiated endpoints accepting and returning JSON, perfect for programmatic access and AJAX clients
- **Graceful Degradation**: The same controller methods handle both protocols, with Spring's content negotiation routing to the appropriate handler

**Example Endpoints:**
```
POST /Monitoring/Agent/Create
  - Form: Content-Type: application/x-www-form-urlencoded
  - JSON: Content-Type: application/json
```

**Theatrical Value**: Like a skilled diplomat fluent in multiple languages, Nexus speaks both the language of traditional web applications and the lingua franca of modern microservices.

### 4. **Spring Security Integration with Role-Based Access Control**
A sophisticated authentication and authorization system ensures only authorized personnel can access sensitive operations.

**Security Features:**
- **Multi-Tier User Roles**: Support for ADMIN, SUPER_ADMIN, and USER roles with different privilege levels
- **System Administrator**: Special hard-coded super admin credentials for initial system bootstrap
- **Database-Backed Users**: Regular users stored in H2 database with JPA persistence
- **Session Management**: JDBC-backed HTTP sessions for scalability across multiple server instances
- **Interceptor-Based Access Control**: Global request interceptor validates user authentication before allowing access to protected resources

**Theatrical Value**: Like a medieval castle with multiple gates, each requiring different levels of clearance - from the outer bailey accessible to all, to the inner keep restricted to trusted knights, and finally the throne room reserved for the king alone.

### 5. **WebSocket/STOMP Real-Time Communication**
Built on Spring's WebSocket support with STOMP protocol for structured messaging.

**Architecture:**
- **STOMP Over WebSocket**: Uses Simple Text Oriented Messaging Protocol over WebSocket for request-response and publish-subscribe patterns
- **Message Broker**: In-memory SimpleBroker handles message routing and topic subscriptions
- **Destination Prefixes**: 
  - `/app/*` for application-specific message handling
  - `/topic/*` for broadcast topics (pub-sub)
  - `/user/queue/*` for user-specific messages
- **Session Event Listeners**: Track WebSocket connection lifecycle (connect, disconnect, subscribe events)

**Message Flow:**
```
Agent → /app/metrics → MonitoringAgentController → /topic/metrics → Dashboard
Agent → /app/metrics → Database Update → /topic/agents/status → All Clients
```

**Theatrical Value**: This is the nervous system of Nexus - electrical signals (WebSocket messages) racing along neural pathways (message channels), creating instantaneous reflexes and coordinated responses across the entire organism.

### 6. **Groovy-Powered Domain Models**
Strategic use of Groovy for domain models and services provides expressive, concise code while maintaining Java interoperability.

**Groovy Components:**
- **Domain Models**: Entities like `Agent`, `AppUser`, `RatesComponent` written in Groovy with JPA annotations
- **Service Classes**: Repository and service implementations leveraging Groovy's syntactic sugar
- **Configuration**: `AppConf` and `AppProperty` configuration classes benefit from Groovy's reduced boilerplate
- **Controllers**: Select controllers use Groovy for handling complex business logic with more readable code

**Why Groovy?**
- Reduced boilerplate for POJOs (automatic getters/setters)
- Native JSON support with builders
- Seamless integration with Spring Boot annotations
- Gradual typing for flexibility where needed

**Theatrical Value**: Groovy is the elegant calligraphy overlaying the sturdy foundation of Java - same words, more artfully expressed.

### 7. **Multi-Module Maven Architecture**
Clean separation of concerns through a well-structured multi-module build.

**Modules:**

#### **nexus-parent** (Root POM)
- Dependency management for all modules
- Unified version control for Spring Boot, Groovy, and Log4j2
- Common dependencies like Jackson, Gson, WebJars
- Plugin configuration for Spring Boot and Groovy compilation

#### **nexus-common** (Shared Library)
- Contains `Metrics` DTO shared between server and agents
- WebSocket/STOMP utility classes
- Provides common abstractions for cross-cutting concerns
- Zero external dependencies beyond the parent

#### **nexus** (Main Server Application)
- Spring Boot web application with embedded Tomcat
- All controllers, services, and repositories
- Thymeleaf templates for web UI
- WebSocket server configuration
- H2 database integration
- Security configuration

#### **nexus-agent** (Monitoring Agent)
- Standalone Java application (not Spring Boot)
- Minimal dependencies (Java-WebSocket library)
- JVM metrics collection using `JvmMetricsCollector`
- Resilient WebSocket client with auto-reconnect
- STOMP protocol implementation
- Configuration via `application.properties`

**Theatrical Value**: Like a well-organized theater production - the stage (nexus), the actors (nexus-agent), and the costume department (nexus-common) - each with clear responsibilities, yet working in perfect harmony.

### 8. **Comprehensive User Management**
Full-featured user administration system with authentication and CRUD operations.

**Capabilities:**
- **User Authentication**: Login system with username/password validation
- **Session Tracking**: User sessions tracked and associated with authenticated users
- **User CRUD**: Create, read, update, and delete users through web interface
- **Role Assignment**: Assign roles during user creation/modification
- **Default Passwords**: New users get default password "0000" for first-time setup
- **Flash Messages**: User feedback through flash attributes for success/error messages

**Theatrical Value**: The stage door manager, keeping track of every actor's entrance and exit, ensuring only those with the proper credentials can access the backstage areas.

### 9. **Advanced Metrics Collection**
Deep visibility into JVM health through comprehensive metrics gathering.

**Collected Metrics:**
- **Thread Count**: Active threads in the JVM
- **Heap Memory**: Current usage and maximum available heap
- **Timestamp**: Precise millisecond-level timestamps for each metric snapshot
- **Agent Identification**: Each metric tagged with originating agent ID
- **Extensible Design**: Easy to add CPU, GC, or custom application metrics

**Collection Process:**
1. Agent JVM introspection via `ManagementFactory` MBeans
2. JSON serialization of metrics
3. STOMP message transmission to server
4. Server-side aggregation and storage
5. Broadcast to all connected dashboard clients

**Theatrical Value**: Like a team of stage managers with clipboards, constantly checking lighting levels, sound volume, and prop placement, ensuring the show runs flawlessly.

### 10. **Thymeleaf-Based Web Interface**
Server-side rendered templates for a responsive, professional web UI.

**Features:**
- **Bootstrap 5 Integration**: Modern, responsive design through WebJars
- **jQuery & D3.js**: Interactive charts and data visualizations
- **Bootstrap Icons**: Consistent iconography throughout the interface
- **Live Updates**: Client-side JavaScript subscribes to WebSocket topics for real-time UI updates
- **Template Hierarchy**: Organized template structure with reusable fragments
- **Form Validation**: Client and server-side validation for data integrity

**Page Structure:**
```
/                              → Landing page
/AppUser/LoginForm             → Authentication
/Monitoring/Agent/Landing      → Agent management dashboard
/Monitoring/Agent/Create       → Register new agents
/AppUser/List                  → User administration
```

**Theatrical Value**: The grand proscenium arch through which audiences experience the performance - beautiful, functional, and hiding the complex machinery working behind the scenes.

### 11. **H2 Database with JPA/Hibernate**
Embedded database providing persistence without external dependencies.

**Configuration:**
- **File-Based Storage**: Data persists to `~/jpadb` directory
- **Web Console**: H2 console available at `/h2-console` for direct SQL access
- **Session Store**: JDBC-backed Spring Session for distributed session management
- **Schema Management**: Hibernate DDL set to "validate" mode for production safety
- **Automatic Initialization**: Spring Session tables auto-created

**Entities:**
- `Agent`: Monitoring agent registrations with heartbeat tracking
- `AppUser`: Application users with authentication credentials
- `RatesComponent`: Financial instrument rate components
- `AutosysJob`: Autosys job definitions and metadata

**Theatrical Value**: The theater's archives - preserving every script, every costume design, every set blueprint for future productions.

### 12. **Structured Logging with Log4j2**
Enterprise-grade asynchronous logging with the Disruptor pattern.

**Features:**
- **Async Appenders**: Non-blocking logging using LMAX Disruptor for high throughput
- **Spring Boot Integration**: Excludes default Logback in favor of Log4j2
- **Configurable Levels**: Per-package log level configuration
- **Structured Outputs**: Console and file appenders with customizable patterns
- **Performance**: Minimal latency impact on application threads

**Theatrical Value**: The stage manager's log book, capturing every cue, every entrance, every technical note, without ever slowing down the performance.

## 🏗️ Architecture & Technology Stack

### Core Technologies
- **Java 17**: Modern LTS Java with records, pattern matching, and text blocks
- **Spring Boot 3.2.3**: Latest Spring Framework with native support
- **Groovy 4.0.15**: Concise, expressive JVM language for select components
- **Maven**: Dependency management and multi-module build coordination

### Web & Communication
- **Spring WebSocket**: WebSocket server with STOMP broker
- **Thymeleaf**: Server-side template engine
- **Bootstrap 5.3.3**: Responsive CSS framework
- **jQuery 3.7.1**: DOM manipulation and AJAX
- **D3.js 5.16.0**: Data-driven visualizations
- **SockJS/STOMP**: Fallback WebSocket with messaging protocol

### Data & Persistence
- **Spring Data JPA**: Repository abstraction and ORM
- **Hibernate**: JPA implementation
- **H2 Database 2.2.224**: Embedded SQL database
- **Spring Session JDBC**: Distributed session management

### Agent Communication
- **Java-WebSocket 1.5.5**: Lightweight WebSocket client library
- **STOMP Protocol**: Structured messaging over WebSocket

### Utilities & Libraries
- **Jackson 2.17.0**: JSON serialization/deserialization
- **Gson 2.10.1**: Alternative JSON processor
- **Log4j2 2.23.1**: Asynchronous logging framework
- **Disruptor 3.4.4**: High-performance inter-thread messaging
- **Javassist 3.30.2**: Runtime bytecode manipulation
- **OpenCSV 5.9**: CSV file processing
- **Weka 3.8.6**: Machine learning algorithms

## 📦 Module Details

### nexus-parent
**Purpose**: Root aggregator POM providing unified dependency management

**Key Responsibilities:**
- Version property definitions (Spring Boot, Groovy, Log4j2)
- Dependency version management via `<dependencyManagement>`
- Common dependencies inherited by all modules
- Plugin configuration for Spring Boot and Groovy

**Build Lifecycle:**
```bash
mvn clean install    # Builds all modules in order
```

### nexus-common
**Purpose**: Shared classes used by both server and agents

**Contents:**
- `app.websocket.dto.Metrics`: Metric payload DTO
- `JvmMetricsCollector`: Utility for collecting JVM statistics
- Other shared utilities and abstractions

**Dependencies**: Minimal - only what's inherited from parent

### nexus
**Purpose**: Main server application providing monitoring hub and web interface

**Key Components:**
- `NexusApplication`: Main class and entry point
- `AppConf`: Spring Boot configuration and component scanning
- Controllers: Web endpoints for UI and API
- Services & Repositories: Business logic and data access
- WebSocket Configuration: STOMP broker and endpoint registration
- Security Configuration: Authentication and authorization

**Deployment:**
```bash
cd nexus
mvn spring-boot:run
# Or
java -jar target/nexus-1.0-SNAPSHOT.jar
```

**Default Port**: 9000

### nexus-agent
**Purpose**: Standalone monitoring agent for remote JVM observation

**Key Components:**
- `NexusAgentApp`: Main class with agent lifecycle
- `ResilientWebSocketClient`: Auto-reconnecting WebSocket client
- `StompSession`: STOMP protocol implementation
- `JvmMetricsCollector`: Metrics gathering (from nexus-common)

**Configuration** (`application.properties`):
```properties
server.uri=ws://localhost:9000/ws
agent.id=nexus-agent-001
metrics.interval.seconds=5
reconnect.initial.seconds=2
reconnect.max.seconds=60
```

**Deployment:**
```bash
cd nexus-agent
mvn package
java -jar target/nexus-agent-1.0-SNAPSHOT.jar
```

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Modern web browser (Chrome, Firefox, Safari)

### Installation

1. **Clone the repository:**
```bash
git clone <repository-url>
cd nexus-parent
```

2. **Build the project:**
```bash
mvn clean install
```

3. **Start the Nexus server:**
```bash
cd nexus
mvn spring-boot:run
```

4. **Access the web interface:**
Open your browser to `http://localhost:9000`

5. **Login:**
- Username: `system`
- Password: `system`

### Deploying Monitoring Agents

1. **Register an agent in the web UI:**
   - Navigate to `/Monitoring/Agent/Landing`
   - Click "Create Agent"
   - Provide Agent ID and Display Name

2. **Configure agent properties:**
Edit `nexus-agent/src/main/resources/application.properties`:
```properties
server.uri=ws://localhost:9000/ws
agent.id=my-agent-001
metrics.interval.seconds=5
```

3. **Start the agent:**
```bash
cd nexus-agent
mvn package
java -jar target/nexus-agent-1.0-SNAPSHOT.jar
```

4. **Verify connection:**
The agent dashboard should show your agent with a recent heartbeat timestamp and live metrics.

## 🔧 Configuration

### Server Configuration
Edit `nexus/src/main/resources/application.properties`:

**Application Settings:**
```properties
app.name=Nexus
app.admin.appUser=system
app.admin.password=system
```

**Server Settings:**
```properties
server.port=9000
spring.http.converters.preferred-json-mapper=gson
```

**Database Settings:**
```properties
spring.datasource.url=jdbc:h2:file:~/jpadb;DB_CLOSE_DELAY=-1
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

**Session Settings:**
```properties
spring.session.store-type=jdbc
spring.session.jdbc.initialize-schema=always
```

### Agent Configuration
Edit `nexus-agent/src/main/resources/application.properties`:

```properties
# WebSocket server endpoint
server.uri=ws://localhost:9000/ws

# Unique identifier for this agent
agent.id=nexus-agent-001

# Metrics collection interval (seconds)
metrics.interval.seconds=5

# Reconnection backoff settings
reconnect.initial.seconds=2
reconnect.max.seconds=60
```

## 📊 Usage Examples

### Creating an Agent via REST API

```bash
curl -X POST http://localhost:9000/Monitoring/Agent/Create \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "agent-prod-001",
    "displayName": "Production Server 1",
    "description": "Main production application server"
  }'
```

### WebSocket Client Example (JavaScript)

```javascript
// Connect to WebSocket
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function (frame) {
    console.log('Connected: ' + frame);
    
    // Subscribe to metrics topic
    stompClient.subscribe('/topic/metrics', function (message) {
        const metrics = JSON.parse(message.body);
        console.log('Received metrics:', metrics);
        updateDashboard(metrics);
    });
    
    // Subscribe to agent status updates
    stompClient.subscribe('/topic/agents/status', function (message) {
        const agents = JSON.parse(message.body);
        console.log('Agent status update:', agents);
        refreshAgentList(agents);
    });
});
```

### Sending Commands to Agents

The system supports server-to-agent commands via WebSocket:

```java
// From server controller
messagingTemplate.convertAndSendToUser(
    agentId, 
    "/queue/commands", 
    "CMD:SET_INTERVAL=10"
);
```

**Supported Commands:**
- `PING`: Health check (agent responds with `PONG`)
- `CMD:RESTART`: Request agent restart
- `CMD:SET_INTERVAL=N`: Change metrics collection interval

## 🎬 Real-World Scenario

Imagine you're managing a microservices architecture with 50 services across 10 servers. Here's how Nexus orchestrates this symphony:

1. **Initial Setup**: Deploy the Nexus server on a monitoring host. The server starts its WebSocket endpoint and waits for agents to connect.

2. **Agent Deployment**: On each application server, deploy a nexus-agent alongside your services. Each agent registers with a unique ID like `api-server-01`, `db-server-02`, etc.

3. **Real-Time Monitoring**: Operators log into the Nexus web interface. They see a live grid of all 50 agents, each showing current thread count, heap usage, and last-seen timestamp. The grid updates every 10 seconds via WebSocket broadcasts.

4. **Incident Detection**: One application server starts experiencing memory pressure. The heap usage metric for `api-server-07` climbs from 60% to 85% to 95%. Operators see this in real-time, before any user-facing issues occur.

5. **Historical Analysis**: After resolving the incident, the team queries the H2 database to extract historical metrics, identifying that the memory spike correlates with a specific batch job that runs at 2 AM.

6. **Scaling Decision**: Based on continuous metrics, the team notices several agents consistently running at high thread counts. They make data-driven decisions to scale horizontally, adding more application instances.

This is Nexus in action - not just monitoring, but enabling proactive operations through real-time visibility.

## 🔐 Security Considerations

1. **Change Default Credentials**: Immediately update the system admin password in production
2. **HTTPS/WSS**: Use secure WebSocket (WSS) and HTTPS in production environments
3. **Database Security**: Move from H2 to PostgreSQL/MySQL with proper access controls
4. **Session Timeout**: Configure appropriate session timeout values
5. **Agent Authentication**: Implement agent authentication tokens to prevent unauthorized agents
6. **CORS Policy**: Review and restrict `setAllowedOrigins("*")` in production

## 🎯 Future Enhancements

- **Alert System**: Email/SMS notifications for metric threshold breaches
- **Historical Charts**: Time-series visualization with D3.js/Chart.js
- **Agent Auto-Discovery**: Zero-configuration agent registration via service discovery
- **Distributed Tracing**: Integration with OpenTelemetry for request tracing
- **Custom Metrics**: Plugin system for application-specific metrics
- **High Availability**: Redis-backed message broker for multi-server deployments
- **Machine Learning**: Weka integration for anomaly detection in metrics

## 📝 License

This project is part of the `gcliff.next.gen` organization. All rights reserved.

## 🤝 Contributing

Contributions are welcome! Please ensure all tests pass and code follows existing patterns before submitting pull requests.

---

**Built with ❤️ using Spring Boot, Groovy, and WebSocket technology**
