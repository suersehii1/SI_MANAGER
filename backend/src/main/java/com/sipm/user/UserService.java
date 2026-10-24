package com.sipm.user;

import com.sipm.common.error.BusinessException;
import com.sipm.common.error.ErrorCode;
import com.sipm.common.error.FieldErrorItem;
import com.sipm.common.page.PageResponse;
import com.sipm.common.util.LikePatterns;
import com.sipm.security.AppUserPrincipal;
import com.sipm.user.dto.UserCreateRequest;
import com.sipm.user.dto.UserResponse;
import com.sipm.user.dto.UserSummary;
import com.sipm.user.dto.UserUpdateRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {
    /** 선택 가능한 직급 목록 (프론트 utils/constants.js JOB_TITLES와 동일하게 유지) */
    public static final List<String> JOB_TITLES = List.of("사원", "선임", "책임", "수석", "임원", "대표", "부대표", "기타");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long id) {
        return UserResponse.from(get(id));
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> BusinessException.notFound("사용자"));
    }

    /** Fetch a user for assignment/assignee use: must exist (404) and be enabled (400 USER_DISABLED). */
    @Transactional(readOnly = true)
    public User getEnabled(Long id) {
        User u = get(id);
        if (!u.isEnabled()) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "비활성화된 사용자는 지정할 수 없습니다: " + u.getName());
        }
        return u;
    }

    @Transactional(readOnly = true)
    public List<UserSummary> lookup(String keyword) {
        Specification<User> spec = Specification.where(UserRepository.enabled(true))
                .and(UserRepository.keyword(LikePatterns.contains(keyword), false));
        return userRepository.findAll(spec, Sort.by("name").ascending().and(Sort.by("id")))
                .stream().map(UserSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String keyword, Boolean enabled, Pageable pageable) {
        Specification<User> spec = Specification.where(UserRepository.enabled(enabled))
                .and(UserRepository.keyword(LikePatterns.contains(keyword), true));
        return PageResponse.of(userRepository.findAll(spec, pageable), UserResponse::from);
    }

    @Transactional
    public UserResponse create(UserCreateRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
        User u = new User(req.username(), passwordEncoder.encode(req.password()), req.name().trim());
        u.setEmail(blankToNull(req.email()));
        u.setJobTitle(checkJobTitle(blankToNull(req.jobTitle()), null));
        u.setEnabled(req.enabled() == null || req.enabled());
        u.setRoles(resolveRoles(req.roles()));
        return UserResponse.from(userRepository.saveAndFlush(u));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest req, AppUserPrincipal me) {
        User u = get(id);
        boolean enabled = req.enabled() == null ? u.isEnabled() : req.enabled();
        // 관리자가 자기 자신을 비활성화하거나 ROLE_ADMIN을 빼면 관리자가 사라질 수 있으므로 막는다
        if (u.getId().equals(me.getId())) {
            if (!enabled || !req.roles().contains(Role.ADMIN) && u.hasRole(Role.ADMIN)) {
                throw new BusinessException(ErrorCode.SELF_MODIFICATION_NOT_ALLOWED);
            }
        }
        // 비밀번호는 null/빈 문자열이면 변경하지 않는다 (그래서 길이 검사를 DTO가 아닌 여기서 한다)
        if (req.password() != null && !req.password().isEmpty()) {
            if (req.password().length() < 8 || req.password().length() > 64) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(),
                        List.of(new FieldErrorItem("password", "Size", "비밀번호는 8~64자여야 합니다.", null)));
            }
            u.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        u.setName(req.name().trim());
        u.setEmail(blankToNull(req.email()));
        u.setJobTitle(checkJobTitle(blankToNull(req.jobTitle()), u.getJobTitle()));
        u.setEnabled(enabled);
        u.setRoles(resolveRoles(req.roles()));
        return UserResponse.from(userRepository.saveAndFlush(u));
    }

    /**
     * 직급은 정해진 목록 중 하나만 허용한다 (비워두는 것은 허용).
     * 단, 목록이 생기기 전에 등록된 기존 값(예: 과장, 대리)은 수정 시 바꾸지 않으면 그대로 둔다.
     */
    private static String checkJobTitle(String jobTitle, String currentJobTitle) {
        if (jobTitle == null || JOB_TITLES.contains(jobTitle) || jobTitle.equals(currentJobTitle)) {
            return jobTitle;
        }
        throw BusinessException.validation("jobTitle", "INVALID_JOB_TITLE",
                "직급은 " + String.join("/", JOB_TITLES) + " 중 하나여야 합니다.", jobTitle);
    }

    private Set<Role> resolveRoles(List<String> names) {
        Set<Role> roles = new HashSet<>();
        for (String n : new HashSet<>(names)) {
            roles.add(roleRepository.findByName(n).orElseThrow(() -> BusinessException.validation(
                    "roles", "INVALID_ROLE", "허용되지 않은 역할입니다.", n)));
        }
        return roles;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
