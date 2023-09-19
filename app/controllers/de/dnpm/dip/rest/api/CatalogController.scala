package de.dnpm.dip.rest.api



import java.net.URI
import javax.inject.Inject
import scala.concurrent.{
  Future,
  ExecutionContext
}
import play.api.mvc.{
  Action,
  AnyContent,
  BaseController,
  ControllerComponents
}
import play.api.libs.json.Json.toJson
import de.dnpm.dip.catalog.api.CatalogService
import de.dnpm.dip.rest.util.{
  Collection,
  JsonOps
}



class CatalogController @Inject()(
  override val controllerComponents: ControllerComponents
)(
  implicit ec: ExecutionContext
)
extends BaseController
with JsonOps
{

  val catalogService =
    CatalogService
      .getInstance[Future]
      .get


  def codeSystemInfos: Action[AnyContent] =
    Action.async {
      catalogService.codeSystemInfos
        .map(Collection(_))
        .map(toJson(_))
        .map(Ok(_))

    }


  def codeSystem(
    uri: URI,
    version: Option[String]
  ): Action[AnyContent] =
    Action.async {
      catalogService.codeSystem(uri,version)
        .map(JsonResult(_))

    }



}
