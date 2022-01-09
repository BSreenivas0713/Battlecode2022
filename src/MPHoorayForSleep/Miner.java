package MPHoorayForSleep;

import battlecode.common.*;
import MPHoorayForSleep.Debug.*;
import MPHoorayForSleep.Util.*;

public class Miner extends Robot {
    static int roundNumBorn;
    static int minerCount;
    static boolean explorer;
    static MapLocation unitLeadLoc;
    static MapLocation closestLead;
    static MapLocation goldSource;
    static float overallDX;
    static float overallDY;

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

    //Update the location of the closest lead ore, return a list of all lead ores within action radius of Miner
    public int[] findLeadAndGold() throws GameActionException {
        goldSource = null;
        overallDX = 0;
        overallDY = 0;
// find the best lead source, prioritizing lead that is within your action radius
        MapLocation[] locs = rc.senseNearbyLocationsWithLead(visionRadiusSquared);
        MapLocation loc;
        unitLeadLoc = null;
        int[] actionRadiusArr = new int[9];
        for (int x = 0; x < 3; x ++) {for (int y = 0; y < 3; y ++ ) {actionRadiusArr[3 * x + y] = 0;}}
        closestLead = null;
        minerCount = 0;
        int distToClosestLead = Integer.MAX_VALUE;
        for(int i = locs.length - 1; i >= 0; i--) {
            loc = locs[i];
            int leadAmount = rc.senseLead(loc);
            if (leadAmount > 1){
                int currDist = currLoc.distanceSquaredTo(loc);
                if(currDist < distToClosestLead) {
                    distToClosestLead = currDist;
                    closestLead = loc;
                    
                }
                if (currDist <= actionRadiusSquared) {
                    actionRadiusArr[(1 + (loc.x - currLoc.x)) * 3  + (1 + (loc.y - currLoc.y))] = leadAmount;
                }
            } else {
                unitLeadLoc = loc;
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
            }
        }
        return actionRadiusArr;
    }

    // Deplete unit lead sources if far away from home and more enemies than friends
    public boolean shouldDepleteUnitLead() throws GameActionException {
        return EnemySensable.length > FriendlySensable.length &&
            !currLoc.isWithinDistanceSquared(home, Util.MIN_DIST_TO_DEPLETE_UNIT_LEAD);
    }
    public String bigArrToString(int[] bigArr) {
        return "[ " + "[ " + bigArr[0] + " " + bigArr[1] + " " + bigArr[2] + " ]," + "[ " + bigArr[3] + " " + bigArr[4] + " "  + bigArr[5] + " ]," + "[ "+  bigArr[6] + " " + bigArr[7] + " "  + bigArr[8] + " ]," + "]";
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
        if(closestLead != null) {
            // Debug.printString("Lead found: " + closestLead.toString());
            Debug.printString(bigArrToString(actionRadiusArr));
            if(unitLeadLoc != null && shouldDepleteUnitLead() && rc.canMineLead(unitLeadLoc)) {
                Debug.printString("Depleting unit lead");
                rc.mineLead(unitLeadLoc);
            }
            boolean done = false;
            int numTimesMined = 0;
            //Go through all lead deposits in action radius, and mine as much as possible (consider the case where two lead ores
            //within action radius have sizes 3 and 4. We want to mine them both down to 1 in the same turn 
            for(int x = 0; x < 3; x ++) {
                for (int y = 0; y < 3; y++) {
                    if(actionRadiusArr[3 *x + y] > 1) {
                        MapLocation leadSource = currLoc.translate(x - 1, y - 1);
                        int supposedLeadValue = actionRadiusArr[3 * x + y];
                        while(rc.canMineLead(leadSource) && supposedLeadValue != 1) {
                            rc.mineLead(leadSource);
                            numTimesMined++;
                            supposedLeadValue--;
                            // if(rc.getRoundNum() < 20) {
                            //     System.out.println(Integer.toString(numTimesMined) + " " + Integer.toString(supposedLeadValue) + " " + Integer.toString(x) + " " + Integer.toString(y) + " ");
                            // }
                            amMining = true;
                        }
                        if(numTimesMined == 5) {
                            done = true;
                        }
                    }
                    if(done) {break;}
                }
                if(done) {break;}
            }
            
        }
        Direction[] dir = {};
        String str = "";
        if (!amMining) {
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
        //no need to complicate things, just go towards the closest ore
        if(closestLead != null) {
            dir = Nav.greedyDirection(currLoc.directionTo(closestLead));
            str = "going towards lead at" + closestLead.toString();
            //Consider getting rid of this minerCount if statment and retesting
            if(minerCount >= 4) {
                dir = Nav.greedyDirection(DirectionAway);
                str = "going away from other miners: " + DirectionAway.toString();
            }
        }

        RobotInfo closestEnemy = getClosestEnemy(RobotType.SOLDIER);
        if(closestEnemy != null) {
            dir = Nav.greedyDirection(currLoc.directionTo(closestEnemy.getLocation()).opposite());
            str = "going away from enemy";
        }
        closestEnemy = getClosestEnemy(RobotType.WATCHTOWER);
        if(closestEnemy != null) {
            dir = Nav.greedyDirection(currLoc.directionTo(closestEnemy.getLocation()).opposite());
            str = "going away from enemy";
        }
        
        if(goldSource != null) {
            dir = Nav.greedyDirection(currLoc.directionTo(goldSource));
            str = "going toward gold";
        }

        // Debug.printString(str);
        tryMoveDest(dir);
    }
}
