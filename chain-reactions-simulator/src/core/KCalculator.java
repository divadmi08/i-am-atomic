package core;

import config.SimulationConfig;

public class KCalculator {

    public static double compute(SimulationConfig c) {

        double enrichment = c.enrichmentPercent / 100.0;
        double densityFactor = clamp(Math.sqrt(c.densityGcm3 / 6.0), 0.15, 1.8);
        double massFactor = 1.0 - Math.exp(-c.massKg / 52.0);
        double reflectorFactor = 1.0 + 0.35 * (c.reflectorPercent / 100.0);

        double leakagePenalty = 0.34 * Math.exp(-c.massKg / 38.0);
        double reproduction = 2.15 * enrichment * densityFactor * massFactor * reflectorFactor;

        double k = reproduction - leakagePenalty;

        return clamp(k, 0.0, 2.5);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
