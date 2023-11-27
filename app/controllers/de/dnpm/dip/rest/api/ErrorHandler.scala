package de.dnpm.dip.rest.api



import javax.inject.{
  Inject,
  Provider,
  Singleton
}
import play.api.{
  Environment,
  Configuration,
  OptionalSourceMapper
}
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.{
  RequestHeader,
  Result
}
import play.api.mvc.Results.InternalServerError
import play.api.routing.Router
import play.api.libs.json.Json.toJson
import scala.concurrent.Future
import de.dnpm.dip.rest.util.Outcome



@Singleton
class ErrorHandler @Inject() (
  env: Environment,
  config: Configuration,
  sourceMapper: OptionalSourceMapper,
  router: Provider[Router]
)
extends DefaultHttpErrorHandler(env,config,sourceMapper,router)
{

  import scala.util.chaining._


  override def onServerError(
    request: RequestHeader,
    exception: Throwable
  ): Future[Result] =
    Future.successful(
      Outcome("A server error occurred: " + exception.getMessage)
        .pipe(toJson(_))
        .pipe(InternalServerError(_))
    )

}
