package org.greencheek.annotations.service;

import java.lang.annotation.Annotation;

/**
 * User: dominictootell
 * Date: 27/02/2013
 * Time: 21:59
 */
public interface AnnotationReader
{
    Annotation[] getAnnotations(Class clazz);
    void close();
}
