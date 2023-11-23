package de.dnpm.dip.rest.api


import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._
import play.api.mvc.Results.{
  BadRequest,
  Ok
}
import play.api.libs.json.Json
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



  val controller: QueryController[UseCase]


  final val baseRoutes: Routes = {

    // ------------------------------------------------------------------------
    // ETL Routes:
    // ------------------------------------------------------------------------

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

    case POST(p"/queries"?q"mode=$mode") =>
      mode match {
        case QueryMode(md) =>
          controller.submit(md)

        case _ =>
          controller.Action {
            BadRequest(
              Json.toJson(
                Outcome(s"Invalid Query Mode value, expected one of: {${Query.Mode.values.mkString(",")}}")
              )
            )
          }
      }

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

    case PUT(p"/queries/${QueryId(id)}"?q"mode=$mode") =>
      mode match {
        case QueryMode(md) =>
          controller.update(id,Some(md))

        case _ =>
          controller.Action {
            BadRequest(
              Json.toJson(
                Outcome(s"Invalid Query Mode value, expected one of: {${Query.Mode.values.mkString(",")}}")
              )
            )
          }
      }

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


/*
    case GET(p"/queries/${QueryId(id)}/patient-matches"
             ? q_o"offset=${int(offset)}"
             ? q_o"limit=${int(limit)}"
             ? q_s"gender=${Genders(genders)}"
             ? q_o"age[min]=${int(ageMin)}"
             ? q_o"age[max]=${int(ageMax)}"
             ? q_s"vitalStatus=${VitalStatuses(vitalStatus)}"
             ? q_s"site=${Sites(sites)}") =>
      controller.patientMatches(
        offset,
        limit,
        PatientFilter(
          Option(genders),
          ageMin,
          ageMax,
          Option(vitalStatus),
          Option(sites)
        )
      )(id)


    // TODO: remove redundant endpoint
    case GET(p"/queries/${QueryId(id)}/patients"
             ? q_o"offset=${int(offset)}"
             ? q_o"limit=${int(limit)}"
             ? q_s"gender=${Genders(genders)}"
             ? q_o"age[min]=${int(ageMin)}"
             ? q_o"age[max]=${int(ageMax)}"
             ? q_s"vitalStatus=${VitalStatuses(vitalStatus)}"
             ? q_s"site=${Sites(sites)}") =>
      controller.patientMatches(
        offset,
        limit,
        PatientFilter(
          Option(genders),
          ageMin,
          ageMax,
          Option(vitalStatus),
          Option(sites)
        )
      )(id)
*/

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
