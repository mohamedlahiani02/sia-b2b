package tn.sia.b2b.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.sia.b2b.identity.domain.User;
import tn.sia.b2b.identity.domain.UserStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByStatus(UserStatus status);

    List<User> findAllByCustomerSourceRef(String customerSourceRef);
}
