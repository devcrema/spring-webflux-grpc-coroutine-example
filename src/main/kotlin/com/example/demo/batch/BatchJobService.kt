package com.example.demo.batch

import com.example.demo.batch.BatchJobServiceOuterClass.GetBatchResultsRequest
import com.example.demo.batch.BatchJobServiceOuterClass.GetBatchResultsResponse
import org.lognet.springboot.grpc.GRpcService
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobInstance
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.explore.JobExplorer
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@GRpcService
class BatchJobService(private val jobExplorer: JobExplorer) : ReactorBatchJobServiceGrpc.BatchJobServiceImplBase() {

    override fun getBatchResults(request: Mono<GetBatchResultsRequest>): Mono<GetBatchResultsResponse> =
            request.subscribeOn(Schedulers.boundedElastic())
                    .map { requestProto ->
                        jobExplorer.findJobInstancesByJobName(
                                requestProto.jobName,
                                (requestProto.page * requestProto.size).toInt(),
                                requestProto.size.toInt())
                                .let { jobInstances ->
                                    GetBatchResultsResponse.newBuilder()
                                            .addAllJobInstances(jobInstances.map { it.toProto() })
                                            .build()
                                }
                    }

    fun JobInstance.toProto(): BatchJobServiceOuterClass.JobInstance = BatchJobServiceOuterClass.JobInstance.newBuilder()
            .setJobInstanceDescription(this.toString())
            .addAllJobExecutions(jobExplorer.getJobExecutions(this).map { it.toProto() })
            .build()

    fun JobExecution.toProto(): BatchJobServiceOuterClass.JobExecution = BatchJobServiceOuterClass.JobExecution.newBuilder()
            .setJobExecutionDescription(this.toString())
            .addAllStepExecutionDescription(this.stepExecutions.map { it.toProto() })
            .build()

    fun StepExecution.toProto() = this.toString()
}