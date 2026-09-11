package tn.sia.b2b.identity.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "tax_id", nullable = false)
    private String taxId;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(nullable = false)
    private String phone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> address;

    @Column(name = "customer_source_ref")
    private String customerSourceRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {}

    public static User createPending(UUID id, String email, String passwordHash,
                                     String companyName, String taxId, String contactName,
                                     String phone, Map<String, Object> address) {
        User u = new User();
        u.id = id;
        u.email = email.toLowerCase().trim();
        u.passwordHash = passwordHash;
        u.role = UserRole.VIEWER;
        u.status = UserStatus.PENDING;
        u.companyName = companyName;
        u.taxId = taxId;
        u.contactName = contactName;
        u.phone = phone;
        u.address = address;
        u.createdAt = Instant.now();
        return u;
    }

    public void activate(String customerSourceRef) {
        this.status = UserStatus.ACTIVE;
        this.customerSourceRef = customerSourceRef;
    }

    public void close() {
        this.status = UserStatus.CLOSED;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isPending() {
        return status == UserStatus.PENDING;
    }

    public boolean isSuspended() {
        return status == UserStatus.SUSPENDED;
    }

    public boolean isLinkedToCustomer() {
        return customerSourceRef != null && !customerSourceRef.isBlank();
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public String getCompanyName() { return companyName; }
    public String getTaxId() { return taxId; }
    public String getContactName() { return contactName; }
    public String getPhone() { return phone; }
    public Map<String, Object> getAddress() { return address; }
    public String getCustomerSourceRef() { return customerSourceRef; }
    public Instant getCreatedAt() { return createdAt; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(UserRole role) { this.role = role; }
}
