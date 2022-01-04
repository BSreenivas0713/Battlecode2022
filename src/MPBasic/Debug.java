package MPBasic;

import battlecode.common.*;

public class Debug {
    static final boolean verbose = false;

    private static RobotController rc;

    static void init(RobotController r) {
        rc = r;
    }

    static void println(String s) {
        if (verbose) {
            System.out.println(s);
        }
    }
    
    static void print(boolean cond, String s) {
        if (verbose) {
            System.out.print(s);
        }
    }

    //could be used with more complex parameters to print nicer things
    static void setIndicatorString(String s) {
        rc.setIndicatorString(s);
    }
}
