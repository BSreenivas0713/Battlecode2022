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
    static MapLocation[] archonLocations;
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

    public Robot(RobotController r) throws GameActionException {
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
            loadArchonLocations();
        }

        if (home == null) {
            home = rc.getLocation();
        }
    }

    public void loadArchonLocations() throws GameActionException {
        archonLocations = Comms.getFriendlyArchonLocations();
    }
    public double getLeadDistTradeoffScore(MapLocation loc, int leadAmount, int minerCap) {
        int radiusSquared = currLoc.distanceSquaredTo(loc);
        int minersMining = 0;
        double currDist = Math.sqrt((double) radiusSquared);
        double AmountMinedBeforeWeGetThere = 0;
        int currMinerCount = 0;
        for (RobotInfo friendlyRobot: FriendlySensable) {
            if(currMinerCount > minerCap) {
                break;
            }
            if(friendlyRobot.type == RobotType.MINER) {
                currMinerCount++;
                MapLocation robotLoc = friendlyRobot.getLocation();
                double distToSource = Math.sqrt((double)robotLoc.distanceSquaredTo(loc));
                if(distToSource < currDist) {
                    AmountMinedBeforeWeGetThere += (currDist - distToSource) * 5;
                }
            }
        }
        if(radiusSquared == 0 && leadAmount > 1){return Integer.MAX_VALUE;}
        return (float)leadAmount - currDist * 5  - AmountMinedBeforeWeGetThere;
    }

    public double getLeadDistTradeoffScore(int radiusSquared, int leadAmount) {
        if(radiusSquared == 0 && leadAmount > 1){return Integer.MAX_VALUE;}
        return (float)leadAmount - Math.sqrt((double) radiusSquared) * 5;
    }

    public boolean isSmallMap() {
        return Util.MAP_AREA <= Util.MAX_AREA_FOR_FAST_INIT;
    }
    
    public void reportEnemies() throws GameActionException {
        int count = 0;
        int totalX = 0;
        int totalY = 0;
        for (RobotInfo bot : EnemySensable) {
            if (count == 20) {
                break;
            }
            switch(bot.getType()) {
                case SOLDIER:
                case WATCHTOWER:
                case ARCHON:
                case SAGE:
                case LABORATORY:
                    totalX += bot.location.x;
                    totalY += bot.location.y;
                    count++;
                    break;
                case MINER:
                case BUILDER:
                    if(!Comms.foundEnemySoldier) {
                        totalX += bot.location.x;
                        totalY += bot.location.y;
                        count++;
                    }
                    break;
            }
        }
        if (count != 0) {
            Comms.updateAvgEnemyLoc(totalX, totalY, count);
        }
        
    }

    public void initTurn() throws GameActionException {
        // bytecodeCheck();
        Nav.initTurn();
    }

    public void bytecodeCheck() throws GameActionException {
        if(turnCount >= 2) {
            int bcLeft = Clock.getBytecodesLeft();
            if(bcLeft <= robotType.bytecodeLimit - 250) {
                System.out.println("Ran out of Bytecode last turn");
            }
        }
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

    public RobotInfo[] getEnemyAttackable() throws GameActionException {
        int size = 0;
        RobotInfo[] maxEnemyAttackable = new RobotInfo[EnemySensable.length];
        RobotInfo enemyRobot;
        for (int i = EnemySensable.length; --i >= 0;) {
            enemyRobot = EnemySensable[i];
            switch(enemyRobot.getType()) {
                case SOLDIER:
                case WATCHTOWER:
                case SAGE:
                    maxEnemyAttackable[size++] = enemyRobot;
                    break;
                default: 
                    break;
            }
        }
        RobotInfo[] enemyAttackable = new RobotInfo[size];
        System.arraycopy(maxEnemyAttackable, 0, enemyAttackable, 0, size);
        return enemyAttackable;
    }

    public RobotInfo getBestEnemy(RobotInfo[] sensable) throws GameActionException {
        loadEnemies(sensable);

        RobotInfo res = null;

        // Prioritize these the least
        if(maybeLab != null) res = maybeLab;

        if(maybeMiner != null) res = maybeMiner;
        if(maybeBuilder != null) res = maybeBuilder;
        if(maybeArchon != null) res = maybeArchon;
        if(maybeWatchtower != null) res = maybeWatchtower;
        if(maybeSage != null) res = maybeSage;
        if(maybeSoldier != null) res = maybeSoldier;

        return res;
    }

    public boolean tryAttackBestEnemy() throws GameActionException {
        // Try to attack someone
        RobotInfo bestEnemy = getBestEnemy();
        if(bestEnemy != null) {
            if(rc.canAttack(bestEnemy.getLocation())) {
                rc.attack(bestEnemy.getLocation());
                // Debug.printString("Attacking: " + bestEnemy.getLocation().toString());
                return true;
            } else {
                // Debug.printString("Enemy: " + bestEnemy.getLocation().toString());
            }
        }
        return false;
    }

    // Rotates at distance rad due to BugNav
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

    // Rotates at distance rad due to BugNav
    // Also ignores higher rubble squares if you're already close
    boolean moveMoreSafely(MapLocation loc, int rad) throws GameActionException {
        if (loc == null) return false;
        int d = rc.getLocation().distanceSquaredTo(loc);
        d = Math.min(d, rad);
        boolean[] imp = new boolean[Util.directionsCenter.length];
        boolean greedy = false;
        for (int i = Util.directionsCenter.length; i-- > 0; ){
            MapLocation newLoc = rc.getLocation().add(Util.directionsCenter[i]);
            if (newLoc.distanceSquaredTo(loc) <= d ||
                (currLoc.isWithinDistanceSquared(loc, 2 * rad) && rc.senseRubble(currLoc) < (20 + 1.2 * Util.getRubble(newLoc)))) {
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
