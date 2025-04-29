#!/bin/bash

# Script to create GitHub issues for GoCommerce Phase 1
echo "Creating GitHub issues for GoCommerce Phase 1..."

# Create milestone if it doesn't exist yet
echo "Creating Phase 1 milestone..."
gh api repos/:owner/:repo/milestones --method POST -f title="Phase 1" -f state="open" -f description="Infrastructure and core services implementation (4 weeks)" || echo "Milestone may already exist"

# Create labels if they don't exist yet
echo "Creating required labels..."
LABELS=("infrastructure" "multi-tenancy" "high-priority" "api" "security" "authentication" "authorization" "devops" "docker" "ci-cd" "database" "i18n")

for label in "${LABELS[@]}"; do
  echo "Creating label: $label"
  gh label create "$label" --force || echo "Label $label may already exist"
done

# Multi-Tenant Foundation Issues
echo "Creating Multi-Tenant Foundation issues..."

gh issue create --title "Set up schema-per-tenant infrastructure" \
  --body "## Description
Implement the database schema isolation mechanism that supports multiple tenants with proper data separation.

## Tasks
- [ ] Create TenantResolver with subdomain support
- [ ] Implement schema-based multi-tenancy in Hibernate
- [ ] Set up tenant context management

## Acceptance Criteria
- Tenant resolver correctly identifies tenant from subdomain
- Database operations are properly isolated per tenant schema
- Application can switch between tenant contexts correctly

## Epic
Epic 1: Foundation and Infrastructure" \
  --label "infrastructure" --label "multi-tenancy" --label "high-priority" \
  --milestone "Phase 1"

gh issue create --title "Develop tenant provisioning system" \
  --body "## Description
Create APIs and services to register and provision new tenants on the platform.

## Tasks
- [ ] Implement tenant entity model and repository
- [ ] Create tenant registration API
- [ ] Develop automated schema creation for new tenants
- [ ] Set up tenant configuration storage

## Acceptance Criteria
- New tenants can be registered via API
- Tenant database schemas are automatically created
- Tenant configuration is properly stored and retrieved

## Epic
Epic 1: Foundation and Infrastructure" \
  --label "infrastructure" --label "multi-tenancy" --label "api" \
  --milestone "Phase 1"

gh issue create --title "Implement tenant context management" \
  --body "## Description
Create a system to track and manage the current tenant context throughout request processing.

## Tasks
- [ ] Implement tenant identification from requests
- [ ] Create tenant context holder
- [ ] Set up request filters for tenant resolution

## Acceptance Criteria
- Application correctly identifies tenant from incoming requests
- Tenant context is maintained throughout the request lifecycle
- Operations are executed in the correct tenant context

## Epic
Epic 1: Foundation and Infrastructure" \
  --label "infrastructure" --label "multi-tenancy" \
  --milestone "Phase 1"

# Authentication Framework Issues

echo "Creating Authentication Framework issues..."

gh issue create --title "Configure Keycloak integration" \
  --body "## Description
Set up and configure Keycloak as the authentication provider for the platform.

## Tasks
- [ ] Configure Docker setup for Keycloak
- [ ] Set up realm and client settings
- [ ] Configure user properties and initial roles
- [ ] Test basic authentication flow

## Acceptance Criteria
- Keycloak is properly configured in the Docker environment
- Authentication realms and clients are set up
- Basic user roles (Customer, Store Admin, Platform Admin) are configured
- Authentication flow works end-to-end

## Epic
Epic 3: Authentication and Authorization" \
  --label "security" --label "authentication" --label "infrastructure" \
  --milestone "Phase 1"

gh issue create --title "Implement OAuth2 authentication flow" \
  --body "## Description
Create authentication endpoints and token handling for secure API access.

## Tasks
- [ ] Set up OAuth2 endpoints
- [ ] Implement token issuance and validation
- [ ] Create refresh token mechanism
- [ ] Test authentication flows

## Acceptance Criteria
- OAuth2 endpoints function correctly
- JWTs are properly issued and validated
- Refresh token mechanism works as expected
- Authentication flow is secure and reliable

## Epic
Epic 3: Authentication and Authorization" \
  --label "security" --label "authentication" --label "api" \
  --milestone "Phase 1"

gh issue create --title "Develop role-based access control" \
  --body "## Description
Implement RBAC with essential roles for the MVP: Customer, Store Admin, Platform Admin.

## Tasks
- [ ] Define role hierarchy
- [ ] Implement permission validation
- [ ] Create security annotations for endpoints
- [ ] Test role-based access restrictions

## Acceptance Criteria
- Role hierarchy is properly defined
- Permissions are validated correctly
- API endpoints are secured with appropriate annotations
- Access control is enforced according to user roles

## Epic
Epic 3: Authentication and Authorization" \
  --label "security" --label "authorization" \
  --milestone "Phase 1"

# Development Infrastructure Issues

echo "Creating Development Infrastructure issues..."

gh issue create --title "Complete Docker environment setup" \
  --body "## Description
Finalize the containerized environment for all required services.

## Tasks
- [ ] Configure Docker Compose for local development
- [ ] Set up MariaDB container
- [ ] Configure networking between services
- [ ] Document environment setup process

## Acceptance Criteria
- Docker Compose environment runs all required services
- Services are properly connected and configured
- Environment setup is documented

## Epic
Epic 1: Foundation and Infrastructure" \
  --label "infrastructure" --label "devops" --label "docker" \
  --milestone "Phase 1"

gh issue create --title "Set up CI/CD foundation" \
  --body "## Description
Establish basic continuous integration and delivery pipeline.

## Tasks
- [ ] Configure build automation
- [ ] Set up testing framework
- [ ] Create deployment scripts
- [ ] Document CI/CD process

## Acceptance Criteria
- CI/CD pipeline is configured and operational
- Automated builds run successfully
- Testing framework is integrated into the pipeline
- CI/CD process is documented

## Epic
Epic 1: Foundation and Infrastructure" \
  --label "infrastructure" --label "devops" --label "ci-cd" \
  --milestone "Phase 1"

gh issue create --title "Implement database migration framework" \
  --body "## Description
Set up Flyway for database schema version control and migrations.

## Tasks
- [ ] Configure Flyway in the application
- [ ] Create initial migration scripts
- [ ] Test migration process
- [ ] Document migration workflow

## Acceptance Criteria
- Flyway is properly configured
- Database migrations run successfully
- Migration process is tested and verified
- Migration workflow is documented

## Epic
Epic 1: Foundation and Infrastructure" \
  --label "infrastructure" --label "database" \
  --milestone "Phase 1"

gh issue create --title "Configure internationalization (i18n)" \
  --body "## Description
Set up internationalization with English as primary language.

## Tasks
- [ ] Set up i18n configuration
- [ ] Create message resources
- [ ] Implement locale resolution
- [ ] Test language switching

## Acceptance Criteria
- i18n is properly configured
- Message resources are available for English
- Application can handle different locales
- Language switching works as expected

## Epic
Epic 1: Foundation and Infrastructure" \
  --label "infrastructure" --label "i18n" \
  --milestone "Phase 1"

echo "All GitHub issues for Phase 1 have been created!"