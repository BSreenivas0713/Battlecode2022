package MPTempName;

import battlecode.common.*;
import MPTempName.Debug.*;
import MPTempName.Util.*;

public class Miner extends Robot {
    static int roundNumBorn;
    static int minerCount;
    static boolean explorer;
    static MapLocation[] unitLeadLocs;
    static int numUnitLeadLocs;
    static MapLocation bestLead;
    static MapLocation goldSource;
    static float overallDX;
    static float overallDY;

    public Miner(RobotController r) throws GameActionException {
        super(r);
        roundNumBorn = r.getRoundNum();
        unitLeadLocs = new MapLocation[5];
        if(Util.rng.nextInt(6) == 0) {
            explorer = true;
        }
        else {
            explorer = false;
        }
    }

    //Update the location of the closest lead ore, return a list of all lead ores within action radius of Miner
    public int[] findLeadAndGold() throws GameActionException {
        goldSource = null;
        overallDX = 0;
        overallDY = 0;
// find the best lead source, prioritizing lead that is within your action radius
        MapLocation[] locs = rc.senseNearbyLocationsWithLead(visionRadiusSquared);
        MapLocation loc;
        numUnitLeadLocs = 0;
        int[] actionRadiusArr = new int[9];
        for (int x = 0; x < 3; x ++) {for (int y = 0; y < 3; y ++ ) {actionRadiusArr[3 * x + y] = 0;}}

        bestLead = null;
        minerCount = 0;
        double bestScore = Integer.MIN_VALUE;
        for(int i = locs.length - 1; i >= 0; i--) {
            loc = locs[i];
            int leadAmount = rc.senseLead(loc);
            if (leadAmount > 1){
                int currDist = currLoc.distanceSquaredTo(loc);
                double currScore = getLeadDistTradeoffScore(currDist, leadAmount);
                if(currScore > bestScore) {
                    bestScore = currScore;
                    bestLead = loc; 
                }
                if (currDist <= actionRadiusSquared) {
                    actionRadiusArr[(1 + (loc.x - currLoc.x)) * 3  + (1 + (loc.y - currLoc.y))] = leadAmount;
                }
            } else if(rc.canMineLead(loc) && numUnitLeadLocs < 5) {
                unitLeadLocs[numUnitLeadLocs++] = loc;
            }
        }

        locs = rc.senseNearbyLocationsWithGold(RobotType.MINER.visionRadiusSquared);
        if(locs.length > 0) {
            goldSource = locs[0];
        }
        RobotInfo possibleFriendly;
        for (int i = FriendlySensable.length - 1; i >= 0; i--) {
            possibleFriendly = FriendlySensable[i];
            loc = possibleFriendly.location;
            if (possibleFriendly.type == RobotType.MINER && !loc.equals(currLoc)) {
                minerCount ++;
                overallDX += currLoc.directionTo(possibleFriendly.getLocation()).dx * (100 / (currLoc.distanceSquaredTo(loc)));
                overallDY += currLoc.directionTo(possibleFriendly.getLocation()).dy * (100 / (currLoc.distanceSquaredTo(loc)));
            }
        }
        return actionRadiusArr;
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
        int[] actionRadiusArr = findLeadAndGold();
        Direction DirectionAway = currLoc.directionTo(currLoc.translate((int)overallDX, (int)overallDY)).opposite();
        if(goldSource != null) {
            Debug.printString("Gold found");
            while(rc.canMineGold(goldSource)) {
                rc.mineGold(goldSource);
            }
        }

        if(shouldDepleteUnitLead() && numUnitLeadLocs > 0) {
            Debug.printString("Depleting unit lead");
            for(int i = 0; i < numUnitLeadLocs && rc.canMineLead(unitLeadLocs[i]); i++) {
                rc.mineLead(unitLeadLocs[i]);
            }
        }

        if(bestLead != null) {
            Debug.printString("Lead: " + bestLead.toString());
            //Go through all lead deposits in action radius, and mine as much as possible (consider the case where two lead ores
            //within action radius have sizes 3 and 4. We want to mine them both down to 1 in the same turn 
            for(int x = 0; x < 3 && rc.isActionReady(); x ++) {
                for (int y = 0; y < 3 && rc.isActionReady(); y++) {
                    if(actionRadiusArr[3 *x + y] > 1) {
                        MapLocation leadSource = currLoc.translate(x - 1, y - 1);
                        while(rc.canMineLead(leadSource) && actionRadiusArr[3 * x + y] != 1) {
                            rc.mineLead(leadSource);
                            actionRadiusArr[3 * x + y]--;
                            amMining = true;
                        }
                    }
                }
            }
        }

        MapLocation target = null;
        String str = "";
        boolean canMine = rc.senseNearbyLocationsWithLead(2, 2).length > 0;
        if (!amMining && !canMine) {
            // target = Explore.getExploreTarget();
            target = Explore.explorePathfinding();
            str = "Exploring: " + target.toString();
            // Debug.setIndicatorDot(Debug.INDICATORS, target, 255, 204, 102);
        }

        if(rc.getRoundNum() == roundNumBorn + 1) {
            for(RobotInfo robot: FriendlySensable) {
                if(robot.getType() == RobotType.ARCHON) {
                    target = rc.adjacentLocation(currLoc.directionTo(robot.getLocation()).opposite());
                    str = "going away from AR";
                }
            }
        }

        //no need to complicate things, just go towards the closest ore
        if(bestLead != null) {
            MapLocation currLoc = rc.getLocation();
            target = Nav.getBestRubbleSquareAdjacentTo(bestLead);
            if(currLoc.isAdjacentTo(bestLead) && rc.senseRubble(currLoc) == rc.senseRubble(target)) target = currLoc;
            str = "going towards lead";
            if(minerCount >= 5) {
                // Move away if you can still mine any lead at the new location.
                Direction[] possibleDirs = Nav.greedyDirection(DirectionAway);
                for(Direction possibleDir : possibleDirs) {
                    if(rc.canMove(possibleDir)) {
                        MapLocation newLoc = rc.adjacentLocation(possibleDir);
                        if(rc.senseNearbyLocationsWithLead(newLoc, 2, 2).length > 0) {
                            target = rc.adjacentLocation(possibleDir);
                            str = "going away from other miners: " + DirectionAway.toString();
                        }
                        break;
                    }
                }
            }
            str += " #Miners: " + minerCount;
        }

        RobotInfo closestEnemy = getClosestEnemy(RobotType.SOLDIER);
        if(goldSource != null) {
            target = goldSource;
            str = "going toward gold";
        }
        if(closestEnemy != null) {
            target = Nav.getGreedyTargetAway(closestEnemy.getLocation());
            str = "going away from enemy soldier";
        }
        closestEnemy = getClosestEnemy(RobotType.WATCHTOWER);
        if(closestEnemy != null) {
            target = Nav.getGreedyTargetAway(closestEnemy.getLocation());
            str = "going away from enemy Watchtower";
        }
        closestEnemy = getClosestEnemy(RobotType.SAGE);
        if(closestEnemy != null) {
            target = Nav.getGreedyTargetAway(closestEnemy.getLocation());
            str = "going away from enemy Sage";
        }
        Debug.printString(str);
        Nav.move(target);
    }
}
