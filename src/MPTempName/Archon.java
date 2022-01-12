package MPTempName;

import battlecode.common.*;

import MPTempName.Debug.*;
import MPTempName.Util.*;
import MPTempName.Comms.*;
import java.util.ArrayDeque;

public class Archon extends Robot {
    static enum State {
        CHILLING,
        OBESITY,
        UNDER_ATTACK,
        INIT,
    };

    static int MAX_NUM_MINERS;
    static int MIN_NUM_MINERS;

    static int robotCounter;
    static int chillingCounter;
    static int obesityCounter; 
    static int minerCount;
    static int minerMiningCount;
    static int numMinersBuilt;
    static int minerDirOffset;
    static int soldierCount;
    static int builderCount;
    static State currentState;
    static int flagIndex;
    static int archonNumber;
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
    static int distressSemaphore = 0;
    static MapLocation closestLeadOre;
    static Buildable currentBuild = Buildable.MINER;
    static Buildable nextBuild = Buildable.MINER;

    public Archon(RobotController r) throws GameActionException {
        super(r);
        //writing all Archon locations immediately on round 0
        stateStack = new ArrayDeque<State>();
        MAX_NUM_MINERS = Math.min(Util.MAX_MINERS,
                                    rc.getMapWidth() * rc.getMapHeight() /
                                    Util.MAX_MAP_SIZE_TO_MINER_RATIO);
        MIN_NUM_MINERS = MAX_NUM_MINERS / 5;
        Debug.println("Min number of miners: " + MIN_NUM_MINERS);
        int nextArchon = Comms.incrementFriendly();
        int myLocFlag = Comms.encodeLocation();
        r.writeSharedArray(nextArchon, myLocFlag);
        flagIndex = nextArchon + Comms.mapLocToFlag;
        homeFlagIdx = flagIndex;
        changeState(State.INIT);
        archonNumber = nextArchon;
        leadNeededByBuilders = 0;
        percentLeadToTake = Util.leadPercentage(rc.getArchonCount(), nextArchon, 0);
        soldierCount = 0;
        builderCount = 0;
        findBestLeadSource();
        nonWallDirections = findnonWallDirections();
        maxLeadUsedByArchons = 75 * ((1 + rc.getArchonCount()) - archonNumber);
        leadObesity = rc.getArchonCount() * 180 + maxLeadUsedByArchons;
        minerDirOffset = Util.rng.nextInt(8);
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
        Comms.encodeBuildGuess(archonNumber, currentBuild);
        Direction[] orderedDirs = Util.getOrderedDirections(mainDir);
        if (!Comms.canBuildPrioritized(archonNumber, currentState == State.INIT)) {
            Debug.printString("Not my turn.");
            return false;
        } else {
            Debug.printString("My turn.");
        }
        for(Direction dir : orderedDirs) {
            if (rc.canBuildRobot(toBuild, dir)){
                if (Comms.getTurn() != rc.getArchonCount()) {
                    Comms.useLead(toBuild);
                }
                Comms.encodeBuildGuess(archonNumber, nextBuild);
                rc.buildRobot(toBuild, dir);
                RobotInfo robot = rc.senseRobotAtLocation(rc.getLocation().add(dir));
                //in future, add info about this new robot to maps
                if (robot == null) {
                    System.out.println("CRITICAL: EC didn't find the robot it just built");
                }
                robotCounter += 1;
                Comms.incrementBuiltRobots(archonNumber, robotCounter);
                return true;
            }
        }
        return false;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        // System.out.println(Comms.getTurn());
        if (Comms.getTurn() == 1) {
            Comms.resetAlive();
            Comms.clearUsedLead();
            Comms.clearBuildGuesses();
        }
        Comms.setAlive(archonNumber);
        Comms.setCanBuild(archonNumber, rc.isActionReady());
        clearAndResetHelpers();
        Comms.resetAvgEnemyLoc();
        boolean underAttack = false;
        updateLead();
        updateRobotCounts();
        updateClosestLeadOre();
        boolean isObese = checkForObesity();
        toggleState(underAttack, isObese);
        doStateAction();
        tryToRepair();
        Comms.advanceTurn();
        // Debug.setIndicatorString(leadToUse + "; " + robotCounter + "; num alive enemies: " + Comms.aliveEnemyArchonCount());
        // if (Comms.enemyArchonCount() > 0) {
        //     System.out.println(rc.readSharedArray(Comms.firstEnemy) + "; " + rc.readSharedArray(Comms.firstEnemy + 1) + "; " + rc.readSharedArray(Comms.firstEnemy + 2) + "; " + rc.readSharedArray(Comms.firstEnemy + 3));
        // }
    }


    // pick a random lead ore for  a miner to go to, if spawned on this turn
    //
    public void updateClosestLeadOre() throws GameActionException{
        MapLocation[] locs = rc.senseNearbyLocationsWithLead(visionRadiusSquared);
        double bestLeadScore = Integer.MIN_VALUE;
        MapLocation bestLeadLoc = null;
        for(MapLocation loc: locs) {
            double currScore = getLeadDistTradeoffScore(loc.distanceSquaredTo(currLoc), rc.senseLead(loc));
            if(currScore > bestLeadScore && rc.senseLead(loc) > 1 && !loc.equals(currLoc)) {
                bestLeadScore = currScore;
                bestLeadLoc = loc;
            }
        }
        if (bestLeadLoc != null) {
            closestLeadOre = bestLeadLoc;
        }
        else {
            closestLeadOre = currLoc.add(Util.randomDirection());
        }
        Debug.printString("ore: " + closestLeadOre);
    }

    public boolean shouldCallForHelp() throws GameActionException {
        // int numFriendlies = 0;
        // for (RobotInfo robot : FriendlySensable) {
        //     switch (robot.getType()) {
        //         case SOLDIER: case WATCHTOWER: case SAGE: 
        //             numFriendlies++;
        //             break;
        //         default:
        //             break;
        //     }
        // }
        // int numEnemies = 0;
        // for (RobotInfo robot : EnemySensable) {
        //     switch (robot.getType()) {
        //         case SOLDIER: case WATCHTOWER: case SAGE: 
        //             numEnemies++;
        //             break;
        //         default:
        //             break;
        //     }
        // }
        // stop calling for help if health is below 7.5%
        if (rc.getHealth() < rc.getType().getMaxHealth(1) * 0.075) {
            return false;
            // if (numEnemies > 2 * numFriendlies) {
            //     return false;
            // }
        }
        return true;
    }

    public boolean broadcastSoldierNear() throws GameActionException {
        if (shouldCallForHelp()) {
            for(RobotInfo robot: rc.senseNearbyRobots(rc.getType().visionRadiusSquared)) {
                if (robot.type == RobotType.SOLDIER && robot.team == rc.getTeam().opponent()) {
                    distressSemaphore = 10;
                    return true;
                }
            }
            if (Comms.getICFromFlag(rc.readSharedArray(flagIndex)) == Comms.InformationCategory.UNDER_ATTACK) {
                if (distressSemaphore == 0) {
                    return false;
                } else {
                    distressSemaphore--;
                    return true;
                }
            }
            return false;
        } else {
            distressSemaphore = 0;
            return false;
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
        if ((soldierCount >= Util.SOLDIERS_NEEDED_TO_RUSH) && Comms.aliveEnemyArchonCount() > 0) {
            //tell soldiers near me to rush
            // nextSoldierFlag = Comms.encodeSoldierStateFlag(Comms.SoldierStateCategory.RUSH_SOLDIERS);
        }
        minerMiningCount = Comms.getMinerMiningCount();
        builderCount = Comms.getBuilderCount();
        lastPayDay += 1;
        if (minerCount <= minerMiningCount) {
            lastPayDay = 0;
        }
    }

    public int buildMiner(int counter) throws GameActionException {
        if (minerCount < MAX_NUM_MINERS) {
            Debug.printString("Building miner");
            if(buildRobot(RobotType.MINER, currLoc.directionTo(closestLeadOre))){
                counter++;
                numMinersBuilt++;
                nextFlag = Comms.encodeArchonFlag(Comms.InformationCategory.DIRECTION, Util.exploreDirections[(numMinersBuilt + minerDirOffset) % 8]);
            }
        }
        else {
            Debug.printString("Building soldier");
            if(buildRobot(RobotType.SOLDIER, Util.randomDirection())){
                counter++;
            }
        }
        return counter;
    }

    public int buildSoldier(int counter) throws GameActionException {
        Debug.printString("Building soldier");
        if(buildRobot(RobotType.SOLDIER, Util.randomDirection())){
            counter++;
        }
        return counter;
    }

    public int buildBuilder(int counter) throws GameActionException {
        Debug.printString("Building builder, num builders: " + builderCount);
        if(buildRobot(RobotType.BUILDER, Util.randomDirection())){
            counter++;
        }
        return counter;
    }

    public int minerSoldier31(int counter) throws GameActionException {
        switch(counter % 3) {
            case 0: 
                currentBuild = Buildable.SOLDIER;
                nextBuild = Buildable.SOLDIER;
                counter = buildSoldier(counter);
                break;
            case 1:
                currentBuild = Buildable.SOLDIER;
                nextBuild = Buildable.MINER;
                counter = buildSoldier(counter);
                break;
            default:
                currentBuild = Buildable.MINER;
                nextBuild = Buildable.SOLDIER;
                counter = buildMiner(counter);
                break;
        }
        return counter;
    }

    public int minerSoldierRatio(int mod, int counter) throws GameActionException {
        switch(counter % mod) {
            case (0):
                currentBuild = Buildable.MINER;
                nextBuild = Buildable.SOLDIER;
                counter = buildMiner(counter);
                break;
            default:
                currentBuild = Buildable.SOLDIER;
                if (counter % mod == mod - 1) {
                    nextBuild = Buildable.MINER;
                } else {
                    nextBuild = Buildable.SOLDIER;
                }
                counter = buildSoldier(counter);
                break;
        }
        return counter;
    }

    public int SoldierBuilderRatio(int mod, int counter) throws GameActionException {
        switch(counter % mod) {
            case 0:
                currentBuild = Buildable.BUILDER;
                nextBuild = Buildable.SOLDIER;
                counter = buildBuilder(counter);
                break;
            default:
                currentBuild = Buildable.SOLDIER;
                if (counter % mod == mod - 1) {
                    nextBuild = Buildable.BUILDER;
                } else {
                    nextBuild = Buildable.SOLDIER;
                }
                counter = buildSoldier(counter);
                break;

        }
        return counter;
    }
    public void doStateAction() throws GameActionException {
        switch(currentState) {
            case INIT:
                Debug.printString("Init");
                /*if (leadToUse < Util.LeadThreshold) {
                    break;
                }*/
                firstRounds();
                break;
            case CHILLING:
                Debug.printString("Chilling");
                /*if (leadToUse < Util.LeadThreshold) {
                    break;
                }*/
                if(minerCount <= MIN_NUM_MINERS && soldierCount >= (1/3) * minerCount) {
                    chillingCounter = minerSoldier31(chillingCounter);
                }
                else {
                    chillingCounter = minerSoldierRatio(7, chillingCounter);
                }
                // Debug.printString("CHILLING state, last pay day: " + lastPayDay);
                break;
            case UNDER_ATTACK:
                Debug.printString("Under Attack");
                chillingCounter = buildSoldier(chillingCounter);
                break;
            case OBESITY:
                Debug.printString("Obesity");
                int leadForBuilders = rc.getTeamLeadAmount(rc.getTeam()) - maxLeadUsedByArchons;
                int watchtowersPossible = leadForBuilders / 180;
                if (/*watchtowersPossible > builderCount &&*/ builderCount <= MIN_NUM_MINERS) {
                    obesityCounter = SoldierBuilderRatio(11, obesityCounter);
                } else {
                    obesityCounter = buildSoldier(obesityCounter);
                    break;
                }
                break;
            default: 
                changeState(State.CHILLING);
                break;
        }
    }

    public void firstRounds() throws GameActionException {
        RobotType toBuild = RobotType.MINER;
        Direction dir = currLoc.directionTo(closestLeadOre);
        currentBuild = Buildable.MINER;
        nextBuild = Buildable.MINER;
        if(buildRobot(toBuild,dir)) {
            numMinersBuilt++;
            nextFlag = Comms.encodeArchonFlag(Comms.InformationCategory.DIRECTION, Util.exploreDirections[(numMinersBuilt + minerDirOffset) % 8]);
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
        percentLeadToTake = Util.leadPercentage(rc.getArchonCount(), archonNumber, builderPercentage);
        leadToUse = (int) (availableLead * (percentLeadToTake));
        if (leadToUse < Util.LeadThreshold) {
            if (Comms.getArchonWithLeastFirstRoundBuilt() == archonNumber) {
                leadToUse = (int) (availableLead * (1.0 - builderPercentage));
            } else {
                leadToUse = 0;
            }
        }
    }

    public void toggleState(boolean underAttack, boolean isObese) throws GameActionException {
        switch (currentState) {
            case INIT:
                Debug.printString("lead obesity: " + leadObesity);
                if((robotCounter >= 3 && Comms.foundEnemy) || minerCount >= MIN_NUM_MINERS) {
                    changeState(State.CHILLING);
                }
                else if(rc.getTeamLeadAmount(rc.getTeam()) > leadObesity) {
                    stateStack.push(State.CHILLING);
                    changeState(State.OBESITY);
                }
                break;
            case UNDER_ATTACK:
                if (!underAttack) {
                    changeState(stateStack.pop());
                }
                break;
            case CHILLING:
                if (underAttack) {
                    stateStack.push(currentState);
                    changeState(State.UNDER_ATTACK);
                } else if (isObese) {
                    stateStack.push(currentState);
                    changeState(State.OBESITY);
                }
                break;
            case OBESITY:
                if (underAttack) {
                    stateStack.push(currentState);
                    changeState(State.UNDER_ATTACK);
                } else if (rc.getTeamLeadAmount(rc.getTeam()) < leadObesity) {
                    changeState(stateStack.pop());
                }
                break;
            default:
                break;
        }
    }
    
    public void changeState(State newState) throws GameActionException {
        currentState = newState;
        InformationCategory ic;
        switch (newState) {
            case UNDER_ATTACK:
                ic = InformationCategory.UNDER_ATTACK;
                break;
            default: // CHILLING, OBESITY, INIT
                ic = InformationCategory.EMPTY;
                break;
        }
        int flag = Comms.encodeArchonFlag(ic);
        Comms.writeIfChanged(flagIndex, flag);
    }

    public boolean checkForObesity() throws GameActionException {
        return rc.getTeamLeadAmount(rc.getTeam()) > leadObesity;
    }

    public void clearAndResetHelpers() throws GameActionException {
        Comms.writeIfChanged(Comms.FIRST_HELPER_COUNTER, 0);
        Comms.writeIfChanged(Comms.SECOND_HELPER_COUNTER, 0);
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