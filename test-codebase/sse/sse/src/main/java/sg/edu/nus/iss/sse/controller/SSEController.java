package sg.edu.nus.iss.sse.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class SSEController {
    @GetMapping("/api/sse")
    public SseEmitter streamSse() {
        SseEmitter emitter = new SseEmitter();
        new Thread(() -> {
            try {
                String localFilePath = "/home/kenneth/Downloads/musicFile.mp3";
                Path path = Paths.get(localFilePath);
                byte[] musicFileBytes = Files.readAllBytes(path);
                String base64 = Base64.getEncoder().encodeToString(musicFileBytes);
                emitter.send(base64);
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }
}
