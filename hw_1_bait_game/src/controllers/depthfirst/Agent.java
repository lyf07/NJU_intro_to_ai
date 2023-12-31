package controllers.depthfirst;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;

import javax.swing.plaf.nimbus.State;
import java.awt.*;
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

        printDebug(npcPositions,"npc");
        printDebug(fixedPositions,"fix");
        printDebug(movingPositions,"mov");
        printDebug(resourcesPositions,"res");
        printDebug(portalPositions,"por");
        System.out.println();

        Types.ACTIONS action = null;
        StateObservation stCopy = stateObs.copy();
        if (searchActionList == null) {
            searchActionList = depthFirstSearch(stCopy);
        }
        if (searchActionList.size() > 0) {
            action = searchActionList.remove(0);
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

    /**
     * Prints the number of different types of sprites available in the "positions" array.
     * Between brackets, the number of observations of each type.
     * @param positions array with observations.
     * @param str identifier to print
     */
    private void printDebug(ArrayList<Observation>[] positions, String str)
    {
        if(positions != null){
            System.out.print(str + ":" + positions.length + "(");
            for (int i = 0; i < positions.length; i++) {
                System.out.print(positions[i].size() + ",");

            }
            System.out.print("); ");
        }else System.out.print(str + ": 0; ");
    }

    /**
     * Gets the player the control to draw something on the screen.
     * It can be used for debug purposes.
     * @param g Graphics device to draw to.
     */
    public void draw(Graphics2D g)
    {
        int half_block = (int) (block_size*0.5);
        for(int j = 0; j < grid[0].length; ++j)
        {
            for(int i = 0; i < grid.length; ++i)
            {
                if(grid[i][j].size() > 0)
                {
                    Observation firstObs = grid[i][j].get(0); //grid[i][j].size()-1
                    //Three interesting options:
                    int print = firstObs.category; //firstObs.itype; //firstObs.obsID;
                    g.drawString(print + "", i*block_size+half_block,j*block_size+half_block);
                }
            }
        }
    }
}
