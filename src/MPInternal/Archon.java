package MPInternal;

import battlecode.common.*;

import MPInternal.Debug.*;
import MPInternal.Util.*;
import MPInternal.Comms.*;
import java.util.ArrayDeque;

public class Archon extends Robot {
    static enum State {
        CHILLING,
        OBESITY,
        UNDER_ATTACK,
        INIT,
    };

    static Direction[] BUILD_DIRECTIONS;
    static Direction[] EXPLORE_DIRECTIONS;

    static int MAX_NUM_MINERS;
    static int MIN_NUM_MINERS;

    static int robotCounter;
    static int chillingCounter;
    static int obesityCounter; 
    static int minerCount;
    static int soldierCount;
    static int builderCount;

    static int numMinersBuilt;
    static int minerDirOffset;

    static State currentState;
    static int flagIndex;
    static int archonNumber;
    static ArrayDeque<State> stateStack;

    static int leadNeededByBuilders;
    
    static int leadObesity;
    static int maxLeadUsedByArchons;
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

        archonNumber = Comms.incrementFriendly();
        int myLocFlag = Comms.encodeLocation();
        r.writeSharedArray(archonNumber, myLocFlag);
        homeFlagIdx = archonNumber + Comms.mapLocToFlag;
        changeState(State.INIT);
        leadNeededByBuilders = 0;
        soldierCount = 0;
        builderCount = 0;
        maxLeadUsedByArchons = 75 * ((1 + rc.getArchonCount()) - archonNumber);
        leadObesity = rc.getArchonCount() * 180 + maxLeadUsedByArchons;
        minerDirOffset = Util.rng.nextInt(8);
        loadBuildDirections();
        pruneExploreDirections();
    }

    // Loads build directions in order of increasing rubble, randomly breaking ties.
    public void loadBuildDirections() throws GameActionException {
        boolean[] usedDir = new boolean[Util.directions.length];
        Direction[] dirs = new Direction[Util.directions.length];
        int numDirections = 0;
        int rubble;
        int minRubble;
        int numEqual;
        int bestDir;
        MapLocation loc;
        for(int i = 0; i < Util.directions.length; i++) {
            minRubble = 101;
            bestDir = -1;
            numEqual = 0;
            for(int j = 0; j < Util.directions.length; j++) {
                loc = rc.adjacentLocation(Util.directions[j]);
                if(usedDir[j] || !rc.onTheMap(loc)) continue;
                rubble = rc.senseRubble(loc);
                if(rubble < minRubble) {
                    minRubble = rubble;
                    bestDir = j;
                } else if(rubble == minRubble) {
                    numEqual++;
                    if(Util.rng.nextInt(numEqual) == 0) {
                        minRubble = rubble;
                        bestDir = j;
                    }
                }
            }

            if(bestDir != -1) {
                usedDir[bestDir] = true;
                dirs[numDirections++] = Util.directions[bestDir];
            }
        }

        BUILD_DIRECTIONS = new Direction[numDirections];
        System.arraycopy(dirs, 0, BUILD_DIRECTIONS, 0, numDirections);
    }

    void pruneExploreDirections() throws GameActionException {
        Direction[] validExploreDirs = new Direction[8];
        int numValidDirs = 0;
        boolean[] isValidDir = new boolean[9];
        for(Direction dir : Util.directions) {
            if(Explore.isValidExploreDir(dir)) {
                validExploreDirs[numValidDirs++] = dir;
                isValidDir[dir.ordinal()] = true;
            }
        }

        EXPLORE_DIRECTIONS = new Direction[numValidDirs];
        numValidDirs = 0;
        for(Direction dir : Util.exploreDirectionsOrder) {
            if(isValidDir[dir.ordinal()]) {
                EXPLORE_DIRECTIONS[numValidDirs++] = dir;
                Debug.printString(dir.toString());
            }
        }
    }

    public boolean buildRobot(RobotType toBuild) throws GameActionException {
        return buildRobot(toBuild, BUILD_DIRECTIONS);
    }

    public boolean buildRobot(RobotType toBuild, Direction mainDir) throws GameActionException {
        return buildRobot(toBuild, Util.getOrderedDirections(mainDir));
    }

    public boolean buildRobot(RobotType toBuild, Direction[] orderedDirs) throws GameActionException {
        Comms.encodeBuildGuess(archonNumber, currentBuild);
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
            Comms.drawClusterDots();
        }
        Comms.setAlive(archonNumber);
        Comms.setCanBuild(archonNumber, rc.isActionReady());
        Comms.resetAvgEnemyLoc();
        reportEnemies();
        boolean underAttack = false;
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
        MapLocation[] locs = rc.senseNearbyLocationsWithLead(-1, 2);
        double bestLeadScore = Integer.MIN_VALUE;
        MapLocation bestLeadLoc = null;
        for(MapLocation loc: locs) {
            double currScore = getLeadDistTradeoffScore(loc.distanceSquaredTo(currLoc), rc.senseLead(loc));
            if(currScore > bestLeadScore && !loc.equals(currLoc)) {
                bestLeadScore = currScore;
                bestLeadLoc = loc;
            }
        }
        if (bestLeadLoc != null) {
            closestLeadOre = bestLeadLoc;
            Debug.printString("ore: " + closestLeadOre);
        } else {
            closestLeadOre = null;
        }
    }

    public void updateRobotCounts() throws GameActionException {
        minerCount = Comms.getMinerCount();
        soldierCount = Comms.getSoldierCount();
        builderCount = Comms.getBuilderCount();
    }

    public void signalNextExploreDirection() {
        numMinersBuilt++;
        nextFlag = Comms.encodeArchonFlag(Comms.InformationCategory.DIRECTION, EXPLORE_DIRECTIONS[(numMinersBuilt + minerDirOffset) % EXPLORE_DIRECTIONS.length]);
    }

    public int buildMiner(int counter) throws GameActionException {
        if (minerCount < MAX_NUM_MINERS) {
            Debug.printString("Building miner");
            if(closestLeadOre != null) {
                if(buildRobot(RobotType.MINER, currLoc.directionTo(closestLeadOre))){
                    counter++;
                    signalNextExploreDirection();
                }
            } else {
                if(buildRobot(RobotType.MINER)) {
                    counter++;
                    signalNextExploreDirection();
                }
            }
        }
        else {
            Debug.printString("Building soldier");
            if(buildRobot(RobotType.SOLDIER)){
                counter++;
            }
        }
        return counter;
    }

    public int buildSoldier(int counter) throws GameActionException {
        Debug.printString("Building soldier");
        if(buildRobot(RobotType.SOLDIER)) {
            counter++;
        }
        return counter;
    }

    public int buildBuilder(int counter) throws GameActionException {
        Debug.printString("Building builder, num builders: " + builderCount);
        if(buildRobot(RobotType.BUILDER)) {
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
                firstRounds();
                break;
            case CHILLING:
                Debug.printString("Chilling");
                if(minerCount <= MIN_NUM_MINERS && soldierCount >= (1/3) * minerCount) {
                    chillingCounter = minerSoldier31(chillingCounter);
                }
                else {
                    chillingCounter = minerSoldierRatio(7, chillingCounter);
                }
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
        currentBuild = Buildable.MINER;
        nextBuild = Buildable.MINER;

        if(closestLeadOre != null) {
            if(buildRobot(RobotType.MINER, currLoc.directionTo(closestLeadOre))) {
                signalNextExploreDirection();
            }
        } else {
            if(buildRobot(RobotType.MINER)) {
                signalNextExploreDirection();
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
        Comms.writeIfChanged(homeFlagIdx, flag);
    }

    public boolean checkForObesity() throws GameActionException {
        return rc.getTeamLeadAmount(rc.getTeam()) > leadObesity;
    }

    // Tries to repair the lowest health droid in range if an action is ready.
    public void tryToRepair() throws GameActionException {
        if(!rc.isActionReady()) return;

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