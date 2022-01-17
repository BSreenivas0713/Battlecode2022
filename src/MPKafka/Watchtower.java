package MPKafka;

import battlecode.common.*;
import MPKafka.Debug.*;
import MPKafka.Util.*;

public class Watchtower extends Robot {
    static int timeSinceLastAttacked;
    public Watchtower(RobotController r) throws GameActionException {
        super(r);
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
            //try vision again with updates
            if (rc.senseNearbyRobots(actionRadiusSquared, rc.getTeam().opponent()).length != 0) {
                if(rc.canTransform()) {
                    rc.transform();
                    timeSinceLastAttacked = 0;
                }
            }
            else {
                if(avgEnemyLoc != null) {
                    Direction bestDir = Nav.navTo(avgEnemyLoc);
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
