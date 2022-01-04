package MPBasic;

import battlecode.common.*;
import MPBasic.Debug.*;
import MPBasic.Util.*;
import MPBasic.Comms.*;

public class Soldier extends Robot{
    public Soldier(RobotController r) throws GameActionException {
        super(r);
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
        // Try to attack someone
        RobotInfo bestEnemy = getBestEnemy();
        if (bestEnemy != null && rc.canAttack(bestEnemy.location)) {
            rc.attack(bestEnemy.location);
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
                        // Nav.setDest(bestLoc);
                    }
                }
            }

            Direction[] bestDirs = null;
            if (bestLoc != null) {
                // Direction bestDir = Nav.gradientDescent();
                Direction bestDir = Nav.getGreedyDirection(rc.getLocation().directionTo(bestLoc));
                bestDirs = Util.getInOrderDirections(bestDir);
    
                Debug.setIndicatorString("Targeting Archon at: " + bestLoc.toString());
                Debug.setIndicatorDot(Debug.INDICATORS, bestLoc, 255, 0, 0);
                Debug.setIndicatorLine(Debug.INDICATORS, rc.getLocation(), rc.getLocation().add(bestDir), 0, 0, 255);
            }
            else {
                bestDirs = Nav.exploreGreedy();
            }
            tryMoveDest(bestDirs);

        // Then try to move randomly.
        } else {
            Debug.setIndicatorString("Exploring");
            tryMoveDest(Nav.exploreGreedy());
        }
    }
}
