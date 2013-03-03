package org.greencheek.annotations.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.sun.xml.internal.xsom.parser.AnnotationParserFactory;
import org.greencheek.annotations.domain.ClassWithAnnotations;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * User: dominictootell
 * Date: 27/02/2013
 * Time: 22:02
 */
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
