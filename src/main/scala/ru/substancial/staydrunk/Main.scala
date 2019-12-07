package ru.substancial.staydrunk

import java.io.BufferedReader
import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import domain.StayDrunkClient

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object Main {

  def main(args: Array[String]): Unit = {

    implicit val sys: ActorSystem = ActorSystem("HelloWorldClient")
    implicit val mat: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = sys.dispatcher

    val client = StayDrunkClient(GrpcClientSettings.fromConfig("domain.StayDrunk"))
    val goer = new OkGoer(client)

    val burpBytes = Files.readAllBytes(Paths.get(getClass.getResource("/burp.mp3").toURI))

    val bot = new TheBot(goer, burpBytes)
    val eol = bot.run()

    println("Vse v poryade")

    Await.result(eol, Duration.Inf)

  }

}
