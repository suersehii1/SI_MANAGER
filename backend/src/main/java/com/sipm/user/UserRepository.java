package com.sipm.user;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    static Specification<User> keyword(String pattern, boolean includeEmail) {
        return (root, q, cb) -> {
            if (pattern == null) {
                return null;
            }
            var username = cb.like(cb.lower(root.get("username")), pattern, '\\');
            var name = cb.like(cb.lower(root.get("name")), pattern, '\\');
            if (includeEmail) {
                return cb.or(username, name, cb.like(cb.lower(root.get("email")), pattern, '\\'));
            }
            return cb.or(username, name);
        };
    }

    static Specification<User> enabled(Boolean enabled) {
        return (root, q, cb) -> enabled == null ? null : cb.equal(root.get("enabled"), enabled);
    }
}
