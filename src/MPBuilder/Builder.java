package MPBuilder;

import battlecode.common.*;
import MPBuilder.Debug.*;
import MPBuilder.Util.*;
import MPBuilder.fast.FastIterableLocSet;


public class Builder extends Robot{
    static boolean repairing;
    static boolean making;
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
        for(RobotInfo robot: FriendlySensable) {
            if(robot.type == RobotType.ARCHON) {
                if(currLoc.distanceSquaredTo(robot.location) <= 8) {
                    seenFriendly = true;
                }
                MapLocation robotLoc = robot.location;
                for(MapLocation newLoc: Util.makePattern(robotLoc)) {
                    if(currLoc.distanceSquaredTo(newLoc) <=2) {
                        if(rc.canBuildRobot(RobotType.WATCHTOWER, currLoc.directionTo(newLoc))) {
                            rc.setIndicatorString("Building a Watchtower");
                            rc.buildRobot(RobotType.WATCHTOWER, currLoc.directionTo(newLoc));
                        }
                        else {
                            rc.setIndicatorString("Cannot make robot at: " + newLoc.toString());
                        }
                    }
                }               
            }
            if(robot.type == RobotType.WATCHTOWER) {
                if(currLoc.distanceSquaredTo(robot.location) <= 8) {
                    seenFriendly = true;
                }               
                MapLocation robotLoc = robot.location;
                MapLocation[] Pattern = Util.makePattern(robotLoc);
                for(MapLocation newLoc: Pattern) {
                    if(currLoc.distanceSquaredTo(newLoc) <=2 && rc.canBuildRobot(RobotType.WATCHTOWER, currLoc.directionTo(newLoc))) {
                        rc.setIndicatorString("Building a Watchtower");
                        making = true;
                        rc.buildRobot(RobotType.WATCHTOWER, currLoc.directionTo(newLoc));
                    }
                }
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
                    rc.setIndicatorString("repairing at " + robot.location.toString() + ", health is " + robot.health + ", max health is " + Util.WatchTowerHealths[robot.level - 1]);
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
            if(seenFriendly) {
                rc.setIndicatorString("Friendly near, moving away from home");
                tryMoveDest(Util.getInOrderDirections(currLoc.directionTo(home).opposite()));
            }
            else {
                rc.setIndicatorString("No Friendly near, moving towards home");
                tryMoveDest(Util.getInOrderDirections(currLoc.directionTo(home)));
            }
        }
            
        else {
            int a = 0;//rc.setIndicatorString("repairing");
        }
    }
}
