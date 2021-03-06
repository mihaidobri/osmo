package osmo.visualizer.fsmbuild;

import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import osmo.tester.model.FSMTransition;

import java.awt.Color;

/**
 * @author Teemu Kanstren
 */
public class TransitionVertextLabelRenderer extends DefaultVertexLabelRenderer {
  public TransitionVertextLabelRenderer(Color pickedVertexLabelColor) {
    super(pickedVertexLabelColor);
  }

  @Override
  protected void setValue(Object value) {
    FSMTransition t = (FSMTransition) value;
    super.setValue(t.getName());
  }
}
