package de.dnpm.dip.rest.api


import java.time.LocalDateTime
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._
import play.api.mvc.Results.{
  NotFound,
  Ok
}
import play.api.libs.json.{
  Json,
  JsObject
}
import de.dnpm.dip.coding.Coding
import de.dnpm.dip.model.{
  Id,
  Patient,
  Site
}
import de.dnpm.dip.service.mvh.{
  TransferTAN,
  Submission
}
import de.dnpm.dip.service.query.{
  Querier,
  Query,
  PreparedQuery,
  UseCaseConfig
}
import de.dnpm.dip.rest.util.{
  Extractor,
  Outcome
}
import shapeless.Witness


abstract class UseCaseRouter[UseCase <: UseCaseConfig]
(
  private val pref: String
)
extends SimpleRouter
{

  this: FakeDataGen[UseCase#PatientRecord] =>


  protected val querier =
    Extractor(Querier(_))

  protected val Origin: Extractor[String,Coding[Site]] =
    Extractor(Coding[Site](_))

  protected val QueryId =
    Extractor(Query.Id(_))

  protected val PreparedQueryId =
    Extractor(PreparedQuery.Id(_))

  protected val PatId =
    Extractor(Id[Patient](_))

  protected val dateTime =
    Extractor.option[LocalDateTime]

  protected val TAN =
    Extractor(Id[TransferTAN](_))


  implicit def enumExtractor[E <: Enumeration](
    implicit w: Witness.Aux[E]
  ): Extractor[String,E#Value] =
    s => w.value.values.find(_.toString == s)

  protected val ReportStatusSet =
    Extractor.option(
      Extractor.csvSet[Submission.Report.Status.Value]
    )

  val prefix =
    if (pref startsWith "/")
      pref
    else
      s"/$pref"


  protected val controller: UseCaseController[UseCase]

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
      controller.Action { req =>
        jsonSchemas(format.getOrElse(APPLICATION_JSON)).get(version.getOrElse("draft-12").toLowerCase) match {
          case Some(sch) =>
            Ok(Json.prettyPrint(sch)).as(APPLICATION_JSON)

          case None =>
            NotFound(
              Json.toJson(Outcome(s"Invalid JSON schema version, expected one of {${jsonSchemas.keys.mkString(",")}}"))
            )
        }
      }

    case POST(p"/etl/patient-record:validate") => controller.validate

    case POST(p"/etl/patient-record") => controller.processUpload

    case DELETE(p"/etl/patient/${PatId(patId)}") => controller.deleteData(patId)


    // ------------------------------------------------------------------------
    // Data Validation Result Routes:
    // ------------------------------------------------------------------------

    case GET(p"/validation/infos") =>
      controller.validationInfos

    case GET(p"/validation/report/${PatId(patId)}") =>
      controller.validationReport(patId)

    case GET(p"/validation/patient-record/${PatId(patId)}") =>
      controller.validationPatientRecord(patId)


    // ------------------------------------------------------------------------
    // Peer-to-peer Routes:
    // ------------------------------------------------------------------------

    case GET(p"/peer2peer/status-info") => controller.statusInfo

    case POST(p"/peer2peer/query") => controller.peerToPeerQuery

//    case POST(p"/peer2peer/patient-record") => controller.patientRecordRequest
    case GET(p"/peer2peer/patient-record"
             ? q"origin=${Origin(site)}"
             & q"querier=${querier(q)}"
             & q"patient=${PatId(id)}"
             & q_o"snapshot=${long(snp)}") => controller.patientRecord(site,q,id,snp)



    // ------------------------------------------------------------------------
    // MVH Endpoints  
    // ------------------------------------------------------------------------

    case GET(p"/peer2peer/mvh/submission-reports"
      ? q_o"created-after=${dateTime(start)}"
      & q_o"created-before=${dateTime(end)}"
      & q_o"status=${ReportStatusSet(status)}") => controller.mvhSubmissionReports(start,end,status)

    case POST(p"/peer2peer/mvh/submission-reports/${TAN(id)}:submitted") => controller.confirmReportSubmitted(id)


    // ------------------------------------------------------------------------
    // Query Routes:
    // ------------------------------------------------------------------------

    case POST(p"/queries") => controller.submit

    case GET(p"/queries"?q"id=${QueryId(id)}") => controller.get(id)

    case GET(p"/queries/${QueryId(id)}/filters/$part") => controller.defaultFilter(id,part)

    case GET(p"/queries/${QueryId(id)}") => controller.get(id)

    case DELETE(p"/queries"?q"id=${QueryId(id)}") => controller.delete(id)

    case DELETE(p"/queries/${QueryId(id)}") => controller.delete(id)

    case PUT(p"/queries/${QueryId(id)}") => controller.update(id)

    case GET(p"/queries/${QueryId(id)}/demographics") => controller.demographics(id)

    case GET(p"/queries/${QueryId(id)}/patient-matches") => controller.patientMatches(id)

    case GET(p"/queries/${QueryId(id)}/patient-record"?q"id=${PatId(patId)}") => controller.patientRecord(id,patId)

    case GET(p"/queries/${QueryId(id)}/patient-record/${PatId(patId)}") => controller.patientRecord(id,patId)

    case GET(p"/queries") => controller.queries


    // ------------------------------------------------------------------------
    // Prepared Query Routes:
    // ------------------------------------------------------------------------

    case POST(p"/prepared-queries") => controller.createPreparedQuery

    case GET(p"/prepared-queries/${PreparedQueryId(id)}") => controller.getPreparedQuery(id)

    case GET(p"/prepared-queries"?q"id=${PreparedQueryId(id)}") => controller.getPreparedQuery(id)

    case GET(p"/prepared-queries") => controller.getPreparedQueries

    case PATCH(p"/prepared-queries/${PreparedQueryId(id)}") => controller.updatePreparedQuery(id)

    case PATCH(p"/prepared-queries"?q"id=${PreparedQueryId(id)}") => controller.updatePreparedQuery(id)

    case DELETE(p"/prepared-queries/${PreparedQueryId(id)}") => controller.deletePreparedQuery(id)

    case DELETE(p"/prepared-queries"?q"id=${PreparedQueryId(id)}") => controller.deletePreparedQuery(id)

  }


  val additionalRoutes: Routes =
    PartialFunction.empty


  override lazy val routes: Routes =
    baseRoutes orElse additionalRoutes

}
