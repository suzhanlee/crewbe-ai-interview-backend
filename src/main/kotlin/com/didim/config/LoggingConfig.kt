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