package MPBuilder;

import battlecode.common.*;
import MPBuilder.Debug.*;
import MPBuilder.Util.*;

public class Builder extends Robot{
    public Builder(RobotController r) throws GameActionException {
        super(r);
    }
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Comms.incrementBuilderCount();
        boolean repairing = false;
        int overallDX = 0;
        int overallDY = 0;
        for(RobotInfo robot: FriendlySensable) {
            if(robot.type == RobotType.WATCHTOWER) {
                if(rc.canRepair(robot.location)) {
                    rc.repair(robot.location);
                }
            }
            if(/*robot.location.distanceSquaredTo(currLoc) < Util.WatchTowerDomain &&*/ (robot.type == RobotType.WATCHTOWER || robot.type == RobotType.ARCHON)) {
                overallDX += currLoc.directionTo(robot.getLocation()).dx * (10000 / (currLoc.distanceSquaredTo(robot.location)));
                overallDY += currLoc.directionTo(robot.getLocation()).dy * (10000 / (currLoc.distanceSquaredTo(robot.location)));
            }
        }
        if(overallDX != 0 || overallDY != 0) {
            Direction DirectionAway = currLoc.directionTo(currLoc.translate(overallDX, overallDY)).opposite();
            tryMoveDest(Util.getInOrderDirections(DirectionAway));
        }
        else {
            Direction newDir = Util.randomDirection();
            if (rc.canBuildRobot(RobotType.WATCHTOWER,newDir)) {
                rc.buildRobot(RobotType.WATCHTOWER, newDir);
            }
        }
    }
}
