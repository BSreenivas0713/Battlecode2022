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

    static final Direction[] exploreDirections = {
        Direction.NORTH,
        Direction.SOUTHEAST,
        Direction.SOUTHWEST,
        Direction.NORTHWEST,
        Direction.EAST,
        Direction.SOUTH,
        Direction.NORTHEAST,
        Direction.WEST,
    };

    static final int LeadThreshold = 50;
    static final int ArchonStraightVisionRange = 5;
    static final int ArchonDiagVisionRange = 4;
    static final int MinerDomain = 8;
    static final int WatchTowerDomain = 15;
    static final int MAX_MINERS = 128;
    static final int MAX_MAP_SIZE_TO_MINER_RATIO = 16;
    static final int[] WatchTowerHealths = new int[]{130,143,156};
    
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
    
    static Direction[] getOrderedDirections(Direction dir) {
        return new Direction[]{dir, dir.rotateLeft(), dir.rotateLeft().rotateLeft(), dir.opposite().rotateRight(), dir.opposite(),
                dir.opposite().rotateLeft(), dir.rotateRight().rotateRight(), dir.rotateRight()};
    }
    static Direction[] getInOrderDirections(Direction target_dir) {
        return new Direction[]{target_dir, target_dir.rotateRight(), target_dir.rotateLeft(), 
            target_dir.rotateRight().rotateRight(), target_dir.rotateLeft().rotateLeft()};
    }
    static Direction getFirstValidInOrderDirection(Direction dir){
        for(Direction newDir : Util.getInOrderDirections(dir)) {
            if(rc.canMove(newDir)) {
                return newDir;
            }
        }
        return dir;
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
}
