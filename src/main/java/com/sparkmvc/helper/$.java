package com.sparkmvc.helper;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.io.File;
import java.net.URLDecoder;
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
}
