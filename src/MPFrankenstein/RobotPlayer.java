package MPFrankenstein;

import battlecode.common.*;
import MPFrankenstein.Util.*;
import MPFrankenstein.Comms.*;
import MPFrankenstein.Debug.*;


public strictfp class RobotPlayer {

    static int turnCount = 0;
    static Robot bot;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        Debug.init(rc);
        Comms.init(rc);
        Nav.init(rc);
        Util.init(rc);

        int setupFlag = rc.readSharedArray(0);
        int dataFlag = 0;
        int ourArchons = Comms.friendlyArchonCount();
        int homeFlagIdx = Comms.firstArchonFlag;
        RobotInfo[] sensableWithin2 = rc.senseNearbyRobots(2, rc.getTeam());
        if (rc.getType() != RobotType.ARCHON) {
            for (RobotInfo robot : sensableWithin2) {
                if (robot.getType() == RobotType.ARCHON && robot.getTeam() == rc.getTeam()) {
                    MapLocation robotLoc = robot.getLocation();
                    for (int i = Comms.firstArchon; i < Comms.firstArchon + ourArchons; i++) {
                        int testFlag = rc.readSharedArray(i);
                        if (robotLoc.equals(Comms.locationFromFlag(testFlag))) {
                            homeFlagIdx = i + Comms.mapLocToFlag;
                            dataFlag = rc.readSharedArray(i + Comms.mapLocToFlag);
                            break;
                        }
                    }
                    break;
                }
            }
        }

        switch (rc.getType()) {
            case ARCHON:     bot = new Archon(rc);  break;
            case MINER:      bot = new Miner(rc);  break;
            case SOLDIER:
                InformationCategory ic = Comms.getICFromFlag(dataFlag);
                if (ic == InformationCategory.DEFENSE_SOLDIERS) {
                    bot = new DefenseSoldier(rc, homeFlagIdx);
                } else {
                    bot = new Soldier(rc, homeFlagIdx);
                }
                break;
            case LABORATORY: bot = new Laboratory(rc);  break;
            case WATCHTOWER: bot = new Watchtower(rc);  break;
            case BUILDER:    bot = new Builder(rc);  break;
            case SAGE:       bot = new Sage(rc); break;
        }
        while (true) {

            turnCount += 1;

            try {
                bot.takeTurn();
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

                reset(rc);
            }
        }
    }

    // Last resort if a bot errors out in deployed code
    // Certain static variables might need to be cleared to ensure 
    // a successful return to execution.
    public static void reset(RobotController rc) throws GameActionException {
        switch (rc.getType()) {
                case ARCHON:     bot = new Archon(rc); /*Archon.currentState = Archon.State.CHILLING;*/  break;
                case MINER:      bot = new Miner(rc);  break;
                case SOLDIER:    bot = new Soldier(rc);  break;
                case LABORATORY: bot = new Laboratory(rc);  break;
                case WATCHTOWER: bot = new Watchtower(rc);  break;
                case BUILDER:    bot = new Builder(rc);  break;
                case SAGE:       bot = new Sage(rc); break;
        }
        // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
    }
}
