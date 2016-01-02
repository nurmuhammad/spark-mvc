package com.sparkmvc.model;

import com.sparkmvc.init.Config;
import org.reflections.Reflections;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Nurmuhammad on 27-Nov-15.
 */

public abstract class aModel implements Serializable {

    private static final Map<Class<? extends aModel>, Map<String, String>> columnMaps = new HashMap<>();

    public static Map<Class<? extends aModel>, Map<String, String>> getColumnMaps() {

        if(!columnMaps.isEmpty()){
            return columnMaps;
        }

        Reflections reflections = new Reflections(Config.get("scan.package", "com"));

        Set<Class<? extends aModel>> classes = reflections.getSubTypesOf(aModel.class);

        final String regex = "([a-z])([A-Z])";
        final String replacement = "$1_$2";
        classes.stream().forEach(aClass -> {
            for (Field field: aClass.getFields()){
                String name = field.getName();
                name = name.replaceAll(regex, replacement).toLowerCase();
                if(field.getName().equals(name)) continue;
                Map<String, String> map = columnMaps.get(aClass);
                if(map==null){
                    map = new HashMap<>();
                    columnMaps.put(aClass, map);
                }
                map.put(name, field.getName());
            }
        });

        return columnMaps;
    }
}
