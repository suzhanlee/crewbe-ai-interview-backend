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