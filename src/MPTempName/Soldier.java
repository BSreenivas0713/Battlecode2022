package MPTempName;

import battlecode.common.*;
import MPTempName.Debug.*;
import MPTempName.Util.*;
import MPTempName.Comms.*;

public class Soldier extends Robot {
    static enum SoldierState {
        EXPLORING,
        GOING_TO_HEAL,
        HEALING,
    }

    static SoldierState currState;
    static int homeFlagIdx;
    static int targetId;
    static MapLocation distressLocation;

    static int numEnemySoldiers;
    static int overallEnemySoldierDx;
    static int overallEnemySoldierDy;

    static int numFriendlySoldiers;
    static int overallFriendlySoldierDx;
    static int overallFriendlySoldierDy;
    static MapLocation avgEnemyLoc;
    static RobotInfo closestEnemy;
    static int numFriendlies;
    static int numEnemies;
    static MapLocation closestAttackingEnemy;
    static int numEnemySoldiersAttackingUs;
    static int lastRoundSawEnemy;

    static RobotInfo[] enemyAttackable;

    static MapLocation healTarget;
    static int healTargetIdx;
    static int healCounter;
    static boolean canHeal;

    public Soldier(RobotController r) throws GameActionException {
        this(r, Comms.firstArchonFlag);
    }

    public Soldier(RobotController r, int homeFlagIndex) throws GameActionException {
        super(r);
        currState = SoldierState.EXPLORING;
        homeFlagIdx = homeFlagIndex;
        canHeal = true;
    } 

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        closestEnemy = getBestEnemy(EnemySensable);
        resetShouldRunAway();
        enemyAttackable = getEnemyAttackable();
        numEnemies = enemyAttackable.length;
        if(numEnemies != 0) lastRoundSawEnemy = rc.getRoundNum();
        avgEnemyLoc = Comms.getClosestCluster(currLoc);
        Comms.incrementSoldierCounter();
        trySwitchState();
        doStateAction();
    }

    public void trySwitchState() throws GameActionException {
        switch(currState) {
            case EXPLORING:
                // Run away if 1/3 health left
                if(rc.getHealth() <= RobotType.SOLDIER.health / 3 ||
                    (rc.getRoundNum() >= lastRoundSawEnemy + Util.MIN_TURNS_NO_ENEMY_TO_HEAL_HALF &&
                        rc.getHealth() <= RobotType.SOLDIER.health / 2 &&
                        !Comms.existsArchonMoving())) {
                    if(canHeal && loadHealTarget()) {
                        currState = SoldierState.GOING_TO_HEAL;
                    }
                }
                break;
            case GOING_TO_HEAL:
                if(rc.getHealth() == RobotType.SOLDIER.health) {
                    currState = SoldierState.EXPLORING;
                } else if(needToReloadTarget()) {
                    if(!reloadTarget()) {
                        currState = SoldierState.EXPLORING;
                    }
                } else if(currLoc.isWithinDistanceSquared(healTarget, RobotType.ARCHON.actionRadiusSquared)) {
                    currState = SoldierState.HEALING;
                    healCounter = 0;
                }
                break;
            case HEALING:
                healCounter++;
                if (healCounter >= Util.HealTimeout) {
                    currState = SoldierState.EXPLORING;
                    canHeal = false;
                } else if(rc.getHealth() == RobotType.SOLDIER.health) {
                    currState = SoldierState.EXPLORING;
                } else if(needToReloadTarget()) {
                    if(!reloadTarget()) {
                        currState = SoldierState.EXPLORING;
                    } else {
                        currState = SoldierState.GOING_TO_HEAL;
                    }
                }
                break;
        }
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
        currState = SoldierState.GOING_TO_HEAL;
        Debug.printString("Reloading");
        return loadHealTarget();
    }

    public void doStateAction() throws GameActionException {
        switch(currState) {
            case EXPLORING:
                if (!tryMoveTowardsEnemy()) {
                    soldierExplore();
                }
                break;
            case GOING_TO_HEAL:
                Comms.incrementNumTroopsHealingAt(healTargetIdx);
                Debug.setIndicatorDot(Debug.INDICATORS, healTarget, 0, 255, 0);
                Debug.printString("Going to heal");
                tryAttackBestEnemy();
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
                if(numEnemies != 0) {
                    tryMoveTowardsEnemy();
                } else if(avgEnemyLoc != null && healTarget != null && avgEnemyLoc.distanceSquaredTo(healTarget) <= RobotType.ARCHON.actionRadiusSquared) {
                    Debug.printString("oter sid");
                    soldierExplore();
                } else {
                    tryAttackBestEnemy();
                    moveMoreSafely(healTarget, Util.HEAL_DIST_TO_HOME);
                }
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

    public void resetShouldRunAway() throws GameActionException {
        numEnemySoldiersAttackingUs = 0;
        numFriendlies = 0;
        closestAttackingEnemy = null;
        numEnemies = 0;
        overallEnemySoldierDx = 0;
        overallEnemySoldierDy = 0;
        int closestSoldierDist = Integer.MAX_VALUE;
        for (RobotInfo bot : EnemySensable) {
            MapLocation candidateLoc = bot.getLocation();
            int candidateDist = currLoc.distanceSquaredTo(candidateLoc);
            boolean isWatchtower = bot.getType() == RobotType.WATCHTOWER;
            if (bot.getType() == RobotType.SOLDIER || isWatchtower) {
                // This code makes it so that we don't consider Portables as attacking enemies - it made our bot worse as of Hooray for spring
                // so it is commented out for now
                /*boolean canAttack = false;
                if(!isWatchtower || bot.getMode() == RobotMode.TURRET) {
                    canAttack = true;
                }
                if(canAttack) {numEnemies++;}*/
                if (candidateDist <= actionRadiusSquared /*&& canAttack*/) {
                    numEnemySoldiersAttackingUs++;
                    overallEnemySoldierDx += currLoc.directionTo(candidateLoc).dx * (100 / (currLoc.distanceSquaredTo(candidateLoc)));
                    overallEnemySoldierDy += currLoc.directionTo(candidateLoc).dy * (100 / (currLoc.distanceSquaredTo(candidateLoc)));
                }
                if (candidateDist < closestSoldierDist) {
                    closestSoldierDist = candidateDist;
                    closestAttackingEnemy = candidateLoc;
                }
            }
        }
        MapLocation closestEnemyLocation = currLoc;
        if(closestAttackingEnemy != null) {
            closestEnemyLocation = closestAttackingEnemy;
        }
        for (RobotInfo Fbot : FriendlySensable) {
            RobotType FbotType = Fbot.getType();
            if (FbotType == RobotType.SOLDIER || FbotType == RobotType.WATCHTOWER) {
                MapLocation FbotLocation = Fbot.getLocation();
                // Debug.printString(" " + FbotLocation + " ");
                if((FbotLocation).distanceSquaredTo(closestEnemyLocation) <= FbotType.visionRadiusSquared) {
                    numFriendlies++;
                }
            }
        }
    }

    public boolean shouldRunAway() {
        //Not only should there be no soldiers attacking us, but if we see 2 soldiers between our action radius and our vision radius, we should not go forward
        //Consider changing the numFriendlies < numEnemies to <= and retesting
        // Debug.printString("enemyAction: " + numEnemySoldiersAttackingUs + "enemy: " + numEnemies + "friends: " + numFriendlies);
        return numEnemySoldiersAttackingUs > 0 || (numFriendlies + 1 < numEnemies);
    }

    public void moveAndAttack(Direction[] targetDirs, boolean attackFirst) throws GameActionException{
        if(attackFirst) {
            tryAttackBestEnemy();
            tryMoveDest(targetDirs);
        }
        else {
            tryMoveDest(targetDirs);
            tryAttackBestEnemy();
        }
    }


    public Direction chooseForwardDirection(MapLocation loc) throws GameActionException {
        Direction Dir = currLoc.directionTo(loc);
        Direction[] dirsToConsider = Util.getInOrderDirections(Dir);
        Direction bestDirSoFar = null;
        int bestRubble = rc.senseRubble(loc) + 2;
        int bestEnemiesSeen = Integer.MAX_VALUE;
        for(Direction newDir: dirsToConsider) {
            if (rc.canMove(newDir)) {
                MapLocation targetLoc = currLoc.add(newDir);
                int locRubble = rc.senseRubble(currLoc);
                int newDirRubble = rc.senseRubble(targetLoc);
                boolean notTooMuchRubble = newDirRubble < (10 + locRubble) && newDirRubble <= bestRubble;
                int currEnemiesSeen = 0;
                for (RobotInfo enemy: enemyAttackable) {
                    MapLocation enemyLoc = enemy.getLocation();
                    if(enemyLoc.distanceSquaredTo(targetLoc) <= actionRadiusSquared) {
                        currEnemiesSeen++;
                    }
                }
                if(notTooMuchRubble && (currEnemiesSeen <= bestEnemiesSeen ||  bestEnemiesSeen == 0)) {
                    if (currEnemiesSeen != 0) {
                        if (currEnemiesSeen < bestEnemiesSeen || bestEnemiesSeen == 0) {
                            bestDirSoFar = newDir;
                            bestRubble = newDirRubble;
                            bestEnemiesSeen = currEnemiesSeen;
                        }
                        else if (currEnemiesSeen == bestEnemiesSeen && newDirRubble < bestRubble) {
                            bestDirSoFar = newDir;
                            bestRubble = newDirRubble;
                            bestEnemiesSeen = currEnemiesSeen;                    
                        }
                    }
                    else {
                        if(bestEnemiesSeen == Integer.MAX_VALUE) {
                            if (newDirRubble < bestRubble) {
                                bestDirSoFar = newDir;
                                bestRubble = newDirRubble;
                                bestEnemiesSeen = currEnemiesSeen;                                 
                            }
                        }
                    }
                }                
            }
        }
        return bestDirSoFar;
    }        

    public Direction chooseBackupDirection(Direction Dir) throws GameActionException {
        Direction[] dirsToConsider = Util.getInOrderDirections(Dir);
        // Debug.printString("dir:" + Dir + " " + rc.getActionCooldownTurns());
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
                for (RobotInfo enemy: enemyAttackable) {
                    MapLocation enemyLoc = enemy.getLocation();
                    int enemyActionRadius = enemy.getType().actionRadiusSquared;
                    if(enemyLoc.distanceSquaredTo(targetLoc) <= enemyActionRadius) {
                        currEnemiesStillSeen++;
                    }
                }
                if(notTooMuchRubble && currEnemiesStillSeen <= bestEnemiesStillSeen) {
                    // Debug.printString("R");
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
        return bestDirSoFar;
    }

    public boolean tryMoveTowardsEnemy() throws GameActionException {
        // move towards it if found
        boolean alreadyCalculated = false;
        if (closestEnemy != null) {
            MapLocation dest;
            Direction dir = null;
            boolean attackFirst = false;
            if(shouldRunAway()) {
                attackFirst = true;
                // Positive so that we move towards the point mass.
                dest = currLoc.translate(-overallEnemySoldierDx, -overallEnemySoldierDy);//(overallFriendlySoldierDx, overallFriendlySoldierDy);
                Direction possibleDir = currLoc.directionTo(dest);
                dir = chooseBackupDirection(possibleDir);
                Debug.printString("t: " + rc.getMovementCooldownTurns() + " " + dir);
                if (dir == null) {
                    Debug.printString("RA bad");
                    tryAttackBestEnemy();
                    return true;
                }
                Debug.printString("RA, Dest: " + dir);
                Direction[] targetDirs = Util.getInOrderDirections(dir);
                moveAndAttack(targetDirs, attackFirst);
                return true;
            } else {
                dest = closestEnemy.getLocation();
                RobotType closestEnemyType = closestEnemy.getType();
                if(closestEnemyType == RobotType.MINER || closestEnemyType == RobotType.ARCHON || closestEnemyType == RobotType.BUILDER) {
                    dir = Nav.navTo(dest);
                    MapLocation targetLoc = currLoc.add(dir);

                    //rubble check
                    int locRubble = Util.getRubble(targetLoc);
                    int currRubble = rc.senseRubble(currLoc);
                    if(rc.onTheMap(targetLoc) && locRubble > (20 + 1.2 * currRubble) && closestEnemyType!= RobotType.ARCHON) {
                        Debug.printString("rub high");
                        tryAttackBestEnemy();
                        return true;
                    }
                    //We're already close to a non-attacking enemy, and moving would put us in lower passability
                    int distanceNeeded = 5;
                    //This can be changed if Archons start running away from us
                    if(closestEnemyType == RobotType.ARCHON) {distanceNeeded = actionRadiusSquared;}
                    if(currLoc.distanceSquaredTo(dest) <= distanceNeeded && locRubble > currRubble) {
                        Debug.printString("close");
                        tryAttackBestEnemy();
                        return true;
                    }
                    Debug.printString("enemyfren");
                    Direction[] targetDirs = Util.getInOrderDirections(dir);
                    moveAndAttack(targetDirs, attackFirst);
                    return true;
                }
                else {
                    dir = chooseForwardDirection(dest);
                    attackFirst = false;
                    if (dir == null) {
                        Debug.printString("Fw bad");
                        tryAttackBestEnemy();
                        return true;
                    }
                    Debug.printString("Fw, Dest: " + dir);
                    Direction[] targetDirs = Util.getInOrderDirections(dir);
                    moveAndAttack(targetDirs, attackFirst);
                    return true;
                }
            }
        }
        return false;
    }


    public void soldierExplore() throws GameActionException {
        MapLocation target;
        if(avgEnemyLoc != null) {
            target = avgEnemyLoc;
            Debug.printString("going for it");
        } else {
            boolean seeArchonInSensable = false;
            for (RobotInfo bot : EnemySensable) {
                if (bot.getType() == RobotType.ARCHON) {
                    seeArchonInSensable = true;
                }
            }
            if (!seeArchonInSensable) {
                Comms.broadcastSoldierNearClusterButNothingFound();
            }
            target = Explore.getLegacyExploreTarget();
            Debug.printString("Exploring: " + target.toString());
        }
        if (currLoc.distanceSquaredTo(target) <= Util.JUST_OUTSIDE_OF_VISION_RADIUS) {
            Nav.tryMoveSafely(target);
            Debug.printString("saf mov");
        }
        else {
            Nav.move(target);
            Debug.printString("reg mov");
        }
    }
}
