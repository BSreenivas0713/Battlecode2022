package MPTempName;

import battlecode.common.*;
import MPTempName.Debug.*;
import MPTempName.Util.*;
import MPTempName.Comms.*;

public class Robot {
    static RobotController rc; 
    static int turnCount;
    static RobotType robotType;

    static MapLocation home;
    static RobotInfo[] EnemySensable;
    static RobotInfo[] FriendlySensable;
    static MapLocation currLoc;

    static int actionRadiusSquared;
    static int visionRadiusSquared;

    static int homeFlagIdx;
    static int nextFlag;
    static int defaultFlag;
    // This is the order of priorities to attack enemies
    static RobotInfo maybeArchon = null;
    static RobotInfo maybeWatchtower = null;
    static RobotInfo maybeSage = null;
    static RobotInfo maybeSoldier = null;
    static RobotInfo maybeBuilder = null;
    static RobotInfo maybeMiner = null;
    static RobotInfo maybeLab = null;

    static Team team;
    static Team opponent;

    public Robot(RobotController r) {
        rc = r;
        turnCount = 0;
        robotType = rc.getType();
        actionRadiusSquared = robotType.actionRadiusSquared;
        visionRadiusSquared = robotType.visionRadiusSquared;
        team = rc.getTeam();
        opponent = team.opponent();

        if(robotType == RobotType.ARCHON) {
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
    public double getLeadDistTradeoffScore(int radiusSquared, int leadAmount) {
        if(radiusSquared == 0 && leadAmount > 1){return Integer.MAX_VALUE;}
        return (float)leadAmount - Math.sqrt((double) radiusSquared) * 5;
    }


    public void reportEnemies() throws GameActionException {
        for (RobotInfo bot : EnemySensable) {
            switch(bot.getType()) {
                case SOLDIER:
                case WATCHTOWER:
                case ARCHON:
                case SAGE:
                    Comms.updateAvgEnemyLoc(bot.getLocation());
                    break;
                case MINER:
                case BUILDER:
                case LABORATORY:
                    if(!Comms.foundEnemySoldier)
                        Comms.updateAvgEnemyLoc(bot.getLocation());
                    break;
            }
        }
    }

    public void initTurn() throws GameActionException {
        Nav.initTurn();
    }

    public void takeTurn() throws GameActionException {
        turnCount += 1;
        EnemySensable = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        FriendlySensable = rc.senseNearbyRobots(-1, rc.getTeam());
        
        // setting flag on next turn if archon
        if (rc.getType() == RobotType.ARCHON) {
            if(rc.readSharedArray(homeFlagIdx) != nextFlag) {
                rc.writeSharedArray(homeFlagIdx, nextFlag);
                nextFlag = defaultFlag;
            }
        }
        else {
            reportEnemies();
        }

        currLoc = rc.getLocation();
        Comms.broadcastEnemyFound(EnemySensable);
        // Debug.setIndicatorDot(Debug.info, home, 255, 255, 255);
        // if(rc.getRoundNum() > 150) {
        //     rc.resign();
        // }
    }

    public void endTurn() throws GameActionException {
        Explore.initialize();
        switch(robotType) {
            case MINER:
            case BUILDER:
            case SOLDIER:
            case SAGE:
            case WATCHTOWER:
                Explore.markSeen();
        }
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
    public void loadEnemies(RobotInfo[] enemies) {
        maybeArchon = null;
        maybeWatchtower = null;
        maybeSage = null;
        maybeSoldier = null;
        maybeBuilder = null;
        maybeMiner = null;
        maybeLab = null;

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
        return getBestEnemy(rc.senseNearbyRobots(actionRadiusSquared, rc.getTeam().opponent()));
    }

    public RobotInfo getBestEnemy(RobotInfo[] sensable) throws GameActionException {
        loadEnemies(sensable);

        RobotInfo res = null;

        // Prioritize these the least
        if(maybeLab != null) res = maybeLab;
        if(maybeSage != null) res = maybeSage;

        if(maybeMiner != null) res = maybeMiner;
        if(maybeBuilder != null) res = maybeBuilder;
        if(maybeArchon != null) res = maybeArchon;
        if(maybeWatchtower != null) res = maybeWatchtower;
        if(maybeSoldier != null) res = maybeSoldier;

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

    // Haven't tested this
    // I think the idea is that it will rotate at distance rad due to bugNav
    boolean moveSafely(MapLocation loc, int rad) throws GameActionException {
        if (loc == null) return false;
        int d = rc.getLocation().distanceSquaredTo(loc);
        d = Math.min(d, rad);
        boolean[] imp = new boolean[Util.directionsCenter.length];
        boolean greedy = false;
        for (int i = Util.directionsCenter.length; i-- > 0; ){
            MapLocation newLoc = rc.getLocation().add(Util.directionsCenter[i]);
            if (newLoc.distanceSquaredTo(loc) <= d) {
                imp[i] = true;
                greedy = true;
            }
        }
        Pathfinding.setImpassable(imp);
        Nav.move(loc, greedy);
        return true;
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

}
