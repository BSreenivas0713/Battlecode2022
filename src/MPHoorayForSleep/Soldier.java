package MPHoorayForSleep;

import battlecode.common.*;
import MPHoorayForSleep.Debug.*;
import MPHoorayForSleep.Util.*;
import MPHoorayForSleep.Comms.*;

public class Soldier extends Robot {
    static enum SoldierState {
        DEFENSE,
        RUSHING,
        DONE_RUSHING,
        EXPLORING,
        HELPING,
    }
    static SoldierState currState;
    static int homeFlagIdx;
    static MapLocation target;
    static int targetId;
    static MapLocation distressLocation;

    static int numEnemySoldiers;
    static int overallEnemySoldierDx;
    static int overallEnemySoldierDy;

    static int numFriendlySoldiers;
    static int randomnum;
    static int overallFriendlySoldierDx;
    static int overallFriendlySoldierDy;
    static MapLocation avgEnemyLoc;
    static RobotInfo closestEnemy;
    static int numFriendlies;
    static int numEnemies;
    static int numEnemySoldiersAttackingUs;

    public Soldier(RobotController r) throws GameActionException {
        this(r, Comms.firstArchonFlag);
    }

    public Soldier(RobotController r, int homeFlagIndex) throws GameActionException {
        super(r);
        currState = SoldierState.EXPLORING;
        homeFlagIdx = homeFlagIndex;
        numEnemySoldiers = 0;
        overallEnemySoldierDx = 0;
        overallEnemySoldierDy = 0;
        numFriendlySoldiers = 0;
        overallFriendlySoldierDx = 0;
        overallFriendlySoldierDy = 0;
        randomnum = Util.rng.nextInt(2);
    } 

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        closestEnemy = getClosestEnemy();
        findFriendlySoldiers();
        resetShouldRunAway();
        avgEnemyLoc = Comms.getClosestCluster(currLoc);
        // if(avgEnemyLoc != null) {
        //     rc.setIndicatorDot(avgEnemyLoc, 0, 255, 0);
        // }
        Debug.printString(avgEnemyLoc + "");
        // Debug.setIndicatorLine(Debug.INDICATORS, currLoc, avgEnemyLoc, 255, 255, 0);
        Comms.incrementRushSoldierCounter();
        trySwitchState();
        switch (currState) {
            case DEFENSE:
                Debug.printString("Defending");
                if (!tryAttackBestEnemy()) {
                    if (!tryMoveTowardsEnemy()) {
                        latticeAroundHome();
                    }
                }
                break;
            case RUSHING:
                Debug.printString("Rushing");
                tryAttackBestEnemy();
                moveTowardsWeakestArchon();
                break;
            case DONE_RUSHING:
                Debug.printString("Done rushing");
                if (!tryAttackBestEnemy()) {
                    if (!tryMoveTowardsEnemy()) {
                        latticeAroundHomeAfterRushing();
                    }
                }
                break;
            case EXPLORING:
                tryAttackBestEnemy();
                if (!tryMoveTowardsEnemy()) { 
                    soldierExplore();
                }
                break;
            case HELPING: 
                Debug.printString("Helping");
                tryAttackBestEnemy();
                moveTowardsDistressedArchon();
                break;
            default:
                break;
        }
        // try to move if not moved in above functions
    }

    public void trySwitchState() throws GameActionException {
        int maxHelpers;
        int bestDistance;
        maxHelpers = Comms.readMaxHelper();
        bestDistance = Util.MAP_MAX_DIST_SQUARED / 4;
        distressLocation = null;
        int distressLocationIdx = 0;

        // for(int x = Comms.firstArchonFlag; x < Comms.firstArchonFlag + 4; x++) {
        //     int flag = rc.readSharedArray(x);
        //     if(Comms.getICFromFlag(flag) == Comms.InformationCategory.UNDER_ATTACK) {
        //         int locationIndex = x - Comms.mapLocToFlag;
        //         MapLocation archonInTrouble = Comms.locationFromFlag(rc.readSharedArray(locationIndex));
        //         int distance = rc.getLocation().distanceSquaredTo(archonInTrouble);
        //         // either there aren't enough helpers and you're on the right half of the map
        //         // or you're close to the distressed archon already
        //         if (distance < bestDistance) {
        //             distressLocationIdx = locationIndex;
        //             currState = SoldierState.HELPING;
        //             distressLocation = archonInTrouble;
        //         }
        //     }
        // }
        // if (distressLocation != null)  {
        //     //only update counter for the final one you're gonna help
        //     // Comms.incrementHelpersForArchon(distressLocationIdx);
        //     return;
        // }
        // else if (currState != SoldierState.RUSHING && 
        //     Comms.getSoldierCatFromFlag(rc.readSharedArray(Comms.SOLDIER_STATE_IDX)) == Comms.SoldierStateCategory.RUSH_SOLDIERS) {
        //     currState = SoldierState.RUSHING;
        //     MapLocation[] targetAndId = findWeakestArchon(Comms.enemyArchonCount());
        //     target = targetAndId[0];
        //     targetId = targetAndId[1].x;
        // }
        // else if (currState == SoldierState.RUSHING && Comms.isArchonDead(targetId)) {
        //     target = null;
        //     targetId = 0;
        //     currState = SoldierState.EXPLORING;
        // }
        // else if(currState != SoldierState.RUSHING){
        //     currState = SoldierState.EXPLORING;
        // }
    }

    public RobotInfo getClosestEnemy() {
        RobotInfo robot;
        RobotInfo closestRobot = null;
        int leastDistance = Integer.MAX_VALUE;
        int currDistance;
        MapLocation loc;
        numEnemySoldiers = 0;
        overallEnemySoldierDx = 0;
        overallEnemySoldierDy = 0;

        for (int i = EnemySensable.length - 1; i >= 0; i--) {
            robot = EnemySensable[i];
            loc = robot.location;
            currDistance = robot.getLocation().distanceSquaredTo(currLoc);
            if(leastDistance > currDistance) {
                leastDistance = currDistance;
                closestRobot = robot;
            }

            if(robot.type == RobotType.SOLDIER && currDistance < actionRadiusSquared) {
                numEnemySoldiers++;
                overallEnemySoldierDx += currLoc.directionTo(loc).dx * (10000 / (currLoc.distanceSquaredTo(loc)));
                overallEnemySoldierDy += currLoc.directionTo(loc).dy * (10000 / (currLoc.distanceSquaredTo(loc)));
            }
        }

        return closestRobot;
    }

    public void findFriendlySoldiers() {
        overallFriendlySoldierDx = 0;
        overallFriendlySoldierDy = 0;
        numFriendlySoldiers = 0;

        // get averaged overall dx and overall dy to friendly sensables
        RobotInfo friend;
        MapLocation loc;
        for (int i = FriendlySensable.length - 1; i >= 0; i--) {
            friend = FriendlySensable[i];
            loc = friend.location;
            if (friend.getType() == RobotType.SOLDIER) {
                numFriendlySoldiers++;
                overallFriendlySoldierDx += currLoc.directionTo(loc).dx * (10000 / (currLoc.distanceSquaredTo(loc) + 1));
                overallFriendlySoldierDy += currLoc.directionTo(loc).dy * (10000 / (currLoc.distanceSquaredTo(loc) + 1));
            }
        }

        // Weight home Archon ever so slightly.
        // Only really has an effect when no soldiers are seen
        // Primarily for running towards other soldiers/home when running away from enemies.
        overallFriendlySoldierDx += currLoc.directionTo(home).dx * (10000 / (currLoc.distanceSquaredTo(home) + 1));
        overallFriendlySoldierDy += currLoc.directionTo(home).dy * (10000 / (currLoc.distanceSquaredTo(home) + 1));
        numFriendlySoldiers++;
    }

    public void resetShouldRunAway() throws GameActionException {
        numEnemySoldiersAttackingUs = 0;
        numFriendlies = 0;
        numEnemies = 0;
        MapLocation closestEnemySoldier = null;
        int closestSoldierDist = Integer.MAX_VALUE;
        for (RobotInfo bot : EnemySensable) {
            MapLocation candidateLoc = bot.getLocation();
            int candidateDist = currLoc.distanceSquaredTo(candidateLoc);
            if (bot.getType() == RobotType.SOLDIER || bot.getType() == RobotType.WATCHTOWER) {
                numEnemies++;
                if (candidateDist <= actionRadiusSquared) {
                    numEnemySoldiersAttackingUs++;
                }
                if (candidateDist < closestSoldierDist) {
                    closestSoldierDist = candidateDist;
                    closestEnemySoldier = candidateLoc;
                }
            }
        }
        for (RobotInfo Fbot : FriendlySensable) {
            if (Fbot.getType() == RobotType.SOLDIER || Fbot.getType() == RobotType.WATCHTOWER) {
                numFriendlies++;
            }
        }
    }

    public boolean shouldRunAway() {
        //Not only should there be no soldiers attacking us, but if we see 2 soldiers between our action radius and our vision radius, we should not go forward
        //Consider changing the numFriendlies < numEnemies to <= and retesting
        Debug.printString("enemyAction: " + numEnemySoldiersAttackingUs + "enemy: " + numEnemies + "friends: " + numFriendlies);
        return numEnemySoldiersAttackingUs > 0 || (numFriendlies < numEnemies);
    }

    public boolean tryMoveTowardsEnemy() throws GameActionException {
        // move towards it if found
        if (closestEnemy != null) {
            MapLocation dest;
            if(shouldRunAway()) {
                // Positive so that we move towards the point mass.
                dest = currLoc.translate(-overallEnemySoldierDx, -overallEnemySoldierDy);//(overallFriendlySoldierDx, overallFriendlySoldierDy);
                Debug.printString("Running away, Dest: " + dest);
            } else {
                dest = closestEnemy.getLocation();
                Debug.printString("Going towards closest Enemy");
            }
            if(dest != null) {
                Direction dir = Nav.getBestDir(dest);
                //Don't go towards miners if it forces us to go to low passability squares(the formula I used is kind of arbitrary, so its definitely tweakable)
                //keep in mind, however, that on Intersection its like passability 1 versus 85 so any formula thats halfway decent will work there
                MapLocation targetLoc = currLoc.add(dir);
                if(closestEnemy.getType() == RobotType.MINER && rc.onTheMap(targetLoc) && 
                    rc.senseRubble(targetLoc) > (20 + 1.2 * rc.senseRubble(currLoc))) {
                    return false;
                }
                else {
                    Direction[] targetDirs = Util.getInOrderDirections(dir);
                    tryMoveDest(targetDirs);
                    return true;
                }
            }
        }
        return false;
    }

    public void latticeAroundHome() throws GameActionException {

        // move towards this dest
        MapLocation dest;
        if (numFriendlySoldiers >= 3) {
            // This is negative since we're moving *away* from the point mass
            dest = currLoc.translate(-overallFriendlySoldierDx, -overallFriendlySoldierDy);
        }
        else {
            //move toward from home if no soldier found within sensing radius
            dest = home;
        }
        Direction dir = Nav.getBestDir(dest);
        Direction[] targetDirs = Util.getInOrderDirections(dir);
        tryMoveDest(targetDirs);
    }

    public void latticeAroundHomeAfterRushing() throws GameActionException {

        // move towards this dest
        MapLocation dest;
        if (numFriendlySoldiers >= 3 && currLoc.distanceSquaredTo(home) < 4 * RobotType.ARCHON.visionRadiusSquared) {
            dest = currLoc.translate(-overallFriendlySoldierDx, -overallFriendlySoldierDy);
        }
        else {
            //move toward from home if no soldier found within sensing radius
            dest = home;
        }
        Direction dir = Nav.getBestDir(dest);
        Direction[] targetDirs = Util.getInOrderDirections(dir);
        tryMoveDest(targetDirs);
    }

    public MapLocation[] findWeakestArchon(int theirArchons) throws GameActionException {
        int leastHealth = Comms.NUM_HEALTH_BUCKETS;
        MapLocation bestLoc = null;
        int idxOfBestLoc = 0;
        for (int i = Comms.firstEnemy; i < Comms.firstEnemy + theirArchons; i++) {
            int currFlag = rc.readSharedArray(i);
            if (currFlag != Comms.DEAD_ARCHON_FLAG) {
                int currHealth = Comms.getHealthBucket(currFlag);
                if (currHealth < leastHealth) {
                    leastHealth = currHealth;
                    bestLoc = Comms.locationFromFlag(currFlag);
                    idxOfBestLoc = i - Comms.firstEnemy;
                }
            }
        }
        return new MapLocation[]{bestLoc, new MapLocation(idxOfBestLoc, 0)};
    }

    public void moveTowardsWeakestArchon() throws GameActionException {
        if (target != null) {
            Debug.printString("Targeting Archon at: " + target.toString());
        }
        if(shouldRunAway()) {
            // Positive so that we move towards the point mass.
            MapLocation dest = currLoc.translate(overallFriendlySoldierDx, overallFriendlySoldierDy);
            Direction dir = Nav.getBestDir(dest);
            Direction[] targetDirs = Util.getInOrderDirections(dir);
            tryMoveDest(targetDirs);
            return;
        }
        // First try to move to the Archon with least health
        int theirArchons = Comms.enemyArchonCount();
        if (theirArchons > 0) {
            MapLocation bestLoc = target;

            Direction[] bestDirs = {};
            if (bestLoc != null) {
                if(currLoc.distanceSquaredTo(bestLoc) > 2) {
                    Direction bestDir = Nav.getBestDir(bestLoc);
                    bestDirs = Util.getInOrderDirections(bestDir);
                    // Debug.setIndicatorLine(Debug.INDICATORS, rc.getLocation(), rc.getLocation().add(bestDir), 0, 0, 255);
                }
                Debug.setIndicatorDot(Debug.INDICATORS, bestLoc, 255, 0, 0);
            }
            else {
                bestDirs = Nav.explore();
            }
            tryMoveDest(bestDirs);
        }
        else {
            currState = SoldierState.EXPLORING;
        }
    }
    public void moveTowardsDistressedArchon() throws GameActionException {
            MapLocation bestLoc = distressLocation;
            Direction[] bestDirs = {};
            if (bestLoc != null) {
                if(currLoc.distanceSquaredTo(bestLoc) > 15) {
                    Direction bestDir = Nav.getBestDir(bestLoc);
                    bestDirs = Util.getInOrderDirections(bestDir);
                    // Debug.setIndicatorLine(Debug.INDICATORS, rc.getLocation(), rc.getLocation().add(bestDir), 0, 0, 255);
                    Debug.printString("Going to defend Archon at: " + bestLoc.toString());
                }
                else {
                    if (!tryMoveTowardsEnemy()) { 
                        Direction toHome = currLoc.directionTo(home);
                        Direction spinDir = Util.turnRight90(toHome);
                        bestDirs = Util.getInOrderDirections(Nav.getBestDir(currLoc.add(spinDir)));
                        Debug.printString("Spinning around Archon at: " + bestLoc.toString());
                    }
                }
    
                Debug.setIndicatorDot(Debug.INDICATORS, bestLoc, 255, 0, 0);
            }
            else {
                Debug.printString("NULL DISTRESS");
            }
            tryMoveDest(bestDirs);
    }

    public void soldierExplore() throws GameActionException {
        // MapLocation highestIdLoc = currLoc;
        // int highestId = rc.getID();
        // for (RobotInfo bot : FriendlySensable) {
        //     if (bot.getType() == RobotType.SOLDIER) {
        //         if (bot.getID() > highestId) {
        //             highestId = bot.getID();
        //             highestIdLoc = bot.getLocation();
        //         }
        //     }
        // }
        // if (highestId != rc.getID() && currLoc.distanceSquaredTo(home) > 1.5 * RobotType.ARCHON.visionRadiusSquared) {
        //     Direction[] targets = Util.getInOrderDirections(currLoc.directionTo(highestIdLoc));
        //     tryMoveDest(targets);
        // }
        // else {
        //     tryMoveDest(Nav.explore());
        // }
        if (avgEnemyLoc != null && currLoc.distanceSquaredTo(avgEnemyLoc) > visionRadiusSquared) {
            Direction bestDir = Nav.getBestDir(avgEnemyLoc);
            Direction[] targets = Util.getInOrderDirections(bestDir);
            tryMoveDest(targets);
        }
        else {
            tryMoveDest(Nav.explore());
        }
    }
}
