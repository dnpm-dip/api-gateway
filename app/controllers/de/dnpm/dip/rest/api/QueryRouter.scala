package de.dnpm.dip.rest.api


import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._
import play.api.mvc.Results.{
  BadRequest,
  NotFound,
  Ok
}
import play.api.libs.json.{
  Json,
  JsObject
}
import de.dnpm.dip.model.{
  Id,
  Patient,
  Gender,
  VitalStatus,
  Site
}
import de.dnpm.dip.coding.{
  Coding,
  CodeSystem
}
import de.dnpm.dip.service.query.{
  Query,
  PatientFilter,
  PreparedQuery,
  UseCaseConfig
}
import de.dnpm.dip.rest.util.{
  Extractor,
  Outcome
}


abstract class QueryRouter[UseCase <: UseCaseConfig]
(
  private val pref: String
)
extends SimpleRouter
{

  import scala.util.chaining._

  private val QueryId =
    Extractor(Query.Id(_))

  private val PreparedQueryId =
    Extractor(PreparedQuery.Id(_))

  private val PatId =
    Extractor(Id[Patient](_))

  private val QueryMode =
    Extractor.AsCoding[Query.Mode.Value]

  private val Genders =
    Extractor.AsCodings[Gender.Value]

  private val VitalStatuses =
    Extractor.AsCodings[VitalStatus.Value]

  private val Sites =
    Extractor.AsCodingsOf[Site]


  val prefix =
    if (pref startsWith "/")
      pref
    else
      s"/$pref"


  protected val controller: QueryController[UseCase]

  protected val APPLICATION_JSON = "application/json"

  protected val jsonSchemas: Map[String,Map[String,JsObject]]


  final val baseRoutes: Routes = {

    case GET(p"/sites") => controller.sites


    // ------------------------------------------------------------------------
    // ETL Routes:
    // ------------------------------------------------------------------------
    case GET(p"/etl/patient-record/schema"
      ? q_o"version=$version"
      & q_o"format=$format") =>
      controller.Action {
        jsonSchemas(format.getOrElse(APPLICATION_JSON)).get(version.getOrElse("draft-12").toLowerCase) match {
          case Some(sch) =>
            Ok(sch)
          case None =>
            NotFound(
              Json.toJson(Outcome(s"Invalid JSON schema version, expected one of {${jsonSchemas.keys.mkString(",")}}"))
            )
        }
      }

    case POST(p"/etl/patient-record:validate") =>
      controller.validate

    case POST(p"/etl/patient-record") =>
      controller.upload

    case DELETE(p"/etl/patient/${PatId(patId)}") =>
      controller.deletePatient(patId)


    // ------------------------------------------------------------------------
    // Peer-to-peer Routes:
    // ------------------------------------------------------------------------

    case POST(p"/peer2peer/query") =>
      controller.peerToPeerQuery

    case POST(p"/peer2peer/patient-record") =>
      controller.patientRecordRequest


    // ------------------------------------------------------------------------
    // Query Routes:
    // ------------------------------------------------------------------------

    case POST(p"/queries") =>
      controller.submit

    case GET(p"/queries"?q"id=${QueryId(id)}") =>
      controller.get(id)

    case GET(p"/queries/${QueryId(id)}") =>
      controller.get(id)

    case DELETE(p"/queries"?q"id=${QueryId(id)}") =>
      controller.delete(id)

    case DELETE(p"/queries/${QueryId(id)}") =>
      controller.delete(id)

    case PUT(p"/queries/${QueryId(id)}") =>
      controller.update(id)

    case GET(p"/queries/${QueryId(id)}/summary") =>
      controller.summary(id)

    case GET(p"/queries/${QueryId(id)}/patient-matches"
             ? q_o"offset=${int(offset)}"
             ? q_o"limit=${int(limit)}") =>
      controller.patientMatches(offset,limit)(id)

    case GET(p"/queries/${QueryId(id)}/patients"
             ? q_o"offset=${int(offset)}"
             ? q_o"limit=${int(limit)}") =>
      controller.patientMatches(offset,limit)(id)

    case GET(p"/queries/${QueryId(id)}/patient-record"?q"id=${PatId(patId)}") =>
      controller.patientRecord(id,patId)

    case GET(p"/queries/${QueryId(id)}/patient-record/${PatId(patId)}") =>
      controller.patientRecord(id,patId)

    case GET(p"/queries") =>
      controller.queries


    // ------------------------------------------------------------------------
    // Prepared Query Routes:
    // ------------------------------------------------------------------------

    case POST(p"/prepared-queries") =>
      controller.createPreparedQuery

    case GET(p"/prepared-queries/${PreparedQueryId(id)}") =>
      controller.getPreparedQuery(id)

    case GET(p"/prepared-queries"?q"id=${PreparedQueryId(id)}") =>
      controller.getPreparedQuery(id)

    case GET(p"/prepared-queries") =>
      controller.getPreparedQueries

    case PATCH(p"/prepared-queries/${PreparedQueryId(id)}") =>
      controller.updatePreparedQuery(id)

    case PATCH(p"/prepared-queries"?q"id=${PreparedQueryId(id)}") =>
      controller.updatePreparedQuery(id)

    case DELETE(p"/prepared-queries/${PreparedQueryId(id)}") =>
      controller.deletePreparedQuery(id)

    case DELETE(p"/prepared-queries"?q"id=${PreparedQueryId(id)}") =>
      controller.deletePreparedQuery(id)

  }


  val additionalRoutes: Routes =
    PartialFunction.empty


  override lazy val routes: Routes =
    baseRoutes orElse additionalRoutes

}
