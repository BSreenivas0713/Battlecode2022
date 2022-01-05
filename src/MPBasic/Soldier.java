package MPBasic;

import battlecode.common.*;
import MPBasic.Debug.*;
import MPBasic.Util.*;
import MPBasic.Comms.*;

public class Soldier extends Robot {
    static enum SoldierState {
        DEFENSE,
        RUSHING,
    }
    static SoldierState currState;
    static int homeFlagIdx;

    public Soldier(RobotController r) throws GameActionException {
        super(r);
        currState = SoldierState.DEFENSE;
        homeFlagIdx = Comms.firstArchonFlag;
    }

    public Soldier(RobotController r, int homeFlagIndex) throws GameActionException {
        super(r);
        currState = SoldierState.DEFENSE;
        homeFlagIdx = homeFlagIndex;
    } 

    public RobotInfo getBestEnemy(){
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(actionRadiusSquared, opponent);
        if (enemies.length == 0) {
            return null;
        }
        for(RobotInfo enemy: enemies) {
            if(enemy.type == RobotType.ARCHON){return enemy;}
            if(enemy.type == RobotType.WATCHTOWER){return enemy;}
            if(enemy.type == RobotType.SAGE){return enemy;}
        }
        if (enemies.length != 0) {return enemies[0];} else {return null;}

    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
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
                if (!tryAttackBestEnemy()) {
                    moveTowardsWeakestArchon();
                }
                break;
            default:
                break;
        }
        // try to move if not moved in above functions
        tryMoveDest(Nav.exploreGreedy());
    }

    public void trySwitchState() throws GameActionException {
        // if >= 15 latticing soldiers, switch to rushing
        if (currState != SoldierState.RUSHING && 
            Comms.getICFromFlag(rc.readSharedArray(homeFlagIdx)) == Comms.InformationCategory.RUSH_SOLDIERS) {
            currState = SoldierState.RUSHING;
        }
    }

    public boolean tryAttackBestEnemy() throws GameActionException {
        // Try to attack someone
        RobotInfo bestEnemy = getBestEnemy();
        if (bestEnemy != null && rc.canAttack(bestEnemy.getLocation())) {
            rc.attack(bestEnemy.getLocation());
            return true;
        }
        return false;
    }

    public boolean tryMoveTowardsEnemy() throws GameActionException {
        int minEnemyDistSquared = Integer.MAX_VALUE;
        RobotInfo closestEnemy = null;
        // find closest enemy
        for (RobotInfo enemy : EnemySensable) {
            int candidateDist = currLoc.distanceSquaredTo(enemy.getLocation());
            if (candidateDist < minEnemyDistSquared) {
                minEnemyDistSquared = candidateDist;
                closestEnemy = enemy;
            }
        }
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
        if (soldiersFound >= 3 && currLoc.distanceSquaredTo(home) < RobotType.ARCHON.visionRadiusSquared) {
            awayDir = currLoc.directionTo(currLoc.translate(overallDx, overallDy)).opposite();
        }
        else {
            //move away from home if no soldier found within sensing radius
            awayDir = currLoc.directionTo(home);
        }
        Direction[] targetDirs = Util.getInOrderDirections(awayDir);
        tryMoveDest(targetDirs);
    }

    public void moveTowardsWeakestArchon() throws GameActionException {
        // First try to move to the Archon with least health
        int theirArchons = Comms.enemyArchonCount();
        if (theirArchons > 0) {
            int leastHealth = Comms.NUM_HEALTH_BUCKETS;
            MapLocation bestLoc = null;
            for (int i = Comms.firstEnemy; i < Comms.firstEnemy + theirArchons; i++) {
                int currFlag = rc.readSharedArray(i);
                if (currFlag != Comms.DEAD_ARCHON_FLAG) {
                    int currHealth = Comms.getHealthBucket(currFlag);
                    if (currHealth < leastHealth) {
                        leastHealth = currHealth;
                        bestLoc = Comms.locationFromFlag(currFlag);
                    }
                }
            }

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
    }
}
