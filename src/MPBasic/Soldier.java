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

        Direction dir;
        // First try to move to the Archon with least health
        int theirArchons = Comms.enemyArchonCount();
        if (theirArchons > 0) {
            int leastHealth = 8;
            MapLocation bestLoc = null;
            for (int i = Comms.firstEnemy; i < Comms.firstEnemy + theirArchons; i++) {
                int currFlag = rc.readSharedArray(i);
                int currHealth = Comms.getHealthBucket(currFlag);
                if (currHealth < leastHealth) {
                    leastHealth = currHealth;
                    bestLoc = Comms.locationFromFlag(currFlag);
                }
            }
            dir = rc.getLocation().directionTo(bestLoc);
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        // Then try to move randomly.
        } else {
            tryMoveDest(Nav.exploreGreedy(rc));
        }
    }
}
