package MPTempName;

import battlecode.common.*;
import MPTempName.Debug.*;
import MPTempName.Util.*;

public class Sage extends Robot{

    static int homeFlagIdx;
    static MapLocation home;
    static int targetId;
    static boolean isRunning;
    static int runSemaphore;
    static Direction runDirection;
    static RobotInfo[] victims;

    public Sage(RobotController r) throws GameActionException {
        this(r, Comms.firstArchonFlag);
    }

    public Sage(RobotController r, int homeFlagIndex) throws GameActionException {
        super(r);
        home = Comms.locationFromFlag(rc.readSharedArray(homeFlagIndex - Comms.mapLocToFlag));
        homeFlagIdx = homeFlagIndex;
        isRunning = false;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (isRunning) {
            runSemaphore--;
        }
        victims = getVictims();
        boolean almostReady = rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT + GameConstants.COOLDOWNS_PER_TURN;
        tryAttackArchon();
        int soldierCount = scanSoldiers();
        doSageAction(soldierCount, almostReady);
    }

    public RobotInfo[] getVictims() throws GameActionException {
        return rc.senseNearbyRobots(RobotType.SAGE.actionRadiusSquared, rc.getTeam().opponent());
    }

    public void tryAttackArchon() throws GameActionException {
        for (RobotInfo robot : victims) {
            if (robot.type == RobotType.ARCHON && rc.canEnvision(AnomalyType.FURY)) {
                Debug.printString("Envisioning Fury");
                rc.envision(AnomalyType.FURY);
                return;
            }
        }
    }

    public int scanSoldiers() throws GameActionException {
        int count = 0;
        for (RobotInfo robot : EnemySensable) {
            if (robot.type == RobotType.SOLDIER) {
                count++;
            }
        }
        return count;
    }

    public void doSageAction(int soldierCount, boolean almostReady) throws GameActionException {
        if (soldierCount > 0) {
            MapLocation averageEnemyLoc = getAverage();
            if (almostReady) {
                Debug.printString("Fighting!");
                isRunning = false;
                Nav.move(averageEnemyLoc);
                tryAttack();
            } else {
                Debug.printString("Cool: " + rc.getActionCooldownTurns());
                Debug.printString("Running!");
                Direction dir = averageEnemyLoc.directionTo(currLoc);
                Direction[] dirs = Util.getInOrderDirections(chooseBackupDirection(dir));
                tryMoveDest(dirs);
                runSemaphore = 10;
                isRunning = true;
                runDirection = dir;
            }
        } else {
            if (!isRunning || runSemaphore == 0) {
                isRunning = false;
                moveTowardsCluster();
            } else {
                Debug.printString("Cool: " + rc.getActionCooldownTurns());
                Debug.printString("Running!");
                Direction[] dirs = Util.getInOrderDirections(chooseBackupDirection(runDirection));
                tryMoveDest(dirs);
            }
        }
    }

    public MapLocation getAverage() throws GameActionException {
        MapLocation averageLoc = null;
        int totalX = 0;
        int totalY = 0;
        int numFound = 0;
        for (RobotInfo robot : EnemySensable) {
            MapLocation loc = robot.location;
            totalX += loc.x;
            totalY += loc.y;
            numFound++;
        }
        if (numFound != 0) {
            averageLoc = new MapLocation(totalX / numFound, totalY / numFound);
        }
        return averageLoc;
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
                case MINER:
                case BUILDER:
                case SAGE:
                    totalHealth += robot.type.getMaxHealth(1) / 10;
                default:
                    break;
            }
        }
        Debug.printString("T: " + totalHealth);
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