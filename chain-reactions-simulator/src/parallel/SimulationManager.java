package parallel;

import config.SimulationConfig;
import stats.ResultStats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SimulationManager {

    private final SimulationConfig config;

    public SimulationManager(SimulationConfig config) {
        this.config = config;
    }

    public ResultStats run() throws Exception {

        int threads = Math.min(Runtime.getRuntime().availableProcessors(), config.simulations);
        int baseRuns = config.simulations / threads;
        int remainingRuns = config.simulations % threads;

        ExecutorService executor =
                Executors.newFixedThreadPool(threads);

        List<Future<ResultStats>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            int runs = baseRuns + (i < remainingRuns ? 1 : 0);
            futures.add(executor.submit(
                    new SimulationWorker(config, runs, i)
            ));
        }

        ResultStats finalStats = new ResultStats();

        for (Future<ResultStats> f : futures) {
            finalStats.merge(f.get());
        }

        executor.shutdown();

        return finalStats;
    }
}
