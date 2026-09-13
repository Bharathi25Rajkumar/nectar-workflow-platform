package com.nectar.workflow.repository;

import com.nectar.workflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsernameAndTenantId(String username, UUID tenantId);

    @Query("SELECT u FROM User u JOIN FETCH u.tenant WHERE u.username = :username AND u.tenant.id = :tenantId")
    Optional<User> findWithTenantByUsernameAndTenantId(@Param("username")String username, @Param("tenantId") UUID tenantId);
}
