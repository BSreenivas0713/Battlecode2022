package MPBasic;

import battlecode.common.*;

import javax.tools.DocumentationTool.Location;

import MPBasic.Debug.*;
import MPBasic.Util.*;
import MPBasic.Comms.*;

public class Archon extends Robot {
    static enum State {
        CHILLING,
        INIT,
    };
    static int robotCounter;
    static State currentState;
    static int flagIndex;
    static MapLocation leadSource;
    static Direction[] nonWallDirections;

    public Archon(RobotController r) throws GameActionException {
        super(r);
        //writing all Archon locations immediately on round 0
        int nextArchon = Comms.incrementFriendly();
        int myLocFlag = Comms.encodeLocation();
        r.writeSharedArray(nextArchon, myLocFlag);
        flagIndex = nextArchon + Comms.mapLocToFlag;
        currentState = getInitialState();
        findBestLeadSource();
        nonWallDirections = findnonWallDirections();
        System.out.println("nonWallDirections: " + nonWallDirections.toString());
    }
    public void findBestLeadSource() throws GameActionException{
        leadSource = null;
        int bestLeadSource = -1;
        for(MapLocation loc: rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), RobotType.ARCHON.visionRadiusSquared)) {
            int lead_amount = rc.senseLead(loc);
            if (lead_amount != 0 && lead_amount > bestLeadSource){
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
            }
            robotCounter += 1;
            return true;
        }
        return false;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        doStateAction();

    }
    public void doStateAction() throws GameActionException {
        switch(currentState) {
            case INIT: 
                firstRounds();
                if (robotCounter == 3) {currentState = State.CHILLING;}
                break;
            case CHILLING:
                // Pick a direction to build in.
                if (Util.rng.nextBoolean()) {
                    // Let's try to build a miner.
                    Debug.setIndicatorString("Trying to build a miner");
                    buildRobot(RobotType.MINER, Util.randomDirection());
                } else {
                    // Let's try to build a soldier.
                    Debug.setIndicatorString("Trying to build a soldier");
                    buildRobot(RobotType.SOLDIER, Util.randomDirection());
                }
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
