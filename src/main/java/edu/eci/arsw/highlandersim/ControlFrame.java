package edu.eci.arsw.highlandersim;

import edu.eci.arsw.immortals.Immortal;
import edu.eci.arsw.immortals.ImmortalManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public final class ControlFrame extends JFrame {

  private ImmortalManager manager;
  private final JTextArea output = new JTextArea(16, 50);
  private final JButton startBtn = new JButton("Start");
  private final JButton pauseAndCheckBtn = new JButton("Pause & Check");
  private final JButton resumeBtn = new JButton("Resume");
  private final JButton stopBtn = new JButton("Stop");

  private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(8, 2, 5000, 1));
  private final JSpinner healthSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 10000, 10));
  private final JSpinner damageSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
  private final JComboBox<String> fightMode = new JComboBox<>(new String[]{"ordered", "naive"});

  public ControlFrame(int count, String fight) {
    setTitle("Highlander Simulator – ARSW");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout(8,8));

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(new JLabel("Count:"));
    countSpinner.setValue(count);
    top.add(countSpinner);
    top.add(new JLabel("Health:"));
    top.add(healthSpinner);
    top.add(new JLabel("Damage:"));
    top.add(damageSpinner);
    top.add(new JLabel("Fight:"));
    fightMode.setSelectedItem(fight);
    top.add(fightMode);
    add(top, BorderLayout.NORTH);

    output.setEditable(false);
    output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    add(new JScrollPane(output), BorderLayout.CENTER);

    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bottom.add(startBtn);
    bottom.add(pauseAndCheckBtn);
    bottom.add(resumeBtn);
    bottom.add(stopBtn);
    add(bottom, BorderLayout.SOUTH);

    startBtn.addActionListener(this::onStart);
    pauseAndCheckBtn.addActionListener(this::onPauseAndCheck);
    resumeBtn.addActionListener(this::onResume);
    stopBtn.addActionListener(this::onStop);

    pack();
    setLocationByPlatform(true);
    setVisible(true);
  }

  private void onStart(ActionEvent e) {
    safeStop();
    int n = (Integer) countSpinner.getValue();
    int health = (Integer) healthSpinner.getValue();
    int damage = (Integer) damageSpinner.getValue();
    String fight = (String) fightMode.getSelectedItem();
    manager = new ImmortalManager(n, fight, health, damage);
    manager.start();
    output.setText("Simulation started with %d immortals (health=%d, damage=%d, fight=%s)%n"
            .formatted(n, health, damage, fight));
  }

  private void onPauseAndCheck(ActionEvent e) {
    if (manager == null) {
      output.setText("No simulation running. Press START first.\n");
      return;
    }

    manager.pause();

    List<ImmortalManager.ImmortalSnapshot> snapshots = manager.getAtomicSnapshot();

    long totalHealth = 0;
    int aliveCount = 0;

    StringBuilder sb = new StringBuilder();
    sb.append("\n───────────────────────────────────────────────\n");

    for (ImmortalManager.ImmortalSnapshot snapshot : snapshots) {
      int h = snapshot.health();
      totalHealth += h;
      if (snapshot.alive()) aliveCount++;

      String status = snapshot.alive() ? "ALIVE" : "DEAD ";
      sb.append(String.format("%-14s : %5d  [%s]%n", snapshot.name(), h, status));
    }

    sb.append("\n───────────────────────────────────────────────\n");
    sb.append(String.format("Total Health:        %d%n", totalHealth));
    sb.append(String.format("Alive Count:         %d / %d%n", aliveCount, snapshots.size()));
    sb.append(String.format("Total Fights:        %d%n", manager.scoreBoard().totalFights()));
    sb.append("───────────────────────────────────────────────\n");



    output.setText(sb.toString());
  }

  private void onResume(ActionEvent e) {
    if (manager == null) {
      return;
    }
    manager.resume();
  }

  private void onStop(ActionEvent e) {
    safeStop();
  }

  private void safeStop() {
    if (manager != null) {
      manager.stop();
      manager = null;
    }
  }

  public static void main(String[] args) {
    int count = Integer.getInteger("count", 8);
    String fight = System.getProperty("fight", "ordered");
    SwingUtilities.invokeLater(() -> new ControlFrame(count, fight));
  }
}