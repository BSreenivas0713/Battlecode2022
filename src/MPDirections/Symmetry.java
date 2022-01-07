package MPDirections;

import battlecode.common.*;

public class Symmetry {
    static RobotController rc;
    static MapLocation[] possibleEnemyLocations;

    static void init(RobotController r) throws GameActionException {
        rc = r;
        guessEnemyArchonLocations();
    }

    static void guessEnemyArchonLocations() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        int w = rc.getMapWidth();
        int h = rc.getMapHeight();

        possibleEnemyLocations = new MapLocation[3];
        possibleEnemyLocations[0] = new MapLocation(w - currLoc.x - 1, currLoc.y);
        possibleEnemyLocations[1] = new MapLocation(currLoc.x, h - currLoc.y - 1);
        possibleEnemyLocations[2] = new MapLocation(w - currLoc.x - 1, h - currLoc.y - 1);
    }
}
