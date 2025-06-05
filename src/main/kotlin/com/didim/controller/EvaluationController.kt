package com.didim.controller

import com.didim.dto.InterviewEvaluationResult
import com.didim.service.InterviewEvaluationService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/evaluation")
class EvaluationController(
    private val evaluationService: InterviewEvaluationService
) {
    
    private val logger = LoggerFactory.getLogger(EvaluationController::class.java)
    
    @PostMapping("/analyze")
    fun analyzeInterview(
        @RequestBody request: EvaluationRequest
    ): ResponseEntity<InterviewEvaluationResult> {
        return try {
            logger.info("📊 면접 평가 요청: interviewId={}", request.interviewId)
            
            // 실제 환경에서는 S3에서 AWS 분석 결과를 가져와야 함
            // 여기서는 예시 데이터 사용
            val mockTranscribeResult = createMockTranscribeResult()
            val mockRekognitionResult = createMockRekognitionResult()
            
            val evaluation = evaluationService.evaluateInterview(
                transcribeResult = mockTranscribeResult,
                rekognitionResult = mockRekognitionResult,
                interviewId = request.interviewId,
                candidateName = request.candidateName
            )
            
            logger.info("✅ 면접 평가 완료: 종합점수={}", evaluation.overallScore)
            ResponseEntity.ok(evaluation)
            
        } catch (e: Exception) {
            logger.error("💥 면접 평가 실패", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null)
        }
    }
    
    @GetMapping("/demo")
    fun getDemoEvaluation(): ResponseEntity<InterviewEvaluationResult> {
        return try {
            val mockTranscribeResult = createMockTranscribeResult()
            val mockRekognitionResult = createMockRekognitionResult()
            
            val evaluation = evaluationService.evaluateInterview(
                transcribeResult = mockTranscribeResult,
                rekognitionResult = mockRekognitionResult,
                interviewId = "demo-interview-001",
                candidateName = "김승무원"
            )
            
            ResponseEntity.ok(evaluation)
            
        } catch (e: Exception) {
            logger.error("💥 데모 평가 실패", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null)
        }
    }
    
    // Mock 데이터 생성 (실제로는 S3에서 가져온 AWS 분석 결과)
    private fun createMockTranscribeResult(): Map<String, Any> {
        return mapOf(
            "jobName" to "interview-stt-demo",
            "results" to mapOf(
                "transcripts" to listOf(
                    mapOf("transcript" to "안녕하세요 저는 김승무원이라고 합니다. 승무원이 되는 것이 어릴 때부터의 꿈이었고, 고객 서비스에 대한 열정이 있어서 지원하게 되었습니다. 안전을 최우선으로 생각하며 승객들에게 최고의 서비스를 제공하고 싶습니다.")
                ),
                "items" to listOf(
                    mapOf(
                        "start_time" to "0.0",
                        "end_time" to "1.5",
                        "alternatives" to listOf(
                            mapOf("confidence" to "0.95", "content" to "안녕하세요")
                        ),
                        "type" to "pronunciation"
                    ),
                    mapOf(
                        "start_time" to "1.6",
                        "end_time" to "2.8",
                        "alternatives" to listOf(
                            mapOf("confidence" to "0.92", "content" to "저는")
                        ),
                        "type" to "pronunciation"
                    )
                )
            )
        )
    }
    
    private fun createMockRekognitionResult(): Map<String, Any> {
        return mapOf(
            "JobStatus" to "SUCCEEDED",
            "Faces" to listOf(
                mapOf(
                    "Timestamp" to 1000,
                    "Face" to mapOf(
                        "BoundingBox" to mapOf(
                            "Width" to 0.28,
                            "Height" to 0.39,
                            "Left" to 0.36,
                            "Top" to 0.30
                        ),
                        "AgeRange" to mapOf(
                            "Low" to 22,
                            "High" to 28
                        ),
                        "Smile" to mapOf(
                            "Value" to true,
                            "Confidence" to 87.5
                        ),
                        "Emotions" to listOf(
                            mapOf("Type" to "HAPPY", "Confidence" to 75.2),
                            mapOf("Type" to "CONFIDENT", "Confidence" to 68.5),
                            mapOf("Type" to "CALM", "Confidence" to 82.1)
                        ),
                        "Pose" to mapOf(
                            "Roll" to -1.2,
                            "Yaw" to 3.5,
                            "Pitch" to -0.8
                        ),
                        "Quality" to mapOf(
                            "Brightness" to 85.2,
                            "Sharpness" to 92.1
                        )
                    )
                )
            )
        )
    }
}

data class EvaluationRequest(
    val interviewId: String,
    val candidateName: String,
    val transcribeJobId: String? = null,
    val rekognitionJobId: String? = null
) 