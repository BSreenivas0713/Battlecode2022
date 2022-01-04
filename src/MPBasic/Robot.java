package MPBasic;

import battlecode.common.*;
import MPBasic.Debug.*;
import MPBasic.Util.*;
import MPBasic.Comms.*;

public class Robot {
    static RobotController rc; 
    static int turnCount;
    static MapLocation home;
    static RobotInfo[] EnemySensable;
    static RobotInfo[] FriendlySensable;

    public Robot(RobotController r) {
        rc = r;
        turnCount = 0;
        
        if(rc.getType() == RobotType.ARCHON) {
            home = rc.getLocation();
        } else {
            RobotInfo[] sensableWithin2 = rc.senseNearbyRobots(2, rc.getTeam());
            for (RobotInfo robot : sensableWithin2) {
                if (robot.getType() == RobotType.ARCHON) {
                    MapLocation robotLoc = robot.getLocation();
                    home = robotLoc;
                }
            }
        }

        if (home == null) {
            home = rc.getLocation();
        }
    }

    public void takeTurn() throws GameActionException {
        AnomalyScheduleEntry[] AnomolySchedule = rc.getAnomalySchedule();
        turnCount += 1;
        EnemySensable = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
        FriendlySensable = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
        // initializeGlobals();
        // turnCount += 1;
        // Debug.setIndicatorDot(Debug.info, home, 255, 255, 255);
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        //Debug.println(Debug.info, "I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    /**
     * TODO: what do we do if an archon moves? right now, our idea is if we've detected more than
     * ArchonCount unique enemies, then update the new archon location by replacing the nearest
     * old one. However, it could be the case that the 3rd unique location we see is actually just
     * the first one moved. Consider this.
     *  */ 
    static void reportEnemyArchon(MapLocation enemyLoc, int health) throws GameActionException {
        int healthBucket = health / Comms.HEALTH_BUCKET_SIZE;
        int encodedEnemyLoc = Comms.encodeLocation(enemyLoc, healthBucket);
        int theirArchons = Comms.enemyArchonCount();
        if (theirArchons < rc.getArchonCount()) {
            boolean shouldInsert = true;
            //only insert archon if not already in list
            for (int i = Comms.firstEnemy; i < Comms.firstEnemy + theirArchons; i++) {
                int testFlag = rc.readSharedArray(i);
                if (enemyLoc.equals(Comms.locationFromFlag(testFlag))) {
                    shouldInsert = false;
                    break;
                }
            }
            if (shouldInsert) {
                rc.writeSharedArray(Comms.firstEnemy + theirArchons, encodedEnemyLoc);
                Comms.incrementEnemy();
            }
        }
    }
}
