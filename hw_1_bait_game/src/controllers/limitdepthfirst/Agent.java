package controllers.limitdepthfirst;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import javax.swing.plaf.nimbus.State;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;


public class Agent extends controllers.sampleRandom.Agent{

    private LinkedList<StateObservation> searchedStates = null;

    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer){
        super(so, elapsedTimer);
    }

    private int depth = 5;

    private boolean hasKey = false;


    private Vector2d goalpos;

    private Vector2d keypos;

    private Vector2d npcPosition;

    private double minCost = 99999;

    private LinkedList<Types.ACTIONS> bestActions = null;

    private LinkedList<Types.ACTIONS> searchedActions = null;

    private LinkedList<Types.ACTIONS> storeActions = null;


    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        // read map information
        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions();
        ArrayList<Observation>[] fixedPositions = stateObs.getImmovablePositions();
        ArrayList<Observation>[] movingPositions = stateObs.getMovablePositions();
        ArrayList<Observation>[] resourcesPositions = stateObs.getResourcesPositions();
        ArrayList<Observation>[] portalPositions = stateObs.getPortalsPositions();
        grid = stateObs.getObservationGrid();
        goalpos = fixedPositions[1].get(0).position; //目标的坐标
        if (!hasKey) {
            keypos = movingPositions[0].get(0).position;//钥匙的坐标
        }


        /*printDebug(npcPositions,"npc");
        printDebug(fixedPositions,"fix");
        printDebug(movingPositions,"mov");
        printDebug(resourcesPositions,"res");
        printDebug(portalPositions,"por");
        System.out.println();               */
        bestActions = new LinkedList<Types.ACTIONS>();
        searchedActions = new LinkedList<Types.ACTIONS>();
        if (searchedStates == null) {
            searchedStates = new LinkedList<StateObservation>();
        }
        if (storeActions == null) {
            storeActions = new LinkedList<Types.ACTIONS>();
        }
        searchedStates.add(stateObs.copy());
        limitDepthSearch(stateObs.copy(), depth);
        minCost = 99999;
        Types.ACTIONS ans = bestActions.getFirst();
        bestActions.removeFirst();
        storeActions.addLast(ans);
        return ans;
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

    private void limitDepthSearch(StateObservation stateObs, int curDepth){
        searchedStates.addLast(stateObs.copy());
        for (Types.ACTIONS action : stateObs.copy().getAvailableActions()) {
            StateObservation stCopy = stateObs.copy();
            stCopy.advance(action);
            npcPosition = stCopy.copy().getAvatarPosition();
            hasKey = judge(npcPosition);
            searchedActions.addLast(action);
            if (curDepth == 0) {
                double expect = heuristic(npcPosition);
                if (expect < minCost) {
                    minCost = expect;
                    bestActions = (LinkedList<Types.ACTIONS>) searchedActions.clone();
                }
            }
            else if (goal_test(stCopy.copy())) {
                minCost = -1;
                bestActions = (LinkedList<Types.ACTIONS>) searchedActions.clone();
                return;
            }
            else if (!in_list(stCopy.copy())) {
                limitDepthSearch(stCopy.copy(), curDepth - 1);
            }
            searchedActions.removeLast();
        }
        searchedStates.removeLast();


    }

    private boolean judge(Vector2d npc) {
        for (StateObservation state : searchedStates) {
            Vector2d curNpcPosition = state.copy().getAvatarPosition();
            if (curNpcPosition.x == keypos.x && curNpcPosition.y == keypos.y)
                return true;
        }
        return false;
    }

    private double heuristic(Vector2d npc){
        if (hasKey) {
            return Math.abs(npc.x - goalpos.x) + Math.abs(npc.y - goalpos.y);
        }
        else {
            return Math.abs(npc.x - keypos.x) + Math.abs(npc.y - keypos.y) +
                    Math.abs(goalpos.x - keypos.x) + Math.abs(goalpos.y - keypos.y);
        }
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
