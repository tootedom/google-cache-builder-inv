## Synopsis

Just a basic investigation into using the google guava library.  Initial thoughts were to look
at the cache to simply wrap the call to Class.getAnnotations which is sub-optimal and can deadlock in
java 6/7, due to http://bugs.sun.com/view_bug.do?bug_id=7122142.

Initial investigation showed up an interesting finding, add a LoadingCache seemed to have negative effect (i.e. slower).

```java
    cache = CacheBuilder.newBuilder()
                   .maximumSize(1000)
                   .expireAfterWrite(10, TimeUnit.SECONDS)
                   .concurrencyLevel(64)
                   .build(new CacheLoader<Class, Annotation[]>() {
                       @Override
                       public Annotation[] load(Class key) throws Exception {
                           return key.getAnnotations();
                       }
                   });
```

Closer inspection and analysis of the use case, it was seen/found that:

a Large number of consecutive cache hits, with no writes; would not be most appropriate use
for the Cache Builder.  The cache builder records access for the get, which adds an entry to a recencyQueue.  As a
result this uses a large amount of memory, that causes gc.

The benchmark also doesn't really simulate what would be a live situation.  You wouldn't ever probably get 1 million
cache reads on the same cache item in succession (for each thread, it is highly unrealistic);
without a write occurring (the recency queue is flushed when a write occurs in the segment of the hash associated with
the recency queue.  So it's unrealistic that you'd get such heavy number of gets.

However, it's interesting to see the overhead of the Cache, and is something to consider when using as a cache for things.

### Benchmark Results

The class (RunCachingTest) in the root package, runs the benchmark

jvm runtime options: -Xmx512m -Xms512m -Xmn128m -XX:-UseBiasedLocking

The benchmark basically does 1 million .get() hits (for eache thread that is running - unrealistic yes, it's just a
throughput test to see the effects of caching, and if there's anything to take into consideration - i.e. in this
case memory).  The benchmark is run with 1,2,4,8,16,32,64 threads.  The first benchmark runs the CacheBuilder scenario,
the second hits the Class.getAnnotations directly.

The benchmark outputs the amount of gc:


#### CacheBuilder

```java
public class CachingAnnotationReader implements AnnotationReader {

    private final LoadingCache<Class,Annotation[]> cache;

    public CachingAnnotationReader()  {
        cache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .concurrencyLevel(64)
                .build(new CacheLoader<Class, Annotation[]>() {
                    @Override
                    public Annotation[] load(Class key) throws Exception {
                        return key.getAnnotations();
                    }
                });

    }

    @Override
    public Annotation[] getAnnotations(final Class clazz) {
//        cache.stats();  // this call reduces the recency queue
        return cache.getUnchecked(clazz);
    }

    public void close() {
        cache.cleanUp();
    }
}
```

The GC/Heap Profile for the above is (the numbers are mb):

```
Young Space:▁▇▇▅▄▁▅▁▁▅▁▁▅▁▁▁▂▃▃▃▁▁▁▄▁▄▂▁▇▇▁▇▁▁▁▁▂▁▁▁▂▅▅▅▅▅▅▁▁▄▄▁▁▃▁▁▁▁▃▆▆▆▆▆▆▆▆▆▆██▃▁▃▅▁▁▁▂▁▁▁▁:min/max:0/102
           :   6   0   8   0   4   5   9   9   0   0   5   2   0   0   7   7   7   3   0   0
           :   1               2   0       4           9               4   5   5   7
           :

Survivor Space:▁▁██▆████████▇██████████▁▁▄▂▂▁███████▁▁▁▁▁▁▁▁▁▁███████▁▁▁██▁▁▁▁▁▁▁▁▁▁██████████▁▁▁▁:min/max:0/12
              :   1   1   1   1   1   1   2   1   1   0   0   1   1   0   0   0   0   1   1   0
              :   2   2   2   2   2   2       2   2           2   2                   2   2

Old Space:▁▁▁▁▁▁▁▃▃▃▅▅▅▅▆▆▆▆▆▆▇▇▇▇▁▁▁▁▁▁▂▂▄▄▆▆▆▇▇▇▇█████▇▃▄▄▄▆▆▆▅▅▅▇▇▇▇▇▇▇▇▇▇▇▇▇▄▄▅▅▅▇▇▇▇▅▅▅▅:min/max:0/384
         :   4   1   2   3   2   3   2   8   2   3   3   1   3   2   3   3   3   1   3   2
         :       2   2   0   7   6   7   4   8   5   8   3   1   7   8   8   8   8   6   7
         :       8   7   5   5   3           3   4   4   2   2   0   3   3   3   7   4   3

Perm Space:▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁:min/max:7/7
          :   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7
```

#### clazz.getAnnotations()

```java
public class BasicAnnotationReader implements AnnotationReader {

    @Override
    public Annotation[] getAnnotations(Class clazz) {
        return clazz.getAnnotations();
    }

    public void close(){}
}
```

The GC/Heap Profile for the above is (the numbers are mb):

```
Young Space:▁▃▇▃▁▅▂▇▄▁▆▄▁▆▃▁▆▃█▅▂▇:min/max:0/96
           :   4   9   4   3   5
           :   1   1   9       5

Survivor Space:▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁:min/max:0/0
              :   0   0   0   0   0

Old Space:▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁:min/max:0/0
         :   0   0   0   0   0

Perm Space:▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁:min/max:7/7
          :   7   7   7   7   7
```

The recency queue is used by the Cache to determine which LRU item in the cache to remove when the maxSize has been
reached.  The item is not determine by the age of the item from when it was last written to the queue (Least Recently Added),
nor is it determine by which item is closest to it's Expiration time (may be these options are nice to have features on the cache).
When using expireAfterWrite(10, TimeUnit.SECONDS); with it being write access you are interested in (just pondering out loud)
However, having said this how the cache works it that the item to remove upon reaching the maximum size is determined
by when it was last accessed, based on access ordering.

### Modifications

You can add "cache.stats" to the CacheBuilder to cleanUp the recency queue.  Which helps, in terms of memory
profile, but this doesn't speed up the caching.  Here's the memory profile with cache.stats in (the graphs are
longer, as it took longer to execute):

```
Young Space:▁▄▇▃▇▃▆▅▄▄▂▂▁▁▃▆▁▄▆▂▄▇▆▁▃▇▄▂▇▄▂▄▂▆▄▂▆▂▄▂▇▂▆▄▂▇▁▆▄▁▆▄▂▆▄▁▅▂▆▃▇▄▁▅▂▆▃▇▄▁▅▂▅▇▄▁▄▁▅▂▇▄▁▅▂▆▄▁▅▂▆▄▁▅▂▆▄▁▅▂▆▃█▅▂▆▃▇▄▁▆▃▇▄▁▅▂▆▃▇▄▁▆▃▇▄▁▅▂▇▄▁▄▁▄▁▄▄▄▁▄▂▅▂▆▁▅▁▄▁▄▁▃▇▃▆▂▅▁▅▁▅▂▆▃▇▂▇▃▇▃▇▃▇▃▇▄▁▄▁▄▁▅▁▅▁▄▁▄▁▅▁▄▁▄▁▄▁▅▁▄▁▄▁▄▁▄▁▄▇▄▁▄▁▄▇▄▇▃▇▃▇▃▇▃▇▂▆▂▆▂▆▃▆▂▅▁▄▁▅▁▅▁▄▄▄▅▃▇▄▁▅▂▅▁▅▂▆:min/max:0/102
           :   4   6   1   7   1   1   2   4   1   2   5   7   4   8   3   6   9   2   6   2   6   3   4   8   2   6   9   3   6   1   3   6   0   0   0   1   0   0   8   6   8   8   9   9   7   3   0   5   0   2   0   0   0   1   9   9   7   8   6   6   5   4   6   6
           :   1   4   8   9   6   4   2   9   7   2   6   7   8       3   3   3   0       8   8       4   5   0   1   9   1   5   0   1   5               9           4   8   3   9   1   7                                       0   6   8   9   4   1   2   8   2   4   5
           :                                                                                                                       0                                                                                               1

Survivor Space:▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▂▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▂▃▃▆▅▅▆▃▅▂▂▁▃▄▅▅▅▆▅▄▅▆▇▆▆▆▆▅▄▃▄▃▁▂▃▂▆▄▃▄▄▄▆▃▂▂▃▃▂▂▄▁▃▅▅▇▅▄▆▁▂▃▅▅▄▄▄▃▂▅▆▃▃▄▃▃▅▆▅▃▃▅▄▂▄█▄▅▃▃▅▃▄▄▅▄▅▄▄▄▃▂▃▅▇█▆▄▃▂▄▃▄▄▆▃▄▅▄▁▂▂▃▅▅▂▁▁:min/max:0/7
              :   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   2   4   1   3   5   5   5   2   1   3   3   1   1   4   3   2   3   4   3   5   4   7   2   3   3   1   7   1   3   4   1   1

Old Space:▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁:min/max:0/0
         :   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0

Perm Space:▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁:min/max:7/7
          :   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7   7
```

### Random hack to remove the recenyQueue

Just a random hack to see what happens when the recencyQueue is a /dev/null type queue.  Not something that I would
recommend doing.  I was just curious is all to see the results.  Making the following local change to 'LocalCache'.
(Use of a PaddedAtomicInteger for readCount might be helpful for performance too)

```java
boolean usesAccessQueue() {
    return expiresAfterAccess() ;
//      || evictsBySize();
  }
```

results are:

```
Young Space:▁▅██:min/max:0/9
           :   9

Survivor Space:▁▁▁▁:min/max:0/0
              :   0

Old Space:▁▁▁▁:min/max:0/0
         :   0

Perm Space:▁▁▁▁:min/max:7/7
          :   7
```