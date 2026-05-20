package view;

import config.SimulationConfig;
import stats.ResultStats;

import java.util.Locale;
import java.util.Scanner;

public class ConsoleView {

    private final Scanner input = new Scanner(System.in);

    public SimulationConfig readConfig() {
        printParameterGuide();

        while (true) {
            try {
                int simulations = readInt("Simulazioni", 10_000_000,
                        SimulationConfig.MIN_SIMULATIONS, SimulationConfig.MAX_SIMULATIONS);
                double massKg = readDouble("Massa materiale fissile in kg", 1000,
                        SimulationConfig.MIN_MASS_KG, SimulationConfig.MAX_MASS_KG);
                double enrichmentPercent = readDouble("Arricchimento percentuale", 40, 0, 100);
                double densityGcm3 = readDouble("Densita g/cm3", 6,
                        SimulationConfig.MIN_DENSITY_GCM3, SimulationConfig.MAX_DENSITY_GCM3);
                double reflectorPercent = readDouble("Riflettore percentuale", 40, 0, 100);
                int steps = readInt("Step massimi", 100,
                        SimulationConfig.MIN_STEPS, SimulationConfig.MAX_STEPS);
                int initialNeutrons = readInt("Neutroni iniziali", 1,
                        SimulationConfig.MIN_INITIAL_NEUTRONS, SimulationConfig.MAX_INITIAL_NEUTRONS);

                return new SimulationConfig(
                        simulations,
                        massKg,
                        enrichmentPercent,
                        densityGcm3,
                        reflectorPercent,
                        steps,
                        initialNeutrons
                );
            } catch (IllegalArgumentException e) {
                System.out.println("Errore nei parametri: " + e.getMessage());
                System.out.println("Reinserisci i valori.");
            }
        }
    }

    public void printResults(ResultStats stats) {
        long total = stats.total();

        System.out.println();
        printPercentage("STABLE", stats.stable, total);
        printPercentage("FAILED", stats.failed, total);
        printPercentage("CRITICAL", stats.critical, total);
        printPercentage("EXPLOSION", stats.explosion, total);
    }

    public boolean shouldContinue() {
        while (true) {
            System.out.print("Premi Invio per continuare oppure x per uscire: ");
            String value = input.nextLine().trim();

            if (value.isEmpty()) {
                System.out.println();
                return true;
            }
            if (value.equalsIgnoreCase("x")) {
                return false;
            }

            System.out.println("Scelta non valida.");
        }
    }

    private int readInt(String label, int defaultValue, int min, int max) {
        while (true) {
            System.out.print(label + " [" + defaultValue + ", " + min + "-" + max + "]: ");
            String value = input.nextLine().trim();

            if (value.isEmpty()) {
                return defaultValue;
            }

            try {
                int parsed = Integer.parseInt(value);
                if (parsed < min || parsed > max) {
                    System.out.println("Inserisci un valore tra " + min + " e " + max + ".");
                    continue;
                }
                return parsed;
            } catch (NumberFormatException e) {
                System.out.println("Inserisci un numero intero valido.");
            }
        }
    }

    private double readDouble(String label, double defaultValue, double min, double max) {
        while (true) {
            System.out.print(label + " [" + defaultValue + ", " + min + "-" + max + "]: ");
            String value = input.nextLine().trim().replace(',', '.');

            if (value.isEmpty()) {
                return defaultValue;
            }

            try {
                double parsed = Double.parseDouble(value);
                if (!Double.isFinite(parsed)) {
                    System.out.println("Inserisci un numero finito.");
                    continue;
                }
                if (parsed < min || parsed > max) {
                    System.out.println("Inserisci un valore tra " + min + " e " + max + ".");
                    continue;
                }
                return parsed;
            } catch (NumberFormatException e) {
            System.out.println("Inserisci un numero valido.");
            }
        }
    }

    private void printParameterGuide() {
        System.out.println();
        System.out.println("Parametri:");
        System.out.println("- Simulazioni: quante prove indipendenti eseguire. Aumenta la precisione, ma richiede piu tempo.");
        System.out.println("- Massa materiale fissile: quantita di materiale. Massa piu alta riduce le perdite e facilita la reazione.");
        System.out.println("- Arricchimento: percentuale di materiale fissile. Piu e alto, piu cresce la probabilita di fissione.");
        System.out.println("- Densita: quanto il materiale e compatto. Densita piu alta aumenta gli urti utili tra neutroni e materiale.");
        System.out.println("- Riflettore: capacita di rimandare neutroni nel sistema. Piu e alto, meno neutroni vengono persi.");
        System.out.println("- Step massimi: durata della simulazione. Piu step permettono catene piu lunghe, ma costano piu tempo.");
        System.out.println("- Neutroni iniziali: neutroni da cui parte ogni prova. Valori alti rendono la crescita piu facile da osservare.");
        System.out.println();
    }

    private void printPercentage(String label, long count, long total) {
        System.out.printf(Locale.US, "%s: %.6f%%%n", label, percent(count, total));
    }

    private double percent(long count, double total) {
        if (total == 0.0) {
            return 0.0;
        }
        return 100.0 * count / total;
    }

}
