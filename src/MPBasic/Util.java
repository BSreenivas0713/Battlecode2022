package MPBasic;

import battlecode.common.*;
import java.util.Random;


public class Util {
    static final Random rng = new Random(2001);

    static final int mapLocToFLag = 8;

    static final int numArchons = 4;
    static final int firstArchon = 1;
    static final int lastArchon = 4;

    public static void friendlyArchonCount(int flag) {
        return flag & 3;
    }

    public static void enemyArchonCount(int flag) {
        return (flag & 12) >> 2;
    }

    public static void xcoord(int flag) {
        return flag & 255;
    }
    public static void ycoord(int flag) {
        return (flag & 65280) >> 8;
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
    
    static Direction[] getOrderedDirections(Direction dir) {
        return new Direction[]{dir, dir.rotateLeft(), dir.rotateLeft().rotateLeft(), dir.opposite().rotateRight(), dir.opposite(),
                dir.opposite().rotateLeft(), dir.rotateRight().rotateRight(), dir.rotateRight()};
    }
    static Direction randomDirection() {
        return directions[Util.rng.nextInt(Util.directions.length)];
    }
}
