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
        maxLeadUsedByArchons = 75 * ((1 + rc.getArchonCount()) - archonNumber);
        leadObesity = 180 + maxLeadUsedByArchons;
        minerDirOffset = Util.rng.nextInt(8);
        loadBuildDirections();
        pruneExploreDirections();
        if (Comms.getTurn() == rc.getArchonCount()) {
            archonSymmetryLocs = guessAndSortSymmetryLocs();
        }
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
        if (!isPrioritizedArchon) {
            Debug.printString("Not my turn.");
            return false;
        } else {
            Debug.printString("My turn.");
        }
        for(Direction dir : orderedDirs) {
            if (rc.canBuildRobot(toBuild, dir)){
                if(toBuild == RobotType.BUILDER && rc.getRoundNum() >= Util.MIN_ROUND_FOR_LAB) {
                    Comms.signalBuilderBuilt();
                }
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
            Comms.resetNumTroopsHealing();
            //todo: zero out the symmetry cluster bit
        }
        Comms.setAlive(archonNumber);
        Comms.setCanBuild(archonNumber, rc.isActionReady());
        Comms.resetAvgEnemyLoc();
        Comms.drawClusterDots();
        isPrioritizedArchon = Comms.canBuildPrioritized(archonNumber, currentState == State.INIT);
        if(isPrioritizedArchon) lastRoundPrioritized = rc.getRoundNum();
        reportEnemies();
        tryUpdateSymmetry();
        boolean underAttack = checkUnderAttack();
        updateRobotCounts();
        updateClosestLeadOre();
        loadArchonLocations();
        boolean isObese = checkForObesity();
        toggleState(underAttack, isObese);
        doStateAction();
        Comms.advanceTurn();
        // Debug.setIndicatorString(leadToUse + "; " + robotCounter + "; num alive enemies: " + Comms.aliveEnemyArchonCount());
        // if (Comms.enemyArchonCount() > 0) {
        //     System.out.println(rc.readSharedArray(Comms.firstEnemy) + "; " + rc.readSharedArray(Comms.firstEnemy + 1) + "; " + rc.readSharedArray(Comms.firstEnemy + 2) + "; " + rc.readSharedArray(Comms.firstEnemy + 3));
        // }
    }
    

    public boolean amImportant() throws GameActionException {
        int numArchons = rc.getArchonCount();
        int[] ArchonOrder = Comms.getArchonOrderGivenClusters();
        int numImportantArchons = ArchonOrder[4];
        Debug.printString("num Imp" + numImportantArchons);
        if (numImportantArchons == numArchons) {
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
        if(rc.getRoundNum() >= Util.MAX_TURN_TO_CHECK_NEARBY_LEAD) {
            closestLeadOre = null;
            return;
        }

        MapLocation[] locs = rc.senseNearbyLocationsWithLead(-1, 2);
        double bestLeadScore = Integer.MIN_VALUE;
        MapLocation bestLeadLoc = null;
        for(MapLocation loc: locs) {
            if(Clock.getBytecodeNum() >= 13000) break;
            double currScore = getLeadDistTradeoffScore(loc, rc.senseLead(loc));
            if(currScore > bestLeadScore && !loc.equals(currLoc)) {
                bestLeadScore = currScore;
                bestLeadLoc = loc;
            }
        }
        if (bestLeadLoc != null) {
            closestLeadOre = bestLeadLoc;
            // Debug.printString("ore: " + closestLeadOre);
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
                initCounter = firstRounds(4, initCounter);
                tryToRepairLastBot();
                break;
            case CHILLING:
                Debug.printString("Chilling");
                // writeLocation();
                if((rc.getRoundNum() < Util.MIN_ROUND_FOR_LAB || Comms.haveBuiltLab())) {
                    if (rc.getTeamGoldAmount(rc.getTeam()) >= RobotType.SAGE.buildCostGold) {
                        currentBuild = Buildable.EMPTY;
                        nextBuild = Buildable.SOLDIER;
                        buildRobot(RobotType.SAGE);
                        break;
                    }
                    if(minerCount <= MIN_NUM_MINERS && soldierCount >= (1/3) * minerCount) {
                        chillingCounter = minerSoldier31(chillingCounter);
                    }
                    else {
                        chillingCounter = minerSoldierRatio(7, chillingCounter);
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
            case UNDER_ATTACK:
                // Debug.printString("Under Attack");
                chillingCounter = buildSoldier(chillingCounter);
                tryToRepairLowestHealth();
                break;
            case OBESITY:
                Debug.printString("Obesity");
                int leadForBuilders = rc.getTeamLeadAmount(rc.getTeam()) - maxLeadUsedByArchons;
                int watchtowersPossible = leadForBuilders / 180;
                if((rc.getRoundNum() < Util.MIN_ROUND_FOR_LAB || Comms.haveBuiltLab())) {
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
                Debug.printString("Going to low rubble spot");
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
        if (!isSmallMap()) {
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
    }


    public void toggleState(boolean underAttack, boolean isObese) throws GameActionException {
        switch (currentState) {
            case INIT:
                // Debug.printString("lead obesity: " + leadObesity);
                if(underAttack) {
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
                } else if(rc.getRoundNum() > lastRoundPrioritized + Util.TURNS_NOT_PRIORITIZED_TO_MOVE &&
                            rc.getRoundNum() > lastRoundMoved + Util.MIN_TURNS_TO_MOVE_AGAIN &&
                            rc.isTransformReady() &&
                            !Comms.existsArchonMoving() &&
                            chooseInitialMoveTarget()) {
                    // Just mark yourself as dead in archon locations so units don't come to get healed
                    rc.writeSharedArray(archonNumber, Comms.DEAD_ARCHON_FLAG);
                    stateStack.push(currentState);
                    changeState(State.MOVING);
                    rc.transform();
                    Comms.setArchonMoving();
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
            case MOVING:
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

    public RobotInfo getNextRobotToRepair() throws GameActionException {
        RobotInfo[] friendlies = rc.senseNearbyRobots(actionRadiusSquared, rc.getTeam());
        RobotInfo maybeSage = null;
        RobotInfo maybeSoldier = null;
        RobotInfo maybeBuilder = null;
        RobotInfo maybeMiner = null;
        RobotInfo friendly = null;
        for (int i = friendlies.length; --i >= 0;) {
            friendly = friendlies[i];
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
            // Switch targets if you have a miner now and a soldier can be healed
            if(lastRobot.type == RobotType.MINER) {
                RobotInfo newRobot = getNextRobotToRepair();
                if(newRobot != null && newRobot.type != RobotType.MINER) {
                    lastRobot = newRobot;
                }
            }
            if(rc.canRepair(lastRobot.location) && lastRobot.health < getMaxHealth(lastRobot.type)) {
                Debug.printString("Healing");
                lastRobotHealed = lastRobot;
                rc.repair(lastRobot.location);
                Debug.setIndicatorLine(Debug.INDICATORS, currLoc, lastRobot.location, 0, 255, 0);
                return;
            }
        }

        tryToRepairLowestHealth();
    }

    // Tries to repair the lowest health droid in range if an action is ready.
    public void tryToRepairLowestHealth() throws GameActionException {
        if(!rc.isActionReady()) return;

        RobotInfo robotToRepair = getNextRobotToRepair();
        if(robotToRepair != null) {
            Debug.printString("Healing");
            rc.repair(robotToRepair.location);
            lastRobotHealed = robotToRepair;
            Debug.setIndicatorLine(Debug.INDICATORS, currLoc, robotToRepair.location, 0, 255, 0);
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
            if(archonLoc.equals(lastClosestArchonToCluster)) return false;
        }
        return true;
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
        MapLocation cluster = getClusterClosestTo(moveTarget);
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

        if(!cluster.isWithinDistanceSquared(moveTarget, Util.MAX_CLUSTER_DIST_CHANGE)) {
            // This can happen if a cluster disappears for a turn.
            // In this case, we don't want to move towards the other one
            // so just keep the same target
            return;
        }

        if(isCharging) {
            moveTarget = cluster;
            return;
        }

        if(lastClosestArchonToCluster.isWithinDistanceSquared(cluster, Util.MIN_DIST_SQUARED_FROM_CLUSTER)) {
            // Small map? Just go to the closest archon
            moveTarget = lastClosestArchonToCluster;
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
        }
    }

    // ~3k bytecode
    public boolean chooseInitialMoveTarget() throws GameActionException {
        MapLocation cluster = getClusterClosestToArchons();
        lastClosestArchonToCluster = currLoc;
        MapLocation farthestArchon = currLoc;
        int maxDist = Integer.MIN_VALUE;
        int minDist = Integer.MAX_VALUE;
        int dist;
        for(MapLocation archonLoc : archonLocations) {
            if(archonLoc == null) continue;
            // Debug.printString(archonLoc.toString());
            dist = archonLoc.distanceSquaredTo(cluster);
            if(dist < minDist) {
                lastClosestArchonToCluster = archonLoc;
                minDist = dist;
            }
            if(dist > maxDist) {
                farthestArchon = archonLoc;
                maxDist = dist;
            }
        }

        // if(lastClosestArchonToCluster.equals(currLoc)) {
        //     // Let's move closer and extend our advantage
        //     moveTarget = cluster;
        //     isCharging = true;
        //     Debug.println("Charging target: " + moveTarget);
        //     return !currLoc.isWithinDistanceSquared(moveTarget, Util.MIN_DIST_TO_MOVE);
        // }

        // Only have the farthest one move
        if(!farthestArchon.equals(currLoc)) {
            // Debug.printString("Not farthest");
            return false;
        }

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
            // Debug.println("Move target far: " + moveTarget);
        }

        return !currLoc.isWithinDistanceSquared(moveTarget, Util.MIN_DIST_TO_MOVE);
    }

    public int getSpotScore(MapLocation loc) throws GameActionException {
        int score = 0;
        MapLocation loc2;
        loc2 = loc.add(Direction.NORTH);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) : 50;
        loc2 = loc.add(Direction.NORTHEAST);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) : 50;
        loc2 = loc.add(Direction.EAST);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) : 50;
        loc2 = loc.add(Direction.SOUTHEAST);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) : 50;
        loc2 = loc.add(Direction.SOUTHWEST);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) : 50;
        loc2 = loc.add(Direction.SOUTHWEST);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) : 50;
        loc2 = loc.add(Direction.WEST);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) : 50;
        loc2 = loc.add(Direction.NORTHWEST);
        score += rc.canSenseLocation(loc2) ? rc.senseRubble(loc2) : 50;

        score += rc.senseRubble(loc) * 20;
        score += Math.sqrt(loc.distanceSquaredTo(currLoc)) * 5;
        return score;
    }

    // Choose a new spot if the new one is significantly better than the last one.
    // Find the minimum rubble spot, breaking ties roughly by
    // the sum of adjacent rubble and the distance to the current location
    public void findGoodSpot() throws GameActionException {
        MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(currLoc, 13);
        MapLocation bestLoc = null;
        MapLocation loc;
        int minScore = Integer.MAX_VALUE;
        int score;
        for(int i = locs.length; --i >= 0;) {
            loc = locs[i];
            if(Clock.getBytecodesLeft() < 4000) {
                Debug.println("had to break finding good spot");
                break;
            }
            if(rc.canSenseRobotAtLocation(loc) || !rc.canSenseLocation(loc)) continue;
            score = getSpotScore(loc);
            if(score < minScore) {
                minScore = score;
                bestLoc = loc;
            }
        }

        if(rc.canSenseLocation(moveTarget) &&
            (!rc.isLocationOccupied(moveTarget) || moveTarget.equals(currLoc))) {
            int currTargetScore = getSpotScore(moveTarget);
            if(currTargetScore > minScore * 2) {
                moveTarget = bestLoc;
                // Debug.println("New good spot: " + moveTarget.toString());
            }
        } else {
            moveTarget = bestLoc;
        }
    }
}