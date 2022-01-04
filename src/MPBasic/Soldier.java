package MPBasic;

import battlecode.common.*;
import MPBasic.Debug.*;
import MPBasic.Util.*;
import MPBasic.Comms.*;

public class Soldier extends Robot{
    public Soldier(RobotController r) throws GameActionException {
        super(r);
    }
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        // Try to attack someone
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(actionRadiusSquared, opponent);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
            }
        }

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
            Direction[] bestDirs = null;
            if (bestLoc != null) {
                bestDirs = Util.getInOrderDirections(rc.getLocation().directionTo(bestLoc));
            }
            else {
                bestDirs = Nav.exploreGreedy(rc);
            }
            tryMoveDest(bestDirs);
        // Then try to move randomly.
        } else {
            tryMoveDest(Nav.exploreGreedy(rc));
        }
    }
}
