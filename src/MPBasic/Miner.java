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
        return diff + (int)(leadAmount * .5);
    }

    public MapLocation[] findLeadAndGold() throws GameActionException {
        MapLocation leadSource = null;
        int bestLeadScore = -1;
        MapLocation goldSource = null;
        int totalLeadSourcesWithinDomain = 0;
        int someoneClaimed = 0;
        int minerCount = 0;
        float overallDX = 0;
        float overallDY = 0;
// find the best lead source, prioritizing lead that is within your action radius
        for(MapLocation loc: rc.getAllLocationsWithinRadiusSquared(currLoc, RobotType.MINER.visionRadiusSquared)) {
            int leadDistToHomeDiff = currLoc.distanceSquaredTo(home) - loc.distanceSquaredTo(home);
            int leadAmount = rc.senseLead(loc);
            int leadScore = getScore(leadDistToHomeDiff, leadAmount);
            if (leadAmount > 1){
                if (leadSource == null) {
                    leadSource = loc;
                    bestLeadScore = leadScore;
                }
                else if (leadSource.isWithinDistanceSquared(currLoc, actionRadiusSquared)) {
                    if (bestLeadScore < leadScore && loc.isWithinDistanceSquared(currLoc, actionRadiusSquared)) {
                        leadSource = loc;
                        bestLeadScore = leadScore;
                    }
                }
                else {
                    if (bestLeadScore < leadScore || loc.isWithinDistanceSquared(currLoc, actionRadiusSquared)) {
                        leadSource = loc;
                        bestLeadScore = leadScore;
                    }
                }
            }               
            
            if (leadAmount > 0 && loc.isWithinDistanceSquared(currLoc, Util.MinerDomain)) {
                totalLeadSourcesWithinDomain ++;            
                RobotInfo possibleFriendly = rc.senseRobotAtLocation(loc);
                if(possibleFriendly != null && possibleFriendly.type == RobotType.MINER && !loc.equals(currLoc)) {
                    someoneClaimed = 1;
                }
            }
            int goldAmount = rc.senseGold(loc);
            if (goldAmount != 0){
                goldSource = loc; 
            }
        }
        return new MapLocation[]{leadSource, goldSource, 
               new MapLocation(totalLeadSourcesWithinDomain, someoneClaimed), 
               new MapLocation((int)(overallDX), (int)(overallDY)),
               new MapLocation(minerCount,0)};
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Comms.incrementMinerCounter();
        // Try to mine on squares around us.
        boolean amMining = false;
        MapLocation[] LeadGoldList = findLeadAndGold();
        MapLocation leadSource = LeadGoldList[0];
        MapLocation goldSource = LeadGoldList[1];
        int totalLeadSourcesWithinDomain = LeadGoldList[2].x;
        int someoneClaimed = LeadGoldList[2].y;
        int minerCount = LeadGoldList[4].x;
        if(goldSource != null) {
            rc.setIndicatorString("Can see Gold!");
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
                amMining = true;
            }
        }
        Direction[] dir = {};
        String str = "";
        String str1 = "domain Size: " + Integer.toString(totalLeadSourcesWithinDomain) + ", ";

        if (!amMining && (totalLeadSourcesWithinDomain < 3 || someoneClaimed == 1)) {
            dir = Nav.explore();
            str = "Exploring";
        }
        for(RobotInfo robot: FriendlySensable) {
            if(robot.getType() == RobotType.ARCHON && rc.getRoundNum() == roundNumBorn + 1) {
                dir = Util.getInOrderDirections(currLoc.directionTo(robot.getLocation()).opposite());
                str = "going away from AR";
            }
        }
        if(leadSource!= null) {
            dir = Util.getInOrderDirections(currLoc.directionTo(leadSource));
            str = "going towards lead";
    }
        if(goldSource!= null) {
            dir = Util.getInOrderDirections(currLoc.directionTo(goldSource));
            str = "going toward gold";
        }

        rc.setIndicatorString(str1 + str);
        tryMoveDest(dir);
    }
}
