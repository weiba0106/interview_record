package com.interviewrecord.auth.infrastructure;

import com.interviewrecord.auth.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    default User requireById(long id) { return findById(id).orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND")); }
}
