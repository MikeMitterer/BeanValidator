/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package at.mikemitterer.bv;


import at.mikemitterer.bv.constraints.Constraint;
import at.mikemitterer.bv.validator.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * Valdaitors registration and Cache.
 * Bean Fields methods and validation annotation cache.
 *
 * @author Jiren
 */
final class ValidatorCache<P> {
    private static final Logger logger = LoggerFactory.getLogger(ValidatorCache.class.getSimpleName());

    /**
     * Map of Class name to  fileds annotations and their reflected getter method array.
     */
    private static final Map<String, AnnotationMetaData[]> classMethodsMap = new HashMap<>();

    /**
     * Annotation to Custome validator instance map.
     */
    private static final Map<Class<? extends Annotation>, Validate> validatorsMap = new HashMap<>();


    private ValidatorCache() {
    }

    /**
     * Get Class fields annotation and getter method object array.
     * - If Class is not present into cache than it add it else it return from
     * the cache.
     */
    public static AnnotationMetaData[] getFields(Object object) {

        String className = object.getClass().getName();
        if (!classMethodsMap.containsKey(className)) {
            addFieldsAndMethodsFromClass(object.getClass());
        }
        return classMethodsMap.get(className);
    }


    /**
     * Check the annotation is AValidation type.
     */
    public static boolean isAValidation(Class<? extends Annotation> annotation) {
        return annotation.isAnnotationPresent(Constraint.class);
    }


    /**
     * Get Validator from the map.
     */
    public static Validate getValidator(Annotation vAnnotation) {
        return validatorsMap.get(vAnnotation.annotationType());
    }

    // --------------------------------------------------------------------------------------------
    // private
    // --------------------------------------------------------------------------------------------

    // private static String getGetMethod(String fieldName) {
    //    return GETTER_METHOD_PREFIX + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    // }


    /**
     * Get field Get Method name.
     */
    private static String getGetMethod(Field field) {
        // Getter method prefix for all primitive and non-premitive datatype except
        // 'boolean' primitive data type
        final String GETTER_METHOD_PREFIX = "get";

        // Getter method prefix for 'boolean' datatype.
        final String GETTER_METHOD_PREFIX_B = "is";

        String prefix;
        String fieldName = field.getName();

        if (field.getType() != boolean.class) {
            prefix = GETTER_METHOD_PREFIX;

        } else {

            prefix = GETTER_METHOD_PREFIX_B;
        }

        return prefix + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1); //.toLowerCase();
    }

    /**
     * Get Annotation with AValidation.
     *
     * - getAnnotations basically gets all annotations that are also inherited from the parent class.
     * - getDeclaredAnnotations gets annotations declared ONLY on the class
     *
     * - See more at: http://djitz.com/neu-mscs/java-reflection-notes-getdeclaredannotations-vs-getannotations-method/#sthash.QxnGlgBC.dpuf
     */
    private static Annotation[] getAValidates(Field field) {
        return filterConstraintAnnotations(field.getAnnotations());
    }

    private static Annotation[] getAValidates(Method method) {
        return filterConstraintAnnotations(method.getAnnotations());
    }

    private static Annotation[] filterConstraintAnnotations(final Annotation[] annotations) {
        ArrayList<Annotation> list = new ArrayList<>();

        for (Annotation annotation : annotations) {

            if (annotation.annotationType().isAnnotationPresent(Constraint.class)) {
                list.add(annotation);
            }
        }

        return list.toArray(new Annotation[list.size()]);
    }

    /**
     * This method register validator.
     * - Get @AValidation annotation from the validation annotation.
     * - create instance of Validator object and add it to validators map.
     */
    private static void registerValidator(Annotation annotation) {
        final Class<? extends Annotation> vAnnotationClass = annotation.annotationType();
        try {
            Constraint constraint = vAnnotationClass.getAnnotation(Constraint.class);

            validatorsMap.put(vAnnotationClass, constraint.validator().newInstance());

            logger.debug("Registered Validator {}", annotation.toString());

        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error("registerValidator failed, Error: {}", ex.toString());

        }

    }

    /**
     * Add class fields 'get'(Getter) Methods and field annotation in Map.
     * - This method check annotation is annotated by 'AValidation' annotation.
     * Using this check at run time checking the annotation is for validation
     * or not.
     * <p/>
     * - This method add field into cache only if it's getter method has public
     * modifier.
     * <p/>
     * - This method check that validator already exist in validators map. If not
     * exist in it than register that validator.So no need to register validator
     * separately it loaded on demand basis.
     */
    private static void addFieldsAndMethodsFromClass(final Class klass) {

        try {
            Map<String, AnnotationMetaData> methodMap = new HashMap<>();

            //List<AnnotationMetaData> methodList = new ArrayList<AnnotationMetaData>();

            addMethodsToMethodList(klass, methodMap);
            addFieldsToMethodList(klass, methodMap);

            if (!methodMap.isEmpty()) {

                //Convert list into array and store in to map.
                classMethodsMap.put(klass.getName(), methodMap.values().toArray(new AnnotationMetaData[methodMap.size()]));
            }
        } catch (Exception ex) {
            logger.error("addFieldsAndMethodsFromClass failed, Error: {}", ex.toString());
        }

    }


    private static void addMethodsToMethodList(final Class klass, final Map<String, AnnotationMetaData> methodMap) {
        Method[] methods = klass.getMethods();

        for (final Method method : methods) {
            final Annotation[] annotations = getAValidates(method);

            if (annotations.length != 0) {

                for (final Annotation annotation : annotations) {
                    if (!validatorsMap.containsKey(annotation.annotationType())) {
                        registerValidator(annotation);
                    }
                }

                if (Modifier.isPublic(method.getModifiers())) {
                    methodMap.put(method.getName(), new AnnotationMetaData(method, annotations));
                } else {
                    logger.warn("Getter {} is not public...", method.getName());
                }

            }
        }
    }

    private static void addFieldsToMethodList(final Class klass, Map<String, AnnotationMetaData> methodMap) {
        Field[] fields = klass.getFields();
        for (final Field field : fields) {
            final Annotation[] annotations = getAValidates(field);

            if (annotations.length != 0) {

                for (final Annotation annotation : annotations) {
                    if (!validatorsMap.containsKey(annotation.annotationType())) {
                        registerValidator(annotation);
                    }
                }

                try {

                    final String methodName = getGetMethod(field);
                    //noinspection unchecked
                    Method method = klass.getDeclaredMethod(methodName);

                    if (Modifier.isPublic(method.getModifiers())) {
                        if (!methodMap.containsKey(methodName)) {
                            methodMap.put(methodName, new AnnotationMetaData(field, method, annotations));
                        } else {
                            logger.warn("Field annotation for {} is already defined with it's getter {}", field.getName(), methodName);
                        }
                    } else {
                        logger.warn("Field getter method has not public modifier: {}", field.getName());
                    }

                } catch (NoSuchMethodException ex) {
                    logger.error("addFieldsAndMethodsFromClass failed, Error: {}", ex.toString());
                }
            }
        }
    }

}
