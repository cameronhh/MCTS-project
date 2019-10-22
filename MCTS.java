package A2Code;

import java.util.Map;
import problem.Action;
import problem.ActionType;
import problem.ProblemSpec;
import simulator.State;


public class MCTS {

    private ProblemSpec ps;

    // a pointer to the current node we are simulating from
    private StateNode current;

    private boolean chatty = false;

    private static int DEPTH = 2;

    private Action prevAction;

    public long maxMemUsed;


    public MCTS(StateNode root, ProblemSpec ps) {
        this.ps = ps;
        this.current = root;
        this.prevAction = null;
        this.maxMemUsed = Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory();
    }

    // resets the search tree for a new rollout from the given StateNode
    // do this with the the root node you want before every simulate() call
    // StateNode parameter doesn't need to be connected to the tree but waste
    // of time if it's not
    public void movePointer(StateNode node) {
        this.current = node;
    }

    // conducts W simulations from StateNode being pointed at by this.current
    // return value is action with greatest expected reward after simulations
    public Action simulate(long timeConstraint) {
        long startTime = System.currentTimeMillis();
        Symulator simon = new Symulator(ps);

        // generate ActionNodes for this.current if they don't exist
        if (!current.hasActions) {
            current.generateActionNodes(Model.getActions(current.state));
        }

        while (System.currentTimeMillis() - startTime < timeConstraint) {
            // for each action, simulate the next state and then rollout to find
            // it's expected value
            for (ActionNode a : current.children.values()) {
                if (chatty)
                    System.out.println("Resetting SYMulator");

                simon.reset(current.state, current.step);

                if (chatty)
                    System.out.println("Symulating the next state after " +
                            "action: " + a.action.getText() + " at: "
                            + current.state.toString());

                // create the StateNode for the next state
                State nextState = simon.step(a.action);

                StateNode newState = new StateNode(nextState, simon.getSteps());

                if (chatty) {
                    System.out.println("We got this state: " + nextState.toString());
                    System.out.println("After " + simon.getSteps() + " steps.." +
                            ".");
                }

                // Rollout the state, also populates newState's children
                rollout(newState, simon);

                // Add new StateNode as child to a (it is a child of the action)
                a.addChild(newState);

                // Update Q(s,a) of this ActionNode
                a.update(newState.averageExpectedValue);
                current.updateQ();

                if (chatty)
                    System.out.println("Nodes rolled out from simulation added to" +
                            " the current root.");

                // reverse traverse up the tree filling in Q(s,a) values
                // fill in avgReward in root node
                // this is done in rollout
            }
        }

        // THEN whichever Q(s,a) is highest in the root node,
        // give the REAL simulator that action, get our real next state
        // do the simulations and roll outs all over again
        // (there is some redundancy here to be worked out)

        if (chatty) {
            System.out.println("Determining the best action...");
            System.out.println("Current Node Reward: " + current.reward);
            System.out.println("Action values for current node: ");
            for (Map.Entry<String, ActionNode> e : current.children.entrySet()) {
                System.out.println("Value for " + e.getKey() + " is " + e.getValue().averageExpectedValue);
            }
        }
        Action res = current.argmaxQ.action;
        if (prevAction != null) {
            if (prevAction.getActionType().equals(res.getActionType())) {
                res = new Action(ActionType.MOVE);
            }
        }

        // memory calculations
        long tmp = Runtime.getRuntime().totalMemory()
                        - Runtime.getRuntime().freeMemory();
        if (tmp > maxMemUsed) maxMemUsed = tmp;

        prevAction = res;
        return res;
    }

    /**
     * rolls out from given node according to some default policy - the
     * 'rollout policy'
     * at the moment the rollout policy is 'just keep moving'
     *
     *
     */
    private void rollout(StateNode node, Symulator simon) {
        if (chatty)
            System.out.println("Rolling out StateNode with state: " +
                    node.state.toString());

        StateNode startState = node;
        StateNode currentState = startState;


        int counter = 0;
        while (!isTerminal(currentState) && counter < DEPTH) {
            // gets an action for this state according to default policy
            Action policy = Model.defaultPolicy(node.state);

            // generate ActionNodes for current state
            if (!currentState.hasActions) {
                currentState.generateActionNodes(Model.getActions(currentState.state));
            }

            // get the ActionNode for policy at this state
            ActionNode actionNode = currentState.getChild(policy);

            // simulate next state
            State newState = simon.step(policy);

            // create new StateNode
            StateNode newStateNode = new StateNode(newState, simon.getSteps());

            // add StateNode to the tree
            currentState.getChild(policy).addChild(newStateNode);

            // move pointer to new state
            currentState = newStateNode;

            // repeat until terminal or depth reached
            counter++;
        }

        // update the Q value of last node
        currentState.updateQ();

        /*
        *  The StateNode passed to this function, startState, has no parent
        *  until it is connected with the rest of the tree in the mcts.simulate
        *  function. That is while we update the parent action's Q(s,a) inside
        *  the loop, because if currentState is still pointing to startState, it
        *  will be null.
        */

        while (currentState.step > startState.step) {
            // update the Q value of state node's parent
            double q = currentState.averageExpectedValue;
            currentState.parent.update(q);

            // move pointer to the previous StateNode
            currentState = currentState.parent.parent;

            // update Q value
            currentState.updateQ();
        }
        if (chatty)
            System.out.println("Done rolling out the node.");
    }

    public boolean isTerminal(StateNode node) {
        return node.step >= ps.getMaxT() || node.state.getPos() == ps.getN();
    }


}
