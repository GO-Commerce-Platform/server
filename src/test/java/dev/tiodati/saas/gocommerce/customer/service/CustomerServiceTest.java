package dev.tiodati.saas.gocommerce.customer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.tiodati.saas.gocommerce.customer.dto.CreateCustomerDto;
import dev.tiodati.saas.gocommerce.customer.dto.CustomerDto;
import dev.tiodati.saas.gocommerce.customer.entity.Customer;
import dev.tiodati.saas.gocommerce.customer.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    void testCreateCustomer() {
        CreateCustomerDto createDto = new CreateCustomerDto("test@example.com", "Test", "User", null, null, null, null, null, null, null, null, null, false, "en");
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setEmail(createDto.email());
        customer.setFirstName(createDto.firstName());
        customer.setLastName(createDto.lastName());

        // No stubbing needed for this test as we are verifying the mapping logic

        CustomerDto result = customerService.createCustomer(UUID.randomUUID(), createDto);

        assertNotNull(result);
        assertEquals(createDto.email(), result.email());
    }
}
