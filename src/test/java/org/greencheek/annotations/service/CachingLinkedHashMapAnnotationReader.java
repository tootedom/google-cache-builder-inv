package org.greencheek.annotations.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import java.lang.annotation.Annotation;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: dominictootell
 * Date: 27/02/2013
 * Time: 22:02
 */
public class CachingLinkedHashMapAnnotationReader implements AnnotationReader {

    private final ConcurrentMap<Class,Annotation[]> cache;
    private final AtomicInteger access = new AtomicInteger(0);

    public CachingLinkedHashMapAnnotationReader()  {
         cache = new ConcurrentLinkedHashMap.Builder<Class,Annotation[]>()
                .maximumWeightedCapacity(1000)
                .build();

    }

    @Override
    public Annotation[] getAnnotations(final Class clazz) {
        if(cache.containsKey(clazz)) {
            return cache.get(clazz);
        } else {
            Annotation[] anos = clazz.getAnnotations();
            Annotation[] prev = cache.putIfAbsent(clazz,anos);
            return prev==null? anos : prev;
        }
    }

    public void close() {
        cache.clear();
    }

}
