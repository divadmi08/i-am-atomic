# Chain Reactions Simulator

Simulatore Monte Carlo in Java di reazioni a catena neutroniche, con esecuzione parallela e interfaccia console.

Il progetto non vuole essere un codice di fisica nucleare ad alta fedelta. Il modello e intenzionalmente semplificato: serve per esplorare in modo intuitivo come massa, arricchimento, densita, riflettore e condizioni iniziali influenzano la probabilita di spegnimento, stabilita, criticita o crescita esplosiva della reazione.

## Obiettivo

L'applicazione esegue molte simulazioni indipendenti della stessa configurazione e stima la frequenza relativa di quattro esiti:

- `STABLE`
- `FAILED`
- `CRITICAL`
- `EXPLOSION`

Ogni simulazione parte da un certo numero iniziale di neutroni, evolve per un numero massimo di step e usa un modello stocastico per aggiornare la popolazione neutronica.

## Struttura del progetto

```text
chain-reactions-simulator/
  src/
    app/       entry point
    config/    validazione e parametri
    core/      modello matematico e simulazione
    model/     enum degli esiti
    parallel/  esecuzione multi-thread
    stats/     aggregazione risultati
    view/      input/output console
```

Componenti principali:

- `app/SimulationApp.java`: ciclo principale dell'applicazione.
- `view/ConsoleView.java`: raccoglie input utente e stampa i risultati.
- `config/SimulationConfig.java`: contiene i parametri della simulazione e le regole di validazione.
- `core/KCalculator.java`: calcola il fattore moltiplicativo effettivo `k`.
- `core/MarkovModel.java`: simula la produzione casuale di neutroni a ogni step.
- `core/ReactionSimulator.java`: decide l'esito di una singola prova.
- `parallel/SimulationManager.java`: divide il lavoro fra piu thread.
- `parallel/SimulationWorker.java`: esegue un blocco di simulazioni.
- `stats/ResultStats.java`: accumula e combina le frequenze osservate.

## Come funziona

Il flusso e questo:

1. L'utente inserisce i parametri dalla console.
2. Viene creato un oggetto `SimulationConfig`.
3. `SimulationManager` divide il numero totale di simulazioni sui thread disponibili.
4. Ogni worker esegue piu prove indipendenti con `ReactionSimulator`.
5. Per ogni prova:
   - viene calcolato `k`;
   - si parte da `initialNeutrons`;
   - a ogni step si genera la nuova popolazione neutronica;
   - se i neutroni scendono a zero la prova fallisce;
   - se superano la soglia di esplosione la prova termina come esplosiva;
   - se si raggiunge il limite di step senza estinzione o esplosione, l'esito dipende da `k`.
6. I risultati di tutti i thread vengono aggregati e convertiti in percentuali.

## Modello matematico

### 1. Calcolo del fattore `k`

Il codice usa la formula:

```text
enrichment     = enrichmentPercent / 100
densityFactor  = clamp(sqrt(densityGcm3 / 6.0), 0.15, 1.8)
massFactor     = 1 - exp(-massKg / 52.0)
reflectorFactor= 1 + 0.35 * (reflectorPercent / 100)

leakagePenalty = 0.34 * exp(-massKg / 38.0)
reproduction   = 2.15 * enrichment * densityFactor * massFactor * reflectorFactor

k = clamp(reproduction - leakagePenalty, 0.0, 2.5)
```

Interpretazione qualitativa:

- `enrichment`: piu materiale fissile utile e presente, maggiore e la probabilita di sostenere la catena.
- `densityFactor`: una densita maggiore tende a favorire interazioni utili tra neutroni e materiale.
- `massFactor`: con piu massa aumenta il confinamento statistico della reazione.
- `reflectorFactor`: un riflettore piu efficace riduce le perdite verso l'esterno.
- `leakagePenalty`: penalita che rappresenta la perdita di neutroni, piu rilevante per masse basse.

Il valore finale `k` viene limitato nell'intervallo `[0.0, 2.5]` per evitare dinamiche numeriche estreme.

### 2. Evoluzione della popolazione neutronica

Dato un numero corrente di neutroni `N`, a ogni step il simulatore genera il numero successivo sommando i contributi indipendenti di ciascun neutrone:

```text
N_(t+1) = sum_{i=1..N_t} X_i
```

dove `X_i` e il numero casuale di neutroni figli generati dal neutrone `i`.

Il campionamento di `X_i` avviene cosi:

```text
zeroProbability = clamp(0.46 - 0.18 * enrichment, 0.18, 0.55)
```

Con probabilita `zeroProbability`, il neutrone non produce alcun discendente:

```text
X = 0
```

Altrimenti si calcola una media attiva:

```text
activeMean = k / (1 - zeroProbability)
```

Da questa media il codice costruisce una variabile discreta:

- prende la parte intera `whole = floor(activeMean)`
- usa la parte frazionaria per decidere se aggiungere `+1`
- se il risultato e `2`, con probabilita `0.32` promuove il valore a `3`
- infine tronca a massimo `4`

In pratica, il modello forza una distribuzione semplice, discreta e limitata:

```text
X in {0, 1, 2, 3, 4}
```

Questa non e una distribuzione derivata da dati sperimentali. E una scelta euristica per ottenere dinamiche plausibili e leggibili.

### 3. Criterio di classificazione degli esiti

Per una singola simulazione:

- `FAILED`: i neutroni diventano `0` prima del numero massimo di step.
- `EXPLOSION`: i neutroni superano `explosionThreshold`.
- `CRITICAL`: non si verifica ne spegnimento ne esplosione entro gli step massimi e `k >= 1.0`.
- `STABLE`: non si verifica ne spegnimento ne esplosione entro gli step massimi e `k < 1.0`.

La soglia di esplosione e:

```text
explosionThreshold = max(500, initialNeutrons * 10)
```

Quindi aumentando i neutroni iniziali cresce anche la soglia necessaria per classificare l'esito come esplosivo.

## Significato dei parametri

### `simulations`

Numero di prove indipendenti eseguite.

- Se aumenta: le percentuali finali diventano piu stabili e meno rumorose.
- Controparte: aumentano tempo di esecuzione e carico CPU.

### `massKg`

Massa del materiale fissile.

- Se aumenta: cresce `massFactor` e si riduce `leakagePenalty`.
- Effetto atteso: la reazione tende a sostenersi piu facilmente.

### `enrichmentPercent`

Percentuale di materiale effettivamente fissile.

- Se aumenta: cresce direttamente il termine di riproduzione.
- Riduce anche `zeroProbability`, cioe abbassa la probabilita che un neutrone non generi discendenti.
- Effetto atteso: piu criticita, meno spegnimento.

### `densityGcm3`

Densita del materiale.

- Se aumenta: cresce `densityFactor` secondo una radice quadrata.
- Effetto atteso: incremento moderato ma reale della capacita di sostenere la catena.

### `reflectorPercent`

Efficacia del riflettore.

- Se aumenta: incrementa `reflectorFactor`.
- Effetto atteso: meno neutroni persi all'esterno, quindi `k` piu alto.

### `steps`

Numero massimo di iterazioni per singola prova.

- Se aumenta: il sistema ha piu tempo per spegnersi o crescere.
- Valori bassi possono classificare come `STABLE` o `CRITICAL` situazioni che con piu tempo evolverebbero in altro modo.

### `initialNeutrons`

Numero iniziale di neutroni per ciascuna prova.

- Se aumenta: la catena parte con piu massa statistica e tende a mostrare piu chiaramente la dinamica media.
- Riduce la varianza relativa delle prime iterazioni.
- Aumenta pero anche `explosionThreshold`, quindi la classificazione `EXPLOSION` richiede numeri assoluti piu alti.

## Come leggere gli esiti

### `FAILED`

Configurazione subcritica o molto dissipativa. I neutroni si esauriscono.

### `STABLE`

La simulazione non collassa ne diverge entro gli step osservati, ma il modello vede `k < 1`. In termini qualitativi, la reazione non e autosostenuta in senso forte.

### `CRITICAL`

Il sistema resta attivo entro la finestra osservata e il fattore `k` e almeno unitario. E il caso vicino a una condizione autosostenuta.

### `EXPLOSION`

La popolazione neutronica supera una soglia alta in tempi relativamente brevi. Nel simulatore significa crescita rapida della catena, non una previsione fisica reale di evento esplosivo.

## Parallelismo

Il numero di thread usati e:

```text
threads = min(availableProcessors, simulations)
```

Ogni thread:

- riceve una quota di simulazioni;
- usa un proprio `SplittableRandom`;
- produce un `ResultStats` locale;
- restituisce il risultato al manager, che lo aggrega.

Questo approccio migliora le prestazioni sulle macchine multi-core senza introdurre condivisione complessa dello stato durante le prove.

## Validazione input

Il progetto impone questi limiti:

- `simulations`: `100` - `50_000_000`
- `massKg`: `0.1` - `10_000.0`
- `densityGcm3`: `0.1` - `25.0`
- `steps`: `1` - `2_000`
- `initialNeutrons`: `1` - `10_000`
- `enrichmentPercent`: `0` - `100`
- `reflectorPercent`: `0` - `100`

I valori vengono controllati in `SimulationConfig` e l'input console gestisce reinserimento in caso di errore.

## Esempio qualitativo

Tendenza generale del modello:

- massa bassa + arricchimento basso + densita bassa -> prevalgono `FAILED`
- parametri intermedi vicino a `k ~= 1` -> cresce la frequenza di `CRITICAL`
- massa alta + arricchimento alto + buon riflettore -> aumenta la probabilita di `EXPLOSION`

Non bisogna leggere questi risultati come soglie fisiche reali. Sono comportamenti del modello implementato nel codice.

## Configurazioni di esempio

Gli esempi sotto servono per orientarsi nell'uso del simulatore. Non garantiscono un esito al 100% su ogni singola prova: descrivono configurazioni che, in genere, nel modello tendono a produrre soprattutto quel comportamento.

Importante: in questo progetto `steps` e `initialNeutrons` cambiano molto il risultato finale. Quindi qui sotto ogni esempio riporta tutti i parametri, non solo massa, arricchimento, densita e riflettore.

Le percentuali riportate sono indicative e dipendono dal seed casuale e dal numero di simulazioni. Sono state verificate sul codice attuale con `200000` simulazioni.

### Esempio `FAILED`

Parametri:

- `simulations = 200000`
- `massKg = 2`
- `enrichmentPercent = 10`
- `densityGcm3 = 2`
- `reflectorPercent = 0`
- `steps = 100`
- `initialNeutrons = 1`

Interpretazione:

- poca massa
- poco materiale fissile utile
- densita bassa
- nessun riflettore

Con questa combinazione il modello tende a far spegnere rapidamente la catena, quindi la percentuale `FAILED` dovrebbe dominare.

Risultato osservato:

- `FAILED` circa `100%`

### Esempio `STABLE`

Parametri:

- `simulations = 200000`
- `massKg = 55`
- `enrichmentPercent = 58`
- `densityGcm3 = 8`
- `reflectorPercent = 40`
- `steps = 20`
- `initialNeutrons = 10`

Interpretazione:

- qui `k` e appena sotto `1`, circa `0.99`
- gli step sono pochi abbastanza da permettere a molte prove di restare vive fino alla fine
- il numero iniziale di neutroni riduce l'estinzione immediata nelle prime iterazioni

In questa zona il modello tende a mostrare `STABLE` come esito dominante, con una quota minore di `FAILED`.

Risultato osservato:

- `STABLE` circa `90.0%`
- `FAILED` circa `10.0%`
- `EXPLOSION` quasi `0%`

### Esempio `CRITICAL`

Parametri:

- `simulations = 200000`
- `massKg = 55`
- `enrichmentPercent = 60`
- `densityGcm3 = 8`
- `reflectorPercent = 40`
- `steps = 20`
- `initialNeutrons = 10`

Interpretazione:

- qui `k` supera di poco `1`, circa `1.03`
- la catena tende a non estinguersi facilmente
- ma non sempre cresce abbastanza da oltrepassare la soglia di esplosione

Questa e una configurazione ragionevole da usare quando vuoi osservare una zona vicina alla criticita nel modello.

Risultato osservato:

- `CRITICAL` circa `93.3%`
- `FAILED` circa `4.9%`
- `EXPLOSION` circa `1.7%`

### Esempio `EXPLOSION`

Parametri:

- `simulations = 200000`
- `massKg = 80`
- `enrichmentPercent = 75`
- `densityGcm3 = 10`
- `reflectorPercent = 70`
- `steps = 100`
- `initialNeutrons = 1`

Interpretazione:

- alta massa
- arricchimento elevato
- densita elevata
- buon riflettore

Qui il modello tende a produrre crescita rapida della popolazione neutronica e quindi una quota alta di `EXPLOSION`.

Risultato osservato:

- `EXPLOSION` circa `64.4%`
- `FAILED` circa `35.6%`

### Variante con piu neutroni iniziali

Se vuoi vedere piu chiaramente la differenza tra configurazioni subcritiche e supercritiche, puoi provare anche:

- `initialNeutrons = 10`

Effetto atteso:

- le prove diventano meno sensibili al caso nelle prime iterazioni
- le dinamiche medie emergono piu in fretta
- ma cresce anche `explosionThreshold`, quindi non sempre aumenta la percentuale finale di `EXPLOSION`

### Metodo pratico per esplorare

Se vuoi usare il simulatore in modo sistematico, conviene:

1. fissare `simulations`, `steps` e `initialNeutrons`
2. partire da un caso chiaramente `FAILED`
3. aumentare un parametro alla volta, per esempio prima `massKg`, poi `enrichmentPercent`
4. osservare quando iniziano a salire `STABLE` e `CRITICAL`
5. aumentare ancora densita e riflettore per spingere il sistema verso `EXPLOSION`

In pratica:

- `massKg` e `enrichmentPercent` sono i controlli piu forti
- `densityGcm3` e `reflectorPercent` rifiniscono il comportamento
- `steps` influenza quanto tempo dai al sistema per mostrare la sua vera dinamica
- `initialNeutrons` cambia molto la probabilita di sopravvivenza iniziale, quindi pesa soprattutto nei casi `STABLE` e `CRITICAL`

## Come eseguire

Il repository al momento non include Maven o Gradle. Si puo eseguire compilando Java manualmente dalla cartella del progetto.

Esempio:

```powershell
cd chain-reactions-simulator
mkdir out
javac -d out src\app\SimulationApp.java src\config\SimulationConfig.java src\core\KCalculator.java src\core\MarkovModel.java src\core\ReactionSimulator.java src\model\Outcome.java src\parallel\SimulationManager.java src\parallel\SimulationWorker.java src\stats\ResultStats.java src\view\ConsoleView.java
java -cp out app.SimulationApp
```

Serve un JDK con supporto alle feature Java usate nel codice, inclusi gli switch expression.

## Limiti del progetto

- Il modello e euristico, non calibrato su dati reali.
- `k` non deriva da un formalismo fisico completo.
- La distribuzione dei neutroni figli e costruita a mano per semplicita.
- Gli esiti sono categorie del simulatore, non classificazioni scientifiche.
- Manca una suite di test automatizzati.
- Manca un sistema di build standard del mondo Java.

## Possibili sviluppi

- aggiungere `README` tecnico con esempi numerici riproducibili
- introdurre test unitari e test statistici
- aggiungere Maven o Gradle
- esportare risultati in CSV
- salvare seed casuali per ripetibilita
- rendere parametrica la distribuzione di offspring
- separare meglio modello fisico, motore Monte Carlo e presentazione

## Nota importante

Questo progetto va interpretato come simulatore didattico/stocastico semplificato. Non va usato per analisi scientifiche, ingegneristiche o decisionali nel mondo reale.
