package MPBasic;

import battlecode.common.*;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;
    static Robot bot;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */


    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // Debug.init(rc);
        // Nav.init(rc);
        System.out.println("I'm a " + rc.getType() + " and I just got created! I have health " + rc.getHealth());

        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");
        int setupFlag = rc.readSharedArray(0);
        int dataFlag = 0;
        int ourArchons = Util.friendlyArchonCount(setupFlag);
        // int theirArchons = Util.enemyArchonCount(setupFlag);
        RobotInfo[] sensableWithin2 = rc.senseNearbyRobots(2, rc.getTeam());
        if (rc.getType() != RobotType.ARCHON) {
            for (RobotInfo robot : sensableWithin2) {
                if (robot.getType() == RobotType.ARCHON && robot.getTeam() == rc.getTeam()) {
                    MapLocation robotLoc = robot.getLocation();
                    for (int i = Util.firstArchon; i < Util.firstArchon + ourArchons; i++) {
                        int testFlag = rc.readSharedArray(i);
                        MapLocation testLoc = MapLocation(xcoord(testFlag), ycoord(testFlag));
                        if (testLoc.equals(robotLoc)) {
                            dataFlag = rc.readSharedArray(i + mapLocToFLag);
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
            case SOLDIER:    bot = new Soldier(rc);  break;
            case LABORATORY: bot = new Laboratory(rc);  break;
            case WATCHTOWER: bot = new Watchtower(rc);  break;
            case BUILDER:    bot = new Builder(rc);  break;
            case SAGE:       bot = new Sage(rc); break;
        }
        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!
            System.out.println("Age: " + turnCount + "; Location: " + rc.getLocation());

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                bot.takeTurn();
                rc.setIndicatorString("Taking a turn");
                // if (bot.changeTo != null) {
                //     bot = bot.changeTo;
                //     bot.changeTo = null;
                //     continue;
                // }
                // Debug.println(Debug.info, "BC left at end: " + Clock.getBytecodesLeft());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

                reset(rc);
            }
        }
        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
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
