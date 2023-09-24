package controllers.Astar;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import javax.swing.plaf.nimbus.State;
import java.awt.*;
import java.util.PriorityQueue;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Comparator;


public class Agent extends controllers.sampleRandom.Agent{

    private class Node {
        public double expectCost; // this cost equals passed cost + heuristic cost

        public double pastCost;

        public StateObservation sto;

        public LinkedList<Types.ACTIONS> pastActions;

        public LinkedList<StateObservation> pastStates;

        public boolean hasKey;



        public Node(double passed, LinkedList<Types.ACTIONS> p, LinkedList<StateObservation> p2) {
            sto = p2.getLast().copy();
            Vector2d npc = sto.getAvatarPosition();
            pastCost = passed;
            pastActions = p;
            pastStates = p2;
            hasKey = p2 == null ? false : judgeKey(this);
            expectCost = passed + heuristic(npc);
        }

        public StateObservation getState(){
            return pastStates.getLast().copy();
        }


        private double heuristic(Vector2d npc){
            if (this.hasKey) {
                double ans = pastActions == null ? Math.abs(npc.x - goalpos.x) + Math.abs(npc.y - goalpos.y) :
                        Math.abs(npc.x - goalpos.x) + Math.abs(npc.y - goalpos.y) + pastActions.size() * 50;
                return ans;
            }
            else {
//                double ans = pastActions == null ? Math.abs(npc.x - keypos.x) + Math.abs(npc.y - keypos.y) +
//                        Math.abs(goalpos.x - keypos.x) + Math.abs(goalpos.y - keypos.y):
//                        Math.abs(npc.x - keypos.x) + Math.abs(npc.y - keypos.y) + Math.abs(goalpos.x - keypos.x)
//                                + Math.abs(goalpos.y - keypos.y) + pastActions.size() * 50;
                double ans = pastActions == null ? Math.abs(npc.x - keypos.x) + Math.abs(npc.y - keypos.y) +
                        Math.abs(goalpos.x - keypos.x) + Math.abs(goalpos.y - keypos.y) + computeBoxHoles(getState()) * 0.2 + getState().getGameScore() * 10:
                        Math.abs(npc.x - keypos.x) + Math.abs(npc.y - keypos.y) + Math.abs(goalpos.x - keypos.x)
                                + Math.abs(goalpos.y - keypos.y) + pastActions.size() * 50 + computeBoxHoles(getState()) * 0.2 + getState().getGameScore() * 10;
                return ans;
            }
        }


        private double computeBoxHoles(StateObservation stateObs){
            double ans = 0;
            ArrayList<Observation>[] fixedPositions = stateObs.copy().getImmovablePositions();
            ArrayList<Observation>[] movingPositions = stateObs.copy().getMovablePositions();
            if (fixedPositions.length > 2)
                holes = fixedPositions[fixedPositions.length - 2];
            if (movingPositions != null)
                boxes = movingPositions[movingPositions.length - 1];
            if (boxes != null && holes != null) {
                for(Observation box: boxes)
                {
                    for(Observation hole: holes)
                    {
                        ans += Math.abs(box.position.x - hole.position.x) + Math.abs(box.position.y - hole.position.y);
                    }
                }
            }
            return ans;
        }
    }

    private LinkedList<StateObservation> searchedStates = null;

    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer){
        super(so, elapsedTimer);
    }

    private int depth = 18;

//    private boolean hasKey = false;


    private Vector2d goalpos = null;

    private Vector2d keypos;

    private Vector2d npcPosition;

    private double minCost = 99999;


    private LinkedList<Types.ACTIONS> storeActions = null;

    private PriorityQueue<Node> searching = null;

    private int cnt = 0;

    private int cnt1 = 0;

    private int step = 0;

    private ArrayList<Observation> boxes = null;

    private ArrayList<Observation> holes = null;
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        // read map information
        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions();
        ArrayList<Observation>[] fixedPositions = stateObs.getImmovablePositions();
        ArrayList<Observation>[] movingPositions = stateObs.getMovablePositions();
        ArrayList<Observation>[] resourcesPositions = stateObs.getResourcesPositions();
        ArrayList<Observation>[] portalPositions = stateObs.getPortalsPositions();
        grid = stateObs.getObservationGrid();
        npcPosition = stateObs.copy().getAvatarPosition();
        if (goalpos == null) {
            goalpos = fixedPositions[1].get(0).position; //目标的坐标
        }
        if (cnt == 0) {
            keypos = movingPositions[0].get(0).position;//钥匙的坐标
        }


        /*printDebug(npcPositions,"npc");
        printDebug(fixedPositions,"fix");
        printDebug(movingPositions,"mov");
        printDebug(resourcesPositions,"res");
        printDebug(portalPositions,"por");
        System.out.println();               */
        Comparator<Node> comparator = Comparator.comparingDouble(o->o.expectCost);
        if (searchedStates == null) {
            searchedStates = new LinkedList<StateObservation>();
        }
        if (storeActions == null) {
            storeActions = new LinkedList<Types.ACTIONS>();
        }
        searchedStates.addLast(stateObs.copy());
        if (searching == null) {
            cnt1++;
            searching = new PriorityQueue<Node>(comparator);
            Node start = new Node(storeActions.size() * 50, null, searchedStates);
            searching.add(start);
            step = 0;
        }
        LinkedList<Types.ACTIONS> ans = astarSearch(stateObs.copy());
        // the last action tells us how to get to this node
        System.out.println("This action is " + ans.get(step).name());
        cnt++;
        storeActions.addLast(ans.get(step));
        return ans.get(step++);
    }


    private boolean goal_test(StateObservation stCopy){
        StateObservation myCopy = stCopy.copy();
        return myCopy.getGameWinner() == Types.WINNER.PLAYER_WINS;
    }

    private boolean in_list(StateObservation stCopy, Node parent) {
        for (StateObservation state: parent.pastStates) {
            if (stCopy.copy().equalPosition(state.copy())) {
                return true;
            }
        }
        return false;
    }


    private int cnt4 = 0;
    // For each time we only give one step
    private LinkedList<Types.ACTIONS> astarSearch(StateObservation stCopy){
        // TODO:
        // 1. get our first element in the stcopy and we want to get every node after any feasible step
        // 2. get the poll element which we'll return
        // NOTE: YOU MAY FIND YOUR ACTION HARD TO ITERATE ALL POSSIBLE SOLUTIONS
        LinkedList<Types.ACTIONS> ret = null;
        int cur_depth = 0;
        while (!searching.isEmpty()) {
            Node cur = searching.poll();
            if (goal_test(cur.getState())) {                // 满足目标后直接返回，并加回原先优先队列
                searching.add(cur);
                ret = cur.pastActions;
                break;
            }
            if (cur.pastActions != null && cur.pastActions.size() == depth ) {
//                if (cur.pastActions.get(1).name() == "ACTION_DOWN")// 达到深度后直接返回，注意，此时是删除元素的
//                    System.out.println("here");
                ret = cur.pastActions;
                searching.add(cur);
                if (step == cur.pastActions.size() / 3) {
                    searching = null;
                }
//                if (step == cur.pastActions.size() - 1) {
//                    searching = null;
//                }
                break;
            }
            for (Types.ACTIONS action : cur.sto.copy().getAvailableActions()) {
                cnt4++;
//                if (cur.pastActions != null && cur.pastActions.size() >=2 &&  cur.pastActions.get(0).name() == "ACTION_LEFT" && cur.pastActions.get(1).name() == "ACTION_DOWN")// 达到深度后直接返回，注意，此时是删除元素的
//                    System.out.println("here");
                StateObservation nxt = cur.sto.copy();
                StateObservation check = nxt.copy();
                check.advance(action);
                if (in_list(check.copy(), cur) || check.copy().getGameWinner() == Types.WINNER.PLAYER_LOSES) {
                    continue;
                }
                Node temp = null;
                // adding element to our searching priority queue

                LinkedList<Types.ACTIONS> pastActions = null;
                if (cur.pastActions != null) {
                    pastActions = (LinkedList<Types.ACTIONS>) cur.pastActions.clone();
                }
                else {
                    pastActions = new LinkedList<Types.ACTIONS>();
                }
                pastActions.add(action);
                LinkedList<StateObservation> past= (LinkedList<StateObservation>) cur.pastStates.clone();
                // 只需要加走了一步之后的情况即可
                nxt.advance(action);                                        // 注意这里nxt走过了一次
                past.addLast(nxt);
                temp = new Node(cur.pastCost,  pastActions, past);
                Node prev = equalNode(temp);            // need prune if in the same place but with larger cost
                if (prev != null && prev.expectCost < temp.expectCost) {
                    continue;
                }
                searching.add(temp);


            }
        }
        return ret;
    }

    // To judge whether we have gotten a key or not

    private boolean judgeKey(Node n) {
        LinkedList<StateObservation> states= n.pastStates;
        for (StateObservation state : states) {
            Vector2d curNpcPosition = state.copy().getAvatarPosition();
            if (curNpcPosition.x == keypos.x && curNpcPosition.y == keypos.y)
                return true;
        }
        return false;
    }


    private Node equalNode(Node other) {
        for (Node temp : searching) {
            if (temp.getState().copy().equalPosition(other.getState().copy())) {
                return temp;
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
