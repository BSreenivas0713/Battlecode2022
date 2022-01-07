package MPFrankenstein;

import battlecode.common.*;
import MPFrankenstein.Debug.*;
import MPFrankenstein.Util.*;
import MPFrankenstein.Comms.*;

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
    static int nextSoldierFlag;
    // This is the order of priorities to attack enemies
    static RobotInfo maybeArchon = null;
    static RobotInfo maybeWatchtower = null;
    static RobotInfo maybeSage = null;
    static RobotInfo maybeSoldier = null;
    static RobotInfo maybeBuilder = null;
    static RobotInfo maybeMiner = null;
    static RobotInfo maybeLab = null;

    public Robot(RobotController r) {
        rc = r;
        turnCount = 0;
        actionRadiusSquared = rc.getType().actionRadiusSquared;
        visionRadiusSquared = rc.getType().visionRadiusSquared;

        
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
        turnCount += 1;
        EnemySensable = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
        FriendlySensable = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
        
        // setting flag on next turn if archon
        if (rc.getType() == RobotType.ARCHON) {
            if (rc.readSharedArray(Comms.SOLDIER_STATE_IDX) != nextSoldierFlag) {
                rc.writeSharedArray(Comms.SOLDIER_STATE_IDX, nextSoldierFlag);
            }
        }
        else {
            for (RobotInfo bot : EnemySensable) {
                if (bot.getType() == RobotType.SOLDIER) {
                    Comms.updateAvgEnemyLoc(bot.getLocation());
                }
            }
        }

        nextSoldierFlag = SoldierStateCategory.EMPTY.ordinal();

        currLoc = rc.getLocation();
        reportKilledArchons();
        tryToReportArchon();
        Comms.broadcastEnemyFound();
        // initializeGlobals();
        // turnCount += 1;
        // Debug.setIndicatorDot(Debug.info, home, 255, 255, 255);
        Debug.flush();
    }

    public RobotInfo getClosestEnemy() {
        return getClosestRobot(EnemySensable);
    }

    public RobotInfo getClosestEnemy(RobotType robotType) {
        return getClosestRobot(EnemySensable, robotType);
    }

    /*
     * Prioritizes attacking enemies in the given order.
     * Prioritizes attacking lower health enemies.
     */
    public void loadEnemies() {
        maybeArchon = null;
        maybeWatchtower = null;
        maybeSage = null;
        maybeSoldier = null;
        maybeBuilder = null;
        maybeMiner = null;
        maybeLab = null;

        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(actionRadiusSquared, opponent);
        RobotInfo enemy;
        for (int i = enemies.length - 1; i >= 0; i--) {
            enemy = enemies[i];
            switch(enemy.type) {
                case ARCHON:
                    if(maybeArchon == null || maybeArchon.health > enemy.health) {
                        maybeArchon = enemy;
                    }
                    break;
                case WATCHTOWER:
                    if(maybeWatchtower == null || maybeWatchtower.health > enemy.health) {
                        maybeWatchtower = enemy;
                    }
                    break;
                case SAGE:
                    if(maybeSage == null || maybeSage.health > enemy.health) {
                        maybeSage = enemy;
                    }
                    break;
                case SOLDIER:
                    if(maybeSoldier == null || maybeSoldier.health > enemy.health) {
                        maybeSoldier = enemy;
                    }
                    break;
                case BUILDER:
                    if(maybeBuilder == null || maybeBuilder.health > enemy.health) {
                        maybeBuilder = enemy;
                    }
                    break;
                case MINER:
                    if(maybeMiner == null || maybeMiner.health > enemy.health) {
                        maybeMiner = enemy;
                    }
                    break;
                case LABORATORY:
                    if(maybeLab == null || maybeLab.health > enemy.health) {
                        maybeLab = enemy;
                    }
                    break;
            }
        }
    }

    public MapLocation getClosestLoc(MapLocation[] locs) {
        MapLocation loc;
        MapLocation closest = null;
        int leastDistance = Integer.MAX_VALUE;
        int currDistance;

        for (int i = locs.length - 1; i >= 0; i--) {
            loc = locs[i];
            currDistance = loc.distanceSquaredTo(currLoc);
            if(leastDistance > currDistance) {
                leastDistance = currDistance;
                closest = loc;
            }
        }

        return closest;
    }

    public RobotInfo getClosestRobot(RobotInfo[] robots, RobotType robotType) {
        RobotInfo robot;
        RobotInfo closestRobot = null;
        int leastDistance = Integer.MAX_VALUE;
        int currDistance;

        for (int i = robots.length - 1; i >= 0; i--) {
            robot = robots[i];
            currDistance = robot.getLocation().distanceSquaredTo(currLoc);
            if(leastDistance > currDistance && robot.type == robotType) {
                leastDistance = currDistance;
                closestRobot = robot;
            }
        }

        return closestRobot;
    }

    public RobotInfo getClosestRobot(RobotInfo[] robots) {
        RobotInfo robot;
        RobotInfo closestRobot = null;
        int leastDistance = Integer.MAX_VALUE;
        int currDistance;

        for (int i = robots.length - 1; i >= 0; i--) {
            robot = robots[i];
            currDistance = robot.getLocation().distanceSquaredTo(currLoc);
            if(leastDistance > currDistance) {
                leastDistance = currDistance;
                closestRobot = robot;
            }
        }

        return closestRobot;
    }

    public MapLocation getClosestArchon() throws GameActionException {
        MapLocation[] enemyArchons = Comms.getEnemyArchonLocations();
        return getClosestLoc(enemyArchons);
    }

    /*
     * Prioritizes miners in general.
     * Unless you're close enough to an Archon, then prioritize soldiers.
     */
    public RobotInfo getBestEnemy() throws GameActionException {
        loadEnemies();

        RobotInfo res = null;

        // Prioritize these the least
        if(maybeLab != null) res = maybeLab;
        if(maybeArchon != null) res = maybeArchon;
        if(maybeSage != null) res = maybeSage;

        if(maybeSoldier != null) res = maybeSoldier;
        if(maybeWatchtower != null) res = maybeWatchtower;
        if(maybeMiner != null) res = maybeMiner;
        if(maybeBuilder != null) res = maybeBuilder;

        // A soldier is prioritized if either you or the enemy is
        // close to home or an enemy archon
        if(maybeSoldier != null) {
            MapLocation closestEnemyArchon = getClosestArchon();
            boolean nearHome = currLoc.distanceSquaredTo(home) < Util.SOLDIER_PRIORITY_ATTACK_DIST;
            boolean enemyNearHome = maybeSoldier.location.distanceSquaredTo(home) < Util.SOLDIER_PRIORITY_ATTACK_DIST;
            boolean nearArchon = closestEnemyArchon != null && currLoc.distanceSquaredTo(closestEnemyArchon) < Util.SOLDIER_PRIORITY_ATTACK_DIST;
            boolean enemyNearArchon = closestEnemyArchon != null && maybeSoldier.location.distanceSquaredTo(closestEnemyArchon) < Util.SOLDIER_PRIORITY_ATTACK_DIST;
            if(nearHome || enemyNearHome || nearArchon || enemyNearArchon) {
                res = maybeSoldier;
            }
        }
        if(maybeArchon != null) {
            if (currLoc.distanceSquaredTo(maybeArchon.getLocation()) < 4) {
                res = maybeArchon;
            }
        }

        return res;
    }

    public boolean tryAttackBestEnemy() throws GameActionException {
        // Try to attack someone
        RobotInfo bestEnemy = getBestEnemy();
        if(bestEnemy != null) {
            if(rc.canAttack(bestEnemy.getLocation())) {
                rc.attack(bestEnemy.getLocation());
                Debug.printString("Attacking: " + bestEnemy.getLocation().toString());
                return true;
            } else {
                Debug.printString("Enemy: " + bestEnemy.getLocation().toString());
            }
        }
        return false;
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
        MapLocation[] friendlyArchonLocs = Comms.getFriendlyArchonLocations();
        for (int i = 0; i < Comms.friendlyArchonCount(); i++) {
            MapLocation loc = friendlyArchonLocs[i];
            if (rc.canSenseLocation(loc)) {
                RobotInfo botAtLoc = rc.senseRobotAtLocation(loc);
                if (botAtLoc == null || (botAtLoc.type != RobotType.ARCHON)) {
                    int newFlag = Comms.encodeArchonFlag(Comms.InformationCategory.EMPTY);
                    Comms.writeIfChanged(i + Comms.mapLocToFlag + Comms.firstArchon, newFlag);
                }
            }
        }
    }
}
