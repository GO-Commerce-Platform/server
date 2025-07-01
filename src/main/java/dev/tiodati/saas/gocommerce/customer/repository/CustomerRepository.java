package dev.tiodati.saas.gocommerce.customer.repository;

import dev.tiodati.saas.gocommerce.customer.entity.Customer;
import dev.tiodati.saas.gocommerce.customer.entity.CustomerStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Customer entity operations.
 * Provides database access methods for customer management.
 */
@ApplicationScoped
public class CustomerRepository implements PanacheRepositoryBase<Customer, UUID> {

    /**
     * Find customer by email address.
     *
     * @param email the customer email
     * @return optional customer
     */
    public Optional<Customer> findByEmail(String email) {
        return find("email = ?1", email.toLowerCase())
                .firstResultOptional();
    }

    /**
     * Find active customers with pagination.
     *
     * @param page the page information
     * @return list of active customers
     */
    public List<Customer> findActiveCustomers(Page page) {
        return find("status = ?1 ORDER BY lastName, firstName", CustomerStatus.ACTIVE)
                .page(page)
                .list();
    }

    /**
     * Find customers by status.
     *
     * @param status the customer status
     * @param page   the page information
     * @return list of customers with the specified status
     */
    public List<Customer> findByStatus(CustomerStatus status, Page page) {
        return find("status = ?1 ORDER BY lastName, firstName", status)
                .page(page)
                .list();
    }

    /**
     * Find customers who opted in for marketing emails.
     *
     * @param page the page information
     * @return list of customers who opted in for marketing
     */
    public List<Customer> findMarketingOptInCustomers(Page page) {
        return find("marketingEmailsOptIn = true AND status = ?1 ORDER BY email", CustomerStatus.ACTIVE)
                .page(page)
                .list();
    }

    /**
     * Search customers by name or email.
     *
     * @param searchTerm the search term
     * @param page       the page information
     * @return list of matching customers
     */
    public List<Customer> searchCustomers(String searchTerm, Page page) {
        String likePattern = "%" + searchTerm.toLowerCase() + "%";
        return find(
                "(LOWER(firstName) LIKE ?1 OR LOWER(lastName) LIKE ?1 OR LOWER(email) LIKE ?1) AND status = ?2 ORDER BY lastName, firstName",
                likePattern, CustomerStatus.ACTIVE)
                .page(page)
                .list();
    }

    /**
     * Count customers by status.
     *
     * @param status the customer status
     * @return count of customers with the specified status
     */
    public long countByStatus(CustomerStatus status) {
        return count("status = ?1", status);
    }

    /**
     * Update customer status.
     *
     * @param customerId the customer ID
     * @param status     the new status
     * @return number of updated records
     */
    @jakarta.transaction.Transactional
    public int updateStatus(UUID customerId, CustomerStatus status) {
        return update("status = ?1 WHERE id = ?2", status, customerId);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
