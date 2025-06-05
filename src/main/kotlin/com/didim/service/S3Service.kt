package com.didim.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration

@Service
class S3Service(
    private val s3Client: S3Client
) {
    
    private val logger = LoggerFactory.getLogger(S3Service::class.java)
    
    @Value("\${aws.s3.recording-bucket}")
    private lateinit var recordingBucket: String
    
    fun generatePresignedUrl(fileName: String, contentType: String): String {
        val s3Key = generateS3Key(fileName)
        
        logger.info("🔗 Pre-Signed URL 생성 시작: s3Key={}, contentType={}", s3Key, contentType)
        
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(recordingBucket)
            .key(s3Key)
            .contentType(contentType)
            .build()
        
        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofHours(1))
            .putObjectRequest(putObjectRequest)
            .build()
        
        S3Presigner.create().use { presigner ->
            val presignedRequest = presigner.presignPutObject(presignRequest)
            val url = presignedRequest.url().toString()
            
            logger.info("✅ Pre-Signed URL 생성 완료: length={}", url.length)
            return url
        }
    }
    
    fun uploadFile(file: MultipartFile, s3Key: String): String {
        logger.info("📤 S3 파일 업로드 시작: s3Key={}, size={}MB", 
                   s3Key, file.size / 1024.0 / 1024.0)
        
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(recordingBucket)
            .key(s3Key)
            .contentType(file.contentType)
            .build()
        
        val requestBody = RequestBody.fromInputStream(file.inputStream, file.size)
        
        val response = s3Client.putObject(putObjectRequest, requestBody)
        
        val s3Url = "https://$recordingBucket.s3.amazonaws.com/$s3Key"
        
        logger.info("✅ S3 파일 업로드 완료: etag={}", response.eTag())
        return s3Url
    }
    
    fun checkFileExists(s3Key: String): Boolean {
        return try {
            s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(recordingBucket)
                    .key(s3Key)
                    .build()
            )
            true
        } catch (e: NoSuchKeyException) {
            false
        }
    }
    
    private fun generateS3Key(fileName: String): String {
        val timestamp = System.currentTimeMillis()
        val randomId = (1..8).map { ('a'..'z').random() }.joinToString("")
        return "videos/interview-$timestamp-$randomId.webm"
    }
} 