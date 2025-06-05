package com.didim.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class PresignedUrlRequest(
    @field:NotBlank(message = "파일명은 필수입니다")
    @field:Size(min = 1, max = 255, message = "파일명은 1-255자여야 합니다")
    val fileName: String,
    
    @field:NotBlank(message = "파일 타입은 필수입니다")
    val fileType: String
)

data class PresignedUrlResponse(
    val success: Boolean,
    val presignedUrl: String,
    val s3Key: String,
    val bucket: String,
    val expiresIn: Int,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class UploadResponse(
    val success: Boolean,
    val s3Url: String? = null,
    val s3Key: String? = null,
    val bucket: String? = null,
    val fileSize: Long? = null,
    val uploadTime: Double? = null,
    val uploadSpeed: Double? = null,
    val error: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class AnalysisRequest(
    @field:NotBlank(message = "S3 키는 필수입니다")
    val s3Key: String,
    
    val bucket: String? = null
)

data class AnalysisResponse(
    val success: Boolean,
    val stt: JobStatus,
    val faceDetection: JobStatus,
    val segmentDetection: JobStatus,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class JobStatus(
    val jobId: String?,
    val status: String,
    val error: String? = null
) 