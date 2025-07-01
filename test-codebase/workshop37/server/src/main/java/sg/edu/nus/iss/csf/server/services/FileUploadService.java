package sg.edu.nus.iss.csf.server.services;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import sg.edu.nus.iss.csf.server.models.Post;
import sg.edu.nus.iss.csf.server.repositories.FileUploadRepositories;

@Service
public class FileUploadService {
    @Autowired
    private FileUploadRepositories fileUploadRepositories;

    public String uploadFile(MultipartFile file, String comments) 
        throws SQLException, IOException{
        return fileUploadRepositories.upload(file, comments);
    }

    public Optional<Post> getPostById(String postId) 
        throws SQLException{
        return fileUploadRepositories.getPostById(postId);
    }
}
