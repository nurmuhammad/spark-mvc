package com.sparkmvc.init;

import com.google.gson.Gson;
import com.sparkmvc.ann.*;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.*;
import spark.template.freemarker.FreeMarkerEngine;
import spark.template.velocity.VelocityTemplateEngine;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Nurmuhammad on 01-Jan-16.
 */
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static Map<Class<?>, Object> controllersMap = new HashMap<>();
    public static Map<Template.TemplateEngine, TemplateEngine> templateMap = new HashMap<>();

    static {
        templateMap.put(Template.TemplateEngine.FREEMARKER, new FreeMarkerEngine());
        templateMap.put(Template.TemplateEngine.VLOCITY, new VelocityTemplateEngine());
    }

    static Map<String, Method> methods = new HashMap<>();

    public static Gson GSON = new Gson();

    public static void init() throws Throwable {

        collectMethods("get", "put", "post", "delete", "head", "connect", "options", "trace");

        Reflections reflections = new Reflections(Config.get("scan.package", "com"), new SubTypesScanner(), new TypeAnnotationsScanner(), new MethodAnnotationsScanner());

        Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(Controller.class);

        Set<Method> gets = reflections.getMethodsAnnotatedWith(GET.class);
        Set<Method> puts = reflections.getMethodsAnnotatedWith(PUT.class);
        Set<Method> posts = reflections.getMethodsAnnotatedWith(POST.class);
        Set<Method> deletes = reflections.getMethodsAnnotatedWith(DELETE.class);
        Set<Method> connects = reflections.getMethodsAnnotatedWith(CONNECT.class);
        Set<Method> heads = reflections.getMethodsAnnotatedWith(HEAD.class);
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
            Method uriMethod = annotation.getClass().getDeclaredMethod("uri");
            String uri = (String) uriMethod.invoke(annotation);

            logger.info("SparkMVC: Initializing @" + httpMethodName.toUpperCase() + " method by " + aClass.getName() + "." + method.getName() + "()");
            initMethod(httpMethodName, uri, controller, method);
        }
    }

    public static void initMethod(String httpMethodName, String uri, Object instance, Method method) throws InvocationTargetException, IllegalAccessException {
        Controller controller = instance.getClass().getAnnotation(Controller.class);
        String path = path(controller.url(), uri, method);
        logger.info("SparkMVC: uri = " + path);

        Template template = null;
        if (method.isAnnotationPresent(Template.class)) {
            template = method.getAnnotation(Template.class);
        }

        Json json = null;
        if (method.isAnnotationPresent(Json.class)) {
            json = method.getAnnotation(Json.class);
        }

        if (template == null && json == null) {
            Method sparkMethod = methods.get(httpMethodName);
            sparkMethod.invoke(null, path, (Route) (request, response) -> method.invoke(instance, request, response));
            return;
        }

        if (template != null) {
            final String viewName = template.viewName();
            Method sparkMethod = methods.get(httpMethodName + "_template");
            sparkMethod.invoke(null, path, (TemplateViewRoute) (request, response) -> {
                Object result = method.invoke(instance, request, response);
                if (result instanceof ModelAndView) {
                    return (ModelAndView) result;
                }
                return Spark.modelAndView(result, viewName);
            }, templateMap.get(template.value()));
            return;
        }

        Method sparkMethod = methods.get(httpMethodName + "_json");
        sparkMethod.invoke(null, path, (Route) (request, response) -> {
            response.type("application/json");
            return method.invoke(instance, request, response);
        }, (ResponseTransformer) GSON::toJson);

    }

    static String path(String controllerUri, String methodUri, Method method) {
        if (Constants.NULL_VALUE.equals(methodUri)) {
            String regex = "([a-z])([A-Z])";
            String replacement = "$1-$2";
            methodUri = method.getName().replaceAll(regex, replacement).toLowerCase();
        }
        return (controllerUri + "/" + methodUri)
                .replaceAll("//", "/").replaceAll("//", "/");
    }

}