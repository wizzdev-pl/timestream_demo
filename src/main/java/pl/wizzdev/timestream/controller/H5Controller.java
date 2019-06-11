package pl.wizzdev.timestream.controller;

import org.springframework.web.bind.annotation.*;
import pl.wizzdev.timestream.utils.H5AddFileInBackground;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


@RestController
public class H5Controller {

    private final static List<String> history = new ArrayList<>();

    @GetMapping("/history")
    public List<String> getHistory() {
        return history;
    }

    @DeleteMapping("/history")
    public List<String> deleteHistory () {
        List<String> historyCopy = new ArrayList<>(history);
        history.clear();
        return historyCopy;
    }

    @PostMapping("/file")
    public String sendFile(@RequestParam("filepath") String filepath) {
        Path path = Paths.get(filepath);
        try {
            H5AddFileInBackground h5AddFileInBackground = new H5AddFileInBackground(
                    path.getFileName().toString(),
                    path.toAbsolutePath().toString()
            );
            h5AddFileInBackground.start();
            history.add(filepath);
        } catch (Exception exception) {
            return exception.getMessage();
        }
        return "OK";
    }
}
