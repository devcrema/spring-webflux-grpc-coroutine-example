package com.example.demo.batch

import com.example.demo.entity.User
import com.example.demo.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.annotation.PostConstruct

@Component
class InactiveUserJobScheduler(
        private val userRepository: UserRepository,
        private val jobLauncher: JobLauncher,
        private val inactiveUserJob: Job
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // test data
    @PostConstruct
    fun testDataGenerator(): Unit =
            (1..110).toList().map {
                User(email = "$it@email.com", enabled = true, lastAccessedAt = LocalDateTime.now(ZoneOffset.UTC).minusYears(2))
            }.let { userRepository.saveAll(it) }
                    .let { log.info("test data generate") }

    // 스케줄링 job 설정
    @Scheduled(fixedDelay = 15 * 1000, initialDelay = 15 * 1000)
    fun runSchedulingJob() =
            JobParametersBuilder().addString("JobID", UUID.randomUUID().toString())
                    .toJobParameters() // job parameter 로 scope bean 에 value 전달 가능
                    .let { jobParameters -> jobLauncher.run(inactiveUserJob, jobParameters) } // run batch
}