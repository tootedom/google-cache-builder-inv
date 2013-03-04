package org.greencheek.annotations;

import org.greencheek.annotations.axis.IterativeVerticalAxisCreator;
import org.greencheek.spark.Spark;
import org.greencheek.annotations.axis.VerticalAxisCreator;
import org.greencheek.spark.Spark2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * User: dominictootell
 * Date: 02/03/2013
 * Time: 16:43
 */
public class HeapMonitor {
    private final MemoryPoolMXBean youngMemoryPool;
    private final MemoryPoolMXBean survivorMemoryPool;
    private final MemoryPoolMXBean oldMemoryPool;
    private final MemoryPoolMXBean permMemoryPool;
    private final boolean hasMemoryPools;
    private ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(1);

    private static Logger log = LoggerFactory.getLogger(HeapMonitor.class);


    private Queue<Integer> youngHeapSize = new ConcurrentLinkedQueue<Integer>();
    private Queue<Integer> survivorHeapSize = new ConcurrentLinkedQueue<Integer>();
    private Queue<Integer> oldHeapSize = new ConcurrentLinkedQueue<Integer>();
    private Queue<Integer> permHeapSize = new ConcurrentLinkedQueue<Integer>();

    public HeapMonitor() {

        MemoryPoolMXBean youngPool = null;
        MemoryPoolMXBean survivorPool = null;
        MemoryPoolMXBean oldPool = null;
        MemoryPoolMXBean permPool = null;

        List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();

        for (MemoryPoolMXBean memoryPool : memoryPools)
        {
            String poolName = memoryPool.getName();
            if ("PS Eden Space".equals(poolName) ||
                    "Par Eden Space".equals(poolName) ||
                    "G1 Eden".equals(poolName))
                youngPool = memoryPool;
            else if ("PS Survivor Space".equals(poolName) ||
                    "Par Survivor Space".equals(poolName) ||
                    "G1 Survivor".equals(poolName))
                survivorPool = memoryPool;
            else if ("PS Old Gen".equals(poolName) ||
                    "CMS Old Gen".equals(poolName) ||
                    "G1 Old Gen".equals(poolName))
                oldPool = memoryPool;
            else if(poolName.contains("Perm"))
                permPool = memoryPool;
        }

        youngMemoryPool = youngPool;
        survivorMemoryPool = survivorPool;
        oldMemoryPool = oldPool;
        permMemoryPool = permPool;

        hasMemoryPools = youngMemoryPool != null && survivorMemoryPool != null && oldMemoryPool != null && permMemoryPool!=null;

        if(hasMemoryPools) {
             threadPool.scheduleAtFixedRate(new HeapMemoryCallable(),0,500, TimeUnit.MILLISECONDS);
        }

    }

    private int[] toIntArray(Integer[] integers) {
        int[] ints = new int[integers.length];

        int i=0;
        for (Integer integer : integers) {
            ints[i] = integer.intValue();
            i++;
        }
        return ints;
    }

    private void logMemoryUseWithAxis(String memoryName,Queue<Integer> memory) {
        VerticalAxisCreator heapAxisOutputter = new IterativeVerticalAxisCreator();

        int[] heap =toIntArray(memory.toArray(new Integer[memory.size()]));

        Spark2.Spark2Result heapGraph = Spark2.graph(heap);

        if(log.isInfoEnabled()) {
            String tabbedLine = String.format("%1$" + (memoryName.length()+(" in mb".length())) + "s", "");
            log.info("{} in mb:",memoryName);
            log.info("(min:{}/max:{})",heapGraph.getMin(),heapGraph.getMax());
            for(String heapGraphLine : heapGraph.graphToStringArray()) {
                log.info("{}:{}",tabbedLine,heapGraphLine);
            }

            for(String axisValue : heapAxisOutputter.verticalAxis(heap,4)) {
                log.info("{}:{}",tabbedLine,axisValue);
            }
            log.info("");
        }





    }

    public void stopRecordingMemoryUse() {
        threadPool.shutdownNow();
    }


    public void stop() {
        stopRecordingMemoryUse();

        logMemoryUseWithAxis("Young Space",youngHeapSize);
        logMemoryUseWithAxis("Survivor Space",survivorHeapSize);
        logMemoryUseWithAxis("Old Space",oldHeapSize);
        logMemoryUseWithAxis("Perm Space",permHeapSize);

        youngHeapSize.clear();
        survivorHeapSize.clear();
        oldHeapSize.clear();
    }


    public int mebiBytes(long bytes)
    {
        return Math.round(bytes / 1024 / 1024);
    }

    public class HeapMemoryCallable implements Runnable {

        @Override
        public void run() {
            long young = youngMemoryPool.getUsage().getUsed();
            long survivor = survivorMemoryPool.getUsage().getUsed();
            long old = oldMemoryPool.getUsage().getUsed();
            long perm = permMemoryPool.getUsage().getUsed();

            youngHeapSize.add(mebiBytes(young));
            survivorHeapSize.add(mebiBytes(survivor));
            oldHeapSize.add(mebiBytes(old));
            permHeapSize.add(mebiBytes(perm));
        }
    }


}
