package MPTempName;

import battlecode.common.*;
import MPTempName.Debug.*;
import MPTempName.Util.*;

public class Miner extends Robot {
    int roundNumBorn;
    int minerCount;
    boolean explorer;
    MapLocation unitLeadLoc;

    public Miner(RobotController r) throws GameActionException {
        super(r);
        roundNumBorn = r.getRoundNum();
        unitLeadLoc = null;
        if(Util.rng.nextInt(6) == 0) {
            explorer = true;
        }
        else {
            explorer = false;
        }
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
        float overallDX = 0;
        float overallDY = 0;
// find the best lead source, prioritizing lead that is within your action radius
        MapLocation[] locs = rc.senseNearbyLocationsWithLead(RobotType.MINER.visionRadiusSquared);
        MapLocation loc;
        unitLeadLoc = null;
        for(int i = locs.length - 1; i >= 0; i--) {
            loc = locs[i];
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
            } else {
                unitLeadLoc = loc;
            }
            
            if (leadAmount > 0 && loc.isWithinDistanceSquared(currLoc, Util.MinerDomain)) {
                totalLeadSourcesWithinDomain ++;            
            }
        }

        locs = rc.senseNearbyLocationsWithGold(RobotType.MINER.visionRadiusSquared);
        if(locs.length > 0) {
            goldSource = locs[0];
        }

        RobotInfo[] sensableWithin8 = rc.senseNearbyRobots(8, rc.getTeam());
        RobotInfo possibleFriendly;
        for (int i = sensableWithin8.length - 1; i >= 0; i--) {
            possibleFriendly = sensableWithin8[i];
            loc = possibleFriendly.location;
            if (possibleFriendly.type == RobotType.MINER && !loc.equals(currLoc)) {
                if(currLoc.distanceSquaredTo(loc) < 3) {
                    minerCount ++;
                    overallDX += currLoc.directionTo(possibleFriendly.getLocation()).dx * (10000 / (currLoc.distanceSquaredTo(loc)));
                    overallDY += currLoc.directionTo(possibleFriendly.getLocation()).dy * (10000 / (currLoc.distanceSquaredTo(loc)));
                }

                if(rc.senseLead(loc) > 0) {
                    someoneClaimed = 1;
                }
            }
        }
        return new MapLocation[]{leadSource, goldSource, 
               new MapLocation(totalLeadSourcesWithinDomain, someoneClaimed), 
               new MapLocation((int)(overallDX), (int)(overallDY)),
               };
    }

    // Deplete unit lead sources if far away from home and more enemies than friends
    public boolean shouldDepleteUnitLead() throws GameActionException {
        return EnemySensable.length > FriendlySensable.length &&
            !currLoc.isWithinDistanceSquared(home, Util.MIN_DIST_TO_DEPLETE_UNIT_LEAD);
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
        Direction DirectionAway = currLoc.directionTo(currLoc.translate(LeadGoldList[3].x, LeadGoldList[3].y)).opposite();
        if(goldSource != null) {
            Debug.printString("Gold found");
            while(rc.canMineGold(goldSource)) {
                rc.mineGold(goldSource);
            }
        }
        if(leadSource != null) {
            Debug.printString("Lead found: " + leadSource.toString());
            if(unitLeadLoc != null && shouldDepleteUnitLead() && rc.canMineLead(unitLeadLoc)) {
                Debug.printString("Depleting unit lead");
                rc.mineLead(unitLeadLoc);
            }
            while(rc.canMineLead(leadSource) && rc.senseLead(leadSource) > 1) {
                Comms.incrementMinerMiningCounter();
                rc.mineLead(leadSource);
                amMining = true;
            }
        }
        Direction[] dir = {};
        String str = "";
        Debug.printString("Domain: " + totalLeadSourcesWithinDomain);

        if (!amMining && (totalLeadSourcesWithinDomain < 15 || someoneClaimed == 1)  && !explorer) {
            dir = Nav.explore();
            str = "Exploring";
        }

        if(rc.getRoundNum() == roundNumBorn + 1) {
            for(RobotInfo robot: FriendlySensable) {
                if(robot.getType() == RobotType.ARCHON) {
                    dir = Nav.greedyDirection(currLoc.directionTo(robot.getLocation()).opposite());
                    str = "going away from AR";
                }
            }
        }

        if(leadSource != null) {
            dir = Nav.greedyDirection(currLoc.directionTo(leadSource));
            str = "going towards lead";
            if(minerCount >= 4 && someoneClaimed == 1 && amMining) {
                dir = Nav.greedyDirection(DirectionAway);
                str = "going away from other miners: " + DirectionAway.toString() + " " + Integer.toString(LeadGoldList[3].x) + " " + Integer.toString(LeadGoldList[3].y);
            }
        }

        RobotInfo closestEnemy = getClosestEnemy(RobotType.SOLDIER);
        if(closestEnemy != null) {
            dir = Nav.greedyDirection(currLoc.directionTo(closestEnemy.getLocation()).opposite());
            str = "going away from enemy";
        }
        
        if(goldSource != null) {
            dir = Nav.greedyDirection(currLoc.directionTo(goldSource));
            str = "going toward gold";
        }

        Debug.printString(str);
        tryMoveDest(dir);
    }
}
