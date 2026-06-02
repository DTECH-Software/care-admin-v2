# 89. Docker and Uptime Monitoring Rules

Status: Current business baseline for BA / QA / client review

## Purpose
Explain operational monitoring and container runtime behavior.

## Business Summary
The application services can run in Docker containers and are monitored by Uptime Kuma using HTTP health checks.

## Main Business Rules
- Docker containers should use restart policy so services restart after server reboot or container failure.
- Container memory limits and JVM memory options should be aligned per service.
- Uptime Kuma monitors each service health endpoint, preferably /actuator/health.
- Health endpoints should return HTTP 200 and body status UP for clean monitoring.
- Down alerts and recovery alerts are sent to configured admin recipients.
- Planned deployments can use maintenance mode to suppress false alerts during expected restarts.
- NGINX/API gateway routing is separate from internal Docker service health checks.

## BA Review Points
- Confirm alert recipients and escalation owner.
- Confirm whether planned maintenance should be recorded before deployments.
- Confirm acceptable health check interval and timeout.

## QA Checkpoints
- Stop one container and verify down email.
- Restart the container and verify recovery email.
- Verify all services return 200 from health endpoint.
- Verify memory limits with docker inspect.

## Client View
- Operations users should receive alerts when services go down and when they recover.

## Related Topics
- 79. Environment/Profile Based Behavior
- 80. CI/CD Deployment Behavior
