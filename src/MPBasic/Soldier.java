package MPBasic;

import battlecode.common.*;
import MPBasic.Debug.*;
import MPBasic.Util.*;
import MPBasic.Comms.*;

public class Soldier extends Robot{
    public Soldier(RobotController r) throws GameActionException {
        super(r);
    }
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length > 0) {
            for (RobotInfo robot : enemies) {
                MapLocation robotLoc = robot.getLocation();
                //report enemy archon if not found yet
                if (robot.getType() == RobotType.ARCHON) {
                    reportEnemyArchon(robotLoc, robot.health);
                }
            }
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
            }
        }

        // Also try to move randomly.
        Direction dir = Util.directions[Util.rng.nextInt(Util.directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}
