package com.supplychain.allocation.engine;

import com.supplychain.allocation.EvaluationDecision;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Chooses the warehouse for an order. Deterministic, with no Spring or database, so it can be tested alone.
 *
 * <p>Stage 1 is a hard rule: a warehouse that is inactive or can't supply every line in full is rejected.
 * Stage 2 scores the rest, and the LOWEST score wins:
 *
 * <pre>score = wD·distance + wC·cost + wI·inventory + wP·priority</pre>
 *
 * <p>Every factor runs from 0 (best) to 1 (worst):
 *
 * <ul>
 *   <li>distance: km / largest km among eligible warehouses
 *   <li>cost: delivery cost / largest cost among eligible warehouses
 *   <li>inventory: average of requested / available across lines, meaning how much stock the order uses
 *   <li>priority: 0 for Standard; for Express, delivery time / longest delivery time among eligible
 * </ul>
 *
 * <p>Ties go to the shorter distance, then the lower cost, then the lowest warehouse ID.
 */
public final class WarehouseScoringEngine {

    private static final int SCALE = 6;
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final Comparator<Scored> BEST_FIRST = Comparator.comparing((Scored s) -> s.score)
            .thenComparingDouble(s -> s.metrics.km)
            .thenComparingDouble(s -> s.metrics.cost)
            .thenComparingLong(s -> s.metrics.candidate.warehouseId());

    public record Weights(BigDecimal distance, BigDecimal cost, BigDecimal inventory, BigDecimal priority) {}

    public record Parameters(Weights weights, double roadFactor, double speedKmh) {}

    public record Line(long productId, String productName, int quantity) {}

    public record Request(double latitude, double longitude, boolean express, List<Line> lines) {}

    /** `available` maps product ID to sellable units. A product with no entry has none. */
    public record Candidate(
            long warehouseId,
            String name,
            boolean active,
            double latitude,
            double longitude,
            BigDecimal baseCost,
            BigDecimal costPerKm,
            int handlingHours,
            Map<Long, Integer> available) {}

    /** The scoring fields and `rank` are null for rejected warehouses, which are never scored. */
    public record Evaluation(
            long warehouseId,
            String warehouseName,
            boolean eligible,
            String rejectionReason,
            BigDecimal distanceKm,
            BigDecimal deliveryCost,
            BigDecimal etaHours,
            BigDecimal normDistance,
            BigDecimal normCost,
            BigDecimal inventoryFactor,
            BigDecimal priorityFactor,
            BigDecimal finalScore,
            Integer rank,
            EvaluationDecision decision) {}

    public record Decision(Optional<Long> selectedWarehouseId, List<Evaluation> evaluations, String explanation) {}

    private record Metrics(Candidate candidate, double km, double cost, double eta, String rejectionReason) {}

    private static final class Scored {
        private final Metrics metrics;
        private final BigDecimal normKm;
        private final BigDecimal normCost;
        private final BigDecimal inventory;
        private final BigDecimal priority;
        private final BigDecimal score;

        private Scored(Metrics metrics, BigDecimal normKm, BigDecimal normCost, BigDecimal inventory,
                BigDecimal priority, Weights weights) {
            this.metrics = metrics;
            this.normKm = normKm;
            this.normCost = normCost;
            this.inventory = inventory;
            this.priority = priority;
            this.score = weights.distance().multiply(normKm)
                    .add(weights.cost().multiply(normCost))
                    .add(weights.inventory().multiply(inventory))
                    .add(weights.priority().multiply(priority))
                    .setScale(SCALE, RoundingMode.HALF_UP);
        }
    }

    private final Parameters parameters;

    public WarehouseScoringEngine(Parameters parameters) {
        this.parameters = parameters;
    }

    public Decision evaluate(Request request, List<Candidate> candidates) {
        List<Metrics> all = candidates.stream().map(candidate -> measure(request, candidate)).toList();
        List<Metrics> eligible = all.stream().filter(m -> m.rejectionReason == null).toList();
        List<Metrics> rejected = all.stream()
                .filter(m -> m.rejectionReason != null)
                .sorted(Comparator.comparing(m -> m.candidate.name()))
                .toList();

        double maxKm = eligible.stream().mapToDouble(Metrics::km).max().orElse(0);
        double maxCost = eligible.stream().mapToDouble(Metrics::cost).max().orElse(0);
        double maxEta = eligible.stream().mapToDouble(Metrics::eta).max().orElse(0);
        List<Scored> ranked = eligible.stream()
                .map(m -> new Scored(
                        m,
                        normalize(m.km, maxKm),
                        normalize(m.cost, maxCost),
                        inventoryFactor(request, m.candidate),
                        request.express() ? normalize(m.eta, maxEta) : BigDecimal.ZERO.setScale(SCALE),
                        parameters.weights()))
                .sorted(BEST_FIRST)
                .toList();

        List<Evaluation> evaluations = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            evaluations.add(toEvaluation(ranked.get(i), i + 1));
        }
        rejected.forEach(m -> evaluations.add(toEvaluation(m)));

        Optional<Long> selected = ranked.stream().findFirst().map(s -> s.metrics.candidate.warehouseId());
        return new Decision(selected, List.copyOf(evaluations), explain(ranked, rejected));
    }

    private Metrics measure(Request request, Candidate candidate) {
        double km = haversineKm(request.latitude(), request.longitude(), candidate.latitude(), candidate.longitude())
                * parameters.roadFactor();
        double cost = candidate.baseCost().doubleValue() + candidate.costPerKm().doubleValue() * km;
        double eta = candidate.handlingHours() + km / parameters.speedKmh();
        return new Metrics(candidate, km, cost, eta, rejectionReason(request, candidate));
    }

    private static String rejectionReason(Request request, Candidate candidate) {
        if (!candidate.active()) {
            return "The warehouse is inactive";
        }
        List<String> shortages = request.lines().stream()
                .filter(line -> availableUnits(candidate, line) < line.quantity())
                .map(line -> "%s (needs %d, has %d)".formatted(
                        line.productName(), line.quantity(), availableUnits(candidate, line)))
                .toList();
        return shortages.isEmpty() ? null : "Insufficient stock of " + String.join(", ", shortages);
    }

    private static int availableUnits(Candidate candidate, Line line) {
        return candidate.available().getOrDefault(line.productId(), 0);
    }

    private static BigDecimal inventoryFactor(Request request, Candidate candidate) {
        double sum = request.lines().stream()
                .mapToDouble(line -> (double) line.quantity() / availableUnits(candidate, line))
                .sum();
        return decimal(sum / request.lines().size(), SCALE);
    }

    private static BigDecimal normalize(double value, double max) {
        return max <= 0 ? BigDecimal.ZERO.setScale(SCALE) : decimal(value / max, SCALE);
    }

    private static BigDecimal decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    private static Evaluation toEvaluation(Scored scored, int rank) {
        Metrics m = scored.metrics;
        return new Evaluation(
                m.candidate.warehouseId(), m.candidate.name(), true, null,
                decimal(m.km, 2), decimal(m.cost, 2), decimal(m.eta, 2),
                scored.normKm, scored.normCost, scored.inventory, scored.priority, scored.score,
                rank, rank == 1 ? EvaluationDecision.SELECTED : EvaluationDecision.NOT_SELECTED);
    }

    private static Evaluation toEvaluation(Metrics m) {
        return new Evaluation(
                m.candidate.warehouseId(), m.candidate.name(), false, m.rejectionReason,
                decimal(m.km, 2), decimal(m.cost, 2), decimal(m.eta, 2),
                null, null, null, null, null, null, EvaluationDecision.REJECTED);
    }

    /** Built only from computed facts, so a claim like "lowest cost" appears only when it is true. */
    private static String explain(List<Scored> ranked, List<Metrics> rejected) {
        StringBuilder text = new StringBuilder();
        if (ranked.isEmpty()) {
            text.append(rejected.isEmpty()
                    ? "No warehouses exist to fulfil this order."
                    : "No warehouse can fulfil the whole order, so nothing was reserved.");
        } else {
            text.append(explainWinner(ranked));
        }
        for (Metrics m : rejected) {
            text.append(' ').append(m.candidate.name()).append(" was rejected: ")
                    .append(Character.toLowerCase(m.rejectionReason.charAt(0)))
                    .append(m.rejectionReason.substring(1))
                    .append('.');
        }
        return text.toString();
    }

    private static String explainWinner(List<Scored> ranked) {
        Scored best = ranked.getFirst();
        String name = best.metrics.candidate.name();
        String km = "%.1f km".formatted(best.metrics.km);
        String cost = rupees(best.metrics.cost);
        if (ranked.size() == 1) {
            return "%s was selected: it was the only warehouse with sufficient inventory (%s, estimated delivery cost %s)."
                    .formatted(name, km, cost);
        }

        Scored next = ranked.get(1);
        String opening;
        if (best.score.compareTo(next.score) == 0) {
            opening = "%s was selected: it had sufficient inventory and tied for the lowest weighted score (%s), winning on %s"
                    .formatted(name, score(best.score), tieBreakReason(best, next));
        } else {
            opening = "%s was selected: it had sufficient inventory and the lowest weighted score (%s, next best %s)"
                    .formatted(name, score(best.score), score(next.score));
        }

        boolean shortest = ranked.stream().allMatch(s -> best.metrics.km <= s.metrics.km);
        boolean cheapest = ranked.stream().allMatch(s -> best.metrics.cost <= s.metrics.cost);
        if (shortest && cheapest) {
            return "%s, with the shortest distance (%s) and lowest delivery cost (%s) among eligible warehouses."
                    .formatted(opening, km, cost);
        }
        if (shortest) {
            return "%s, with the shortest distance (%s) among eligible warehouses.".formatted(opening, km);
        }
        if (cheapest) {
            return "%s, with the lowest delivery cost (%s) among eligible warehouses.".formatted(opening, cost);
        }
        return opening + ".";
    }

    private static String tieBreakReason(Scored best, Scored next) {
        if (best.metrics.km != next.metrics.km) {
            return "shorter distance";
        }
        return best.metrics.cost != next.metrics.cost ? "lower delivery cost" : "the lowest warehouse ID";
    }

    private static String score(BigDecimal score) {
        return score.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }

    private static String rupees(double amount) {
        return NumberFormat.getCurrencyInstance(Locale.of("en", "IN")).format(amount);
    }
}
