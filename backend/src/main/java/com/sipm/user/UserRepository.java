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

    /**
     * 사용자 관리 화면 검색: 이름·영어 이름·이메일 (+ ADMIN이면 아이디도).
     * ADMIN이 아니면 아이디가 화면에 보이지 않으므로 아이디로 찾을 수도 없게 한다.
     */
    static Specification<User> manageKeyword(String pattern, boolean includeUsername) {
        return (root, q, cb) -> {
            if (pattern == null) {
                return null;
            }
            var name = cb.like(cb.lower(root.get("name")), pattern, '\\');
            var englishName = cb.like(cb.lower(root.get("englishName")), pattern, '\\');
            var email = cb.like(cb.lower(root.get("email")), pattern, '\\');
            if (includeUsername) {
                return cb.or(cb.like(cb.lower(root.get("username")), pattern, '\\'), name, englishName, email);
            }
            return cb.or(name, englishName, email);
        };
    }

    static Specification<User> enabled(Boolean enabled) {
        return (root, q, cb) -> enabled == null ? null : cb.equal(root.get("enabled"), enabled);
    }
}
