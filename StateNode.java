package A2Code;

import problem.Action;
import simulator.State;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * StateNode for MCTS, each StateNode contains a state (immutable).
 */
public class StateNode {
    // the state this StateNode represents
    public State state;

    // time step at which this state was reached
    public int step;

    // the action completed before this state was reached
    public ActionNode parent;

    // true if this StateNode's children have been generate
    public boolean hasActions;

    // the actions performed after this state
    public Hashtable<String, ActionNode> children;

    // the reward from this state (Model.reward(this))
    public double reward;

    // this StateNode's ActionNode with the greatest expected future value
    public ActionNode argmaxQ;

    // the greatest expected future value of all ActionNodes in this state
    public double averageExpectedValue;
    // = reward(this) + Q(s,a)
    // = reward plus Q(s,a) of this state's best performing action

    public StateNode(State s, int step) {
        this.state = s;
        this.step = step;
        this.parent = null;
        this.hasActions = false;
        this.children = null;
        this.argmaxQ = null;
        this.averageExpectedValue = 0.0;
        this.reward = Model.reward(this.state, this.step);
    }

    public void generateActionNodes(List<Action> actions) {
        if (hasActions) {
            return;
        }
        children = new Hashtable<String, ActionNode>();
        for (Action a : actions) {
            ActionNode tmp = new ActionNode(a);
            tmp.parent = this;
            children.put(a.getText(), tmp);
        }
        hasActions = true;
    }

    public ActionNode getChild(Action a) {
        return children.get(a.getText());
    }

    /**
     * Updates the averageExpectedValue of this state, used when
     * back-propagating.
     */
    public void updateQ() {
        this.averageExpectedValue = reward;
        if (!hasActions) {
            return;
        }
        double best = children.get("A1").averageExpectedValue;
        argmaxQ = children.get("A1"); // always move if it is equal best option
        for (Map.Entry<String, ActionNode> e : children.entrySet()) {
            if (e.getValue().averageExpectedValue > best) {
                best = e.getValue().averageExpectedValue;
                argmaxQ = e.getValue();
            }
        }
        this.averageExpectedValue = reward + best;
    }
}
