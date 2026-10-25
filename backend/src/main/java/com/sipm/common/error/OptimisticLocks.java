package com.sipm.common.error;

/**
 * 낙관적 잠금 버전 비교 (덮어쓰기 방지).
 * 화면은 데이터를 불러올 때 받은 version을 수정 요청에 그대로 실어 보낸다.
 * 그 사이 다른 사람이 저장해서 DB 버전이 올라갔으면 409 CONCURRENT_MODIFICATION.
 * (version을 보내지 않은 요청은 예전 클라이언트 호환을 위해 검사하지 않는다)
 */
public final class OptimisticLocks {
    private OptimisticLocks() {
    }

    public static void check(Long currentVersion, Long requestVersion) {
        if (requestVersion != null && !requestVersion.equals(currentVersion)) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }
    }
}
