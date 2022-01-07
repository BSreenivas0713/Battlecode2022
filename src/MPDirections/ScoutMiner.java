package MPDirections;

import battlecode.common.*;

// TODO: Have ScoutMiners report the dest that they're going to even if they don't see it
// if they're being attacked and are about to die.
public class ScoutMiner extends Robot {
    MapLocation dest;
    int roundNumBorn;
    int minerCount;
    MapLocation unitLeadLoc;

    public ScoutMiner(RobotController r, MapLocation dest) throws GameActionException {
        super(r);
        roundNumBorn = r.getRoundNum();
        unitLeadLoc = null;
        this.dest = dest;
    }

    public ScoutMiner(RobotController r, MapLocation dest, int homeFlagIndex) throws GameActionException {
        this(r, dest);
        homeFlagIdx = homeFlagIndex;
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

        minerCount = 0;
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
        MapLocation[] LeadGoldList = findLeadAndGold();
        MapLocation leadSource = LeadGoldList[0];
        MapLocation goldSource = LeadGoldList[1];
        int totalLeadSourcesWithinDomain = LeadGoldList[2].x;
        
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
            }
        }
        Direction[] dirs = {};
        String str = "";
        Debug.printString("Domain: " + totalLeadSourcesWithinDomain);

        if(rc.canSenseLocation(dest)) {
            Miner bot = new Miner(rc);
            bot.roundNumBorn = this.roundNumBorn;
            changeTo = bot;
        } else {
            Direction bestDir = Nav.getBestDir(dest);
            dirs = Util.getInOrderDirections(bestDir);

            Debug.setIndicatorDot(Debug.INDICATORS, dest, 255, 0, 0);
            Debug.setIndicatorLine(Debug.INDICATORS, currLoc, currLoc.add(bestDir), 0, 0, 255);
        }

        Debug.printString(str);
        tryMoveDest(dirs);
    }
}
