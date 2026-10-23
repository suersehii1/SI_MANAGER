package com.sipm.wbs;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** specs-wbs §W3 집계 규칙과 W3.1 Worked Example(V4 시드, asOf=2026-03-15) 검증. */
class WbsRollupCalculatorTest {
    private static final LocalDate AS_OF = LocalDate.of(2026, 3, 15);

    /** W3.1 시드 트리. 입력 순서를 일부러 섞어 sortOrder로 정렬되는지도 확인한다. */
    private static List<WbsRollupCalculator.Node> seed(String weightOf22) {
        List<WbsRollupCalculator.Node> n = new ArrayList<>();
        n.add(node(4L, null, 3, "1.00", WbsStatus.NOT_STARTED, "03-26", "03-31", null, null, 0));
        n.add(node(32L, 3L, 1, "1.00", WbsStatus.NOT_STARTED, "03-10", "03-25", null, null, 0));
        n.add(node(1L, null, 0, "1.00", WbsStatus.NOT_STARTED, null, null, null, null, 0));
        n.add(node(11L, 1L, 0, "1.00", WbsStatus.DONE, "01-01", "01-15", "01-15", "01-20", 100));
        n.add(node(12L, 1L, 1, "1.00", WbsStatus.DONE, "01-16", "01-31", "01-21", "01-31", 100));
        n.add(node(2L, null, 1, "1.00", WbsStatus.NOT_STARTED, null, null, null, null, 0));
        n.add(node(21L, 2L, 0, "1.00", WbsStatus.DONE, "02-01", "02-14", "02-01", "02-16", 100));
        n.add(node(22L, 2L, 1, weightOf22, WbsStatus.IN_PROGRESS, "02-01", "02-28", "02-01", null, 80));
        n.add(node(3L, null, 2, "1.00", WbsStatus.NOT_STARTED, null, null, null, null, 0));
        n.add(node(31L, 3L, 0, "1.00", WbsStatus.IN_PROGRESS, "03-01", "03-15", "03-02", null, 50));
        return n;
    }

    @Test
    void workedExampleRollUp() {
        WbsRollupCalculator.Result r = WbsRollupCalculator.calculate(seed("1.00"));
        Map<Long, WbsRollupCalculator.Node> byId = index(r);

        // DFS 전위 순서 + 코드 재번호
        assertThat(r.ordered().stream().map(x -> x.wbsCode))
                .containsExactly("1", "1.1", "1.2", "2", "2.1", "2.2", "3", "3.1", "3.2", "4");
        assertThat(byId.get(22L).level).isEqualTo(2);
        assertThat(byId.get(4L).level).isEqualTo(1);

        // 1 분석: 01-01~01-31 / 01-15~01-31, DONE, 100.0
        WbsRollupCalculator.Node n1 = byId.get(1L);
        assertThat(n1.plannedStart).isEqualTo(d("01-01"));
        assertThat(n1.plannedEnd).isEqualTo(d("01-31"));
        assertThat(n1.actualStart).isEqualTo(d("01-15"));
        assertThat(n1.actualEnd).isEqualTo(d("01-31"));
        assertThat(n1.status).isEqualTo(WbsStatus.DONE);
        assertThat(n1.progress).isEqualByComparingTo("100.0");

        // 2 설계: 02-01~02-28 / 02-01~null (2.2 미종료), IN_PROGRESS, 90.0
        WbsRollupCalculator.Node n2 = byId.get(2L);
        assertThat(n2.plannedStart).isEqualTo(d("02-01"));
        assertThat(n2.plannedEnd).isEqualTo(d("02-28"));
        assertThat(n2.actualStart).isEqualTo(d("02-01"));
        assertThat(n2.actualEnd).isNull();
        assertThat(n2.status).isEqualTo(WbsStatus.IN_PROGRESS);
        assertThat(n2.progress).isEqualByComparingTo("90.0");

        // 3 개발: 03-01~03-25 / 03-02~null, IN_PROGRESS, 25.0
        WbsRollupCalculator.Node n3 = byId.get(3L);
        assertThat(n3.plannedStart).isEqualTo(d("03-01"));
        assertThat(n3.plannedEnd).isEqualTo(d("03-25"));
        assertThat(n3.actualStart).isEqualTo(d("03-02"));
        assertThat(n3.actualEnd).isNull();
        assertThat(n3.status).isEqualTo(WbsStatus.IN_PROGRESS);
        assertThat(n3.progress).isEqualByComparingTo("25.0");

        // 4는 리프: 저장값 그대로
        assertThat(byId.get(4L).progress).isEqualByComparingTo("0.0");
        assertThat(byId.get(4L).hasChildren()).isFalse();

        // overall = (100 + 90 + 25 + 0) / 4 = 53.75 → 53.8 (HALF_UP)
        assertThat(r.overallProgress()).isEqualByComparingTo("53.8");
        assertThat(r.overallProgress().scale()).isEqualTo(1);
    }

    @Test
    void weightChangeOfLeafChangesParentAndOverall() {
        WbsRollupCalculator.Result r = WbsRollupCalculator.calculate(seed("3.00"));
        // 2 = (100×1 + 80×3) / 4 = 85.0, overall = (100 + 85 + 25 + 0) / 4 = 52.5
        assertThat(index(r).get(2L).progress).isEqualByComparingTo("85.0");
        assertThat(r.overallProgress()).isEqualByComparingTo("52.5");
    }

    @Test
    void delayedBoundary() {
        Map<Long, WbsRollupCalculator.Node> byId = index(WbsRollupCalculator.calculate(seed("1.00")));
        assertThat(WbsRollupCalculator.isDelayed(byId.get(22L), AS_OF)).isTrue();  // 02-28, 80%
        assertThat(WbsRollupCalculator.isDelayed(byId.get(2L), AS_OF)).isTrue();   // 집계 02-28, 90%
        assertThat(WbsRollupCalculator.isDelayed(byId.get(31L), AS_OF)).isFalse(); // 03-15 당일은 지연 아님
        assertThat(WbsRollupCalculator.isDelayed(byId.get(1L), AS_OF)).isFalse();  // 100% 완료
        assertThat(WbsRollupCalculator.isDelayed(byId.get(4L), AS_OF)).isFalse();  // 미래
        assertThat(WbsRollupCalculator.isDelayed(byId.get(31L), AS_OF.plusDays(1))).isTrue();
    }

    @Test
    void plannedDaysIsInclusiveAndNullWhenMissing() {
        Map<Long, WbsRollupCalculator.Node> byId = index(WbsRollupCalculator.calculate(seed("1.00")));
        assertThat(WbsRollupCalculator.plannedDays(byId.get(22L))).isEqualTo(28L);
        assertThat(WbsRollupCalculator.plannedDays(byId.get(1L))).isEqualTo(31L);
        WbsRollupCalculator.Node noDates = node(9L, null, 0, "1.00", WbsStatus.NOT_STARTED, "01-01", null, null, null, 0);
        assertThat(WbsRollupCalculator.plannedDays(noDates)).isNull();
    }

    @Test
    void statusAggregation() {
        assertThat(parentStatus(WbsStatus.DONE, WbsStatus.DONE)).isEqualTo(WbsStatus.DONE);
        assertThat(parentStatus(WbsStatus.NOT_STARTED, WbsStatus.NOT_STARTED)).isEqualTo(WbsStatus.NOT_STARTED);
        assertThat(parentStatus(WbsStatus.ON_HOLD, WbsStatus.ON_HOLD)).isEqualTo(WbsStatus.ON_HOLD);
        assertThat(parentStatus(WbsStatus.DONE, WbsStatus.NOT_STARTED)).isEqualTo(WbsStatus.IN_PROGRESS);
        assertThat(parentStatus(WbsStatus.ON_HOLD, WbsStatus.DONE)).isEqualTo(WbsStatus.IN_PROGRESS);
    }

    @Test
    void actualEndOnlyWhenAllChildrenEnded() {
        List<WbsRollupCalculator.Node> nodes = List.of(
                node(1L, null, 0, "1.00", WbsStatus.NOT_STARTED, null, null, null, null, 0),
                node(2L, 1L, 0, "1.00", WbsStatus.DONE, null, null, "01-01", "01-10", 100),
                node(3L, 1L, 1, "1.00", WbsStatus.DONE, null, null, "01-05", "01-20", 100));
        WbsRollupCalculator.Node parent = index(WbsRollupCalculator.calculate(nodes)).get(1L);
        assertThat(parent.actualStart).isEqualTo(d("01-01"));
        assertThat(parent.actualEnd).isEqualTo(d("01-20"));
        // 자식에 기간이 하나도 없으면 집계 기간도 null
        assertThat(parent.plannedStart).isNull();
        assertThat(parent.plannedEnd).isNull();
    }

    @Test
    void multiLevelRollUpUsesRoundedChildValues() {
        // 3단계: 손자 (0, 0, 100) → 자식 33.3, 형제 리프 0 → 루트 (33.3 + 0) / 2 = 16.65 → 16.7
        List<WbsRollupCalculator.Node> nodes = List.of(
                node(1L, null, 0, "1.00", WbsStatus.NOT_STARTED, null, null, null, null, 0),
                node(2L, 1L, 0, "1.00", WbsStatus.NOT_STARTED, null, null, null, null, 0),
                node(3L, 1L, 1, "1.00", WbsStatus.NOT_STARTED, null, null, null, null, 0),
                node(21L, 2L, 0, "1.00", WbsStatus.NOT_STARTED, null, null, null, null, 0),
                node(22L, 2L, 1, "1.00", WbsStatus.NOT_STARTED, null, null, null, null, 0),
                node(23L, 2L, 2, "1.00", WbsStatus.DONE, null, null, null, null, 100));
        WbsRollupCalculator.Result r = WbsRollupCalculator.calculate(nodes);
        Map<Long, WbsRollupCalculator.Node> byId = index(r);
        assertThat(byId.get(2L).progress).isEqualByComparingTo("33.3");
        assertThat(byId.get(1L).progress).isEqualByComparingTo("16.7");
        assertThat(byId.get(23L).wbsCode).isEqualTo("1.1.3");
        assertThat(byId.get(23L).level).isEqualTo(3);
        assertThat(WbsRollupCalculator.subtreeHeight(byId.get(1L))).isEqualTo(3);
        assertThat(WbsRollupCalculator.subtreeHeight(byId.get(3L))).isEqualTo(1);
    }

    @Test
    void emptyTreeHasZeroOverallProgress() {
        WbsRollupCalculator.Result r = WbsRollupCalculator.calculate(List.of());
        assertThat(r.overallProgress()).isEqualByComparingTo("0.0");
        assertThat(r.ordered()).isEmpty();
    }

    // ===================== 헬퍼 =====================

    private static WbsStatus parentStatus(WbsStatus a, WbsStatus b) {
        List<WbsRollupCalculator.Node> nodes = List.of(
                node(1L, null, 0, "1.00", WbsStatus.NOT_STARTED, null, null, null, null, 0),
                node(2L, 1L, 0, "1.00", a, null, null, null, null, 0),
                node(3L, 1L, 1, "1.00", b, null, null, null, null, 0));
        return index(WbsRollupCalculator.calculate(nodes)).get(1L).status;
    }

    private static Map<Long, WbsRollupCalculator.Node> index(WbsRollupCalculator.Result r) {
        return r.ordered().stream().collect(Collectors.toMap(x -> x.id, Function.identity()));
    }

    private static WbsRollupCalculator.Node node(Long id, Long parent, int order, String weight, WbsStatus status,
                                                 String ps, String pe, String as, String ae, int progress) {
        return new WbsRollupCalculator.Node(id, parent, order, new BigDecimal(weight), status,
                d(ps), d(pe), d(as), d(ae), progress);
    }

    /** "MM-dd" → 2026-MM-dd (null 허용) */
    private static LocalDate d(String monthDay) {
        return monthDay == null ? null : LocalDate.parse("2026-" + monthDay);
    }
}
