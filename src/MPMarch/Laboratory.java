package MPMarch;

import battlecode.common.*;
import MPMarch.Debug.*;
import MPMarch.Util.*;

public class Laboratory extends Robot{
    static int startRound;
    public Laboratory(RobotController r) throws GameActionException {
        super(r);
        startRound = rc.getRoundNum();
        
    }
    
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Comms.signalLabStillAlive();
        int soldierCount = Comms.getSteadySoldierIdx();
        int sageCount = Comms.readSageCount();
        Debug.printString("soldCount: " + soldierCount + "sageCount: " + sageCount);
        if(rc.canTransmute() && !Comms.AnyoneUnderAttack() && 1.2 * sageCount < soldierCount) {
            rc.transmute();
            Debug.printString("Transmuting");
        }
    }
    
}
