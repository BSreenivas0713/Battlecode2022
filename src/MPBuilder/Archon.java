package MPBuilder;

import battlecode.common.*;

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
        currentState = State.INIT;
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
                Comms.incrementBuiltRobots(turnNumber, robotCounter);
                return true;
            }
        }
        return false;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        clearAndResetHelpers();
        broadcastSoldierNear();
        updateLead();
        updateRobotCounts();
        checkForObesity();
        doStateAction();
        tryToRepair();
        Debug.setIndicatorString(leadToUse + "; " + robotCounter + "; num alive enemies: " + Comms.aliveEnemyArchonCount());
        // if (Comms.enemyArchonCount() > 0) {
        //     System.out.println(rc.readSharedArray(Comms.firstEnemy) + "; " + rc.readSharedArray(Comms.firstEnemy + 1) + "; " + rc.readSharedArray(Comms.firstEnemy + 2) + "; " + rc.readSharedArray(Comms.firstEnemy + 3));
        // }
    }

    public boolean shouldCallForHelp() throws GameActionException {
        int numFriendlies = FriendlySensable.length;
        int numEnemies = EnemySensable.length;
        if (rc.getHealth() < rc.getType().getMaxHealth(1) * 0.1) {
            if (numEnemies > 2 * numFriendlies) {
                return false;
            }
        }
        return true;
    }

    public void broadcastSoldierNear() throws GameActionException {
        boolean callForHelp = shouldCallForHelp();
        for(RobotInfo robot: rc.senseNearbyRobots(rc.getType().visionRadiusSquared)) {
            if (robot.type == RobotType.SOLDIER && robot.team == rc.getTeam().opponent() && callForHelp) {
                int newFlag = Comms.encodeArchonFlag(Comms.InformationCategory.UNDER_ATTACK);
                rc.writeSharedArray(flagIndex, newFlag);
                return;
            }
        }
        if(Comms.getICFromFlag(rc.readSharedArray(flagIndex)) == Comms.InformationCategory.UNDER_ATTACK || !callForHelp) {
            int newFlag = Comms.encodeArchonFlag(Comms.InformationCategory.EMPTY);
            rc.writeSharedArray(flagIndex, newFlag);
        }
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
        soldierCount = Comms.getRushSoldierCount();
        if ((soldierCount >= Util.SOLDIERS_NEEDED_TO_RUSH || (rc.getRoundNum() < 200 && Comms.getRushSoldierCount() >= 10)) &&
            Comms.aliveEnemyArchonCount() > 0) {
            //tell soldiers near me to rush
            nextSoldierFlag = Comms.encodeSoldierStateFlag(Comms.SoldierStateCategory.RUSH_SOLDIERS);
        }
        minerMiningCount = Comms.getMinerMiningCount();
        builderCount = Comms.getBuilderCount();
        lastPayDay += 1;
        if (minerCount <= minerMiningCount) {
            lastPayDay = 0;
        }
    }
    public int minerSoldier5050(int counter) throws GameActionException {
        switch(counter % 2) {
            case 0: 
                if (minerCount < MAX_NUM_MINERS) { 
                    Debug.setIndicatorString("Trying to build a miner");
                    if(buildRobot(RobotType.MINER, Util.randomDirection())){
                        counter ++;
                    }
                }
                else {
                    Debug.setIndicatorString("Trying to build a soldier");
                    if(buildRobot(RobotType.SOLDIER, Util.randomDirection())){
                        counter ++;
                    }                    
                }
                break;
            case 1:
                Debug.setIndicatorString("Trying to build a soldier");
                if(buildRobot(RobotType.SOLDIER, Util.randomDirection())){
                    counter ++;
                }
                break;
        }
        return counter;
    }
    public int minerSoldier13Ratio(int counter) throws GameActionException {
        switch(counter % 4) {
            case 0:
                if (minerCount < MAX_NUM_MINERS) { 
                    Debug.setIndicatorString("Trying to build a miner");
                    if(buildRobot(RobotType.MINER, Util.randomDirection())){
                        counter ++;
                    }
                }
                else {
                    Debug.setIndicatorString("Trying to build a soldier");
                    if(buildRobot(RobotType.SOLDIER, Util.randomDirection())){
                        counter ++;
                    }                    
                }
                break;
            case 1: case 2: case 3: 
                Debug.setIndicatorString("Trying to build a soldier");
                if(buildRobot(RobotType.SOLDIER, Util.randomDirection())){
                    counter ++;
                }
                break;
        }
        return counter;
    }
    public int SoldierBuilder11Ratio(int counter) throws GameActionException {
        switch(counter % 2) {
            case 0:
                Debug.setIndicatorString("Trying to build a builder, num builders: " + builderCount);
                if(buildRobot(RobotType.BUILDER, Util.randomDirection())){
                    counter++;
                }
                break;
            case 1: 
                Debug.setIndicatorString("Trying to build a soldier");
                if(buildRobot(RobotType.SOLDIER, Util.randomDirection())){
                    counter++;
                }
                break;

        }
        return counter;
    }
    public void doStateAction() throws GameActionException {
        switch(currentState) {
            case INIT: 
                if (leadToUse < Util.LeadThreshold) {
                    break;
                }
                firstRounds();
                break;
            case CHILLING:
                if (leadToUse < Util.LeadThreshold) {
                    break;
                }
                if(minerCount <= MIN_NUM_MINERS) {
                    chillingCounter = minerSoldier5050(chillingCounter);
                }
                else {
                    chillingCounter = minerSoldier13Ratio(chillingCounter);
                }
                // Debug.setIndicatorString("CHILLING state, last pay day: " + lastPayDay);
                break;
            case OBESITY:
                int leadForBuilders = rc.getTeamLeadAmount(rc.getTeam()) - maxLeadUsedByArchons;
                int watchtowersPossible = leadForBuilders / 180;
                if (watchtowersPossible > builderCount && builderCount <= MIN_NUM_MINERS) {
                    obesityCounter = SoldierBuilder11Ratio(obesityCounter);
                } else {
                        Debug.setIndicatorString("Trying to build a soldier");
                        if(buildRobot(RobotType.SOLDIER, Util.randomDirection())){
                            obesityCounter++;
                        }
                        break;
                }
                break;
            default: 
                changeState(State.CHILLING);
                break;
        }
    }
    public void firstRounds() throws GameActionException {
        Debug.setIndicatorString("INIT state");
        RobotType toBuild = RobotType.MINER;
        Direction dir = null;
        // if(leadSource == null) {
        //     dir = Util.randomDirection(nonWallDirections);
        // }
        // else {
        //     dir = rc.getLocation().directionTo(leadSource);
        // }
        dir = Util.randomDirection(nonWallDirections);
        buildRobot(toBuild,dir);
        
        if(robotCounter >= 3 && Comms.foundEnemy) {
            changeState(State.CHILLING);
        }
        else if(rc.getTeamLeadAmount(rc.getTeam()) > leadObesity) {
            stateStack.push(State.CHILLING);
            changeState(State.OBESITY);
        }

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
            if (Comms.getArchonWithLeastFirstRoundBuilt() == turnNumber) {
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
    public void clearAndResetHelpers() throws GameActionException {
        rc.writeSharedArray(Comms.FIRST_HELPER_COUNTER, 0);
        rc.writeSharedArray(Comms.SECOND_HELPER_COUNTER, 0);
        int count = Comms.getRushSoldierCount();
        int newMax = count / rc.getArchonCount();
        Comms.setMaxHelper(newMax);
    }

    // Tries to repair the lowest health droid in range if an action is ready.
    public void tryToRepair() throws GameActionException {
        if(!rc.isActionReady()) {
            return;
        }

        RobotInfo[] friendlies = rc.senseNearbyRobots(actionRadiusSquared, rc.getTeam());
        RobotInfo maybeSage = null;
        RobotInfo maybeSoldier = null;
        RobotInfo maybeBuilder = null;
        RobotInfo maybeMiner = null;
        RobotInfo friendly = null;
        for (int i = friendlies.length - 1; i >= 0; i--) {
            friendly = friendlies[i];
            switch(friendly.type) {
                case SAGE:
                    if(maybeSage == null || maybeSage.health > friendly.health) {
                        maybeSage = friendly;
                    }
                    break;
                case SOLDIER:
                    if(maybeSoldier == null || maybeSoldier.health > friendly.health) {
                        maybeSoldier = friendly;
                    }
                    break;
                case BUILDER:
                    if(maybeBuilder == null || maybeBuilder.health > friendly.health) {
                        maybeBuilder = friendly;
                    }
                    break;
                case MINER:
                    if(maybeMiner == null || maybeMiner.health > friendly.health) {
                        maybeMiner = friendly;
                    }
                    break;
                default:
                    break;
            }
        }

        if(maybeSage != null && rc.canRepair(maybeSage.location)) rc.repair(maybeSage.location);
        if(maybeSoldier != null && rc.canRepair(maybeSoldier.location)) rc.repair(maybeSoldier.location);
        if(maybeBuilder != null && rc.canRepair(maybeBuilder.location)) rc.repair(maybeBuilder.location);
        if(maybeMiner != null && rc.canRepair(maybeMiner.location)) rc.repair(maybeMiner.location);
    }
}
