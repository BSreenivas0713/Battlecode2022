package MPCoordinated;

import battlecode.common.*;
import MPCoordinated.Debug.*;
import MPCoordinated.Util.*;
import MPCoordinated.Comms.*;

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
    static int overallFriendlySoldierDx;
    static int overallFriendlySoldierDy;

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
    } 

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Comms.incrementRushSoldierCounter();
        trySwitchState();
        switch (currState) {
            case DEFENSE:
                Debug.setIndicatorString("in defense mode");
                if (!tryAttackBestEnemy()) {
                    if (!tryMoveTowardsEnemy()) {
                        latticeAroundHome();
                    }
                }
                break;
            case RUSHING:
                Debug.setIndicatorString("in rushing mode");
                tryAttackBestEnemy();
                moveTowardsWeakestArchon();
                break;
            case DONE_RUSHING:
                Debug.setIndicatorString("in done rushing mode");
                if (!tryAttackBestEnemy()) {
                    if (!tryMoveTowardsEnemy()) {
                        latticeAroundHomeAfterRushing();
                    }
                }
                break;
            case EXPLORING:
                Debug.setIndicatorString("in exploring mode");
                tryAttackBestEnemy();
                if (!tryMoveTowardsEnemy()) { 
                    tryMoveDest(Nav.explore());
                }
                break;
            case HELPING: 
                Debug.setIndicatorString("in Helping Mode");
                tryAttackBestEnemy();
                moveTowardsDistressedArchon();
                break;
            default:
                break;
        }
        // try to move if not moved in above functions
    }

    public void trySwitchState() throws GameActionException {
        // if >= 15 latticing soldiers, switch to rushing
        int maxHelpers = Comms.readMaxHelper();
        int bestDistance = Util.MAP_MAX_DIST_SQUARED / 4;
        distressLocation = null;
        for(int x = Comms.firstArchonFlag; x < Comms.firstArchonFlag + 4; x++) {
            int flag = rc.readSharedArray(x);
            if(Comms.getICFromFlag(flag) == Comms.InformationCategory.UNDER_ATTACK) {
                int locationIndex = x - Comms.mapLocToFlag;
                if (Comms.getHelpersForArchon(locationIndex) < maxHelpers) {
                    currState = SoldierState.HELPING;
                    Comms.incrementHelpersForArchon(locationIndex);
                    MapLocation archonInTrouble = Comms.locationFromFlag(rc.readSharedArray(locationIndex));
                    int distance = rc.getLocation().distanceSquaredTo(archonInTrouble);
                    if (distance < bestDistance) {
                        distressLocation = archonInTrouble;
                    }
                }
            }
        }
        if (distressLocation != null)  {
            return;
        }
        else if (currState != SoldierState.RUSHING && 
            Comms.getSoldierCatFromFlag(rc.readSharedArray(Comms.SOLDIER_STATE_IDX)) == Comms.SoldierStateCategory.RUSH_SOLDIERS) {
            currState = SoldierState.RUSHING;
            MapLocation[] targetAndId = findWeakestArchon(Comms.enemyArchonCount());
            target = targetAndId[0];
            targetId = targetAndId[1].x;
        }
        else if (currState == SoldierState.RUSHING && Comms.isArchonDead(targetId)) {
            target = null;
            targetId = 0;
            currState = SoldierState.DONE_RUSHING;
        }
        else if(currState != SoldierState.RUSHING){
            currState = SoldierState.EXPLORING;
        }
    }

    public RobotInfo getClosestEnemy() {
        RobotInfo robot;
        RobotInfo closestRobot = null;
        int leastDistance = Integer.MAX_VALUE;
        int currDistance;
        MapLocation loc;

        for (int i = EnemySensable.length - 1; i >= 0; i--) {
            robot = EnemySensable[i];
            loc = robot.location;
            currDistance = robot.getLocation().distanceSquaredTo(currLoc);
            if(leastDistance > currDistance) {
                leastDistance = currDistance;
                closestRobot = robot;
            }

            if(robot.type == RobotType.SOLDIER) {
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
                overallFriendlySoldierDx += currLoc.directionTo(loc).dx * (10000 / (currLoc.distanceSquaredTo(loc)));
                overallFriendlySoldierDy += currLoc.directionTo(loc).dy * (10000 / (currLoc.distanceSquaredTo(loc)));
            }
        }

        // Weight home Archon ever so slightly.
        // Only really has an effect when no soldiers are seen
        // Primarily for running towards other soldiers/home when running away from enemies.
        overallFriendlySoldierDx += currLoc.directionTo(home).dx * (1000 / (currLoc.distanceSquaredTo(home)));
        overallFriendlySoldierDy += currLoc.directionTo(home).dy * (1000 / (currLoc.distanceSquaredTo(home)));
    }

    public boolean shouldRunAway() {
        return numEnemySoldiers > numFriendlySoldiers;
    }

    public boolean tryMoveTowardsEnemy() throws GameActionException {
        RobotInfo closestEnemy = getClosestEnemy();
        // move towards it if found
        if (closestEnemy != null) {
            findFriendlySoldiers();
            MapLocation dest;
            if(shouldRunAway()) {
                // Positive so that we move towards the point mass.
                dest = currLoc.translate(overallFriendlySoldierDx, overallFriendlySoldierDy);
            } else {
                dest = closestEnemy.getLocation();
            }

            Direction dir = Nav.getBestDir(dest);
            Direction[] targetDirs = Util.getInOrderDirections(dir);
            tryMoveDest(targetDirs);
            return true;
        }
        return false;
    }

    public void latticeAroundHome() throws GameActionException {
        findFriendlySoldiers();

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
        findFriendlySoldiers();

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
        // First try to move to the Archon with least health
        int theirArchons = Comms.enemyArchonCount();
        if (theirArchons > 0) {
            MapLocation bestLoc = target;

            Direction[] bestDirs = {};
            if (bestLoc != null) {
                if(currLoc.distanceSquaredTo(bestLoc) > 2) {
                    Direction bestDir = Nav.getBestDir(bestLoc);
                    bestDirs = Util.getInOrderDirections(bestDir);
                    Debug.setIndicatorLine(Debug.INDICATORS, rc.getLocation(), rc.getLocation().add(bestDir), 0, 0, 255);
                }
    
                Debug.setIndicatorString("Targeting Archon at: " + bestLoc.toString());
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
                    Debug.setIndicatorLine(Debug.INDICATORS, rc.getLocation(), rc.getLocation().add(bestDir), 0, 0, 255);
                }
    
                Debug.setIndicatorString("Defending Archon at: " + bestLoc.toString());
                Debug.setIndicatorDot(Debug.INDICATORS, bestLoc, 255, 0, 0);
            }
            else {
                Debug.setIndicatorString("Distress location null: ERROR");
            }
            tryMoveDest(bestDirs);
    }
}
