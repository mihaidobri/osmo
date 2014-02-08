package osmo.tester.gui.jfx.configurationtab;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import osmo.tester.OSMOTester;
import osmo.tester.gui.jfx.GUIState;
import osmo.tester.gui.jfx.configurationtab.generator.Exploration;
import osmo.tester.gui.jfx.configurationtab.generator.GeneratorDescription;
import osmo.tester.gui.jfx.configurationtab.generator.Greedy;
import osmo.tester.gui.jfx.configurationtab.generator.GreedyParameters;
import osmo.tester.gui.jfx.configurationtab.generator.MultiCore;
import osmo.tester.gui.jfx.configurationtab.generator.MultiGreedy;
import osmo.tester.gui.jfx.configurationtab.generator.Requirements;
import osmo.tester.gui.jfx.configurationtab.generator.SingleCore;
import osmo.tester.optimizer.greedy.GreedyOptimizer;

/**
 * @author Teemu Kanstren
 */
public class GeneratorPane extends VBox {
  private final GUIState state;
  private Node old = null;
  private ComboBox<GeneratorDescription> generatorCombo;
  private GeneratorDescription generator = null;

  public GeneratorPane(GUIState state) {
    super(10);
    this.state = state;
    createLabelPane();
  }

  private void createLabelPane() {
    HBox hbox = new HBox();
    hbox.setSpacing(10);
    hbox.setAlignment(Pos.CENTER_LEFT);
    getChildren().add(hbox);
    ObservableList<Node> kids = hbox.getChildren();
    kids.add(new Label("Generator Settings"));
    generatorCombo = new ComboBox<>();
    generatorCombo.setOnAction((event) -> setGenerator(generatorCombo.getValue()));
    ObservableList<GeneratorDescription> items = generatorCombo.getItems();
    SingleCore singleCore = new SingleCore();
    items.addAll(singleCore, new MultiCore(), new Greedy(state), new MultiGreedy(state), new Exploration(), new Requirements());
    generatorCombo.setValue(singleCore);
    kids.add(generatorCombo);
    Button button = new Button("Generate");
    button.setOnAction((event) -> startGenerator());
    kids.add(button);
  }

  private void setGenerator(GeneratorDescription generator) {
    this.generator = generator;
    if (old != null) getChildren().remove(old);
    old = generator.createPane();
    getChildren().add(old);
  }

  private void startGenerator() {
    GeneratorDescription choice = generatorCombo.getValue();
    if (choice instanceof SingleCore) {
      startSingleCore();
    }
    if (choice instanceof MultiCore) {
      
    }
    if (choice instanceof Greedy) {
      startGreedy();
    }
    if (choice instanceof MultiGreedy) {

    }
    if (choice instanceof Exploration) {

    }
    if (choice instanceof Requirements) {

    }
  }

  private void startSingleCore() {
    state.openSingleCoreExecution();
    OSMOTester tester = new OSMOTester();
    tester.setConfig(state.getOsmoConfig());
    long seed = state.getSeed();
    tester.generate(seed);
  }
  
  private void startGreedy() {
    Greedy greedyDesc = (Greedy) generator;
    greedyDesc.storeParameters();
    state.openGreedyExecution();
    GreedyOptimizer greedy = new GreedyOptimizer(state.getOsmoConfig(), state.getScoreConfig());
    greedy.addIterationListener(state.getGreedyParameters().getListener());
    GreedyParameters gp = state.getGreedyParameters();
    greedy.setThreshold(gp.getThreshold());
    greedy.setTimeout(gp.getTimeoutInSeconds());
    greedy.setMax(gp.getMaxTests());
    greedy.search(gp.getPopulation(), state.getSeed());
  }
}
