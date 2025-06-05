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