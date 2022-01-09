package MPHoorayForSleep;

import battlecode.common.*;
import MPHoorayForSleep.Debug.*;
import MPHoorayForSleep.Util.*;

public class Watchtower extends Robot {
    static int timeSinceLastAttacked;
    static int spinDirection;
    public Watchtower(RobotController r) throws GameActionException {
        super(r);
        spinDirection = Util.rng.nextInt(1);
        timeSinceLastAttacked = 5;
    }
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        MapLocation avgEnemyLoc = Comms.getClosestCluster(currLoc);
        if(rc.getMode() == RobotMode.TURRET) {
            if(tryAttackBestEnemy()) {
                timeSinceLastAttacked = 0;
            }
            else {
                timeSinceLastAttacked++;
                Debug.printString("Time Since Last Attacked: " + timeSinceLastAttacked);
            }
            if (timeSinceLastAttacked >= 5 && avgEnemyLoc != null && currLoc.distanceSquaredTo(avgEnemyLoc) > visionRadiusSquared * 1.5 && home.distanceSquaredTo(avgEnemyLoc) > visionRadiusSquared * 1.5) {
                if(rc.canTransform()) {
                    rc.transform();
                }
            }
        }
        else {
            if (rc.senseNearbyRobots(actionRadiusSquared, rc.getTeam().opponent()).length != 0) {
                if(rc.canTransform()) {
                    rc.transform();
                    timeSinceLastAttacked = 0;
                }
            }
            else {
                if(avgEnemyLoc != null) {
                    Direction bestDir = Nav.getBestDir(avgEnemyLoc);
                    tryMove(bestDir);
                    if(Math.random() < .5) {
                        tryMoveDest(Nav.greedyDirection(Util.turnRight90(bestDir)));
                    }
                    else {
                        tryMoveDest(Nav.greedyDirection(Util.turnLeft90(bestDir)));
                    }
                }
            }
        }

    }
}
