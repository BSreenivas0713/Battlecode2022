package MPTempName;

import battlecode.common.*;
import MPTempName.bfs.*;

public class Nav {
    static RobotController rc;

    static MapLocation lastCurrLoc;
    static MapLocation currentTarget;
    static int closestDistanceToDest;
    static int turnsSinceClosestDistanceDecreased;
    static int turnsGreedy;

    static final int BYTECODE_REMAINING = 1500;
    static final int BYTECODE_REMAINING_NON_MINER_BUILDER = 2000;
    //static final int BYTECODE_BFS = 5000;
    static final int GREEDY_TURNS = 4;

    static void init(RobotController r) {
        rc = r;
        BFSUnrolled29.init(rc);
        BFSUnrolled20.init(rc);
        BFSUnrolled18.init(rc);
        BFSUnrolled13.init(rc);
        MapTracker.reset();
        Pathfinding.init(rc);
        closestDistanceToDest = Integer.MAX_VALUE;
        turnsSinceClosestDistanceDecreased = 0;
    }

    // @requires loc is adjacent to currLoc
    public static Direction[] getDirsToAdjSquares(MapLocation loc) {
        switch(rc.getLocation().directionTo(loc)) {
            case SOUTH:
                return new Direction[]{Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.SOUTHEAST, Direction.SOUTHWEST};
            case NORTH:
                return new Direction[]{Direction.NORTH, Direction.EAST, Direction.WEST, Direction.NORTHEAST, Direction.NORTHWEST};
            case EAST:
                return new Direction[]{Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.NORTHEAST, Direction.SOUTHEAST};
            case WEST:
                return new Direction[]{Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.NORTHWEST, Direction.SOUTHWEST};
            case NORTHEAST:
                return new Direction[]{Direction.NORTHEAST, Direction.NORTH, Direction.EAST};
            case SOUTHEAST:
                return new Direction[]{Direction.SOUTHEAST, Direction.SOUTH, Direction.EAST};
            case SOUTHWEST:
                return new Direction[]{Direction.SOUTHWEST, Direction.SOUTH, Direction.WEST};
            case NORTHWEST:
                return new Direction[]{Direction.NORTHWEST, Direction.NORTH, Direction.WEST};
            default:
                return new Direction[]{};
        }
    }

    public static MapLocation getBestRubbleSquareAdjacentTo(MapLocation dest) throws GameActionException {
        int minRubble = Integer.MAX_VALUE;
        MapLocation bestLoc = null;
        MapLocation loc;
        int rubble;
        // This is a bug but it does worse??? I hate life.
        // Maybe because it biases towards a square, 
        // try breaking ties based on distance
        // for(int i = Util.directions.length; --i >= 0;) {
        for(int i = Util.directions.length - 1; --i >= 0;) {
            loc = dest.add(Util.directions[i]);
            if(rc.canSenseLocation(loc)) {
                rubble = rc.senseRubble(loc);
                if(rubble < minRubble) {
                    minRubble = rubble;
                    bestLoc = loc;
                }
            }
        }

        // Debug.setIndicatorDot(Debug.INDICATORS, bestLoc, 51, 204, 255);
        return bestLoc;
    }
    
    public static Direction getBestDirBetterRubbleSquareAdjacentTo(MapLocation dest) throws GameActionException {
        MapLocation bestLoc = getBestRubbleSquareAdjacentTo(dest);
        if(bestLoc.equals(rc.getLocation())) {
            return Direction.CENTER;
        }
        Direction dir = Nav.getBestDir(bestLoc);
        return dir == null ? Direction.CENTER : dir;
    }

    static Direction navTo(MapLocation dest) throws GameActionException {
        if(!rc.isMovementReady())
            return Direction.CENTER;

        MapLocation currLoc = rc.getLocation();

        if(currLoc.isAdjacentTo(dest))
            return getBestDirBetterRubbleSquareAdjacentTo(dest);

        if(!dest.equals(currentTarget)) {
            currentTarget = dest;
            closestDistanceToDest = currLoc.distanceSquaredTo(dest);
            turnsSinceClosestDistanceDecreased = 0;
        }

        int dist = currLoc.distanceSquaredTo(dest);
        if(dist < closestDistanceToDest) {
            closestDistanceToDest = dist;
            turnsSinceClosestDistanceDecreased = 0;
        } else {
            turnsSinceClosestDistanceDecreased++;
        }
        
        if(turnsSinceClosestDistanceDecreased >= 3) {
            Debug.println(Debug.PATHFINDING, "BFS failed to get closer in two turns: Falling back to directionTo");
            return currLoc.directionTo(dest);
        } else {
            Debug.println(Debug.PATHFINDING, "Doing BFS normally");
        }
        Direction dir = Nav.getBestDir(dest);
        return dir == null ? Util.getFirstValidInOrderDirection(currLoc.directionTo(dest)) : dir;
    }

    public static Direction getBestDir(MapLocation dest) throws GameActionException {
        return getBestDir(dest, 0);
    }

    public static Direction getBestDir(MapLocation dest, int bytecodeCushion) throws GameActionException {
        int bcLeft = Clock.getBytecodesLeft();
        if(bcLeft >= BFSUnrolled29.MIN_BC_TO_USE + bytecodeCushion && rc.getType().visionRadiusSquared >= 29) {
            return BFSUnrolled29.getBestDir(dest);
        } else if(bcLeft >= BFSUnrolled20.MIN_BC_TO_USE + bytecodeCushion) {
            return BFSUnrolled20.getBestDir(dest);
        } else if(bcLeft >= BFSUnrolled18.MIN_BC_TO_USE + bytecodeCushion) {
            return BFSUnrolled18.getBestDir(dest);
        } else if(bcLeft >= BFSUnrolled13.MIN_BC_TO_USE + bytecodeCushion) {
            return BFSUnrolled13.getBestDir(dest);
        } else {
            return getGreedyDirection(rc.getLocation().directionTo(dest));
        }
    }


    public static Direction getGreedyDirection(Direction dir) throws GameActionException {
        Direction[] bestDirs = greedyDirection(dir);
        if(bestDirs.length > 0) {
            return bestDirs[0];
        } else {
            return Util.getFirstValidInOrderDirection(dir);
        }
    }
    
    public static Direction[] greedyDirection(Direction dir) throws GameActionException {
        Direction left = dir.rotateLeft();
        Direction right = dir.rotateRight();

        MapLocation currLoc = rc.getLocation();
        MapLocation loc = currLoc.add(dir);
        MapLocation leftLoc = currLoc.add(left);
        MapLocation rightLoc = currLoc.add(right);

        double dirRubble = 100;
        double leftRubble = 100;
        double rightRubble = 100;

        int numToInsert = 0;
        if(rc.canMove(dir)) {
            numToInsert++;
            dirRubble = rc.senseRubble(loc);
        }
        if(rc.canMove(left)) {
            numToInsert++;
            leftRubble = rc.senseRubble(leftLoc);
        }
        if(rc.canMove(right)) {
            numToInsert++;
            rightRubble = rc.senseRubble(rightLoc);
        }

        // Hard coded 3 length array sort lol
        Direction[] orderedDirs = new Direction[3];
        if(dirRubble <= leftRubble && leftRubble <= rightRubble) {
            orderedDirs[0] = dir; orderedDirs[1] = left; orderedDirs[2] = right;
        } else if(dirRubble <= rightRubble && rightRubble <= leftRubble) {
            orderedDirs[0] = dir; orderedDirs[1] = right; orderedDirs[2] = left;
        } else if(rightRubble <= dirRubble && dirRubble <= leftRubble) {
            orderedDirs[0] = right; orderedDirs[1] = dir; orderedDirs[2] = left;
        } else if(rightRubble <= leftRubble && leftRubble <= dirRubble) {
            orderedDirs[0] = right; orderedDirs[1] = left; orderedDirs[2] = dir;
        } else if(leftRubble <= dirRubble && dirRubble <= rightRubble) {
            orderedDirs[0] = left; orderedDirs[1] = dir; orderedDirs[2] = right;
        } else if(leftRubble <= rightRubble && rightRubble <= dirRubble) {
            orderedDirs[0] = left; orderedDirs[1] = right; orderedDirs[2] = dir;
        }

        Direction[] dirs = new Direction[numToInsert];
        System.arraycopy(orderedDirs, 0, dirs, 0, numToInsert);
        return dirs;
    }

    static MapLocation getGreedyTargetAway(MapLocation loc) throws GameActionException {
        Direction[] dirs = greedyDirection(rc.getLocation().directionTo(loc).opposite());
        return rc.getLocation().add(Util.getFirstMoveableDir(dirs));
    }

    static void reset(){
        turnsGreedy = 0;
        MapTracker.reset();
    }

    static void update(MapLocation target){
        if (currentTarget == null || target.distanceSquaredTo(currentTarget) > 0){
            reset();
        } else --turnsGreedy;
        currentTarget = target;
        MapTracker.add(rc.getLocation());
    }

    static void activateGreedy() {
        turnsGreedy = GREEDY_TURNS;
    }

    static void initTurn() {
        Pathfinding.initTurn();
    }

    static void tryMoveSafely(MapLocation target) throws GameActionException{
        Debug.printString("Saf mov");
        MapLocation currLoc = rc.getLocation();

        Direction correctDir = currLoc.directionTo(target);
        Direction[] importantDirs = Util.getInOrderDirections(correctDir);

        int currRubble = rc.senseRubble(currLoc);
        int currBestDist = Integer.MAX_VALUE;
        int currBestRubble = Integer.MAX_VALUE;
        Direction currBestDirection = Direction.CENTER;

        for(Direction possibleDir: importantDirs) {
            MapLocation targetLoc = currLoc.add(possibleDir);
            int targetLocDist = currLoc.distanceSquaredTo(targetLoc);
            int nextRubble = Util.getRubble(targetLoc);
            if (nextRubble < currRubble + 10 && rc.canMove(possibleDir)) {
                if (nextRubble < currBestRubble) {
                    currBestRubble = nextRubble;
                    currBestDist = targetLocDist;
                    currBestDirection = possibleDir;
                }
                else if (nextRubble == currBestRubble && targetLocDist < currBestDist) {
                    currBestRubble = nextRubble;
                    currBestDist = targetLocDist;
                    currBestDirection = possibleDir;
                }
            } 
        }
        if(rc.canMove(currBestDirection)) {
            rc.move(currBestDirection);
            Debug.printString("m" + currBestDirection);
        }
    }
    
    static void move(MapLocation target) throws GameActionException {
        move(target, false);
    }

    static void move(MapLocation target, boolean greedy) throws GameActionException {
        if (target == null) return;
        if (!rc.isMovementReady()) return;
        if (rc.getLocation().distanceSquaredTo(target) == 0) return;

        update(target);

        if (!greedy && turnsGreedy <= 0){
            Direction dir = getBestDir(target, BYTECODE_REMAINING);
            if (dir != null && !MapTracker.check(rc.adjacentLocation(dir))){
                Explore.move(dir);
                return;
            } else activateGreedy();
        }

        switch(rc.getType()) {
            case MINER:
            case BUILDER:
                if (Clock.getBytecodesLeft() >= BYTECODE_REMAINING) {
                    Pathfinding.move(target);
                    --turnsGreedy;
                } else {
                    Debug.setIndicatorDot(true, rc.getLocation(), 255, 255, 255);
                    Debug.println("Didn't have enough BC");
                }
                break;
            default:
                if (Clock.getBytecodesLeft() >= BYTECODE_REMAINING_NON_MINER_BUILDER) {
                    Pathfinding.move(target);
                    --turnsGreedy;
                } else {
                    Debug.setIndicatorDot(true, rc.getLocation(), 255, 255, 255);
                    Debug.println("Didn't have enough BC");
                }
                break;
        }
    }
}
