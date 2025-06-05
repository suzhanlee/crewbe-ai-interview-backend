package com.didim.dto

import java.time.LocalDateTime

// 전체 면접 평가 결과
data class InterviewEvaluationResult(
    val interviewId: String,
    val candidateName: String,
    val evaluationDate: LocalDateTime,
    val overallScore: Double, // 0-100 점수
    val overallGrade: String, // A, B, C, D, F
    val speechAnalysis: SpeechAnalysisResult,
    val visualAnalysis: VisualAnalysisResult,
    val contentAnalysis: ContentAnalysisResult,
    val recommendations: List<String>,
    val detailedFeedback: String
)

// 음성 분석 결과 (Transcribe 기반)
data class SpeechAnalysisResult(
    val clarity: ScoreDetail, // 발음 명확도
    val fluency: ScoreDetail, // 유창성
    val pace: ScoreDetail, // 말하기 속도
    val volume: ScoreDetail, // 음성 크기
    val pauseAnalysis: PauseAnalysis, // 침묵/멈춤 분석
    val totalSpeakingTime: Double, // 총 발화 시간 (초)
    val wordCount: Int, // 총 단어 수
    val averageWordsPerMinute: Double // 분당 단어 수
)

// 시각 분석 결과 (Rekognition 기반)  
data class VisualAnalysisResult(
    val posture: ScoreDetail, // 자세
    val eyeContact: ScoreDetail, // 아이컨택
    val facialExpression: ScoreDetail, // 표정
    val professionalAppearance: ScoreDetail, // 전문적 외모
    val confidence: ScoreDetail, // 자신감 (감정 분석 기반)
    val emotionStability: ScoreDetail, // 감정 안정성
    val gestureAnalysis: GestureAnalysis // 제스처 분석
)

// 내용 분석 결과 (Transcribe 텍스트 기반)
data class ContentAnalysisResult(
    val answerCompleteness: ScoreDetail, // 답변 완성도
    val relevance: ScoreDetail, // 질문 연관성
    val professionalTermUsage: ScoreDetail, // 전문 용어 사용
    val serviceOrientation: ScoreDetail, // 서비스 지향성
    val problemSolvingSkill: ScoreDetail, // 문제 해결 능력
    val communicationSkill: ScoreDetail, // 의사소통 능력
    val keywordAnalysis: KeywordAnalysis // 키워드 분석
)

// 점수 세부 정보
data class ScoreDetail(
    val score: Double, // 0-100 점수
    val grade: String, // A, B, C, D, F
    val description: String, // 평가 설명
    val improvementSuggestion: String // 개선 제안
)

// 침묵/멈춤 분석
data class PauseAnalysis(
    val totalPauses: Int, // 총 멈춤 횟수
    val averagePauseDuration: Double, // 평균 멈춤 시간 (초)
    val longestPause: Double, // 최장 멈춤 시간 (초)
    val pauseFrequency: Double, // 분당 멈춤 횟수
    val appropriatePauses: Int, // 적절한 멈춤 횟수
    val excessivePauses: Int // 과도한 멈춤 횟수
)

// 제스처 분석
data class GestureAnalysis(
    val headMovement: MovementDetail, // 머리 움직임
    val handGestures: MovementDetail, // 손동작 (추정)
    val bodyStability: MovementDetail, // 몸의 안정성
    val facingDirection: FacingDetail // 얼굴 방향성
)

// 움직임 세부 정보
data class MovementDetail(
    val frequency: String, // HIGH, MEDIUM, LOW
    val appropriateness: String, // APPROPRIATE, EXCESSIVE, INSUFFICIENT
    val description: String
)

// 얼굴 방향성 세부 정보
data class FacingDetail(
    val averageYaw: Double, // 좌우 회전 평균
    val averagePitch: Double, // 상하 회전 평균
    val averageRoll: Double, // 기울기 평균
    val stability: String, // STABLE, UNSTABLE
    val eyeContactPercentage: Double // 아이컨택 비율 (추정)
)

// 키워드 분석
data class KeywordAnalysis(
    val serviceKeywords: List<DetectedKeyword>, // 서비스 관련 키워드
    val professionalKeywords: List<DetectedKeyword>, // 전문성 키워드
    val positiveKeywords: List<DetectedKeyword>, // 긍정적 키워드
    val confidenceKeywords: List<DetectedKeyword>, // 자신감 관련 키워드
    val formalLanguageUsage: Double // 정중한 언어 사용 비율
)

// 감지된 키워드
data class DetectedKeyword(
    val keyword: String,
    val count: Int,
    val timestamps: List<Double>, // 등장 시점들
    val context: String, // 사용 맥락
    val appropriateness: String // APPROPRIATE, INAPPROPRIATE
) 