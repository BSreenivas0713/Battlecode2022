package MPTempName;

import battlecode.common.*;
import MPTempName.Debug.*;
import MPTempName.Util.*;
import MPTempName.fast.FastIterableLocSet;


public class Builder extends Robot{
    static boolean repairing;
    static boolean making;
    static MapLocation closestEmptySpotToHome;
    public Builder(RobotController r) throws GameActionException {
        super(r);
    }
    public boolean enemyNear() throws GameActionException {
        for (RobotInfo robot: EnemySensable) {
            RobotType robottype = robot.getType();
            if( robottype != RobotType.MINER && robottype != RobotType.BUILDER && robottype != RobotType.LABORATORY) {
                return true;
            }
        }
        return false;
    }
    public boolean makeLabIfPossible() throws GameActionException {
        if(!Comms.haveBuiltLab() && Comms.haveBuiltBuilderForFinalLab()) {
            int bestRubble = Integer.MAX_VALUE;
            Direction bestDir = null;
            for(Direction Dir: Util.getFullInOrderDirections(currLoc.directionTo(home).opposite())) {
                int currRubble = Util.getRubble(currLoc.add(Dir));
                if (rc.canBuildRobot(RobotType.LABORATORY, Dir) && currRubble < bestRubble) {
                    bestRubble = currRubble;
                    bestDir = Dir;
                }
            }
            if (bestDir != null) {
                making = true;
                rc.buildRobot(RobotType.LABORATORY, bestDir);
                Comms.signalLabBuilt();
            }
            return true;
        }
        return false;
    }
    public boolean makeWatchtowerIfPossible() throws GameActionException{
        boolean seenFriendly = false;
        closestEmptySpotToHome = null;
        int distClosetsEmptySpotToHome = -1;
        RobotInfo robot;
        int archonTowerCount = 0;
        for (RobotInfo Friend: FriendlySensable) {
            if(Friend.type == RobotType.ARCHON || Friend.type == RobotType.WATCHTOWER) {
                archonTowerCount++;
            }
        }
        for (int i = FriendlySensable.length - 1; i >= 0; i--) {
            robot = FriendlySensable[i];
            switch(robot.type) {
                case ARCHON:
                case WATCHTOWER:
                    if(currLoc.distanceSquaredTo(robot.location) <= 8) {
                        seenFriendly = true;
                    }
                    MapLocation robotLoc = robot.location;
                    for(MapLocation newLoc: Util.makePattern(robotLoc)) {
                        if(!(Comms.haveBuiltBuilderForFinalLab() && !Comms.haveBuiltLab()) &&  robot.mode == RobotMode.TURRET && archonTowerCount < 13 && currLoc.distanceSquaredTo(newLoc) <= 2 && rc.canBuildRobot(RobotType.WATCHTOWER, currLoc.directionTo(newLoc))) {
                            Debug.printString("Building a Watchtower");
                            making = true;
                            rc.buildRobot(RobotType.WATCHTOWER, currLoc.directionTo(newLoc));
                        }
                        if((closestEmptySpotToHome == null || newLoc.distanceSquaredTo(home) < distClosetsEmptySpotToHome)) {
                            if(rc.canSenseLocation(newLoc)) {
                                if(rc.senseRobotAtLocation(newLoc) == null) {
                                    closestEmptySpotToHome = newLoc;
                                    distClosetsEmptySpotToHome = newLoc.distanceSquaredTo(home);
                                }
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return seenFriendly;
    }

    public void repairIfPossible() throws GameActionException{
        for(RobotInfo robot: FriendlySensable) {
            if(currLoc.distanceSquaredTo(robot.location) < actionRadiusSquared && (robot.getType() == RobotType.WATCHTOWER || robot.getType() == RobotType.LABORATORY)) {
                if(robot.health < robot.getType().getMaxHealth(robot.level)) {
                    if(rc.canRepair(robot.location)) {
                        rc.repair(robot.location);
                    }
                    repairing = true;
                    Debug.printString("Repairing " + robot.location.toString() + ", Health " + robot.health + "/" + Util.WatchTowerHealths[robot.level - 1]);
                }
                if(rc.canMutate(robot.location)) {
                    rc.mutate(robot.location);
                }
            }
        }
    }

    public boolean runFromEnemy() throws GameActionException {
        MapLocation target = null;
        RobotInfo closestEnemy = getClosestEnemy(RobotType.SOLDIER);
        if(closestEnemy != null) {
            target = Nav.getGreedyTargetAway(closestEnemy.getLocation());
        }
        closestEnemy = getClosestEnemy(RobotType.WATCHTOWER);
        if(closestEnemy != null) {
            target = Nav.getGreedyTargetAway(closestEnemy.getLocation());
        }
        closestEnemy = getClosestEnemy(RobotType.SAGE);
        if(closestEnemy != null) {
            target = Nav.getGreedyTargetAway(closestEnemy.getLocation());
        }
        if(target != null) {
            Nav.move(target);
            return true;
        }
        else {
            return false;
        }
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Comms.incrementBuilderCount();
        repairing = false;
        making = false;
        boolean runningFromEnemy = runFromEnemy();
        makeLabIfPossible();
        boolean needToMakeLab = !Comms.haveBuiltLab();
        boolean seenFriendly = makeWatchtowerIfPossible();
        repairIfPossible();



        if(!repairing && !making && !runningFromEnemy) {
            if(needToMakeLab) {
                if(currLoc.distanceSquaredTo(home) < robotType.ARCHON.visionRadiusSquared) {
                    Debug.printString("getting good Lab Loc");
                    MapLocation avgEnemyLoc = home;
                    MapLocation betterEnemyLoc = Comms.getClosestCluster(currLoc);
                    if(betterEnemyLoc != null) {
                        avgEnemyLoc = betterEnemyLoc;
                    }
                    tryMoveDest(Util.getInOrderDirections(Nav.getGreedyDirection(currLoc.directionTo(avgEnemyLoc).opposite())));
                }
            }
            else if(closestEmptySpotToHome != null) {
                Debug.printString("Moving towards location that needs to be filled");
                tryMoveDest(Util.getInOrderDirections(currLoc.directionTo(closestEmptySpotToHome)));
            }

            else if(seenFriendly) {
                Debug.printString("Friendly near, moving away from home");
                Direction awayFromHome = currLoc.directionTo(home).opposite();
                if(!rc.onTheMap(currLoc.add(awayFromHome))) {
                    Direction dir = Nav.getBestDir(currLoc.add(Util.turnRight90(currLoc.directionTo(home))));
                    if(dir != null) {
                        tryMoveDest(Util.getInOrderDirections(dir));
                    }
                }
                else {
                    Direction dir = Nav.getBestDir(currLoc.add(awayFromHome));
                    if(dir != null) {
                        tryMoveDest(Util.getInOrderDirections(dir));
                    }
                }
            }
            else {
                Debug.printString("No Friendly near, moving towards home");
                Direction dir = Nav.getBestDir(home);
                if(dir != null) {
                    tryMoveDest(Util.getInOrderDirections(dir));
                }
            }
        }
    }
}
