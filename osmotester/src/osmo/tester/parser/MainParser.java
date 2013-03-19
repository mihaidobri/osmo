package osmo.tester.parser;

import osmo.common.log.Logger;
import osmo.tester.annotation.AfterSuite;
import osmo.tester.annotation.AfterTest;
import osmo.tester.annotation.BeforeSuite;
import osmo.tester.annotation.BeforeTest;
import osmo.tester.annotation.EndCondition;
import osmo.tester.annotation.ExplorationEnabler;
import osmo.tester.annotation.GenerationEnabler;
import osmo.tester.annotation.Guard;
import osmo.tester.annotation.LastStep;
import osmo.tester.annotation.Post;
import osmo.tester.annotation.Pre;
import osmo.tester.annotation.RequirementsField;
import osmo.tester.annotation.StateName;
import osmo.tester.annotation.TestStep;
import osmo.tester.annotation.TestSuiteField;
import osmo.tester.annotation.Transition;
import osmo.tester.annotation.Variable;
import osmo.tester.generator.testsuite.TestSuite;
import osmo.tester.model.FSM;
import osmo.tester.model.ModelFactory;
import osmo.tester.model.data.SearchableInput;
import osmo.tester.parser.annotation.AfterSuiteParser;
import osmo.tester.parser.annotation.AfterTestParser;
import osmo.tester.parser.annotation.BeforeSuiteParser;
import osmo.tester.parser.annotation.BeforeTestParser;
import osmo.tester.parser.annotation.EndConditionParser;
import osmo.tester.parser.annotation.ExplorationEnablerParser;
import osmo.tester.parser.annotation.GenerationEnablerParser;
import osmo.tester.parser.annotation.GuardParser;
import osmo.tester.parser.annotation.LastStepParser;
import osmo.tester.parser.annotation.PostParser;
import osmo.tester.parser.annotation.PreParser;
import osmo.tester.parser.annotation.SearchableInputParser;
import osmo.tester.parser.annotation.StateNameParser;
import osmo.tester.parser.annotation.TestSuiteFieldParser;
import osmo.tester.parser.annotation.TransitionParser;
import osmo.tester.parser.annotation.VariableParser;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The main parser that takes the given model object and parses it for specific registered annotations,
 * passes these to specific {@link AnnotationParser} implementations to update the {@link osmo.tester.model.FSM} representation
 * according to the information for the specific annotation.
 *
 * @author Teemu Kanstren
 */
public class MainParser {
  private static Logger log = new Logger(MainParser.class);
  /** Key = Annotation type, Value = The parser object for that annotation. */
  private final Map<Class<? extends Annotation>, AnnotationParser> annotationParsers = new HashMap<>();
  /** Key = Annotation type, Value = The parser object for that annotation. */
  private final Map<Class, AnnotationParser> fieldParsers = new HashMap<>();

  public MainParser() {
    //we set up the parser objects for the different annotation types
    annotationParsers.put(Transition.class, new TransitionParser());
    annotationParsers.put(TestStep.class, new TransitionParser());
    annotationParsers.put(LastStep.class, new LastStepParser());
    annotationParsers.put(Guard.class, new GuardParser());
    annotationParsers.put(AfterTest.class, new AfterTestParser());
    annotationParsers.put(BeforeTest.class, new BeforeTestParser());
    annotationParsers.put(AfterSuite.class, new AfterSuiteParser());
    annotationParsers.put(BeforeSuite.class, new BeforeSuiteParser());
    annotationParsers.put(TestSuiteField.class, new TestSuiteFieldParser());
    annotationParsers.put(RequirementsField.class, new osmo.tester.parser.annotation.RequirementsFieldParser());
    annotationParsers.put(Pre.class, new PreParser());
    annotationParsers.put(Post.class, new PostParser());
    annotationParsers.put(EndCondition.class, new EndConditionParser());
    annotationParsers.put(StateName.class, new StateNameParser());
    annotationParsers.put(Variable.class, new VariableParser());
    annotationParsers.put(ExplorationEnabler.class, new ExplorationEnablerParser());
    annotationParsers.put(GenerationEnabler.class, new GenerationEnablerParser());

    fieldParsers.put(SearchableInput.class, new SearchableInputParser());
  }

  /**
   * Initiates parsing the given model object for the annotations that define the finite state machine (FSM) aspects
   * of the test model.
   *
   * @param factory Factory to create the model objects to be parsed.
   * @return The FSM object created from the given model object that can be used for test generation.
   */
  public ParserResult parse(ModelFactory factory, TestSuite suite) {
    log.debug("parsing");
    FSM fsm = new FSM();
    ParserResult result = new ParserResult(fsm);
    ParserParameters parameters = new ParserParameters();
    suite.init();
    parameters.setSuite(suite);
    String errors = "";
    Collection<ModelObject> modelObjects = factory.createModelObjects();
    for (ModelObject mo : modelObjects) {
      String prefix = mo.getPrefix();
      parameters.setPrefix(prefix);
      Object obj = mo.getObject();
      parameters.setModel(obj);
      //first we check any annotated fields that are relevant
      errors += parseFields(result, parameters);
      //next we check any annotated methods that are relevant
      errors += parseMethods(result, parameters);
    }
    //finally we check that the generated FSM itself is valid
    fsm.checkFSM(errors);
    return result;
  }

  /**
   * Parse the relevant annotated fields and pass these to correct {@link AnnotationParser} objects.
   *
   * @param result The parse results will be provided here.
   * @return A string listing all found errors.
   */
  private String parseFields(ParserResult result, ParserParameters parameters) {
    Object obj = parameters.getModel();
    //first we find all declared fields of any scope and type (private, protected, ...)
    Collection<Field> fields = getAllFields(obj.getClass());
    log.debug("fields " + fields.size());
    String errors = "";
    //now we loop through all fields defined in the model object
    for (Field field : fields) {
      log.debug("field:" + field);
      //set the field to be accessible from the parser objects
      parameters.setField(field);
      Annotation[] annotations = field.getAnnotations();
      parameters.setFieldAnnotations(annotations);
      //loop through all defined annotations for each field
      for (Annotation annotation : annotations) {
        Class<? extends Annotation> annotationClass = annotation.annotationType();
        log.debug("class:" + annotationClass);
        AnnotationParser parser = annotationParsers.get(annotationClass);
        if (parser == null) {
          //unsupported annotation (e.g. for some completely different aspect)
          continue;
        }
        log.debug("parser:" + parser);
        //set the annotation itself as a parameter to the used parser object
        parameters.setAnnotation(annotation);
        //and finally parse it
        errors += parser.parse(result, parameters);
      }
      errors = parseField(field, result, parameters, errors);
    }
    return errors;
  }

  private String parseField(Field field, ParserResult result, ParserParameters parameters, String errors) {
    log.debug("parsefield");
    Class fieldClass = field.getType();
    for (Class parserType : fieldParsers.keySet()) {
      if (parserType.isAssignableFrom(fieldClass)) {
        AnnotationParser fieldParser = fieldParsers.get(parserType);
        if (fieldParser != null) {
          log.debug("field parser invocation:" + parameters);
          errors += fieldParser.parse(result, parameters);
        }
      }
    }
    return errors;
  }

  public static Collection<Field> getAllFields(Class clazz) {
    Class<?> superclass = clazz.getSuperclass();
    Collection<Field> fields = new ArrayList<>();
    if (superclass != null) {
      fields.addAll(getAllFields(superclass));
    }
    Collections.addAll(fields, clazz.getDeclaredFields());
    return fields;
  }

  /**
   * Parse the relevant annotated methods and pass these to correct {@link AnnotationParser} objects.
   *
   * @param result This is where the parsing results are given.
   * @return String representing any errors encountered.
   */
  private String parseMethods(ParserResult result, ParserParameters parameters) {
    Object obj = parameters.getModel();
    //first we get all methods defined in the test model object (also all scopes -> private, protected, ...)
    Collection<Method> methods = getAllMethods(obj.getClass());
    //there are always some methods inherited from java.lang.Object so we checking them here is pointless. FSM.check will do it
    log.debug("methods " + methods.size());
    String errors = "";
    //loop through all the methods defined in the given object
    for (Method method : methods) {
      log.debug("method:" + method);
      parameters.setMethod(method);
      Annotation[] annotations = method.getAnnotations();
      //check all annotations for supported ones, use the given object to process them
      for (Annotation annotation : annotations) {
        Class<? extends Annotation> annotationClass = annotation.annotationType();
        log.debug("class:" + annotationClass);
        AnnotationParser parser = annotationParsers.get(annotationClass);
        if (parser == null) {
          //unsupported annotation (e.g. for some completely different aspect)
          continue;
        }
        log.debug("parser:" + parser);
        //set the annotation itself as a parameter to the used parser object
        parameters.setAnnotation(annotation);
        //and finally parse it
        errors += parser.parse(result, parameters);
      }
    }
    return errors;
  }

  private Collection<Method> getAllMethods(Class clazz) {
    Class<?> superclass = clazz.getSuperclass();
    List<Method> methods = new ArrayList<>();
    if (superclass != null) {
      methods.addAll(getAllMethods(superclass));
    }
    Collections.addAll(methods, clazz.getMethods());
    Collections.sort(methods, new Comparator<Method>() {
      @Override
      public int compare(Method o1, Method o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    return methods;
  }
}