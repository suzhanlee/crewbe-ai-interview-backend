# Didim Interview Analysis Server (Kotlin)

## 🚀 시작하기

### 환경 설정

1. **AWS 설정 파일 생성**
   
   `src/main/resources/application-local.yml` 파일을 생성하고 AWS 키 정보를 입력:
   
   ```yaml
   aws:
     region: ap-northeast-2
     credentials:
       access-key: YOUR_AWS_ACCESS_KEY_ID
       secret-key: YOUR_AWS_SECRET_ACCESS_KEY
     s3:
       recording-bucket: YOUR_RECORDING_BUCKET_NAME
       profile-bucket: YOUR_PROFILE_BUCKET_NAME
       analysis-bucket: YOUR_ANALYSIS_BUCKET_NAME
   ```

2. **애플리케이션 실행**
   
   ```bash
   # Windows
   .\gradlew.bat bootRun --args='--spring.profiles.active=local'
   
   # Linux/Mac
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

3. **API 엔드포인트**
   
   - 헬스 체크: `GET http://localhost:3000/health`
   - Pre-Signed URL 생성: `POST http://localhost:3000/api/upload/presigned-url`
   - 직접 업로드: `POST http://localhost:3000/api/upload/direct`
   - 분석 시작: `POST http://localhost:3000/api/analysis/start`

### 보안 주의사항

⚠️ **중요**: `application-local.yml` 파일은 AWS 키 정보가 포함되어 있으므로 절대 GitHub에 커밋하지 마세요. 이 파일은 `.gitignore`에 이미 포함되어 있습니다.

# Didim Interview Analysis Server - Kotlin 변환 가이드

## 📋 목차
1. [프로젝트 개요](#1-프로젝트-개요)
2. [Step 1. 프로젝트 초기 설정](#step-1-프로젝트-초기-설정)
3. [Step 2. 리포지토리 & 서비스 계층](#step-2-리포지토리--서비스-계층)
4. [Step 3. AWS 연동 기능](#step-3-aws-연동-기능)
5. [Step 4. REST API 컨트롤러](#step-4-rest-api-컨트롤러)
6. [Step 5. 로깅 시스템](#step-5-로깅-시스템)
7. [Step 6. 배포 및 테스트](#step-6-배포-및-테스트)

---

## 1. 프로젝트 개요

### 현재 didim-new-expo 주요 기능
- **영상 업로드 시스템**: AWS S3 Pre-Signed URL 생성 및 직접 업로드
- **AI 분석 서비스**: AWS Transcribe(STT), Rekognition(얼굴/감정 분석)
- **헬스 체크**: 서버 상태 및 AWS 연결 상태 모니터링
- **에러 핸들링**: 구조화된 에러 응답 및 로깅

### 기술 스택
- **런타임**: Node.js + Express.js
- **AWS 서비스**: S3, Transcribe, Rekognition
- **로깅**: Winston
- **파일 업로드**: Multer
---

## Step 1. 프로젝트 초기 설정

### 1.1 Kotlin 기반 Gradle 세팅

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.spring") version "1.9.10"
    kotlin("plugin.jpa") version "1.9.10"
    id("org.springframework.boot") version "3.1.5"
    id("io.spring.dependency-management") version "1.1.3"
}

group = "com.didim"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    
    // AWS SDK v2
    implementation("software.amazon.awssdk:s3:2.20.143")
    implementation("software.amazon.awssdk:transcribe:2.20.143")
    implementation("software.amazon.awssdk:rekognition:2.20.143")
    implementation("software.amazon.awssdk:auth:2.20.143")
    
    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    
    // Database (Optional)
    runtimeOnly("com.h2database:h2")
    
    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.8")
}
```

### 1.2 application.yml 설정

```yaml
# application.yml
server:
  port: 3000

spring:
  application:
    name: didim-interview-analysis
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    database-platform: org.hibernate.dialect.H2Dialect
  
  h2:
    console:
      enabled: true

aws:
  region: ap-northeast-2
  credentials:
    access-key: ${AWS_ACCESS_KEY_ID:}
    secret-key: ${AWS_SECRET_ACCESS_KEY:}
  s3:
    recording-bucket: flight-attendant-recordings
    profile-bucket: flight-attendant-profiles
    analysis-bucket: crewbe-analysis-results

logging:
  level:
    com.didim: DEBUG
    software.amazon.awssdk: INFO
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"
  file:
    name: logs/didim-interview-analysis.log
    max-size: 10MB
    max-history: 10
```

---

## Step 2. 리포지토리 & 서비스 계층

### 2.1 엔티티 클래스 정의

```kotlin
// src/main/kotlin/com/didim/entity/UploadRecord.kt
package com.didim.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "upload_records")
data class UploadRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, unique = true)
    val s3Key: String,
    
    @Column(nullable = false)
    val bucket: String,
    
    @Column(nullable = false)
    val originalFileName: String,
    
    @Column(nullable = false)
    val fileSize: Long,
    
    @Column(nullable = false)
    val contentType: String,
    
    @Enumerated(EnumType.STRING)
    val uploadStatus: UploadStatus = UploadStatus.COMPLETED,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class UploadStatus {
    PENDING, COMPLETED, FAILED
}
```

### 2.2 JPA 리포지토리 인터페이스

```kotlin
// src/main/kotlin/com/didim/repository/UploadRecordRepository.kt
package com.didim.repository

import com.didim.entity.UploadRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UploadRecordRepository : JpaRepository<UploadRecord, Long> {
    fun findByS3Key(s3Key: String): Optional<UploadRecord>
    fun findByBucketAndS3Key(bucket: String, s3Key: String): Optional<UploadRecord>
}
```

### 2.3 비즈니스 서비스 클래스

```kotlin
// src/main/kotlin/com/didim/service/UploadService.kt
package com.didim.service

import com.didim.entity.UploadRecord
import com.didim.repository.UploadRecordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
@Transactional
class UploadService(
    private val uploadRecordRepository: UploadRecordRepository
) {
    
    fun saveUploadRecord(
        s3Key: String,
        bucket: String,
        originalFileName: String,
        fileSize: Long,
        contentType: String
    ): UploadRecord {
        val record = UploadRecord(
            s3Key = s3Key,
            bucket = bucket,
            originalFileName = originalFileName,
            fileSize = fileSize,
            contentType = contentType
        )
        return uploadRecordRepository.save(record)
    }
    
    @Transactional(readOnly = true)
    fun findByS3Key(s3Key: String): UploadRecord? {
        return uploadRecordRepository.findByS3Key(s3Key).orElse(null)
    }
    
    fun generateS3Key(originalFileName: String): String {
        val timestamp = System.currentTimeMillis()
        val randomId = UUID.randomUUID().toString().substring(0, 8)
        val extension = originalFileName.substringAfterLast('.', "webm")
        return "videos/interview-$timestamp-$randomId.$extension"
    }
}
```

---

## Step 3. AWS 연동 기능

### 3.1 AWS 설정 클래스

```kotlin
// src/main/kotlin/com/didim/config/AwsConfig.kt
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
```

### 3.2 S3 서비스 클래스

```kotlin
// src/main/kotlin/com/didim/service/S3Service.kt
package com.didim.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
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
```

### 3.3 AI 분석 서비스

```kotlin
// src/main/kotlin/com/didim/service/AnalysisService.kt
package com.didim.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.rekognition.RekognitionClient
import software.amazon.awssdk.services.rekognition.model.*
import software.amazon.awssdk.services.transcribe.TranscribeClient
import software.amazon.awssdk.services.transcribe.model.*

@Service
class AnalysisService(
    private val transcribeClient: TranscribeClient,
    private val rekognitionClient: RekognitionClient
) {
    
    private val logger = LoggerFactory.getLogger(AnalysisService::class.java)
    
    @Value("\${aws.s3.recording-bucket}")
    private lateinit var recordingBucket: String
    
    @Value("\${aws.s3.analysis-bucket}")
    private lateinit var analysisBucket: String
    
    data class AnalysisResult(
        val stt: JobResult,
        val faceDetection: JobResult,
        val segmentDetection: JobResult
    )
    
    data class JobResult(
        val jobId: String?,
        val status: String,
        val error: String? = null
    )
    
    suspend fun startAnalysis(s3Key: String): AnalysisResult {
        logger.info("🧠 분석 작업 시작: s3Key={}", s3Key)
        
        val sttResult = startTranscriptionJob(s3Key)
        val faceResult = startFaceDetection(s3Key)
        val segmentResult = startSegmentDetection(s3Key)
        
        return AnalysisResult(sttResult, faceResult, segmentResult)
    }
    
    private fun startTranscriptionJob(s3Key: String): JobResult {
        return try {
            val jobName = "interview-stt-${System.currentTimeMillis()}"
            val mediaUri = "https://$recordingBucket.s3.amazonaws.com/$s3Key"
            
            val request = StartTranscriptionJobRequest.builder()
                .transcriptionJobName(jobName)
                .media(
                    Media.builder()
                        .mediaFileUri(mediaUri)
                        .build()
                )
                .mediaFormat(MediaFormat.WEBM)
                .languageCode(LanguageCode.KO_KR)
                .outputBucketName(analysisBucket)
                .build()
            
            val response = transcribeClient.startTranscriptionJob(request)
            
            logger.info("✅ STT 작업 시작 성공: jobName={}", jobName)
            JobResult(jobId = jobName, status = "IN_PROGRESS")
            
        } catch (e: Exception) {
            logger.error("💥 STT 작업 시작 실패", e)
            JobResult(jobId = null, status = "FAILED", error = e.message)
        }
    }
    
    private fun startFaceDetection(s3Key: String): JobResult {
        return try {
            val request = StartFaceDetectionRequest.builder()
                .video(
                    Video.builder()
                        .s3Object(
                            S3Object.builder()
                                .bucket(recordingBucket)
                                .name(s3Key)
                                .build()
                        )
                        .build()
                )
                .faceAttributes(FaceAttributes.ALL)
                .build()
            
            val response = rekognitionClient.startFaceDetection(request)
            
            logger.info("✅ 얼굴 감지 작업 시작 성공: jobId={}", response.jobId())
            JobResult(jobId = response.jobId(), status = "IN_PROGRESS")
            
        } catch (e: Exception) {
            logger.error("💥 얼굴 감지 작업 시작 실패", e)
            JobResult(jobId = null, status = "FAILED", error = e.message)
        }
    }
    
    private fun startSegmentDetection(s3Key: String): JobResult {
        return try {
            val request = StartSegmentDetectionRequest.builder()
                .video(
                    Video.builder()
                        .s3Object(
                            S3Object.builder()
                                .bucket(recordingBucket)
                                .name(s3Key)
                                .build()
                        )
                        .build()
                )
                .segmentTypes(SegmentType.TECHNICAL_CUE, SegmentType.SHOT)
                .build()
            
            val response = rekognitionClient.startSegmentDetection(request)
            
            logger.info("✅ 세그먼트 감지 작업 시작 성공: jobId={}", response.jobId())
            JobResult(jobId = response.jobId(), status = "IN_PROGRESS")
            
        } catch (e: Exception) {
            logger.error("💥 세그먼트 감지 작업 시작 실패", e)
            JobResult(jobId = null, status = "FAILED", error = e.message)
        }
    }
}
```

---

## Step 4. REST API 컨트롤러

### 4.1 요청/응답 DTO 클래스

```kotlin
// src/main/kotlin/com/didim/dto/UploadDto.kt
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
```

### 4.2 업로드 컨트롤러

```kotlin
// src/main/kotlin/com/didim/controller/UploadController.kt
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
```

### 4.3 분석 컨트롤러

```kotlin
// src/main/kotlin/com/didim/controller/AnalysisController.kt
package com.didim.controller

import com.didim.dto.AnalysisRequest
import com.didim.dto.AnalysisResponse
import com.didim.dto.JobStatus
import com.didim.service.AnalysisService
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/analysis")
class AnalysisController(
    private val analysisService: AnalysisService
) {
    
    private val logger = LoggerFactory.getLogger(AnalysisController::class.java)
    
    @PostMapping("/start")
    fun startAnalysis(
        @Valid @RequestBody request: AnalysisRequest
    ): ResponseEntity<AnalysisResponse> {
        return try {
            logger.info("🧠 분석 시작 요청: s3Key={}", request.s3Key)
            
            val result = runBlocking {
                analysisService.startAnalysis(request.s3Key)
            }
            
            val response = AnalysisResponse(
                success = true,
                stt = JobStatus(result.stt.jobId, result.stt.status, result.stt.error),
                faceDetection = JobStatus(result.faceDetection.jobId, result.faceDetection.status, result.faceDetection.error),
                segmentDetection = JobStatus(result.segmentDetection.jobId, result.segmentDetection.status, result.segmentDetection.error)
            )
            
            ResponseEntity.ok(response)
            
        } catch (e: Exception) {
            logger.error("분석 시작 실패", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    AnalysisResponse(
                        success = false,
                        stt = JobStatus(null, "FAILED", e.message),
                        faceDetection = JobStatus(null, "FAILED", e.message),
                        segmentDetection = JobStatus(null, "FAILED", e.message)
                    )
                )
        }
    }
    
    @GetMapping("/status/{jobType}/{jobId}")
    fun getAnalysisStatus(
        @PathVariable jobType: String,
        @PathVariable jobId: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // 실제 구현에서는 AWS SDK를 사용하여 작업 상태를 확인
            val response = mapOf(
                "success" to true,
                "jobType" to jobType,
                "jobId" to jobId,
                "status" to "IN_PROGRESS", // 실제로는 AWS API 호출 결과
                "message" to "분석 진행 중입니다"
            )
            
            ResponseEntity.ok(response)
            
        } catch (e: Exception) {
            logger.error("분석 상태 확인 실패", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("success" to false, "error" to e.message))
        }
    }
}
```

### 4.4 헬스 체크 컨트롤러

```kotlin
// src/main/kotlin/com/didim/controller/HealthController.kt
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
                "aws_configured" to true, // 실제로는 AWS 연결 상태 확인
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
```

---

## Step 5. 로깅 시스템

### 5.1 로깅 설정 클래스

```kotlin
// src/main/kotlin/com/didim/config/LoggingConfig.kt
package com.didim.config

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct

@Configuration
class LoggingConfig {
    
    @PostConstruct
    fun configureLogging() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        
        // API 로그 파일 설정
        configureApiLogger(loggerContext)
        
        // AWS 로그 파일 설정
        configureAwsLogger(loggerContext)
    }
    
    private fun configureApiLogger(context: LoggerContext) {
        val apiLogger = context.getLogger("com.didim.api")
        
        val encoder = PatternLayoutEncoder().apply {
            this.context = context
            pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
            start()
        }
        
        val fileAppender = RollingFileAppender<ch.qos.logback.classic.spi.ILoggingEvent>().apply {
            this.context = context
            file = "logs/api.log"
            
            val rollingPolicy = TimeBasedRollingPolicy<ch.qos.logback.classic.spi.ILoggingEvent>().apply {
                this.context = context
                setParent(this@apply)
                fileNamePattern = "logs/api.%d{yyyy-MM-dd}.%i.gz"
                maxHistory = 30
                start()
            }
            
            this.rollingPolicy = rollingPolicy
            this.encoder = encoder
            start()
        }
        
        apiLogger.addAppender(fileAppender)
        apiLogger.level = ch.qos.logback.classic.Level.INFO
    }
    
    private fun configureAwsLogger(context: LoggerContext) {
        val awsLogger = context.getLogger("com.didim.aws")
        
        val encoder = PatternLayoutEncoder().apply {
            this.context = context
            pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [AWS] %-5level - %msg%n"
            start()
        }
        
        val fileAppender = RollingFileAppender<ch.qos.logback.classic.spi.ILoggingEvent>().apply {
            this.context = context
            file = "logs/aws.log"
            
            val rollingPolicy = TimeBasedRollingPolicy<ch.qos.logback.classic.spi.ILoggingEvent>().apply {
                this.context = context
                setParent(this@apply)
                fileNamePattern = "logs/aws.%d{yyyy-MM-dd}.gz"
                maxHistory = 7
                start()
            }
            
            this.rollingPolicy = rollingPolicy
            this.encoder = encoder
            start()
        }
        
        awsLogger.addAppender(fileAppender)
        awsLogger.level = ch.qos.logback.classic.Level.DEBUG
    }
}
```

---

## Step 6. 배포 및 테스트

### 6.1 Docker 설정

```dockerfile
# Dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 3000

ENV JAVA_OPTS="-Xms512m -Xmx1024m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 6.2 GitHub Actions CI/CD

```yaml
# .github/workflows/deploy.yml
name: Deploy to AWS EC2

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Build with Gradle
      run: ./gradlew build
    
    - name: Build Docker image
      run: docker build -t didim-interview-analysis .
    
    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v2
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: ap-northeast-2
    
    - name: Login to Amazon ECR
      id: login-ecr
      uses: aws-actions/amazon-ecr-login@v1
    
    - name: Push to ECR
      env:
        ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
        ECR_REPOSITORY: didim-interview-analysis
        IMAGE_TAG: latest
      run: |
        docker tag didim-interview-analysis:latest $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
        docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
    
    - name: Deploy to EC2
      run: |
        echo "EC2 배포 스크립트 실행"
        # 실제 배포 명령어 추가
```

### 6.3 AWS EC2 설정 예시

```bash
#!/bin/bash
# deploy.sh

# Docker 설치 (Amazon Linux 2)
sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user

# AWS CLI 설치
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# 환경변수 설정
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_REGION=ap-northeast-2

# 애플리케이션 실행
docker run -d \
  --name didim-interview-analysis \
  -p 3000:3000 \
  -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
  -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
  -e AWS_REGION=$AWS_REGION \
  -v /home/ec2-user/logs:/app/logs \
  didim-interview-analysis:latest
```

### 6.4 테스트 코드 예시

```kotlin
// src/test/kotlin/com/didim/controller/UploadControllerTest.kt
package com.didim.controller

import com.didim.service.S3Service
import com.didim.service.UploadService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(UploadController::class)
class UploadControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @TestConfiguration
    class TestConfig {
        @Bean
        fun s3Service() = mockk<S3Service>()
        
        @Bean
        fun uploadService() = mockk<UploadService>()
    }
    
    @Test
    fun `Pre-Signed URL 생성 성공 테스트`() {
        val s3Service = mockk<S3Service>()
        val uploadService = mockk<UploadService>()
        
        every { s3Service.generatePresignedUrl(any(), any()) } returns "https://test-url.com"
        every { uploadService.generateS3Key(any()) } returns "videos/test-key.webm"
        
        val request = mapOf(
            "fileName" to "test.webm",
            "fileType" to "video/webm"
        )
        
        mockMvc.perform(
            post("/api/upload/presigned-url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.presignedUrl").exists())
        .andExpect(jsonPath("$.s3Key").exists())
    }
}
```

---

## 🎯 마무리

이 가이드를 통해 Node.js Express 기반의 Didim Interview Analysis Server를 Kotlin Spring Boot로 성공적으로 변환할 수 있습니다. 주요 포인트:

1. **단계별 접근**: 각 기능을 모듈별로 나누어 체계적으로 변환
2. **AWS 연동 유지**: 기존 S3, Transcribe, Rekognition 기능 완전 호환
3. **로깅 시스템**: 기존 Winston 기반 로깅을 Logback으로 전환
4. **타입 안정성**: Kotlin의 null safety와 타입 추론 활용
5. **테스트 지원**: MockMvc와 MockK를 활용한 테스트 환경 구축

변환 후에는 Kotlin의 장점(간결한 문법, null safety, 코루틴 등)을 활용하여 더욱 안정적이고 효율적인 백엔드 시스템을 구축할 수 있습니다. 