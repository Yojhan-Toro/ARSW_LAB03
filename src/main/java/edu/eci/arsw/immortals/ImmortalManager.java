package edu.eci.arsw.immortals;

import edu.eci.arsw.concurrency.PauseController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class ImmortalManager implements AutoCloseable {
  private final List<Immortal> population = new CopyOnWriteArrayList<>();
  private final List<Future<?>> futures = new ArrayList<>();
  private final PauseController controller = new PauseController();
  private final ScoreBoard scoreBoard = new ScoreBoard();
  private ExecutorService exec;

  private final String fightMode;
  private final int initialHealth;
  private final int damage;
  private final int initialPopulation;

  public ImmortalManager(int n, String fightMode) {
    this(n, fightMode, Integer.getInteger("health", 100), Integer.getInteger("damage", 10));
  }

  public ImmortalManager(int n, String fightMode, int initialHealth, int damage) {
    this.fightMode = fightMode;
    this.initialHealth = initialHealth;
    this.damage = damage;
    this.initialPopulation = n;

    for (int i=0; i<n; i++) {
      population.add(new Immortal("Immortal-"+i, initialHealth, damage, population, scoreBoard, controller));
    }
  }

  public synchronized void start() {
    if (exec != null) stop();
    exec = Executors.newVirtualThreadPerTaskExecutor();
    futures.clear();
    for (Immortal im : population) {
      futures.add(exec.submit(im));
    }
  }

  public void pause() {
    controller.pause();
    try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
  }

  public void resume() { controller.resume(); }

  public void stop() {

    for (Immortal im : population) im.stop();


    controller.resume();


    if (exec != null) {
      exec.shutdown();
      try {

        if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
          exec.shutdownNow();

          if (!exec.awaitTermination(2, TimeUnit.SECONDS)) {
            System.err.println("Executor no terminó correctamente");
          }
        }
      } catch (InterruptedException e) {
        exec.shutdownNow();
        Thread.currentThread().interrupt();
      }
      exec = null;
    }


    for (Future<?> f : futures) {
      f.cancel(true);
    }
    futures.clear();
  }

  public int aliveCount() {
    int c = 0;
    for (Immortal im : population) if (im.isAlive()) c++;
    return c;
  }


  public long totalHealth() {
    long sum = 0;
    for (Immortal im : population) {
      sum += im.getHealth();
    }
    return sum;
  }


  public List<ImmortalSnapshot> getAtomicSnapshot() {

    List<Immortal> sorted = new ArrayList<>(population);
    sorted.sort((a, b) -> a.name().compareTo(b.name()));

    List<ImmortalSnapshot> snapshots = new ArrayList<>();


    for (Immortal im : sorted) {
      im.getLock().lock();
    }

    try {

      for (Immortal im : population) {
        snapshots.add(new ImmortalSnapshot(im.name(), im.getHealthSnapshot(), im.isAlive()));
      }
    } finally {
      for (int i = sorted.size() - 1; i >= 0; i--) {
        sorted.get(i).getLock().unlock();
      }
    }

    return snapshots;
  }

  public List<Immortal> populationSnapshot() {
    return Collections.unmodifiableList(new ArrayList<>(population));
  }

  public ScoreBoard scoreBoard() { return scoreBoard; }
  public PauseController controller() { return controller; }
  public int getInitialPopulation() { return initialPopulation; }
  public int getInitialHealth() { return initialHealth; }
  public int getDamage() { return damage; }

  @Override public void close() { stop(); }

  public static class ImmortalSnapshot {
    private final String name;
    private final int health;
    private final boolean alive;

    public ImmortalSnapshot(String name, int health, boolean alive) {
      this.name = name;
      this.health = health;
      this.alive = alive;
    }

    public String name() { return name; }
    public int health() { return health; }
    public boolean alive() { return alive; }
  }
}