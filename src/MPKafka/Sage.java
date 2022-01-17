package MPKafka;

import battlecode.common.*;
import MPKafka.Debug.*;
import MPKafka.Util.*;

public class Sage extends Robot{
    static enum SageState {
        EXPLORING,
        GOING_TO_HEAL,
        HEALING,
    }

    static SageState currState;

    static MapLocation healTarget;
    static int healTargetIdx;
    static int healCounter;
    static boolean canHeal;

    static int homeFlagIdx;
    static MapLocation home;
    static int targetId;
    static boolean isRunning;
    static int runSemaphore;
    static Direction runDirection;
    static RobotInfo[] victims;

    static int overallAttackingEnemyDx;
    static int overallAttackingEnemyDy;
    static int numAttackingEnemies;
    static MapLocation averageAttackingEnemyLocation;

    public Sage(RobotController r) throws GameActionException {
        this(r, Comms.firstArchonFlag);
    }

    public Sage(RobotController r, int homeFlagIndex) throws GameActionException {
        super(r);
        home = Comms.locationFromFlag(rc.readSharedArray(homeFlagIndex - Comms.mapLocToFlag));
        homeFlagIdx = homeFlagIndex;
        isRunning = false;
        currState = SageState.EXPLORING;
        canHeal = true;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Debug.printString("Cool: " + rc.getActionCooldownTurns());
        if (isRunning) {
            runSemaphore--;
        }
        victims = getVictims();
        boolean almostReady = rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT + GameConstants.COOLDOWNS_PER_TURN;
        tryAttackArchon();
        getAverage();
        trySwitchState();
        doSageAction(almostReady);
    }

    public RobotInfo[] getVictims() throws GameActionException {
        return rc.senseNearbyRobots(RobotType.SAGE.actionRadiusSquared, rc.getTeam().opponent());
    }

    public void tryAttackArchon() throws GameActionException {
        if (!rc.isActionReady()) {
            return;
        }
        for (RobotInfo robot : victims) {
            if (robot.type == RobotType.ARCHON) {
                Debug.printString("Envisioning Fury");
                rc.envision(AnomalyType.FURY);
                return;
            }
        }
        for (RobotInfo robot : EnemySensable) {
            if (robot.type == RobotType.ARCHON) {
                Nav.move(robot.location);
                if (rc.canAttack(robot.location)) {
                    Debug.printString("Envisioning Fury");
                    rc.envision(AnomalyType.FURY);
                    return;
                }
            }
        }
    }

    public void trySwitchState() throws GameActionException {
        switch(currState) {
            case EXPLORING:
                // Run away if 1/3 health left
                if(rc.getHealth() <= robotType.health / 3 ||
                    (numAttackingEnemies == 0 && rc.getHealth() <= robotType.health / 2 && !Comms.existsArchonMoving())) {
                    if(canHeal && loadHealTarget()) {
                        currState = SageState.GOING_TO_HEAL;
                    }
                }
                break;
            case GOING_TO_HEAL:
                if(rc.getHealth() == robotType.health) {
                    currState = SageState.EXPLORING;
                } else if(needToReloadTarget()) {
                    if(!reloadTarget()) {
                        currState = SageState.EXPLORING;
                    }
                } else if(currLoc.isWithinDistanceSquared(healTarget, RobotType.ARCHON.actionRadiusSquared)) {
                    currState = SageState.HEALING;
                    healCounter = 0;
                }
                break;
            case HEALING:
                healCounter++;
                if (healCounter == Util.HealTimeout) {
                    currState = SageState.EXPLORING;
                    canHeal = false;
                } else if(rc.getHealth() == robotType.health) {
                    currState = SageState.EXPLORING;
                } else if(needToReloadTarget()) {
                    if(!reloadTarget()) {
                        currState = SageState.EXPLORING;
                    } else {
                        currState = SageState.GOING_TO_HEAL;
                    }
                }
                break;
        }
    }

    public void doSageAction(boolean almostReady) throws GameActionException {
        switch(currState) {
            case EXPLORING:
                if (numAttackingEnemies > 0) {
                    if (almostReady) {
                        Debug.printString("Fighting!");
                        isRunning = false;
                        Nav.move(averageAttackingEnemyLocation);
                        tryAttack();
                    } else {
                        Debug.printString("Running!");
                        Direction dir = averageAttackingEnemyLocation.directionTo(currLoc);
                        Direction[] dirs = Util.getInOrderDirections(chooseBackupDirection(dir));
                        tryMoveDest(dirs);
                        runSemaphore = 5;
                        isRunning = true;
                        runDirection = dir;
                    }
                } else {
                    if (!isRunning || runSemaphore <= 0) {
                        isRunning = false;
                        moveTowardsCluster();
                    } else {
                        Debug.printString("Running!");
                        Direction[] dirs = Util.getInOrderDirections(chooseBackupDirection(runDirection));
                        tryMoveDest(dirs);
                    }
                }
                break;
            case GOING_TO_HEAL:
                Comms.incrementNumTroopsHealingAt(healTargetIdx);
                Debug.setIndicatorDot(Debug.INDICATORS, healTarget, 0, 255, 0);
                Debug.printString("Going to heal");
                tryAttack();
                if(currLoc.isWithinDistanceSquared(healTarget, 4 * Util.HEAL_DIST_TO_HOME)) {
                    moveMoreSafely(healTarget, Util.HEAL_DIST_TO_HOME);
                } else {
                    Nav.move(healTarget);
                }
                break;
            case HEALING:
                Comms.incrementNumTroopsHealingAt(healTargetIdx);
                Debug.setIndicatorDot(Debug.INDICATORS, healTarget, 0, 255, 0);
                Debug.printString("Healing");
                tryAttack();
                moveMoreSafely(healTarget, Util.HEAL_DIST_TO_HOME);
                break;

        }
    }

    public int estimateWaitTimeAt(int targetIdx, int prioritizedArchon) throws GameActionException {
        MapLocation archonLoc = archonLocations[targetIdx];
        int numHealing = Comms.getNumTroopsHealingAt(targetIdx);
        int archonHealRate = -RobotType.ARCHON.damage;
        int waitTime = numHealing * Util.AVERAGE_HEALTH_TO_HEAL / archonHealRate / 2 + (int)Math.sqrt(currLoc.distanceSquaredTo(archonLoc));
        if(prioritizedArchon == targetIdx) waitTime += numHealing;
        return waitTime;
    }

    // Weight the prioritized archon less
    // Returns whether we found a target
    public boolean loadHealTarget() throws GameActionException {
        loadArchonLocations();
        int prioritizedArchon = Comms.getPrioritizedArchon() - 1;
        healTarget = null;
        int bestWait = Integer.MAX_VALUE;
        int bestWaitIdx = -1;
        for(int i = 0; i < Comms.friendlyArchonCount(); i++) {
            MapLocation archonLoc = archonLocations[i];
            if(archonLoc == null || Comms.isAtHealingCap(i)) continue;
            int waitTime = estimateWaitTimeAt(i, prioritizedArchon);
            if(waitTime < bestWait) {
                bestWait = waitTime;
                bestWaitIdx = i;
            }
        }

        if(bestWaitIdx != -1) {
            healTarget = archonLocations[bestWaitIdx];
            healTargetIdx = bestWaitIdx;
        }

        return healTarget != null;
    }

    public boolean needToReloadTarget() throws GameActionException {
        loadArchonLocations();
        for(MapLocation archonLoc : archonLocations) {
            if(archonLoc == null) continue;
            if(archonLoc.equals(healTarget)) return false;
        }
        return true;
    }

    public boolean reloadTarget() throws GameActionException {
        Comms.markArchonDead(healTarget);
        currState = SageState.GOING_TO_HEAL;
        Debug.printString("Reloading");
        return loadHealTarget();
    }

    public void getAverage() throws GameActionException {
        averageAttackingEnemyLocation = null;
        overallAttackingEnemyDx = 0;
        overallAttackingEnemyDy = 0;
        numAttackingEnemies = 0;
        RobotInfo robot;
        MapLocation loc;
        for (int i = EnemySensable.length; --i >= 0;) {
            robot = EnemySensable[i];
            switch(robot.type) {
                case SAGE:
                case SOLDIER:
                case WATCHTOWER:
                    loc = robot.location;
                    overallAttackingEnemyDx += loc.x;
                    overallAttackingEnemyDy += loc.y;
                    numAttackingEnemies++;
            }
        }
        if (numAttackingEnemies != 0) {
            averageAttackingEnemyLocation = new MapLocation(overallAttackingEnemyDx / numAttackingEnemies, overallAttackingEnemyDy / numAttackingEnemies);
        }
    }    

    public void tryAttack() throws GameActionException {
        MapLocation target = findTarget();
        if (target == null && victims.length > 0 && rc.canEnvision(AnomalyType.CHARGE)) {
            Debug.printString("Envisioning Charge");
            rc.envision(AnomalyType.CHARGE);
        } else if (target != null && victims.length > 0 && rc.canAttack(target)) {
            Debug.printString("Attacking " + target);
            rc.attack(target);
        }
    }

    public MapLocation findTarget() throws GameActionException {
        int totalHealth = 0;
        MapLocation bestSoldier = null;
        int bestSoldierHealth = 0;
        for (RobotInfo robot : victims) {
            switch (robot.type) {
                case SOLDIER:
                    if (robot.health > bestSoldierHealth) {
                        bestSoldierHealth = robot.health;
                        bestSoldier = robot.location;
                    }
                    // Intentional fallthrough
                case MINER:
                case BUILDER:
                case SAGE:
                    totalHealth += robot.type.getMaxHealth(1) / 10;
                default:
                    break;
            }
        }
        if (bestSoldierHealth > 45) {
            bestSoldierHealth = 45;
        }
        if (bestSoldierHealth >= totalHealth) {
            return bestSoldier;
        } else {
            return null;
        }
    }

    public void moveTowardsCluster() throws GameActionException {
        Debug.printString("Following clusters");
        MapLocation clusterLoc = Comms.getClosestCluster(currLoc);
        Nav.move(clusterLoc);
    }

    // Copied from Soldier code
    public Direction chooseBackupDirection(Direction Dir) throws GameActionException {
        Direction[] dirsToConsider = Util.getInOrderDirections(Dir);
        Direction bestDirSoFar = null;
        int bestRubble = 101;
        int bestEnemiesStillSeen = Integer.MAX_VALUE;
        for(Direction newDir: dirsToConsider) {
            if (rc.canMove(newDir)) {
                MapLocation targetLoc = currLoc.add(newDir);
                int locRubble = rc.senseRubble(currLoc);
                int newDirRubble = rc.senseRubble(targetLoc);
                boolean notTooMuchRubble = newDirRubble < (10 + locRubble);
                int currEnemiesStillSeen = 0;
                for (RobotInfo enemy: getEnemyAttackable()) {
                    MapLocation enemyLoc = enemy.getLocation();
                    int enemyActionRadius = enemy.getType().actionRadiusSquared;
                    if(enemyLoc.distanceSquaredTo(targetLoc) <= enemyActionRadius) {
                        currEnemiesStillSeen++;
                    }
                }
                if(notTooMuchRubble && currEnemiesStillSeen <= bestEnemiesStillSeen) {
                    if (currEnemiesStillSeen < bestEnemiesStillSeen) {
                        bestDirSoFar = newDir;
                        bestRubble = newDirRubble;
                        bestEnemiesStillSeen = currEnemiesStillSeen;
                    }
                    else if (currEnemiesStillSeen == bestEnemiesStillSeen && newDirRubble < bestRubble) {
                        bestDirSoFar = newDir;
                        bestRubble = newDirRubble;
                        bestEnemiesStillSeen = currEnemiesStillSeen;                    
                    }
                }                
            }
        }
        if (bestDirSoFar == null) {
            return Dir;
        } else {
            return bestDirSoFar;
        }
    }
}