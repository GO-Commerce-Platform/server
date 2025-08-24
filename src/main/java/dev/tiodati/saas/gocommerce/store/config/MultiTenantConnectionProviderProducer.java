// CDI Producer disabled to avoid bean conflicts
// Hibernate will use CDI alternatives directly:
// - TestSchemaTenantConnectionProvider with @Alternative @Priority(1) in tests
// - SchemaTenantConnectionProvider in production
// - UnifiedTenantResolver with @PersistenceUnitExtension for tenant resolution

// This approach relies on proper CDI alternative selection rather than
// programmatic bean production which was causing ambiguity.
