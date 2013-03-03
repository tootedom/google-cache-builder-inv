package org.greencheek.annotations.service;

import java.lang.annotation.Annotation;

/**
 * User: dominictootell
 * Date: 27/02/2013
 * Time: 21:59
 */
public class BasicAnnotationReader implements AnnotationReader {

    @Override
    public Annotation[] getAnnotations(Class clazz) {
        return clazz.getAnnotations();
    }

    public void close(){}
}
