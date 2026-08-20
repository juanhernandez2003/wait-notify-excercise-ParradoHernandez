package edu.eci.arsw.primefinder;

import java.util.LinkedList;
import java.util.List;

public class PrimeFinderThread extends Thread {

	int a, b;

	private final List<Integer> primes;

	/** Monitor compartido que coordina la pausa/continuacion. */
	private final PauseMonitor monitor;

	public PrimeFinderThread(int a, int b, PauseMonitor monitor) {
		super();
		this.primes = new LinkedList<>();
		this.a = a;
		this.b = b;
		this.monitor = monitor;
	}

	@Override
	public void run() {
		try {
			for (int i = a; i < b; i++) {
				// Punto de chequeo: si hay una pausa pendiente el hilo se
				// suspende aqui con wait(), sin espera activa.
				monitor.checkPause();

				if (isPrime(i)) {
					addPrime(i);
					System.out.println(i);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			// Se avisa al monitor para que el controlador no espere por un
			// hilo que ya termino su rango.
			monitor.workerFinished();
		}
	}

	boolean isPrime(int n) {
		boolean ans;
		if (n > 2) {
			ans = n % 2 != 0;
			for (int i = 3; ans && i * i <= n; i += 2) {
				ans = n % i != 0;
			}
		} else {
			ans = n == 2;
		}
		return ans;
	}

	private synchronized void addPrime(int n) {
		primes.add(n);
	}

	public synchronized List<Integer> getPrimes() {
		return primes;
	}

	/** Numero de primos hallados hasta el momento por este hilo. */
	public synchronized int getPrimeCount() {
		return primes.size();
	}

}
