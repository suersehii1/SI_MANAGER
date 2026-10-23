package com.sipm.wbs;

import com.sipm.metrics.DateRanges;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WBS 트리 조립 + 집계(roll-up) 계산 (specs-wbs §W3). Spring 의존성 없는 순수 클래스.
 *
 * 사용법: Node 목록(평탄)을 {@link #calculate(List)}에 넘기면
 *  1) parentId로 트리를 조립하고 (형제는 sortOrder, id 순)
 *  2) 후위 순회로 부모 노드의 기간/진척률/상태를 자식들로부터 계산하고
 *  3) wbsCode("1.2.3")와 level(루트=1)을 부여한 뒤
 *  4) DFS 전위 순서 목록을 돌려준다.
 */
public final class WbsRollupCalculator {
    public static final int MAX_DEPTH = 5;

    private WbsRollupCalculator() {
    }

    /** 계산 단위 노드. 입력값(저장값)과 계산 결과를 함께 담는다. */
    public static final class Node {
        // ----- 입력 (DB 저장값) -----
        public final Long id;
        public final Long parentId;
        public final int sortOrder;
        public final BigDecimal weight;
        public WbsStatus status;
        public LocalDate plannedStart;
        public LocalDate plannedEnd;
        public LocalDate actualStart;
        public LocalDate actualEnd;
        /** 리프는 저장된 정수 진척률, 부모는 집계값(소수 1자리). */
        public BigDecimal progress;

        // ----- 계산 결과 -----
        public final List<Node> children = new ArrayList<>();
        public String wbsCode;
        public int level;

        public Node(Long id, Long parentId, int sortOrder, BigDecimal weight, WbsStatus status,
                    LocalDate plannedStart, LocalDate plannedEnd, LocalDate actualStart, LocalDate actualEnd,
                    int progress) {
            this.id = id;
            this.parentId = parentId;
            this.sortOrder = sortOrder;
            this.weight = weight;
            this.status = status;
            this.plannedStart = plannedStart;
            this.plannedEnd = plannedEnd;
            this.actualStart = actualStart;
            this.actualEnd = actualEnd;
            this.progress = BigDecimal.valueOf(progress).setScale(1, RoundingMode.HALF_UP);
        }

        public boolean hasChildren() {
            return !children.isEmpty();
        }
    }

    /** 계산 결과: 루트 목록, DFS 순서 목록, 전체 진척률. */
    public record Result(List<Node> roots, List<Node> ordered, BigDecimal overallProgress) {
    }

    public static Result calculate(List<Node> nodes) {
        // 1) 트리 조립. 부모를 찾을 수 없는 노드는 루트로 취급한다.
        Map<Long, Node> byId = new HashMap<>();
        for (Node n : nodes) {
            byId.put(n.id, n);
        }
        List<Node> roots = new ArrayList<>();
        for (Node n : nodes) {
            Node parent = n.parentId == null ? null : byId.get(n.parentId);
            if (parent == null) {
                roots.add(n);
            } else {
                parent.children.add(n);
            }
        }
        Comparator<Node> order = Comparator.comparingInt((Node n) -> n.sortOrder).thenComparing(n -> n.id);
        roots.sort(order);
        for (Node n : nodes) {
            n.children.sort(order);
        }

        // 2) 후위 순회 집계 + 3) 코드/레벨 부여 + 4) DFS 순서 수집
        List<Node> ordered = new ArrayList<>();
        for (int i = 0; i < roots.size(); i++) {
            visit(roots.get(i), String.valueOf(i + 1), 1, ordered);
        }
        return new Result(roots, ordered, weightedProgress(roots));
    }

    /** 전위 순서로 목록에 추가하면서, 자식 처리 후(후위) 자기 집계값을 계산한다. */
    private static void visit(Node node, String code, int level, List<Node> ordered) {
        node.wbsCode = code;
        node.level = level;
        ordered.add(node);
        for (int i = 0; i < node.children.size(); i++) {
            visit(node.children.get(i), code + "." + (i + 1), level + 1, ordered);
        }
        if (node.hasChildren()) {
            rollUp(node);
        }
    }

    /** 부모 노드의 기간/진척률/상태를 자식 값으로부터 계산 (자식은 이미 집계 완료 상태). */
    static void rollUp(Node parent) {
        LocalDate ps = null;
        LocalDate pe = null;
        LocalDate as = null;
        LocalDate ae = null;
        boolean allActualEnded = true;
        boolean allDone = true;
        boolean allNotStarted = true;
        boolean allOnHold = true;
        for (Node c : parent.children) {
            ps = earlier(ps, c.plannedStart);   // 계획 시작: non-null 최솟값
            pe = later(pe, c.plannedEnd);       // 계획 종료: non-null 최댓값
            as = earlier(as, c.actualStart);    // 실제 시작: non-null 최솟값
            if (c.actualEnd == null) {
                allActualEnded = false;         // 실제 종료: 모든 자식이 종료돼야 값이 있음
            } else {
                ae = later(ae, c.actualEnd);
            }
            allDone &= c.status == WbsStatus.DONE;
            allNotStarted &= c.status == WbsStatus.NOT_STARTED;
            allOnHold &= c.status == WbsStatus.ON_HOLD;
        }
        parent.plannedStart = ps;
        parent.plannedEnd = pe;
        parent.actualStart = as;
        parent.actualEnd = allActualEnded ? ae : null;
        parent.progress = weightedProgress(parent.children);
        if (allDone) {
            parent.status = WbsStatus.DONE;
        } else if (allNotStarted) {
            parent.status = WbsStatus.NOT_STARTED;
        } else if (allOnHold) {
            parent.status = WbsStatus.ON_HOLD;
        } else {
            parent.status = WbsStatus.IN_PROGRESS;
        }
    }

    /** Σ(w × p) / Σw, 소수 1자리 HALF_UP. 노드가 없으면 0.0. */
    public static BigDecimal weightedProgress(List<Node> nodes) {
        BigDecimal sumW = BigDecimal.ZERO;
        BigDecimal sumWP = BigDecimal.ZERO;
        for (Node n : nodes) {
            sumW = sumW.add(n.weight);
            sumWP = sumWP.add(n.weight.multiply(n.progress));
        }
        if (sumW.signum() == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return sumWP.divide(sumW, 1, RoundingMode.HALF_UP);
    }

    /** 지연: plannedEnd < asOf 이고 진척률 < 100 (plannedEnd 당일은 지연 아님). */
    public static boolean isDelayed(Node n, LocalDate asOf) {
        return n.plannedEnd != null && n.plannedEnd.isBefore(asOf)
                && n.progress.compareTo(BigDecimal.valueOf(100)) < 0;
    }

    /** plannedStart..plannedEnd 양끝 포함 일수, 하나라도 null이면 null. */
    public static Long plannedDays(Node n) {
        if (n.plannedStart == null || n.plannedEnd == null) {
            return null;
        }
        return DateRanges.days(n.plannedStart, n.plannedEnd);
    }

    /** 노드를 루트로 하는 서브트리의 높이 (리프=1). 이동 시 깊이 검사에 사용. */
    public static int subtreeHeight(Node n) {
        int max = 0;
        for (Node c : n.children) {
            max = Math.max(max, subtreeHeight(c));
        }
        return max + 1;
    }

    private static LocalDate earlier(LocalDate cur, LocalDate cand) {
        if (cand == null) {
            return cur;
        }
        return cur == null || cand.isBefore(cur) ? cand : cur;
    }

    private static LocalDate later(LocalDate cur, LocalDate cand) {
        if (cand == null) {
            return cur;
        }
        return cur == null || cand.isAfter(cur) ? cand : cur;
    }
}
