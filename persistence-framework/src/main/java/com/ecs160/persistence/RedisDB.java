package com.ecs160.persistence;

import java.lang.annotation.Annotation;
import com.ecs160.persistence.annotations.PersistableObject;
import com.ecs160.persistence.annotations.PersistableField;
import com.ecs160.persistence.annotations.Id;
import redis.clients.jedis.Jedis;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

public class RedisDB {

    private Jedis jedisSession;
    private static RedisDB instance;

    private RedisDB() {
        jedisSession = new Jedis("localhost", 6379);
    }

    public static RedisDB getInstance() {
        if (instance == null) {
            instance = new RedisDB();
        }
        return instance;
    }

    public boolean persist(Object obj) {
        if (obj == null) {
            return false;
        }

        Class<?> clazz = obj.getClass();
        if (!clazz.isAnnotationPresent(PersistableObject.class)) {
            return false;
        }

        try {
            // generating redis key here from the object
            String redisKey = generateRedisKey(obj, clazz);
            if (redisKey == null) {
                return false;
            }

            // hash map for redis
            Map<String, String> hashMap = new HashMap<>();

            // processing all @PersistableField fields, skipping @Id field since its used for the key not stored
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(PersistableField.class)) {
                    continue;
                }
                
                // skipping @Id field since we used it for the redis key not stored as a field
                if (field.isAnnotationPresent(Id.class)) {
                    continue;
                }

                field.setAccessible(true);
                Object value = field.get(obj);

                if (value == null) {
                    continue;
                }

                String fieldName = field.getName();
                String redisFieldName = mapFieldNameToRedis(fieldName);

                // handling list , these are the child objects
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    if (!list.isEmpty()) {
                        // persisting child objects recursively- this is the list of child objects
                        List<String> childIds = new ArrayList<>();
                        for (Object child : list) {
                            if (child != null) {
                                // recursively persisting child object
                                persist(child);
                                // getting child's id for issue we need to extract just the number part
                                String childId = getIdValue(child);
                                if (childId != null) {
                                    // if it's an issue id in format "issue:{language}:{number}" extracting just the number
                                    if (childId.startsWith("issue:") && childId.contains(":")) {
                                        String[] parts = childId.split(":");
                                        if (parts.length >= 3) {
                                            childId = parts[2]; // extracting the number part
                                        }
                                    }
                                    childIds.add(childId);
                                }
                            }
                        }
                        // storing comma-separated ids
                        hashMap.put(redisFieldName, String.join(",", childIds));
                    }
                } else {
                    // converting value to string
                    String stringValue = convertToString(value);
                    hashMap.put(redisFieldName, stringValue);
                }
            }

            // storing in redis as hash
            if (!hashMap.isEmpty()) {
                jedisSession.hset(redisKey, hashMap);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return false;
    }

    public Object load(Object object) {
        if (object == null) {
            return null;
        }

        Class<?> clazz = object.getClass();
        if (!clazz.isAnnotationPresent(PersistableObject.class)) {
            return null;
        }

        try {
            // getting id value from object
            String idValue = getIdValue(object);
            if (idValue == null) {
                return null;
            }

            // generating redis key
            String redisKey = generateRedisKey(object, clazz);
            if (redisKey == null) {
                return null;
            }

            // checking if key exists
            if (!jedisSession.exists(redisKey)) {
                return null;
            }

            // loading all fields from redis hash
            Map<String, String> hashMap = jedisSession.hgetAll(redisKey);
            if (hashMap.isEmpty()) {
                return null;
            }

            // creating new instance
            Object result = clazz.getDeclaredConstructor().newInstance();

            // setting all @PersistableField fields
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(PersistableField.class)) {
                    continue;
                }

                field.setAccessible(true);
                String fieldName = field.getName();
                String redisFieldName = mapFieldNameToRedis(fieldName);
                String redisValue = hashMap.get(redisFieldName);

                if (redisValue == null || redisValue.isEmpty()) {
                    continue;
                }

                // handling list (child objects)
                if (List.class.isAssignableFrom(field.getType())) {
                    String[] childIds = redisValue.split(",");
                    List<Object> childList = new ArrayList<>();

                    Class<?> childType = getListElementType(field, clazz);
                    if (childType != null) {
                        String parentLanguage = null;
                        if (clazz.getSimpleName().equals("Repo") && field.getName().equals("issues")) {
                            parentLanguage = (String) clazz.getDeclaredField("language").get(result);
                        }

                        for (String childId : childIds) {
                            String trimmedId = childId.trim();
                            if (!trimmedId.isEmpty()) {
                                Object childObj = childType.getDeclaredConstructor().newInstance();
                                Field idField = findIdField(childType);
                                if (idField != null) {
                                    idField.setAccessible(true);
                                    
                                    // constructing full redis key for issue
                                    String fullKey;
                                    if (childType.getSimpleName().equals("Issue") && parentLanguage != null) {
                                        fullKey = "issue:" + parentLanguage + ":" + trimmedId;
                                    } else {
                                        fullKey = trimmedId;
                                    }
                                    
                                    setFieldValue(idField, childObj, fullKey);

                                    // recursively loading child
                                    Object loadedChild = load(childObj);
                                    if (loadedChild != null) {
                                        childList.add(loadedChild);
                                    }
                                }
                            }
                        }
                    }
                    field.set(result, childList);
                } else {
                    // converting from string to field type
                    Object convertedValue = convertFromString(redisValue, field.getType());
                    field.set(result, convertedValue);
                }
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // helper methods

    private String generateRedisKey(Object obj, Class<?> clazz) throws Exception {
        // extracting id value
        String idValue = getIdValue(obj);
        if (idValue == null) {
            return null;
        }

        // checking if id already contains the full redis key format
        if (idValue.contains(":")) {
            return idValue;
        }

        // otherwise need to construct key based on class type
        String className = clazz.getSimpleName();
        if (className.equals("Repo")) {
            // need language trying to get from object
            Field langField = clazz.getDeclaredField("language");
            langField.setAccessible(true);
            String language = (String) langField.get(obj);
            if (language == null) {
                // trying to get from redis if object was partially loaded
                return null;
            }
            // extracting index from id or using id as index
            return "repos:" + language + ":" + idValue;
        } else if (className.equals("Issue")) {
            // issue id should already be in format "issue:{language}:{number}"
            // if it's just a number thenwe can't construct the key without language
            if (idValue.contains(":")) {
                return idValue;
            }
            return null;
        }

        return null;
    }

    private String getIdValue(Object obj) throws Exception {
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                Object value = field.get(obj);
                return value != null ? value.toString() : null;
            }
        }
        return null;
    }

    private Field findIdField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        return null;
    }

    private String mapFieldNameToRedis(String fieldName) {
        // mapping java field names to redis field names here
        switch (fieldName) {
            case "ownerLogin":
                return "owner";
            case "htmlUrl":
                return "url";
            case "stargazersCount":
                return "stars";
            case "forksCount":
                return "forks";
            case "openIssuesCount":
                return "openIssues";
            case "commitsAfterForkCount":
                return "commitsAfterFork";
            case "body":
                return "Description";
            default:
                return fieldName;
        }
    }

    private String convertToString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof ZonedDateTime) {
            return value.toString();
        }
        return value.toString();
    }

    private Object convertFromString(String value, Class<?> targetType) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        if (targetType == String.class) {
            return value;
        } else if (targetType == Integer.class || targetType == int.class) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else if (targetType == ZonedDateTime.class) {
            try {
                return ZonedDateTime.parse(value);
            } catch (Exception e) {
                return null;
            }
        }
        return value;
    }

    private Class<?> getListElementType(Field field, Class<?> containingClass) {
        // trying to determine element type from field name or annotation
        String fieldName = field.getName();
        if (fieldName.equals("issues")) {
            try {
                return Class.forName("com.ecs160.hw.model.Issue");
            } catch (ClassNotFoundException e) {
                // trying alternative package
                try {
                    return Class.forName("com.ecs160.model.Issue");
                } catch (ClassNotFoundException e2) {
                    return null;
                }
            }
        } else if (fieldName.equals("forks")) {
            try {
                return Class.forName("com.ecs160.hw.model.Repo");
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    private void setFieldValue(Field field, Object obj, String value) throws Exception {
        Class<?> fieldType = field.getType();
        Object convertedValue = convertFromString(value, fieldType);
        field.set(obj, convertedValue);
    }
}
