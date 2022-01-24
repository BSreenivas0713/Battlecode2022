package MPTempName;

import battlecode.common.*;
import MPTempName.Debug.*;
import MPTempName.Util.*;
import MPTempName.Comms.InformationCategory;

public class Laboratory extends Robot{
    static int startRound;
    static int MIN_NUM_MINERS;


    public Laboratory(RobotController r) throws GameActionException {
        this(r, Comms.firstArchonFlag);
    }

    public Laboratory(RobotController r, int homeFlagIndex) throws GameActionException {
        super(r);
        startRound = rc.getRoundNum();
        homeFlagIdx = homeFlagIndex;
        int MAX_NUM_MINERS = Math.min(Util.MAX_MINERS,
        rc.getMapWidth() * rc.getMapHeight() /
        Util.MAX_MAP_SIZE_TO_MINER_RATIO);
        MIN_NUM_MINERS = MAX_NUM_MINERS / 5;
    }
    
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Comms.incrementAliveLabs();
        
        int soldierCount = Comms.getSteadySoldierIdx();
        int sageCount = Comms.readSageCount();
        int minerCount = Comms.getSteadyMinerIdx();
        Debug.printString("mineCount: " + minerCount + "sageCount: " + sageCount);
        if(rc.canTransmute() &&
            ((minerCount >= (sageCount)/2) || 
                minerCount >= MIN_NUM_MINERS ||
                rc.getTeamLeadAmount(team) >= 75)) {
            Debug.printString("transmuting");
            rc.transmute();
        }
    }
    
}
