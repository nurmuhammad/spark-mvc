package com.sparkmvc.init;

import com.sparkmvc.ann.Before;
import com.sparkmvc.ann.Controller;
import com.sparkmvc.ann.GET;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import spark.Spark;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Nurmuhammad on 01-Jan-16.
 */
public class Application {

    public static Map<Class<?>, Object> controllersMap = new HashMap<>();

    public static void init() throws Throwable {

        Reflections reflections = new Reflections(Config.get("scan.package"), new SubTypesScanner(), new TypeAnnotationsScanner(), new MethodAnnotationsScanner());

        Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(Controller.class);

        Set<Method> gets = reflections.getMethodsAnnotatedWith(GET.class);

        for (Class<?> aClass : controllers) {
            Object obj = aClass.newInstance();
            controllersMap.put(aClass, obj);
        }

        for (Method method : gets) {
            System.out.println(method.getName());
            Class<?> aClass = method.getDeclaringClass();
            Object controller = controllersMap.get(aClass);
            initGetMethod(aClass, controller, method);
        }

    }

    public static void initBeforeMethod(Class aClass, Object instance, Method method) {
        Before get = method.getAnnotation(Before.class);
        Spark.before(get.uri(), (request, response) -> {
            method.invoke(instance, request, response);
        });
    }

    public static void initGetMethod(Class aClass, Object instance, Method method) {
        GET get = method.getAnnotation(GET.class);
        Spark.get(get.uri(), (request, response) -> {
            return method.invoke(instance, request, response);
//            return "";
        });
    }



}