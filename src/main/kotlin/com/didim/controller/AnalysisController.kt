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