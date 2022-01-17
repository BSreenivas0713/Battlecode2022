package MPKafka;

import battlecode.common.*;
import MPKafka.Debug.*;
import MPKafka.Util.*;

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
