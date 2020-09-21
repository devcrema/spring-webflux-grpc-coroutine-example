package com.example.demo.batch

import com.example.demo.entity.User
import com.example.demo.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
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
                User(email = "$it@email.com", enabled = true, lastAccessedAt = LocalDateTime.now(UTC).minusYears(2))
            }.let { userRepository.saveAll(it) }
                    .let { log.info("test data generate") }

    fun runInactiveUserJob(pageSize: Int = 10, criteriaTime: LocalDateTime = LocalDateTime.now(UTC).minusYears(1)) =
            JobParametersBuilder()
                    .addLong("pageSize", 10)
                    .addString("criteriaTime", criteriaTime.toString())
                    /**
                     * job parameter 가 완전히 동일한 경우, 같은 job 이라고 판단하여서 동시에 실행할 수 없게 된다.
                     * 만약 매번 동시에 실행이 가능해야하는 job이라면 아래와 같이 랜덤한 값을 추가로 주면 된다.
                     * .addString("jobId", UUID.randomUUID().toString())
                     */
                    .toJobParameters() // job parameter 로 scope bean 에 value 전달 가능
                    .let { jobParameters -> jobLauncher.run(inactiveUserJob, jobParameters) } // run batch

    // 스케줄링 job 설정
//    @Scheduled(fixedDelay = 10 * 1000, initialDelay = 10 * 1000)
//    fun runSchedulingJob() = runInactiveUserJob(
//            pageSize = 10,
//            criteriaTime = LocalDateTime.now(UTC).minusYears(1) // 1년전 활동 유저 비활성화
//    )

}