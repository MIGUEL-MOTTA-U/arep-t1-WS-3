package escuelaing.edu.co.src.dev;

import escuelaing.edu.co.framework.annotations.GetMapping;
import escuelaing.edu.co.framework.annotations.RequestParam;
import escuelaing.edu.co.framework.annotations.RestController;

@RestController("/api/v1")
public class UserController {

    @GetMapping("/users")
    public String getAllUsers() {
        return "List of all users";
    }

    @GetMapping("/users/greeting")
    public String getUserById(@RequestParam(value = "name", defaultValue = "World") String name) {
        return "Hello " + name + "!";
    }
}
