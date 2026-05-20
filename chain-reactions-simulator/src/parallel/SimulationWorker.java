package parallel;

import config.SimulationConfig;
import core.ReactionSimulator;
import stats.ResultStats;
import model.Outcome;

import java.util.SplittableRandom;
import java.util.concurrent.Callable;

public class SimulationWorker implements Callable<ResultStats> {

    private final SimulationConfig config;
    private final int runs;
    private final int workerIndex;

    public SimulationWorker(SimulationConfig config, int runs, int workerIndex) {
        this.config = config;
        this.runs = runs;
        this.workerIndex = workerIndex;
    }

    @Override
    public ResultStats call() {

        ReactionSimulator sim = new ReactionSimulator(new SplittableRandom(System.nanoTime() + workerIndex));
        ResultStats stats = new ResultStats();

        for (int i = 0; i < runs; i++) {
            Outcome o = sim.run(config);
            stats.add(o);
        }

        return stats;
    }
}
