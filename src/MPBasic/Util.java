package MPBasic;

import battlecode.common.*;
import java.util.Random;


public class Util {
    static final Random rng = new Random(2001);

    static final int mapLocToFlag = 8;

    static final int numArchons = 4;
    static final int firstArchon = 1;
    static final int lastArchon = 4;

    public static int friendlyArchonCount(int flag) {
        return flag & 3;
    }
    public static int enemyArchonCount(int flag) {
        return (flag & 12) >> 2;
    }

    public static int xcoord(int flag) {
        return flag & 255;
    }
    public static int ycoord(int flag) {
        return (flag & 65280) >> 8;
    }

    public static int storeMyLoc(RobotController rc) {
        return rc.getLocation().x + (rc.getLocation().y << 8);
    } 

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
    static Direction randomDirection() {
        return directions[Util.rng.nextInt(Util.directions.length)];
    }
}
