package com.supplychain.allocation.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.supplychain.allocation.EvaluationDecision;
import com.supplychain.allocation.engine.WarehouseScoringEngine.Candidate;
import com.supplychain.allocation.engine.WarehouseScoringEngine.Decision;
import com.supplychain.allocation.engine.WarehouseScoringEngine.Evaluation;
import com.supplychain.allocation.engine.WarehouseScoringEngine.Line;
import com.supplychain.allocation.engine.WarehouseScoringEngine.Parameters;
import com.supplychain.allocation.engine.WarehouseScoringEngine.Request;
import com.supplychain.allocation.engine.WarehouseScoringEngine.Weights;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

class WarehouseScoringEngineTest {

    private static final long PHONE = 1;
    private static final long HEADPHONES = 2;
    private static final Weights DEFAULT_WEIGHTS = weights("0.40", "0.30", "0.20", "0.10");
    private static final Offset<Double> PRECISE = Offset.offset(0.0001);

    private static final double[] DAVANAGERE = {14.4644, 75.9218};
    private static final double[] TUMAKURU = {13.3409, 77.1010};

    private static final List<Line> DEMO_ORDER =
            List.of(new Line(PHONE, "Smartphone", 10), new Line(HEADPHONES, "Wireless Headphones", 5));

    private static final Candidate HUBLI = candidate(1, "Hubli", 15.3647, 75.1240, "120", "14", 12, 15, 10);
    private static final Candidate BENGALURU = candidate(2, "Bengaluru", 12.9716, 77.5946, "180", "16", 6, 50, 30);
    private static final Candidate MYSURU = candidate(3, "Mysuru", 12.2958, 76.6394, "100", "13", 18, 5, 20);

    private static Weights weights(String distance, String cost, String inventory, String priority) {
        return new Weights(
                new BigDecimal(distance), new BigDecimal(cost), new BigDecimal(inventory), new BigDecimal(priority));
    }

    private static Candidate candidate(
            long id, String name, double lat, double lon, String base, String perKm, int handling, int phones, int headphones) {
        return new Candidate(
                id, name, true, lat, lon, new BigDecimal(base), new BigDecimal(perKm), handling,
                Map.of(PHONE, phones, HEADPHONES, headphones));
    }

    private static Candidate inactive(Candidate c) {
        return new Candidate(
                c.warehouseId(), c.name(), false, c.latitude(), c.longitude(), c.baseCost(), c.costPerKm(),
                c.handlingHours(), c.available());
    }

    private static Decision decide(double[] destination, boolean express, List<Line> lines, Candidate... candidates) {
        return decide(DEFAULT_WEIGHTS, destination, express, lines, candidates);
    }

    private static Decision decide(
            Weights weights, double[] destination, boolean express, List<Line> lines, Candidate... candidates) {
        return new WarehouseScoringEngine(new Parameters(weights, 1.25, 45))
                .evaluate(new Request(destination[0], destination[1], express, lines), List.of(candidates));
    }

    private static Evaluation find(Decision decision, String name) {
        return decision.evaluations().stream()
                .filter(e -> e.warehouseName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static double value(BigDecimal number) {
        return number.doubleValue();
    }

    // The worked example in docs/ARCHITECTURE.md, section 5.3.

    @Test
    void workedExampleSelectsHubliForDavanagere() {
        Decision decision = decide(DAVANAGERE, false, DEMO_ORDER, HUBLI, BENGALURU, MYSURU);

        assertThat(decision.selectedWarehouseId()).contains(1L);
        Evaluation hubli = find(decision, "Hubli");
        assertThat(value(hubli.distanceKm())).isCloseTo(164.7, Offset.offset(0.05));
        assertThat(value(hubli.deliveryCost())).isCloseTo(2426.41, Offset.offset(0.5));
        assertThat(value(hubli.normDistance())).isCloseTo(0.5371, PRECISE);
        assertThat(value(hubli.normCost())).isCloseTo(0.4770, PRECISE);
        assertThat(value(hubli.inventoryFactor())).isCloseTo(0.5833, PRECISE);
        assertThat(value(hubli.finalScore())).isCloseTo(0.4746, PRECISE);
        assertThat(hubli.decision()).isEqualTo(EvaluationDecision.SELECTED);
        assertThat(hubli.rank()).isEqualTo(1);

        Evaluation bengaluru = find(decision, "Bengaluru");
        assertThat(value(bengaluru.distanceKm())).isCloseTo(306.7, Offset.offset(0.05));
        assertThat(value(bengaluru.normDistance())).isEqualTo(1.0);
        assertThat(value(bengaluru.normCost())).isEqualTo(1.0);
        assertThat(value(bengaluru.inventoryFactor())).isCloseTo(0.1833, PRECISE);
        assertThat(value(bengaluru.finalScore())).isCloseTo(0.7367, PRECISE);
        assertThat(bengaluru.decision()).isEqualTo(EvaluationDecision.NOT_SELECTED);
        assertThat(bengaluru.rank()).isEqualTo(2);
    }

    @Test
    void theCustomersLocationChangesTheWinner() {
        Decision decision = decide(TUMAKURU, false, DEMO_ORDER, HUBLI, BENGALURU, MYSURU);

        assertThat(decision.selectedWarehouseId()).contains(2L);
        assertThat(value(find(decision, "Bengaluru").finalScore())).isCloseTo(0.2064, PRECISE);
        assertThat(value(find(decision, "Hubli").finalScore())).isCloseTo(0.8167, PRECISE);
    }

    // Eligibility

    @Test
    void aWarehouseShortOfAnyLineIsRejectedWithASpecificReason() {
        Evaluation mysuru = find(decide(DAVANAGERE, false, DEMO_ORDER, HUBLI, MYSURU), "Mysuru");

        assertThat(mysuru.eligible()).isFalse();
        assertThat(mysuru.decision()).isEqualTo(EvaluationDecision.REJECTED);
        assertThat(mysuru.rejectionReason()).isEqualTo("Insufficient stock of Smartphone (needs 10, has 5)");
        assertThat(mysuru.finalScore()).isNull();
        assertThat(mysuru.rank()).isNull();
    }

    @Test
    void rejectedWarehousesStillRecordDistanceAndCost() {
        Evaluation mysuru = find(decide(DAVANAGERE, false, DEMO_ORDER, HUBLI, MYSURU), "Mysuru");
        assertThat(mysuru.distanceKm()).isPositive();
        assertThat(mysuru.deliveryCost()).isPositive();
    }

    @Test
    void everyShortLineIsReported() {
        Candidate low = candidate(1, "Low", 15, 75, "1", "1", 1, 1, 2);
        Evaluation evaluation = find(decide(DAVANAGERE, false, DEMO_ORDER, low), "Low");
        assertThat(evaluation.rejectionReason())
                .contains("Smartphone (needs 10, has 1)", "Wireless Headphones (needs 5, has 2)");
    }

    @Test
    void aProductWithNoStockRecordCountsAsZero() {
        Candidate none = new Candidate(1, "None", true, 15, 75, BigDecimal.ONE, BigDecimal.ONE, 1, Map.of());
        assertThat(find(decide(DAVANAGERE, false, DEMO_ORDER, none), "None").rejectionReason())
                .contains("Smartphone (needs 10, has 0)");
    }

    @Test
    void stockExactlyEqualToTheRequestIsEnough() {
        Candidate exact = candidate(1, "Exact", 15, 75, "1", "1", 1, 10, 5);
        assertThat(decide(DAVANAGERE, false, DEMO_ORDER, exact).selectedWarehouseId()).contains(1L);
    }

    @Test
    void anInactiveWarehouseIsNeverChosenHoweverGoodItIs() {
        Decision decision = decide(DAVANAGERE, false, DEMO_ORDER, inactive(HUBLI), BENGALURU);

        assertThat(decision.selectedWarehouseId()).contains(2L);
        assertThat(find(decision, "Hubli").rejectionReason()).isEqualTo("The warehouse is inactive");
    }

    @Test
    void noWarehouseIsChosenWhenNoneCanSupplyTheWholeOrder() {
        // Twelve units exist in total, but no single warehouse has six: orders are not split.
        List<Line> monitors = List.of(new Line(PHONE, "Monitor", 6));
        Candidate a = candidate(1, "A", 15, 75, "1", "1", 1, 4, 0);
        Candidate b = candidate(2, "B", 13, 77, "1", "1", 1, 4, 0);
        Candidate c = candidate(3, "C", 12, 76, "1", "1", 1, 4, 0);

        Decision decision = decide(DAVANAGERE, false, monitors, a, b, c);

        assertThat(decision.selectedWarehouseId()).isEmpty();
        assertThat(decision.evaluations()).allMatch(e -> e.decision() == EvaluationDecision.REJECTED);
        assertThat(decision.explanation()).startsWith("No warehouse can fulfil the whole order");
    }

    @Test
    void noWarehousesAtAllIsHandled() {
        Decision decision = decide(DAVANAGERE, false, DEMO_ORDER);
        assertThat(decision.selectedWarehouseId()).isEmpty();
        assertThat(decision.evaluations()).isEmpty();
    }

    // Normalization

    @Test
    void aSingleEligibleWarehouseIsScoredAndSelected() {
        Decision decision = decide(DAVANAGERE, false, DEMO_ORDER, HUBLI, MYSURU);

        Evaluation hubli = find(decision, "Hubli");
        assertThat(hubli.decision()).isEqualTo(EvaluationDecision.SELECTED);
        assertThat(value(hubli.normDistance())).isEqualTo(1.0);
        assertThat(value(hubli.normCost())).isEqualTo(1.0);
        assertThat(decision.explanation()).contains("only warehouse with sufficient inventory");
    }

    @Test
    void aWarehouseInTheCustomersCityDoesNotDivideByZero() {
        Candidate here = new Candidate(
                1, "Here", true, DAVANAGERE[0], DAVANAGERE[1], BigDecimal.ZERO, BigDecimal.ZERO, 0,
                Map.of(PHONE, 10, HEADPHONES, 5));

        Evaluation evaluation = find(decide(DAVANAGERE, true, DEMO_ORDER, here), "Here");

        assertThat(evaluation.normDistance()).isZero();
        assertThat(evaluation.normCost()).isZero();
        assertThat(evaluation.priorityFactor()).isZero();
    }

    @Test
    void proportionsArePreservedRatherThanStretched() {
        // With min-max scaling, these near-identical distances would read as 0 and 1.
        Candidate near = candidate(1, "Near", 14.5644, 75.9218, "100", "10", 1, 50, 50);
        Candidate alsoNear = candidate(2, "AlsoNear", 14.3644, 75.9218, "100", "10", 1, 50, 50);
        Decision decision = decide(DAVANAGERE, false, DEMO_ORDER, near, alsoNear);

        assertThat(value(find(decision, "Near").normDistance())).isGreaterThan(0.99);
        assertThat(value(find(decision, "AlsoNear").normDistance())).isGreaterThan(0.99);
    }

    @Test
    void theInventoryFactorMeasuresHowMuchStockTheOrderUses() {
        Decision decision = decide(DAVANAGERE, false, DEMO_ORDER, HUBLI, BENGALURU);
        // Hubli: (10/15 + 5/10) / 2. Bengaluru: (10/50 + 5/30) / 2.
        assertThat(value(find(decision, "Hubli").inventoryFactor())).isCloseTo(0.583333, Offset.offset(0.000001));
        assertThat(value(find(decision, "Bengaluru").inventoryFactor())).isCloseTo(0.183333, Offset.offset(0.000001));
    }

    // Weights

    @Test
    void weightsDriveTheOutcome() {
        Weights distanceOnly = weights("1", "0", "0", "0");
        Weights inventoryOnly = weights("0", "0", "1", "0");

        assertThat(decide(distanceOnly, DAVANAGERE, false, DEMO_ORDER, HUBLI, BENGALURU).selectedWarehouseId())
                .contains(1L);
        // Bengaluru has far more spare stock, so it wins once only that counts.
        assertThat(decide(inventoryOnly, DAVANAGERE, false, DEMO_ORDER, HUBLI, BENGALURU).selectedWarehouseId())
                .contains(2L);
    }

    @Test
    void theScoreIsTheWeightedSumOfItsFactors() {
        Evaluation hubli = find(decide(DAVANAGERE, true, DEMO_ORDER, HUBLI, BENGALURU), "Hubli");

        double expected = 0.4 * value(hubli.normDistance())
                + 0.3 * value(hubli.normCost())
                + 0.2 * value(hubli.inventoryFactor())
                + 0.1 * value(hubli.priorityFactor());
        assertThat(value(hubli.finalScore())).isCloseTo(expected, Offset.offset(0.00001));
    }

    // Priority

    @Test
    void standardOrdersHaveNoPriorityFactor() {
        Decision decision = decide(DAVANAGERE, false, DEMO_ORDER, HUBLI, BENGALURU);
        assertThat(find(decision, "Hubli").priorityFactor()).isZero();
        assertThat(find(decision, "Bengaluru").priorityFactor()).isZero();
    }

    @Test
    void expressOrdersFavourTheFastestWarehouse() {
        // Hubli is nearer but handles orders slowly, so Bengaluru has the shorter total time.
        Decision decision = decide(DAVANAGERE, true, DEMO_ORDER, HUBLI, BENGALURU);

        assertThat(value(find(decision, "Hubli").priorityFactor())).isEqualTo(1.0);
        assertThat(value(find(decision, "Bengaluru").priorityFactor())).isCloseTo(0.8183, PRECISE);
    }

    @Test
    void expressCanChangeTheWinner() {
        Weights priorityHeavy = weights("0.10", "0.10", "0.10", "0.70");

        assertThat(decide(priorityHeavy, DAVANAGERE, false, DEMO_ORDER, HUBLI, BENGALURU).selectedWarehouseId())
                .contains(1L);
        assertThat(decide(priorityHeavy, DAVANAGERE, true, DEMO_ORDER, HUBLI, BENGALURU).selectedWarehouseId())
                .contains(2L);
    }

    // Ranking and ties

    @Test
    void warehousesAreRankedBestFirstWithRejectedLast() {
        Candidate third = candidate(4, "Third", 12.9, 77.6, "500", "30", 6, 50, 30);
        Decision decision = decide(DAVANAGERE, false, DEMO_ORDER, third, BENGALURU, MYSURU, HUBLI);

        assertThat(decision.evaluations())
                .extracting(Evaluation::warehouseName)
                .containsExactly("Hubli", "Bengaluru", "Third", "Mysuru");
        assertThat(decision.evaluations()).extracting(Evaluation::rank).containsExactly(1, 2, 3, null);
    }

    @Test
    void anEqualScoreIsWonByTheShorterDistance() {
        // Distance-only weights would differ, so use inventory-only weights to force a tied score.
        Weights inventoryOnly = weights("0", "0", "1", "0");
        Candidate far = candidate(1, "Far", 15.3, 75.1, "100", "10", 1, 50, 50);
        Candidate near = candidate(2, "Near", 14.5, 75.9, "100", "10", 1, 50, 50);

        Decision decision = decide(inventoryOnly, DAVANAGERE, false, DEMO_ORDER, far, near);

        assertThat(find(decision, "Far").finalScore()).isEqualByComparingTo(find(decision, "Near").finalScore());
        assertThat(decision.selectedWarehouseId()).contains(2L);
        assertThat(decision.explanation()).contains("tied for the lowest weighted score", "winning on shorter distance");
    }

    @Test
    void equalScoreAndDistanceIsWonByTheLowerCost() {
        Weights inventoryOnly = weights("0", "0", "1", "0");
        Candidate pricey = candidate(1, "Pricey", 15, 75, "500", "10", 1, 50, 50);
        Candidate cheap = candidate(2, "Cheap", 15, 75, "100", "10", 1, 50, 50);

        Decision decision = decide(inventoryOnly, DAVANAGERE, false, DEMO_ORDER, pricey, cheap);

        assertThat(decision.selectedWarehouseId()).contains(2L);
        assertThat(decision.explanation()).contains("winning on lower delivery cost");
    }

    @Test
    void aCompleteTieIsWonByTheLowestWarehouseId() {
        Candidate second = candidate(7, "Seven", 15, 75, "100", "10", 1, 50, 50);
        Candidate first = candidate(3, "Three", 15, 75, "100", "10", 1, 50, 50);

        Decision decision = decide(DAVANAGERE, false, DEMO_ORDER, second, first);

        assertThat(decision.selectedWarehouseId()).contains(3L);
        assertThat(decision.explanation()).contains("winning on the lowest warehouse ID");
    }

    @Test
    void theResultDoesNotDependOnTheOrderOfTheCandidates() {
        Decision forward = decide(DAVANAGERE, true, DEMO_ORDER, HUBLI, BENGALURU, MYSURU);
        Decision backward = decide(DAVANAGERE, true, DEMO_ORDER, MYSURU, BENGALURU, HUBLI);

        assertThat(backward.selectedWarehouseId()).isEqualTo(forward.selectedWarehouseId());
        assertThat(backward.evaluations()).isEqualTo(forward.evaluations());
        assertThat(backward.explanation()).isEqualTo(forward.explanation());
    }

    @Test
    void repeatingADecisionGivesTheSameResult() {
        assertThat(decide(DAVANAGERE, true, DEMO_ORDER, HUBLI, BENGALURU, MYSURU))
                .isEqualTo(decide(DAVANAGERE, true, DEMO_ORDER, HUBLI, BENGALURU, MYSURU));
    }

    // Explanations must only claim what is true.

    @Test
    void theExplanationStatesTheRealReasons() {
        assertThat(decide(DAVANAGERE, false, DEMO_ORDER, HUBLI, BENGALURU, MYSURU).explanation())
                .isEqualTo("Hubli was selected: it had sufficient inventory and the lowest weighted score "
                        + "(0.4746, next best 0.7367), with the shortest distance (164.7 km) and lowest delivery "
                        + "cost (₹2,426.41) among eligible warehouses. Mysuru was rejected: insufficient stock of "
                        + "Smartphone (needs 10, has 5).");
    }

    @Test
    void theExplanationDoesNotClaimAnAdvantageTheWinnerLacks() {
        // Inventory decides this one: the winner is neither nearest nor cheapest.
        Weights inventoryOnly = weights("0", "0", "1", "0");

        String explanation = decide(inventoryOnly, DAVANAGERE, false, DEMO_ORDER, HUBLI, BENGALURU).explanation();

        assertThat(explanation).startsWith("Bengaluru was selected");
        assertThat(explanation).doesNotContain("shortest distance", "lowest delivery cost");
    }

    @Test
    void theExplanationMentionsOnlyTheAdvantageThatApplies() {
        Candidate nearButPricey = candidate(1, "NearPricey", 14.5, 75.9, "9000", "10", 1, 50, 50);
        Candidate farButCheap = candidate(2, "FarCheap", 15.3, 75.1, "1", "1", 1, 50, 50);
        Weights distanceOnly = weights("1", "0", "0", "0");

        String explanation = decide(distanceOnly, DAVANAGERE, false, DEMO_ORDER, nearButPricey, farButCheap).explanation();

        assertThat(explanation).contains("shortest distance").doesNotContain("lowest delivery cost");
    }
}
