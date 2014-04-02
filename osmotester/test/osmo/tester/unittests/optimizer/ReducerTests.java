package osmo.tester.unittests.optimizer;

import org.junit.Test;
import osmo.common.TestUtils;
import osmo.tester.OSMOConfiguration;
import osmo.tester.generator.ReflectiveModelFactory;
import osmo.tester.generator.endcondition.Length;
import osmo.tester.generator.testsuite.TestCase;
import osmo.tester.model.FSMTransition;
import osmo.tester.optimizer.reducer.Analyzer;
import osmo.tester.optimizer.reducer.FuzzerTask;
import osmo.tester.optimizer.reducer.debug.Invariants;
import osmo.tester.optimizer.reducer.Reducer;
import osmo.tester.optimizer.reducer.ReducerConfig;
import osmo.tester.optimizer.reducer.ReducerState;
import osmo.tester.optimizer.reducer.debug.invariants.NumberOfSteps;
import osmo.tester.scenario.Scenario;
import osmo.tester.scenario.Slice;
import osmo.tester.unittests.testmodels.ErrorModelProbability;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author Teemu Kanstren
 */
public class ReducerTests {
  @Test
  public void probableModel() throws Exception {
    TestUtils.recursiveDelete("osmo-output");
    ReducerConfig config = new ReducerConfig(111);
    config.setParallelism(1);
    Reducer reducer = new Reducer(config);
    OSMOConfiguration osmoConfig = reducer.getOsmoConfig();
    osmoConfig.setFactory(new ReflectiveModelFactory(ErrorModelProbability.class));
    osmoConfig.setTestEndCondition(new Length(50));
    osmoConfig.setSuiteEndCondition(new Length(20));
    config.setTotalTime(TimeUnit.SECONDS, 1);
    config.setPopulationSize(50);
    config.setLength(10);
    config.setTestMode(true);
    ReducerState state = reducer.search();
    List<TestCase> tests = state.getTests();
    assertEquals("Number of tests", 1, tests.size());
    TestCase test1 = tests.get(0);
    assertEquals("Final test length", 1, test1.getAllStepNames().size());
    assertEquals("Iteration lengths", "[5, 3, 1]", state.getLengths().toString());
    String report = TestUtils.readFile("osmo-output/reducer-111/reducer-final.txt", "UTF8");
    String expected = TestUtils.getResource(ReducerTests.class, "expected-reducer.txt");
    report = TestUtils.unifyLineSeparators(report, "\n");
    expected = TestUtils.unifyLineSeparators(expected, "\n");
    assertEquals("Reducer report", expected, report);
    List<String> files = TestUtils.listFiles("osmo-output/reducer-111", ".html", false);
    assertEquals("Generated report files", "[final-tests.html]", files.toString());
  }
  
  @Test
  public void scenarioBuilding() {
    ReducerState state = new ReducerState(null, new ReducerConfig(111));
    FuzzerTask task = new FuzzerTask(null, 0, state);
    TestCase test = new TestCase(0);
    test.addStep(new FSMTransition("hello1"));
    test.addStep(new FSMTransition("hello2"));
    test.addStep(new FSMTransition("hello2"));
    Scenario scenario = task.createScenario(test);
    List<Slice> slices = scenario.getSlices();
    assertEquals("Number of slices", 2, slices.size());
    Slice slice1 = null;
    Slice slice2 = null;
    for (Slice slice : slices) {
      if (slice.getStepName().equals("hello1")) slice1 = slice;
      if (slice.getStepName().equals("hello2")) slice2 = slice;
    }
    assertEquals("Slice 1 name", "hello1", slice1.getStepName());
    assertEquals("Slice 1 min", 0, slice1.getMin());
    assertEquals("Slice 1 max", 1, slice1.getMax());
    assertEquals("Slice 2 name", "hello2", slice2.getStepName());
    assertEquals("Slice 2 min", 0, slice2.getMin());
    assertEquals("Slice 2 max", 2, slice2.getMax());
  }

  @Test
  public void metrics() {
    TestCase test22 = createTest22();
    NumberOfSteps metrics = new NumberOfSteps(test22);
    Map<String, Integer> counts = metrics.getStepCounts();
    assertEquals("Number of steps", 10, counts.size());
    assertEquals("'Unlock PIN bad' count", 10, (int)counts.get("Unlock PIN bad"));
    assertEquals("'Select EF LP' count", 2, (int)counts.get("Select EF LP"));
    assertEquals("'Select DF Roaming' count", 1, (int)counts.get("Select DF Roaming"));
    assertEquals("'Read Binary' count", 3, (int)counts.get("Read Binary"));
    assertEquals("'Select EF FR' count", 1, (int)counts.get("Select EF FR"));
    assertEquals("'Enable PIN 11' count", 1, (int)counts.get("Enable PIN 11"));
    assertEquals("'Select MF' count", 1, (int)counts.get("Select MF"));
    assertEquals("'Change new PIN' count", 1, (int)counts.get("Change new PIN"));
    assertEquals("'Select DF GSM' count", 1, (int)counts.get("Select DF GSM"));
    assertEquals("'Select EF IMSI' count", 1, (int)counts.get("Select EF IMSI"));
  }

  @Test
  public void invariants() {
    TestCase test14_1 = createTest14_1();
    TestCase test14_2 = createTest14_2();
    TestCase test14_3 = createTest14_3();
    TestCase test14_4 = createTest14_4();
    TestCase test22 = createTest22();
    TestCase test27 = createTest27();
    TestCase test30 = createTest30();
    TestCase test39 = createTest39();
    ReducerState state = new ReducerState(null, new ReducerConfig(111));
    Analyzer analyzer = new Analyzer(createStepList(), state);
    Invariants invariants = analyzer.analyze(test14_1, test14_2, test14_3, test14_4, test22, test27, test30, test39);
    assertEquals("Step counts", "[Unlock PIN bad : 10-11, Select EF LP : 0-5, Select DF Roaming : 0-1, Select EF FR : 0-4, Enable PIN 11 : 0-2, Select MF : 0-1, Change new PIN : 0-2, Select DF GSM : 1-3, Select EF IMSI : 1-3, Read Binary : 1-4, Verify PIN 11 : 0-2, Verify PIN 12 : 0-3, Disable PIN OK : 0-4, Change same PIN : 0-2]", invariants.getUsedStepCounts().toString());
    assertEquals("Missing steps", "[Miss me]", invariants.getMissingSteps().toString());
    assertEquals("Last steps", "[Read Binary]", invariants.getLastSteps().toString());
    assertEquals("Precedences", "[Select DF GSM->Select EF IMSI, Unlock PIN bad->Select EF IMSI]", invariants.getPrecedencePatterns().toString());
    assertEquals("Sequences", "[[Read Binary], [Select DF GSM], [Select EF IMSI], [Unlock PIN bad, Unlock PIN bad]]", invariants.getSequencePatterns().toString());
  }

  @Test
  public void report() {
    TestCase test14_1 = createTest14_1();
    TestCase test14_2 = createTest14_2();
    TestCase test14_3 = createTest14_3();
    TestCase test14_4 = createTest14_4();
    TestCase test22 = createTest22();
    TestCase test27 = createTest27();
    TestCase test30 = createTest30();
    TestCase test39 = createTest39();
    ReducerConfig config = new ReducerConfig(111);
    ReducerState state = new ReducerState(createStepList(), config);
    state.addTest(test39);
    state.testsDone(50);
    state.addTest(test30);
    state.testsDone(50);
    state.addTest(test27);
    state.testsDone(50);
    state.addTest(test22);
    state.testsDone(50);
    state.addTest(test14_4);
    state.testsDone(50);
    state.addTest(test14_3);
    state.testsDone(50);
    state.addTest(test14_2);
    state.testsDone(50);
    state.addTest(test14_1);
    state.testsDone(50);
    Analyzer analyzer = new Analyzer(createStepList(), state);
    analyzer.analyze();
    String report = analyzer.createReport();
    String expected = TestUtils.getResource(ReducerTests.class, "expected-reducer2.txt");
    report = TestUtils.unifyLineSeparators(report, "\n");
    expected = TestUtils.unifyLineSeparators(expected, "\n");
    assertEquals("Reducer report", expected, report);
  }
  
  private TestCase createTest14_1() {
    TestCase test = new TestCase(1);
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Disable PIN OK"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Select DF GSM"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Select EF IMSI"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Read Binary"));
    return test;
  }

  private TestCase createTest14_2() {
    TestCase test = new TestCase(1);
    test.addStep(new FSMTransition("Disable PIN OK"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Select DF GSM"));
    test.addStep(new FSMTransition("Select EF IMSI"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Read Binary"));
    return test;
  }

  private TestCase createTest14_3() {
    TestCase test = new TestCase(1);
    test.addStep(new FSMTransition("Select DF GSM"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Disable PIN OK"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Select EF IMSI"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Read Binary"));
    return test;
  }

  private TestCase createTest14_4() {
    TestCase test = new TestCase(1);
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Disable PIN OK"));
    test.addStep(new FSMTransition("Select DF GSM"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Select EF IMSI"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Unlock PIN bad"));
    test.addStep(new FSMTransition("Read Binary"));
    return test;
  }

  private TestCase createTest22() {
    TestCase test = new TestCase(1);
    test.addStep(new FSMTransition("Unlock PIN bad"));//1 //1
    test.addStep(new FSMTransition("Unlock PIN bad"));    //2
    test.addStep(new FSMTransition("Unlock PIN bad"));    //3
    test.addStep(new FSMTransition("Select EF LP")); //2
    test.addStep(new FSMTransition("Select EF LP"));
    test.addStep(new FSMTransition("Select DF Roaming")); //3
    test.addStep(new FSMTransition("Read Binary")); //4
    test.addStep(new FSMTransition("Select EF FR")); //5
    test.addStep(new FSMTransition("Unlock PIN bad"));    //4
    test.addStep(new FSMTransition("Unlock PIN bad"));    //5
    test.addStep(new FSMTransition("Unlock PIN bad"));    //6
    test.addStep(new FSMTransition("Unlock PIN bad"));    //7
    test.addStep(new FSMTransition("Enable PIN 11")); //6
    test.addStep(new FSMTransition("Select MF")); //7
    test.addStep(new FSMTransition("Unlock PIN bad"));    //8
    test.addStep(new FSMTransition("Change new PIN")); //8
    test.addStep(new FSMTransition("Select DF GSM")); //9
    test.addStep(new FSMTransition("Select EF IMSI"));//10
    test.addStep(new FSMTransition("Read Binary"));
    test.addStep(new FSMTransition("Unlock PIN bad"));   //9
    test.addStep(new FSMTransition("Unlock PIN bad")); //10
    test.addStep(new FSMTransition("Read Binary"));
    return test;
  }

  private TestCase createTest27() {
    TestCase test = new TestCase(1);
    test.addStep(new FSMTransition("Select DF GSM"));
    test.addStep(new FSMTransition("Verify PIN 12"));
    test.addStep(new FSMTransition("Change same PIN"));
    test.addStep(new FSMTransition("Read Binary"));
    test.addStep(new FSMTransition("Verify PIN 12"));
    test.addStep(new FSMTransition("Unlock PIN bad")); //1
    test.addStep(new FSMTransition("Select EF IMSI"));
    test.addStep(new FSMTransition("Select EF FR"));
    test.addStep(new FSMTransition("Verify PIN 12"));
    test.addStep(new FSMTransition("Select EF IMSI"));
    test.addStep(new FSMTransition("Verify PIN 11"));
    test.addStep(new FSMTransition("Unlock PIN bad")); //2
    test.addStep(new FSMTransition("Unlock PIN bad")); //3
    test.addStep(new FSMTransition("Unlock PIN bad")); //4
    test.addStep(new FSMTransition("Verify PIN 11"));
    test.addStep(new FSMTransition("Select EF LP"));
    test.addStep(new FSMTransition("Unlock PIN bad")); //5
    test.addStep(new FSMTransition("Unlock PIN bad")); //6
    test.addStep(new FSMTransition("Unlock PIN bad")); //7
    test.addStep(new FSMTransition("Select EF IMSI"));
    test.addStep(new FSMTransition("Disable PIN OK"));
    test.addStep(new FSMTransition("Enable PIN 11"));
    test.addStep(new FSMTransition("Unlock PIN bad")); //8
    test.addStep(new FSMTransition("Unlock PIN bad")); //9
    test.addStep(new FSMTransition("Change same PIN"));
    test.addStep(new FSMTransition("Unlock PIN bad")); //9
    test.addStep(new FSMTransition("Read Binary"));
    return test;
  }

  private TestCase createTest30() {
    TestCase test = new TestCase(1);
    test.addStep(new FSMTransition("Select DF GSM"));
    test.addStep(new FSMTransition("Unlock PIN bad")); //1
    test.addStep(new FSMTransition("Unlock PIN bad")); //2
    test.addStep(new FSMTransition("Enable PIN 11"));
    test.addStep(new FSMTransition("Disable PIN OK"));
    test.addStep(new FSMTransition("Verify PIN 11"));
    test.addStep(new FSMTransition("Unlock PIN bad")); //3
    test.addStep(new FSMTransition("Enable PIN 11"));
    test.addStep(new FSMTransition("Disable PIN OK"));
    test.addStep(new FSMTransition("Select MF"));
    test.addStep(new FSMTransition("Unlock PIN bad")); //4
    test.addStep(new FSMTransition("Select EF FR"));
    test.addStep(new FSMTransition("Unlock PIN bad")); //5
    test.addStep(new FSMTransition("Unlock PIN bad")); //6
    test.addStep(new FSMTransition("Unlock PIN bad")); //7
    test.addStep(new FSMTransition("Select DF GSM"));
    test.addStep(new FSMTransition("Unlock PIN bad")); //8
    test.addStep(new FSMTransition("Select DF Roaming"));
    test.addStep(new FSMTransition("Select EF FR"));
    test.addStep(new FSMTransition("Select DF GSM"));
    test.addStep(new FSMTransition("Unlock PIN bad")); //9
    test.addStep(new FSMTransition("Unlock PIN bad")); //10
    test.addStep(new FSMTransition("Verify PIN 11"));
    test.addStep(new FSMTransition("Change same PIN"));
    test.addStep(new FSMTransition("Select EF LP"));
    test.addStep(new FSMTransition("Select EF LP"));
    test.addStep(new FSMTransition("Select EF FR"));
    test.addStep(new FSMTransition("Read Binary"));
    test.addStep(new FSMTransition("Select EF IMSI"));
    test.addStep(new FSMTransition("Read Binary"));
    return test;
  }

  private TestCase createTest39() {
    TestCase test = new TestCase(1);
    test.addStep(new FSMTransition("Unlock PIN bad")); //1
    test.addStep(new FSMTransition("Disable PIN OK"));
    test.addStep(new FSMTransition("Read Binary"));
    test.addStep(new FSMTransition("Unlock PIN bad")); //2
    test.addStep(new FSMTransition("Select DF GSM"));
    test.addStep(new FSMTransition("Read Binary"));
    test.addStep(new FSMTransition("Disable PIN OK"));
    test.addStep(new FSMTransition("Unlock PIN bad")); //3
    test.addStep(new FSMTransition("Unlock PIN bad")); //4
    test.addStep(new FSMTransition("Change new PIN"));
    test.addStep(new FSMTransition("Read Binary"));
    test.addStep(new FSMTransition("Unlock PIN bad"));  //5
    test.addStep(new FSMTransition("Select EF FR"));
    test.addStep(new FSMTransition("Unlock PIN bad"));  //6
    test.addStep(new FSMTransition("Select EF LP"));
    test.addStep(new FSMTransition("Verify PIN 12"));
    test.addStep(new FSMTransition("Verify PIN 11"));
    test.addStep(new FSMTransition("Change same PIN"));
    test.addStep(new FSMTransition("Select EF LP"));
    test.addStep(new FSMTransition("Select MF"));
    test.addStep(new FSMTransition("Select EF LP"));
    test.addStep(new FSMTransition("Disable PIN OK"));
    test.addStep(new FSMTransition("Select EF FR"));
    test.addStep(new FSMTransition("Select DF Roaming"));
    test.addStep(new FSMTransition("Select EF LP"));
    test.addStep(new FSMTransition("Unlock PIN bad"));  //7
    test.addStep(new FSMTransition("Select EF FR"));
    test.addStep(new FSMTransition("Change new PIN"));
    test.addStep(new FSMTransition("Unlock PIN bad"));  //8
    test.addStep(new FSMTransition("Verify PIN 12"));
    test.addStep(new FSMTransition("Unlock PIN bad")); //9
    test.addStep(new FSMTransition("Unlock PIN bad")); //10
    test.addStep(new FSMTransition("Select DF GSM"));
    test.addStep(new FSMTransition("Select EF LP"));
    test.addStep(new FSMTransition("Unlock PIN bad")); //11
    test.addStep(new FSMTransition("Disable PIN OK"));
    test.addStep(new FSMTransition("Select EF IMSI"));
    test.addStep(new FSMTransition("Select EF FR"));
    test.addStep(new FSMTransition("Read Binary"));
    return test;
  }

  private List<String> createStepList() {
    List<String> steps = new ArrayList<>();
    steps.add("Unlock PIN bad");
    steps.add("Select EF LP");
    steps.add("Select DF Roaming");
    steps.add("Select EF FR");
    steps.add("Enable PIN 11");
    steps.add("Select MF");
    steps.add("Change new PIN");
    steps.add("Select DF GSM");
    steps.add("Select EF IMSI");
    steps.add("Read Binary");
    steps.add("Verify PIN 11");
    steps.add("Verify PIN 12");
    steps.add("Disable PIN OK");
    steps.add("Change same PIN");
    steps.add("Miss me");
    return steps;
  }
}
