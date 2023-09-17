package controllers.depthfirst;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;

import javax.swing.plaf.nimbus.State;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;

public class Agent extends controllers.sampleRandom.Agent{

    private LinkedList<StateObservation> searchedStates = null;
    protected List<Types.ACTIONS> searchActionList = null;
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer){
        super(so, elapsedTimer);
    }

    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        // read map information
        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions();
        ArrayList<Observation>[] fixedPositions = stateObs.getImmovablePositions();
        ArrayList<Observation>[] movingPositions = stateObs.getMovablePositions();
        ArrayList<Observation>[] resourcesPositions = stateObs.getResourcesPositions();
        ArrayList<Observation>[] portalPositions = stateObs.getPortalsPositions();
        grid = stateObs.getObservationGrid();

        /*printDebug(npcPositions,"npc");
        printDebug(fixedPositions,"fix");
        printDebug(movingPositions,"mov");
        printDebug(resourcesPositions,"res");
        printDebug(portalPositions,"por");
        System.out.println();               */

        Types.ACTIONS action = null;
        StateObservation stCopy = stateObs.copy();
        if (searchActionList == null) {
            searchActionList = depthFirstSearch(stCopy);
        }
        if (searchActionList.size() > 0) {
            action = searchActionList.remove(0);
            System.out.println("This move is: " + action.name());
        }
        return action;
    }


    private boolean goal_test(StateObservation stCopy){
        StateObservation myCopy = stCopy.copy();
        return myCopy.getGameWinner() == Types.WINNER.PLAYER_WINS;
    }

    private boolean in_list(StateObservation stCopy) {
        for (StateObservation state: searchedStates) {
            if (stCopy.copy().equalPosition(state.copy())) {
                return true;
            }
        }
        return false;
    }
    private List<Types.ACTIONS> depthFirstSearch(StateObservation stCopy) {
        if (searchedStates == null) {
            searchedStates = new LinkedList<StateObservation>();
        }
        LinkedList<Types.ACTIONS> searchedActions = new LinkedList<Types.ACTIONS>();
        StateObservation myCopy = stCopy.copy();
        ArrayList<Types.ACTIONS> actions = stCopy.getAvailableActions();
        for (Types.ACTIONS action : actions) {
            searchedActions.addLast(action);
            searchedStates.addLast(myCopy.copy());
            myCopy.advance(action);
            if (goal_test(myCopy)) {
                return searchedActions;
            }
            LinkedList<Types.ACTIONS> ans = null;
            if (in_list(myCopy.copy()) ||  (ans = (LinkedList<Types.ACTIONS>)depthFirstSearch(myCopy.copy())) == null) {
                searchedActions.removeLast();
                myCopy = stCopy.copy();
                continue;
            }
            else {
                for (Types.ACTIONS rightAction: ans) {
                    searchedActions.addLast(rightAction);
                }
                return searchedActions;
            }
        }
        return null;
    }
}
