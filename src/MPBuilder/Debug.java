package MPBuilder;

import battlecode.common.*;

public class Debug {
    static final boolean VERBOSE = false;
    public static final boolean INFO = true;
    public static final boolean PATHFINDING = false;
    public static final boolean INDICATORS = true;

    private static RobotController rc;

    static void init(RobotController r) {
        rc = r;
    }

    static void println(boolean cond, String s) {
        if(VERBOSE && cond) {
            System.out.println(s);
        }
    }

    static void println(String s) {
        Debug.println(Debug.INFO, s);
    }
    
    static void print(boolean cond, String s) {
        if(VERBOSE && cond) {
            System.out.print(s);
        }
    }

    static void setIndicatorDot(boolean cond, MapLocation loc, int r, int g, int b) {
        if(VERBOSE && INDICATORS && cond && loc != null) {
            rc.setIndicatorDot(loc, r, g, b);
        }
    }

    static void setIndicatorLine(boolean cond, MapLocation startLoc, MapLocation endLoc, int r, int g, int b) {
        if(VERBOSE && INDICATORS && cond && startLoc != null && endLoc != null) {
            rc.setIndicatorLine(startLoc, endLoc, r, g, b);
        }
    }

    //could be used with more complex parameters to print nicer things
    static void setIndicatorString(String s) {
        rc.setIndicatorString(s);
    }
}
