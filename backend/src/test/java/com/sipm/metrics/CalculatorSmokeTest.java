package com.sipm.metrics;

import com.sipm.wbs.WbsRollupCalculator;
import com.sipm.wbs.WbsStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 순수 계산기 스모크 테스트 (§3.6(a), W3.1) + 시드 BCrypt 해시 검증. */
class CalculatorSmokeTest {

    @Test
    void manMonthProration() {
        DateRanges.Span a1Actual = ManMonthCalculator.effectiveSpan(Basis.ACTUAL, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31), LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 3, 15));
        BigDecimal jan = ManMonthCalculator.monthMM(BigDecimal.ONE, a1Actual, YearMonth.of(2026, 1));
        assertThat(ManMonthCalculator.round2(jan)).isEqualByComparingTo("0.55");
        BigDecimal mar = ManMonthCalculator.monthMM(BigDecimal.ONE, a1Actual, YearMonth.of(2026, 3));
        assertThat(ManMonthCalculator.round2(mar)).isEqualByComparingTo("0.48");
    }

    @Test
    void wbsRollup() {
        BigDecimal w = new BigDecimal("1.00");
        List<WbsRollupCalculator.Node> nodes = List.of(
                node(2L, null, 1, w, WbsStatus.IN_PROGRESS, 0),
                node(21L, 2L, 0, w, WbsStatus.DONE, 100),
                node(22L, 2L, 1, new BigDecimal("3.00"), WbsStatus.IN_PROGRESS, 80));
        WbsRollupCalculator.Result r = WbsRollupCalculator.calculate(nodes);
        assertThat(r.roots().get(0).progress).isEqualByComparingTo("85.0");
        assertThat(r.ordered().get(2).wbsCode).isEqualTo("1.2");
    }

    @Test
    void seedHashesMatch() {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        assertThat(enc.matches("admin1234", "$2a$10$Dak9d7683Dss68Ylb/bGHus.HBLbIV.OBWHmVqeGDIItvxBmLVnF.")).isTrue();
        assertThat(enc.matches("user1234", "$2a$10$MuKHA9p4Q7ve.uvmYuUO4Ok57q7WfywUh.n.rIJ44/OiBHj7gb97a")).isTrue();
    }

    private static WbsRollupCalculator.Node node(Long id, Long parent, int order, BigDecimal w, WbsStatus s, int p) {
        return new WbsRollupCalculator.Node(id, parent, order, w, s, null, null, null, null, p);
    }
}
