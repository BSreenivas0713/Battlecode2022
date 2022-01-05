package MPBuilder;

import battlecode.common.*;
import MPBuilder.Debug.*;
import MPBuilder.Util.*;
import MPBuilder.Comms.*;

public class Robot {
    static RobotController rc; 
    static int turnCount;
    static MapLocation home;
    static RobotInfo[] EnemySensable;
    static RobotInfo[] FriendlySensable;
    static MapLocation currLoc;
    static int actionRadiusSquared;
    static int visionRadiusSquared;
    static int homeFlagIdx;
    static int nextFlag;
    static int defaultFlag;

    public Robot(RobotController r) {
        rc = r;
        turnCount = 0;
        actionRadiusSquared = rc.getType().actionRadiusSquared;
        visionRadiusSquared = rc.getType().visionRadiusSquared;
        defaultFlag = 0;

        
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
        
        // setting flag on next turn if archon
        if (rc.getType() == RobotType.ARCHON && rc.readSharedArray(homeFlagIdx) != nextFlag) {
            rc.writeSharedArray(homeFlagIdx, nextFlag);
            nextFlag = defaultFlag;
        }

        EnemySensable = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
        FriendlySensable = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
        currLoc = rc.getLocation();
        reportKilledArchons();
        tryToReportArchon();
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
    static boolean tryMoveDest(Direction[] target_dir) throws GameActionException {
        // Debug.println(Debug.info, "Dest direction: " + dir);
        for(Direction dir : target_dir) {
            if(rc.canMove(dir)) {
                rc.move(dir);  
                return true;
            }
        }
        return false;
    }

    static void tryToReportArchon() throws GameActionException {
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(visionRadiusSquared, opponent);
        for (RobotInfo robot : enemies) {
            MapLocation robotLoc = robot.getLocation();
            //report enemy archon if not found yet
            if (robot.getType() == RobotType.ARCHON) {
                reportEnemyArchon(robotLoc, robot.ID, robot.health);
            }
        }
    }

    static void reportEnemyArchon(MapLocation enemyLoc, int enemyID, int health) throws GameActionException {
        int healthBucket = health / Comms.HEALTH_BUCKET_SIZE;
        int encodedEnemyLoc = Comms.encodeLocation(enemyLoc, healthBucket);
        int theirArchons = Comms.enemyArchonCount();
        boolean shouldInsert = true;
        int[] ids = Comms.getIDs();
        for (int i = 0; i < theirArchons; i++) {
            if (enemyID == ids[i]) {
                shouldInsert = false;
                int testFlag = rc.readSharedArray(Comms.firstEnemy + i);
                if (testFlag != encodedEnemyLoc) {
                    rc.writeSharedArray(Comms.firstEnemy + i, encodedEnemyLoc);
                }
                break;
            }
        }
        if (shouldInsert) {
            int newArchon = Comms.incrementEnemy(enemyID);
            rc.writeSharedArray(newArchon, encodedEnemyLoc);
        }
    }

    static void reportKilledArchons() throws GameActionException {
        MapLocation[] enemyArchonLocs = Comms.getEnemyArchonLocations();
        for (int i = 0; i < Comms.enemyArchonCount(); i++) {
            MapLocation loc = enemyArchonLocs[i];
            if (rc.canSenseLocation(loc)) {

                RobotInfo botAtLoc = rc.senseRobotAtLocation(loc);
                if (botAtLoc == null || (botAtLoc.type != RobotType.ARCHON)) {
                    rc.writeSharedArray(Comms.firstEnemy + i, Comms.DEAD_ARCHON_FLAG);
                }
            }
        }
    }
}
