package com.sparkmvc.engine;

/*
 * Copyright 2015 - Per Wendel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.mustachejava.DefaultMustacheFactory;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.Loader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.sparkmvc.helper.$;
import com.sparkmvc.init.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;
import spark.TemplateEngine;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * Template Engine based on Pebble.
 *
 * @author Nikki
 */
public class PebbleTemplateEngine extends TemplateEngine {

    private static final Logger logger = LoggerFactory.getLogger(PebbleTemplateEngine.class);

    /**
     * The Pebble Engine instance.
     */
    private final PebbleEngine engine;

    /**
     * Construct a new template engine using pebble with a default engine.
     */
    public PebbleTemplateEngine() {

        logger.info("SparkMVC: Seeking Pebble templates folder -------------->");

        Loader loader;
        String dir = Config.get("template.pebble.dir", $.templateFolder());
        if (dir != null) {
            loader = new FileLoader();
            loader.setPrefix(dir);
            logger.info("SparkMVC: Uses '" + dir + "' path for Pebble templates...");
        } else {
            loader = new ClasspathLoader();
        }

        this.engine = new PebbleEngine.Builder().loader(loader).build();
    }

    /**
     * Construct a new template engine using pebble with a specified engine.
     *
     * @param engine The pebble template engine.
     */
    public PebbleTemplateEngine(PebbleEngine engine) {
        this.engine = engine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public String render(ModelAndView modelAndView) {
        Object model = modelAndView.getModel();

        if (model == null || model instanceof Map) {
            try {
                StringWriter writer = new StringWriter();

                PebbleTemplate template = engine.getTemplate(modelAndView.getViewName());
                if (model == null) {
                    template.evaluate(writer);
                } else {
                    template.evaluate(writer, (Map<String, Object>) modelAndView.getModel());
                }

                return writer.toString();
            } catch (PebbleException | IOException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            throw new IllegalArgumentException("Invalid model, model must be instance of Map.");
        }
    }
}
