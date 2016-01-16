package com.sparkmvc.helper;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.sparkmvc.init.Context;
import spark.Request;
import spark.Response;
import spark.Session;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;

/**
 * Created by Nurmuhammad on 10-Jan-16.
 */

public class $ {

    public static boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String)
            return ((String) value).trim().length() == 0;
        if (value instanceof Collection) return ((Collection) value).isEmpty();
        return (value instanceof Map) && ((Map) value).isEmpty();
    }

    public static String md5(String value) {
        HashFunction hf = Hashing.md5();
        HashCode hc = hf.newHasher()
                .putString(value, Charsets.UTF_8)
                .hash();

        return hc.toString();
    }

    public static String b64encode(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    public static String b64decode(String s) {
        return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
    }

    public static String runFolder() {
        String runFolder;
        try {
            runFolder = URLDecoder.decode(ClassLoader.getSystemClassLoader().getResource(".").getPath(), "UTF-8");
            File tempFile = new File(runFolder);
            if (tempFile.isDirectory()) {
                runFolder = tempFile.getPath();
            } else runFolder = null;
        } catch (Throwable ignored) {
            runFolder = null;
        }

        try {
            if (runFolder == null) {
                File file = new java.io.File($.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
                if (file.exists() && file.isFile()) {
                    file = file.getParentFile();
                }
                runFolder = file.getPath();
                File tempFile = new File(runFolder);
                if (tempFile.isDirectory()) {
                    runFolder = tempFile.getPath();
                } else runFolder = null;
            }
        } catch (Throwable ignored) {
            runFolder = null;
        }

        return runFolder;
    }

    public static String templateFolder() {
        String templateDir;
        try {
            templateDir = URLDecoder.decode(ClassLoader.getSystemClassLoader().getResource(".").getPath(), "UTF-8");
            templateDir += File.separator + "template";
            File tempFile = new File(templateDir);
            if (tempFile.isDirectory()) {
                templateDir = tempFile.getPath();
            } else templateDir = null;
        } catch (Throwable ignored) {
            templateDir = null;
        }

        try {
            if (templateDir == null) {
                File file = new java.io.File($.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
                if (file.exists() && file.isFile()) {
                    file = file.getParentFile();
                }
                templateDir = file.getPath();
                templateDir += File.separator + "template";
                File tempFile = new File(templateDir);
                if (tempFile.isDirectory()) {
                    templateDir = tempFile.getPath();
                } else templateDir = null;
            }
        } catch (Throwable ignored) {
            templateDir = null;
        }

        return templateDir;
    }

    public static <T> T get(Class<T> type) {
        return Context.get(type);
    }

    public static Request request() {
        return Context.get(Request.class);
    }

    public static Response response() {
        return Context.get(Response.class);
    }

    public static Session session() {
        return Context.get(Session.class);
    }

    public static String url() {
        StringBuilder builder = new StringBuilder(request().scheme())
                .append("://")
                .append(request().host());

        if (request().pathInfo() != null) {
            builder.append(request().pathInfo());
        }
        if (request().queryString() != null) {
            builder.append("?")
                    .append(request().queryString());
        }

        return builder.toString();
    }

    public static String path() {
        StringBuilder builder = new StringBuilder();

        if (request().pathInfo() != null) {
            builder.append(request().pathInfo());
        } else {
            builder.append("/");
        }

        if (request().queryString() != null) {
            builder.append("?")
                    .append(request().queryString());
        }

        return builder.toString();
    }
}
