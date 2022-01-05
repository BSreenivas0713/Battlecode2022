package MPBuilder;

import battlecode.common.*;

import javax.tools.DocumentationTool.Location;

import MPBuilder.Debug.*;
import MPBuilder.Util.*;
import MPBuilder.Comms.*;
import java.util.ArrayDeque;

public class Archon extends Robot {
    static enum State {
        CHILLING,
        OBESITY,
        INIT,
    };

    static int MAX_NUM_MINERS;
    static int MIN_NUM_MINERS;

    static int robotCounter;
    static int chillingCounter;
    static int obesityCounter; 
    static int minerCount;
    static int minerMiningCount;
    static int soldierCount;
    static int builderCount;
    static State currentState;
    static int flagIndex;
    static int turnNumber;
    static double percentLeadToTake;
    static ArrayDeque<State> stateStack;
    static int leadToUse;
    static int leadNeededByBuilders;
    static int lastPayDay;
    static MapLocation leadSource;
    static Direction[] nonWallDirections;
    static int soldiersNearby = 0;
    static int leadObesity;
    static int maxLeadUsedByArchons;

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
        homeFlagIdx = flagIndex;
        currentState = getInitialState();
        Comms.updateState(nextArchon, currentState.ordinal());
        turnNumber = nextArchon;
        leadNeededByBuilders = 0;
        percentLeadToTake = Util.leadPercentage(rc.getArchonCount(), nextArchon, 0);
        soldierCount = 0;
        builderCount = 0;
        findBestLeadSource();
        nonWallDirections = findnonWallDirections();
        maxLeadUsedByArchons = 75 * (5 - turnNumber);
        leadObesity = rc.getArchonCount() * 180 + maxLeadUsedByArchons;
        
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
        if (toBuild == RobotType.SOLDIER) {
            soldierCount++;
            if (soldierCount % 3 == 0) {
                nextFlag = Comms.encodeArchonFlag(InformationCategory.DEFENSE_SOLDIERS);
            }
        }
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
        updateLead();
        updateRobotCounts();
        checkForObesity();
        doStateAction();
        // if (Comms.enemyArchonCount() > 0) {
        //     System.out.println(rc.readSharedArray(Comms.firstEnemy) + "; " + rc.readSharedArray(Comms.firstEnemy + 1) + "; " + rc.readSharedArray(Comms.firstEnemy + 2) + "; " + rc.readSharedArray(Comms.firstEnemy + 3));
        // }
    }
    public void updateRobotCounts() throws GameActionException {
        minerCount = Comms.getMinerCount();
        // update soldiers within sensing radius count
        soldiersNearby = 0;
        for (RobotInfo friend : FriendlySensable) {
            if (friend.getType() == RobotType.SOLDIER) {
                soldiersNearby++;
            }
        }
        if (Comms.getRushSoldierCount() >= Util.SOLDIERS_NEEDED_TO_RUSH &&
            Comms.enemyArchonCount() > 0) {
            //tell soldiers near me to rush
            nextFlag = Comms.encodeArchonFlag(Comms.InformationCategory.RUSH_SOLDIERS);
        }
        minerMiningCount = Comms.getMinerMiningCount();
        builderCount = Comms.getBuilderCount();
        lastPayDay += 1;
        if (minerCount <= minerMiningCount) {
            lastPayDay = 0;
        }
    }

    public void doStateAction() throws GameActionException {
        switch(currentState) {
            case INIT: 
                firstRounds();
                Debug.setIndicatorString("INIT state");
                if (robotCounter == 3) {currentState = State.CHILLING;}
                break;
            case CHILLING:
                if (leadToUse < Util.LeadThreshold) {
                    break;
                }
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
                else {
                    if (lastPayDay <= 30) {
                        switch(chillingCounter) {
                            case 0:
                                Debug.setIndicatorString("Trying to build a miner");
                                if(buildRobot(RobotType.MINER, Util.randomDirection())){
                                    chillingCounter ++;
                                }
                                break;
                            case 1:  case 2:
                                Debug.setIndicatorString("Trying to build a soldier");
                                if(buildRobot(RobotType.SOLDIER, Util.randomDirection())){
                                    chillingCounter = 0;
                                }
                                break;
                        }
                    }
                    else {
                        switch(chillingCounter) {
                            case 0: 
                                Debug.setIndicatorString("Trying to build a miner");
                                if(buildRobot(RobotType.MINER, Util.randomDirection())){
                                    chillingCounter ++;
                                }
                                break;
                            case 1: case 2:
                                Debug.setIndicatorString("Trying to build a soldier");
                                if(buildRobot(RobotType.SOLDIER, Util.randomDirection())){
                                    chillingCounter = (chillingCounter + 1) % 3;
                                }
                                break;
                        }
                    }
                }
                Debug.setIndicatorString("CHILLING state, last pay day: " + lastPayDay);
                break;
            case OBESITY:
                int leadForBuilders = rc.getTeamLeadAmount(rc.getTeam()) - maxLeadUsedByArchons;
                int watchtowersPossible = leadForBuilders / 180;
                if (watchtowersPossible > builderCount) {
                    switch(obesityCounter % 4) {
                        case 0:
                            Debug.setIndicatorString("Trying to build a miner");
                            if(buildRobot(RobotType.MINER, Util.randomDirection())){
                                obesityCounter++;
                            }
                            break;
                        case 1: 
                            Debug.setIndicatorString("Trying to build a soldier");
                            if(buildRobot(RobotType.SOLDIER, Util.randomDirection())){
                                obesityCounter++;
                            }
                            break;
                        case 2: case 3:
                            Debug.setIndicatorString("Trying to build a builder");
                            if(buildRobot(RobotType.BUILDER, Util.randomDirection())){
                                obesityCounter++;
                            }
                            break;
                    }
                } else {
                    switch(obesityCounter % 3) {
                        case 0:
                            Debug.setIndicatorString("Trying to build a miner");
                            if(buildRobot(RobotType.MINER, Util.randomDirection())){
                                obesityCounter++;
                            }
                            break;
                        case 1: 
                            Debug.setIndicatorString("Trying to build a soldier");
                            if(buildRobot(RobotType.SOLDIER, Util.randomDirection())){
                                obesityCounter++;
                            }
                            break;
                        case 2:
                            Debug.setIndicatorString("Trying to build a builder");
                            if(buildRobot(RobotType.BUILDER, Util.randomDirection())){
                                obesityCounter++;
                            }
                            break;
                    }
                }
                break;
            default: 
                changeState(State.CHILLING);
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

    public void updateLead() throws GameActionException {
        double availableLead = (double) rc.getTeamLeadAmount(rc.getTeam());
        leadNeededByBuilders = (int) (availableLead - maxLeadUsedByArchons);
        if (leadNeededByBuilders <= 0) {leadNeededByBuilders = 0;}
        if (availableLead == 0) {
            leadToUse = 0;
            return;
        }
        double builderPercentage = ((double) leadNeededByBuilders) / availableLead;
        percentLeadToTake = Util.leadPercentage(rc.getArchonCount(), turnNumber, builderPercentage);
        leadToUse = (int) (availableLead * (percentLeadToTake));
        if (leadToUse < Util.LeadThreshold) {
            if (rc.getRoundNum() % rc.getArchonCount() == turnNumber - 1) {
                leadToUse = (int) (availableLead * (1.0 - builderPercentage));
            } else {
                leadToUse = 0;
            }
        }
    }
    
    public void changeState(State newState) throws GameActionException {
        currentState = newState;
        Comms.updateState(turnNumber, newState.ordinal());
    }
    public void checkForObesity() throws GameActionException {
        if(currentState == State.CHILLING && rc.getTeamLeadAmount(rc.getTeam()) > leadObesity) {
            stateStack.push(currentState);
            changeState(State.OBESITY);
        }
        else if(currentState == State.OBESITY && rc.getTeamLeadAmount(rc.getTeam()) < 500) {
            State oldState = stateStack.pop();
            changeState(oldState);
        }
    }
}
