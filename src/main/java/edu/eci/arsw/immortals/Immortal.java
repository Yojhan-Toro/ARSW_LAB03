package edu.eci.arsw.immortals;

import edu.eci.arsw.concurrency.PauseController;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

public final class Immortal implements Runnable {
  private final String name;
  private int health;
  private final int damage;
  private final List<Immortal> population;
  private final ScoreBoard scoreBoard;
  private final PauseController controller;
  private volatile boolean running = true;

  private final ReentrantLock lock = new ReentrantLock();

  public Immortal(String name, int health, int damage, List<Immortal> population, ScoreBoard scoreBoard, PauseController controller) {
    this.name = Objects.requireNonNull(name);
    this.health = health;
    this.damage = damage;
    this.population = Objects.requireNonNull(population);
    this.scoreBoard = Objects.requireNonNull(scoreBoard);
    this.controller = Objects.requireNonNull(controller);
  }

  public String name() { return name; }

  public int getHealth() {
    lock.lock();
    try {
      return health;
    } finally {
      lock.unlock();
    }
  }

  public int getHealthSnapshot() {
    return health;
  }

  public ReentrantLock getLock() {
    return lock;
  }

  public boolean isAlive() { return getHealth() > 0 && running; }
  public void stop() { running = false; }

  @Override public void run() {
    try {
      while (running) {

        controller.awaitIfPaused();
        if (!running) break;

        var opponent = pickOpponent();
        if (opponent == null) continue;

        String mode = System.getProperty("fight", "ordered");
        if ("naive".equalsIgnoreCase(mode)) fightNaive(opponent);
        else fightOrdered(opponent);

        Thread.sleep(2);
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  private Immortal pickOpponent() {
    if (population.size() <= 1) return null;
    Immortal other;
    int attempts = 0;
    do {
      other = population.get(ThreadLocalRandom.current().nextInt(population.size()));
      attempts++;

      if (attempts > 100) return null;
    } while (other == this || !other.isAlive());
    return other;
  }

  private void fightNaive(Immortal other) {
    synchronized (this) {
      synchronized (other) {
        performFight(other);
      }
    }
  }

  private void fightOrdered(Immortal other) {

    Immortal first = this.name.compareTo(other.name) < 0 ? this : other;
    Immortal second = this.name.compareTo(other.name) < 0 ? other : this;

    synchronized (first) {
      synchronized (second) {
        performFight(other);
      }
    }
  }


  private void performFight(Immortal other) {

    if (this.health <= 0 || other.health <= 0) return;


    other.health -= this.damage;
    this.health += this.damage / 2;


    scoreBoard.recordFight();
  }
}