package MPTempName;

import battlecode.common.*;
import MPTempName.Debug.*;
import MPTempName.Util.*;
import MPTempName.Comms.*;

public class Soldier extends Robot {
    static enum SoldierState {
        DEFENSE,
        RUSHING,
        DONE_RUSHING,
        EXPLORING,
        HELPING,
    }
    static SoldierState currState;
    static int homeFlagIdx;
    static MapLocation target;
    static int targetId;
    static MapLocation distressLocation;

    static int numEnemySoldiers;
    static int overallEnemySoldierDx;
    static int overallEnemySoldierDy;

    static int numFriendlySoldiers;
    static int overallFriendlySoldierDx;
    static int overallFriendlySoldierDy;
    static MapLocation avgEnemyLoc;
    static RobotInfo closestEnemy;
    static int numFriendlies;
    static int numEnemies;
    static MapLocation closestAttackingEnemy;
    static int numEnemySoldiersAttackingUs;

    public Soldier(RobotController r) throws GameActionException {
        this(r, Comms.firstArchonFlag);
    }

    public Soldier(RobotController r, int homeFlagIndex) throws GameActionException {
        super(r);
        currState = SoldierState.EXPLORING;
        homeFlagIdx = homeFlagIndex;
    } 

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        closestEnemy = getBestEnemy(EnemySensable);
        resetShouldRunAway();
        avgEnemyLoc = Comms.getClosestCluster(currLoc);
        Comms.incrementSoldierCounter();
        if (!tryMoveTowardsEnemy()) { 
            soldierExplore();
        }
    }

    public void resetShouldRunAway() throws GameActionException {
        numEnemySoldiersAttackingUs = 0;
        numFriendlies = 0;
        closestAttackingEnemy = null;
        numEnemies = 0;
        overallEnemySoldierDx = 0;
        overallEnemySoldierDy = 0;
        int closestSoldierDist = Integer.MAX_VALUE;
        for (RobotInfo bot : EnemySensable) {
            MapLocation candidateLoc = bot.getLocation();
            int candidateDist = currLoc.distanceSquaredTo(candidateLoc);
            boolean isWatchtower = bot.getType() == RobotType.WATCHTOWER;
            if (bot.getType() == RobotType.SOLDIER || isWatchtower) {
                // This code makes it so that we don't consider Portables as attacking enemies - it made our bot worse as of Hooray for spring
                // so it is commented out for now
                /*boolean canAttack = false;
                if(!isWatchtower || bot.getMode() == RobotMode.TURRET) {
                    canAttack = true;
                }
                if(canAttack) {numEnemies++;}*/
                numEnemies++;
                if (candidateDist <= actionRadiusSquared /*&& canAttack*/) {
                    numEnemySoldiersAttackingUs++;
                    overallEnemySoldierDx += currLoc.directionTo(candidateLoc).dx * (100 / (currLoc.distanceSquaredTo(candidateLoc)));
                    overallEnemySoldierDy += currLoc.directionTo(candidateLoc).dy * (100 / (currLoc.distanceSquaredTo(candidateLoc)));
                }
                if (candidateDist < closestSoldierDist) {
                    closestSoldierDist = candidateDist;
                    closestAttackingEnemy = candidateLoc;
                }
            }
        }
        MapLocation closestEnemyLocation = currLoc;
        if(closestAttackingEnemy != null) {
            closestEnemyLocation = closestAttackingEnemy;
        }
        for (RobotInfo Fbot : FriendlySensable) {
            RobotType FbotType = Fbot.getType();
            if (FbotType == RobotType.SOLDIER || FbotType == RobotType.WATCHTOWER) {
                MapLocation FbotLocation = Fbot.getLocation();
                // Debug.printString(" " + FbotLocation + " ");
                if((FbotLocation).distanceSquaredTo(closestEnemyLocation) <= FbotType.visionRadiusSquared) {
                    numFriendlies++;
                }
            }
        }
    }

    public boolean shouldRunAway() {
        //Not only should there be no soldiers attacking us, but if we see 2 soldiers between our action radius and our vision radius, we should not go forward
        //Consider changing the numFriendlies < numEnemies to <= and retesting
        // Debug.printString("enemyAction: " + numEnemySoldiersAttackingUs + "enemy: " + numEnemies + "friends: " + numFriendlies);
        return numEnemySoldiersAttackingUs > 0 || (numFriendlies + 1 < numEnemies);
    }
    public void moveAndAttack(Direction[] targetDirs, boolean attackFirst) throws GameActionException{
        if(attackFirst) {
            tryAttackBestEnemy();
            tryMoveDest(targetDirs);
        }
        else {
            tryMoveDest(targetDirs);
            tryAttackBestEnemy();
        }
    }
    public boolean tryMoveTowardsEnemy() throws GameActionException {
        // move towards it if found
        boolean alreadyCalculated = false;
        if (closestEnemy != null) {
            MapLocation dest;
            Direction dir = null;
            boolean attackFirst = false;
            if(shouldRunAway()) {
                attackFirst = true;
                // Positive so that we move towards the point mass.
                dest = currLoc.translate(-overallEnemySoldierDx, -overallEnemySoldierDy);//(overallFriendlySoldierDx, overallFriendlySoldierDy);
                dir = Nav.navTo(dest);
                alreadyCalculated = true;
                MapLocation targetLoc = currLoc.add(dir);
                int locRubble = Util.getRubble(targetLoc);
                int currRubble = rc.senseRubble(currLoc);
                if(rc.onTheMap(targetLoc) && locRubble > (20 + 1.2 * currRubble)) {
                    Debug.printString("moving Away bad");
                    return true;
                }
                Debug.printString("RA, Dest: " + dir);
            } else {
                dest = closestEnemy.getLocation();
                attackFirst = false;
                // Debug.printString("Going towards closest Enemy");
            }
            if(dest != null) {
                if(!alreadyCalculated) {
                    dir = Nav.navTo(dest);
                }
                //Don't go towards miners if it forces us to go to low passability squares(the formula I used is kind of arbitrary, so its definitely tweakable)
                //keep in mind, however, that on Intersection its like passability 1 versus 85 so any formula thats halfway decent will work there
                MapLocation targetLoc = currLoc.add(dir);
                RobotType closestEnemyType = closestEnemy.getType();
                if(closestEnemyType == RobotType.MINER || closestEnemyType == RobotType.ARCHON || closestEnemyType == RobotType.BUILDER) {
                    //rubble check
                    int locRubble = Util.getRubble(targetLoc);
                    int currRubble = rc.senseRubble(currLoc);
                    if(rc.onTheMap(targetLoc) && locRubble > (20 + 1.2 * currRubble) && closestEnemyType!= RobotType.ARCHON) {
                        Debug.printString("rub high");
                        tryAttackBestEnemy();
                        return true;
                    }
                    //We're already close to a non-attacking enemy, and moving would put us in lower passability
                    int distanceNeeded = 5;
                    //This can be changed if Archons start running away from us
                    if(closestEnemyType == RobotType.ARCHON) {distanceNeeded = actionRadiusSquared;}
                    if(currLoc.distanceSquaredTo(dest) <= distanceNeeded && locRubble > currRubble) {
                        Debug.printString("close");
                        tryAttackBestEnemy();
                        return true;
                    }
                    Debug.printString("enemyfren");
                    Direction[] targetDirs = Util.getInOrderDirections(dir);
                    moveAndAttack(targetDirs, attackFirst);
                    return true;
                }
                else {
                    int destRubble = Util.getRubble(dest);
                    int targetLocRubble = Util.getRubble(currLoc.add(dir));
                    int currRubble = Util.getRubble(currLoc);
                    if(currRubble > 1.2 * destRubble || 1.2 * currRubble + 15 < targetLocRubble) {
                        Debug.printString("not worth atak");
                        tryAttackBestEnemy();
                        return true;
                    }
                    Debug.printString("Atak");
                    Direction[] targetDirs = Util.getInOrderDirections(dir);
                    moveAndAttack(targetDirs, attackFirst);
                    return true;
                }
            }
        }
        return false;
    }


    public void soldierExplore() throws GameActionException {
        MapLocation target;
        if (avgEnemyLoc != null && currLoc.distanceSquaredTo(avgEnemyLoc) > visionRadiusSquared) {
            target = avgEnemyLoc;
            Debug.printString("Avg enemy: " + target.toString());
        } else {
            target = Explore.getLegacyExploreTarget();
            Debug.printString("Exploring: " + target.toString());
        }
        Nav.move(target);
    }
}
