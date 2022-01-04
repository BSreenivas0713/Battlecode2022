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

    static double leadPercentage(int numArchons, int thisArchon) {
        switch (numArchons) {
            case 1:
                return 0.2;
            case 2:
                switch(thisArchon) {
                    case 1:
                        return 0.6;
                    case 2:
                        return 0.33;
                    default:
                        return 0;
                }
            case 3:
                switch(thisArchon) {
                    case 1:
                        return 0.75;
                    case 2:
                        return 0.67;
                    case 3:
                        return 0.5;
                    default:
                        return 0;
                }
            case 4:
                switch(thisArchon) {
                    case 1:
                        return 0.8;
                    case 2:
                        return 0.75;
                    case 3:
                        return 0.67;
                    case 4:
                        return 0.5;
                    default:
                        return 0;
                }
            default:
                return 0;
        }
    }
}
