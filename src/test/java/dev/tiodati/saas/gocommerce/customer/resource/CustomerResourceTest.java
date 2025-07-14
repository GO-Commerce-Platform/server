package dev.tiodati.saas.gocommerce.customer.resource;

import dev.tiodati.saas.gocommerce.customer.dto.CreateCustomerDto;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@TestSecurity(user = "test-user", roles = { "PLATFORM_ADMIN" })
class CustomerResourceTest {

    @Test
    void testCreateCustomer() {
        CreateCustomerDto createDto = new CreateCustomerDto("test@example.com", "Test", "User", null, null, null, null, null, null, null, null, null, false, "en");

        given()
                .contentType(ContentType.JSON)
                .body(createDto)
                .when()
                .post("/api/v1/stores/{storeId}/customers", UUID.randomUUID())
                .then()
                .statusCode(201)
                .body("email", is(createDto.email()));
    }
}
