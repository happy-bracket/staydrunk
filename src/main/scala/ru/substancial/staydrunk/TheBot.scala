package ru.substancial.staydrunk

import java.util.concurrent.ConcurrentHashMap

import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.api.{ChatActions, RequestHandler}
import com.bot4s.telegram.clients.FutureSttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.methods.SendVoice
import com.bot4s.telegram.models.InputFile
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import com.softwaremill.sttp.{SttpBackend, SttpBackendOptions}
import io.grpc.StatusRuntimeException
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

import scala.concurrent.Future

class TheBot(val goer: Goer[Future], val burpBytes: Array[Byte]) extends TelegramBot
  with Polling
  with Commands[Future]
  with ChatActions[Future] {

  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  implicit val backend: SttpBackend[Future, Nothing] = OkHttpFutureBackend(
    options = SttpBackendOptions.socksProxy("127.0.0.1", 1080)
  )
  override val client: RequestHandler[Future] = new FutureSttpClient(
    ""
  )

  private val stages: ConcurrentHashMap[Int, Stage] = new ConcurrentHashMap

  onCommand("where_to_drink") { implicit msg =>
    val userId = msg.from.get.id
    stages.put(userId, WLForDirections)
    reply("Мнееээ... грррх... нужна твояаээ.. гее--юю гееолока.. цияб,юю..").map(_ => Unit)
  }

  onCommand("the_good_place") { implicit msg =>
    val userId = msg.from.get.id
    stages.put(userId, WLForAssertion)
    reply("Мнееээ... грррх... нужна твояаээ.. гее--юю гееолока.. цияб,юю..").map(_ => Unit)
  }

  onMessage { implicit msg =>
    if (msg.text.fold {
      true
    } { t => !t.startsWith("/") }) {
      val userId = msg.from.get.id
      val stage = stages.get(userId)
      val loc = msg.location
      loc.fold {
        reply("Со мной не разговаривают. Иди отсюда").flatMap(_ => unit)
      } { l =>
        stages.remove(userId)
        stage match {
          case null => for {
            _ <- reply("Ты дурак чтоли? Ты зачем мне это прислал?")
          } yield unit
          case WLForDirections => for {
            _ <- reply("Ща я тебе чего-нибудь найду...")
            _ <- uploadingPhoto
            res <- goer.goForDirections(l).recover { case _ => "" }
            _ <- reply(if (res == "") "Скучен город, в котором негде пить." else res)
            burp = InputFile("burp1488", burpBytes)
            _ <- request(SendVoice(msg.source, burp))
          } yield Unit
          case WLForAssertion => for {
            _ <- reply("Щаа...")
            _ <- typing
            _ <- goer.goForAssertions(l)
            _ <- reply("Нормально, записал.")
          } yield Unit
        }
      }
    } else {
      unit
    }.recoverWith {
      case _ => reply("Скучен город, в котором негде пить.").map(_ => Unit)
    }
  }

}

sealed trait Stage

object WLForDirections extends Stage

object WLForAssertion extends Stage