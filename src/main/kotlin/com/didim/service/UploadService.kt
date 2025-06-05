package com.didim.service

import com.didim.entity.UploadRecord
import com.didim.repository.UploadRecordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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