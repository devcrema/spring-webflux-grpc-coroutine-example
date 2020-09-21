package com.example.demo.batch

import com.example.demo.entity.User
import com.example.demo.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.DeadlockLoserDataAccessException
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.EnableScheduling
import java.lang.Thread.sleep
import java.time.LocalDateTime

@Suppress("SpringElInspection")
@Configuration
@EnableBatchProcessing
@EnableScheduling
class InactiveUserJobConfiguration(
        private val userRepository: UserRepository,
        private val inactiveUserItemReader: ItemReader<User>,
        private val inactiveUserProcessor: ItemProcessor<User, User>,
        private val inactiveUserWriter: ItemWriter<User>
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // job 설정
    @Bean
    fun inactiveUserJob(jobBuilderFactory: JobBuilderFactory, inactiveJobStep: Step): Job =
            jobBuilderFactory["inactiveUserJob"]
                    .start(inactiveJobStep) // 시작
                    .listener(object : JobExecutionListener {
                        override fun beforeJob(jobExecution: JobExecution) {
                            log.info("before job")
                        }

                        override fun afterJob(jobExecution: JobExecution) {
                            log.info("after Job")
                        }
                    })
                    .build()

    // step 설정
    @Bean
    @JobScope
    fun inactiveJobStep(stepBuilderFactory: StepBuilderFactory, @Value("#{jobParameters[pageSize]}") pageSize: Int): Step =
            stepBuilderFactory["inactiveUserStep"]
                    .chunk<User, User>(pageSize) // 청크(데이터 단위) 설정, <input, output>
                    .reader(inactiveUserItemReader) // reader, processor, writer 설정
                    .processor(inactiveUserProcessor)
                    .writer(inactiveUserWriter)
                    .faultTolerant().retryLimit(2).retry(DeadlockLoserDataAccessException::class.java) // when deadlock then retry
                    .build()


    // reader 설정
    @Bean
    @StepScope
    fun inactiveUserItemReader(
            @Value("#{jobParameters[pageSize]}") pageSize: Int,
            @Value("#{jobParameters[criteriaTime]}") criteriaTime: String): RepositoryItemReader<User> =
            RepositoryItemReaderBuilder<User>()
                    .repository(userRepository)
                    .methodName("findAllByLastAccessedAtBefore")
                    .pageSize(pageSize)
                    .maxItemCount(Int.MAX_VALUE)
                    .arguments(listOf(LocalDateTime.parse(criteriaTime)))
                    .sorts(mapOf("id" to Sort.Direction.ASC))
                    .name("inactiveUserItemReader")
                    .build()

    // processor 설정
    @Bean
    @StepScope
    fun inactiveUserProcessor(): ItemProcessor<User, User> =
            ItemProcessor { user ->
                user.also { it.enabled = false }
                        .also { sleep(500) } // for test
                        .also { log.info("process data: $it") }
            }

    // writer 설정
    @Bean
    @StepScope
    fun inactiveUserWriter(): ItemWriter<User> =
            ItemWriter { users: List<User> ->
                userRepository.saveAll(users)
                        .also { log.info("write data: $it") }
            }
}
