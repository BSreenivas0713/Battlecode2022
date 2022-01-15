package MPJynx;

import battlecode.common.*;
import MPJynx.Util.*;
import MPJynx.Comms.*;
import MPJynx.Debug.*;


public strictfp class RobotPlayer {

    static Robot bot;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        Debug.init(rc);
        Util.init(rc);
        Comms.init(rc);
        Explore.init(rc);
        Nav.init(rc);

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

        InformationCategory ic = Comms.getICFromFlag(dataFlag);
        switch (rc.getType()) {
            case ARCHON:     bot = new Archon(rc);  break;
            case MINER:
                bot = new Miner(rc);
                if(ic == InformationCategory.DIRECTION) {
                    Explore.lastExploreDir = Comms.decodeArchonFlagDirection(dataFlag);
                    Debug.printString("Dir: " + Explore.lastExploreDir);
                }
                break;
            case SOLDIER:
                bot = new Soldier(rc, homeFlagIdx);
                break;
            case LABORATORY: bot = new Laboratory(rc);  break;
            case WATCHTOWER: bot = new Watchtower(rc);  break;
            case BUILDER:    bot = new Builder(rc);  break;
            case SAGE:       bot = new Sage(rc); break;
        }

        while (true) {
            try {
                bot.initTurn();
                bot.takeTurn();
                bot.endTurn();
                Debug.flush();
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
