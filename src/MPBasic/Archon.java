package MPBasic;

import battlecode.common.*;

import javax.tools.DocumentationTool.Location;

import MPBasic.Debug.*;
import MPBasic.Util.*;
import MPBasic.Comms.*;
import java.util.ArrayDeque;

public class Archon extends Robot {
    static enum State {
        CHILLING,
        INIT,
    };

    static int MAX_NUM_MINERS;
    static int MIN_NUM_MINERS;

    static int robotCounter;
    static int chillingCounter;
    static int minerCount;
    static State currentState;
    static int flagIndex;
    static int turnNumber;
    static double leadToLeave;
    static ArrayDeque<State> stateStack;
    static int leadToUse;
    static int leadNeededByBuilders;
    static MapLocation leadSource;
    static Direction[] nonWallDirections;
    static int endOfTurnMoney;

    public Archon(RobotController r) throws GameActionException {
        super(r);
        //writing all Archon locations immediately on round 0
        stateStack = new ArrayDeque<State>();
        MAX_NUM_MINERS = Math.min(Util.MAX_MINERS,
                                    rc.getMapWidth() * rc.getMapHeight() /
                                    Util.MAX_MAP_SIZE_TO_MINER_RATIO);
        MIN_NUM_MINERS = MAX_NUM_MINERS / 4;
        Debug.println("Max number of miners: " + MAX_NUM_MINERS+ ", Min number of miners: " + MIN_NUM_MINERS);
        int nextArchon = Comms.incrementFriendly();
        int myLocFlag = Comms.encodeLocation();
        r.writeSharedArray(nextArchon, myLocFlag);
        flagIndex = nextArchon + Comms.mapLocToFlag;
        currentState = getInitialState();
        turnNumber = nextArchon;
        leadNeededByBuilders = 0;
        leadToLeave = Util.leadPercentage(rc.getArchonCount(), nextArchon, 0);
        findBestLeadSource();
        nonWallDirections = findnonWallDirections();
        
        // System.out.println("nonWallDirections: " + nonWallDirections.toString());
    }

    public void findBestLeadSource() throws GameActionException{
        leadSource = null;
        int bestLeadSource = 0;
        for(MapLocation loc: rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), RobotType.ARCHON.visionRadiusSquared)) {
            int lead_amount = rc.senseLead(loc);
            if (lead_amount > bestLeadSource) {
                leadSource = loc;
            }
        }
    }

    public Direction[] findnonWallDirections() throws GameActionException {
        int nonWallCount = 0;
        for (Direction dir: Direction.allDirections()) {
            int visionRange = 0;
            if(dir.dx == 0 || dir.dy == 0) {
                visionRange = Util.ArchonStraightVisionRange;
             }
            else {
                visionRange = Util.ArchonDiagVisionRange;
            }
            if (rc.onTheMap(rc.getLocation().translate(dir.dx * visionRange, dir.dy * visionRange))) {
                nonWallCount ++;
            }
        }
        int iter = 0;
        Direction[] validDirections = new Direction[nonWallCount];
        for (Direction dir: Direction.allDirections()) {
            int visionRange = 0;
            if(dir.dx == 0 || dir.dy == 0) {
                visionRange = Util.ArchonStraightVisionRange;
             }
            else {
                visionRange = Util.ArchonDiagVisionRange;
            }
            if (rc.onTheMap(rc.getLocation().translate(dir.dx *visionRange, dir.dy * visionRange))) {
                validDirections[iter] = dir;
                iter++;
            }
        }
        return validDirections;
    }
    public State getInitialState() {
        return State.INIT;
    }

    public boolean buildRobot(RobotType toBuild, Direction mainDir) throws GameActionException {
        Direction[] orderedDirs = Util.getOrderedDirections(mainDir);
        for(Direction dir : orderedDirs) {
            if (rc.canBuildRobot(toBuild, dir)){
                rc.buildRobot(toBuild, dir);
                RobotInfo robot = rc.senseRobotAtLocation(rc.getLocation().add(dir));
                //in future, add info about this new robot to maps
                if (robot == null) {
                    System.out.println("CRITICAL: EC didn't find the robot it just built");
                }
                robotCounter += 1;
                return true;
            }
        }
        return false;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        // Update leadNeededByBuilders by reading a comms flag
        double availableLead = (double) rc.getTeamLeadAmount(rc.getTeam());
        double builderPercentage = ((double) leadNeededByBuilders) / availableLead;
        if (builderPercentage > 0.5) {
            builderPercentage = 0.5;
        }
        leadToLeave = Util.leadPercentage(rc.getArchonCount(), turnNumber, builderPercentage);
        leadToUse = (int) (availableLead * (1.0 - leadToLeave));
        updateRobotCounts();
        doStateAction();
        // if (Comms.enemyArchonCount() > 0) {
        //     System.out.println(rc.readSharedArray(Comms.firstEnemy) + "; " + rc.readSharedArray(Comms.firstEnemy + 1) + "; " + rc.readSharedArray(Comms.firstEnemy + 2) + "; " + rc.readSharedArray(Comms.firstEnemy + 3));
        // }
    }
    public void updateRobotCounts() throws GameActionException {
        minerCount = Comms.getMinerCount();
    }

    public void doStateAction() throws GameActionException {
        switch(currentState) {
            case INIT: 
                firstRounds();
                Debug.setIndicatorString("INIT state");
                if (robotCounter == 3) {currentState = State.CHILLING;}
                break;
            case CHILLING:
                if(minerCount <= MIN_NUM_MINERS) {
                    switch(chillingCounter) {
                        case 0: 
                            Debug.setIndicatorString("Trying to build a miner");
                            if(buildRobot(RobotType.MINER, Util.randomDirection())){
                                chillingCounter ++;
                            }
                            break;
                        case 1:
                            Debug.setIndicatorString("Trying to build a soldier");
                            if(buildRobot(RobotType.SOLDIER, Util.randomDirection())){
                                chillingCounter = 0;
                            }
                            break;
                    }
                }

                Debug.setIndicatorString("CHILLING state");
                break;
            default: 
                currentState = State.CHILLING;
                break;
        }
    }
    public void firstRounds() throws GameActionException {
        RobotType toBuild = null;
        switch(robotCounter) {
            case 0: case 1: case 2: 
                toBuild = RobotType.MINER;
                break;
        }
        Direction dir = null;
        if(leadSource == null) {
            dir = Util.randomDirection(nonWallDirections);
        }
        else {
            dir = rc.getLocation().directionTo(leadSource);
        }
        buildRobot(toBuild,dir);
    }
    
}
