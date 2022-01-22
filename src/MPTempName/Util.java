package MPTempName;

import battlecode.common.*;
import java.util.Random;


public class Util {
    static Random rng;

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };
    
    static final Direction[] directionsCenter = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
        Direction.CENTER
    };

    static final Direction[] exploreDirectionsOrder = {
        Direction.NORTH,
        Direction.SOUTHEAST,
        Direction.SOUTHWEST,
        Direction.NORTHWEST,
        Direction.EAST,
        Direction.SOUTH,
        Direction.WEST,
        Direction.NORTHEAST,
    };

    static Direction[] exploreDirections = exploreDirectionsOrder;

    static final int LeadThreshold = 50;
    static final int MinerDomain = 8;
    static final int WatchTowerDomain = 15;
    static final int MAX_MINERS = 128;
    static final int MIN_AREA_FOR_90_MINERS = 2000;
    static final int MIN_ROUND_FOR_LAB = 1800;
    static final int MAX_AREA_FOR_FAST_INIT = 625;
    static final int MAX_MAP_SIZE_TO_MINER_RATIO = 16;
    static final int JUST_OUTSIDE_OF_VISION_RADIUS = 34;
    static final int[] WatchTowerHealths = new int[]{150,270,486};
    static final int HealTimeout = 200;
    
    private static RobotController rc;
    static int SOLDIERS_NEEDED_TO_RUSH;
    static int MAP_WIDTH;
    static int MAP_HEIGHT;
    static int MAP_AREA;
    static int MAP_MAX_DIST_SQUARED;

    // Distance an enemy soldier needs to be within to an Archon
    // to be prioritized over a miner when attacking.
    static final int SOLDIER_PRIORITY_ATTACK_DIST = 40;

    // Distance a miner needs to be from home to deplete unit lead sources
    static final int MIN_DIST_TO_DEPLETE_UNIT_LEAD = 256;
    static final int MAX_MINER_LEAD_MULT = 30;

    static final int HEAL_DIST_TO_HOME = 5;
    static final int AVERAGE_HEALTH_TO_HEAL = 2 * RobotType.SOLDIER.health / 3;

    static final int TURNS_NOT_PRIORITIZED_TO_MOVE = 40;
    static final int MIN_DIST_SQUARED_FROM_CLUSTER = RobotType.ARCHON.visionRadiusSquared * 2;
    static final int MAX_CLUSTER_DIST_CHANGE = RobotType.ARCHON.visionRadiusSquared * 9;
    static final int MIN_DIST_FROM_PROJECTION = 25;
    static final int MIN_TURNS_TO_MOVE_AGAIN = 100;
    static final int MIN_TURN_TO_MOVE = 50;
    static final int MIN_DIST_TO_MOVE = RobotType.ARCHON.visionRadiusSquared;
    static final int MIN_ADJ_RUBBLE_MULTIPLIER = 5;

    // Spiral path going through all locations within radius 13
    // Note: iterate through this backwards
    static final Direction[] DIR_PATH_13 = new Direction[] {
        Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST, Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.NORTH, /* Direction.CENTER, */
    };

    static void init(RobotController r) {
        rc = r;
        rng = new Random(rc.getRoundNum()*23981 + rc.getID()*10289);
        MAP_HEIGHT = rc.getMapHeight();
        MAP_WIDTH = rc.getMapWidth();
        MAP_AREA = MAP_HEIGHT * MAP_WIDTH;
        // if (MAP_AREA <= 1500) {
        //     SOLDIERS_NEEDED_TO_RUSH = 10;
        // } else if (MAP_AREA <= 2500) {
        //     SOLDIERS_NEEDED_TO_RUSH = 20;
        // } else {
        //     SOLDIERS_NEEDED_TO_RUSH = 30;
        // }
        SOLDIERS_NEEDED_TO_RUSH = 30;

        MAP_MAX_DIST_SQUARED = MAP_HEIGHT * MAP_HEIGHT + MAP_WIDTH * MAP_WIDTH;
    }
    static MapLocation[] makePattern(MapLocation loc) {
        return new MapLocation[] {loc.translate(2, 0),
                                  loc.translate(-2, 0),
                                  loc.translate(0, 2),
                                  loc.translate(0, -2)};
    }    
    static Direction turnLeft90(Direction dir) {
        return dir.rotateLeft().rotateLeft();
    }

    static Direction turnRight90(Direction dir) {
        return dir.rotateRight().rotateRight();
    }

    static boolean canAttackorArchon(RobotType newType) throws GameActionException {
        switch(newType) {
            case SOLDIER:
            case WATCHTOWER:
            case ARCHON:
            case SAGE:
                return true;
            default:
                return false;
        }
    }
    static Direction[] getOrderedDirections(Direction dir) {
        return new Direction[]{dir, dir.rotateLeft(), dir.rotateLeft().rotateLeft(), dir.opposite().rotateRight(), dir.opposite(),
                dir.opposite().rotateLeft(), dir.rotateRight().rotateRight(), dir.rotateRight()};
    }

    static Direction[] getInOrderDirections(Direction target_dir) {
        return new Direction[]{target_dir, target_dir.rotateRight(), target_dir.rotateLeft(), 
            target_dir.rotateRight().rotateRight(), target_dir.rotateLeft().rotateLeft()};
    }
    
    static Direction[] getFullInOrderDirections(Direction target_dir) {
        return new Direction[]{target_dir, target_dir.rotateRight(), target_dir.rotateLeft(), 
            target_dir.rotateRight().rotateRight(), target_dir.rotateLeft().rotateLeft(), target_dir.rotateRight().rotateRight().rotateRight(), 
            target_dir.rotateLeft().rotateLeft().rotateLeft(), target_dir.opposite()};
    }

    static Direction getFirstValidInOrderDirection(Direction dir){
        for(Direction newDir : Util.getInOrderDirections(dir)) {
            if(rc.canMove(newDir)) {
                return newDir;
            }
        }
        return Direction.CENTER;
    }

    static Direction randomDirection() {
        return directions[Util.rng.nextInt(Util.directions.length)];
    }
    static Direction randomDirection(Direction[] newDirections) {
        return newDirections[Util.rng.nextInt(newDirections.length)];
    }

    static double leadPercentage(int numArchons, int thisArchon, double amountToLeave) {
        double upForGrabs = 1.0 - amountToLeave;
        double myShare = upForGrabs / (double) numArchons;
        double amountLeftForMyTurn = 1.0 - ((double) thisArchon - 1.0) * myShare;
        return myShare / amountLeftForMyTurn;
    }

    public static boolean onTheMap(MapLocation location) {
        return 0 <= location.x && location.x < MAP_WIDTH &&
                0 <= location.y && location.y < MAP_HEIGHT;
    }

    // Note: Not distance squared
    // Equivalent condition, can I add n to any dimension and
    // still get a location within the map
    public static boolean isLessThanDistOfEdge(MapLocation location, int n) {
        return location.x < n || location.y < n ||
                MAP_WIDTH <= location.x + n || MAP_HEIGHT <= location.y + n;
    }

    public static MapLocation[] guessEnemyLoc(MapLocation ourLoc) throws GameActionException {
        MapLocation[] results;
        int height = rc.getMapHeight();
        int width = rc.getMapWidth();
        MapLocation verticalFlip = new MapLocation(ourLoc.x, rc.getMapHeight() - ourLoc.y);
        MapLocation horizontalFlip = new MapLocation(rc.getMapWidth() - ourLoc.x, ourLoc.y);
        if (height == width) {
            results = new MapLocation[]{verticalFlip, horizontalFlip};
        } else {
            MapLocation rotation = new MapLocation(ourLoc.y, ourLoc.x);
            results = new MapLocation[]{verticalFlip, horizontalFlip, rotation};
        }
        return results;
    }

    public static MapLocation[] removeDupes(MapLocation[] original) {
        int count = 0;
        boolean[] test = new boolean[original.length];
        for (int i = 0; i < original.length; i++) {
            test[i] = true;
        }
        for (int i = 0; i < original.length; i++) {
            if (test[i]) {
                for (int j = i; j < original.length; j++) {
                    if (original[j].equals(original[i])) {
                        test[j] = false;
                    }
                }
            }
        }
        for (int i = 0; i < original.length; i++) {
            if (test[i]) {count++;}
        }
        MapLocation[] result = new MapLocation[count];
        int current = 0;
        for (int i = 0; i < original.length; i++) {
            if (test[i]) {
                result[current] = original[i];
                current++;
            }
        }
        return result;
    }

    public static MapLocation[] guessEnemyLocs() throws GameActionException {
        int height = rc.getMapHeight();
        int width = rc.getMapWidth();
        boolean isSquare = height == width;
        MapLocation[] locs1 = guessEnemyLoc(Comms.locationFromFlag(rc.readSharedArray(1)));
        MapLocation[] locs2 = guessEnemyLoc(Comms.locationFromFlag(rc.readSharedArray(2)));
        MapLocation[] locs3 = guessEnemyLoc(Comms.locationFromFlag(rc.readSharedArray(3)));
        MapLocation[] locs4 = guessEnemyLoc(Comms.locationFromFlag(rc.readSharedArray(4)));
        MapLocation[] result; 
        switch (rc.getArchonCount()) {
            case 1:
                return locs1;
            case 2:
                if (isSquare) {
                    result = new MapLocation[4];
                    result[0] = locs1[0];
                    result[1] = locs1[1];
                    result[2] = locs2[0];
                    result[3] = locs2[1];
                } else {
                    result = new MapLocation[6];
                    result[0] = locs1[0];
                    result[1] = locs1[1];
                    result[2] = locs1[2];
                    result[3] = locs2[0];
                    result[4] = locs2[1];
                    result[5] = locs2[2];
                }
                return removeDupes(result);
            case 3:
                if (isSquare) {
                    result = new MapLocation[6];
                    result[0] = locs1[0];
                    result[1] = locs1[1];
                    result[2] = locs2[0];
                    result[3] = locs2[1];
                    result[4] = locs3[0];
                    result[5] = locs3[1];
                } else {
                    result = new MapLocation[9];
                    result[0] = locs1[0];
                    result[1] = locs1[1];
                    result[2] = locs1[2];
                    result[3] = locs2[0];
                    result[4] = locs2[1];
                    result[5] = locs2[2];
                    result[6] = locs3[0];
                    result[7] = locs3[1];
                    result[8] = locs3[2];
                }
                return removeDupes(result);
            case 4:
                if (isSquare) {
                    result = new MapLocation[8];
                    result[0] = locs1[0];
                    result[1] = locs1[1];
                    result[2] = locs2[0];
                    result[3] = locs2[1];
                    result[4] = locs3[0];
                    result[5] = locs3[1];
                    result[6] = locs4[0];
                    result[7] = locs4[1];
                } else {
                    result = new MapLocation[12];
                    result[0] = locs1[0];
                    result[1] = locs1[1];
                    result[2] = locs1[2];
                    result[3] = locs2[0];
                    result[4] = locs2[1];
                    result[5] = locs2[2];
                    result[6] = locs3[0];
                    result[7] = locs3[1];
                    result[8] = locs3[2];
                    result[9] = locs4[0];
                    result[10] = locs4[1];
                    result[11] = locs4[2];
                }
                return removeDupes(result);
            default:
                return new MapLocation[]{};
        }
    }

    public static MapLocation[] narrowToThree(MapLocation[] all) {
        if (all.length <= 3) {
            return all;
        }
        MapLocation[] clusters = new MapLocation[3];
        int[] counts = new int[]{1, 1, 1};
        for (int i = 0; i < 3; i++) {
            clusters[i] = all[i];
        }
        for (int i = 3; i < all.length; i++) {
            int closest = -1;
            int closestDist = Integer.MAX_VALUE;
            MapLocation test = all[i];
            for (int j = 0; j < 3; j++) {
                MapLocation cluster = clusters[j];
                int distance = cluster.distanceSquaredTo(test);
                if (distance < closestDist) {
                    closestDist = distance;
                    closest = j;
                }
            }
            MapLocation bestCluster = clusters[closest];
            int count = counts[closest];
            int newX = (bestCluster.x * count + test.x) / (count + 1);
            int newY = (bestCluster.y * count + test.y) / (count + 1);
            MapLocation newCluster = new MapLocation(newX, newY);
            clusters[closest] = newCluster;
            counts[closest] = count + 1;
        }
        return clusters;
    }
    
    static int distance(MapLocation A, MapLocation B){
        return Math.max(Math.abs(A.x - B.x), Math.abs(A.y - B.y));
    }

    // Returns the location on the opposite side from loc wrt to your own location
    static MapLocation invertLocation(MapLocation loc) {
        int dx = loc.x - rc.getLocation().x;
        int dy = loc.y - rc.getLocation().y;
        return rc.getLocation().translate(-dx, -dy);
    }

    static int clip(int n, int lo, int hi) {
        return Math.min(Math.max(n, lo), hi);
    }

    static MapLocation clipToWithinMap(MapLocation loc) {
        return new MapLocation(clip(loc.x, 0, MAP_WIDTH), clip(loc.y, 0, MAP_HEIGHT));
    }

    static int getRubble(MapLocation loc) throws GameActionException {
        if(rc.canSenseLocation(loc)) return rc.senseRubble(loc);
        return 100;
    }

    static MapLocation addMult(MapLocation loc, Direction dir, int mult) {
        return loc.translate(dir.dx * mult, dir.dy * mult);
    }

    static Direction getFirstMoveableDir(Direction[] dirs) {
        for(Direction dir : dirs) {
            if(rc.canMove(dir)) {
                return dir;
            }
        }
        return Direction.CENTER;
    }

    public static void sortLocations(MapLocation[] possibleFlips, MapLocation[] listOfArchons) {
        for (int i = 0;  i < possibleFlips.length;  i++) {
            MapLocation itemToInsert = possibleFlips[i];
            int j = i;
            while (j != 0  &&  greaterThan(possibleFlips[j-1], itemToInsert, listOfArchons)) {
                possibleFlips[j] = possibleFlips[j-1];  j = j-1;
            }
            possibleFlips[j] = itemToInsert;
        }
    }

    public static boolean greaterThan(MapLocation loc1, MapLocation loc2, MapLocation[] listOfArchons) {
        int currDist1 = 0;
        for(MapLocation archonLoc: listOfArchons) {
            if(!archonLoc.equals(new MapLocation(0,0))) {
                currDist1 += loc1.distanceSquaredTo(archonLoc);
            }
        }
        int currDist2 = 0;
        for(MapLocation archonLoc: listOfArchons) {
            if(!archonLoc.equals(new MapLocation(0,0))) {
                currDist2 += loc2.distanceSquaredTo(archonLoc);
            }
        }
        return currDist1 > currDist2;
    }

    public static int cooldownTurnsRemaining(int rubble, int baseCooldown) {
        return (int) (Math.floor((1 + (rubble / 10)) * baseCooldown) / 10);
    }
}
