package MPBuilder;

import battlecode.common.*;
import MPBuilder.Debug.*;
import MPBuilder.Util.*;

public class Builder extends Robot{
    public Builder(RobotController r) throws GameActionException {
        super(r);
    }
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Comms.incrementBuilderCount();
    }
}
