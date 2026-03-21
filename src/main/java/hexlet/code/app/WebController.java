package hexlet.code.app;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
public class WebController {

    @GetMapping("/")
    String home() {
        return "Hello World!";
    }
}
