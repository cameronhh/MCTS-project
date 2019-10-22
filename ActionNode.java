package A2Code;

import problem.Action;
import simulator.State;

import java.util.HashSet;

/**
 * ActionNode for MCTS
 * Represents an Action and stores the StateNodes for States incurred from
 * taking this.action
 */
public class ActionNode {

    // the action this ActionNode represents
    public Action action;

    // the state this action was executed from
    public StateNode parent;

    // the states achieved from taking this action
    public HashSet<StateNode> children;

    // number of times action has been taken from it's state
    public int n;

    // Q(s, a) where s = this.parent.state, a = this.a
    public double averageExpectedValue;

    public ActionNode(Action a){
        this.action = a;
        this.parent = null;
        this.children = new HashSet<StateNode>();
        this.n = 0;
        this.averageExpectedValue = 0.0;
    }

    public void addChild(StateNode node) {
        node.parent = this;
        children.add(node);
    }

    public void update(double q) {
        // Q(s,a) = (Q(s,a) * n + q) / n + 1
        // n + 1
        averageExpectedValue = (averageExpectedValue * n + q) / (n + 1);
        n++;
    }

    // returns null if doesn't exist
    public StateNode getStateNode(State state) {
        for (StateNode s : children) {
            if (s.state.equals(state)) {
                return s;
            }
        }
        return null;
    }

}
