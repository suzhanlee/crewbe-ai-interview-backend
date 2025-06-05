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