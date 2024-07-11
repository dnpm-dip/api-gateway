package de.dnpm.dip.rest.api



import javax.inject.Inject
import scala.concurrent.{
  Future,
  ExecutionContext
}
import play.api.mvc.{
  Action,
  AnyContent,
  RequestHeader,
  ControllerComponents
}
import play.api.libs.json.{
  Json,
  Format,
  Reads,
  Writes
}
import de.dnpm.dip.rest.util._
import de.dnpm.dip.util.Completer
import de.dnpm.dip.service.query.{
  PatientFilter,
  Query,
  Querier,
  ResultSet
}
import de.dnpm.dip.coding.{
  Coding,
  CodeSystem
}
import de.dnpm.dip.coding.hgnc.HGNC
import de.dnpm.dip.coding.icd.ICD10GM 
import de.dnpm.dip.mtb.model.{
  MTBPatientRecord,
  Completers
}
import de.dnpm.dip.mtb.model.v1
import v1.mappings._
import de.dnpm.dip.util.mapping.syntax._
import de.dnpm.dip.service.query.Query
import de.dnpm.dip.mtb.validation.api.{
  MTBValidationPermissions,
  MTBValidationService
}
import de.dnpm.dip.mtb.query.api.{
  MTBConfig,
  MTBFilters,
  DiagnosisFilter,
  KaplanMeier,
  MTBQueryPermissions,
  MTBQueryService,
  MTBResultSet
}
import de.dnpm.dip.mtb.query.api.KaplanMeier.{
  SurvivalType,
  Grouping
}
import de.dnpm.dip.auth.api.{
  Authorization,
  UserPermissions,
  UserAuthenticationService
}

class MTBController @Inject()(
  override val controllerComponents: ControllerComponents,
)(
  implicit ec: ExecutionContext,
)
extends UseCaseController[MTBConfig]
with ValidationAuthorizations[UserPermissions]
with QueryAuthorizations[UserPermissions]
{

  import scala.util.chaining._
  import de.dnpm.dip.rest.util.AuthorizationConversions._


  override lazy val prefix = "mtb"

  override implicit val completer: Completer[MTBPatientRecord] =
    Completers.mtbPatientRecordCompleter


  override val validationService: MTBValidationService =
    MTBValidationService.getInstance.get

  override val queryService: MTBQueryService =
    MTBQueryService.getInstance.get

//  import scala.language.implicitConversions

  // For implicit conversion of Filter DTO to predicate function
  import queryService.filterToPredicate


  override val SubmitQuery =
    MTBQueryPermissions.SubmitQuery

  override val ReadQueryResult =
    MTBQueryPermissions.ReadResultSummary

  override val ReadPatientRecord =
    MTBQueryPermissions.ReadPatientRecord

  override val ReadValidationInfos =
    MTBValidationPermissions.ReadValidationInfos

  override val ReadValidationReport =
    MTBValidationPermissions.ReadValidationReport

  override val ReadInvalidPatientRecord =
    MTBValidationPermissions.ReadInvalidPatientRecord


  private val DiagnosisCodes =
    Extractor.AsCodingsOf[ICD10GM]



  override def FilterFrom(
    req: RequestHeader,
  ): MTBFilters = 
    MTBFilters(
      PatientFilterFrom(req),
      DiagnosisFilter(
        req.queryString.get("diagnosis[code]") collect {
          case DiagnosisCodes(icd10s) if icd10s.nonEmpty => icd10s
        }
      )
    )



  implicit val hgnc: CodeSystem[HGNC] =
    HGNC.GeneSet
      .getInstance[cats.Id]
      .get
      .latest

  override val patientRecordParser =
    parse.using(
      _.contentType match {
        case Some("application/json+v2") =>
          JsonBody[MTBPatientRecord]

        case _ =>
          JsonBody[v1.MTBPatientRecord].map(_.mapTo[MTBPatientRecord])
      }
    )


  def kaplanMeierConfig: Action[AnyContent] =
    Action {
      queryService.survivalConfig
        .pipe(Json.toJson(_))
        .pipe(Ok(_))
    }


  def therapyResponses(id: Query.Id): Action[AnyContent] =
    AuthorizedAction(OwnershipOf(id)).async { 
      implicit req =>

        queryService.resultSet(id)
          .map(
            _.map(
              _.medicationStats(FilterFrom(req))
            )
          )
          .map(JsonResult(_,s"Invalid Query ID ${id.value}"))

    }


  def survivalStatistics(
    id: Query.Id,
    typ: Option[SurvivalType.Value],
    grp: Option[Grouping.Value]
  ): Action[AnyContent] =
    AuthorizedAction(OwnershipOf(id)).async { 
      implicit req =>
        queryService.resultSet(id)
          .map(
            _.map(_.survivalStatistics(typ,grp))
          )
          .map(JsonResult(_,s"Invalid Query ID ${id.value}"))

    }


}
