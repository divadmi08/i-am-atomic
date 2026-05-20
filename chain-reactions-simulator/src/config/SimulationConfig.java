package config;

public class SimulationConfig {

    public static final int MIN_SIMULATIONS = 100;
    public static final int MAX_SIMULATIONS = 50_000_000;
    public static final double MIN_MASS_KG = 0.1;
    public static final double MAX_MASS_KG = 10_000.0;
    public static final double MIN_DENSITY_GCM3 = 0.1;
    public static final double MAX_DENSITY_GCM3 = 25.0;
    public static final int MIN_STEPS = 1;
    public static final int MAX_STEPS = 2_000;
    public static final int MIN_INITIAL_NEUTRONS = 1;
    public static final int MAX_INITIAL_NEUTRONS = 10_000;

    public final int simulations;
    public final double massKg;
    public final double enrichmentPercent;
    public final double densityGcm3;
    public final double reflectorPercent;
    public final int steps;
    public final int initialNeutrons;
    public final int explosionThreshold;

    public SimulationConfig(int simulations, double massKg, double enrichmentPercent,
                            double densityGcm3, double reflectorPercent, int steps) {
        this(simulations, massKg, enrichmentPercent, densityGcm3, reflectorPercent, steps, 1);
    }

    public SimulationConfig(int simulations, double massKg, double enrichmentPercent,
                            double densityGcm3, double reflectorPercent, int steps,
                            int initialNeutrons) {
        if (simulations < MIN_SIMULATIONS || simulations > MAX_SIMULATIONS) {
            throw new IllegalArgumentException("simulations must be between " + MIN_SIMULATIONS + " and " + MAX_SIMULATIONS);
        }
        if (massKg < MIN_MASS_KG || massKg > MAX_MASS_KG) {
            throw new IllegalArgumentException("massKg must be between " + MIN_MASS_KG + " and " + MAX_MASS_KG);
        }
        if (enrichmentPercent < 0 || enrichmentPercent > 100) {
            throw new IllegalArgumentException("enrichmentPercent must be between 0 and 100");
        }
        if (densityGcm3 < MIN_DENSITY_GCM3 || densityGcm3 > MAX_DENSITY_GCM3) {
            throw new IllegalArgumentException("densityGcm3 must be between " + MIN_DENSITY_GCM3 + " and " + MAX_DENSITY_GCM3);
        }
        if (reflectorPercent < 0 || reflectorPercent > 100) {
            throw new IllegalArgumentException("reflectorPercent must be between 0 and 100");
        }
        if (steps < MIN_STEPS || steps > MAX_STEPS) {
            throw new IllegalArgumentException("steps must be between " + MIN_STEPS + " and " + MAX_STEPS);
        }
        if (initialNeutrons < MIN_INITIAL_NEUTRONS || initialNeutrons > MAX_INITIAL_NEUTRONS) {
            throw new IllegalArgumentException("initialNeutrons must be between " + MIN_INITIAL_NEUTRONS + " and " + MAX_INITIAL_NEUTRONS);
        }

        this.simulations = simulations;
        this.massKg = massKg;
        this.enrichmentPercent = enrichmentPercent;
        this.densityGcm3 = densityGcm3;
        this.reflectorPercent = reflectorPercent;
        this.steps = steps;
        this.initialNeutrons = initialNeutrons;
        this.explosionThreshold = computeExplosionThreshold(initialNeutrons);
    }

    private static int computeExplosionThreshold(int initialNeutrons) {
        return Math.max(500, initialNeutrons * 10);
    }
}
