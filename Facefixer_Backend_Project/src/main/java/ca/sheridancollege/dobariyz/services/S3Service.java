package ca.sheridancollege.dobariyz.services;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

//@Service
public class S3Service {

    private final String bucketName;
    private final String region;
    private final S3Client s3Client;

    public S3Service(
            @Value("${aws.s3.bucket}") String bucketName,
            @Value("${aws.region}") String region,
            @Value("${aws.access-key}") String accessKey,
            @Value("${aws.secret-key}") String secretKey
    ) {
        this.bucketName = bucketName;
        this.region = region;

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of(region))
                .build();
    }

    // Upload file
    public String uploadFile(String keyName, Path filePath) throws IOException {
        System.out.println("Uploading file to bucket: " + bucketName + " with key: " + keyName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .build();  // ✅ No ACL

        s3Client.putObject(putObjectRequest, filePath);
        System.out.println("Upload complete: " + keyName);

        // Return S3 URL (can be used internally via /image)
        return "s3://" + bucketName + "/" + keyName;
    }

    // Download file
    public byte[] downloadFile(String keyName) throws IOException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .build();

        return s3Client.getObject(getObjectRequest).readAllBytes();
    }
}