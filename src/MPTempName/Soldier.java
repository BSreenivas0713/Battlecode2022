package MPTempName;

import battlecode.common.*;
import MPTempName.Debug.*;
import MPTempName.Util.*;
import MPTempName.Comms.*;

public class Soldier extends Robot {
    static enum SoldierState {
        EXPLORING,
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

    static RobotInfo[] enemyAttackable;

    static MapLocation healTarget;

    public Soldier(RobotController r) throws GameActionException {
        this(r, Comms.firstArchonFlag);
    }

    public Soldier(RobotController r, int homeFlagIndex) throws GameActionException {
        super(r);
        currState = SoldierState.EXPLORING;
        homeFlagIdx = homeFlagIndex;
    } 

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        closestEnemy = getBestEnemy(EnemySensable);
        resetShouldRunAway();
        enemyAttackable = getEnemyAttackable();
        numEnemies = enemyAttackable.length;
        avgEnemyLoc = Comms.getClosestCluster(currLoc);
        Comms.incrementSoldierCounter();
        trySwitchState();
        doStateAction();
    }

    public void trySwitchState() throws GameActionException {
        switch(currState) {
            case EXPLORING:
                // Run away if 1/3 health left
                if(rc.getHealth() <= RobotType.SOLDIER.health / 3) {
                    currState = SoldierState.HEALING;
                    loadHealTarget();
                }
                break;
            case HEALING:
                if(rc.getHealth() == RobotType.SOLDIER.health) {
                    currState = SoldierState.EXPLORING;
                }
                // TODO: maybe also exit if there are a lot of units to be healed
                // and you've healed to 2/3?
                break;
        }
    }

    public void doStateAction() throws GameActionException {
        switch(currState) {
            case EXPLORING:
                if (!tryMoveTowardsEnemy()) {
                    soldierExplore();
                }
                break;
            case HEALING:
                Debug.setIndicatorDot(Debug.INDICATORS, healTarget, 0, 255, 0);
                Debug.printString("Healing");
                tryAttackBestEnemy();
                moveMoreSafely(healTarget, Util.HEAL_DIST_TO_HOME);
                break;
        }
    }

    // Choose an archon inversely proportional to the distance to it
    // Weight the prioritized archon less
    public void loadHealTarget() throws GameActionException {
        int prioritizedArchon = Comms.getPrioritizedArchon() - 1;
        healTarget = null;
        double[] probs = new double[Comms.friendlyArchonCount()];
        double totalProb = 0;
        for(int i = 0; i < Comms.friendlyArchonCount(); i++) {
            MapLocation archonLoc = archonLocations[i];
            probs[i] = 0;
            if(archonLoc == null) continue;
            Debug.printString("D: " + currLoc.distanceSquaredTo(archonLoc));
            probs[i] = 1.0 / currLoc.distanceSquaredTo(archonLoc);
            if(i == prioritizedArchon) probs[i] /= 4;
            // dists[i] = 1.0 / Util.distance(currLoc, archonLoc);
            totalProb += probs[i];
        }

        double r = Util.rng.nextDouble() * totalProb;
        for(int i = 0; i < Comms.friendlyArchonCount(); i++) {
            if(r <= probs[i]) {
                healTarget = archonLocations[i];
                break;
            }
            r -= probs[i];
        }

        if(healTarget == null) {
            healTarget = archonLocations[prioritizedArchon];
        }
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


    public Direction chooseForwardDirection(Direction Dir) throws GameActionException {
        Direction[] dirsToConsider = Util.getInOrderDirections(Dir);
        Direction bestDirSoFar = null;
        int bestRubble = 101;
        int bestEnemiesSeen = Integer.MAX_VALUE;
        for(Direction newDir: dirsToConsider) {
            if (rc.canMove(newDir)) {
                MapLocation targetLoc = currLoc.add(newDir);
                int locRubble = rc.senseRubble(currLoc);
                int newDirRubble = rc.senseRubble(targetLoc);
                boolean notTooMuchRubble = newDirRubble < (10 + locRubble);
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
                    dir = chooseForwardDirection(currLoc.directionTo(dest));
                    attackFirst = false;
                    if (dir == null) {
                        Debug.printString("RA bad");
                        tryAttackBestEnemy();
                        return true;
                    }
                    Debug.printString("RA, Dest: " + dir);
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
        if (avgEnemyLoc != null && currLoc.distanceSquaredTo(avgEnemyLoc) > visionRadiusSquared) {
            target = avgEnemyLoc;
            Debug.printString("Avg enemy: " + target.toString());
        } else {
            target = Explore.getLegacyExploreTarget();
            Debug.printString("Exploring: " + target.toString());
        }
        Nav.move(target);
    }
}
