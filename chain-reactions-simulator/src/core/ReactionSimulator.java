package core;

import config.SimulationConfig;
import model.Outcome;

import java.util.SplittableRandom;

public class ReactionSimulator {

    private final MarkovModel model;

    public ReactionSimulator(SplittableRandom random) {
        this.model = new MarkovModel(random);
    }

    public Outcome run(SimulationConfig c) {

        double k = KCalculator.compute(c);

        int neutrons = c.initialNeutrons;

        for (int i = 0; i < c.steps; i++) {

            neutrons = model.step(neutrons, k, c.enrichmentPercent);

            if (neutrons == 0)
                return Outcome.FAILED;

            if (neutrons > c.explosionThreshold)
                return Outcome.EXPLOSION;
        }

        if (k >= 1.0)
            return Outcome.CRITICAL;

        return Outcome.STABLE;
    }
}
