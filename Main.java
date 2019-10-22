package A2Code;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import problem.Action;
import problem.ActionType;
import problem.ProblemSpec;
import problem.Terrain;
import simulator.Simulator;
import simulator.State;


public class Main {

	final static long PER_STEP = 200; // some buffer to account for
	// constant expressions at start

    public static void main(String[] args) {

    	boolean trial = false;

    	int trials = 1;

		boolean extroverted = true;

        ProblemSpec ps;
        Simulator gameSimulator;
        try {
            ps = new PS(args[0]);

            Model model = new Model(ps);

			double totalReward = 0.0;
			int successes = 0;
			long startTime = 0;
			long totalTime = 0;
			long totalMemory = 0;
			MCTS mcts = new MCTS(null, null);

            for (int i = 0; i < trials; i ++) {
            	String outcome = " pass";
            	startTime = System.currentTimeMillis();
				gameSimulator = new Simulator(ps, args[1]);
				gameSimulator.reset(); // Always reset when creating a simulator;

				State initialState = State.getStartState(ps.getFirstCarType(),
						ps.getFirstDriver(), ps.getFirstTireModel());

				/*
				 * Simulates actions uniformly from each state.
				 * Rolls out simulated action according to default policy (just move)
				 * Reward function values 'quick wins' more than long ones.
				 * Simulates actions until PER_STEP timer has run out.
				 */
				int steps = 0;

				if (extroverted) System.out.println("Creating root node from initial state...");

				StateNode current = new StateNode(initialState, steps);

				if (extroverted) System.out.println("Creating MCTS...");

				mcts = new MCTS(current, ps);
				mcts.movePointer(current); // inherently done in MCTS constructor
				// but do it anyways for good measure

				State nextState;

				while(true) {
					if (extroverted) {
						System.out.println();
						System.out.println("Starting simulation...");
						System.out.println("Game time elapsed		- " + steps);
						System.out.print("Current state			- " + current.state.toString());
					}

					if (steps >= ps.getMaxT()) {
						if (extroverted) System.out.println("Game output				- " +
								"Time exceeded, terminating and printing to " +
								"output file");
						// run game simulator to trigger file output
						outcome = " fail";
						gameSimulator.step(new Action(ActionType.MOVE));
						break;
					}

					Action nextAction = mcts.simulate(PER_STEP);

					if (extroverted) {
						System.out.println("Simulation output		- " + nextAction.getText());
					}

					nextState = gameSimulator.step(nextAction);
					steps = gameSimulator.getSteps();

					totalReward += Model.reward(nextState, steps);

					if (extroverted) System.out.print("Game output		 		- "
							+ nextState.toString());

					// check finished
					if (nextState.getPos() == ps.getN()) {
						if (extroverted) {
							System.out.println();
							System.out.println("Goal state reached, terminating " +
									"and printing to output file.");
						}
						successes += 1;
						break;
					}

					// get the ActionNode for the action we just did
					ActionNode node = current.children.get(nextAction.getText());

					// if we already have a StateNode for the state the simulator
					// gave us, get it
					StateNode next = node.getStateNode(nextState);

					if (next == null) {
						// we need to make a new StateNode
						next = new StateNode(nextState, steps);
					}

					// disconnect from the tree, using an online method we don't
					// care about what's already happened
					next.parent = null;

					// reset ptrs for next simulation
					current = next;
					mcts.movePointer(current);
					Model.n = current.state.getPos();
				}
				Model.n = 1;

				if (trial) {
					long tmpTime = (System.currentTimeMillis()-startTime);
					StringBuilder sb = new StringBuilder();
					sb.append("Race ");
					sb.append((i+1));
					sb.append("	|");
					sb.append(outcome);
					sb.append(" (");
					sb.append(steps);
					sb.append(")	| ");
					sb.append("Time ");
					sb.append("taken (ms)");
					sb.append(": ");
					sb.append(tmpTime);
					sb.append("	| Max memory used: ");
					sb.append(mcts.maxMemUsed/(1024*1024));
					sb.append("MB	|");

					System.out.println(sb.toString());
					totalTime += tmpTime;
					totalMemory += mcts.maxMemUsed;

					System.out.println("Runtime statistics:");
					System.out.println("Average time taken 				= " + (totalTime/trials));
					System.out.println("Average max memory used 		= " + (totalMemory
							/ (1024*1024*trials)) + "Mb");
					System.out.println("Average total Reward received 	= " + (totalReward/trials));
					System.out.println("Success rate					= " + ((double)successes/trials));
				}
			}
			if (extroverted) System.out.println();

        } catch (IOException e) {
            System.out.println("IO Exception occurred");
            System.exit(1);
        }
    }
    
    // Construct a table: 
    /*
     * STATE         | -4, -3, -2, ... 4, 5, Slip, Breakdown  
     * P(Te, Dr, CT) |0.1  0.1 0.05          0.05       0.5 
     * 
     * */
    private static LinkedList<double[]> createTransitionTable(ProblemSpec ps, Simulator sym){
    	
    	LinkedList<double []> tf = new LinkedList<double []>();
        
        int combo = ps.getCarOrder().size() * ps.getDriverOrder().size() * ps.getTerrainMap().size(); 
        int noCars = ps.getCarOrder().size();
        int noDrivers = ps.getDriverOrder().size();
        
        // get ready for the most nested forloop ever 
    	for(int j=0; j<noCars; j++) {
    		for(int k=0; k<noDrivers; k++) {
    			for(int l=-4; l<=5; l++) {
        			;
        		}
    		}
    	}

        
        for(int i=0; i < combo; i++) {
        	if(i < noCars) {
        		tf.add(ps.getCarMoveProbability().get(ps.getCarOrder().get(i)));
        	} else if (i < noDrivers + noCars){
        		tf.add(ps.getDriverMoveProbability().get(ps.getDriverOrder().get(i-noDrivers)));
        	} 
        }
     	return tf;     	
	
    }
    
    /*
     * generates a list of states that we are calculating probablities for
     */
    private static LinkedList<String> getStateLabels(ProblemSpec ps){
        int noCars = ps.getCarOrder().size();
        int noDrivers = ps.getDriverOrder().size();
        
        LinkedList<String> states = new LinkedList<String>(); 
    	// states labels 
    	for(int j=0; j<noCars; j++) {
    		for(int k=0; k<noDrivers; k++) {
        		states.add(String.
        				format("P(%s, %s)", ps.getCarOrder().get(j), ps.getDriverOrder().get(k)));
    		}
    	}
    	return states; 
    }
    
    
    // this is hideous im sorry- but it is useful!  
    private static void printTransitionTable(ProblemSpec ps, LinkedList<double []> tf, Terrain[] terrains) {
        LinkedList<String> states = getStateLabels(ps);
    	
    	StringBuilder st = new StringBuilder(); 
    	st.append("STATE \t\t\t\t\t| ");

    	for(int i=-4; i<=5; i++) {
            if(terrains[i+4].toString().length() > 4) {
                st.append(terrains[i+4] + "\t| ");            	
            } else {
            	st.append(terrains[i+4] + "\t\t| ");
            }
    	}    	
    	st.append("\n");
    	
    	st.append("STATE \t\t\t\t\t| ");
    	int rowchar = 60; 
    	for(int i=-4; i<=5; i++) {
                st.append(i + "\t\t| ");
            rowchar += 18; 
    	}
    	st.append("sl\t\t| br\n");
    	
    	for(int i = 0; i<rowchar; i++) {
    		st.append("-");
    	}
    	st.append("\n");
    
    	for(int i=0; i < tf.size(); i++) {
    		st.append(states.get(i));
    		if(states.get(i).length() > 18) {
            	st.append("\t");    			
    		} else {
            	st.append("\t\t");
    		}
    		
    		for(int j=0; j < tf.get(i).length; j++) {
    			if(j==0) {
        			st.append(" \t\t| " + tf.get(i)[j]);    				
    			} else {
        			st.append(" \t\t " + tf.get(i)[j]);
    			}
    		}
        	st.append("\n");
        }
    	
    	System.out.print(st);
	}


	// this is legit just to get a full print but still pull support code 
    private static class PS extends ProblemSpec{

		public PS(String fileName) throws IOException {
			super(fileName);

		}
		
		@Override 
		public String toString() {
			StringBuilder sb = new StringBuilder();
	        // 1.
	        sb.append("level: ").append(this.getLevel().getLevelNumber()).append("\n");
	        // 2.
	        sb.append("discount: ").append(this.getDiscountFactor()).append("\n");
	        sb.append("recoverTime: ").append(this.getSlipRecoveryTime()).append("\n");
	        sb.append("repairTime: ").append(this.getRepairTime()).append("\n");
	        // 3.
	        sb.append("N: ").append(this.getN()).append("\n");
	        sb.append("maxT: ").append(this.getMaxT()).append("\n");
	        // 4.
	        sb.append("Environment map: [");
	        for (int i = 0; i < this.getEnvironmentMap().length; i++) {
	            sb.append(this.getEnvironmentMap()[i].asString());
	            if (i < this.getEnvironmentMap().length - 1)
	                sb.append(" | ");
	        }
	        sb.append("]\n");
	        // 5
	        sb.append("Car Types: [");
	        for (int i = 0; i <  this.getCarOrder().size(); i++) {
	        	sb.append(this.getCarOrder().get(i));
	            if (i < this.getCarOrder().size() - 1)
	                sb.append(" | ");
	        }
	        sb.append("]\n");
	        // 6. 
	        sb.append("Car move probabilities:\n");
	        for (int i = 0; i < this.getCarMoveProbability().size(); i++) {
	        	System.out.println(i);
	        	sb.append(this.getCarOrder().get(i) + ": [");
	        	double[] carProbs =this.getCarMoveProbability().get(this.getCarOrder().get(i));
	            for (int j = 0; j < carProbs.length; j++) {
	            	sb.append(carProbs[j]);
	                if (j < carProbs.length - 1)
	                    sb.append(" | ");
	            }
	            sb.append("]\n");
	        }
	        // 7. 
	        sb.append("Drivers: ").append(this.getDriverOrder()).append('\n');
	        // 8. 
	        sb.append("Driver move probabilities:\n");
	        for (int i = 0; i < getDriverMoveProbability().size(); i++) {
	        	sb.append(this.getDriverOrder().get(i) + ": [");
	        	double[] driverProbs = getDriverMoveProbability().get(this.getDriverOrder().get(i));
	            for (int j = 0; j < driverProbs.length; j++) {
	            	sb.append(driverProbs[j]);
	                if (j < driverProbs.length - 1)
	                    sb.append(" | ");
	            }
	            sb.append("]\n");
	        }
	        // 9. 
	        sb.append("Tire move probabilities:\n");
	        for (int i = 0; i < getTireModelMoveProbability().size(); i++) {
	        	sb.append(getTireOrder().get(i) + ": [");
	        	System.out.println(getTireOrder().get(i));
	        	double[] tireProbs = getTireModelMoveProbability().get(getTireOrder().get(i));
	            for (int j = 0; j < tireProbs.length; j++) {
	            	sb.append(tireProbs[j]);
	                if (j < tireProbs.length - 1)
	                    sb.append(" | ");
	            }
	            sb.append("]\n");
	        }
	        // 10. Fuel usage probability 
	        sb.append("idk i just want death").append(getFuelUsage());
	        sb.append("Fuel Usage: ").append(getFuelUsage());
	        for (int i=0; i<getFuelUsage().length; i++) {
	        	System.out.println(Arrays.toString(getFuelUsage()[i]));
	        }
	        // 11. 


	        return sb.toString();
		}
    	
    }
    
}
