# Full-Stack System Design Projects

A collection of full-stack system design exercises built with **Spring Boot**, **RabbitMQ**, and modern front ends. Each project demonstrates asynchronous messaging, service boundaries, and production-minded packaging (Docker, multi-module Maven).

## Job Scheduling Platform

Distributed job scheduling with a **Spring Boot** backend, **RabbitMQ** for work queues and status events, and an **Angular 17** UI. Producers schedule jobs; workers consume messages; the UI tracks job lifecycle and system health. Suitable for exploring queue-based load distribution, retries, and decoupled worker scaling.

## API Orchestration Gateway

An orchestration layer in front of downstream services: **Spring Boot** coordinates flows, **RabbitMQ** carries async commands and callbacks, and a **React 18** dashboard surfaces gateway traffic, routing, and operational views. Useful for studying API composition, message-driven integration, and gateway patterns versus direct client-to-service calls.

## Design patterns comparison

| Pattern / concern | Job Scheduling Platform | API Orchestration Gateway |
|-------------------|-------------------------|---------------------------|
| Integration style | Queue-centric job pipeline | Gateway + async messaging to backends |
| Primary UI stack | Angular 17 | React 18 |
| Async backbone | RabbitMQ exchanges/queues | RabbitMQ for orchestration events |
| Typical use case | Background work, schedules, workers | API aggregation, routing, cross-service flows |
| Scaling knob | Add consumers / partition queues | Scale gateway and downstream handlers |

## Quick start

1. **Prerequisites:** Java 17+, Maven, Node.js (LTS), Docker and Docker Compose, RabbitMQ (or use Compose where provided).
2. **Per project:** Open the job-scheduling-platform or api-orchestration-gateway folder; follow each project README.md and docker-compose.yml for service ports and startup order.
3. **General flow:** Start infrastructure (for example RabbitMQ), run Spring Boot services, then install dependencies and run the front end (npm install, then ng serve or npm start as documented in each project).

## Tech stack

| Layer | Job Scheduling Platform | API Orchestration Gateway |
|-------|-------------------------|---------------------------|
| Backend | Spring Boot (Java) | Spring Boot (Java) |
| Messaging | RabbitMQ | RabbitMQ |
| Front end | Angular 17 | React 18 |
| Build | Maven, npm | Maven, npm |
| Ops | Docker / Compose | Docker / Compose |

## Author

**Vakkalakula Lokesh**

---
*Repository: [projects-java](https://github.com/vakkalakulalokesh/projects-java)*
