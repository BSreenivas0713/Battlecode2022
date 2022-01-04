package MPBasic;

import battlecode.common.*;
import MPBasic.Debug.*;
import MPBasic.Util.*;

public class Miner extends Robot {
    public Miner(RobotController r) throws GameActionException {
        super(r);
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
                System.out.println("there is lead here!");
                leadSource = loc;
            }
            int goldAmount = rc.senseGold(loc);
            if (goldAmount != 0 && goldAmount > bestGoldSource){
                System.out.println("there is gold here!");
                goldSource = loc;
            }
        }
        if(goldSource != null) {
            while(rc.canMineLead(goldSource)) {
                rc.mineLead(goldSource);
            }
        }
        if(leadSource != null) {
            while(rc.canMineLead(leadSource)) {
                rc.mineLead(leadSource);
            }
        }
        Direction dir = Util.directions[Util.rng.nextInt(Util.directions.length)];
        for(RobotInfo robot: FriendlySensable) {
            if(robot.getType() == RobotType.ARCHON) {
                dir = rc.getLocation().directionTo(robot.getLocation()).opposite();
            }
        }
        if(leadSource!= null) {
            dir = rc.getLocation().directionTo(leadSource);
        }
        if(goldSource!= null) {
            dir = rc.getLocation().directionTo(leadSource);
        }
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}
