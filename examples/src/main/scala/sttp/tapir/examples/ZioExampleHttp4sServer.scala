package sttp.tapir.examples

import org.http4s._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{Has, IO, Runtime, Task, UIO, ZIO, ZLayer, ZEnv}
import sttp.tapir.ztapir._
import sttp.tapir.server.http4s.ztapir._
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import cats.syntax.all._
import UserLayer.UserService
import sttp.tapir.examples.ZioExampleHttp4sServer.Pet
import zio.console.Console

object ZioExampleHttp4sServer extends App {
  case class Pet(species: String, url: String)

  import io.circe.generic.auto._
  import sttp.tapir.json.circe._

  // Sample endpoint, with the logic implemented directly using .toRoutes
  val petEndpoint: ZEndpoint[Int, String, Pet] =
    endpoint.get.in("pet" / path[Int]("petId")).errorOut(stringBody).out(jsonBody[Pet])

  val petRoutes: HttpRoutes[Task] = petEndpoint.toRoutes { petId =>
    if (petId == 35) {
      UIO(Pet("Tapirus terrestris", "https://en.wikipedia.org/wiki/Tapir"))
    } else {
      IO.fail("Unknown pet id")
    }
  }

  // Same endpoint as above, but using a custom application layer
  val pet2Endpoint: ZEndpoint[Int, String, Pet] =
    endpoint.get.in("pet2" / path[Int]("petId")).errorOut(stringBody).out(jsonBody[Pet])

  val pet2Routes: HttpRoutes[Task] = pet2Endpoint.toRoutes(petId => UserService.hello(petId).provideLayer(UserLayer.liveEnv))

  // Final service is just a conjunction of different Routes
  implicit val runtime: Runtime[ZEnv] = Runtime.default
  val service: HttpRoutes[Task] = petRoutes <+> pet2Routes

  //
  // Same as above, but combining endpoint description with server logic:
  //

  val petServerEndpoint = petEndpoint.zServerLogic { petId =>
    if (petId == 35) {
      UIO(Pet("Tapirus terrestris", "https://en.wikipedia.org/wiki/Tapir"))
    } else {
      IO.fail("Unknown pet id")
    }
  }
  val petServerRoutes: HttpRoutes[Task] = petServerEndpoint.toRoutes

  val pet2ServerEndpoint = pet2Endpoint.zServerLogic { petId => UserService.hello(petId).provideLayer(UserLayer.liveEnv) }
  val pet2ServerRoutes: HttpRoutes[Task] = petServerEndpoint.toRoutes

  import sttp.tapir.docs.openapi._
  import sttp.tapir.openapi.circe.yaml._
  val yaml = List(petEndpoint).toOpenAPI("Our pets", "1.0").toYaml

  val serve = BlazeServerBuilder[Task](runtime.platform.executor.asEC)
    .bindHttp(8080, "localhost")
    .withHttpApp(Router("/" -> (service <+> new SwaggerHttp4s(yaml).routes[Task])).orNotFound)
    .serve
    .compile
    .drain

  runtime.unsafeRun(serve)
}

object UserLayer {
  type UserService = Has[UserService.Service]

  object UserService {
    trait Service {
      def hello(id: Int): ZIO[Any, String, Pet]
    }

    val live: ZLayer[Console, Nothing, Has[Service]] = ZLayer.fromFunction { console: Console => (id: Int) =>
      {
        console.get.putStrLn(s"Got Pet request for $id") >>
          ZIO.succeed(Pet(id.toString, "https://zio.dev"))
      }
    }

    def hello(id: Int): ZIO[UserService, String, Pet] = ZIO.accessM(_.get.hello(id))
  }

  val liveEnv: ZLayer[Any, Nothing, Has[UserService.Service]] = Console.live >>> UserService.live
}
