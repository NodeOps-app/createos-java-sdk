# Changelog

All notable changes will be documented here. This project follows Semantic
Versioning and the Keep a Changelog structure.

## Unreleased

### Fixed

- Reject empty HTTP 200 responses without a JSend envelope.
- Update Jackson to 2.18.10 to include upstream security fixes.
- Let template log followers override the request timeout for long builds.
- Use the primary `screen-0` identifier in the desktop example.

### Changed

- Read the default API key from `CREATEOS_API_KEY` instead of
  `CREATEOS_SANDBOX_API_KEY`.
- Prepare signed, tag-triggered Maven Central publishing and GitHub Releases.

### Removed

- GitHub Packages publishing. The previously published `0.1.0` Maven package
  was also deleted; the GitHub Release remains available.

## 0.1.0 - 2026-09-16

### Added

- Initial Java 17 SDK for CreateOS Sandbox
- Sandbox lifecycle, execution, files, managed processes, networking, disks,
  templates, ingress, and desktop APIs
- Compile-checked examples corresponding to the Go SDK examples
- Authentication, path-isolation, pagination, and transport contract tests
- Google Java formatting, Checkstyle, JaCoCo, Javadocs, and publication artifacts
- Tag-triggered GitHub Packages and GitHub Release publishing
