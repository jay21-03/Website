package com.bautruc.ecommerce.identity.application;

import com.bautruc.ecommerce.common.exception.GlobalExceptionHandler;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.exception.ResourceNotFoundException;
import com.bautruc.ecommerce.common.response.PageResponse;
import com.bautruc.ecommerce.common.security.AuthenticatedUser;
import com.bautruc.ecommerce.common.security.SecurityErrorCodes;
import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserRole;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserQueryService {
    private final UserJpaRepository userRepository;

    public UserQueryService(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User currentUser(AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new BusinessException(
                        SecurityErrorCodes.AUTH_TOKEN_INVALID,
                        "Authentication token is invalid.",
                        HttpStatus.UNAUTHORIZED
                ));
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new BusinessException(
                    SecurityErrorCodes.USER_BLOCKED,
                    "User is blocked.",
                    HttpStatus.UNAUTHORIZED
            );
        }
        return user;
    }

    @Transactional(readOnly = true)
    public Page<User> listUsers(
            String keyword,
            UserRole role,
            UserStatus status,
            Integer page,
            Integer size,
            String sort
    ) {
        Pageable pageable = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                normalizeSort(sort)
        );
        return userRepository.findAll(specification(keyword, role, status), pageable);
    }

    @Transactional(readOnly = true)
    public User detail(Long id) {
        return findRequired(id);
    }

    @Transactional(readOnly = true)
    public java.util.List<Long> activeAdminIds() {
        return userRepository.findByRoleAndStatusOrderById(UserRole.ADMIN, UserStatus.ACTIVE)
                .stream().map(User::getId).toList();
    }

    User findRequired(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        IdentityErrorCodes.USER_NOT_FOUND,
                        "User not found."
                ));
    }

    private Specification<User> specification(String keyword, UserRole role, UserStatus status) {
        return (root, query, criteriaBuilder) -> {
            java.util.ArrayList<Predicate> predicates = new java.util.ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern)
                ));
            }
            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return PageResponse.DEFAULT_PAGE;
        }
        if (page < 0) {
            throw validation("page must be greater than or equal to 0.");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return PageResponse.DEFAULT_SIZE;
        }
        if (size < 1 || size > PageResponse.MAX_SIZE) {
            throw validation("size must be between 1 and 100.");
        }
        return size;
    }

    private Sort normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",", -1);
        if (parts.length != 2) {
            throw validation("sort must use field,direction format.");
        }
        String field = parts[0].trim();
        String direction = parts[1].trim();
        if (!field.equals("createdAt") && !field.equals("email")) {
            throw validation("sort field is not allowed.");
        }
        Sort.Direction sortDirection;
        if ("asc".equalsIgnoreCase(direction)) {
            sortDirection = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(direction)) {
            sortDirection = Sort.Direction.DESC;
        } else {
            throw validation("sort direction is not allowed.");
        }
        return Sort.by(sortDirection, field);
    }

    private BusinessException validation(String message) {
        return new BusinessException(
                GlobalExceptionHandler.VALIDATION_FAILED,
                message,
                HttpStatus.BAD_REQUEST
        );
    }
}
