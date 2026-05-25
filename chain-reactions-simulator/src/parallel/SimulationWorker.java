package parallel;

import config.SimulationConfig;
import core.ReactionSimulator;
import stats.ResultStats;
import model.Outcome;

import java.util.SplittableRandom;
import java.util.concurrent.Callable;

/**
 * Un singolo worker che gira su un thread.
 * Implementa Callable<ResultStats> così il thread pool può
 * raccogliere il risultato tramite Future.get().
 *
 * RICHIEDE (costruttore):
 *   - SimulationConfig config  → parametri fisici della simulazione
 *   - int runs                 → quante prove deve eseguire questo worker
 *   - int workerIndex          → indice del worker (usato per diversificare il seed)
 *
 * RESTITUISCE (call):
 *   - ResultStats              → contatori delle prove eseguite da questo worker
 */
public class SimulationWorker implements Callable<ResultStats> {

    private final SimulationConfig config;
    private final int runs;
    private final int workerIndex;

    // Costruttore: memorizza i parametri che arrivano dal SimulationManager.
    // Non fa nient'altro — nessuna simulazione viene avviata qui.
    public SimulationWorker(SimulationConfig config, int runs, int workerIndex) {
        this.config = config;
        this.runs = runs;
        this.workerIndex = workerIndex;
    }

    /**
     * Questo metodo viene chiamato automaticamente dal thread pool
     * quando il thread parte. È l'equivalente di run() per Runnable,
     * ma in più può restituire un valore e lanciare eccezioni checked.
     *
     * RESTITUISCE: ResultStats con i contatori di questo blocco di prove.
     */
    @Override
    public ResultStats call() {

        // Crea un generatore casuale indipendente per questo thread.
        // System.nanoTime() garantisce un seed basato sull'orologio ad alta risoluzione.
        // + workerIndex evita che due thread creati quasi simultaneamente
        // ottengano lo stesso nanoTime e quindi lo stesso seed.
        // SplittableRandom è thread-safe per uso singolo (non condiviso tra thread).
        ReactionSimulator sim = new ReactionSimulator(
                new SplittableRandom(System.nanoTime() + workerIndex)
        );

        // Accumulatore locale: nessuna condivisione con altri thread,
        // quindi non serve sincronizzazione.
        ResultStats stats = new ResultStats();

        // Esegue esattamente 'runs' prove indipendenti.
        for (int i = 0; i < runs; i++) {

            // Ogni chiamata a sim.run() è una simulazione completa:
            // parte da initialNeutrons, evolve per al massimo 'steps' iterazioni,
            // e restituisce uno dei quattro Outcome.
            Outcome o = sim.run(config);

            // Aggiunge l'esito al contatore locale.
            stats.add(o);
        }

        // Restituisce il ResultStats al SimulationManager tramite Future.
        return stats;
    }
}