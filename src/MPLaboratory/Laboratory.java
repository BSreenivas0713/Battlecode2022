package MPLaboratory;

import battlecode.common.*;
import MPLaboratory.Debug.*;
import MPLaboratory.Util.*;

public class Laboratory extends Robot{
    public Laboratory(RobotController r) throws GameActionException {
        super(r);
    }
    
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        int soldierCount = Comms.getSteadySoldierIdx();
        int sageCount = Comms.readSageCount();
        Debug.printString("soldCount: " + soldierCount + "sageCount: " + sageCount);
        if(rc.canTransmute() && !Comms.AnyoneUnderAttack() && soldierCount > 5 && 2 * sageCount < soldierCount) {
            rc.transmute();
            Debug.printString("Transmuting");
        }
    }
    
}
