package com.sparkmvc.init;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sparkmvc.ann.*;
import com.sparkmvc.engine.FreeMarkerEngine;
import com.sparkmvc.engine.MustacheTemplateEngine;
import com.sparkmvc.engine.PebbleTemplateEngine;
import com.sparkmvc.helper.$;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.*;
import spark.template.velocity.VelocityTemplateEngine;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static spark.Spark.after;
import static spark.Spark.before;

/**
 * @author nurmuhammad
 */

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static Map<Class<?>, Object> controllersMap = new HashMap<>();
    public static Map<Template.TemplateEngine, TemplateEngine> templateMap = new HashMap<>();

    static Map<String, Method> methods = new HashMap<>();

    public static Gson GSON = new GsonBuilder().setExclusionStrategies(new AnnotationSkipStrategy()).create();

    public static void init() throws Throwable {

        before((request, response) -> {
            Context.set(Request.class, request);
            Context.set(Response.class, response);
            Context.set(Session.class, request.session());
        });

        collectMethods("get", "put", "post", "delete", "head", "connect", "options", "trace");

        Reflections reflections = new Reflections(Config.get("scan.package", "com"), new SubTypesScanner(), new TypeAnnotationsScanner(), new MethodAnnotationsScanner());

        Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(Controller.class);

        Set<Method> gets = reflections.getMethodsAnnotatedWith(GET.class);
        Set<Method> posts = reflections.getMethodsAnnotatedWith(POST.class);
        Set<Method> puts = reflections.getMethodsAnnotatedWith(PUT.class);
        Set<Method> deletes = reflections.getMethodsAnnotatedWith(DELETE.class);
        Set<Method> heads = reflections.getMethodsAnnotatedWith(HEAD.class);
        Set<Method> connects = reflections.getMethodsAnnotatedWith(CONNECT.class);
        Set<Method> options = reflections.getMethodsAnnotatedWith(OPTIONS.class);
        Set<Method> traces = reflections.getMethodsAnnotatedWith(TRACE.class);

        Set<Method> befores = reflections.getMethodsAnnotatedWith(Before.class);
        Set<Method> afters = reflections.getMethodsAnnotatedWith(After.class);

        Set<Method> inits = reflections.getMethodsAnnotatedWith(Init.class);

        for (Class<?> aClass : controllers) {
            Object instance = aClass.newInstance();

            inits.forEach(method -> {
                if (aClass.equals(method.getDeclaringClass())) {
                    try {
                        logger.info("SparkMVC: Initializing " + aClass.getName() + "." + method.getName() + "()...");
                        method.setAccessible(true);
                        method.invoke(instance);
                        return;
                    } catch (Exception e) {
                        logger.error("SparkMVC: Method throws when invoking @Init method. Class:" + aClass.getName() + ".... method:" + method.getName(), e);
                    }
                }
            });

            controllersMap.put(aClass, instance);
        }

        httpMethods(gets, GET.class);
        httpMethods(posts, POST.class);
        httpMethods(puts, PUT.class);
        httpMethods(deletes, DELETE.class);
        httpMethods(heads, HEAD.class);
        httpMethods(connects, CONNECT.class);
        httpMethods(options, OPTIONS.class);
        httpMethods(traces, TRACE.class);


        for (Method method : befores) {
            Class<?> aClass = method.getDeclaringClass();
            Object controller = controllersMap.get(aClass);
            Before before = method.getAnnotation(Before.class);
            initBeforeFilter(before, controller, method);
        }

        for (Method method : afters) {
            Class<?> aClass = method.getDeclaringClass();
            Object controller = controllersMap.get(aClass);
            After after = method.getAnnotation(After.class);
            initAfterFilter(after, controller, method);
        }
    }

    private static void collectMethods(String... methodNames) {
        for (String methodName : methodNames) {
            collectMethods(methodName);
        }
    }

    private static void collectMethods(String methodName) {

        try {
            methods.put(methodName, Spark.class.getDeclaredMethod(methodName, String.class, Route.class));
            methods.put(methodName + "_template", Spark.class.getDeclaredMethod(methodName, String.class, TemplateViewRoute.class, TemplateEngine.class));
            methods.put(methodName + "_json", Spark.class.getDeclaredMethod(methodName, String.class, Route.class, ResponseTransformer.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void httpMethods(Set<Method> methods, Class<? extends Annotation> annotationClass) throws Exception {
        for (Method method : methods) {
            Class<?> aClass = method.getDeclaringClass();
            Object controller = controllersMap.get(aClass);
            if (!method.isAnnotationPresent(annotationClass))
                continue;

            Annotation annotation = method.getAnnotation(annotationClass);
            String httpMethodName = annotation.annotationType().getSimpleName().toLowerCase();
            Method uriMethod = annotation.getClass().getDeclaredMethod("value");
            String uri = (String) uriMethod.invoke(annotation);
            Method absolutePathMethod = annotation.getClass().getDeclaredMethod("absolutePath");
            boolean absolutePath = (boolean) absolutePathMethod.invoke(annotation);

            logger.info("SparkMVC: Initializing @" + httpMethodName.toUpperCase() + " method by " + aClass.getName() + "." + method.getName() + "()");
            initMethod(httpMethodName, uri, controller, method, absolutePath);
        }
    }

    public static void initMethod(String httpMethodName, String uri, Object instance, Method method, boolean absolutePath) throws InvocationTargetException, IllegalAccessException {
        Controller controller = instance.getClass().getAnnotation(Controller.class);
        String path = path(absolutePath ? "" : controller.url(), uri, method);
        logger.info("SparkMVC: " + httpMethodName + "(uri = " + path + ")");

        Template template = method.getAnnotation(Template.class);
        Json json = method.getAnnotation(Json.class);

        final Cacheable cacheable = method.getAnnotation(Cacheable.class);

        if (template == null && json == null) {
            Method sparkMethod = methods.get(httpMethodName);
            if (cacheable == null) {
                sparkMethod.invoke(null, path, (Route) (request, response) -> methodInvoke(method, instance, request, response));
            } else {
                sparkMethod.invoke(null, path, (Route) (request, response) -> {
                    String cacheKey = cacheKey(instance, request);

                    Object result = Cache.get(cacheKey);
                    if (result != null)
                        return result;

                    result = methodInvoke(method, instance, request, response);
                    return Cache.put(cacheKey, result, cacheable.expire());
                });
            }
            return;
        }

        if (template != null && cacheable == null) {
            final String viewName = template.viewName();
            Method sparkMethod = methods.get(httpMethodName + "_template");
            sparkMethod.invoke(null, path, (TemplateViewRoute) (request, response) -> {
                Object result = methodInvoke(method, instance, request, response);
                if (result instanceof ModelAndView) {
                    return (ModelAndView) result;
                }
                return Spark.modelAndView(result, viewName);
            }, getTemplateEngine(template.value()));
            return;
        } else if (template != null) {

            final String viewName = template.viewName();
            Method sparkMethod = methods.get(httpMethodName);
            TemplateEngine engine = getTemplateEngine(template.value());

            sparkMethod.invoke(null, path, (Route) (request, response) -> {
                String cacheKey = cacheKey(instance, request);
                Object result = Cache.get(cacheKey);
                if (result != null)
                    return result;

                result = methodInvoke(method, instance, request, response);
                if (result instanceof ModelAndView) {
                    result = engine.render(result);
                } else {
                    result = engine.render(Spark.modelAndView(result, viewName));
                }

                return Cache.put(cacheKey, result, cacheable.expire());

            });
            return;
        }

        if (cacheable == null) {
            Method sparkMethod = methods.get(httpMethodName + "_json");
            sparkMethod.invoke(null, path, (Route) (request, response) -> {
                response.type("application/json");
                return methodInvoke(method, instance, request, response);
            }, (ResponseTransformer) GSON::toJson);
        } else {
            Method sparkMethod = methods.get(httpMethodName);
            sparkMethod.invoke(null, path, (Route) (request, response) -> {
                response.type("application/json");
                String cacheKey = cacheKey(instance, request);
                Object result = Cache.get(cacheKey);
                if (result != null)
                    return result;

                result = methodInvoke(method, instance, request, response);
                result = GSON.toJson(result);

                return Cache.put(cacheKey, result, cacheable.expire());

            });
        }

    }

    public static void initBeforeFilter(Before before, Object instance, Method method) throws Throwable {

        logger.info("SparkMVC: Initializing @Before filter to " + instance.getClass().getName() + "." + method.getName());

        Controller controller = instance.getClass().getAnnotation(Controller.class);

        if (before.value().length == 0) {
            String regex = "([a-z])([A-Z])";
            String replacement = "$1-$2";
            String methodUri = method.getName().replaceAll(regex, replacement).toLowerCase();
            String path = (controller.url() + "/" + methodUri + "/*")
                    .replaceAll("//", "/").replaceAll("//", "/");
            path = path.startsWith("/") ? path : "/" + path;
            before(path, (request, response) -> methodInvoke(method, instance, request, response));
            return;
        }

        for (Uri uri : before.value()) {
            if (uri.absolutePath() && Constants.NULL_VALUE.equals(uri.value())) {
                before((request, response) -> methodInvoke(method, instance, request, response));
                continue;
            }

            if (uri.absolutePath()) {
                String path = uri.value().startsWith("/") ? uri.value() : "/" + uri.value();
                before(path, (request, response) -> methodInvoke(method, instance, request, response));
                continue;
            }

            String path = (controller.url() + "/" + (Constants.NULL_VALUE.equals(uri.value()) ? "*" : uri.value()))
                    .replaceAll("//", "/").replaceAll("//", "/");
            path = path.startsWith("/") ? path : "/" + path;
            before(path, (request, response) -> methodInvoke(method, instance, request, response));
        }

    }

    public static void initAfterFilter(After after, Object instance, Method method) {

        logger.info("SparkMVC: Initializing @After filter to " + instance.getClass().getName() + "." + method.getName());

        Controller controller = instance.getClass().getAnnotation(Controller.class);

        if (after.value().length == 0) {
            String regex = "([a-z])([A-Z])";
            String replacement = "$1-$2";
            String methodUri = method.getName().replaceAll(regex, replacement).toLowerCase();
            String path = (controller.url() + "/" + methodUri + "/*")
                    .replaceAll("//", "/").replaceAll("//", "/");
            path = path.startsWith("/") ? path : "/" + path;
            after(path, (request, response) -> methodInvoke(method, instance, request, response));
            return;
        }

        for (Uri uri : after.value()) {
            if (uri.absolutePath() && Constants.NULL_VALUE.equals(uri.value())) {
                after((request, response) -> methodInvoke(method, instance, request, response));
                continue;
            }

            if (uri.absolutePath()) {
                String path = uri.value().startsWith("/") ? uri.value() : "/" + uri.value();
                after(path, (request, response) -> methodInvoke(method, instance, request, response));
                continue;
            }

            String path = (controller.url() + "/" + (Constants.NULL_VALUE.equals(uri.value()) ? "*" : uri.value()))
                    .replaceAll("//", "/").replaceAll("//", "/");
            path = path.startsWith("/") ? path : "/" + path;
            after(path, (request, response) -> methodInvoke(method, instance, request, response));
        }
    }

    private static Object methodInvoke(Method method, Object instance, Object... args) throws Exception {
        try {
            method.setAccessible(true);
            if (method.getParameterCount() > 0) {
                return method.invoke(instance, args);
            } else {
                return method.invoke(instance);
            }
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
    }

    static String path(String controllerUri, String methodUri, Method method) {
        if (Constants.NULL_VALUE.equals(methodUri)) {
            String regex = "([a-z])([A-Z])";
            String replacement = "$1-$2";
            methodUri = method.getName().replaceAll(regex, replacement).toLowerCase();
        }

        String path = (controllerUri + "/" + methodUri)
                .replaceAll("//", "/").replaceAll("//", "/");
        return (path.startsWith("/")) ? path : "/" + path;

    }

    static String cacheKey(Object controller, Request request) {

        StringBuilder builder = new StringBuilder("URL.")
                .append(controller.getClass().getName())
                .append(".")
                .append(request.requestMethod())
                .append(".")
                .append($.url());

        return builder.toString();
    }

    static TemplateEngine getTemplateEngine(Template.TemplateEngine template) {
        TemplateEngine templateEngine = templateMap.get(template);
        if (templateEngine == null) {
            switch (template) {
                case VELOCITY:
                    templateEngine = new VelocityTemplateEngine();
                    break;
                case MUSTACHE:
                    templateEngine = new MustacheTemplateEngine();
                    break;
                case PEBBLE:
                    templateEngine = new PebbleTemplateEngine();
                    break;
                default:
                    templateEngine = new FreeMarkerEngine();
            }
            templateMap.put(template, templateEngine);
        }

        return templateEngine;
    }

}