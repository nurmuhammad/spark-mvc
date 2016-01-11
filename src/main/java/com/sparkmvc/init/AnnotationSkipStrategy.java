package com.sparkmvc.init;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.sparkmvc.ann.Skip;

/**
 * @author Nurmuhammad
 */

public class AnnotationSkipStrategy implements ExclusionStrategy {

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        return f.getAnnotation(Skip.class) != null;
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return clazz.getAnnotation(Skip.class) != null;
    }

}
