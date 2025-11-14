# Security Best Practices for Running Operaton in Production

This document outlines recommended security practices when deploying and operating Operaton in production environments.

## Table of Contents

- [Disclosure and Communication](#disclosure-and-communication)
- [Network Security](#network-security)
- [Authentication and Authorization](#authentication-and-authorization)
- [Configuration Security](#configuration-security)
- [Database Security](#database-security)
- [Monitoring and Logging](#monitoring-and-logging)
- [Updates and Patching](#updates-and-patching)

## Disclosure and Communication

### Implement security.txt

Deploy a `security.txt` file to provide security researchers with a clear communication channel:

1. Customize the included `security.txt.example` file with your organization's information
2. Sign the file with your PGP key
3. Serve it at `https://your-domain/.well-known/security.txt`
4. Update the expiration date at least annually

For more information, see [RFC 9116](https://www.rfc-editor.org/rfc/rfc9116.html) and visit [securitytxt.org](https://securitytxt.org/).

### Establish a Security Response Process

- Define clear security incident response procedures
- Designate security contacts and escalation paths
- Create a vulnerability disclosure policy
- Consider establishing a bug bounty program

## Network Security

### Use HTTPS/TLS

- Always use TLS 1.2 or higher for all connections
- Configure strong cipher suites
- Use valid certificates from trusted CAs
- Enable HTTP Strict Transport Security (HSTS)

### Reverse Proxy Configuration

When using a reverse proxy (e.g., Caddy, nginx, Apache):

- Implement rate limiting to prevent DoS attacks
- Configure appropriate timeout values
- Filter and validate all incoming requests
- Use Web Application Firewall (WAF) rules when appropriate
- Forward security headers (X-Frame-Options, CSP, etc.)

### Network Isolation

- Deploy Operaton in a private network segment when possible
- Use firewalls to restrict access to necessary ports only
- Implement network segmentation between application tiers
- Consider using a VPN or bastion host for administrative access

## Authentication and Authorization

### Use Production Configuration

- Never use the default configuration in production
- Enable authentication for all web applications and REST API
- Use the included `production.yml` as a starting point:
  ```bash
  ./start.sh --production
  ```

### Strong Authentication

- Enforce strong password policies
- Implement multi-factor authentication (MFA) when possible
- Use OAuth2 for API authentication when appropriate:
  ```bash
  ./start.sh --oauth2 --production
  ```
- Regularly rotate credentials and API keys

### Authorization Best Practices

- Follow the principle of least privilege
- Define granular permissions for users and groups
- Regularly audit user permissions
- Remove or disable unused accounts promptly

## Configuration Security

### Secure Configuration Files

- Protect configuration files with appropriate file permissions (600 or 640)
- Store sensitive values (passwords, keys) in environment variables or secret management systems
- Never commit sensitive configuration to version control
- Use encrypted connections for all external services

### Disable Unnecessary Features

- Disable the example invoice application in production:
  - Remove `--example` flag or exclude from default startup
- Only enable required optional components (webapps, REST API, OAuth2)
- Disable debug endpoints and features

### Environment-Specific Configuration

- Maintain separate configurations for development, staging, and production
- Use the `production.yml` configuration template for production deployments
- Validate configuration changes in non-production environments first

## Database Security

### Database Credentials

- Use strong, unique passwords for database accounts
- Store database credentials securely (e.g., using secret management tools)
- Use separate database accounts for different environments
- Grant minimum required permissions to the Operaton database user

### Database Encryption

- Enable encryption at rest for database storage
- Use encrypted connections (SSL/TLS) between Operaton and the database
- Consider implementing transparent data encryption (TDE) where available

### Database Hardening

- Do not use H2 in production; use a production-grade database (PostgreSQL, MySQL, Oracle, etc.)
- Keep the database server updated with security patches
- Implement database access controls and auditing
- Regular backup and test restore procedures

## Monitoring and Logging

### Enable Security Logging

- Log all authentication attempts (successful and failed)
- Log authorization failures and access denials
- Monitor for suspicious patterns (brute force, unusual API usage)
- Log administrative actions and configuration changes

### Log Management

- Centralize logs for analysis and retention
- Implement log rotation and retention policies
- Ensure logs do not contain sensitive information (passwords, tokens)
- Protect log files with appropriate permissions

### Monitoring and Alerting

- Monitor application and system metrics
- Set up alerts for security-relevant events
- Track failed login attempts and unusual activity
- Monitor resource usage for potential DoS attacks

## Updates and Patching

### Keep Operaton Updated

- Subscribe to security announcements from the Operaton project
- Apply security patches promptly
- Test updates in non-production environments first
- Maintain an update schedule for regular maintenance

### Dependency Management

- Keep all dependencies (Java, libraries, OS packages) up to date
- Monitor for security vulnerabilities in dependencies
- Use tools like OWASP Dependency-Check or Snyk

### Emergency Response

- Have a plan for emergency security updates
- Maintain rollback procedures
- Document the update process for your environment

## Additional Resources

- [Operaton Security Policy](https://github.com/operaton/operaton/security/policy)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [CIS Benchmarks](https://www.cisecurity.org/cis-benchmarks/)

## Contributing

If you have additional security best practices to suggest, please contribute to this document by following the guidelines in [CONTRIBUTING.md](../../../CONTRIBUTING.md).
