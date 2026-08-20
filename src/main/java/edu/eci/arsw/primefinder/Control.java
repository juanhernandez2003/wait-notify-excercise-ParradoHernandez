/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arsw.primefinder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Hilo controlador: lanza los hilos trabajadores y, cada TMILISECONDS,
 * los suspende a todos, reporta cuantos primos se han encontrado y espera
 * ENTER para reanudarlos.
 */
public class Control extends Thread {

    private final static int NTHREADS = 3;
    private final static int MAXVALUE = 30000000;
    private final static int TMILISECONDS = 5000;

    private final int NDATA = MAXVALUE / NTHREADS;

    private final PrimeFinderThread pft[];

    /** Monitor unico compartido con todos los hilos trabajadores. */
    private final PauseMonitor monitor;

    private Control() {
        super();
        this.pft = new PrimeFinderThread[NTHREADS];
        this.monitor = new PauseMonitor(NTHREADS);

        int i;
        for (i = 0; i < NTHREADS - 1; i++) {
            PrimeFinderThread elem = new PrimeFinderThread(i * NDATA, (i + 1) * NDATA, monitor);
            pft[i] = elem;
        }
        pft[i] = new PrimeFinderThread(i * NDATA, MAXVALUE + 1, monitor);
    }

    public static Control newControl() {
        return new Control();
    }

    @Override
    public void run() {
        for (int i = 0; i < NTHREADS; i++) {
            pft[i].start();
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        try {
            while (true) {

                Thread.sleep(TMILISECONDS);

                if (monitor.allFinished()) {
                    break;
                }

                // 1. Se activa la condicion de pausa.
                monitor.pauseAll();

                // 2. Se espera (con wait(), sin espera activa) a que todos los
                //    hilos vivos esten realmente suspendidos, para que el
                //    conteo corresponda a un estado consistente.
                monitor.awaitAllPaused();

                // 3. Reporte.
                System.out.println();
                System.out.println("=====================================================");
                System.out.println(" PAUSA: se han encontrado " + totalPrimes() + " numeros primos.");
                System.out.println(" Presione ENTER para reanudar...");
                System.out.println("=====================================================");
                in.readLine();

                // 4. Se libera la pausa y se despierta a todos los hilos.
                monitor.resumeAll();
            }

            for (int i = 0; i < NTHREADS; i++) {
                pft[i].join();
            }

            System.out.println();
            System.out.println("Busqueda terminada. Total de primos encontrados: " + totalPrimes());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Suma los primos hallados por todos los hilos trabajadores. */
    private int totalPrimes() {
        int total = 0;
        for (int i = 0; i < NTHREADS; i++) {
            total += pft[i].getPrimeCount();
        }
        return total;
    }

}
