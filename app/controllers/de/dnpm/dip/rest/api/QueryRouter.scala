package de.dnpm.dip.rest.api


import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._
import play.api.libs.json.Writes
import de.dnpm.dip.model.{
  Id,
  Patient
}
import de.dnpm.dip.service.query.{
  Query,
  UseCaseConfig
}
import de.dnpm.dip.rest.util.Extractor



object QueryId extends Extractor(Query.Id(_))

object PatId extends Extractor(Id[Patient](_))


abstract class QueryRouter[UseCase <: UseCaseConfig]
(
  private val pref: String
)
extends SimpleRouter
{

  val prefix =
    if (pref startsWith "/")
      pref
    else
      s"/$pref"


  val controller: QueryController[UseCase]


  final val baseRoutes: Routes = {

    case POST(p"/patient-record") =>
      controller.upload

    case DELETE(p"/patient/${PatId(patId)}") =>
      controller.delete(patId)


    case POST(p"/query") =>
      controller.submit

    case GET(p"/query/${QueryId(id)}") =>
      controller.get(id)

    case GET(p"/query"?q"id=${QueryId(id)}") =>
      controller.get(id)

    case PUT(p"/query/$id/filters") =>
      controller.applyFilters

    case PUT(p"/query/$id") =>
      controller.update

    case GET(p"/query/${QueryId(id)}/summary") =>
      controller.summary(id)

    case GET(p"/query/${QueryId(id)}/patients") =>
      controller.patientMatches(id)

    case GET(p"/query/${QueryId(id)}/patient-record/${PatId(patId)}") =>
      controller.patientRecord(id,patId)

  }


  val additionalRoutes: Routes =
    PartialFunction.empty


  override val routes: Routes =
    baseRoutes orElse additionalRoutes

}
