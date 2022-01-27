package MPTempName;

import battlecode.common.*;
import MPTempName.Debug.*;
import MPTempName.Util.*;
import MPTempName.Comms.InformationCategory;

public class Laboratory extends Robot{
    static enum LabState {
        NORMAL,
        MOVING,
    }

    static int startRound;
    static int MIN_NUM_MINERS;

    static LabState currState;

    static MapLocation moveTarget;
    static boolean justCalculatedTarget;

    public Laboratory(RobotController r) throws GameActionException {
        this(r, Comms.firstArchonFlag);
    }

    public Laboratory(RobotController r, int homeFlagIndex) throws GameActionException {
        super(r);
        startRound = rc.getRoundNum();
        homeFlagIdx = homeFlagIndex;
        int MAX_NUM_MINERS = Math.min(Util.MAX_MINERS,
        rc.getMapWidth() * rc.getMapHeight() /
        Util.MAX_MAP_SIZE_TO_MINER_RATIO);
        MIN_NUM_MINERS = MAX_NUM_MINERS / 5;
        currState = LabState.NORMAL;
    }
    
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Comms.incrementAliveLabs();
        justCalculatedTarget = false;

        trySwitchState();
        doStateAction();
    }

    public void trySwitchState() throws GameActionException {
        switch(currState) {
            case NORMAL:
                if(rc.canTransform() && findNewSpot()) {
                    currState = LabState.MOVING;
                    justCalculatedTarget = true;
                    rc.transform();
                    // Debug.println("Lab moving");
                }
                break;
            case MOVING:
                if(moveTarget.equals(currLoc) && rc.canTransform()) {
                    currState = LabState.NORMAL;
                    rc.transform();
                    // Debug.println("Lab done moving");
                }
                break;
        }
    }

    public void doStateAction() throws GameActionException {
        switch(currState) {
            case NORMAL:
                Debug.printString("Normal");
                int sageCount = Comms.readSageCount();
                int minerCount = Comms.getSteadyMinerIdx();
                Debug.printString("mineCount: " + minerCount + "sageCount: " + sageCount);
                if(rc.canTransmute() &&
                    ((minerCount >= (sageCount)/2) ||
                        minerCount >= MIN_NUM_MINERS ||
                        (rc.getTeamLeadAmount(team) >= 75 && !Comms.checkIfArchonBuildingLab())
                        || rc.getTeamLeadAmount(team) >= 250)) { // if we are building a lab, don't trasmute or else we will be stuck building lab forever
                    Debug.printString("transmuting");
                    rc.transmute();
                }
                break;
            case MOVING:
                Debug.printString("Moving");
                reloadMoveTarget();
                Nav.move(moveTarget);
                Debug.setIndicatorLine(Debug.INDICATORS, rc.getLocation(), moveTarget, 204, 0, 255);
                break;
        }
    }

    public boolean findNewSpot() throws GameActionException {
        MapLocation loc = currLoc;
        int rubble;
        int minRubble = Integer.MAX_VALUE;
        MapLocation bestLoc = null;
        for(int i = Util.DIR_PATH_13.length; --i >= 0;) {
            loc = loc.add(Util.DIR_PATH_13[i]);
            if(Clock.getBytecodesLeft() < 1000) {
                Debug.printString("Broke search");
                break;
            }
            if(rc.canSenseRobotAtLocation(loc) || !rc.canSenseLocation(loc)) continue;
            rubble = rc.senseRubble(loc);
            if(rubble < minRubble) {
                minRubble = rubble;
                bestLoc = loc;
            }
        }

        if(minRubble + Util.MIN_RUBBLE_DIFF_TO_MOVE <= rc.senseRubble(currLoc)) {
            moveTarget = bestLoc;
            return true;
        }

        return false;
    }

    public void reloadMoveTarget() throws GameActionException {
        if(justCalculatedTarget) return;

        MapLocation loc = currLoc;
        int rubble;
        int minRubble = Integer.MAX_VALUE;
        MapLocation bestLoc = null;
        for(int i = Util.DIR_PATH_13.length; --i >= 0;) {
            loc = loc.add(Util.DIR_PATH_13[i]);
            if(Clock.getBytecodesLeft() < 1000) {
                Debug.printString("Broke search");
                break;
            }
            if(rc.canSenseRobotAtLocation(loc) || !rc.canSenseLocation(loc)) continue;
            rubble = rc.senseRubble(loc);
            if(rubble < minRubble) {
                minRubble = rubble;
                bestLoc = loc;
            }
        }

        if(rc.canSenseLocation(moveTarget) && !rc.canSenseRobotAtLocation(moveTarget)) {
            if(rc.senseRubble(moveTarget) > minRubble) {
                moveTarget = bestLoc;
            }
        } else if(!moveTarget.equals(currLoc)) {
            moveTarget = bestLoc;
        }
    }
}
