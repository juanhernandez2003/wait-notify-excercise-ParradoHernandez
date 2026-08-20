package edu.eci.arsw.primefinder;

/**
 * Monitor compartido por todos los hilos del programa.
 *
 * Es el UNICO objeto sobre el que se hace synchronized / wait() / notifyAll(),
 * de modo que todo el estado de la pausa (bandera 'paused', numero de hilos
 * suspendidos y numero de hilos vivos) se lee y se escribe siempre bajo el
 * mismo lock. Esto evita condiciones de carrera y "lost wakeups".
 */
public class PauseMonitor {

    /**
     * Condicion de pausa. Es volatile unicamente para permitir el "camino
     * rapido" de checkPause(): un hilo trabajador puede leerla sin tomar el
     * lock cuando no hay pausa pendiente. La escritura y toda la logica de
     * espera siguen ocurriendo dentro de bloques synchronized.
     */
    private volatile boolean paused = false;

    /** Cuantos hilos trabajadores estan efectivamente detenidos en wait(). */
    private int pausedWorkers = 0;

    /** Cuantos hilos trabajadores siguen vivos (no han terminado su rango). */
    private int liveWorkers;

    public PauseMonitor(int workers) {
        this.liveWorkers = workers;
    }

    /**
     * Lo invoca el hilo controlador: activa la condicion de pausa.
     * No bloquea; solo cambia el estado.
     */
    public synchronized void pauseAll() {
        paused = true;
    }

    /**
     * Lo invoca el hilo controlador despues de pauseAll(): espera (sin espera
     * activa) hasta que TODOS los hilos vivos esten realmente suspendidos.
     * Asi el conteo de primos que se muestra corresponde a un estado estable.
     */
    public synchronized void awaitAllPaused() throws InterruptedException {
        while (pausedWorkers < liveWorkers) {
            wait();
        }
    }

    /**
     * Lo invoca el hilo controlador: desactiva la pausa y despierta a todos
     * los hilos suspendidos. Se usa notifyAll() (no notify()) porque en el
     * mismo monitor esperan varios hilos por condiciones distintas.
     */
    public synchronized void resumeAll() {
        paused = false;
        notifyAll();
    }

    /**
     * Punto de chequeo que invocan los hilos trabajadores en cada iteracion.
     *
     * Camino rapido: si no hay pausa pendiente no se toma el lock, con lo cual
     * el costo por iteracion es una lectura volatile. Si la bandera se activa
     * justo despues de la lectura, el hilo simplemente se detiene en la
     * siguiente iteracion; el controlador lo espera en awaitAllPaused().
     */
    public void checkPause() throws InterruptedException {
        if (!paused) {
            return;
        }
        synchronized (this) {
            // Se vuelve a evaluar la condicion YA dentro del lock.
            if (!paused) {
                return;
            }
            pausedWorkers++;
            notifyAll();            // avisa al controlador que este hilo ya se detuvo
            while (paused) {        // wait() siempre dentro de un while (spurious wakeups)
                wait();
            }
            pausedWorkers--;
        }
    }

    /**
     * Lo invoca un hilo trabajador cuando termina su rango, para que el
     * controlador no se quede esperando por un hilo que ya no existe.
     */
    public synchronized void workerFinished() {
        liveWorkers--;
        notifyAll();
    }

    public synchronized boolean allFinished() {
        return liveWorkers == 0;
    }
}
