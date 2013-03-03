import org.greencheek.annotations.GCMonitor;
import org.greencheek.annotations.HeapMonitor;
import org.greencheek.annotations.domain.ClassWithAnnotations;
import org.greencheek.annotations.service.AnnotationReader;
import org.greencheek.annotations.service.BasicAnnotationReader;
import org.greencheek.annotations.service.CachingAnnotationReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


/**
 * User: dominictootell
 * Date: 27/02/2013
 * Time: 22:11
 */
public class RunCachingTest {

    private static final int MAX_NO_THREADS = 64;
    private static final CompilationMXBean jit = ManagementFactory.getCompilationMXBean();
    private static ExecutorService threadPool = Executors.newFixedThreadPool(MAX_NO_THREADS);
    private static Logger log = LoggerFactory.getLogger(RunCachingTest.class);
    private static final AnnotationReader cache = new CachingAnnotationReader();
    private static final AnnotationReader noncache = new BasicAnnotationReader();
    private static final GCMonitor gcMonitor = new GCMonitor();


    private static void runThreadedExecution(int threads, int iterations, AnnotationReaderExecutor callable)
            throws ExecutionException, InterruptedException
    {
        gcMonitor.start();
        long startCompileTime = jit.getTotalCompilationTime();
        RunCachingTest.executeTestWithThreads(threads,iterations,callable);
        gcMonitor.report();
        long endCompileTime = jit.getTotalCompilationTime();
        log.info("Jit compilation: {}",(endCompileTime-startCompileTime));
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        int iterations = 1000000;

        AnnotationReaderExecutor cachingCallable = new AnnotationReaderExecutor("CacheBuilder",cache,iterations);//createCallables(NO_THREADS,iterations,cache);
        AnnotationReaderExecutor noncachingCallables = new AnnotationReaderExecutor("GetAnnotations",noncache,iterations);//createCallables(NO_THREADS,iterations,noncache);


        log.info("Warmup");
        log.info("--------------");

        long startCompileTime = jit.getTotalCompilationTime();
        gcMonitor.start();
        RunCachingTest.executeTestWithThreads(1,iterations,noncachingCallables);
        RunCachingTest.executeTestWithThreads(10,iterations,noncachingCallables);
        RunCachingTest.executeTestWithThreads(20,iterations,noncachingCallables);
        long endCompileTime = jit.getTotalCompilationTime();
        gcMonitor.report();
        log.info("Jit compilation: {}",(endCompileTime-startCompileTime));


        gcMonitor.start();
        startCompileTime = jit.getTotalCompilationTime();
        RunCachingTest.executeTestWithThreads(1,iterations,cachingCallable);
        RunCachingTest.executeTestWithThreads(10,iterations,cachingCallable);
        RunCachingTest.executeTestWithThreads(20,iterations,cachingCallable);
        endCompileTime = jit.getTotalCompilationTime();
        gcMonitor.report();
        log.info("Jit compilation: {}",(endCompileTime-startCompileTime));
        log.info("--------------");

        System.gc();

        log.info("");
        log.info("");
        log.info("");
        log.info("");
        log.info("Run");
        log.info("--------------");
        log.info("CACHE BUILDER");
        log.info("--------------");

        HeapMonitor heapMonitor = new HeapMonitor();

        runThreadedExecution(1, iterations, cachingCallable);
        runThreadedExecution(2, iterations, cachingCallable);
        runThreadedExecution(4, iterations, cachingCallable);
        runThreadedExecution(8, iterations, cachingCallable);
        runThreadedExecution(16, iterations, cachingCallable);
        runThreadedExecution(32, iterations, cachingCallable);
        runThreadedExecution(64, iterations, cachingCallable);

        heapMonitor.stop();
        cache.close();
        System.gc();

        log.info("");
        log.info("");
        log.info("");
        log.info("");

        log.info("--------------");
        log.info("GET ANNOTATIONS");
        log.info("--------------");

        heapMonitor = new HeapMonitor();

        runThreadedExecution(1, iterations, noncachingCallables);
        runThreadedExecution(2, iterations, noncachingCallables);
        runThreadedExecution(4, iterations, noncachingCallables);
        runThreadedExecution(8, iterations, noncachingCallables);
        runThreadedExecution(16, iterations, noncachingCallables);
        runThreadedExecution(32, iterations, noncachingCallables);
        runThreadedExecution(64, iterations, noncachingCallables);

        threadPool.shutdownNow();

        heapMonitor.stop();
    }

    static class Monitor
    {
        final long duration;
        public Monitor(long durationNs)
        {
            duration = durationNs;
        }

        public long getDuration() { return duration; }
    }

    private static void executeTestWithThreads(final int numThreads,
                                        final int iterations,
                                        final AnnotationReaderExecutor callable) throws InterruptedException, ExecutionException
    {

        List<Future<Monitor>> futures = new ArrayList<Future<Monitor>>();

        for(int i=0;i<numThreads;i++)
        {
            futures.add(threadPool.submit(callable));
        }

        long average = 0;

        for(Future<Monitor> future : futures) {
            Monitor m = future.get();
            average += (m.getDuration());
        }

        log.info("'{}' Finished with {} thread(s). Average time: {} ms", callable.getName(), numThreads, ((average / numThreads) / iterations));
    }

    static class AnnotationReaderExecutor implements Callable<Monitor>
    {
        private final AnnotationReader reader;
        private final int loops;
        private final String name;


        public AnnotationReaderExecutor(String name,AnnotationReader reader, int iterations)
        {
            this.reader = reader;
            this.loops = iterations;
            this.name = name;
        }

        public AnnotationReader getReader() {
            return reader;
        }

        public String getName() {
            return name;
        }

        @Override
        public Monitor call() {

            long startTime = System.nanoTime();
            for (long i = 0; i < loops; i++) {
                reader.getAnnotations(ClassWithAnnotations.class);
            }
            long endTime = System.nanoTime();
            return new Monitor(endTime - startTime);

        }
    }

}
