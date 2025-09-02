package escuelaing.edu.co.src.dev;

import escuelaing.edu.co.framework.annotations.GetMapping;
import escuelaing.edu.co.framework.annotations.RestController;

@RestController("/api/v1")
public class BooksController {
    @GetMapping("/books")
    public String getBooks() {
        return "List of books";
    }
}
