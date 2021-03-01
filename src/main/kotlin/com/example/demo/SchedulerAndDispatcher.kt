package com.example.demo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactor.asCoroutineDispatcher
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

object SchedulerAndDispatcher {

    val IO_SCHEDULER : Scheduler = Schedulers.newBoundedElastic(10, Int.MAX_VALUE, "reactor-io")
    val IO_DISPATCHER: CoroutineDispatcher = IO_SCHEDULER.asCoroutineDispatcher()
}