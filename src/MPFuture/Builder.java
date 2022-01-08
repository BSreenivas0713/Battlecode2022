package MPFuture;

import battlecode.common.*;
import MPFuture.Debug.*;
import MPFuture.Util.*;
import MPFuture.fast.FastIterableLocSet;


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
                        if(robot.mode == RobotMode.TURRET && archonTowerCount < 13 && currLoc.distanceSquaredTo(newLoc) <= 2 && rc.canBuildRobot(RobotType.WATCHTOWER, currLoc.directionTo(newLoc))) {
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

    public void repairWatchtowerIfPossible() throws GameActionException{
        for(RobotInfo robot: FriendlySensable) {
            if(currLoc.distanceSquaredTo(robot.location) < actionRadiusSquared && robot.getType() == RobotType.WATCHTOWER) {
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

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Comms.incrementBuilderCount();
        repairing = false;
        making = false;
        boolean seenFriendly = makeWatchtowerIfPossible();
        repairWatchtowerIfPossible();
        if(!repairing && !making) {
            if(closestEmptySpotToHome != null) {
                Debug.printString("Moving towards location that needs to be filled");
                tryMoveDest(Util.getInOrderDirections(currLoc.directionTo(closestEmptySpotToHome)));
            }

            if(seenFriendly) {
                Debug.printString("Friendly near, moving away from home");
                Direction awayFromHome = currLoc.directionTo(home).opposite();
                if(!rc.onTheMap(currLoc.add(awayFromHome))) {
                    tryMoveDest(Util.getInOrderDirections(Util.turnRight90(currLoc.directionTo(home))));
                }
                else {
                    tryMoveDest(Nav.greedyDirection(awayFromHome));
                }
            }
            else {
                Debug.printString("No Friendly near, moving towards home");
                tryMoveDest(Nav.greedyDirection(currLoc.directionTo(home)));
            }
        }
    }
}
