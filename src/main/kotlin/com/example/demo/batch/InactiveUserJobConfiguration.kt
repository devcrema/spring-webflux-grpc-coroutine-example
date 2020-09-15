package com.example.demo.batch

import com.example.demo.entity.User
import com.example.demo.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.DeadlockLoserDataAccessException
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID
import javax.annotation.PostConstruct

@Configuration
@EnableBatchProcessing
@EnableScheduling
class InactiveUserJobConfiguration(
        private val userRepository: UserRepository,
        private val jobLauncher: JobLauncher,
        private val jobBuilderFactory: JobBuilderFactory,
        private val stepBuilderFactory: StepBuilderFactory
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // test data
    @PostConstruct
    fun testDataGenerator(): Unit =
            (1..110).toList().map {
                User(email = "$it@email.com", enabled = true, lastAccessedAt = LocalDateTime.now(UTC).minusYears(2))
            }.let { userRepository.saveAll(it) }

    // 스케줄링 job 설정
    @Scheduled(fixedDelay = 30 * 1000, initialDelay = 30 * 1000)
    fun runSchedulingJob() =
            JobParametersBuilder().addString("JobID", UUID.randomUUID().toString())
                    .toJobParameters() // job parameter 로 scope bean 에 value 전달 가능
                    .also { println("data job before all: ${userRepository.findAll()}") }
                    .let { jobLauncher.run(inactiveUserJob(jobBuilderFactory, inactiveJobStep(stepBuilderFactory)), it) } // run batch
                    .also { log.info("data job after all: ${userRepository.findAll()}") }

    // job 설정
    @Bean
    fun inactiveUserJob(jobBuilderFactory: JobBuilderFactory, inactiveJobStep: Step): Job =
            jobBuilderFactory["inactiveUserJob"]
                    .start(inactiveJobStep) // 시작
                    .build()

    // step 설정
    @Bean
    fun inactiveJobStep(stepBuilderFactory: StepBuilderFactory): Step =
            stepBuilderFactory["inactiveUserStep"]
                    .chunk<User, User>(10) // 청크(데이터 단위) 설정, <input, output>
                    .reader(inactiveUserItemReader()) // reader, processor, writer 설정
                    .processor(inactiveUserProcessor())
                    .writer(inactiveUserWriter())
                    .faultTolerant().retryLimit(2).retry(DeadlockLoserDataAccessException::class.java) // when deadlock then retry
                    .build()


    // reader 설정
    @Bean
    @StepScope
    fun inactiveUserItemReader(): RepositoryItemReader<User> =
            RepositoryItemReaderBuilder<User>()
                    .repository(userRepository)
                    .methodName("findAllByLastAccessedAtBefore")
                    .pageSize(10)
                    .maxItemCount(Int.MAX_VALUE)
                    .arguments(listOf(LocalDateTime.now(UTC).minusYears(1))) // 1년전 활동 유저 비활성화
                    .sorts(mapOf("id" to Sort.Direction.ASC))
                    .name("inactiveUserItemReader")
                    .build()

    // processor 설정
    @Bean
    fun inactiveUserProcessor(): ItemProcessor<User, User> =
            ItemProcessor { user ->
                user.also { it.enabled = false }
                        .also { log.info("process data: $it") }
            }

    // writer 설정
    @Bean
    fun inactiveUserWriter(): ItemWriter<User> =
            ItemWriter { users: List<User> ->
                userRepository.saveAll(users)
                        .also { log.info("write data: $it") }
            }
}
