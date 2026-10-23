package com.sipm.common.page;

import com.sipm.common.error.BusinessException;
import com.sipm.common.error.ErrorCode;
import com.sipm.common.error.FieldErrorItem;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * page/size/sort 쿼리 파라미터 → Pageable 변환 (specs §4.2).
 * - sort는 API 필드명("field,asc|desc", 반복 가능)을 받아 허용 목록(allowed)으로 엔티티 속성에 매핑. 목록 밖이면 400 INVALID_SORT.
 * - size는 1..100, 100 초과는 100으로 잘라낸다(clamp).
 * - 주의: 컨트롤러에서 request.getParameterValues("sort")로 원본 값을 넘겨야 한다.
 *   String[]로 바인딩하면 Spring이 "createdAt,desc" 한 개를 쉼표 기준으로 두 개로 쪼개 버린다.
 *
 * (English) Resolves page/size/sort query parameters into a Pageable.
 * Sort values ("field,asc|desc", repeatable) use API field names mapped to entity attributes.
 * Raw sort values must be read via request.getParameterValues("sort") so that a single
 * "field,dir" value is not split by Spring's String[] conversion.
 */
public final class SortResolver {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private SortResolver() {
    }

    public static Pageable pageable(Integer page, Integer size, String[] rawSorts, Map<String, String> allowed,
                                    String defaultSort, boolean idDescTieBreaker) {
        int p = page == null ? 0 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        if (p < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(),
                    List.of(new FieldErrorItem("page", "Min", "page는 0 이상이어야 합니다.", p)));
        }
        if (s < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(),
                    List.of(new FieldErrorItem("size", "Min", "size는 1 이상이어야 합니다.", s)));
        }
        if (s > MAX_SIZE) {
            s = MAX_SIZE;
        }
        return PageRequest.of(p, s, resolve(rawSorts, allowed, defaultSort, idDescTieBreaker));
    }

    public static Sort resolve(String[] rawSorts, Map<String, String> allowed, String defaultSort,
                               boolean idDescTieBreaker) {
        List<String> values = new ArrayList<>();
        if (rawSorts != null) {
            for (String v : rawSorts) {
                if (v != null && !v.isBlank()) {
                    values.add(v.trim());
                }
            }
        }
        if (values.isEmpty()) {
            values.add(defaultSort);
        }
        List<Sort.Order> orders = new ArrayList<>();
        boolean hasId = false;
        for (String value : values) {
            String[] parts = value.split(",");
            if (parts.length == 0 || parts.length > 2) {
                throw invalid(value);
            }
            String field = parts[0].trim();
            Sort.Direction dir = Sort.Direction.ASC;
            if (parts.length == 2) {
                String d = parts[1].trim().toLowerCase();
                if (d.equals("asc")) {
                    dir = Sort.Direction.ASC;
                } else if (d.equals("desc")) {
                    dir = Sort.Direction.DESC;
                } else {
                    throw invalid(value);
                }
            }
            String property = allowed.get(field);
            if (property == null) {
                throw invalid(value);
            }
            if (property.equals("id")) {
                hasId = true;
            }
            orders.add(new Sort.Order(dir, property));
        }
        if (idDescTieBreaker && !hasId) {
            orders.add(Sort.Order.desc("id"));
        }
        return Sort.by(orders);
    }

    private static BusinessException invalid(String value) {
        return new BusinessException(ErrorCode.INVALID_SORT, "허용되지 않은 정렬 조건입니다: " + value,
                List.of(new FieldErrorItem("sort", "INVALID_SORT", "허용되지 않은 정렬 조건입니다.", value)));
    }
}
