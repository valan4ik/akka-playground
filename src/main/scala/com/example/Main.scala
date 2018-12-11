package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

object BossyActor {
  def props(message: String, printerActor: ActorRef): Props = Props(new BossyActor(message, printerActor))
  case object Sleep
}

class BossyActor(message: String, printerActor: ActorRef) extends Actor {
  import BossyActor._
  import SleepyActor._

  def receive = {
    case Sleep =>
      val recipient = sender()
      printerActor ! GoToSleep("hello world")
  }
}

object SleepyActor {
  def props: Props = Props[SleepyActor]
  final case class GoToSleep(greeting: String)
}

class SleepyActor extends Actor with ActorLogging {
  import SleepyActor._


  def anotherFuture: Future[String] = {
    Future {
      log.info("Future in future")
      Thread.sleep(9000)
      log.info("Future in future ended")
      "going out of inception"
    }
  }

  def fallAsleep: Future[String] = {
    Future {
      log.info("Enterned first future")
      anotherFuture
      Thread.sleep(10000)
      log.info("Left first future")
      "Woke up"
    }
  }

  def receive = {
    case GoToSleep(greeting) => fallAsleep
  }
}

object AkkaQuickstart extends App {
  import BossyActor._

  implicit val DefaultSyncTimeout: Timeout = Timeout(2 seconds)
  val logger = Logger("MainLogger")
//  implicit val ec: ExecutionContext = system.dispatcher


  val system: ActorSystem = ActorSystem("helloAkka")

  val worker: ActorRef = system.actorOf(SleepyActor.props, "printerActor")

  val howdyGreeter: ActorRef =
    system.actorOf(BossyActor.props("MainActor", worker), "MainManagingActor")

  // =============================================
  // Test akka pattern and future/future in future
//  howdyGreeter ! Sleep
//  val future = howdyGreeter ? Sleep
//  future.map {
//    a => println("Future is finished")
//  }.recover {
//    case e: Exception => logger.info(e.getMessage)
//  }
  // =============================================

  // =============================================
  // Test timeouts
  howdyGreeter ! Sleep
  val future2 = howdyGreeter ? Sleep
  Utils.withTimeout(future2, 1500).map {
    a => println("Future is finished")
  }.recover {
    case e: Exception => logger.info(e.getMessage)
  }
  // =============================================

}

