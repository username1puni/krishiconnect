package com.krishiconnect.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.krishiconnect.entity.Role;
import com.krishiconnect.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    long countByRole(Role role);

    @Query("""
            select count(u) from User u
            where u.role in (com.krishiconnect.entity.Role.PROVIDER, com.krishiconnect.entity.Role.SPECIALIST)
              and u.verified = false and u.enabled = true
            """)
    long countPendingVerification();

    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (:verified is null or u.verified = :verified)
              and (:q is null or lower(u.fullName) like lower(concat('%', :q, '%'))
                   or u.phone like concat('%', :q, '%')
                   or lower(u.email) like lower(concat('%', :q, '%'))
                   or lower(u.village) like lower(concat('%', :q, '%')))
            """)
    Page<User> search(@Param("role") Role role, @Param("verified") Boolean verified, @Param("q") String q,
                      Pageable pageable);
}
