# 80. CI/CD Deployment Behavior

Status: Current business baseline for BA / QA / client review

## Purpose
Explain deployment and runtime operations for the Care services.

## Business Summary
Services are built as Spring Boot jars and can run directly with Java or inside Docker containers. Monitoring is handled outside the application using Uptime Kuma.

## Main Business Rules
- Services can be deployed with Docker using service-specific Dockerfiles and docker-compose files.
- Containers should use restart policy so they come back after service failure or server reboot.
- Java memory limits should be configured with container memory limits and JVM options where required.
- Health checks should use /actuator/health where exposed and allowed by security configuration.
- Uptime Kuma monitors service health and sends down/recovery emails to configured recipients.
- During planned deployment, monitors can be placed in maintenance to avoid false down alerts.
- GitHub Actions deployment should stop old Java processes or containers before starting the new containerized service.

## BA Review Points
- Confirm required monitoring recipients and escalation path.
- Confirm acceptable downtime window during deployment.
- Confirm which environments require Docker deployment.

## QA Checkpoints
- Verify all service containers restart after reboot.
- Verify /actuator/health returns HTTP 200 for monitored services.
- Verify Uptime Kuma sends down and recovery emails.
- Verify memory limits are applied using docker inspect.

## Client View
- The system should restart predictably, expose health status, and notify responsible users when a service fails or recovers.

## Related Topics
- 79. Environment/Profile Based Behavior
- 89. Docker and Uptime Monitoring Rules
