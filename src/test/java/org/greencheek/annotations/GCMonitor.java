package org.greencheek.annotations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

public class GCMonitor {
    private static final GarbageCollectorMXBean youngCollector;
    private static final GarbageCollectorMXBean oldCollector;
    private static boolean hasCollectors;

    private static Logger log = LoggerFactory.getLogger(GCMonitor.class);


    static {
        List<GarbageCollectorMXBean> garbageCollectors = ManagementFactory.getGarbageCollectorMXBeans();
        GarbageCollectorMXBean yC = null;
        GarbageCollectorMXBean oC = null;
        for (GarbageCollectorMXBean garbageCollector : garbageCollectors) {
            if ("PS Scavenge".equals(garbageCollector.getName()) ||
                    "ParNew".equals(garbageCollector.getName()) ||
                    "G1 Young Generation".equals(garbageCollector.getName()))
                yC = garbageCollector;
            else if ("PS MarkSweep".equals(garbageCollector.getName()) ||
                    "ConcurrentMarkSweep".equals(garbageCollector.getName()) ||
                    "G1 Old Generation".equals(garbageCollector.getName()))
                oC = garbageCollector;
        }

        hasCollectors = yC != null && oC != null;
        youngCollector = yC;
        oldCollector = oC;
    }

    private volatile long youngStartNo;
    private volatile long oldStartNo;

    public void start() {
        if(hasCollectors) {
            youngStartNo = youngCollector.getCollectionCount();
            oldStartNo = oldCollector.getCollectionCount();
        }
    }

    public void report() {
        if(hasCollectors) {
           long youngNowNo = youngCollector.getCollectionCount();
           long oldNowNo = oldCollector.getCollectionCount();
            log.info("number of young gc collections: {}, number of old gc collections: {}",youngNowNo-youngStartNo,oldNowNo-oldStartNo);

        }
    }

}