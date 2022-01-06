package MPCoordinated;

import battlecode.common.*;
import MPCoordinated.Debug.*;
import MPCoordinated.Util.*;

public class Watchtower extends Robot {
    public Watchtower(RobotController r) throws GameActionException {
        super(r);
    }
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        tryAttackBestEnemy();
    }
}
