package MPBasic;

import battlecode.common.*;
import MPBasic.Debug.*;
import MPBasic.Util.*;

public class Miner extends Robot {
    int roundNumBorn;
    public Miner(RobotController r) throws GameActionException {
        super(r);
        roundNumBorn = r.getRoundNum();
    }
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        // Try to mine on squares around us.
        MapLocation leadSource = null;
        int bestLeadSource = -1;
        MapLocation goldSource = null;
        int bestGoldSource = -1;
        for(MapLocation loc: rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), RobotType.MINER.visionRadiusSquared)) {
            int leadAmount = rc.senseLead(loc);
            if (leadAmount != 0 && leadAmount > bestLeadSource){
                leadSource = loc;
            }
            int goldAmount = rc.senseGold(loc);
            if (goldAmount != 0 && goldAmount > bestGoldSource){
                goldSource = loc;
            }
        }
        if(goldSource != null) {
            while(rc.canMineGold(goldSource)) {
                rc.setIndicatorString("Mining Gold!");
                rc.mineGold(goldSource);
            }
        }
        if(leadSource != null) {
            while(rc.canMineLead(leadSource)) {
                rc.setIndicatorString("Mining Lead!");
                rc.mineLead(leadSource);
            }
        }
        Direction[] dir = Nav.exploreGreedy(rc);
        String str = "";
        for(RobotInfo robot: FriendlySensable) {
            if(robot.getType() == RobotType.ARCHON && rc.getRoundNum() == roundNumBorn + 1) {
                dir = Util.getInOrderDirectios(rc.getLocation().directionTo(robot.getLocation()).opposite());
                str = "going away from AR";
            }
        }
        if(leadSource!= null) {
            dir = Util.getInOrderDirectios(rc.getLocation().directionTo(leadSource));
            str = "going towards lead";
        }
        if(goldSource!= null) {
            dir = Util.getInOrderDirectios(rc.getLocation().directionTo(goldSource));
            str = "going toward gold";
        }
        if (tryMoveDest(dir)) {
            rc.setIndicatorString(str);
        }
    }
}
