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

    public int getScore(int diff, int leadAmount) {
        return diff + (int)(leadAmount * .3);
    }

    public MapLocation[] findLeadAndGold() throws GameActionException {
        MapLocation leadSource = null;
        int bestLeadScore = -1;
        MapLocation goldSource = null;
        int bestGoldSource = -1;
        // find the best lead source, prioritizing lead that is within your action radius
        for(MapLocation loc: rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), RobotType.MINER.visionRadiusSquared)) {
            int leadDistToHomeDiff = rc.getLocation().distanceSquaredTo(home) - loc.distanceSquaredTo(home);
            int leadAmount = rc.senseLead(loc);
            int leadScore = getScore(leadDistToHomeDiff, leadAmount);
            if (leadAmount > 1){
                if (leadSource == null) {
                    leadSource = loc;
                    bestLeadScore = leadScore;
                }
                else if (leadSource.isWithinDistanceSquared(rc.getLocation(), actionRadiusSquared)) {
                    if (bestLeadScore < leadScore && loc.isWithinDistanceSquared(rc.getLocation(), actionRadiusSquared)) {
                        leadSource = loc;
                        bestLeadScore = leadScore;
                    }
                }
                else {
                    if (bestLeadScore < leadScore || loc.isWithinDistanceSquared(rc.getLocation(), actionRadiusSquared)) {
                        leadSource = loc;
                        bestLeadScore = leadScore;
                    }
                }
            }
            int goldAmount = rc.senseGold(loc);
            if (goldAmount != 0){
                if (goldSource == null) {
                    goldSource = loc; 
                    bestGoldSource = goldAmount;
                }
                else if (goldSource.isWithinDistanceSquared(rc.getLocation(), actionRadiusSquared)) {
                    if (goldAmount > bestGoldSource && loc.isWithinDistanceSquared(rc.getLocation(), actionRadiusSquared)) {
                        goldSource = loc;
                        bestGoldSource = goldAmount;
                    }
                }
                else {
                    if (goldAmount > bestGoldSource || loc.isWithinDistanceSquared(rc.getLocation(), actionRadiusSquared)) {
                        goldSource = loc;
                        bestGoldSource = goldAmount;
                    }
                }
            }
        }
        return new MapLocation[]{leadSource, goldSource};
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Comms.incrementMinerCounter();
        // Try to mine on squares around us.
        boolean amMining = false;
        MapLocation[] LeadGoldList = findLeadAndGold();
        MapLocation leadSource = LeadGoldList[0];
        MapLocation goldSource = LeadGoldList[1];
        if(goldSource != null) {
            rc.setIndicatorString("Can see Lead!");
            while(rc.canMineGold(goldSource)) {
                rc.setIndicatorString("Mining Gold!");
                rc.mineGold(goldSource);
            }
        }
        if(leadSource != null) {
            rc.setIndicatorString("Can see Lead: " + leadSource.toString());
            while(rc.canMineLead(leadSource) && rc.senseLead(leadSource) > 1) {
                rc.setIndicatorString("Mining Lead!");
                rc.mineLead(leadSource);
            }
        }
        Direction[] dir = {};
        if (!amMining) { 
            dir = Nav.exploreGreedy();
        }
        String str = "";
        for(RobotInfo robot: FriendlySensable) {
            if(robot.getType() == RobotType.ARCHON && rc.getRoundNum() == roundNumBorn + 1) {
                dir = Util.getInOrderDirections(rc.getLocation().directionTo(robot.getLocation()).opposite());
                str = "going away from AR";
            }
        }
        if(leadSource!= null) {
            dir = Util.getInOrderDirections(rc.getLocation().directionTo(leadSource));
            str = "going towards lead";
        }
        if(goldSource!= null) {
            dir = Util.getInOrderDirections(rc.getLocation().directionTo(goldSource));
            str = "going toward gold";
        }
        if (tryMoveDest(dir)) {
            rc.setIndicatorString(str);
        }
    }
}
