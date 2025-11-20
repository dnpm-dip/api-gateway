package de.dnpm.dip.rest.api



import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Success
import play.api.mvc.{
  Action,
  AnyContent,
  RequestHeader,
  ControllerComponents
}
import play.api.libs.json.Json
import play.api.cache.{
  Cached,
  AsyncCacheApi => Cache
}
import de.dnpm.dip.rest.util._
import de.dnpm.dip.util.Completer
import de.dnpm.dip.coding.{
  Coding,
  CodeSystem
}
import de.dnpm.dip.coding.hgnc.HGNC
import de.dnpm.dip.coding.icd.ICD10GM 
import de.dnpm.dip.model.{
  Medications,
  Patient
}
import de.dnpm.dip.mtb.model.{
  MTBPatientRecord,
  Completers
}
import de.dnpm.dip.service.query.Query
import de.dnpm.dip.service.mvh.Report
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
  MTBQueryPermissions,
  MTBQueryService
}
import de.dnpm.dip.mtb.mvh.api.MTBMVHService
import de.dnpm.dip.mtb.query.api.KaplanMeier.{
  SurvivalType,
  Grouping
}
import de.dnpm.dip.auth.api.{
  UserPermissions,
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
  import Json.toJson
  import de.dnpm.dip.rest.util.AuthorizationConversions._


  override lazy val prefix = "mtb"

  override implicit val completer: Completer[MTBPatientRecord] =
    Completers.mtbPatientRecordCompleter

  override implicit val patientSetter: (MTBPatientRecord,Patient) => MTBPatientRecord =
    (record,patient) => record.copy(patient = patient)

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


  import CodingExtractors._

  private val DiagnosisCodings =
    Extractor.csvSet[Coding[ICD10GM]]

  private val MedicationCodings =
    Extractor.csvSet(Extractor.csvSet[Coding[Medications]]("\\+"))
    

  override def FilterFrom(
    req: RequestHeader,
  ): MTBFilters = 
    MTBFilters(
      PatientFilterFrom(req),
      DiagnosisFilter(
        req.queryString.get("diagnosis[code]").flatMap(_.headOption) collect {
          case DiagnosisCodings(codings) if codings.nonEmpty => codings
        }
      ),
      RecommendationFilter(
        req.queryString.get("therapyRecommendation[medication]").flatMap(_.headOption) collect { 
          case MedicationCodings(codings) if codings.nonEmpty => codings.tap(println)
        }
      ),
      TherapyFilter(
        req.queryString.get("therapy[medication]").flatMap(_.headOption) collect { 
          case MedicationCodings(codings) if codings.nonEmpty => codings
        }
      )
    )


  import MTBFilters._  // For Json Writes of MTBFilter components

  override val filterComponent = {
    case "patient"                => (_.patient.pipe(toJson(_)))
    case "diagnosis"              => (_.diagnosis.pipe(toJson(_)))
    case "therapy-recommendation" => (_.therapyRecommendation.pipe(toJson(_)))
    case "therapy"                => (_.therapyRecommendation.pipe(toJson(_)))
  }


  implicit val hgnc: CodeSystem[HGNC] =
    HGNC.GeneSet
      .getInstance[cats.Id]
      .get
      .latest


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
            .map(
              JsonResult(_,s"Invalid Query ID ${id.value}")
                .withHeaders(CACHE_CONTROL -> CACHE_CONTROL_SETTINGS)
            )
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
            .map(
              JsonResult(_,s"Invalid Query ID ${id.value}")
                .withHeaders (CACHE_CONTROL -> CACHE_CONTROL_SETTINGS)
            )
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
                 .pipe(Collection(_).paginated)
              )
            )
            .map(
              JsonResult(_,s"Invalid Query ID ${id.value}")
                .withHeaders(CACHE_CONTROL -> CACHE_CONTROL_SETTINGS)
            )
            .andThen {
              case Success(res) if res.header.status == OK => addCachedResult(id,req.uri)
            }
      }
    }


  def therapyResponsesByVariant(id: Query.Id) =
    cached.status(_.uri,OK,cachingDuration){
      AuthorizedAction(OwnershipOf(id)).async { 
        implicit req =>
  
          queryService.resultSet(id)
            .map(
              _.map(
                _.therapyResponsesBySupportingVariant(FilterFrom(req))
                 .pipe(Collection(_).paginated)
              )
            )
            .map(
              JsonResult(_,s"Invalid Query ID ${id.value}")
                .withHeaders(CACHE_CONTROL -> CACHE_CONTROL_SETTINGS)
            )
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
            .map(
              JsonResult(_,s"Invalid Query ID ${id.value}")
                .withHeaders(CACHE_CONTROL -> CACHE_CONTROL_SETTINGS)
            )
            .andThen {
              case Success(res) if res.header.status == OK => addCachedResult(id,req.uri)
            }
      }
    }

  override def mvhReport(criteria: Report.Criteria) =
    Action.async { 
      mvhService.report(criteria)
        .map(toJson(_))
        .map(Ok(_))
    }

}
