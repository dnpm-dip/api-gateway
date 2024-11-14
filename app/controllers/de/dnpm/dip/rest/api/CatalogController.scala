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
import play.api.cache.Cached
import play.api.libs.json.Json.toJson
import de.dnpm.dip.coding.{
  Coding,
  CodeSystem,
  ValueSet
}
import de.dnpm.dip.coding.atc.ATC
import de.dnpm.dip.catalog.api.CatalogService
import de.dnpm.dip.rest.util.{
  Collection,
  JsonOps
}
import de.dnpm.dip.rest.util.sapphyre.Hyper



class CatalogController @Inject()(
  val cached: Cached,
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


  def codeSystemInfos: Action[AnyContent] =
    Action.async {
      catalogService.codeSystemInfos
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


  private def getCodeSystem(
    uri: URI,
    version: Option[String],
    filters: Seq[String]
  ): Future[Option[CodeSystem[Any]]] = {

      import cats.syntax.traverse._

      val fltrs =
        toFilters(filters)

      (uri,version) match {
        // Workaround to handle ATC versioning inconsistency: 
        // If ATC CodeSystem is requested without a specific version,
        // return a concatenation of all ATC entries distinct by name
        case (u,None) if u == Coding.System[ATC].uri =>
          catalogService
            .codeSystemProvider(u)
            .flatMap(
              _.traverse { 
                atc =>
                  atc.versions
                    .toList
                    .sorted(atc.versionOrdering.reverse)
                    .traverse(version => catalogService.codeSystem(u,Some(version),fltrs))
                    .map( 
                      _.flatten
                       .reduce(
                         (acc,cs) =>
                           acc.copy(
                             version = None,
                             concepts = (acc.concepts ++ cs.concepts).distinctBy(_.display)
                           )
                       )
                  )
              }
            )

        case _ =>
          catalogService.codeSystem(uri,version,fltrs)
      }

    }


  def codeSystem(
    uri: URI,
    version: Option[String],
    filters: Seq[String]
  ) =
    cached.status(_.uri,OK){
      Action.async {
        getCodeSystem(uri,version,filters)
          .map(_.map(Hyper(_)))
          .map(JsonResult(_))
      }
    }


  def valueSetInfos: Action[AnyContent] =
    Action.async {
      catalogService.valueSetInfos
        .map(_.map(Hyper(_)))
        .map(Collection(_))
        .map(toJson(_))
        .map(Ok(_))
    }


  def valueSet(
    uri: URI,
    version: Option[String],
    filters: Seq[String]
  ) =
    cached.status(_.uri,OK){
      Action.async {
        catalogService.valueSet(uri,version)
          .filter(_.isDefined)
          .fallbackTo(
            getCodeSystem(uri,version,filters)
              .map(_.map(ValueSet.from(_)))
          )
          .map(_.map(Hyper(_)))
          .map(JsonResult(_))
      }
    }

}
