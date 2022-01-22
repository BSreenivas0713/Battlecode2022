package MPTempName;

import battlecode.common.*;
import MPTempName.Debug.*;
import MPTempName.Util.*;
import MPTempName.fast.FastIterableLocSet;


public class Builder extends Robot{
    static enum BuilderState {
        REPAIRING,
        NORMAL,
        GOING_TO_HEAL,
    }

    static BuilderState currState;
    static boolean repairing;
    static boolean making;
    static int startRound;

    static RobotInfo maybePrototype;
    static RobotInfo repairTarget;
    static MapLocation healTarget;

    public Builder(RobotController r) throws GameActionException {
        super(r);
        startRound = rc.getRoundNum();
        currState = BuilderState.NORMAL;
    }

    public boolean enemyNear() throws GameActionException {
        for (RobotInfo robot: EnemySensable) {
            RobotType robottype = robot.getType();
            if( robottype != RobotType.MINER && robottype != RobotType.BUILDER && robottype != RobotType.LABORATORY) {
                return true;
            }
        }
        return false;
    }

    public boolean makeLabIfPossible() throws GameActionException {
        if(!Comms.haveBuiltLab()) {
            int bestRubble = Integer.MAX_VALUE;
            Direction bestDir = null;
            for(Direction Dir: Util.getFullInOrderDirections(currLoc.directionTo(home).opposite())) {
                int currRubble = Util.getRubble(currLoc.add(Dir));
                if (rc.canBuildRobot(RobotType.LABORATORY, Dir) && currRubble < bestRubble) {
                    bestRubble = currRubble;
                    bestDir = Dir;
                }
            }
            if (bestDir != null) {
                making = true;
                rc.buildRobot(RobotType.LABORATORY, bestDir);
                maybePrototype = rc.senseRobotAtLocation(rc.getLocation().add(bestDir));
                Comms.signalLabStillAlive();
            }
            return true;
        }
        return false;
    }

    public boolean makeWatchtowerIfPossible() throws GameActionException{
        boolean seenFriendly = false;
        RobotInfo robot;
        int archonTowerCount = 0;
        for (RobotInfo Friend: FriendlySensable) {
            if(Friend.type == RobotType.ARCHON || Friend.type == RobotType.WATCHTOWER) {
                archonTowerCount++;
            }
        }
        int[] ArchonOrder = Comms.getArchonOrderGivenClusters();
        int numImportantArchons = ArchonOrder[4];
        for (int i = FriendlySensable.length - 1; i >= 0; i--) {
            robot = FriendlySensable[i];
            switch(robot.type) {
                case ARCHON:
                case WATCHTOWER:
                    if(currLoc.distanceSquaredTo(robot.location) <= 8) {
                        seenFriendly = true;
                    }
                    MapLocation robotLoc = robot.location;
                    for(MapLocation newLoc: Util.makePattern(robotLoc)) {
                        if(!(Comms.haveBuiltBuilderForFinalLab() &&
                            !Comms.haveBuiltLab()) &&
                            robot.mode == RobotMode.TURRET &&
                            archonTowerCount < 13 &&
                            currLoc.distanceSquaredTo(newLoc) <= 2 &&
                            rc.canBuildRobot(RobotType.WATCHTOWER, currLoc.directionTo(newLoc)) &&
                            rc.getTeamLeadAmount(team) >= 75 * numImportantArchons + 150) {
                            Debug.printString("Building a Watchtower");
                            making = true;
                            rc.buildRobot(RobotType.WATCHTOWER, currLoc.directionTo(newLoc));
                            maybePrototype = rc.senseRobotAtLocation(newLoc);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return seenFriendly;
    }

    public void repairIfPossible() throws GameActionException{
        maybePrototype = null;
        for(RobotInfo robot: FriendlySensable) {
            if(currLoc.distanceSquaredTo(robot.location) <= actionRadiusSquared) {
                switch (robot.type) {
                    case WATCHTOWER:
                    case LABORATORY:
                        if(robot.health < robot.getType().getMaxHealth(robot.level)) {
                            if(rc.canRepair(robot.location)) {
                                rc.repair(robot.location);
                            }
                            repairing = true;
                            Debug.printString("Repairing " + robot.location.toString() + ", Health " + robot.health + "/" + robot.getType().getMaxHealth(robot.level));
                        }
                        // if(rc.canMutate(robot.location)) {
                        //     rc.mutate(robot.location);
                        // }
                        if(robot.mode == RobotMode.PROTOTYPE) {
                            maybePrototype = robot;
                        }
                        break;
                    case ARCHON:
                        if (robot.health < RobotType.ARCHON.getMaxHealth(robot.level)) {
                            if (rc.canRepair(robot.location)) {
                                moveToLowerRubble(currLoc, currLoc.directionTo(robot.location));
                                rc.repair(robot.location);
                                repairing = true;
                                Debug.printString("Repairing Archon");
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public boolean runFromEnemy() throws GameActionException {
        MapLocation target = null;
        RobotInfo closestEnemy = getClosestEnemy(RobotType.SOLDIER);
        if(closestEnemy != null) {
            target = Nav.getGreedyTargetAway(closestEnemy.getLocation());
        }
        closestEnemy = getClosestEnemy(RobotType.WATCHTOWER);
        if(closestEnemy != null) {
            target = Nav.getGreedyTargetAway(closestEnemy.getLocation());
        }
        closestEnemy = getClosestEnemy(RobotType.SAGE);
        if(closestEnemy != null) {
            target = Nav.getGreedyTargetAway(closestEnemy.getLocation());
        }
        if(target != null) {
            Nav.move(target);
            return true;
        }
        else {
            return false;
        }
    }

    // Updates home to the closest archon
    public void updateHome() throws GameActionException {
        MapLocation closestArchonToHome = null;
        int minDist = Integer.MAX_VALUE;
        for(MapLocation archonLoc : archonLocations) {
            if(archonLoc == null) continue;
            if(archonLoc.isWithinDistanceSquared(home, minDist)) {
                closestArchonToHome = archonLoc;
                minDist = archonLoc.distanceSquaredTo(home);
            }
        }
        home = closestArchonToHome;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Comms.incrementBuilderCount();
        loadArchonLocations();
        updateHome();
        repairing = false;
        making = false;
        makeLabIfPossible();
        boolean seenFriendly = makeWatchtowerIfPossible();

        trySwitchState();
        doStateAction();
    }

    public void trySwitchState() throws GameActionException {
        switch(currState) {
            case NORMAL:
                if(making && maybePrototype != null) {
                    currState = BuilderState.REPAIRING;
                    repairTarget = maybePrototype;
                } else if(Comms.haveBuiltLab() && checkNeedHelp()) {
                    currState = BuilderState.GOING_TO_HEAL;
                }
                break;
            case REPAIRING:
                if(repairTarget == null || !rc.canSenseRobot(repairTarget.ID)) {
                    currState = BuilderState.NORMAL;
                } else {
                    RobotInfo robot = rc.senseRobot(repairTarget.ID);
                    if(robot.health >= robot.type.getMaxHealth(robot.level)) {
                        currState = BuilderState.NORMAL;
                    }
                }
                break;
            case GOING_TO_HEAL:
                if(!checkNeedHelp()) {
                    currState = BuilderState.NORMAL;
                } else if(currLoc.isWithinDistanceSquared(healTarget, RobotType.BUILDER.actionRadiusSquared)) {
                    currState = BuilderState.REPAIRING;
                    repairTarget = null;
                    if(rc.canSenseLocation(healTarget)) {
                        RobotInfo robot = rc.senseRobotAtLocation(healTarget);
                        switch(robot.type) {
                            case ARCHON:
                                if(robot.health < robot.getType().getMaxHealth(robot.level)) {
                                    repairTarget = robot;
                                    healTarget = null;
                                    home = robot.location;
                                }
                        }
                    }

                    if(repairTarget == null) {
                        loadRepairTarget();
                        if(repairTarget == null) {
                            currState = BuilderState.NORMAL;
                        } else {
                            currState = BuilderState.REPAIRING;
                            home = repairTarget.location;
                        }
                    }
                }
                break;
        }
    }

    // This should really only ever load an archon
    public void loadRepairTarget() throws GameActionException {
        for(RobotInfo robot: FriendlySensable) {
            switch (robot.type) {
                case WATCHTOWER:
                case LABORATORY:
                    if(robot.health < robot.getType().getMaxHealth(robot.level)) {
                        repairTarget = robot;
                    }
                    break;
                case ARCHON:
                    if (robot.health < RobotType.ARCHON.getMaxHealth(robot.level)) {
                        repairTarget = robot;
                        break;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    // Checks if any archons currently need help and goes to the closest
    public boolean checkNeedHelp() throws GameActionException {
        int numArchons = Comms.friendlyArchonCount();
        MapLocation archonLoc;
        int minDist = Integer.MAX_VALUE;
        healTarget = null;
        Debug.printString("Checking for help");

        for(int i = 0; i < numArchons; i++) {
            archonLoc = archonLocations[i];
            if(archonLoc == null) continue;
            if(Comms.getArchonNeedsHeal(i) && archonLoc.isWithinDistanceSquared(currLoc, minDist)) {
                Debug.printString("" + archonLoc);
                healTarget = archonLoc;
                minDist = archonLoc.distanceSquaredTo(currLoc);
            }
        }

        return healTarget != null;
    }

    public boolean existsProto() throws GameActionException {
        return maybePrototype != null;
    }

    public void doStateAction() throws GameActionException {
        switch(currState) {
            case NORMAL:
                Debug.printString("Normal");
                doNormalAction();
                break;
            case REPAIRING:
                Debug.printString("Repairing");
                repairTarget = rc.senseRobot(repairTarget.ID);
                MapLocation target = Nav.getBestRubbleSquareAdjacentTo(repairTarget.getLocation());
                Nav.move(target);
                if(rc.canRepair(repairTarget.getLocation())) {
                    rc.repair(repairTarget.getLocation());
                }
                break;
            case GOING_TO_HEAL:
                Debug.printString("Going to repair");
                Nav.move(healTarget);
                break;
        }
    }

    public void doNormalAction() throws GameActionException {
        repairIfPossible();
        if(!repairing && !making) {
            if(!Comms.haveBuiltLab()) {
                if(currLoc.distanceSquaredTo(home) <= robotType.ARCHON.visionRadiusSquared) {
                    MapLocation avgEnemyLoc = home;
                    MapLocation betterEnemyLoc = Comms.getClosestCluster(currLoc);
                    if(betterEnemyLoc != null) {
                        avgEnemyLoc = betterEnemyLoc;
                    }
                    int currRubble = rc.senseRubble(currLoc);
                    Direction bestDir = null;
                    int bestRubble = Integer.MAX_VALUE;
                    Debug.printString("" + currLoc.directionTo(avgEnemyLoc).opposite());
                    for(Direction dir: Util.getInOrderDirections(currLoc.directionTo(avgEnemyLoc).opposite())) {
                        MapLocation newLoc = currLoc.add(dir);
                        int newRubble = Util.getRubble(newLoc);
                        if (rc.canMove(dir) && newRubble <= 5 + currRubble && newRubble < bestRubble && newLoc.distanceSquaredTo(home) <= robotType.ARCHON.visionRadiusSquared) {
                            bestDir = dir;
                            bestRubble = newRubble;
                        }
                    }
                    if(bestDir != null) {
                        tryMoveDest(Util.getInOrderDirections(bestDir));
                    }
                }
            } else if(!runFromEnemy()) {
                if(currLoc.isWithinDistanceSquared(home, 2)) {
                    Debug.printString("Moving away from home");
                    Direction awayFromHome = currLoc.directionTo(home).opposite();
                    if(!rc.onTheMap(currLoc.add(awayFromHome))) {
                        Nav.move(currLoc.add(Util.turnRight90(currLoc.directionTo(home))));
                    } else {
                        Nav.move(currLoc.add(awayFromHome));
                    }
                }
            }
        }
    }

    public void moveToLowerRubble(MapLocation moveFrom, Direction dir) throws GameActionException {
        int myRubble = Util.getRubble(moveFrom);
        Direction left = dir.rotateLeft();
        Direction right = dir.rotateRight();
        int rubble0 = Util.getRubble(moveFrom.add(dir));
        if (rubble0 <= myRubble && rc.canMove(dir)) {
            rc.move(dir);
            return;
        }
        int rubble1 = Util.getRubble(moveFrom.add(left));
        int rubble2 = Util.getRubble(moveFrom.add(right));
        if (rubble1 < myRubble) {
            if (rubble1 < rubble2) {
                if (rc.canMove(left)) {
                    rc.move(left);
                } else if (rubble2 < myRubble && rc.canMove(right)) {
                    rc.move(right);
                }
            } else {
                if (rc.canMove(right)) {
                    rc.move(right);
                } else if (rc.canMove(left)) {
                    rc.move(left);
                }
            }
        } else if (rubble2 < myRubble && rc.canMove(right)) {
            rc.move(right);
        }
    }
}
