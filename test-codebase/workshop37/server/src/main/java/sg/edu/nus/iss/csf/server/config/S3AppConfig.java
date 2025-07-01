package sg.edu.nus.iss.csf.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Configuration
public class S3AppConfig {
    @Value("${do.storage.key}")
    private String accessKey;

    @Value("${do.storage.secret}")
    private String secretKey;

    @Value("${do.storage.endpoint}")
    private String endPoint;

    @Value("${do.storage.region}")
    private String endPointRegion;

    @Bean
    public AmazonS3 createS3Client(){
        BasicAWSCredentials awsCreds = 
            new BasicAWSCredentials(accessKey, secretKey);
        EndpointConfiguration endpoint = 
            new EndpointConfiguration(endPoint, endPointRegion);

        return AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(endpoint)
            .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
            .build();
    }
}
