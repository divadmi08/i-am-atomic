package app;

import config.SimulationConfig;
import parallel.SimulationManager;
import stats.ResultStats;
import view.ConsoleView;

import java.util.Locale;

public class SimulationApp {

    public static void main(String[] args) throws Exception {

        Locale.setDefault(Locale.US);
        ConsoleView view = new ConsoleView();

        do {
            SimulationConfig config = view.readConfig();
            SimulationManager manager = new SimulationManager(config);
            ResultStats stats = manager.run();

            view.printResults(stats);
        } while (view.shouldContinue());
    }
}
