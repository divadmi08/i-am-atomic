package parallel;

import config.SimulationConfig;
import stats.ResultStats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Orchestratore del parallelismo.
 * Divide il totale delle simulazioni tra N thread,
 * lancia i worker, aspetta che finiscano e aggrega i risultati.
 *
 * RICHIEDE (costruttore):
 *   - SimulationConfig config → parametri della simulazione
 *
 * RESTITUISCE (run):
 *   - ResultStats             → risultato aggregato di tutte le simulazioni
 */
public class SimulationManager {

    private final SimulationConfig config;

    // Costruttore: memorizza il config, non fa altro.
    public SimulationManager(SimulationConfig config) {
        this.config = config;
    }

    /**
     * Metodo principale: lancia i thread e restituisce il risultato finale.
     * Dichiara 'throws Exception' perché Future.get() può lanciare
     * InterruptedException o ExecutionException.
     *
     * RESTITUISCE: ResultStats aggregato di tutti i thread.
     */
    public ResultStats run() throws Exception {

        // Sceglie quanti thread usare.
        // availableProcessors() restituisce il numero di core logici della JVM.
        // Math.min evita di creare più thread delle simulazioni totali
        // (es: 100 simulazioni su 16 core → usa solo 100 thread sarebbe assurdo,
        // quindi min(16, 100) = 16 va bene; ma min(16, 5) = 5 se le simulazioni
        // sono pochissime).
        int threads = Math.min(
                Runtime.getRuntime().availableProcessors(),
                config.simulations
        );

        // Divide equamente le simulazioni tra i thread.
        // Esempio: 1000 simulazioni su 3 thread → baseRuns = 333, remainingRuns = 1
        // → thread 0 fa 334, thread 1 fa 333, thread 2 fa 333. Totale: 1000. ✓
        int baseRuns = config.simulations / threads;
        int remainingRuns = config.simulations % threads;

        // Crea un pool di thread fissi: esattamente 'threads' thread disponibili.
        // I Callable vengono accodati e assegnati ai thread man mano che si liberano.
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        // Lista di Future: ognuno rappresenta il risultato futuro di un worker.
        // Future è un "promise" — il risultato arriverà quando il thread finisce.
        List<Future<ResultStats>> futures = new ArrayList<>();

        // Crea e sottomette un worker per ogni thread.
        for (int i = 0; i < threads; i++) {

            // I primi 'remainingRuns' thread fanno baseRuns + 1 prove,
            // gli altri fanno esattamente baseRuns. Questo distribuisce il
            // resto (simulazioni % threads) senza sprechi.
            int runs = baseRuns + (i < remainingRuns ? 1 : 0);

            // executor.submit(Callable) mette il worker in coda,
            // lo fa partire appena un thread è disponibile,
            // e restituisce subito un Future (non aspetta il risultato).
            futures.add(executor.submit(
                    new SimulationWorker(config, runs, i)
            ));
        }

        // Accumulatore finale: raccoglierà i risultati di tutti i thread.
        ResultStats finalStats = new ResultStats();

        // Itera sui Future in ordine di sottomissione.
        for (Future<ResultStats> f : futures) {

            // f.get() è una chiamata BLOCCANTE:
            // aspetta che quel worker abbia finito, poi restituisce il suo ResultStats.
            // Se il worker ha lanciato un'eccezione, qui viene re-lanciata
            // come ExecutionException.
            finalStats.merge(f.get());
        }

        // Spegne il pool: non accetta nuovi task, aspetta che quelli in corso finiscano
        // (ma qui sono già tutti finiti dopo il ciclo sopra), poi libera i thread.
        executor.shutdown();

        // Restituisce il ResultStats aggregato di tutte le simulazioni.
        return finalStats;
    }
}