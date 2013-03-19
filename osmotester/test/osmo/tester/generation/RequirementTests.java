package osmo.tester.generation;

import org.junit.Before;
import org.junit.Test;
import osmo.tester.OSMOConfiguration;
import osmo.tester.OSMOTester;
import osmo.tester.generator.endcondition.Length;
import osmo.tester.model.Requirements;
import osmo.tester.testmodels.ValidTestModel2;

import static junit.framework.Assert.*;

/** @author Teemu Kanstren */
public class RequirementTests {
  @Before
  public void setSeed() {
    OSMOConfiguration.setSeed(333);
  }
  
  @Test
  public void fullCoverage() {
    Requirements reqs = new Requirements();
    reqs.add(ValidTestModel2.REQ_EPIX);
    reqs.add(ValidTestModel2.REQ_HELLO);
    reqs.add(ValidTestModel2.REQ_WORLD);
    OSMOTester osmo = new OSMOTester(new ValidTestModel2(reqs));
    osmo.addTestEndCondition(new Length(3));
    osmo.addSuiteEndCondition(new Length(1));
    osmo.generate();
    assertEquals(3, reqs.getUniqueCoverage().size());
    assertEquals(3, reqs.getRequirements().size());
    assertEquals(0, reqs.getExcess().size());
  }

  @Test
  public void excessCoverage() {
    Requirements reqs = new Requirements();
    reqs.add(ValidTestModel2.REQ_HELLO);
    OSMOTester osmo = new OSMOTester(new ValidTestModel2(reqs));
    osmo.addTestEndCondition(new Length(10));
    osmo.addSuiteEndCondition(new Length(1));
    osmo.generate();
    assertEquals(3, reqs.getUniqueCoverage().size());
    assertEquals(1, reqs.getRequirements().size());
    assertEquals(2, reqs.getExcess().size());
  }

  @Test
  public void fullExcessCoverage() {
    Requirements reqs = new Requirements();
    OSMOTester osmo = new OSMOTester(new ValidTestModel2(reqs));
    osmo.addTestEndCondition(new Length(3));
    osmo.addSuiteEndCondition(new Length(1));
    osmo.generate();
    assertEquals(3, reqs.getUniqueCoverage().size());
    assertEquals(0, reqs.getRequirements().size());
    assertEquals(3, reqs.getExcess().size());
  }

  @Test
  public void lackingCoverage() {
    Requirements reqs = new Requirements();
    reqs.add(ValidTestModel2.REQ_EPIX);
    reqs.add(ValidTestModel2.REQ_HELLO);
    reqs.add(ValidTestModel2.REQ_WORLD);
    reqs.add("undefined");
    OSMOTester osmo = new OSMOTester(new ValidTestModel2(reqs));
    osmo.addTestEndCondition(new Length(3));
    osmo.addSuiteEndCondition(new Length(1));
    osmo.generate();
    assertEquals(3, reqs.getUniqueCoverage().size());
    assertEquals(4, reqs.getRequirements().size());
    assertEquals(0, reqs.getExcess().size());
  }
}