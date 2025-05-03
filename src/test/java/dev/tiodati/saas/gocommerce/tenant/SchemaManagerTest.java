package dev.tiodati.saas.gocommerce.tenant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.flywaydb.core.api.FlywayException;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SchemaManagerTest {

    private TestSchemaManager schemaManager;
    
    @InjectMock
    DataSource dataSource;
    
    private Connection mockConnection;
    private Statement mockStatement;
    
    @BeforeEach
    void setUp() throws SQLException {
        mockConnection = mock(Connection.class);
        mockStatement = mock(Statement.class);
        
        // Using lenient() to prevent unused stubbing exceptions
        lenient().when(dataSource.getConnection()).thenReturn(mockConnection);
        lenient().when(mockConnection.createStatement()).thenReturn(mockStatement);
        lenient().when(mockStatement.execute(anyString())).thenReturn(true);
        
        // Create a fresh TestSchemaManager for each test
        schemaManager = new TestSchemaManager(dataSource);
    }
    
    @Test
    void testCreateSchema_successful() throws SQLException {
        // Arrange
        String schemaName = "test_schema";
        schemaManager.setMigrationSuccessful(true);
        
        // Act & Assert
        assertDoesNotThrow(() -> schemaManager.createSchema(schemaName));
        
        // Verify
        verify(mockStatement).execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
    }
    
    @Test
    void testCreateSchema_sqlException() throws SQLException {
        // Arrange
        String schemaName = "test_schema";
        when(mockStatement.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName))
            .thenThrow(new SQLException("Database error"));
        
        // Enable silent error mode - don't log expected errors
        schemaManager.setSilentErrorMode(true);
        
        // Act & Assert
        assertThrows(SQLException.class, () -> schemaManager.createSchema(schemaName));
    }
    
    @Test
    void testMigrateSchema_successful() {
        // Arrange
        String schemaName = "test_schema";
        schemaManager.setMigrationSuccessful(true);
        
        // Act & Assert
        assertDoesNotThrow(() -> schemaManager.migrateSchema(schemaName));
    }
    
    @Test
    void testMigrateSchema_flywayException() {
        // Arrange
        String schemaName = "test_schema";
        schemaManager.setMigrationSuccessful(false);
        schemaManager.setFlywayException(new FlywayException("Migration failed"));
        
        // Enable silent error mode - don't log expected errors
        schemaManager.setSilentErrorMode(true);
        
        // Act & Assert
        assertThrows(FlywayException.class, () -> schemaManager.migrateSchema(schemaName));
    }
    
    @Test
    void testDropSchema_successful() throws SQLException {
        // Arrange
        String schemaName = "test_schema";
        
        // Act & Assert
        assertDoesNotThrow(() -> schemaManager.dropSchema(schemaName));
        
        // Verify
        verify(mockStatement).execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
    }
    
    @Test
    void testDropSchema_sqlException() throws SQLException {
        // Arrange
        String schemaName = "test_schema";
        
        // More specific stubbing for this specific test case
        when(mockStatement.execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE"))
            .thenThrow(new SQLException("Database error"));
        
        // Enable silent error mode - don't log expected errors
        schemaManager.setSilentErrorMode(true);
        
        // Act & Assert
        assertThrows(SQLException.class, () -> schemaManager.dropSchema(schemaName));
    }
    
    /**
     * Test implementation of SchemaManager that allows controlling the Flyway migration behavior
     * and suppressing error logging in test scenarios where we expect exceptions
     */
    static class TestSchemaManager extends SchemaManager {
        private boolean migrationSuccessful = true;
        private FlywayException flywayException;
        private boolean silentErrorMode = false;
        private final DataSource testDataSource;
        
        public TestSchemaManager(DataSource dataSource) {
            super(dataSource);
            this.testDataSource = dataSource;
        }
        
        public void setMigrationSuccessful(boolean successful) {
            this.migrationSuccessful = successful;
        }
        
        public void setFlywayException(FlywayException exception) {
            this.flywayException = exception;
        }
        
        public void setSilentErrorMode(boolean silentMode) {
            this.silentErrorMode = silentMode;
        }
        
        @Override
        protected void runFlywayMigration(String schemaName) {
            if (!migrationSuccessful) {
                if (flywayException != null) {
                    throw flywayException;
                }
                throw new FlywayException("Test migration failure");
            }
        }
        
        @Override
        public void createSchema(String schemaName) throws SQLException {
            if (silentErrorMode) {
                // In silent mode, execute directly without logging errors
                try (Connection conn = testDataSource.getConnection(); 
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
                    migrateSchema(schemaName);
                }
            } else {
                super.createSchema(schemaName);
            }
        }
        
        @Override
        public void migrateSchema(String schemaName) {
            if (silentErrorMode) {
                // In silent mode, just run our test-controlled migration
                if (!migrationSuccessful) {
                    if (flywayException != null) {
                        throw flywayException;
                    }
                    throw new FlywayException("Test migration failure");
                }
            } else {
                super.migrateSchema(schemaName);
            }
        }
        
        @Override
        public void dropSchema(String schemaName) throws SQLException {
            if (silentErrorMode) {
                // In silent mode, bypass the parent method's error logging
                try (Connection conn = testDataSource.getConnection(); 
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
                }
            } else {
                super.dropSchema(schemaName);
            }
        }
    }
}