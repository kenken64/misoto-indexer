package sg.edu.nus.iss.sse.controller;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MusicController {
    @GetMapping("/api/download-music")
    public String downloadMusic() {
        try {
            String fileUrl = "https://ncs.io/track/download/396f65ed-7e68-4eab-a5ce-71eb9adf5aa6";
            String localFilePath = "/home/kenneth/Downloads/musicFile.mp3";
            URI uri = new URI(fileUrl);
            try (InputStream in = uri.toURL().openStream();
                    FileOutputStream out = new FileOutputStream(localFilePath)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return "Music file downloaded successfully!";
        } catch (Exception e) {
            return "Error downloading music file: " + e.getMessage();
        }
    }
}
