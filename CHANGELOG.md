# Changelog

All notable changes to the GoCommerce project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Internationalization (i18n) framework with support for English, Spanish, Brazilian Portuguese (pt_BR) and Portuguese
  - Implemented message bundles for all four languages
  - Created `MessageService` for accessing localized messages
  - Added locale resolution via URL, cookies, or Accept-Language header
  - Provided REST endpoints for locale management
  - Added comprehensive documentation in I18N.md
  - Added specific support for Brazilian Portuguese (pt_BR) as the secondary language