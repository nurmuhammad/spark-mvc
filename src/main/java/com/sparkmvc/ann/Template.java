package com.sparkmvc.ann;

/**
 * Created by Nurmuhammad on 29-Dec-15.
 */
public @interface Template {

    TemplateEngine value() default TemplateEngine.FREEMARKER;

    String viewName() default Constants.NULL_VALUE;

    enum TemplateEngine {
        FREEMARKER, VLOCITY
    }

}