package de.dnpm.dip.rest.api



import javax.inject.Inject
import scala.concurrent.{
  Future,
  ExecutionContext
}
import scala.util.{
  Success,
  Try
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
import play.api.cache.{
  Cached,
  AsyncCacheApi => Cache
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
  Code,
  Coding,
  CodeSystem
}
import de.dnpm.dip.coding.hgnc.HGNC
import de.dnpm.dip.coding.icd.ICD10GM 
import de.dnpm.dip.model.Medications
import de.dnpm.dip.mtb.model.{
  MTBPatientRecord,
  Completers
}
import de.dnpm.dip.mtb.model.v1
import v1.mappings._
import de.dnpm.dip.util.mapping.syntax._
import de.dnpm.dip.service.DataUpload
import de.dnpm.dip.service.query.Query
import de.dnpm.dip.mtb.validation.api.{
  MTBValidationPermissions,
  MTBValidationService
}
import de.dnpm.dip.mtb.query.api.{
  MTBConfig,
  MTBFilters,
  DiagnosisFilter,
  RecommendationFilter,
  TherapyFilter,
  KaplanMeier,
  MTBQueryPermissions,
  MTBQueryService,
  MTBResultSet
}
import de.dnpm.dip.mtb.mvh.api.MTBMVHService
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
  override val cache: Cache,
  override val cached: Cached,
  override val controllerComponents: ControllerComponents,
)(
  implicit ec: ExecutionContext,
)
extends UseCaseController[MTBConfig]
with ValidationAuthorizations[UserPermissions]
with QueryAuthorizations[UserPermissions]
with MTBHypermedia
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

  override val mvhService: MTBMVHService =
    MTBMVHService.getInstance.get


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


  import Extractor._
  import CodingExtractors._

  private val DiagnosisCodings =
    Extractor.set[Coding[ICD10GM]]

  private val MedicationCodings =
    Extractor.set(
      Extractor.csvSet[Coding[Medications]]
    )


  override def FilterFrom(
    req: RequestHeader,
  ): MTBFilters = 
    MTBFilters(
      PatientFilterFrom(req),
      DiagnosisFilter(
        req.queryString.get("diagnosis[code]") collect {
          case DiagnosisCodings(codings) if codings.nonEmpty => codings
        }
      ),
      RecommendationFilter(
        req.queryString.get("recommendation[medication]") collect { 
          case MedicationCodings(codings) if codings.nonEmpty => codings
        }
      ),
      TherapyFilter(
        req.queryString.get("therapy[medication]") collect { 
          case MedicationCodings(codings) if codings.nonEmpty => codings
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
          JsonBody[DataUpload[MTBPatientRecord]]

        case _ =>
          JsonBody[DataUpload[v1.MTBPatientRecord]].map {
            case DataUpload(record,meta) =>
              DataUpload(
                record.mapTo[MTBPatientRecord],
                meta
              )
          }
      }
    )


  def kaplanMeierConfig: Action[AnyContent] =
    Action {
      queryService.survivalConfig
        .pipe(Json.toJson(_))
        .pipe(Ok(_))
    }

  def tumorDiagnostics(id: Query.Id) =
    cached.status(_.uri,OK,cachingDuration){
      AuthorizedAction(OwnershipOf(id)).async { 
        implicit req =>
  
          queryService.resultSet(id)
            .map(_.map(_.tumorDiagnostics(FilterFrom(req))))
            .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
            .andThen {
              case Success(res) if res.header.status == OK => addCachedResult(id,req.uri)
            }
      }
    }


  def medication(id: Query.Id) =
    cached.status(_.uri,OK,cachingDuration){
      AuthorizedAction(OwnershipOf(id)).async { 
        implicit req =>
      
          queryService.resultSet(id)
            .map(_.map(_.medication(FilterFrom(req))))
            .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
            .andThen {
              case Success(res) if res.header.status == OK => addCachedResult(id,req.uri)
            }
      }
    }


  def therapyResponses(id: Query.Id) =
    cached.status(_.uri,OK,cachingDuration){
      AuthorizedAction(OwnershipOf(id)).async { 
        implicit req =>
  
          queryService.resultSet(id)
            .map(
              _.map(
                _.therapyResponses(FilterFrom(req))
                 .pipe(Collection(_))
              )
            )
            .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
            .andThen {
              case Success(res) if res.header.status == OK => addCachedResult(id,req.uri)
            }
      }
    }


  def survivalStatistics(
    id: Query.Id,
    typ: Option[SurvivalType.Value],
    grp: Option[Grouping.Value]
  ) =
    cached.status(_.uri,OK,cachingDuration){
      AuthorizedAction(OwnershipOf(id)).async { 
        implicit req =>
          queryService.resultSet(id)
            .map(
              _.map(_.survivalStatistics(typ,grp))
            )
            .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
            .andThen {
              case Success(res) if res.header.status == OK => addCachedResult(id,req.uri)
            }
      }
    }

}
