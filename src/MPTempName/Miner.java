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
    static MapLocation bestGold;
    static double bestLeadScore;
    static double bestGoldScore;
    static float overallDX;
    static float overallDY;

    MapLocation closestCluster;
    RobotInfo[] enemyAttackable;
    RobotInfo[] friendlyAttackable;

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
        overallDX = 0;
        overallDY = 0;
// find the best lead source, prioritizing lead that is within your action radius
        MapLocation[] locs = rc.senseNearbyLocationsWithLead(visionRadiusSquared);
        MapLocation loc;
        numUnitLeadLocs = 0;
        int[] actionRadiusArr = new int[9];
        for (int x = 0; x < 3; x ++) {for (int y = 0; y < 3; y ++ ) {actionRadiusArr[3 * x + y] = 0;}}

        bestLead = null;
        bestLeadScore = Integer.MIN_VALUE;
        bestGold = null;
        bestGoldScore = Integer.MIN_VALUE;

        minerCount = 0;
        for(int i = locs.length - 1; i >= 0; i--) {
            loc = locs[i];
            if(!shouldConsiderResourceLoc(loc)) continue;
            int leadAmount = rc.senseLead(loc);
            if (leadAmount > 1){
                int currDist = currLoc.distanceSquaredTo(loc);
                double currScore = getLeadDistTradeoffScore(currDist, leadAmount);
                if(currScore > bestLeadScore) {
                    bestLeadScore = currScore;
                    bestLead = loc; 
                }
                if (currDist <= actionRadiusSquared) {
                    actionRadiusArr[(1 + (loc.x - currLoc.x)) * 3  + (1 + (loc.y - currLoc.y))] = leadAmount;
                }
            } else if(rc.canMineLead(loc) && numUnitLeadLocs < 5) {
                unitLeadLocs[numUnitLeadLocs++] = loc;
            }
        }

        locs = rc.senseNearbyLocationsWithGold(-1);
        for(int i = locs.length - 1; i >= 0; i--) {
            loc = locs[i];
            if(!shouldConsiderResourceLoc(loc)) continue;
            int goldAmount = rc.senseGold(loc);
            int currDist = currLoc.distanceSquaredTo(loc);
            double currScore = getGoldDistTradeoffScore(currDist, goldAmount);
            if(currScore > bestGoldScore) {
                bestGoldScore = currScore;
                bestGold = loc;
            }
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

    public boolean shouldConsiderResourceLoc(MapLocation loc) throws GameActionException {
        if(closestCluster != null &&
            closestCluster.isWithinDistanceSquared(loc, Util.MIN_LEAD_DIST_FROM_CLUSTER)) {
            // Work around miners reporting the cluster
            return currLoc.isWithinDistanceSquared(currLoc, RobotType.MINER.visionRadiusSquared) &&
                enemyAttackable.length == 0;
        }
        return true;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Comms.incrementMinerCounter();
        closestCluster = Comms.getClosestCluster(currLoc);
        enemyAttackable = getEnemyAttackable();
        friendlyAttackable = getFriendlyAttackable();

        // Try to mine on squares around us.
        boolean amMining = false;
        int[] actionRadiusArr = findLeadAndGold();
        Direction DirectionAway = currLoc.directionTo(currLoc.translate((int)overallDX, (int)overallDY)).opposite();

        // Go through all deposits in action radius, and mine as much as possible (consider the case where two lead ores
        // within action radius have sizes 3 and 4. We want to mine them both down to 1 in the same turn
        // Mine all gold first
        MapLocation source;
        if(bestGold != null) {
            int displayedGoldScore = (int)Util.clip(bestGoldScore, -100, 100);
            Debug.printString("Au " + displayedGoldScore + ": " + bestGold.toString());
            for(int x = 0; x < 3 && rc.isActionReady(); x ++) {
                for (int y = 0; y < 3 && rc.isActionReady(); y++) {
                    source = currLoc.translate(x - 1, y - 1);
                    while(rc.canMineGold(source)) {
                        rc.mineGold(source);
                        amMining = true;
                    }
                }
            }
        }

        if(bestLead != null) {
            int displayedLeadScore = (int)Util.clip(bestLeadScore, -100, 100);
            Debug.printString("Pb " + displayedLeadScore + ": " + bestLead.toString());
            for(int x = 0; x < 3 && rc.isActionReady(); x ++) {
                for (int y = 0; y < 3 && rc.isActionReady(); y++) {
                    source = currLoc.translate(x - 1, y - 1);
                    if(actionRadiusArr[3 * x + y] > 1) {
                        while(rc.canMineLead(source) && actionRadiusArr[3 * x + y] != 1) {
                            rc.mineLead(source);
                            actionRadiusArr[3 * x + y]--;
                            amMining = true;
                        }
                    }
                }
            }
        }

        if(numUnitLeadLocs > 0 && shouldDepleteUnitLead()) {
            Debug.printString("Depleting unit lead");
            for(int i = 0; i < numUnitLeadLocs && rc.canMineLead(unitLeadLocs[i]); i++) {
                rc.mineLead(unitLeadLocs[i]);
            }
        }

        MapLocation target = null;
        String str = "";
        boolean canMine = (bestLead != null || bestGold != null) &&
                            (rc.senseNearbyLocationsWithLead(2, 2).length > 0 ||
                            rc.senseNearbyLocationsWithGold(2, 1).length > 0);
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
            str = "To Pb";
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

        if(bestGold != null && bestGoldScore >= bestLeadScore) {
            target = Nav.getBestRubbleSquareAdjacentTo(bestGold);
            if(currLoc.isAdjacentTo(bestGold) && rc.senseRubble(currLoc) == rc.senseRubble(target)) target = currLoc;
            str = "To Au";
        }

        RobotInfo closestEnemy = getClosestRobot(enemyAttackable);
        RobotInfo closestFriendly = getClosestRobot(enemyAttackable);
        // Run away if either
        // - You see fewer friendly attackers than enemy attackers
        // - the closest enemy is closer than the clsoest friendly
        if(closestEnemy != null) {
            if(enemyAttackable.length + 5 >= friendlyAttackable.length) {
                target = Nav.getGreedyTargetAway(closestEnemy.getLocation());
                str = "Enemies++ " + closestEnemy.type;
            }

            if(closestFriendly != null &&
                currLoc.distanceSquaredTo(closestEnemy.location) <
                currLoc.distanceSquaredTo(closestFriendly.location)) {
                target = Nav.getGreedyTargetAway(closestEnemy.getLocation());
                str = "Close enemy " + closestEnemy.type;
            }
        }

        Debug.printString(str);
        Nav.move(target);
    }
}
