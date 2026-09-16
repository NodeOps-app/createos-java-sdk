# Security policy

## Reporting a vulnerability

Do not open a public issue for a suspected vulnerability. Report it privately
through the GitHub Security tab for `NodeOps-app/createos-java-sdk`, including
the affected SDK version, reproduction steps, impact, and any suggested fix.

Avoid including live API keys, sandbox identifiers, response bodies containing
secrets, or customer data. Revoke any credential that may have been exposed
during testing.

## Supported versions

Until the first stable release, security fixes are applied to the latest
published `0.x` version. After `1.0.0`, the latest minor release receives
security updates. Older versions should be upgraded before reporting an issue.

## SDK security boundaries

The SDK protects API credentials from caller header overrides, unauthenticated
requests, redirects, and cross-origin requests. CreateOS authorization and
workload isolation are enforced by the service. Applications remain responsible
for secret storage, command authorization, output handling, and timely resource
cleanup.
