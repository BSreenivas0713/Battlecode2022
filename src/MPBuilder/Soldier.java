package MPBuilder;

import battlecode.common.*;
import MPBuilder.Debug.*;
import MPBuilder.Util.*;
import MPBuilder.Comms.*;

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

    public Soldier(RobotController r) throws GameActionException {
        super(r);
        currState = SoldierState.EXPLORING;
        homeFlagIdx = Comms.firstArchonFlag;
    }

    public Soldier(RobotController r, int homeFlagIndex) throws GameActionException {
        super(r);
        currState = SoldierState.EXPLORING;
        homeFlagIdx = homeFlagIndex;
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
                break;
            case HELPING: 
                Debug.setIndicatorString("in Helping Mode");
                tryAttackBestEnemy();
                moveTowardsDistressedArchon();
            default:
                break;
        }
        // try to move if not moved in above functions
        tryMoveDest(Nav.explore());
    }

    public void trySwitchState() throws GameActionException {
        // if >= 15 latticing soldiers, switch to rushing
        int maxHelpers = Comms.readMaxHelper();
        for(int x = Comms.firstArchonFlag; x < Comms.firstArchonFlag + 4; x++) {
            int flag = rc.readSharedArray(x);
            if(Comms.getICFromFlag(flag) == Comms.InformationCategory.UNDER_ATTACK) {
                int locationIndex = x - Comms.mapLocToFlag;
                if (Comms.getHelpersForArchon(locationIndex) < maxHelpers) {
                    currState = SoldierState.HELPING;
                    Comms.incrementHelpersForArchon(locationIndex);
                    distressLocation = Comms.locationFromFlag(rc.readSharedArray(locationIndex));
                    return;
                }
            }
        }
        if (currState != SoldierState.RUSHING && 
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


    public boolean tryMoveTowardsEnemy() throws GameActionException {
        RobotInfo closestEnemy = getClosestEnemy();
        // move towards it if found
        if (closestEnemy != null) {
            Direction[] targets = Util.getInOrderDirections(currLoc.directionTo(closestEnemy.getLocation()));
            tryMoveDest(targets);
            return true;
        }
        return false;
    }

    public void latticeAroundHome() throws GameActionException {
        int overallDx = 0;
        int overallDy = 0;

        // get averaged overall dx and overall dy to friendly sensables
        int soldiersFound = 0;
        for (RobotInfo friend : FriendlySensable) {
            if (friend.getType() == RobotType.SOLDIER) {
                soldiersFound++;
                MapLocation loc = friend.getLocation();
                overallDx += currLoc.directionTo(loc).dx * (10000 / (currLoc.distanceSquaredTo(loc)));
                overallDy += currLoc.directionTo(loc).dy * (10000 / (currLoc.distanceSquaredTo(loc)));
            }
        }
        // move away from this direction
        Direction awayDir = null;
        if (soldiersFound >= 3) {
            awayDir = currLoc.directionTo(currLoc.translate(overallDx, overallDy)).opposite();
        }
        else {
            //move away from home if no soldier found within sensing radius
            awayDir = currLoc.directionTo(home);
        }
        Direction[] targetDirs = Util.getInOrderDirections(awayDir);
        tryMoveDest(targetDirs);
    }

    public void latticeAroundHomeAfterRushing() throws GameActionException {
        int overallDx = 0;
        int overallDy = 0;

        // get averaged overall dx and overall dy to friendly sensables
        int soldiersFound = 0;
        for (RobotInfo friend : FriendlySensable) {
            if (friend.getType() == RobotType.SOLDIER) {
                soldiersFound++;
                MapLocation loc = friend.getLocation();
                overallDx += currLoc.directionTo(loc).dx * (10000 / (currLoc.distanceSquaredTo(loc)));
                overallDy += currLoc.directionTo(loc).dy * (10000 / (currLoc.distanceSquaredTo(loc)));
            }
        }
        // move away from this direction
        Direction awayDir = null;
        if (soldiersFound >= 3 && currLoc.distanceSquaredTo(home) < 4 * RobotType.ARCHON.visionRadiusSquared) {
            awayDir = currLoc.directionTo(currLoc.translate(overallDx, overallDy)).opposite();
        }
        else {
            //move away from home if no soldier found within sensing radius
            awayDir = currLoc.directionTo(home);
        }
        Direction[] targetDirs = Util.getInOrderDirections(awayDir);
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
