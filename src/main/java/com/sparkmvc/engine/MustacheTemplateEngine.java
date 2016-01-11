package com.sparkmvc.engine;

/*
 * Copyright 2014
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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import com.sparkmvc.helper.$;
import com.sparkmvc.init.Config;
import org.eclipse.jetty.io.RuntimeIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;
import spark.TemplateEngine;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

/**
 * Defaults to the 'templates' directory under the resource path.
 *
 * @author Sam Pullara https://github.com/spullara
 */
public class MustacheTemplateEngine extends TemplateEngine {

    private static final Logger logger = LoggerFactory.getLogger(MustacheTemplateEngine.class);

    private MustacheFactory mustacheFactory;

    /**
     * Constructs a mustache template engine
     */
    public MustacheTemplateEngine() {

        logger.info("SparkMVC: Seeking Mustache templates folder -------------->");

        String dir = Config.get("template.mustache.dir", $.templateFolder());
        if (dir != null) {
            mustacheFactory = new DefaultMustacheFactory(new File(dir));
            logger.info("SparkMVC: Uses '" + dir + "' path for Mustache templates...");
        } else {
            mustacheFactory = new DefaultMustacheFactory("template");
        }
    }

    /**
     * Constructs a mustache template engine
     *
     * @param resourceRoot the resource root
     */
    public MustacheTemplateEngine(String resourceRoot) {
        mustacheFactory = new DefaultMustacheFactory(resourceRoot);
    }

    /**
     * Constructs a mustache template engine
     *
     * @param mustacheFactory the mustache factory
     */
    public MustacheTemplateEngine(MustacheFactory mustacheFactory) {
        this.mustacheFactory = mustacheFactory;
    }

    @Override
    public String render(ModelAndView modelAndView) {
        String viewName = modelAndView.getViewName();
        Mustache mustache = mustacheFactory.compile(viewName);
        StringWriter stringWriter = new StringWriter();
        try {
            mustache.execute(stringWriter, modelAndView.getModel()).close();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        return stringWriter.toString();
    }
}

