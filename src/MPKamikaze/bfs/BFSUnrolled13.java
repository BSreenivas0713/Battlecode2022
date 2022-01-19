package MPKamikaze.bfs;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class BFSUnrolled13 {

    static RobotController rc;
    // Includes a ~200 BC cushion
    public static final int MIN_BC_TO_USE = 3100;

    public static void init(RobotController r) {
        rc = r;
    }

    static MapLocation l43;
    static double v43;
    static Direction d43;
    static double p43;

    static MapLocation l44;
    static double v44;
    static Direction d44;
    static double p44;

    static MapLocation l45;
    static double v45;
    static Direction d45;
    static double p45;

    static MapLocation l46;
    static double v46;
    static Direction d46;
    static double p46;

    static MapLocation l47;
    static double v47;
    static Direction d47;
    static double p47;

    static MapLocation l55;
    static double v55;
    static Direction d55;
    static double p55;

    static MapLocation l56;
    static double v56;
    static Direction d56;
    static double p56;

    static MapLocation l57;
    static double v57;
    static Direction d57;
    static double p57;

    static MapLocation l58;
    static double v58;
    static Direction d58;
    static double p58;

    static MapLocation l59;
    static double v59;
    static Direction d59;
    static double p59;

    static MapLocation l60;
    static double v60;
    static Direction d60;
    static double p60;

    static MapLocation l61;
    static double v61;
    static Direction d61;
    static double p61;

    static MapLocation l68;
    static double v68;
    static Direction d68;
    static double p68;

    static MapLocation l69;
    static double v69;
    static Direction d69;
    static double p69;

    static MapLocation l70;
    static double v70;
    static Direction d70;
    static double p70;

    static MapLocation l71;
    static double v71;
    static Direction d71;
    static double p71;

    static MapLocation l72;
    static double v72;
    static Direction d72;
    static double p72;

    static MapLocation l73;
    static double v73;
    static Direction d73;
    static double p73;

    static MapLocation l74;
    static double v74;
    static Direction d74;
    static double p74;

    static MapLocation l81;
    static double v81;
    static Direction d81;
    static double p81;

    static MapLocation l82;
    static double v82;
    static Direction d82;
    static double p82;

    static MapLocation l83;
    static double v83;
    static Direction d83;
    static double p83;

    static MapLocation l84;
    static double v84;
    static Direction d84;
    static double p84;

    static MapLocation l85;
    static double v85;
    static Direction d85;
    static double p85;

    static MapLocation l86;
    static double v86;
    static Direction d86;
    static double p86;

    static MapLocation l87;
    static double v87;
    static Direction d87;
    static double p87;

    static MapLocation l94;
    static double v94;
    static Direction d94;
    static double p94;

    static MapLocation l95;
    static double v95;
    static Direction d95;
    static double p95;

    static MapLocation l96;
    static double v96;
    static Direction d96;
    static double p96;

    static MapLocation l97;
    static double v97;
    static Direction d97;
    static double p97;

    static MapLocation l98;
    static double v98;
    static Direction d98;
    static double p98;

    static MapLocation l99;
    static double v99;
    static Direction d99;
    static double p99;

    static MapLocation l100;
    static double v100;
    static Direction d100;
    static double p100;

    static MapLocation l107;
    static double v107;
    static Direction d107;
    static double p107;

    static MapLocation l108;
    static double v108;
    static Direction d108;
    static double p108;

    static MapLocation l109;
    static double v109;
    static Direction d109;
    static double p109;

    static MapLocation l110;
    static double v110;
    static Direction d110;
    static double p110;

    static MapLocation l111;
    static double v111;
    static Direction d111;
    static double p111;

    static MapLocation l112;
    static double v112;
    static Direction d112;
    static double p112;

    static MapLocation l113;
    static double v113;
    static Direction d113;
    static double p113;

    static MapLocation l121;
    static double v121;
    static Direction d121;
    static double p121;

    static MapLocation l122;
    static double v122;
    static Direction d122;
    static double p122;

    static MapLocation l123;
    static double v123;
    static Direction d123;
    static double p123;

    static MapLocation l124;
    static double v124;
    static Direction d124;
    static double p124;

    static MapLocation l125;
    static double v125;
    static Direction d125;
    static double p125;


    public static Direction getBestDir(MapLocation target){
        l84 = rc.getLocation();
        v84 = 0;
        l71 = l84.add(Direction.WEST);
        v71 = 1000000;
        d71 = null;
        l83 = l71.add(Direction.SOUTHEAST);
        v83 = 1000000;
        d83 = null;
        l97 = l83.add(Direction.NORTHEAST);
        v97 = 1000000;
        d97 = null;
        l85 = l97.add(Direction.NORTHWEST);
        v85 = 1000000;
        d85 = null;
        l72 = l85.add(Direction.WEST);
        v72 = 1000000;
        d72 = null;
        l58 = l72.add(Direction.SOUTHWEST);
        v58 = 1000000;
        d58 = null;
        l70 = l58.add(Direction.SOUTHEAST);
        v70 = 1000000;
        d70 = null;
        l82 = l70.add(Direction.SOUTHEAST);
        v82 = 1000000;
        d82 = null;
        l96 = l82.add(Direction.NORTHEAST);
        v96 = 1000000;
        d96 = null;
        l110 = l96.add(Direction.NORTHEAST);
        v110 = 1000000;
        d110 = null;
        l98 = l110.add(Direction.NORTHWEST);
        v98 = 1000000;
        d98 = null;
        l86 = l98.add(Direction.NORTHWEST);
        v86 = 1000000;
        d86 = null;
        l73 = l86.add(Direction.WEST);
        v73 = 1000000;
        d73 = null;
        l60 = l73.add(Direction.WEST);
        v60 = 1000000;
        d60 = null;
        l59 = l60.add(Direction.SOUTH);
        v59 = 1000000;
        d59 = null;
        l45 = l59.add(Direction.SOUTHWEST);
        v45 = 1000000;
        d45 = null;
        l57 = l45.add(Direction.SOUTHEAST);
        v57 = 1000000;
        d57 = null;
        l56 = l57.add(Direction.SOUTH);
        v56 = 1000000;
        d56 = null;
        l69 = l56.add(Direction.EAST);
        v69 = 1000000;
        d69 = null;
        l81 = l69.add(Direction.SOUTHEAST);
        v81 = 1000000;
        d81 = null;
        l95 = l81.add(Direction.NORTHEAST);
        v95 = 1000000;
        d95 = null;
        l108 = l95.add(Direction.EAST);
        v108 = 1000000;
        d108 = null;
        l109 = l108.add(Direction.NORTH);
        v109 = 1000000;
        d109 = null;
        l123 = l109.add(Direction.NORTHEAST);
        v123 = 1000000;
        d123 = null;
        l111 = l123.add(Direction.NORTHWEST);
        v111 = 1000000;
        d111 = null;
        l112 = l111.add(Direction.NORTH);
        v112 = 1000000;
        d112 = null;
        l99 = l112.add(Direction.WEST);
        v99 = 1000000;
        d99 = null;
        l87 = l99.add(Direction.NORTHWEST);
        v87 = 1000000;
        d87 = null;
        l74 = l87.add(Direction.WEST);
        v74 = 1000000;
        d74 = null;
        l61 = l74.add(Direction.WEST);
        v61 = 1000000;
        d61 = null;
        l47 = l60.add(Direction.WEST);
        v47 = 1000000;
        d47 = null;
        l46 = l47.add(Direction.SOUTH);
        v46 = 1000000;
        d46 = null;
        l44 = l45.add(Direction.SOUTH);
        v44 = 1000000;
        d44 = null;
        l43 = l44.add(Direction.SOUTH);
        v43 = 1000000;
        d43 = null;
        l55 = l56.add(Direction.SOUTH);
        v55 = 1000000;
        d55 = null;
        l68 = l55.add(Direction.EAST);
        v68 = 1000000;
        d68 = null;
        l94 = l81.add(Direction.EAST);
        v94 = 1000000;
        d94 = null;
        l107 = l94.add(Direction.EAST);
        v107 = 1000000;
        d107 = null;
        l121 = l108.add(Direction.EAST);
        v121 = 1000000;
        d121 = null;
        l122 = l121.add(Direction.NORTH);
        v122 = 1000000;
        d122 = null;
        l124 = l123.add(Direction.NORTH);
        v124 = 1000000;
        d124 = null;
        l125 = l124.add(Direction.NORTH);
        v125 = 1000000;
        d125 = null;
        l113 = l112.add(Direction.NORTH);
        v113 = 1000000;
        d113 = null;
        l100 = l113.add(Direction.WEST);
        v100 = 1000000;
        d100 = null;

        try {
            if (rc.canMove(Direction.WEST)) {
                p71 = 1.0 + rc.senseRubble(l71) / 10.0;
                v71 = p71;
                d71 = Direction.WEST;
            }
            if (rc.canMove(Direction.SOUTH)) {
                p83 = 1.0 + rc.senseRubble(l83) / 10.0;
                v83 = p83;
                d83 = Direction.SOUTH;
                if (v83 > v71 + p83) {
                    v83 = v71 + p83;
                    d83 = d71;
                }
            }
            if (rc.canMove(Direction.NORTH)) {
                p85 = 1.0 + rc.senseRubble(l85) / 10.0;
                v85 = p85;
                d85 = Direction.NORTH;
                if (v85 > v71 + p85) {
                    v85 = v71 + p85;
                    d85 = d71;
                }
            }
            if (rc.canMove(Direction.EAST)) {
                p97 = 1.0 + rc.senseRubble(l97) / 10.0;
                v97 = p97;
                d97 = Direction.EAST;
                if (v97 > v85 + p97) {
                    v97 = v85 + p97;
                    d97 = d85;
                }
                if (v97 > v83 + p97) {
                    v97 = v83 + p97;
                    d97 = d83;
                }
            }
            if (rc.canMove(Direction.SOUTHWEST)) {
                p70 = 1.0 + rc.senseRubble(l70) / 10.0;
                v70 = p70;
                d70 = Direction.SOUTHWEST;
                if (v70 > v71 + p70) {
                    v70 = v71 + p70;
                    d70 = d71;
                }
                if (v70 > v83 + p70) {
                    v70 = v83 + p70;
                    d70 = d83;
                }
            }
            if (rc.canMove(Direction.NORTHWEST)) {
                p72 = 1.0 + rc.senseRubble(l72) / 10.0;
                v72 = p72;
                d72 = Direction.NORTHWEST;
                if (v72 > v71 + p72) {
                    v72 = v71 + p72;
                    d72 = d71;
                }
                if (v72 > v85 + p72) {
                    v72 = v85 + p72;
                    d72 = d85;
                }
            }
            if (rc.canMove(Direction.SOUTHEAST)) {
                p96 = 1.0 + rc.senseRubble(l96) / 10.0;
                v96 = p96;
                d96 = Direction.SOUTHEAST;
                if (v96 > v97 + p96) {
                    v96 = v97 + p96;
                    d96 = d97;
                }
                if (v96 > v83 + p96) {
                    v96 = v83 + p96;
                    d96 = d83;
                }
            }
            if (rc.canMove(Direction.NORTHEAST)) {
                p98 = 1.0 + rc.senseRubble(l98) / 10.0;
                v98 = p98;
                d98 = Direction.NORTHEAST;
                if (v98 > v85 + p98) {
                    v98 = v85 + p98;
                    d98 = d85;
                }
                if (v98 > v97 + p98) {
                    v98 = v97 + p98;
                    d98 = d97;
                }
            }
            if (rc.canSenseLocation(l58)) {
                p58 = 1.0 + rc.senseRubble(l58) / 10.0;
                if (v58 > v71 + p58) {
                    v58 = v71 + p58;
                    d58 = d71;
                }
                if (v58 > v70 + p58) {
                    v58 = v70 + p58;
                    d58 = d70;
                }
                if (v58 > v72 + p58) {
                    v58 = v72 + p58;
                    d58 = d72;
                }
            }
            if (rc.canSenseLocation(l82)) {
                p82 = 1.0 + rc.senseRubble(l82) / 10.0;
                if (v82 > v83 + p82) {
                    v82 = v83 + p82;
                    d82 = d83;
                }
                if (v82 > v70 + p82) {
                    v82 = v70 + p82;
                    d82 = d70;
                }
                if (v82 > v96 + p82) {
                    v82 = v96 + p82;
                    d82 = d96;
                }
            }
            if (rc.canSenseLocation(l86)) {
                p86 = 1.0 + rc.senseRubble(l86) / 10.0;
                if (v86 > v85 + p86) {
                    v86 = v85 + p86;
                    d86 = d85;
                }
                if (v86 > v72 + p86) {
                    v86 = v72 + p86;
                    d86 = d72;
                }
                if (v86 > v98 + p86) {
                    v86 = v98 + p86;
                    d86 = d98;
                }
            }
            if (rc.canSenseLocation(l110)) {
                p110 = 1.0 + rc.senseRubble(l110) / 10.0;
                if (v110 > v97 + p110) {
                    v110 = v97 + p110;
                    d110 = d97;
                }
                if (v110 > v98 + p110) {
                    v110 = v98 + p110;
                    d110 = d98;
                }
                if (v110 > v96 + p110) {
                    v110 = v96 + p110;
                    d110 = d96;
                }
            }
            if (rc.canSenseLocation(l57)) {
                p57 = 1.0 + rc.senseRubble(l57) / 10.0;
                if (v57 > v71 + p57) {
                    v57 = v71 + p57;
                    d57 = d71;
                }
                if (v57 > v70 + p57) {
                    v57 = v70 + p57;
                    d57 = d70;
                }
                if (v57 > v58 + p57) {
                    v57 = v58 + p57;
                    d57 = d58;
                }
            }
            if (rc.canSenseLocation(l59)) {
                p59 = 1.0 + rc.senseRubble(l59) / 10.0;
                if (v59 > v71 + p59) {
                    v59 = v71 + p59;
                    d59 = d71;
                }
                if (v59 > v72 + p59) {
                    v59 = v72 + p59;
                    d59 = d72;
                }
                if (v59 > v58 + p59) {
                    v59 = v58 + p59;
                    d59 = d58;
                }
            }
            if (rc.canSenseLocation(l69)) {
                p69 = 1.0 + rc.senseRubble(l69) / 10.0;
                if (v69 > v83 + p69) {
                    v69 = v83 + p69;
                    d69 = d83;
                }
                if (v69 > v70 + p69) {
                    v69 = v70 + p69;
                    d69 = d70;
                }
                if (v69 > v82 + p69) {
                    v69 = v82 + p69;
                    d69 = d82;
                }
                if (v69 > v57 + p69) {
                    v69 = v57 + p69;
                    d69 = d57;
                }
            }
            if (rc.canSenseLocation(l73)) {
                p73 = 1.0 + rc.senseRubble(l73) / 10.0;
                if (v73 > v85 + p73) {
                    v73 = v85 + p73;
                    d73 = d85;
                }
                if (v73 > v72 + p73) {
                    v73 = v72 + p73;
                    d73 = d72;
                }
                if (v73 > v86 + p73) {
                    v73 = v86 + p73;
                    d73 = d86;
                }
                if (v73 > v59 + p73) {
                    v73 = v59 + p73;
                    d73 = d59;
                }
            }
            if (rc.canSenseLocation(l95)) {
                p95 = 1.0 + rc.senseRubble(l95) / 10.0;
                if (v95 > v83 + p95) {
                    v95 = v83 + p95;
                    d95 = d83;
                }
                if (v95 > v96 + p95) {
                    v95 = v96 + p95;
                    d95 = d96;
                }
                if (v95 > v82 + p95) {
                    v95 = v82 + p95;
                    d95 = d82;
                }
            }
            if (rc.canSenseLocation(l99)) {
                p99 = 1.0 + rc.senseRubble(l99) / 10.0;
                if (v99 > v85 + p99) {
                    v99 = v85 + p99;
                    d99 = d85;
                }
                if (v99 > v98 + p99) {
                    v99 = v98 + p99;
                    d99 = d98;
                }
                if (v99 > v86 + p99) {
                    v99 = v86 + p99;
                    d99 = d86;
                }
            }
            if (rc.canSenseLocation(l109)) {
                p109 = 1.0 + rc.senseRubble(l109) / 10.0;
                if (v109 > v97 + p109) {
                    v109 = v97 + p109;
                    d109 = d97;
                }
                if (v109 > v96 + p109) {
                    v109 = v96 + p109;
                    d109 = d96;
                }
                if (v109 > v110 + p109) {
                    v109 = v110 + p109;
                    d109 = d110;
                }
                if (v109 > v95 + p109) {
                    v109 = v95 + p109;
                    d109 = d95;
                }
            }
            if (rc.canSenseLocation(l111)) {
                p111 = 1.0 + rc.senseRubble(l111) / 10.0;
                if (v111 > v97 + p111) {
                    v111 = v97 + p111;
                    d111 = d97;
                }
                if (v111 > v98 + p111) {
                    v111 = v98 + p111;
                    d111 = d98;
                }
                if (v111 > v110 + p111) {
                    v111 = v110 + p111;
                    d111 = d110;
                }
                if (v111 > v99 + p111) {
                    v111 = v99 + p111;
                    d111 = d99;
                }
            }
            if (rc.canSenseLocation(l56)) {
                p56 = 1.0 + rc.senseRubble(l56) / 10.0;
                if (v56 > v70 + p56) {
                    v56 = v70 + p56;
                    d56 = d70;
                }
                if (v56 > v57 + p56) {
                    v56 = v57 + p56;
                    d56 = d57;
                }
                if (v56 > v69 + p56) {
                    v56 = v69 + p56;
                    d56 = d69;
                }
            }
            if (rc.canSenseLocation(l60)) {
                p60 = 1.0 + rc.senseRubble(l60) / 10.0;
                if (v60 > v72 + p60) {
                    v60 = v72 + p60;
                    d60 = d72;
                }
                if (v60 > v59 + p60) {
                    v60 = v59 + p60;
                    d60 = d59;
                }
                if (v60 > v73 + p60) {
                    v60 = v73 + p60;
                    d60 = d73;
                }
            }
            if (rc.canSenseLocation(l108)) {
                p108 = 1.0 + rc.senseRubble(l108) / 10.0;
                if (v108 > v96 + p108) {
                    v108 = v96 + p108;
                    d108 = d96;
                }
                if (v108 > v109 + p108) {
                    v108 = v109 + p108;
                    d108 = d109;
                }
                if (v108 > v95 + p108) {
                    v108 = v95 + p108;
                    d108 = d95;
                }
            }
            if (rc.canSenseLocation(l112)) {
                p112 = 1.0 + rc.senseRubble(l112) / 10.0;
                if (v112 > v98 + p112) {
                    v112 = v98 + p112;
                    d112 = d98;
                }
                if (v112 > v99 + p112) {
                    v112 = v99 + p112;
                    d112 = d99;
                }
                if (v112 > v111 + p112) {
                    v112 = v111 + p112;
                    d112 = d111;
                }
            }
            if (rc.canSenseLocation(l45)) {
                p45 = 1.0 + rc.senseRubble(l45) / 10.0;
                if (v45 > v58 + p45) {
                    v45 = v58 + p45;
                    d45 = d58;
                }
                if (v45 > v57 + p45) {
                    v45 = v57 + p45;
                    d45 = d57;
                }
                if (v45 > v59 + p45) {
                    v45 = v59 + p45;
                    d45 = d59;
                }
            }
            if (rc.canSenseLocation(l81)) {
                p81 = 1.0 + rc.senseRubble(l81) / 10.0;
                if (v81 > v82 + p81) {
                    v81 = v82 + p81;
                    d81 = d82;
                }
                if (v81 > v69 + p81) {
                    v81 = v69 + p81;
                    d81 = d69;
                }
                if (v81 > v95 + p81) {
                    v81 = v95 + p81;
                    d81 = d95;
                }
            }
            if (rc.canSenseLocation(l87)) {
                p87 = 1.0 + rc.senseRubble(l87) / 10.0;
                if (v87 > v86 + p87) {
                    v87 = v86 + p87;
                    d87 = d86;
                }
                if (v87 > v73 + p87) {
                    v87 = v73 + p87;
                    d87 = d73;
                }
                if (v87 > v99 + p87) {
                    v87 = v99 + p87;
                    d87 = d99;
                }
            }
            if (rc.canSenseLocation(l123)) {
                p123 = 1.0 + rc.senseRubble(l123) / 10.0;
                if (v123 > v110 + p123) {
                    v123 = v110 + p123;
                    d123 = d110;
                }
                if (v123 > v111 + p123) {
                    v123 = v111 + p123;
                    d123 = d111;
                }
                if (v123 > v109 + p123) {
                    v123 = v109 + p123;
                    d123 = d109;
                }
            }
            if (rc.canSenseLocation(l44)) {
                p44 = 1.0 + rc.senseRubble(l44) / 10.0;
                if (v44 > v58 + p44) {
                    v44 = v58 + p44;
                    d44 = d58;
                }
                if (v44 > v57 + p44) {
                    v44 = v57 + p44;
                    d44 = d57;
                }
                if (v44 > v56 + p44) {
                    v44 = v56 + p44;
                    d44 = d56;
                }
                if (v44 > v45 + p44) {
                    v44 = v45 + p44;
                    d44 = d45;
                }
            }
            if (rc.canSenseLocation(l46)) {
                p46 = 1.0 + rc.senseRubble(l46) / 10.0;
                if (v46 > v58 + p46) {
                    v46 = v58 + p46;
                    d46 = d58;
                }
                if (v46 > v59 + p46) {
                    v46 = v59 + p46;
                    d46 = d59;
                }
                if (v46 > v60 + p46) {
                    v46 = v60 + p46;
                    d46 = d60;
                }
                if (v46 > v45 + p46) {
                    v46 = v45 + p46;
                    d46 = d45;
                }
            }
            if (rc.canSenseLocation(l68)) {
                p68 = 1.0 + rc.senseRubble(l68) / 10.0;
                if (v68 > v82 + p68) {
                    v68 = v82 + p68;
                    d68 = d82;
                }
                if (v68 > v69 + p68) {
                    v68 = v69 + p68;
                    d68 = d69;
                }
                if (v68 > v56 + p68) {
                    v68 = v56 + p68;
                    d68 = d56;
                }
                if (v68 > v81 + p68) {
                    v68 = v81 + p68;
                    d68 = d81;
                }
            }
            if (rc.canSenseLocation(l74)) {
                p74 = 1.0 + rc.senseRubble(l74) / 10.0;
                if (v74 > v86 + p74) {
                    v74 = v86 + p74;
                    d74 = d86;
                }
                if (v74 > v73 + p74) {
                    v74 = v73 + p74;
                    d74 = d73;
                }
                if (v74 > v60 + p74) {
                    v74 = v60 + p74;
                    d74 = d60;
                }
                if (v74 > v87 + p74) {
                    v74 = v87 + p74;
                    d74 = d87;
                }
            }
            if (rc.canSenseLocation(l94)) {
                p94 = 1.0 + rc.senseRubble(l94) / 10.0;
                if (v94 > v82 + p94) {
                    v94 = v82 + p94;
                    d94 = d82;
                }
                if (v94 > v95 + p94) {
                    v94 = v95 + p94;
                    d94 = d95;
                }
                if (v94 > v108 + p94) {
                    v94 = v108 + p94;
                    d94 = d108;
                }
                if (v94 > v81 + p94) {
                    v94 = v81 + p94;
                    d94 = d81;
                }
            }
            if (rc.canSenseLocation(l100)) {
                p100 = 1.0 + rc.senseRubble(l100) / 10.0;
                if (v100 > v86 + p100) {
                    v100 = v86 + p100;
                    d100 = d86;
                }
                if (v100 > v99 + p100) {
                    v100 = v99 + p100;
                    d100 = d99;
                }
                if (v100 > v112 + p100) {
                    v100 = v112 + p100;
                    d100 = d112;
                }
                if (v100 > v87 + p100) {
                    v100 = v87 + p100;
                    d100 = d87;
                }
            }
            if (rc.canSenseLocation(l122)) {
                p122 = 1.0 + rc.senseRubble(l122) / 10.0;
                if (v122 > v110 + p122) {
                    v122 = v110 + p122;
                    d122 = d110;
                }
                if (v122 > v109 + p122) {
                    v122 = v109 + p122;
                    d122 = d109;
                }
                if (v122 > v108 + p122) {
                    v122 = v108 + p122;
                    d122 = d108;
                }
                if (v122 > v123 + p122) {
                    v122 = v123 + p122;
                    d122 = d123;
                }
            }
            if (rc.canSenseLocation(l124)) {
                p124 = 1.0 + rc.senseRubble(l124) / 10.0;
                if (v124 > v110 + p124) {
                    v124 = v110 + p124;
                    d124 = d110;
                }
                if (v124 > v111 + p124) {
                    v124 = v111 + p124;
                    d124 = d111;
                }
                if (v124 > v112 + p124) {
                    v124 = v112 + p124;
                    d124 = d112;
                }
                if (v124 > v123 + p124) {
                    v124 = v123 + p124;
                    d124 = d123;
                }
            }
            if (rc.canSenseLocation(l43)) {
                p43 = 1.0 + rc.senseRubble(l43) / 10.0;
                if (v43 > v57 + p43) {
                    v43 = v57 + p43;
                    d43 = d57;
                }
                if (v43 > v56 + p43) {
                    v43 = v56 + p43;
                    d43 = d56;
                }
                if (v43 > v44 + p43) {
                    v43 = v44 + p43;
                    d43 = d44;
                }
            }
            if (rc.canSenseLocation(l47)) {
                p47 = 1.0 + rc.senseRubble(l47) / 10.0;
                if (v47 > v59 + p47) {
                    v47 = v59 + p47;
                    d47 = d59;
                }
                if (v47 > v60 + p47) {
                    v47 = v60 + p47;
                    d47 = d60;
                }
                if (v47 > v46 + p47) {
                    v47 = v46 + p47;
                    d47 = d46;
                }
            }
            if (rc.canSenseLocation(l55)) {
                p55 = 1.0 + rc.senseRubble(l55) / 10.0;
                if (v55 > v69 + p55) {
                    v55 = v69 + p55;
                    d55 = d69;
                }
                if (v55 > v56 + p55) {
                    v55 = v56 + p55;
                    d55 = d56;
                }
                if (v55 > v68 + p55) {
                    v55 = v68 + p55;
                    d55 = d68;
                }
                if (v55 > v43 + p55) {
                    v55 = v43 + p55;
                    d55 = d43;
                }
            }
            if (rc.canSenseLocation(l61)) {
                p61 = 1.0 + rc.senseRubble(l61) / 10.0;
                if (v61 > v73 + p61) {
                    v61 = v73 + p61;
                    d61 = d73;
                }
                if (v61 > v60 + p61) {
                    v61 = v60 + p61;
                    d61 = d60;
                }
                if (v61 > v74 + p61) {
                    v61 = v74 + p61;
                    d61 = d74;
                }
                if (v61 > v47 + p61) {
                    v61 = v47 + p61;
                    d61 = d47;
                }
            }
            if (rc.canSenseLocation(l107)) {
                p107 = 1.0 + rc.senseRubble(l107) / 10.0;
                if (v107 > v95 + p107) {
                    v107 = v95 + p107;
                    d107 = d95;
                }
                if (v107 > v108 + p107) {
                    v107 = v108 + p107;
                    d107 = d108;
                }
                if (v107 > v94 + p107) {
                    v107 = v94 + p107;
                    d107 = d94;
                }
            }
            if (rc.canSenseLocation(l113)) {
                p113 = 1.0 + rc.senseRubble(l113) / 10.0;
                if (v113 > v99 + p113) {
                    v113 = v99 + p113;
                    d113 = d99;
                }
                if (v113 > v112 + p113) {
                    v113 = v112 + p113;
                    d113 = d112;
                }
                if (v113 > v100 + p113) {
                    v113 = v100 + p113;
                    d113 = d100;
                }
            }
            if (rc.canSenseLocation(l121)) {
                p121 = 1.0 + rc.senseRubble(l121) / 10.0;
                if (v121 > v109 + p121) {
                    v121 = v109 + p121;
                    d121 = d109;
                }
                if (v121 > v108 + p121) {
                    v121 = v108 + p121;
                    d121 = d108;
                }
                if (v121 > v122 + p121) {
                    v121 = v122 + p121;
                    d121 = d122;
                }
                if (v121 > v107 + p121) {
                    v121 = v107 + p121;
                    d121 = d107;
                }
            }
            if (rc.canSenseLocation(l125)) {
                p125 = 1.0 + rc.senseRubble(l125) / 10.0;
                if (v125 > v111 + p125) {
                    v125 = v111 + p125;
                    d125 = d111;
                }
                if (v125 > v112 + p125) {
                    v125 = v112 + p125;
                    d125 = d112;
                }
                if (v125 > v124 + p125) {
                    v125 = v124 + p125;
                    d125 = d124;
                }
                if (v125 > v113 + p125) {
                    v125 = v113 + p125;
                    d125 = d113;
                }
            }

            int dx = target.x - l84.x;
            int dy = target.y - l84.y;
            switch (dx) {
                case -3:
                    switch (dy) {
                        case -2:
                            return d43;
                        case -1:
                            return d44;
                        case 0:
                            return d45;
                        case 1:
                            return d46;
                        case 2:
                            return d47;
                    }
                    break;
                case -2:
                    switch (dy) {
                        case -3:
                            return d55;
                        case -2:
                            return d56;
                        case -1:
                            return d57;
                        case 0:
                            return d58;
                        case 1:
                            return d59;
                        case 2:
                            return d60;
                        case 3:
                            return d61;
                    }
                    break;
                case -1:
                    switch (dy) {
                        case -3:
                            return d68;
                        case -2:
                            return d69;
                        case -1:
                            return d70;
                        case 0:
                            return d71;
                        case 1:
                            return d72;
                        case 2:
                            return d73;
                        case 3:
                            return d74;
                    }
                    break;
                case 0:
                    switch (dy) {
                        case -3:
                            return d81;
                        case -2:
                            return d82;
                        case -1:
                            return d83;
                        case 0:
                            return d84;
                        case 1:
                            return d85;
                        case 2:
                            return d86;
                        case 3:
                            return d87;
                    }
                    break;
                case 1:
                    switch (dy) {
                        case -3:
                            return d94;
                        case -2:
                            return d95;
                        case -1:
                            return d96;
                        case 0:
                            return d97;
                        case 1:
                            return d98;
                        case 2:
                            return d99;
                        case 3:
                            return d100;
                    }
                    break;
                case 2:
                    switch (dy) {
                        case -3:
                            return d107;
                        case -2:
                            return d108;
                        case -1:
                            return d109;
                        case 0:
                            return d110;
                        case 1:
                            return d111;
                        case 2:
                            return d112;
                        case 3:
                            return d113;
                    }
                    break;
                case 3:
                    switch (dy) {
                        case -2:
                            return d121;
                        case -1:
                            return d122;
                        case 0:
                            return d123;
                        case 1:
                            return d124;
                        case 2:
                            return d125;
                    }
                    break;
            }

            Direction ans = null;
            double bestEstimation = 0;
            double initialDist = Math.sqrt(l84.distanceSquaredTo(target));

            
            double dist43 = (initialDist - Math.sqrt(l43.distanceSquaredTo(target))) / v43;
            if (dist43 > bestEstimation) {
                bestEstimation = dist43;
                ans = d43;
            }
            double dist44 = (initialDist - Math.sqrt(l44.distanceSquaredTo(target))) / v44;
            if (dist44 > bestEstimation) {
                bestEstimation = dist44;
                ans = d44;
            }
            double dist45 = (initialDist - Math.sqrt(l45.distanceSquaredTo(target))) / v45;
            if (dist45 > bestEstimation) {
                bestEstimation = dist45;
                ans = d45;
            }
            double dist46 = (initialDist - Math.sqrt(l46.distanceSquaredTo(target))) / v46;
            if (dist46 > bestEstimation) {
                bestEstimation = dist46;
                ans = d46;
            }
            double dist47 = (initialDist - Math.sqrt(l47.distanceSquaredTo(target))) / v47;
            if (dist47 > bestEstimation) {
                bestEstimation = dist47;
                ans = d47;
            }
            double dist55 = (initialDist - Math.sqrt(l55.distanceSquaredTo(target))) / v55;
            if (dist55 > bestEstimation) {
                bestEstimation = dist55;
                ans = d55;
            }
            double dist61 = (initialDist - Math.sqrt(l61.distanceSquaredTo(target))) / v61;
            if (dist61 > bestEstimation) {
                bestEstimation = dist61;
                ans = d61;
            }
            double dist68 = (initialDist - Math.sqrt(l68.distanceSquaredTo(target))) / v68;
            if (dist68 > bestEstimation) {
                bestEstimation = dist68;
                ans = d68;
            }
            double dist74 = (initialDist - Math.sqrt(l74.distanceSquaredTo(target))) / v74;
            if (dist74 > bestEstimation) {
                bestEstimation = dist74;
                ans = d74;
            }
            double dist81 = (initialDist - Math.sqrt(l81.distanceSquaredTo(target))) / v81;
            if (dist81 > bestEstimation) {
                bestEstimation = dist81;
                ans = d81;
            }
            double dist87 = (initialDist - Math.sqrt(l87.distanceSquaredTo(target))) / v87;
            if (dist87 > bestEstimation) {
                bestEstimation = dist87;
                ans = d87;
            }
            double dist94 = (initialDist - Math.sqrt(l94.distanceSquaredTo(target))) / v94;
            if (dist94 > bestEstimation) {
                bestEstimation = dist94;
                ans = d94;
            }
            double dist100 = (initialDist - Math.sqrt(l100.distanceSquaredTo(target))) / v100;
            if (dist100 > bestEstimation) {
                bestEstimation = dist100;
                ans = d100;
            }
            double dist107 = (initialDist - Math.sqrt(l107.distanceSquaredTo(target))) / v107;
            if (dist107 > bestEstimation) {
                bestEstimation = dist107;
                ans = d107;
            }
            double dist113 = (initialDist - Math.sqrt(l113.distanceSquaredTo(target))) / v113;
            if (dist113 > bestEstimation) {
                bestEstimation = dist113;
                ans = d113;
            }
            double dist121 = (initialDist - Math.sqrt(l121.distanceSquaredTo(target))) / v121;
            if (dist121 > bestEstimation) {
                bestEstimation = dist121;
                ans = d121;
            }
            double dist122 = (initialDist - Math.sqrt(l122.distanceSquaredTo(target))) / v122;
            if (dist122 > bestEstimation) {
                bestEstimation = dist122;
                ans = d122;
            }
            double dist123 = (initialDist - Math.sqrt(l123.distanceSquaredTo(target))) / v123;
            if (dist123 > bestEstimation) {
                bestEstimation = dist123;
                ans = d123;
            }
            double dist124 = (initialDist - Math.sqrt(l124.distanceSquaredTo(target))) / v124;
            if (dist124 > bestEstimation) {
                bestEstimation = dist124;
                ans = d124;
            }
            double dist125 = (initialDist - Math.sqrt(l125.distanceSquaredTo(target))) / v125;
            if (dist125 > bestEstimation) {
                bestEstimation = dist125;
                ans = d125;
            }
            return ans;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
