package MPBasic;

import battlecode.common.*;
import MPBasic.Debug.*;
import MPBasic.Util.*;

public class Archon extends Robot {
    static enum State {
        CHILLING,
        INIT,
    };
    static int robotCounter;
    static State currentState;

    public Archon(RobotController r) throws GameActionException {
        super(r);
        currentState = getInitialState();
    }
    public State getInitialState() {
        return State.CHILLING;
    }
    public boolean buildRobot(RobotType toBuild, Direction mainDir) throws GameActionException {
        Direction[] orderedDirs = Util.getOrderedDirections(mainDir);
        for(Direction dir : orderedDirs) {
            if (rc.canBuildRobot(toBuild, dir)){
                rc.buildRobot(toBuild, dir);
                RobotInfo robot = rc.senseRobotAtLocation(rc.getLocation().add(dir));
                } else {
                    System.out.println("CRITICAL: EC didn't find the robot it just built");
                }
                robotCounter += 1;
                return true;
            }
        return false;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        doStateAction();

    }
    public void doStateAction() throws GameActionException {
        switch(currentState) {
            case CHILLING:
                // Pick a direction to build in.
                if (Util.rng.nextBoolean()) {
                    // Let's try to build a miner.
                    rc.setIndicatorString("Trying to build a miner");
                    buildRobot(RobotType.MINER, Util.randomDirection());
                } else {
                    // Let's try to build a soldier.
                    rc.setIndicatorString("Trying to build a soldier");
                    buildRobot(RobotType.SOLDIER, Util.randomDirection());
                }
                break;
            default: 
                currentState = State.CHILLING;
                break;
        }
    }
    
}
