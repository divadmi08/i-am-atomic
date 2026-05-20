package core;

import java.util.SplittableRandom;

public class MarkovModel {

    private final SplittableRandom random;

    public MarkovModel(SplittableRandom random) {
        this.random = random;
    }

    public int step(int neutrons, double k, double enrichmentPercent) {

        int next = 0;

        for (int i = 0; i < neutrons; i++) {
            next += sampleOffspring(k, enrichmentPercent);
        }

        return next;
    }

    private int sampleOffspring(double k, double enrichmentPercent) {
        if (k <= 0.0) {
            return 0;
        }

        double enrichment = enrichmentPercent / 100.0;
        double zeroProbability = clamp(0.46 - 0.18 * enrichment, 0.18, 0.55);

        if (random.nextDouble() < zeroProbability) {
            return 0;
        }

        double activeMean = k / (1.0 - zeroProbability);
        int whole = (int) activeMean;
        double fractional = activeMean - whole;

        int offspring = whole;
        if (random.nextDouble() < fractional) {
            offspring++;
        }

        if (offspring == 2 && random.nextDouble() < 0.32) {
            offspring++;
        }

        return Math.min(offspring, 4);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
