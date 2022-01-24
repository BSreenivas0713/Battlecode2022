package MPMoreLabs;

import battlecode.common.*;
import MPMoreLabs.Debug.*;
import MPMoreLabs.Util.*;
import MPMoreLabs.Comms.InformationCategory;

public class Laboratory extends Robot{
    static int startRound;

    public Laboratory(RobotController r) throws GameActionException {
        this(r, Comms.firstArchonFlag);
    }

    public Laboratory(RobotController r, int homeFlagIndex) throws GameActionException {
        super(r);
        startRound = rc.getRoundNum();
        homeFlagIdx = homeFlagIndex;
    }
    
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Comms.incrementAliveLabs();
        int soldierCount = Comms.getSteadySoldierIdx();
        int sageCount = Comms.readSageCount();
        Debug.printString("soldCount: " + soldierCount + "sageCount: " + sageCount);
        if(rc.canTransmute()) {
            if (Comms.checkIfArchonBuildingLab()) {
                if (rc.getRoundNum() % 2 == 1) {
                    rc.transmute();
                    Debug.printString("Transmuting");
                }
            }
            else {
                rc.transmute();
                Debug.printString("Transmuting");
            }
        }
    }
    
}
