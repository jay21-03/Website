package com.bautruc.ecommerce.identity.infrastructure;

import java.util.List;
import java.util.Optional;

import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserJpaRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmail(String email);

    long countByRoleAndStatus(UserRole role, UserStatus status);

    List<User> findByRoleAndStatusOrderById(UserRole role, UserStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u
            from User u
            where u.role = com.bautruc.ecommerce.identity.domain.UserRole.ADMIN
              and u.status = com.bautruc.ecommerce.identity.domain.UserStatus.ACTIVE
            order by u.id
            """)
    List<User> findActiveAdminsForUpdate();
}
