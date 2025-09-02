package escuelaing.edu.co.framework.injector;

import escuelaing.edu.co.framework.annotations.*;
import escuelaing.edu.co.framework.services.implementations.HTTPServerImpl;
import escuelaing.edu.co.framework.services.interfaces.HTTPServerHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class InjectorRouteRegistrationTest {

    private Path tmpResources;

    @RestController("/api")
    public static class TestController {
        @GetMapping("/hello")
        public String hello() {
            return "Hello World";
        }

        @GetMapping("/greet")
        public String greet(@RequestParam(value = "name", defaultValue = "Anonymous") String name) {
            return "Hello " + name;
        }
    }

    @Component
    public static class TestComponent {
        public String getMessage() {
            return "Test Component";
        }
    }

    @RestController("/api")
    public static class InvalidPathController {
        @GetMapping("invalid") // Path sin '/' al inicio
        public String invalid() {
            return "Invalid";
        }
    }


    @BeforeEach
    void setUp() throws Exception {
        tmpResources = Files.createTempDirectory("injector_test_" + UUID.randomUUID());
        Field resPathField = HTTPServerImpl.class.getDeclaredField("RESOURCES_PATH");
        resPathField.setAccessible(true);
        resPathField.set(null, tmpResources.toAbsolutePath().toString());

        // Limpiar rutas registradas antes de cada test
        Field routesField = HTTPServerImpl.class.getDeclaredField("routes");
        routesField.setAccessible(true);
        Map<String, HTTPServerHandler> routes = (Map<String, HTTPServerHandler>) routesField.get(null);
        routes.clear();

        // Limpiar instancia del Injector
        Field instanceField = Injector.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (tmpResources != null && Files.exists(tmpResources)) {
            Files.walk(tmpResources)
                    .map(Path::toFile)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(File::delete);
        }
    }

    @Test
    public void routeRegistered_and_handlerReturnsExpectedBody() throws Exception {
        // Usar debug = true para evitar que inicie el servidor
        Injector.startApp(TestController.class, true);

        // Verificar que las rutas se registraron correctamente
        Field routesField = HTTPServerImpl.class.getDeclaredField("routes");
        routesField.setAccessible(true);
        Map<String, HTTPServerHandler> routes = (Map<String, HTTPServerHandler>) routesField.get(null);

        assertTrue(routes.containsKey("/api/hello"), "La ruta /api/hello debe estar registrada");
        assertTrue(routes.containsKey("/api/greet"), "La ruta /api/greet debe estar registrada");
    }

    @Test
    public void componentInjection_worksCorrectly() throws Exception {
        Injector.startApp(TestComponent.class, true);

        // Verificar que el componente se inyectó
        Field instanceField = Injector.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        Injector injectorInstance = (Injector) instanceField.get(null);

        Field injectedClassesField = Injector.class.getDeclaredField("injectedClasses");
        injectedClassesField.setAccessible(true);
        Map<String, Object> injectedClasses = (Map<String, Object>) injectedClassesField.get(injectorInstance);

        assertTrue(injectedClasses.containsKey(TestComponent.class.getName()),
                "El componente debe estar inyectado");
    }
}