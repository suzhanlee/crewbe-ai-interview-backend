package com.didim.controller

import com.didim.dto.*
import com.didim.service.S3Service
import com.didim.service.UploadService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/upload")
class UploadController(
    private val s3Service: S3Service,
    private val uploadService: UploadService
) {
    
    private val logger = LoggerFactory.getLogger(UploadController::class.java)
    
    @PostMapping("/presigned-url")
    fun generatePresignedUrl(
        @Valid @RequestBody request: PresignedUrlRequest
    ): ResponseEntity<PresignedUrlResponse> {
        return try {
            val presignedUrl = s3Service.generatePresignedUrl(
                request.fileName, 
                request.fileType
            )
            val s3Key = uploadService.generateS3Key(request.fileName)
            
            val response = PresignedUrlResponse(
                success = true,
                presignedUrl = presignedUrl,
                s3Key = s3Key,
                bucket = "flight-attendant-recordings",
                expiresIn = 3600
            )
            
            ResponseEntity.ok(response)
            
        } catch (e: Exception) {
            logger.error("Pre-Signed URL 생성 실패", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    PresignedUrlResponse(
                        success = false,
                        presignedUrl = "",
                        s3Key = "",
                        bucket = "",
                        expiresIn = 0
                    )
                )
        }
    }
    
    @PostMapping("/direct")
    fun uploadFile(
        @RequestParam("video") file: MultipartFile
    ): ResponseEntity<UploadResponse> {
        return try {
            if (file.isEmpty) {
                return ResponseEntity.badRequest()
                    .body(UploadResponse(success = false, error = "파일이 제공되지 않았습니다"))
            }
            
            val startTime = System.currentTimeMillis()
            val s3Key = uploadService.generateS3Key(file.originalFilename ?: "video")
            val s3Url = s3Service.uploadFile(file, s3Key)
            val uploadTime = (System.currentTimeMillis() - startTime) / 1000.0
            
            // 업로드 기록 저장
            uploadService.saveUploadRecord(
                s3Key = s3Key,
                bucket = "flight-attendant-recordings",
                originalFileName = file.originalFilename ?: "unknown",
                fileSize = file.size,
                contentType = file.contentType ?: "video/webm"
            )
            
            val fileSizeMB = file.size / 1024.0 / 1024.0
            val uploadSpeed = (fileSizeMB * 8) / uploadTime // Mbps
            
            val response = UploadResponse(
                success = true,
                s3Url = s3Url,
                s3Key = s3Key,
                bucket = "flight-attendant-recordings",
                fileSize = file.size,
                uploadTime = uploadTime,
                uploadSpeed = uploadSpeed
            )
            
            ResponseEntity.ok(response)
            
        } catch (e: Exception) {
            logger.error("파일 업로드 실패", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(UploadResponse(success = false, error = e.message))
        }
    }
    
    @GetMapping("/status/{s3Key}")
    fun checkUploadStatus(@PathVariable s3Key: String): ResponseEntity<Map<String, Any>> {
        return try {
            val exists = s3Service.checkFileExists(s3Key)
            val record = uploadService.findByS3Key(s3Key)
            
            val response = mapOf(
                "success" to true,
                "exists" to exists,
                "record" to record,
                "s3Key" to s3Key
            )
            
            ResponseEntity.ok(response)
            
        } catch (e: Exception) {
            logger.error("파일 상태 확인 실패", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("success" to false, "error" to e.message))
        }
    }
} 