package com.didim.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.rekognition.RekognitionClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.transcribe.TranscribeClient

@Configuration
class AwsConfig {
    
    @Value("\${aws.credentials.access-key}")
    private lateinit var accessKey: String
    
    @Value("\${aws.credentials.secret-key}")
    private lateinit var secretKey: String
    
    @Value("\${aws.region}")
    private lateinit var region: String
    
    @Bean
    fun awsCredentialsProvider(): StaticCredentialsProvider {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)
        )
    }
    
    @Bean
    fun s3Client(credentialsProvider: StaticCredentialsProvider): S3Client {
        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build()
    }
    
    @Bean
    fun transcribeClient(credentialsProvider: StaticCredentialsProvider): TranscribeClient {
        return TranscribeClient.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build()
    }
    
    @Bean
    fun rekognitionClient(credentialsProvider: StaticCredentialsProvider): RekognitionClient {
        return RekognitionClient.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build()
    }
} 