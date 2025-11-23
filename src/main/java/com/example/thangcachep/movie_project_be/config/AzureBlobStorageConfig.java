package com.example.thangcachep.movie_project_be.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

/**
 * Azure Blob Storage Configuration
 * Thay thế AWS S3 Configuration
 */
@Configuration
public class AzureBlobStorageConfig {

    @Value("${azure.storage.account-name}")
    private String accountName;

    @Value("${azure.storage.account-key}")
    private String accountKey;

    /**
     * Tạo Bean BlobServiceClient để kết nối Azure Blob Storage
     */
    @Bean
    public BlobServiceClient blobServiceClient() {
        String connectionString = String.format(
                "DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;EndpointSuffix=core.windows.net",
                accountName,
                accountKey
        );

        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }
}

