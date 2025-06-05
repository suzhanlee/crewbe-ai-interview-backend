package com.didim.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class HealthController {
    
    @Value("\${aws.region}")
    private lateinit var awsRegion: String
    
    @Value("\${aws.s3.recording-bucket}")
    private lateinit var recordingBucket: String
    
    @Value("\${aws.s3.profile-bucket}")
    private lateinit var profileBucket: String
    
    @Value("\${aws.s3.analysis-bucket}")
    private lateinit var analysisBucket: String
    
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        val healthData = mapOf(
            "status" to "OK",
            "message" to "Didim Interview Analysis API Server is running",
            "timestamp" to LocalDateTime.now(),
            "environment" to mapOf(
                "aws_region" to awsRegion,
                "aws_configured" to true,
                "buckets" to mapOf(
                    "video" to recordingBucket,
                    "analysis" to analysisBucket,
                    "profile" to profileBucket
                )
            )
        )
        
        return ResponseEntity.ok(healthData)
    }
} 