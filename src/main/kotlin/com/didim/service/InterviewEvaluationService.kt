package com.didim.service

import com.didim.dto.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.math.abs

@Service
class InterviewEvaluationService {
    
    private val logger = LoggerFactory.getLogger(InterviewEvaluationService::class.java)
    
    // 승무원 면접 평가 기준
    private val serviceKeywords = listOf(
        "고객", "서비스", "안전", "친절", "도움", "배려", "소통", "협력", 
        "책임감", "정확성", "신속성", "전문성", "예의", "미소", "감사"
    )
    
    private val professionalKeywords = listOf(
        "승무원", "항공", "비행", "기내", "안전벨트", "구명조끼", "비상상황", 
        "응급처치", "기장", "부기장", "승객", "탑승", "착륙", "이륙"
    )
    
    private val positiveKeywords = listOf(
        "좋다", "훌륭하다", "최선", "열정", "노력", "성장", "발전", "개선", 
        "향상", "긍정적", "적극적", "자신감", "도전", "성취"
    )
    
    fun evaluateInterview(
        transcribeResult: Map<String, Any>,
        rekognitionResult: Map<String, Any>,
        interviewId: String,
        candidateName: String
    ): InterviewEvaluationResult {
        
        logger.info("🎯 면접 평가 시작: interviewId={}, candidate={}", interviewId, candidateName)
        
        // 1. 음성 분석
        val speechAnalysis = analyzeSpeech(transcribeResult)
        
        // 2. 시각 분석  
        val visualAnalysis = analyzeVisual(rekognitionResult)
        
        // 3. 내용 분석
        val contentAnalysis = analyzeContent(transcribeResult)
        
        // 4. 종합 점수 계산
        val overallScore = calculateOverallScore(speechAnalysis, visualAnalysis, contentAnalysis)
        val overallGrade = calculateGrade(overallScore)
        
        // 5. 추천사항 생성
        val recommendations = generateRecommendations(speechAnalysis, visualAnalysis, contentAnalysis)
        
        // 6. 상세 피드백 생성
        val detailedFeedback = generateDetailedFeedback(speechAnalysis, visualAnalysis, contentAnalysis)
        
        val result = InterviewEvaluationResult(
            interviewId = interviewId,
            candidateName = candidateName,
            evaluationDate = LocalDateTime.now(),
            overallScore = overallScore,
            overallGrade = overallGrade,
            speechAnalysis = speechAnalysis,
            visualAnalysis = visualAnalysis,
            contentAnalysis = contentAnalysis,
            recommendations = recommendations,
            detailedFeedback = detailedFeedback
        )
        
        logger.info("✅ 면접 평가 완료: 종합점수={}, 등급={}", overallScore, overallGrade)
        return result
    }
    
    private fun analyzeSpeech(transcribeResult: Map<String, Any>): SpeechAnalysisResult {
        // Transcribe 결과에서 텍스트와 타이밍 정보 추출
        val transcript = extractTranscript(transcribeResult)
        val items = extractItems(transcribeResult)
        
        // 발화 시간 계산
        val totalSpeakingTime = calculateSpeakingTime(items)
        val wordCount = transcript.split("\\s+".toRegex()).size
        val wordsPerMinute = if (totalSpeakingTime > 0) (wordCount * 60.0) / totalSpeakingTime else 0.0
        
        // 침묵 분석
        val pauseAnalysis = analyzePauses(items)
        
        return SpeechAnalysisResult(
            clarity = evaluateClarity(items),
            fluency = evaluateFluency(pauseAnalysis, wordsPerMinute),
            pace = evaluatePace(wordsPerMinute),
            volume = evaluateVolume(items), // confidence 기반 추정
            pauseAnalysis = pauseAnalysis,
            totalSpeakingTime = totalSpeakingTime,
            wordCount = wordCount,
            averageWordsPerMinute = wordsPerMinute
        )
    }
    
    private fun analyzeVisual(rekognitionResult: Map<String, Any>): VisualAnalysisResult {
        // Rekognition 결과에서 얼굴 정보 추출
        val faces = extractFaces(rekognitionResult)
        
        return VisualAnalysisResult(
            posture = evaluatePosture(faces),
            eyeContact = evaluateEyeContact(faces),
            facialExpression = evaluateFacialExpression(faces),
            professionalAppearance = evaluateProfessionalAppearance(faces),
            confidence = evaluateConfidence(faces),
            emotionStability = evaluateEmotionStability(faces),
            gestureAnalysis = analyzeGestures(faces)
        )
    }
    
    private fun analyzeContent(transcribeResult: Map<String, Any>): ContentAnalysisResult {
        val transcript = extractTranscript(transcribeResult)
        
        return ContentAnalysisResult(
            answerCompleteness = evaluateAnswerCompleteness(transcript),
            relevance = evaluateRelevance(transcript),
            professionalTermUsage = evaluateProfessionalTermUsage(transcript),
            serviceOrientation = evaluateServiceOrientation(transcript),
            problemSolvingSkill = evaluateProblemSolvingSkill(transcript),
            communicationSkill = evaluateCommunicationSkill(transcript),
            keywordAnalysis = analyzeKeywords(transcript)
        )
    }
    
    // 헬퍼 메서드들
    private fun extractTranscript(result: Map<String, Any>): String {
        return try {
            val results = result["results"] as? Map<String, Any>
            val transcripts = results?.get("transcripts") as? List<Map<String, Any>>
            transcripts?.firstOrNull()?.get("transcript") as? String ?: ""
        } catch (e: Exception) {
            logger.warn("Transcript 추출 실패", e)
            ""
        }
    }
    
    private fun extractItems(result: Map<String, Any>): List<Map<String, Any>> {
        return try {
            val results = result["results"] as? Map<String, Any>
            results?.get("items") as? List<Map<String, Any>> ?: emptyList()
        } catch (e: Exception) {
            logger.warn("Items 추출 실패", e)
            emptyList()
        }
    }
    
    private fun extractFaces(result: Map<String, Any>): List<Map<String, Any>> {
        return try {
            result["Faces"] as? List<Map<String, Any>> ?: emptyList()
        } catch (e: Exception) {
            logger.warn("Faces 추출 실패", e)
            emptyList()
        }
    }
    
    // 평가 메서드들 (간단한 구현 예시)
    private fun evaluateClarity(items: List<Map<String, Any>>): ScoreDetail {
        val avgConfidence = items.mapNotNull { item ->
            val alternatives = item["alternatives"] as? List<Map<String, Any>>
            alternatives?.firstOrNull()?.get("confidence") as? String
        }.mapNotNull { it.toDoubleOrNull() }.average()
        
        val score = (avgConfidence * 100).coerceIn(0.0, 100.0)
        return ScoreDetail(
            score = score,
            grade = calculateGrade(score),
            description = when {
                score >= 90 -> "매우 명확한 발음"
                score >= 80 -> "명확한 발음"
                score >= 70 -> "보통 수준의 발음"
                score >= 60 -> "다소 불명확한 발음"
                else -> "발음 개선 필요"
            },
            improvementSuggestion = if (score < 80) "발음 연습과 정확한 발성 훈련을 권장합니다." else "현재 수준을 유지하세요."
        )
    }
    
    private fun evaluatePace(wordsPerMinute: Double): ScoreDetail {
        val score = when {
            wordsPerMinute in 120.0..180.0 -> 100.0 // 이상적인 속도
            wordsPerMinute in 100.0..220.0 -> 85.0 // 양호한 속도
            wordsPerMinute in 80.0..250.0 -> 70.0 // 보통 속도
            else -> 50.0 // 너무 빠르거나 느림
        }
        
        return ScoreDetail(
            score = score,
            grade = calculateGrade(score),
            description = when {
                wordsPerMinute < 80 -> "말하기 속도가 너무 느립니다"
                wordsPerMinute > 250 -> "말하기 속도가 너무 빠릅니다"
                else -> "적절한 말하기 속도입니다"
            },
            improvementSuggestion = when {
                wordsPerMinute < 80 -> "조금 더 활발하게 말씀해 보세요"
                wordsPerMinute > 250 -> "천천히 또박또박 말씀해 보세요"
                else -> "현재 속도를 유지하세요"
            }
        )
    }
    
    private fun evaluateServiceOrientation(transcript: String): ScoreDetail {
        val serviceCount = serviceKeywords.sumOf { keyword ->
            transcript.lowercase().split(" ").count { it.contains(keyword) }
        }
        
        val score = (serviceCount * 10.0).coerceIn(0.0, 100.0)
        
        return ScoreDetail(
            score = score,
            grade = calculateGrade(score),
            description = if (serviceCount > 5) "서비스 지향적 답변" else "서비스 마인드 보완 필요",
            improvementSuggestion = if (serviceCount < 3) "고객 서비스와 관련된 경험을 더 구체적으로 언급해 보세요" else "훌륭한 서비스 마인드입니다"
        )
    }
    
    // 기타 평가 메서드들은 비슷한 패턴으로 구현...
    
    private fun calculateOverallScore(
        speech: SpeechAnalysisResult,
        visual: VisualAnalysisResult, 
        content: ContentAnalysisResult
    ): Double {
        return (speech.clarity.score * 0.2 + 
                speech.fluency.score * 0.15 + 
                visual.confidence.score * 0.25 + 
                visual.facialExpression.score * 0.15 + 
                content.serviceOrientation.score * 0.25)
    }
    
    private fun calculateGrade(score: Double): String {
        return when {
            score >= 90 -> "A"
            score >= 80 -> "B" 
            score >= 70 -> "C"
            score >= 60 -> "D"
            else -> "F"
        }
    }
    
    private fun generateRecommendations(
        speech: SpeechAnalysisResult,
        visual: VisualAnalysisResult,
        content: ContentAnalysisResult
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (speech.clarity.score < 75) {
            recommendations.add("발음을 더욱 명확하게 하기 위한 발성 연습을 권장합니다")
        }
        if (visual.confidence.score < 75) {
            recommendations.add("자신감 있는 표정과 자세를 연습해 보세요")
        }
        if (content.serviceOrientation.score < 75) {
            recommendations.add("고객 서비스에 대한 구체적인 경험과 사례를 준비해 보세요")
        }
        
        return recommendations
    }
    
    private fun generateDetailedFeedback(
        speech: SpeechAnalysisResult,
        visual: VisualAnalysisResult,
        content: ContentAnalysisResult
    ): String {
        return """
        [음성 평가]
        - 발음 명확도: ${speech.clarity.description}
        - 말하기 속도: ${speech.pace.description}
        
        [시각 평가]  
        - 자신감: ${visual.confidence.description}
        - 표정: ${visual.facialExpression.description}
        
        [내용 평가]
        - 서비스 지향성: ${content.serviceOrientation.description}
        - 의사소통 능력: ${content.communicationSkill.description}
        """.trimIndent()
    }
    
    // 나머지 평가 메서드들의 스텁 구현
    private fun analyzePauses(items: List<Map<String, Any>>): PauseAnalysis = 
        PauseAnalysis(0, 0.0, 0.0, 0.0, 0, 0)
    
    private fun evaluateFluency(pauseAnalysis: PauseAnalysis, wpm: Double): ScoreDetail = 
        ScoreDetail(75.0, "B", "원활한 유창성", "계속 유지하세요")
        
    private fun evaluateVolume(items: List<Map<String, Any>>): ScoreDetail = 
        ScoreDetail(80.0, "B", "적절한 음성 크기", "현 수준 유지")
        
    private fun calculateSpeakingTime(items: List<Map<String, Any>>): Double = 60.0
    
    private fun evaluatePosture(faces: List<Map<String, Any>>): ScoreDetail = 
        ScoreDetail(75.0, "B", "안정적인 자세", "현 상태 유지")
        
    private fun evaluateEyeContact(faces: List<Map<String, Any>>): ScoreDetail = 
        ScoreDetail(80.0, "B", "적절한 시선 처리", "좋습니다")
        
    private fun evaluateFacialExpression(faces: List<Map<String, Any>>): ScoreDetail = 
        ScoreDetail(85.0, "A", "긍정적인 표정", "훌륭합니다")
        
    private fun evaluateProfessionalAppearance(faces: List<Map<String, Any>>): ScoreDetail = 
        ScoreDetail(90.0, "A", "전문적인 외모", "완벽합니다")
        
    private fun evaluateConfidence(faces: List<Map<String, Any>>): ScoreDetail = 
        ScoreDetail(78.0, "B", "적절한 자신감", "더 당당하게")
        
    private fun evaluateEmotionStability(faces: List<Map<String, Any>>): ScoreDetail = 
        ScoreDetail(82.0, "B", "안정적인 감정", "좋습니다")
        
    private fun analyzeGestures(faces: List<Map<String, Any>>): GestureAnalysis =
        GestureAnalysis(
            MovementDetail("MEDIUM", "APPROPRIATE", "적절한 머리 움직임"),
            MovementDetail("LOW", "APPROPRIATE", "절제된 손동작"),
            MovementDetail("HIGH", "APPROPRIATE", "안정적인 자세"),
            FacingDetail(2.0, -1.0, 0.5, "STABLE", 85.0)
        )
    
    private fun evaluateAnswerCompleteness(transcript: String): ScoreDetail = 
        ScoreDetail(70.0, "C", "답변 완성도 보통", "더 구체적인 답변 필요")
        
    private fun evaluateRelevance(transcript: String): ScoreDetail = 
        ScoreDetail(80.0, "B", "질문과 연관성 높음", "좋습니다")
        
    private fun evaluateProfessionalTermUsage(transcript: String): ScoreDetail = 
        ScoreDetail(65.0, "C", "전문용어 사용 부족", "항공 관련 용어 학습 권장")
        
    private fun evaluateProblemSolvingSkill(transcript: String): ScoreDetail = 
        ScoreDetail(75.0, "B", "문제해결 능력 양호", "사례 중심 답변 권장")
        
    private fun evaluateCommunicationSkill(transcript: String): ScoreDetail = 
        ScoreDetail(80.0, "B", "원활한 의사소통", "현 수준 유지")
        
    private fun analyzeKeywords(transcript: String): KeywordAnalysis =
        KeywordAnalysis(
            emptyList(), emptyList(), emptyList(), emptyList(), 0.8
        )
} 