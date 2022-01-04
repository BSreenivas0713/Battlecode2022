package MPBasic;

import battlecode.common.*;
import MPBasic.Debug.*;
import MPBasic.Util.*;

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
            int theirArchons = Util.enemyArchonCount(rc.readSharedArray(0));
            for (RobotInfo robot : enemies) {
                MapLocation robotLoc = robot.getLocation();
                if (theirArchons < rc.getArchonCount() && robot.getType() == RobotType.ARCHON) {
                    boolean shouldInsert = true;
                    for (int i = Util.firstEnemy; i < Util.firstEnemy + theirArchons; i++) {
                        int testFlag = rc.readSharedArray(i);
                        if (robotLoc.x == Util.xcoord(testFlag) && robotLoc.y == Util.ycoord(testFlag)) {
                            shouldInsert = false;
                            break;
                        }
                    }
                    if (shouldInsert) {
                        int locFlag = Util.storeMyLoc(robot);
                        rc.writeSharedArray(Util.firstEnemy + theirArchons, locFlag);
                        Util.incrementEnemy(rc);
                    }
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
            System.out.println("I moved!");
        }
    }
}
