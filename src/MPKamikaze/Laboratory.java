package MPKamikaze;

import battlecode.common.*;
import MPKamikaze.Debug.*;
import MPKamikaze.Util.*;

public class Laboratory extends Robot{
    public Laboratory(RobotController r) throws GameActionException {
        super(r);
    }
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if(rc.canTransmute()) {
            rc.transmute();
        }
    }
    
}
