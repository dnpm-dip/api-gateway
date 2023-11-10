package de.dnpm.dip.rest.api


import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._
import play.api.mvc.Results.{
  BadRequest,
  Ok
}
import play.api.libs.json.{
  Json,
  Writes
}
import de.dnpm.dip.model.{
  Id,
  Patient,
  Gender,
  VitalStatus
}
import de.dnpm.dip.coding.{
  Coding,
  CodeSystem
}
import de.dnpm.dip.service.query.{
  Query,
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

  private val PatId =
    Extractor(Id[Patient](_))

  private val QueryMode =
    Extractor.Coding[Query.Mode.Value]

  private val Genders =
    Extractor.Codings[Gender.Value]

  private val VitalStatuses =
    Extractor.Codings[VitalStatus.Value]


  val prefix =
    if (pref startsWith "/")
      pref
    else
      s"/$pref"



  val controller: QueryController[UseCase]


  final val baseRoutes: Routes = {

    case POST(p"/etl/patient-record") =>
      controller.upload

    case DELETE(p"/etl/patient/${PatId(patId)}") =>
      controller.deletePatient(patId)


    case POST(p"/peer2peer/query") =>
      controller.peerToPeerQuery

    case POST(p"/peer2peer/patient-record") =>
      controller.patientRecordRequest



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


//    case PUT(p"/queries/${QueryId(id)}/filters") =>
//      controller.applyFilters(id)

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
             ? q_o"limit=${int(limit)}"
             ? q_s"gender=${Genders(genders)}"
             ? q_o"age[min]=${int(ageMin)}"
             ? q_o"age[max]=${int(ageMax)}"
             ? q_s"vitalStatus=${VitalStatuses(vitalStatus)}") =>
      controller.patientMatches(offset,limit,genders,ageMin,ageMax,vitalStatus)(id)

    //TODO: remove
    case GET(p"/queries/${QueryId(id)}/patients"
             ? q_o"offset=${int(offset)}"
             ? q_o"limit=${int(limit)}"
             ? q_s"gender=${Genders(genders)}"
             ? q_o"age[min]=${int(ageMin)}"
             ? q_o"age[max]=${int(ageMax)}"
             ? q_s"vitalStatus=${VitalStatuses(vitalStatus)}") =>
      controller.patientMatches(offset,limit,genders,ageMin,ageMax,vitalStatus)(id)


    case GET(p"/queries/${QueryId(id)}/patient-record"?q"id=${PatId(patId)}") =>
      controller.patientRecord(id,patId)

    case GET(p"/queries/${QueryId(id)}/patient-record/${PatId(patId)}") =>
      controller.patientRecord(id,patId)

    case GET(p"/queries") =>
      controller.queries

  }


  val additionalRoutes: Routes =
    PartialFunction.empty


  override lazy val routes: Routes =
    baseRoutes orElse additionalRoutes

}
