package MPBuilder;

import battlecode.common.*;
import MPBuilder.Debug.*;
import MPBuilder.Util.*;

public class Builder extends Robot{
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
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Comms.incrementBuilderCount();
        boolean repairing = false;
        int overallDX = 0;
        int overallDY = 0;
        for(RobotInfo robot: FriendlySensable) {
            if(robot.type == RobotType.WATCHTOWER) {
                if(robot.health < Util.WatchTowerHealths[robot.level - 1]) {
                    if(rc.canRepair(robot.location)) {
                        rc.repair(robot.location);
                    }
                    repairing = true;
                }
                if(rc.canMutate(robot.location)) {
                    rc.mutate(robot.location);
                }
            }
            if(robot.location.distanceSquaredTo(currLoc) < Util.WatchTowerDomain && (robot.type == RobotType.BUILDER || robot.type == RobotType.WATCHTOWER)) {
                overallDX += currLoc.directionTo(robot.getLocation()).dx * (10000 / (currLoc.distanceSquaredTo(robot.location)));
                overallDY += currLoc.directionTo(robot.getLocation()).dy * (10000 / (currLoc.distanceSquaredTo(robot.location)));
            }
        }
        if(!repairing) {
            if(overallDX != 0 || overallDY != 0) {
                Direction DirectionAway = currLoc.directionTo(currLoc.translate(overallDX, overallDY)).opposite();
                tryMoveDest(Util.getInOrderDirections(DirectionAway));
                rc.setIndicatorString("trying to lattice with other builders");
            }
            else {
                rc.setIndicatorString("creating a new watch tower");
                Direction newDir = Util.randomDirection();
                if (rc.canBuildRobot(RobotType.WATCHTOWER,newDir) && !enemyNear()) {
                    rc.buildRobot(RobotType.WATCHTOWER, newDir);
                }
            }
        }
        else {
            rc.setIndicatorString("repairing");
        }
    }
}
