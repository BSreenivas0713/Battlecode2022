package MPExplore;

import battlecode.common.*;
import MPExplore.Debug.*;
import MPExplore.Util.*;

public class Watchtower extends Robot {
    public Watchtower(RobotController r) throws GameActionException {
        super(r);
    }
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        tryAttackBestEnemy();
    }
}
