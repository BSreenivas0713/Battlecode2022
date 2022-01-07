package MPExplore;

import battlecode.common.*;
import MPExplore.Debug.*;
import MPExplore.Util.*;
import MPExplore.fast.FastIterableLocSet;


public class SpawnKillBuilder extends Robot {
    static MapLocation destination; 

    public SpawnKillBuilder(RobotController r) throws GameActionException {
        super(r);
        destination = null;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        boolean inEmptySpot = rc.senseLead(currLoc) == 0;
        if (inEmptySpot) {
            rc.disintegrate();
        }
        updateDest();
        if (destination != null) {
            Direction[] targets = Nav.greedyDirection(currLoc.directionTo(destination));
            tryMoveDest(targets);
        } else {
            tryMoveDest(Nav.explore());
        }
    }

    public void updateDest() throws GameActionException {
        MapLocation[] squaresToCheckForLead = rc.getAllLocationsWithinRadiusSquared(currLoc, visionRadiusSquared);
        for (int i = 0; i < squaresToCheckForLead.length; i++) {
            if (rc.senseLead(squaresToCheckForLead[i]) == 0) {
                destination = squaresToCheckForLead[i];
            }
        }
    }
}
