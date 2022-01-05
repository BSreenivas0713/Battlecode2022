package MPBasic;

import battlecode.common.*;
import MPBasic.Debug.*;
import MPBasic.Util.*;
import MPBasic.Comms.*;

public class DefenseSoldier extends Robot {
    static int homeFlagIdx;

    public DefenseSoldier(RobotController r) throws GameActionException {
        super(r);
        homeFlagIdx = Comms.firstArchonFlag;
    }

    public DefenseSoldier(RobotController r, int homeFlagIndex) throws GameActionException {
        super(r);
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
        Debug.setIndicatorString("defense soldier");
        if (!tryAttackBestEnemy()) {
            if (!tryMoveTowardsEnemy()) {
                latticeAroundHome();
            }
        }
        // try to move if not moved in above functions
        tryMoveDest(Nav.exploreGreedy());
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
        if (soldiersFound >= 3 && currLoc.distanceSquaredTo(home) < 1.5*RobotType.ARCHON.visionRadiusSquared) {
            awayDir = currLoc.directionTo(currLoc.translate(overallDx, overallDy)).opposite();
        }
        else {
            //move away from home if no soldier found within sensing radius
            awayDir = currLoc.directionTo(home);
        }
        Direction[] targetDirs = Util.getInOrderDirections(awayDir);
        tryMoveDest(targetDirs);
    }
}
