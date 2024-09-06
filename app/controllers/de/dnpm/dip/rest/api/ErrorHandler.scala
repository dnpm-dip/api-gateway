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
  Result,
  Results
}
import play.api.mvc.ControllerHelpers.BAD_REQUEST
import play.api.mvc.Results.{
  BadRequest,
  InternalServerError
}
import play.api.routing.Router
import play.api.libs.json.Json.toJson
import scala.concurrent.Future
import de.dnpm.dip.util.Logging
import de.dnpm.dip.rest.util.Outcome



@Singleton
class ErrorHandler @Inject() (
  env: Environment,
  config: Configuration,
  sourceMapper: OptionalSourceMapper,
  router: Provider[Router]
)
extends DefaultHttpErrorHandler(env,config,sourceMapper,router)
with Logging
{

  import scala.util.chaining._

  override def onClientError(
    request: RequestHeader,
    statusCode: Int,
    message: String
  ): Future[Result] = 
    Outcome(s"Error for request '${request.method} ${request.path}' Status: $statusCode, Message: $message")
      .pipe(toJson(_))
      .pipe(Results.Status(statusCode)(_))
      .pipe(Future.successful(_))


  override def onServerError(
    request: RequestHeader,
    exception: Throwable
  ): Future[Result] =
    exception match {
      case e: IllegalArgumentException =>
        onClientError(
          request,
          BAD_REQUEST,
          e.getMessage
        )

      case _ =>
        Outcome("Server error: " + exception.getMessage)
          .pipe(toJson(_))
          .pipe(InternalServerError(_))
          .pipe(Future.successful(_))
          .tap(_ => log.error("Server error",exception))
    }


/*      
  override def onServerError(
    request: RequestHeader,
    exception: Throwable
  ): Future[Result] =
    Outcome("Server error: " + exception.getMessage)
      .pipe(toJson(_))
      .pipe(InternalServerError(_))
      .pipe(Future.successful(_))
      .tap(_ => log.error("Server error",exception))
*/

}
