package osmo.tester.unittests.testmodels;

import osmo.tester.annotation.AfterTest;
import osmo.tester.annotation.BeforeSuite;
import osmo.tester.annotation.BeforeTest;
import osmo.tester.annotation.CoverageValue;
import osmo.tester.annotation.EndCondition;
import osmo.tester.annotation.ExplorationEnabler;
import osmo.tester.annotation.GenerationEnabler;
import osmo.tester.annotation.Guard;
import osmo.tester.annotation.Post;
import osmo.tester.annotation.Pre;
import osmo.tester.annotation.TestStep;
import osmo.tester.generator.testsuite.TestCaseStep;
import osmo.tester.generator.testsuite.TestSuite;
import osmo.tester.model.Requirements;

import java.io.PrintStream;

/** @author Teemu Kanstren */
public class PartialModel1 {
  private final Requirements req;
  private TestSuite history;
  public static final String REQ_HELLO = "hello";
  public static final String REQ_WORLD = "world";
  public static final String REQ_EPIX = "epix";
  private PrintStream out;

  public PartialModel1(Requirements req) {
    this(req, null);
  }

  public PartialModel1(Requirements req, PrintStream out) {
    this.req = req;
    this.out = out;
  }

  public TestSuite getHistory() {
    return history;
  }

  @BeforeSuite
  public void beforeAll() {
    if (out == null) {
      out = System.out;
    }
  }

  @BeforeTest
  public void reset() {
    req.clearCoverage();
  }

  @AfterTest
  public void end1() {
  }

  @Guard
  public boolean checkHello() {
    return !req.isCovered(REQ_HELLO) && !req.isCovered(REQ_WORLD) && !req.isCovered(REQ_EPIX);
  }

  @TestStep
  public void hello() {
    req.covered(REQ_HELLO);
    out.print(":hello");
  }

  @Guard("world")
  public boolean excessCheck() {
    return true;
  }

  @Guard({"epixx", "world"})
  public boolean gaagaa() {
    return true;
  }

  @Guard("epixx")
  public boolean kitted() {
    return req.isCovered(REQ_WORLD);
  }

  @Post("epixx")
  public void epixxO() {
    out.print(":epixx_oracle");
  }

  @Post("all")
  public void stateCheck() {
    out.print(":gen_oracle");
  }

  @EndCondition
  public boolean ec1() {
    return false;
  }

  @ExplorationEnabler
  public void enabler1() {
  }

  @GenerationEnabler
  public void enabler2() {
  }
  
  @CoverageValue
  public String state1(TestCaseStep step) {
    return step.getName()+"-hello1";
  }

  @Pre("all")
  public void savePreState() {
  }

  @Post("all")
  public void savePostState() {
  }
}
