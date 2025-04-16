package de.dnpm.dip.rest.api



import javax.inject.Inject
import scala.concurrent.ExecutionContext
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
import de.dnpm.dip.service.query.Query
import de.dnpm.dip.coding.Coding 
import de.dnpm.dip.rd.model.{
  HPO,
  RDDiagnosis,
  RDPatientRecord,
  Completers
}
import de.dnpm.dip.rd.validation.api.{
  RDValidationPermissions,
  RDValidationService
}
import de.dnpm.dip.rd.query.api.{
  RDConfig,
  RDFilters,
  HPOFilter,
  DiagnosisFilter,
  RDQueryPermissions,
  RDQueryService
}
import de.dnpm.dip.rd.mvh.api.RDMVHService
import de.dnpm.dip.auth.api.UserPermissions



class RDController @Inject()(
  override val cache: Cache,
  override val cached: Cached,
  override val controllerComponents: ControllerComponents,
)(
  implicit ec: ExecutionContext,
)
extends UseCaseController[RDConfig]
with ValidationAuthorizations[UserPermissions]
with QueryAuthorizations[UserPermissions]
with RDHypermedia
{

  import scala.util.chaining._
  import Json.toJson
  import de.dnpm.dip.rest.util.AuthorizationConversions._


  override lazy val prefix = "rd"

  override implicit val completer: Completer[RDPatientRecord] =
    Completers.rdPatientRecordCompleter


  override val validationService: RDValidationService =
    RDValidationService.getInstance.get

  override val queryService: RDQueryService =
    RDQueryService.getInstance.get

  override val mvhService: RDMVHService =
    RDMVHService.getInstance.get


  override val SubmitQuery =
    RDQueryPermissions.SubmitQuery

  override val ReadQueryResult =
    RDQueryPermissions.ReadResultSummary

  override val ReadPatientRecord =
    RDQueryPermissions.ReadPatientRecord

  override val ReadValidationInfos =
    RDValidationPermissions.ReadValidationInfos

  override val ReadValidationReport =
    RDValidationPermissions.ReadValidationReport

  override val ReadInvalidPatientRecord =
    RDValidationPermissions.ReadInvalidPatientRecord


  import CodingExtractors._


  private val HPOCodings =
    Extractor.csvSet[Coding[HPO]]

  private val Category =
    Extractor.csvSet[Coding[RDDiagnosis.Systems]]


  override def FilterFrom(
    req: RequestHeader,
  ): RDFilters = 
    RDFilters(
      PatientFilterFrom(req),
      HPOFilter(
        req.queryString.get("hpo[value]").flatMap(_.headOption) collect {
          case HPOCodings(codings) if codings.nonEmpty => codings
        }
      ),
      DiagnosisFilter(
        req.queryString.get("diagnosis[category]").flatMap(_.headOption) collect {
          case Category(codings) if codings.nonEmpty => codings
        }
      )
    )
 

  import RDFilters._  // For Json Writes of MTBFilter components

  override val filterComponent = {
    case "patient"   => (_.patient.pipe(toJson(_)))
    case "diagnosis" => (_.diagnosis.pipe(toJson(_)))
    case "hpo"       => (_.hpo.pipe(toJson(_)))
  }

 
  //TODO: Caching
  def diagnostics(id: Query.Id): Action[AnyContent] =
    AuthorizedAction(OwnershipOf(id)).async { 
      implicit req =>

        queryService.resultSet(id)
          .map(_.map(_.diagnostics(FilterFrom(req))))
          .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
    }

}
