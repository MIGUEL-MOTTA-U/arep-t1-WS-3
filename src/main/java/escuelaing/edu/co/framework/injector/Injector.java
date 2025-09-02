package escuelaing.edu.co.framework.injector;

import escuelaing.edu.co.framework.annotations.*;
import escuelaing.edu.co.framework.services.implementations.HTTPServerImpl;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.logging.Logger;

public class Injector {
    private final Set<String> registeredPaths;
    private final Map<String, Object> injectedClasses;
    private final Reflections reflections;
    private static final Logger logger = Logger.getLogger(Injector.class.getName());
    private static Injector instance;

    private Injector(String packageName) {
        injectedClasses = new HashMap<>();
        registeredPaths = new HashSet<>();
        reflections = new Reflections(packageName);
    }

    public static void startApp(Class<?> mainClass) {
        try{
            synchronized (Injector.class) {
                if (instance == null){
                    instance = new Injector(mainClass.getPackageName());
                }
                instance.init();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * This method initiates the Injector with the provided
     * main Class (The Main Class should be at the root of the src files).
     * (and Server if @ResteController is detected)
     */
    public void init() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException{
        solveDependencies();
        solveControllers();
    }

    private void solveControllers() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        System.out.println("Starting Controller Injection");
        Set<Class<?>> restControllerAnnotation = reflections.getTypesAnnotatedWith(RestController.class);
        if (restControllerAnnotation.isEmpty()) {
            System.out.println("No RestControllers found");
            return;
        }
        for (Class<?> c : restControllerAnnotation) {
            injectClass(c);
            String rootPath = c.getAnnotation(RestController.class).value();
            solveGetMappings(c, rootPath);
        }
        System.out.println("Starting Server");
        HTTPServerImpl.start(8080);

    }

    private void solveGetMappings(Class<?> c, String path) {
        for (Method m: c.getDeclaredMethods()) {
            if(m.isAnnotationPresent(GetMapping.class)) {
                registerGetMapping(m, c, path);
            }
        }
    }

    private Parameter validateParameters(Method m) {
        for (Parameter p: m.getParameters()) {
            if (p.isAnnotationPresent(RequestParam.class)) {
                return p;
            }
        }
        return null;
    }

    private void registerGetMapping(Method m, Class<?> c, String path) {
        String pathValue = path + m.getAnnotation(GetMapping.class).value();
        validatePath(pathValue);
        HTTPServerImpl.get(pathValue, (req, res) -> {
            try {
                Object result = null;
                Parameter p = validateParameters(m);
                if (p != null) {

                    String value = p.getAnnotation(RequestParam.class).value();
                    String parsedResponse = req.getValue(value) == null ||req.getValue(value).isEmpty() ? p.getAnnotation(RequestParam.class).defaultValue() : req.getValue(value);
                    result = m.invoke(injectedClasses.get(c.getName()), parsedResponse);
                } else {
                    result = m.invoke(injectedClasses.get(c.getName()));
                }
                res.setBody(result.toString());
                return res;
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
        registeredPaths.add(pathValue);
    }

    private void validatePath(String path) {
        if (registeredPaths.contains(path)) {
            throw new RuntimeException("Path already registered: " + path);
        }
        if (!path.startsWith("/")) {
            throw new RuntimeException("Path must start with /");
        }
        if (path.endsWith("/")) {
            throw new RuntimeException("Path must not end with /");
        }
        if (path.contains("//")) {
            throw new RuntimeException("Path must not contain //");
        }
    }


    private void solveDependencies() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException{
        System.out.println("Starting Dependency Injection");
        for (Class<?> c: reflections.getTypesAnnotatedWith(Component.class)) {
            injectClass(c);
        }
        for (Class<?> c: reflections.getTypesAnnotatedWith(Autowired.class)) {
            injectClass(c);
        }
        System.out.println("Dependency Injection completed");
    }

    private List<Field> getAutowiredFields(Class<?> c) {
        List<Field> fields  = new ArrayList<>();
        for (Field f: c.getDeclaredFields()) {
            if (f.isAnnotationPresent(Autowired.class)) {
                fields.add(f);
            }
        }
        return fields;
    }





    private Object injectClass(Class<?> c)throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (injectedClasses.containsKey(c.getName())) {
            return injectedClasses.get(c.getName());
        }
        List<Field> fieldsToInject = getAutowiredFields(c);
        if (fieldsToInject.isEmpty()) {
            Object result = c.getDeclaredConstructor().newInstance();
            injectedClasses.put(c.getName(), result);
            return result;
        }
        return injectClass(c, fieldsToInject);
    }
    private Object injectClass(Class<?> c, List<Field> fieldsToInject)throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Object classInstance = c.getDeclaredConstructor().newInstance();
        for (Field f: fieldsToInject) {
            Class<?> fieldType = f.getType();
            Object fieldInstance;
            if (injectedClasses.containsKey(fieldType.getName())) {
                fieldInstance = injectedClasses.get(fieldType.getName());
            } else {
                fieldInstance = injectDependency(fieldType);
            }
            f.setAccessible(true);
            try {
                f.set(classInstance, fieldInstance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        injectedClasses.put(c.getName(), classInstance);
        return classInstance;
    }

    private Object injectDependency(Class<?> fieldType)throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (injectedClasses.containsKey(fieldType.getName())) {
            return injectedClasses.get(fieldType.getName());
        }
        if (fieldType.isInterface()) {
            @SuppressWarnings("unchecked")
            Set<Class<?>> implementations = (Set<Class<?>>)(Set<?>) reflections.getSubTypesOf((Class<Object>) fieldType);
            if (implementations.isEmpty()) {
                throw new RuntimeException("No Implementations found for: " + fieldType.getName());
            } if (implementations.size() > 1) {
                throw new RuntimeException("Multiple Implementations found for: " + fieldType.getName() + ". Please specify which one to use.");
            }
            Class<?> implementation = implementations.iterator().next();
            return injectClass(implementation);

        }
        return injectClass(fieldType);
    }
}
