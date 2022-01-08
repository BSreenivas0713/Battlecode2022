package MPGettingBetter;

import battlecode.common.*;
import MPGettingBetter.fast.FastIterableIntSet;
import MPGettingBetter.fast.FasterQueue;

public class Nav {
    static RobotController rc;

    static BFSUnrolled bfs;

    static MapLocation lastDest;
    static int closestDistanceToDest;
    static int turnsSinceClosestDistanceDecreased;

    public static Direction lastExploreDir;
    static final int EXPLORE_BOREDOM = 20;
    static int boredom;

    static void init(RobotController r) {
        rc = r;
        bfs = new BFSUnrolled(r);
        closestDistanceToDest = Integer.MAX_VALUE;
        turnsSinceClosestDistanceDecreased = 0;
        lastExploreDir = null;
    }

    static Direction getBestDir(MapLocation dest) throws GameActionException {
        MapLocation currLoc = rc.getLocation();

        if(!dest.equals(lastDest)) {
            lastDest = dest;
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

        Direction dir = Nav.navTo(dest);
        return dir == null ? currLoc.directionTo(dest) : dir;
    }

    public static Direction navTo(MapLocation dest) throws GameActionException {
        if(Clock.getBytecodesLeft() > 6000) {
            return bfs.getBestDir(dest);
        } else {
            return getGreedyDirection(rc.getLocation().directionTo(dest));
        }
    }

	public static Direction[] explorePathfinding() throws GameActionException {
        // Debug.println(Debug.PATHFINDING, "Exploring");
        if(!rc.isMovementReady())
            return new Direction[0];
        
		if(lastExploreDir == null) {
            lastExploreDir = rc.getLocation().directionTo(Robot.home).opposite();
			boredom = 0;
        }
        
        // Pick a kinda new direction if you've gone in the same direction for a while
		if(boredom >= EXPLORE_BOREDOM) {
            boredom = 0;
            Direction[] newDirChoices = {
                lastExploreDir.rotateLeft(),
                lastExploreDir,
                lastExploreDir.rotateRight(),
            };
			lastExploreDir = newDirChoices[(int) (Math.random() * newDirChoices.length)];
		}
        boredom++;

        // Pick a new direction if you ran into a wall.
        if(!rc.onTheMap(rc.getLocation().add(lastExploreDir))) {
            // lastExploreDir = lastExploreDir.opposite();
            Direction tempExploreDir = null;
            if((int) (Math.random() * 2) == 0) {
                tempExploreDir = Util.turnLeft90(lastExploreDir);
                if(!rc.onTheMap(rc.getLocation().add(tempExploreDir))) {
                    tempExploreDir = Util.turnRight90(lastExploreDir);
                }
            }
            else {
                tempExploreDir = Util.turnRight90(lastExploreDir);
                if(!rc.onTheMap(rc.getLocation().add(tempExploreDir))) {
                    tempExploreDir = Util.turnLeft90(lastExploreDir);
                }
            lastExploreDir = tempExploreDir;
            }
        }

        MapLocation target = rc.getLocation().translate(lastExploreDir.getDeltaX() * 5, 
                                                        lastExploreDir.getDeltaY() * 5);
        return Util.getInOrderDirections(getBestDir(target));
    }

    public static Direction getGreedyDirection(Direction dir) throws GameActionException {
        Direction[] bestDirs = greedyDirection(dir);
        if(bestDirs.length > 0) {
            return bestDirs[0];
        } else {
            return dir;
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
            orderedDirs[0] = left; orderedDirs[1] = dir; orderedDirs[2] = dir;
        }

        Direction[] dirs = new Direction[numToInsert];
        System.arraycopy(orderedDirs, 0, dirs, 0, numToInsert);
        return dirs;
    }

    public static Direction[] exploreGreedy() throws GameActionException {
        // Debug.println(Debug.INFO, "Exploring");
        if(!rc.isMovementReady())
            return new Direction[0];
        
		if(lastExploreDir == null) {
            // Debug.println(Debug.INFO, "changing last Explore Dir");
            Direction oppositeFromHome = Util.randomDirection();
            if (Robot.home != null) {
                oppositeFromHome = rc.getLocation().directionTo(Robot.home).opposite();
            }
            Direction[] oppositeFromHomeDirs = {oppositeFromHome, oppositeFromHome.rotateLeft(), oppositeFromHome.rotateRight()};
            lastExploreDir = oppositeFromHomeDirs[(int)(Math.random() * 3)];
			boredom = 0;
        }
        
		if(boredom >= EXPLORE_BOREDOM) {
            // Debug.println(Debug.INFO, "changing last Explore Dir because of boredom");
            boredom = 0;
            Direction[] newDirChoices = {
                lastExploreDir.rotateLeft(),
                lastExploreDir,
                lastExploreDir.rotateRight()};
			lastExploreDir = newDirChoices[(int) (Math.random() * newDirChoices.length)];
		}
        boredom++;
        
		if(!rc.onTheMap(rc.getLocation().add(lastExploreDir))) {
            // Debug.println(Debug.INFO, "changing last Explore Dir because of a wall");
            Direction tempExploreDir = null;
            if((int) (Math.random() * 2) == 0) {
                tempExploreDir = Util.turnLeft90(lastExploreDir);
                if(!rc.onTheMap(rc.getLocation().add(tempExploreDir))) {
                    tempExploreDir = Util.turnRight90(lastExploreDir);
                }
            }
            else {
                tempExploreDir = Util.turnRight90(lastExploreDir);
                if(!rc.onTheMap(rc.getLocation().add(tempExploreDir))) {
                    tempExploreDir = Util.turnLeft90(lastExploreDir);
                }
            lastExploreDir = tempExploreDir;
            }
        }

        return greedyDirection(lastExploreDir);
    }

    public static Direction[] explore() throws GameActionException {
        return exploreGreedy();
    }
}
