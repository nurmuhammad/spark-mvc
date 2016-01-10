package com.sparkmvc.engine;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import com.sparkmvc.init.Config;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import spark.ModelAndView;
import spark.TemplateEngine;

/**
 * Renders HTML from Route output using FreeMarker.
 * FreeMarker configuration can be set with the {@link FreeMarkerEngine#setConfiguration(Configuration)}
 * method. If no configuration is set the default configuration will be used where
 * ftl files need to be put in directory spark/template/freemarker under the resources directory.
 *
 * @author Alex
 * @author Per Wendel
 */
public class FreeMarkerEngine extends TemplateEngine {

    /**
     * The FreeMarker configuration
     */
    private Configuration configuration;

    /**
     * Creates a FreeMarkerEngine
     */
    public FreeMarkerEngine() {
        this.configuration = createDefaultConfiguration();
    }

    /**
     * Creates a FreeMarkerEngine with a configuration
     *
     * @param configuration The Freemarker configuration
     */
    public FreeMarkerEngine(Configuration configuration) {
        this.configuration = configuration;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String render(ModelAndView modelAndView) {
        try {
            StringWriter stringWriter = new StringWriter();

            Template template = configuration.getTemplate(modelAndView.getViewName());
            template.process(modelAndView.getModel(), stringWriter);

            return stringWriter.toString();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } catch (TemplateException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Sets FreeMarker configuration.
     * Note: If configuration is not set the default configuration
     * will be used.
     *
     * @param configuration the configuration to set
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    private Configuration createDefaultConfiguration() {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
        configuration.setClassForTemplateLoading(FreeMarkerEngine.class, "");
        try {
            String dir = Config.get("template.freemarker.dir", null);
            if (dir != null) {
                configuration.setDirectoryForTemplateLoading(new File(dir));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return configuration;
    }

}

