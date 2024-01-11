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
import de.dnpm.dip.coding.ValueSet
import de.dnpm.dip.catalog.api.CatalogService
import de.dnpm.dip.rest.util.{
  Collection,
  JsonOps
}
import de.dnpm.dip.rest.util.sapphyre.Hyper



class CatalogController @Inject()(
  override val controllerComponents: ControllerComponents
)(
  implicit ec: ExecutionContext
)
extends BaseController
with JsonOps
with CatalogHypermedia
{

  val catalogService =
    CatalogService
      .getInstance[Future]
      .get


  def infos: Action[AnyContent] =
    Action.async {
      catalogService.infos
        .map(_.map(Hyper(_)))
        .map(Collection(_))
        .map(toJson(_))
        .map(Ok(_))
    }


  private def toFilters(
    filters: Seq[String]
  ): Option[List[List[String]]] =
    Option(
      filters
        .map(_.split("\\|").toList)
        .toList
    )
    .filter(_.nonEmpty)


  def codeSystem(
    uri: URI,
    version: Option[String],
    filters: Seq[String]
  ): Action[AnyContent] =
    Action.async {
      catalogService.codeSystem(uri,version,toFilters(filters))
        .map(_.map(Hyper(_)))
        .map(JsonResult(_))
    }


  def valueSet(
    uri: URI,
    version: Option[String],
    filters: Seq[String]
  ): Action[AnyContent] =
    Action.async {
      catalogService.codeSystem(uri,version,toFilters(filters))
        .map(_.map(ValueSet.from(_)))
        .map(_.map(Hyper(_)))
        .map(JsonResult(_))
    }

}
