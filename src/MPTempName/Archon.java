package MPTempName;

import battlecode.common.*;

import MPTempName.Debug.*;
import MPTempName.Util.*;
import MPTempName.Comms.*;
import java.util.ArrayDeque;
import MPTempName.fast.FastIterableLocSet;
import MPTempName.fast.FastMath;

public class Archon extends Robot {
    static enum State {
        CHILLING,
        OBESITY,
        UNDER_ATTACK,
        INIT,
        MOVING,
        FINDING_GOOD_SPOT,
        BUILDING_LAB,
    };

    static Direction[] BUILD_DIRECTIONS;
    static Direction[] EXPLORE_DIRECTIONS;

    static int MAX_NUM_MINERS;
    static int MIN_NUM_MINERS;

    static int robotCounter;
    static int chillingCounter;
    static int initCounter;
    static int obesityCounter; 
    static int minerCount;
    static int soldierCount;
    static int builderCount;
    static int sageCount;
    static int labCount;
    static boolean labCountIncreased;
    static int roundsSinceLastLabBuilt;

    static int numMinersBuilt;
    static int minerDirOffset;

    static State currentState;
    static int flagIndex;
    static int archonNumber;
    static ArrayDeque<State> stateStack;

    static int leadNeededByBuilders;

    static boolean changedOutOfINIT;
    
    static int leadObesity;
    static int maxLeadUsedByArchons;
    static MapLocation closestLeadOre;
    static Buildable currentBuild = Buildable.MINER;
    static Buildable nextBuild = Buildable.MINER;

    static boolean canBuildPrioritized;
    static boolean isPrioritizedArchon;
    static int lastRoundPrioritized;

    static RobotInfo lastRobotHealed;
    static MapLocation[] archonSymmetryLocs; // only set for the last archon

    static MapLocation lastClosestArchonToCluster;
    static MapLocation moveTarget;
    static int lastRoundMoved;
    static boolean isCharging;

    static int numEnemies;
    static MapLocation closestEnemy;
    static int roundsSinceUnderAttack;
    static int TimeToStartFarming;

    static int turnsBeingClosest;

    static int builderRound;

    public Archon(RobotController r) throws GameActionException {
        super(r);
        //writing all Archon locations immediately on round 0
        stateStack = new ArrayDeque<State>();
        
        MAX_NUM_MINERS = Math.min(Util.MAX_MINERS,
                                    rc.getMapWidth() * rc.getMapHeight() /
                                    Util.MAX_MAP_SIZE_TO_MINER_RATIO);
        MIN_NUM_MINERS = MAX_NUM_MINERS / 5;

        archonNumber = Comms.incrementFriendly();
        int myLocFlag = Comms.encodeLocation();
        r.writeSharedArray(archonNumber, myLocFlag);
        homeFlagIdx = archonNumber + Comms.mapLocToFlag;
        changeState(State.INIT);
        leadNeededByBuilders = 0;
        soldierCount = 0;
        builderCount = 0;
        roundsSinceUnderAttack = -1;
        maxLeadUsedByArchons = 75 * ((1 + rc.getArchonCount()) - archonNumber);
        leadObesity = 180 + maxLeadUsedByArchons;
        minerDirOffset = Util.rng.nextInt(8);
        loadBuildDirections();
        pruneExploreDirections();
        if (Comms.getTurn() == rc.getArchonCount()) {
            archonSymmetryLocs = guessAndSortSymmetryLocs();
        }
        if(!isSmallMap()) {
            TimeToStartFarming = 100;
        }
        else {
            TimeToStartFarming = 250;
        }
        lastRoundMoved = Util.MIN_TURN_TO_MOVE - Util.MIN_TURNS_TO_MOVE_AGAIN;
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
    public boolean isSmallMap() {
        return Util.MAP_AREA <= Util.MAX_AREA_FOR_FAST_INIT;
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
                // Debug.printString(dir.toString());
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
        if (!canBuildPrioritized) {
            Debug.printString("Not my turn.");
            return false;
        } else {
            Debug.printString("My turn.");
        }
        for(Direction dir : orderedDirs) {
            if (rc.canBuildRobot(toBuild, dir)){
                if(toBuild == RobotType.BUILDER) {
                    Comms.signalBuilderBuilt();
                    Comms.signaljustBuiltBuilder();
                    Comms.incrementBuilderCount();
                }
                if (Comms.getTurn() != rc.getArchonCount()) {
                    Comms.useLead(toBuild);
                }
                Comms.encodeBuildGuess(archonNumber, nextBuild);
                Comms.signalBuiltRobot();
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
            Comms.resetNumTroopsHealing();
            sageCount = Comms.getSageCount();
            //todo: zero out the symmetry cluster bit
        }
        Comms.setAlive(archonNumber);
        Comms.setCanBuild(archonNumber, rc.isActionReady());
        Comms.resetAvgEnemyLoc();
        Comms.drawClusterDots();
        canBuildPrioritized = Comms.canBuildPrioritized(archonNumber, currentState == State.INIT);
        isPrioritizedArchon = Comms.getCurrentPrioritizedArchon() == archonNumber;
        if(isPrioritizedArchon) lastRoundPrioritized = rc.getRoundNum();
        reportEnemies();
        tryUpdateSymmetry();
        boolean underAttack = checkUnderAttack();
        updateRobotCounts();
        updateClosestLeadOre();
        loadArchonLocations();
        signalNeedHeal();
        boolean isObese = checkForObesity();
        toggleState(underAttack, isObese);
        doStateAction();
        // trySacrifice();
        resetBuilder();
        Comms.advanceTurn();
        // Debug.setIndicatorString(leadToUse + "; " + robotCounter + "; num alive enemies: " + Comms.aliveEnemyArchonCount());
        // if (Comms.enemyArchonCount() > 0) {
        //     System.out.println(rc.readSharedArray(Comms.firstEnemy) + "; " + rc.readSharedArray(Comms.firstEnemy + 1) + "; " + rc.readSharedArray(Comms.firstEnemy + 2) + "; " + rc.readSharedArray(Comms.firstEnemy + 3));
        // }
    }

    public void signalNeedHeal() throws GameActionException {
        if(rc.getHealth() != RobotType.ARCHON.getMaxHealth(rc.getLevel())) {
            Comms.setArchonNeedsHeal(archonNumber - 1);
        } else {
            Comms.resetArchonNeedsHeal(archonNumber - 1);
        }
    }
    
    // public void confirmLabAlive() throws GameActionException {
    //     labStillAlive = Comms.checkLabStillAlive();
    //     if(labStillAlive) {
    //         Comms.signalLabBuilt();
    //     }
    //     else {
    //         Comms.signalLabNotBuilt();
    //     }
    //     if(Comms.getTurn() == rc.getArchonCount()) {
    //         Comms.removeLabStillAlive();
    //     }
    // }

    public void resetBuilder() throws GameActionException {
        if (builderRound != 0 && builderRound + 1 == rc.getRoundNum()) {
            Comms.signalNotjustBuiltBuilder();
        }
        if(Comms.haveJustBuiltBuilder()) {
            Debug.printString("round upd");
            builderRound = rc.getRoundNum();
        }
        boolean builderStillAlive = Comms.haveBuiltBuilderForFinalLab();
        if(builderStillAlive) {
            if(builderRound + 12 <= rc.getRoundNum()) {
                if(Comms.getTurn() == rc.getArchonCount()) {
                    Debug.printString("reset");
                    Comms.signalBuilderNotBuilt();
                }
            }
        }
    }

    public boolean amImportant() throws GameActionException {
        // Debug.printString("check Imp");
        int archonMoving = 0;
        if(Comms.existsArchonMoving()) {
            archonMoving = 1;
        }
        int numArchons = rc.getArchonCount();
        int[] ArchonOrder = Comms.getArchonOrderGivenClusters();
        int numImportantArchons = ArchonOrder[4];
        // Debug.printString("num Imp: " + numImportantArchons);
        if (numImportantArchons == numArchons - archonMoving) {
            return false; //Everyone is close to action, anyone can build a builder;
        }
        else {
            for(int i = 0; i < numImportantArchons; i++) {
                if(archonNumber == ArchonOrder[i]) {
                    return true;
                }
            }
            return false;
        }
    }

    public void tryUpdateSymmetry() throws GameActionException {
        if(!Comms.foundEnemy && Comms.getTurn() == rc.getArchonCount()) {
            Comms.guessEnemyLocs(archonSymmetryLocs);
        }
    }

    public MapLocation[] guessAndSortSymmetryLocs() throws GameActionException {
        FastIterableLocSet possibleLocs = new FastIterableLocSet(12);
        MapLocation[] listOfArchons = new MapLocation[]{Comms.locationFromFlag(rc.readSharedArray(1)), 
                                                        Comms.locationFromFlag(rc.readSharedArray(2)), 
                                                        Comms.locationFromFlag(rc.readSharedArray(3)),
                                                        Comms.locationFromFlag(rc.readSharedArray(4))};
        for(int i = 0; i < 4; i ++) {
            MapLocation ourLoc = listOfArchons[i];
            if(!ourLoc.equals(new MapLocation(0,0))) {
                MapLocation[] possibleFlips = Comms.guessEnemyLoc(ourLoc);
                for(MapLocation possibleFlip: possibleFlips) {
                    boolean IsOk = true;
                    for(MapLocation ArchonLoc: listOfArchons) {
                        if (!ArchonLoc.equals(new MapLocation(0,0)) && possibleFlip.distanceSquaredTo(ArchonLoc) < RobotType.ARCHON.visionRadiusSquared) {
                            IsOk = false;
                        }
                    }
                    if (IsOk) {
                        possibleLocs.add(possibleFlip);
                        possibleLocs.updateIterable();
                    }
                }
            }
        }
        MapLocation[] candidateLocs = new MapLocation[possibleLocs.size];
        for (int i = 0; i < possibleLocs.size; i++) {
            candidateLocs[i] = possibleLocs.locs[i];
        }
        return candidateLocs;
    }

    public boolean checkUnderAttack() throws GameActionException {
        numEnemies = 0;
        int numFriendlies = 0;
        closestEnemy = null;
        int minDist = Integer.MAX_VALUE;
        for (RobotInfo enemy : EnemySensable) {
            RobotType enemyType = enemy.getType();
            if (Util.canAttackorArchon(enemyType)) {
                numEnemies++;
                if(enemy.location.isWithinDistanceSquared(currLoc, minDist)) {
                    closestEnemy = enemy.location;
                    minDist = currLoc.distanceSquaredTo(closestEnemy);
                }
            }
        }
        for (RobotInfo bot : FriendlySensable) {
            if(Clock.getBytecodeNum() >= 5000) break;
            RobotType botType = bot.getType();
            if (Util.canAttackorArchon(botType)) {
                numFriendlies++;
            }
        }
        return numEnemies > numFriendlies;
    }

    // pick a random lead ore for  a miner to go to, if spawned on this turn
    //
    public void updateClosestLeadOre() throws GameActionException{

        MapLocation[] locs = rc.senseNearbyLocationsWithLead(-1, 2);
        int numLocs = locs.length;
        int numMinersPerLoc;
        if(numLocs != 0) {
            numMinersPerLoc = Util.MAX_MINER_LEAD_MULT / numLocs;
        }
        else {
            numMinersPerLoc = 0;
        }
        // int currByteCode = Clock.getBytecodesLeft();
        // Debug.printString("locs: " + numLocs + ", miners: " + numMinersPerLoc);
        double bestLeadScore = Integer.MIN_VALUE;
        MapLocation bestLeadLoc = null;
        for(MapLocation loc: locs) {
            double currScore = getLeadDistTradeoffScore(loc, rc.senseLead(loc), numMinersPerLoc);
            if(currScore > bestLeadScore && !loc.equals(currLoc)) {
                bestLeadScore = currScore;
                bestLeadLoc = loc;
            }
        }
        // int newByteCode = Clock.getBytecodesLeft();
        // int byteCodeUsed = currByteCode - newByteCode;
        // Debug.printString("byte: " + byteCodeUsed);
        if (bestLeadLoc != null) {
            closestLeadOre = bestLeadLoc;
            // Debug.printString("ore: " + closestLeadOre);
        } else {
            closestLeadOre = null;
        }
    }

    public void updateRobotCounts() throws GameActionException {
        if(roundsSinceUnderAttack != -1) {
            roundsSinceUnderAttack++;
        }
        if(Comms.AnyoneUnderAttack()) {
            roundsSinceUnderAttack = 0;
        }
        if (Comms.getTurn() == 1) {
            Comms.resetAliveLabs();
        }
        roundsSinceLastLabBuilt++;
        labCountIncreased = false;
        int tmpCount = Comms.getAliveLabs();
        if (tmpCount > labCount) {
            labCountIncreased = true;
        }
        labCount = tmpCount;
        minerCount = Comms.getMinerCount();
        soldierCount = Comms.getSoldierCount();
        Comms.setSteadySoldierIdx(soldierCount);
        Comms.setSteadyMinerIdx(minerCount);
    }

    public void signalNextExploreDirection() {
        numMinersBuilt++;
        nextFlag = Comms.encodeArchonFlag(Comms.InformationCategory.DIRECTION, EXPLORE_DIRECTIONS[(numMinersBuilt + minerDirOffset) % EXPLORE_DIRECTIONS.length]);
    }

    public int buildMiner(int counter) throws GameActionException {
        if (minerCount < MAX_NUM_MINERS) {
            // Debug.printString("Building miner");
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
            // Debug.printString("Building soldier");
            currentBuild = Buildable.SOLDIER;
            if(buildRobot(RobotType.SOLDIER, Util.randomDirection())){
                counter++;
            }
        }
        return counter;
    }

    public int buildSoldier(int counter) throws GameActionException {
        // Debug.printString("Building soldier");
        if(buildRobot(RobotType.SOLDIER)) {
            counter++;
        }
        return counter;
    }

    public Direction findGoodRubbleDirection() throws GameActionException {
        MapLocation[] allLocs = rc.getAllLocationsWithinRadiusSquared(currLoc, visionRadiusSquared);
        int bestRubble = Integer.MAX_VALUE;
        MapLocation bestLoc = null;
        int bestDistance = Integer.MIN_VALUE;
        MapLocation enemyLoc = Comms.getClosestCluster(currLoc);
        if(enemyLoc == null) {
            enemyLoc = currLoc;
        }
        for (MapLocation loc: allLocs) {
            int currRubble = rc.senseRubble(loc);
            int currDist = loc.distanceSquaredTo(enemyLoc);
            if(currRubble < bestRubble || (currRubble == bestRubble && currDist > bestDistance)) {
                bestRubble = currRubble;
                bestDistance = currDist;
                bestLoc = loc;
            }
        }
        // Debug.printString("best loc: " + bestLoc);
        return home.directionTo(bestLoc);
    }


    public int buildBuilder(int counter) throws GameActionException {
        Debug.printString("Building builder, num builders: " + builderCount);
        if(buildRobot(RobotType.BUILDER)) {
            counter++;
        }
        return counter;
    }

    public int minerSoldier(int counter) throws GameActionException {
        switch(counter % 2) {
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
                initCounter = firstRounds(4, initCounter);
                tryToRepairLastBot();
                break;
            case BUILDING_LAB:
                Debug.printString("Building Lab");
                currentBuild = Buildable.EMPTY;
                nextBuild = Buildable.EMPTY;
                Debug.printString("  " + Comms.haveBuiltBuilderForFinalLab() + " " + amImportant());
                if(!Comms.haveBuiltBuilderForFinalLab() && !amImportant()) {
                    buildRobot(RobotType.BUILDER, findGoodRubbleDirection());
                    currentBuild = Buildable.BUILDER;
                    nextBuild = Buildable.SOLDIER;
                }
                tryToRepairLastBot();
                break;
            case CHILLING:
                Debug.printString("Chilling");
                // writeLocation();
                int goldAmount = rc.getTeamGoldAmount(team);
                if ((goldAmount >= RobotType.SAGE.buildCostGold && isPrioritizedArchon) ||
                    (goldAmount >= 2 * RobotType.SAGE.buildCostGold)) {
                    currentBuild = Buildable.EMPTY;
                    nextBuild = Buildable.SOLDIER;
                    buildRobot(RobotType.SAGE);
                    break;
                }
                else if(soldierCount <= minerCount - 1) {
                    chillingCounter = buildSoldier(chillingCounter);
                }
                else if(minerCount <= MIN_NUM_MINERS) {
                    chillingCounter = minerSoldier(chillingCounter);
                }
                else {
                    chillingCounter = minerSoldierRatio(7, chillingCounter);
                }
                tryToRepairLastBot();
                break;
            case UNDER_ATTACK:
                Debug.printString("Under Attack");
                roundsSinceUnderAttack = 0;
                chillingCounter = buildSoldier(chillingCounter);
                tryToRepairLowestHealth();
                break;
            case OBESITY:
                Debug.printString("Obesity");
                int leadForBuilders = rc.getTeamLeadAmount(rc.getTeam()) - maxLeadUsedByArchons;
                int watchtowersPossible = leadForBuilders / 180;
                if((rc.getRoundNum() < Util.MIN_ROUND_FOR_LAB || labCount >= 1)) {
                    if (/*watchtowersPossible > builderCount &&*/ builderCount <= MIN_NUM_MINERS) {
                        obesityCounter = SoldierBuilderRatio(11, obesityCounter);
                    } else {
                        obesityCounter = buildSoldier(obesityCounter);
                        break;
                    }
                }
                else {
                    if(!amImportant() && !Comms.haveBuiltBuilderForFinalLab()) {
                        Debug.printString("not imp, make lab bulder");
                        currentBuild = Buildable.BUILDER;
                        nextBuild = Buildable.EMPTY;
                        buildRobot(RobotType.BUILDER);
                    }                   
                }
                tryToRepairLastBot();
                break;
            case MOVING:
                Debug.printString("Moving");
                reloadMoveTarget();
                Nav.move(moveTarget);
                // if(closestEnemy != null) {
                //     moveSafely(closestEnemy, Robot.visionRadiusSquared);
                // } else {
                //     Nav.move(moveTarget);
                // }
                Debug.setIndicatorLine(Debug.INDICATORS, currLoc, moveTarget, 204, 0, 255);
                break;
            case FINDING_GOOD_SPOT:
                Debug.printString("Finding spot");
                findGoodSpot();
                Nav.move(moveTarget);
                Debug.setIndicatorLine(Debug.INDICATORS, currLoc, moveTarget, 204, 0, 255);
                break;
            default: 
                changeState(State.CHILLING);
                break;
        }
    }

    public int firstRounds(int mod, int counter) throws GameActionException {
        if (isSmallMap()) {
            if (counter != mod - 1 && counter != mod - 2) {
                currentBuild = Buildable.MINER;
                counter = buildMiner(counter);
                if(counter == mod - 3) {
                    nextBuild = Buildable.MINER;
                }
                else {
                    nextBuild = Buildable.SOLDIER;
                }
            }
            else {
                counter = buildSoldier(counter);
                currentBuild = Buildable.SOLDIER;
                if(counter == mod - 2) {
                    nextBuild = Buildable.SOLDIER;
                }
                else {
                    nextBuild = Buildable.MINER;
                }
            }
            return counter;
        }
        else if (labCount >= 1) {
            // Debug.printString("Correct location");
            if (counter != mod - 1) {
                currentBuild = Buildable.MINER;
                counter = buildMiner(counter);
                if(counter == mod - 2) {
                    nextBuild = Buildable.MINER;
                }
                else {
                    nextBuild = Buildable.SOLDIER;
                }
            }
            else {
                counter = buildSoldier(counter);
                currentBuild = Buildable.SOLDIER;
                nextBuild = Buildable.MINER;
            }
            return counter;
        }
        else {
            Debug.printString("Incorrect location");
            currentBuild = Buildable.MINER;
            if(counter != 3) {
                nextBuild = Buildable.MINER;
            }
            else {
                nextBuild = Buildable.BUILDER;
            }
            counter = buildMiner(counter);
            return counter;
        }
    }

    public boolean shouldMoveToBetterRubble() throws GameActionException {
        int currRubble = rc.senseRubble(currLoc);
        if (currRubble < 30) {
            return false;
        }
        for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(currLoc, visionRadiusSquared)) {
            if (rc.onTheMap(loc) && (currRubble - rc.senseRubble(loc)) >= 20) {
                return true;
            }
        }
        return false;
    }

    public boolean isFarFromCluster() throws GameActionException {
        if (rc.getArchonCount() == 1) {
            if (Util.distance(currLoc, Comms.getClosestCluster(currLoc)) > 
                0.25 * Util.cooldownTurnsRemaining(rc.senseRubble(currLoc), 100) || rc.getRoundNum() < 200) {
                return true;
            }
        }
        else {
            if (currLoc.distanceSquaredTo(Comms.getClosestCluster(currLoc)) > 2.25 * visionRadiusSquared) {
                return true;
            }
        }
        return false;
    }

    public void toggleState(boolean underAttack, boolean isObese) throws GameActionException {
        switch (currentState) { 
            case INIT:
                // Debug.printString("lead obesity: " + leadObesity);
                if(underAttack) {
                    Comms.signalUnderAttack();
                    stateStack.push(State.CHILLING);
                    changeState(State.UNDER_ATTACK);
                }
                else if((robotCounter >= 1 && Comms.foundEnemy) || minerCount >= MIN_NUM_MINERS) {
                    changeState(State.CHILLING);
                }
                else if(rc.getTeamLeadAmount(rc.getTeam()) > leadObesity) {
                    stateStack.push(State.CHILLING);
                    changeState(State.OBESITY);
                }
                else if (robotCounter >= 3 && shouldMoveToBetterRubble() &&
                        rc.isTransformReady() && !Comms.existsArchonMoving()) {
                    rc.writeSharedArray(archonNumber, Comms.DEAD_ARCHON_FLAG);
                    stateStack.push(currentState);
                    changeState(State.FINDING_GOOD_SPOT);
                    moveTarget = currLoc;
                    rc.transform();
                    Comms.setArchonMoving();
                }
                else if (!isSmallMap() && (initCounter == 3 || Comms.checkMakingINITBuilder())) {
                    changedOutOfINIT = true;
                    Comms.signalMakingINITBuilder();
                    stateStack.push(State.CHILLING);
                    changeState(State.BUILDING_LAB);                    
                }
                break;
            case UNDER_ATTACK:
                if (!underAttack) {
                    Comms.signalNotUnderAttack();
                    changeState(stateStack.pop());
                }
                break;
            case CHILLING:
                Debug.printString("uA: " + roundsSinceUnderAttack + " " + rc.getRoundNum() + " " + roundsSinceLastLabBuilt);
                if (underAttack) {
                    Comms.signalUnderAttack();
                    stateStack.push(currentState);
                    changeState(State.UNDER_ATTACK);
                } else if (isObese) {
                    stateStack.push(currentState);
                    changeState(State.OBESITY);
                } else if (shouldMoveToBetterRubble() && 
                            (rc.getRoundNum() > lastRoundPrioritized + Util.TURNS_NOT_PRIORITIZED_TO_MOVE || isFarFromCluster()) &&
                            rc.getRoundNum() > lastRoundMoved + Util.MIN_TURNS_TO_MOVE_AGAIN && 
                            rc.isTransformReady() && !Comms.existsArchonMoving()) {
                    rc.writeSharedArray(archonNumber, Comms.DEAD_ARCHON_FLAG);
                    stateStack.push(currentState);
                    changeState(State.FINDING_GOOD_SPOT);
                    moveTarget = currLoc;
                    rc.transform();
                    Comms.setArchonMoving();
                }
                else if(rc.getRoundNum() > lastRoundPrioritized + Util.TURNS_NOT_PRIORITIZED_TO_MOVE &&
                            rc.getRoundNum() > lastRoundMoved + Util.MIN_TURNS_TO_MOVE_AGAIN &&
                            rc.isTransformReady() &&
                            !Comms.existsArchonMoving() &&
                            Comms.foundEnemy &&
                            chooseInitialMoveTarget()) {
                    // Just mark yourself as dead in archon locations so units don't come to get healed
                    rc.writeSharedArray(archonNumber, Comms.DEAD_ARCHON_FLAG);
                    stateStack.push(currentState);
                    changeState(State.MOVING);
                    turnsBeingClosest = 0;
                    rc.transform();
                    Comms.setArchonMoving();
                } else if(rc.getRoundNum() > roundsSinceLastLabBuilt + 12 &&
                    (soldierCount >= Util.SOLDIER_LAB_MULT * labCount) &&
                    (labCount < Util.MAX_NUM_LABS) &&
                    !isSmallMap() && (roundsSinceUnderAttack > 100 || roundsSinceUnderAttack == -1)) {
                    stateStack.push(currentState);
                    changeState(State.BUILDING_LAB);
                }
                break;
            case OBESITY:
                if (underAttack) {
                    Comms.signalUnderAttack();
                    stateStack.push(currentState);
                    changeState(State.UNDER_ATTACK);
                } else if (rc.getTeamLeadAmount(rc.getTeam()) < leadObesity) {
                    changeState(stateStack.pop());
                } else if(rc.getRoundNum() > roundsSinceLastLabBuilt + 10 &&
                        (labCount == 0 || (labCount == 1 && soldierCount >= 15)) && 
                        (roundsSinceUnderAttack > 100 || roundsSinceUnderAttack == -1)) {
                    stateStack.push(currentState);
                    changeState(State.BUILDING_LAB);
                }
                break;
            case MOVING:
                Debug.printString("target: " + moveTarget + "enemies: " + numEnemies);
                if(currLoc.isWithinDistanceSquared(moveTarget, 13) ||
                    (currLoc.isWithinDistanceSquared(moveTarget, Util.MIN_DIST_SQUARED_FROM_CLUSTER) && numEnemies != 0)
                    || isOldArchonDead()) {
                    changeState(State.FINDING_GOOD_SPOT);
                }
                break;
            case FINDING_GOOD_SPOT:
                if(currLoc.equals(moveTarget) && rc.isTransformReady()) {
                    changeState(stateStack.pop());
                    // Mark yourself as alive again
                    rc.writeSharedArray(archonNumber, Comms.encodeLocation());
                    lastRoundMoved = rc.getRoundNum();
                    rc.transform();
                    Comms.resetArchonMoving();
                    isCharging = false;
                }
                break;
            case BUILDING_LAB:
                if(underAttack) {
                    Comms.signalUnderAttack();
                    stateStack.push(currentState);
                    builderRound = 0;
                    Comms.stopSignalingArchonBuildingLab();
                    changeState(State.UNDER_ATTACK);
                }
                if(labCountIncreased) {
                    builderRound = 0;
                    roundsSinceLastLabBuilt = 0;
                    Comms.stopSignalingArchonBuildingLab();
                    changeState(stateStack.pop());
                }
            default:
                break;
        }
    }

    public void writeLocation() throws GameActionException {
        Comms.writeIfChanged(archonNumber, Comms.encodeLocation());
    }

    public void changeState(State newState) throws GameActionException {
        currentState = newState;
        InformationCategory ic;
        switch (newState) {
            case UNDER_ATTACK:
                ic = InformationCategory.UNDER_ATTACK;
                break;
            case BUILDING_LAB:
                Comms.signalArchonBuildingLab();
                //purposeful fallthrough
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

    public int getMaxHealth(RobotType robotType) {
        switch(robotType) {
            case SAGE: return RobotType.SAGE.health;
            case MINER: return RobotType.MINER.health;
            case SOLDIER: return RobotType.SOLDIER.health;
            case BUILDER: return RobotType.BUILDER.health;
            default: return 0;
        }
    }

    public RobotInfo[] getRestrictedFriendlies() throws GameActionException {
        RobotInfo[] friendlies = rc.senseNearbyRobots(actionRadiusSquared, rc.getTeam());
        if(friendlies.length > 15) friendlies = rc.senseNearbyRobots(13, team);
        if(friendlies.length > 15) friendlies = rc.senseNearbyRobots(10, team);
        return friendlies;
    }

    public RobotInfo getLowestHealthRobotToRepair() throws GameActionException {
        RobotInfo[] friendlies = getRestrictedFriendlies();
        RobotInfo maybeSage = null;
        RobotInfo maybeSoldier = null;
        RobotInfo maybeBuilder = null;
        RobotInfo maybeMiner = null;
        RobotInfo friendly = null;
        for (int i = friendlies.length; --i >= 0;) {
            friendly = friendlies[i];
            if(Clock.getBytecodeNum() >= 15000) break;
            switch(friendly.type) {
                case SAGE:
                    if((maybeSage == null || maybeSage.health > friendly.health)
                        && friendly.health != RobotType.SAGE.health) {
                        maybeSage = friendly;
                    }
                    break;
                case SOLDIER:
                    if((maybeSoldier == null || maybeSoldier.health > friendly.health)
                        && friendly.health != RobotType.SOLDIER.health) {
                        maybeSoldier = friendly;
                    }
                    break;
                case BUILDER:
                    if((maybeBuilder == null || maybeBuilder.health > friendly.health)
                        && friendly.health != RobotType.BUILDER.health) {
                        maybeBuilder = friendly;
                    }
                    break;
                case MINER:
                    if((maybeMiner == null || maybeMiner.health > friendly.health)
                        && friendly.health != RobotType.MINER.health) {
                        maybeMiner = friendly;
                    }
                    break;
                default:
                    break;
            }
        }

        RobotInfo robotToHeal = null;
        if(maybeMiner != null && rc.canRepair(maybeMiner.location)) robotToHeal = maybeMiner;
        if(maybeBuilder != null && rc.canRepair(maybeBuilder.location)) robotToHeal = maybeBuilder;
        if(maybeSoldier != null && rc.canRepair(maybeSoldier.location)) robotToHeal = maybeSoldier;
        if(maybeSage != null && rc.canRepair(maybeSage.location)) robotToHeal = maybeSage;
        return robotToHeal;
    }

    // Remembers the last bot it healed and heals that one first
    public void tryToRepairLastBot() throws GameActionException {
        if(!rc.isActionReady()) return;

        if(lastRobotHealed != null && rc.canSenseRobot(lastRobotHealed.ID)) {
            RobotInfo lastRobot = rc.senseRobot(lastRobotHealed.ID);
            RobotInfo newRobot = getLowestHealthRobotToRepair();
            // Switch targets if you have a miner now and a soldier can be healed
            if(newRobot != null) {
                if(lastRobot.type == RobotType.MINER && newRobot.type != RobotType.MINER) {
                    lastRobot = newRobot;
                }
                if(newRobot.health < Util.MIN_HEALTH_TO_MAINTAIN &&
                    rc.canRepair(newRobot.location)) {
                    Debug.printString("Healing");
                    lastRobotHealed = lastRobot;
                    rc.repair(newRobot.location);
                    return;
                }
            }
            if(rc.canRepair(lastRobot.location) && lastRobot.health < getMaxHealth(lastRobot.type)) {
                Debug.printString("Healing");
                lastRobotHealed = lastRobot;
                rc.repair(lastRobot.location);
                return;
            }
        }

        tryToRepairLowestHealth();
    }

    // Tries to repair the lowest health droid in range if an action is ready.
    public void tryToRepairLowestHealth() throws GameActionException {
        if(!rc.isActionReady()) return;

        RobotInfo robotToRepair = getLowestHealthRobotToRepair();
        if(robotToRepair != null) {
            Debug.printString("Healing");
            rc.repair(robotToRepair.location);
            lastRobotHealed = robotToRepair;
        }
    }

    // Gets the cluster closest to any archon
    // This is the "most important" one
    public MapLocation getClusterClosestToArchons() throws GameActionException {
        MapLocation[] clusters = Comms.getClusters();
        MapLocation cluster = null;
        int minDist = Integer.MAX_VALUE;
        int dist;
        for(MapLocation clusterLoc : clusters) {
            for(MapLocation archonLoc : archonLocations) {
                if(archonLoc == null) continue;
                dist = clusterLoc.distanceSquaredTo(archonLoc);
                if(dist < minDist) {
                    cluster = clusterLoc;
                    minDist = dist;
                }
            }
        }

        return cluster;
    }

    // Abort moving if the archon died
    public boolean isOldArchonDead() throws GameActionException {
        for(MapLocation archonLoc : archonLocations) {
            if(archonLoc == null) continue;
            // Debug.printString("" + lastClosestArchonToCluster + " ");
            // Debug.printString("" + archonLoc + " ");
            if(archonLoc.equals(lastClosestArchonToCluster)) return false;
        }
        return !isCharging;
    }

    public MapLocation getClusterClosestTo(MapLocation loc) throws GameActionException {
        MapLocation[] clusters = Comms.getClusters();
        MapLocation cluster = null;
        int minDist = Integer.MAX_VALUE;
        int dist;
        for(MapLocation clusterLoc : clusters) {
            dist = clusterLoc.distanceSquaredTo(loc);
            if(dist < minDist) {
                cluster = clusterLoc;
                minDist = dist;
            }
        }

        return cluster;
    }

    // Don't move if any cluster is within a certain
    // distance between the line between you and the other archon.
    // Tries to avoid a Sandwich case where you run through the cluster
    public boolean checkWontRunThroughCluster() throws GameActionException {
        MapLocation cluster = getClusterClosestTo(moveTarget);
        MapLocation closestArchon = currLoc;
        int minDist = Integer.MAX_VALUE;
        int dist;
        for(MapLocation archonLoc : archonLocations) {
            if(archonLoc == null) continue;
            dist = archonLoc.distanceSquaredTo(cluster);
            if(dist < minDist) {
                closestArchon = archonLoc;
                minDist = dist;
            }
        }

        return checkWontRunThroughCluster(closestArchon);
    }

    public boolean checkWontRunThroughCluster(MapLocation closestArchon) throws GameActionException {
        Debug.setIndicatorLine(Debug.INDICATORS, closestArchon, currLoc, 255, 102, 153);
        MapLocation[] clusters = Comms.getClusters();
        for(MapLocation cluster : clusters) {
            MapLocation projection = FastMath.getProjection(currLoc, closestArchon, cluster);
            Debug.setIndicatorLine(Debug.INDICATORS, cluster, projection, 255, 102, 255);
            Debug.printString("proj dist: " + projection.distanceSquaredTo(cluster));
            // If the projection is onto the archon, then the cluster is on the other side
            if(!projection.equals(closestArchon) &&
                cluster.isWithinDistanceSquared(projection, Util.MIN_DIST_FROM_PROJECTION)) {
                return false;
            }
        }

        return true;
    }

    public void reloadMoveTarget() throws GameActionException {
        MapLocation cluster = getClusterClosestTo(currLoc);
        lastClosestArchonToCluster = currLoc;
        int minDist = Integer.MAX_VALUE;
        int dist;
        for(MapLocation archonLoc : archonLocations) {
            if(archonLoc == null) continue;
            dist = archonLoc.distanceSquaredTo(cluster);
            if(dist < minDist) {
                lastClosestArchonToCluster = archonLoc;
                minDist = dist;
            }
        }

        // if(!cluster.isWithinDistanceSquared(moveTarget, Util.MAX_CLUSTER_DIST_CHANGE)) {
        //     // This can happen if a cluster disappears for a turn.
        //     // In this case, we don't want to move towards the other one
        //     // so just keep the same target
        //     return;
        // }

        if(isCharging) {
            moveTarget = cluster;
            return;
        }

        if(lastClosestArchonToCluster.isWithinDistanceSquared(cluster, Util.MIN_DIST_SQUARED_FROM_CLUSTER)) {
            // Small map? Just go to the closest archon
            Debug.printString("archon");
            moveTarget = lastClosestArchonToCluster;
        } else {
            Debug.printString("cluster: " + cluster);
            // Otherwise, pick a location on the line from the cluster to the archon,
            // which is the correct distance away from the cluster
            double vX = lastClosestArchonToCluster.x - cluster.x;
            double vY = lastClosestArchonToCluster.y - cluster.y;
            double mag = Math.hypot(vX, vY);
            vX /= mag;
            vY /= mag;

            double x = cluster.x + Math.sqrt(Util.MIN_DIST_SQUARED_FROM_CLUSTER) * vX;
            double y = cluster.y + Math.sqrt(Util.MIN_DIST_SQUARED_FROM_CLUSTER) * vY;
            moveTarget = new MapLocation((int)x, (int)y);
        }
    }

    public MapLocation[][] divideArchonsByCluster() throws GameActionException {
        MapLocation[] clusters = Comms.getClusters();
        MapLocation cluster = null;
        int closestClusterIdx;
        int minDist;
        int dist;
        MapLocation[][] clusterGroups = new MapLocation[clusters.length][4];
        int[] clusterSizes = new int[clusters.length];
        for(MapLocation archonLoc : archonLocations) {
            if(archonLoc == null) continue;
            minDist = Integer.MAX_VALUE;
            closestClusterIdx = -1;
            for(int i = clusters.length; --i >= 0;) {
                cluster = clusters[i];
                dist = cluster.distanceSquaredTo(archonLoc);
                if(dist < minDist) {
                    closestClusterIdx = i;
                    minDist = dist;
                }
            }
            clusterGroups[closestClusterIdx][clusterSizes[closestClusterIdx]++] = archonLoc;
        }

        MapLocation[][] res = new MapLocation[clusters.length][];
        for(int i = res.length; --i >= 0;) {
            MapLocation[] arcs = new MapLocation[clusterSizes[i]];
            System.arraycopy(clusterGroups[i], 0, arcs, 0, arcs.length);
            res[i] = arcs;
        }

        return res;
    }

    // ~3k bytecode
    public boolean chooseInitialMoveTarget() throws GameActionException {
        MapLocation[] clusters = Comms.getClusters();
        MapLocation[][] archonGroups = divideArchonsByCluster();

        MapLocation cluster = null;
        MapLocation farthestArchon = null;
        int maxDist = Integer.MIN_VALUE;
        int dist;
        int clusterIdx = -1;
        for(int i = 0; i < archonGroups.length; i++) {
            for(int j = 0; j < archonGroups[i].length; j++) {
                dist = archonGroups[i][j].distanceSquaredTo(clusters[i]);
                if(dist > maxDist) {
                    maxDist = dist;
                    farthestArchon = archonGroups[i][j];
                    cluster = clusters[i];
                    clusterIdx = i;
                }
            }
        }

        // Debug.printString(cluster.toString());
        // Debug.printString(farthestArchon.toString());
        // Only have the farthest one from the cluster group move
        if(cluster == null || farthestArchon == null || !farthestArchon.equals(currLoc)) {
            // Not the correct archon to move
            return false;
        }

        // Find the closest archon to your cluster
        lastClosestArchonToCluster = currLoc;
        int minDist = Integer.MAX_VALUE;
        for(int i = 0; i < archonGroups[clusterIdx].length; i++) {
            dist = archonGroups[clusterIdx][i].distanceSquaredTo(cluster);
            if(dist < minDist) {
                minDist = dist;
                lastClosestArchonToCluster = archonGroups[clusterIdx][i];
            }
        }

        // if(lastClosestArchonToCluster.equals(currLoc)) {
        //     // Let's move closer and extend our advantage
        //     if(rc.getRoundNum() > lastRoundPrioritized + Util.PRIORITIZED_ARCHON_TURNS_NOT_PRIORITIZED_TO_MOVE &&
        //         rc.getRoundNum() > lastRoundMoved + Util.MIN_TURNS_PRIORITIZED_TO_MOVE_AGAIN) {
        //         moveTarget = cluster;
        //         isCharging = true;
        //         Debug.println("Charging target: " + moveTarget);
        //         return !currLoc.isWithinDistanceSquared(moveTarget, Util.MIN_DIST_TO_MOVE);
        //     }
        //     else {
        //         return false;
        //     }
        // }

        // Min dist to move
        // if(currLoc.isWithinDistanceSquared(lastClosestArchonToCluster, Util.MIN_DIST_TO_MOVE)) {
        //     return false;
        // }

        if(!checkWontRunThroughCluster(lastClosestArchonToCluster)) {
            // Debug.printString("Cluster check");
            return false;
        }

        if(lastClosestArchonToCluster.isWithinDistanceSquared(cluster, Util.MIN_DIST_SQUARED_FROM_CLUSTER)) {
            // Small map? Just go to the closest archon
            moveTarget = lastClosestArchonToCluster;
            Debug.printString("in this case");
            // Debug.println("Move target close: " + moveTarget);
        } else {
            // Otherwise, pick a location on the line from the cluster to the archon,
            // which is the correct distance away from the cluster
            double vX = lastClosestArchonToCluster.x - cluster.x;
            double vY = lastClosestArchonToCluster.y - cluster.y;
            double mag = Math.hypot(vX, vY);
            vX /= mag;
            vY /= mag;

            double x = cluster.x + Math.sqrt(Util.MIN_DIST_SQUARED_FROM_CLUSTER) * vX;
            double y = cluster.y + Math.sqrt(Util.MIN_DIST_SQUARED_FROM_CLUSTER) * vY;
            moveTarget = new MapLocation((int)x, (int)y);
            // Debug.printString("Move target far: " + moveTarget);
        }

        return !currLoc.isWithinDistanceSquared(moveTarget, Util.MIN_DIST_TO_MOVE);
    }

    public int getSpotScoreSafe(MapLocation loc, int initialScore) throws GameActionException {
        int score = initialScore;
        MapLocation loc2;
        loc2 = loc.add(Direction.NORTH);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) / 4 : 50;
        loc2 = loc.add(Direction.NORTHEAST);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) / 4 : 50;
        loc2 = loc.add(Direction.EAST);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) / 4 : 50;
        loc2 = loc.add(Direction.SOUTHEAST);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) / 4 : 50;
        loc2 = loc.add(Direction.SOUTH);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) / 4 : 50;
        loc2 = loc.add(Direction.SOUTHWEST);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) / 4 : 50;
        loc2 = loc.add(Direction.WEST);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) / 4 : 50;
        loc2 = loc.add(Direction.NORTHWEST);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) / 4 : 50;

        score += rc.senseRubble(loc) * 20;
        score += Math.sqrt(loc.distanceSquaredTo(currLoc)) * 5;
        return score;
    }

    public int getSpotScore(MapLocation loc, int initialScore) throws GameActionException {
        int score = initialScore;
        MapLocation loc2;
        loc2 = loc.add(Direction.NORTH);
        score += rc.senseRubble(loc2) / 4;
        loc2 = loc.add(Direction.NORTHEAST);
        score += rc.senseRubble(loc2) / 4;
        loc2 = loc.add(Direction.EAST);
        score += rc.senseRubble(loc2) / 4;
        loc2 = loc.add(Direction.SOUTHEAST);
        score += rc.senseRubble(loc2) / 4;
        loc2 = loc.add(Direction.SOUTH);
        score += rc.senseRubble(loc2) / 4;
        loc2 = loc.add(Direction.SOUTHWEST);
        score += rc.senseRubble(loc2) / 4;
        loc2 = loc.add(Direction.WEST);
        score += rc.senseRubble(loc2) / 4;
        loc2 = loc.add(Direction.NORTHWEST);
        score += rc.senseRubble(loc2) / 4;

        score += rc.senseRubble(loc) * 20;
        score += Math.sqrt(loc.distanceSquaredTo(currLoc)) * 5;
        return score;
    }

    public MapLocation getCloseArchon() throws GameActionException {
        MapLocation closeArchon = null;
        int minDist = RobotType.ARCHON.visionRadiusSquared * 4;
        for(MapLocation archonLoc : archonLocations) {
            if(archonLoc == null) continue;
            if(archonLoc.isWithinDistanceSquared(currLoc, minDist)) {
                closeArchon = archonLoc;
                minDist = archonLoc.distanceSquaredTo(currLoc);
            }
        }

        return closeArchon;
    }

    // Choose a new spot if the new one is significantly better than the last one.
    // Find the minimum rubble spot, breaking ties roughly by
    // the sum of adjacent rubble and the distance to the current location
    public void findGoodSpot() throws GameActionException {
        // Note: We can probably get away with calculating only on movement cooldown
        // We lost a game when I tested with this initially (prob rng)
        // if(rc.isMovementReady()) return;

        MapLocation closeArchon = getCloseArchon();
        closeArchon = closeArchon == null ? moveTarget : closeArchon;
        MapLocation bestLoc = null;
        MapLocation loc = currLoc;
        int minScore = Integer.MAX_VALUE;
        int score;
        // Dir path goes 3 out, plus another for sensing the loc next to it.
        if(Util.isLessThanDistOfEdge(currLoc, 4)) {
            // Safe version that does rc.canSenseLocation checks
            for(int i = Util.DIR_PATH_13.length; --i >= 0;) {
                loc = loc.add(Util.DIR_PATH_13[i]);
                if(Clock.getBytecodesLeft() < 4000) {
                    Debug.println("Safe: had to break finding good spot");
                    break;
                }
                if(rc.canSenseRobotAtLocation(loc) || !rc.canSenseLocation(loc)) continue;
                score = getSpotScoreSafe(loc, moveTarget.distanceSquaredTo(loc) + 
                                            closeArchon.distanceSquaredTo(loc));
                if(score < minScore) {
                    minScore = score;
                    bestLoc = loc;
                }
            }
        } else {
            // Assumes all locations are sensable
            for(int i = Util.DIR_PATH_13.length; --i >= 0;) {
                loc = loc.add(Util.DIR_PATH_13[i]);
                if(Clock.getBytecodesLeft() < 4000) {
                    Debug.println("had to break finding good spot");
                    break;
                }
                if(rc.canSenseRobotAtLocation(loc)) continue;
                score = getSpotScore(loc, moveTarget.distanceSquaredTo(loc) + 
                                            closeArchon.distanceSquaredTo(loc));
                if(score < minScore) {
                    minScore = score;
                    bestLoc = loc;
                }
            }
        }

        if (bestLoc != null) {
            if(rc.canSenseLocation(moveTarget) &&
                (!rc.isLocationOccupied(moveTarget) || moveTarget.equals(currLoc))) {
                int currTargetScore = getSpotScoreSafe(moveTarget, 0);
                if(currTargetScore > minScore * 2) {
                    moveTarget = bestLoc;
                    // Debug.println("New good spot: " + moveTarget.toString());
                }
            } else {
                moveTarget = bestLoc;
            }
        }
    }

    public void trySacrifice() throws GameActionException {
        MapLocation cluster = Comms.getClosestCluster(currLoc);
        if (rc.getRoundNum() == 1 || cluster == null) {
            return;
        }
        MapLocation farthestArchon = null;
        int farthestDist = 0;
        for (int i = Comms.firstArchon; i < Comms.firstArchon + Comms.friendlyArchonCount(); i++) {
            MapLocation tempLoc = Comms.locationFromFlag(rc.readSharedArray(i));
            int tempDist = tempLoc.distanceSquaredTo(cluster);
            if (tempDist > farthestDist) {
                farthestArchon = tempLoc;
                farthestDist = tempDist;
            }
        }
        int count = rc.getArchonCount();
        if (count > 2 && count == Comms.friendlyArchonCount() 
            && farthestArchon.equals(currLoc) && numMinersBuilt > 0) {
            if (Comms.getTurn() == rc.getArchonCount()) {
                Comms.advanceTurn();
            }
            rc.disintegrate();
        }
    }
}