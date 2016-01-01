package com.sparkmvc.ann;

/**
 * Created by Nurmuhammad on 29-Dec-15.
 */
public @interface Template {

    TemplateEngine value() default TemplateEngine.FREEMARKER;

    enum TemplateEngine {
        FREEMARKER, VLOCITY
    }

}