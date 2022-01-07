package MPDirections;

import battlecode.common.*;
import MPDirections.Debug.*;
import MPDirections.Util.*;

public class Watchtower extends Robot {
    public Watchtower(RobotController r) throws GameActionException {
        super(r);
    }
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        tryAttackBestEnemy();
    }
}
