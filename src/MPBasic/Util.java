package MPBasic;

import battlecode.common.*;
import java.util.Random;


public class Util {
    static final Random rng = new Random(2001);

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

    static final int ArchonStraightVisionRange = 5;
    static final int ArchonDiagVisionRange = 4;
    static final int MinerDomain = 8;
    static final int MAX_MINERS = 128;
    static final int MAX_MAP_SIZE_TO_MINER_RATIO = 16;
    
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

    static Direction randomDirection() {
        return directions[Util.rng.nextInt(Util.directions.length)];
    }
    static Direction randomDirection(Direction[] newDirections) {
        return newDirections[Util.rng.nextInt(newDirections.length)];
    }

    static double leadPercentage(int numArchons, int thisArchon, double amountToLeave) {
        double upForGrabs = 1.0 - amountToLeave;
        double myShare = upForGrabs / numArchons;
        double amountLeftForMyTurn = 1.0 - ((double) thisArchon - 1.0) * myShare;
        return myShare / amountLeftForMyTurn;
    }
}
