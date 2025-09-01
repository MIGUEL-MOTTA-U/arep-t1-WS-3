package escuelaing.edu.co.framework.injector;

import escuelaing.edu.co.framework.annotations.Autowired;
import escuelaing.edu.co.framework.annotations.Component;
import escuelaing.edu.co.framework.annotations.Qualifier;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Injector {
    List<Class<?>> classes;
    private static final Logger logger = Logger.getLogger(Injector.class.getName());
    private static Injector instance;

    private Injector() {
        this.classes = new ArrayList<>();
    }

    public static void startApp(Class<?> mainClass) {
        try{
            synchronized (Injector.class) {
                if (instance == null){
                    instance = new Injector();
                }
                instance.init(mainClass);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init(Class<?> mainClass) {
        listAllClasses(mainClass);
    }

    private void listAllClasses(Class<?> mainClass) {
        String packageNamme = mainClass.getPackageName();
        logger.info("Package name: " + packageNamme);
        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of("src/main/java/" + packageNamme.replace(".", "/")));
            List<Path> files = new ArrayList<>();
            recursiveSearchResources(stream, files);
            for (Path p: files) {
                try {
                    String parsedClassPath = p.toString().replace("\\", ".").replace(".java", "").replace("src.main.", "");
                    Class<?> c = Class.forName(parsedClassPath);
                    if (c.isAnnotationPresent(Component.class) || c.isAnnotationPresent(Qualifier.class) || c.isAnnotationPresent(Autowired.class)) {
                        logger.info("Found injectable class : " + c.getName());
                        getBean(c);
                    }

                    classes.add(c);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    private void recursiveSearchResources(DirectoryStream<Path> currentStream, List<Path> files) {
        for (Path p: currentStream) {
            if(p.toString().endsWith(".java")) files.add(p);
            if (Files.isDirectory(p)){
                try {
                    DirectoryStream<Path> newStream = Files.newDirectoryStream(p);
                    recursiveSearchResources(newStream, files);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Object getBean(Class<?> c) {

        Class<?>[] c2 = c.getInterfaces();
        for (Class<?> i: c2) {
            String interfaceName = i.getName().split("\\.")[i.getName().split("\\.").length - 1];
            String className = c.getName().split("\\.")[c.getName().split("\\.").length - 1];
            System.out.println("Interface: " + interfaceName + " implemented by Class: " + className);
        }
        return null;
        }


}
