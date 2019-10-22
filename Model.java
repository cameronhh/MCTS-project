package A2Code;

import problem.*;
import simulator.State;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class Model {

    private static ProblemSpec ps;
    public static int n;

    public Model(ProblemSpec ps) {
        this.ps = ps;
        this.n = 1;
    }

    public static double reward(State state, int steps) {
        double res = 0.0;
        if (state.getPos() < n) {
            return -0.05;
        }
        int stepsLeft = ps.getMaxT() - steps;

        // if goal state
        if (state.getPos() == ps.getN()) {
            res = 0.5 + 0.5 * (double)stepsLeft / ps.getMaxT();
        } else {
            res = 0.0001 * state.getPos() * stepsLeft;
        }
        // getting some weird 0s
        res = Math.max(res, 0.0);

        // take a min just in case
        return Math.min(res, 1.0);
    }

    /**
     * Simply just returns MOVE at this point.
     * Abstracted it to make it easier to change later
     * @param state
     * @return
     */
    public static Action defaultPolicy(State state) {
        return new Action(ActionType.MOVE);
    }

    /**
     * Yanked from old MonteCarloTreeSearch
     * @param state
     * @return
     */
    public static List<Action> getActions(State state) {
        // get action types
        List<ActionType> tmp = ps.getLevel().getAvailableActions();

        List<Action> res = new LinkedList<>();

        String cur;
        // A1: add move action
        res.add(new Action(tmp.get(0)));

        // A2: add change car actions
        cur = state.getCarType();

        for (String s : ps.getCarOrder()) {
            if (!s.equals(cur)) {
                res.add(new Action(tmp.get(1), s));
            }
        }

        // A3: add change driver actions
        cur = state.getDriver();

        for (String s : ps.getDriverOrder()) {
            if (!s.equals(cur)) {
                res.add(new Action(tmp.get(2), s));
            }
        }

        // A4: add change tire actions
        Tire curT = state.getTireModel();

        for (Tire t : ps.getTireOrder()) {
            if (!t.equals(curT)) {
                res.add(new Action(tmp.get(3), t));
            }
        }

        // Level UP BOIS
        if (ps.getLevel().getLevelNumber() >= 2){

            // A5: add fuel
            int curFuel = state.getFuel();
            if (curFuel < 20) {
                res.add(new Action(tmp.get(4), 10));
            }

            // A6: change tire pressure (50%, 75% or 100%)
            TirePressure curPressure = state.getTirePressure();
            for(TirePressure i: TirePressure.values()) {
                if(i != curPressure) {
                    res.add(new Action(tmp.get(5), i));
                }
            }
        }

        // Level UP once again
        if (ps.getLevel().getLevelNumber() >= 4){
            // A7: combination of changing car type and changing driver
            String carType = state.getCarType();
            String driver = state.getDriver();
            for (String i : ps.getCarOrder()) {
                for (String j: ps.getDriverOrder()) {
                    if (carType != i && driver != j) {
                        res.add(new Action(tmp.get(6), i, j));
                    }
                }
            }
        }

        // and once more
        if (ps.getLevel().getLevelNumber() >= 5){
            // A8: combination of changing tire, fuel, pressure
            TirePressure pressure = state.getTirePressure();
            Tire tire = state.getTireModel();
            int curFuel = state.getFuel();
            for(Tire i: ps.getTireOrder()) {
                    for(TirePressure k: TirePressure.values()) {
                        if(tire != i  && pressure != k) {
                            res.add(new Action(tmp.get(7), i, Math.min(10, 50), k));
                        }
                    }
            }
        }
        return res;
    }
}
