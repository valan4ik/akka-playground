package com.example

import java.io.Closeable
import java.util.concurrent.{TimeUnit, TimeoutException}

import org.jboss.netty.util.{HashedWheelTimer, Timeout, TimerTask}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.{Duration, MILLISECONDS}

object TimeoutScheduler{
  val timer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS)
  def scheduleTimeout(promise: Promise[_], after: Duration): Timeout = {
    timer.newTimeout(new TimerTask{
      def run(timeout: Timeout){
        promise.failure(new TimeoutException("Future timed out after " + after.toMillis + " millis"))
      }
    }, after.toNanos, TimeUnit.NANOSECONDS)
  }
}

object Utils {
  def usingResource[A <: Closeable, B](resource: A)(f: A => B): B = {
    try {
      f(resource)
    } finally {
      resource.close()
    }
  }

  def withTimeout[T](fut: Future[T],
                     timeoutMillis: Long
                    )(implicit ec: ExecutionContext): Future[T] = {
    val prom = Promise[T]()
    val timeout = Duration(timeoutMillis, MILLISECONDS)
    val timer = TimeoutScheduler.scheduleTimeout(prom, timeout)
    val combinedFut = Future.firstCompletedOf(List(fut, prom.future))
    fut onComplete{_ => timer.cancel()}
    combinedFut
  }
}