package osmo.tester.unittests.generation;

import osmo.tester.OSMOConfiguration;
import osmo.tester.generator.listener.GenerationListener;
import osmo.tester.generator.testsuite.TestCase;
import osmo.tester.generator.testsuite.TestCaseStep;
import osmo.tester.generator.testsuite.TestSuite;
import osmo.tester.model.FSM;
import osmo.tester.model.FSMTransition;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.*;

/** @author Teemu Kanstren */
public class TestDistributionListener implements GenerationListener {
  private Map<String, Integer> steps = new HashMap<>();
  private Map<String, Integer> expected = new HashMap<>();

  public void setExpected(String step, int count) {
    expected.put(step, count);
  }

  @Override
  public void init(long seed, FSM fsm, OSMOConfiguration config) {
  }

  @Override
  public void guard(FSMTransition transition) {
  }

  @Override
  public void stepStarting(TestCaseStep step) {

  }

  @Override
  public void stepDone(TestCaseStep step) {
    String name = step.getName();
    Integer count = steps.get(name);
    if (count == null) {
      count = 0;
    }
    count++;
    steps.put(name, count);
  }

  @Override
  public void lastStep(String name) {
  }

  @Override
  public void pre(FSMTransition transition) {
  }

  @Override
  public void post(FSMTransition transition) {
  }

  @Override
  public void testStarted(TestCase test) {
  }

  @Override
  public void testEnded(TestCase test) {
  }

  @Override
  public void suiteStarted(TestSuite suite) {
  }

  @Override
  public void suiteEnded(TestSuite suite) {
  }

  public void validate(String msg) {
    assertEquals(msg, expected, steps);
  }

  public Map<String, Integer> getSteps() {
    return steps;
  }

  @Override
  public void testError(TestCase test, Throwable error) {
  }
}
