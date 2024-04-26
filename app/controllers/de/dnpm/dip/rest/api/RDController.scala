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
  ResultSet
}
import de.dnpm.dip.coding.Coding 
import de.dnpm.dip.rd.model.{
  HPO,
  Orphanet,
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
  RDPermissions,
  RDQueryService,
  RDResultSet
}
import de.dnpm.dip.auth.api.{
  Authorization,
  UserPermissions,
  UserAuthenticationService
}



class RDController @Inject()(
  override val controllerComponents: ControllerComponents,
)(
  implicit ec: ExecutionContext,
)
extends UseCaseController[RDConfig]
with ValidationAuthorizations[UserPermissions]
with QueryAuthorizations[UserPermissions]
{

  import de.dnpm.dip.rest.util.AuthorizationConversions._
  import RDPermissions._


  override lazy val prefix = "rd"

  override implicit val completer: Completer[RDPatientRecord] =
    Completers.rdPatientRecordCompleter


  override val validationService: RDValidationService =
    RDValidationService.getInstance.get

  override val queryService: RDQueryService =
    RDQueryService.getInstance.get

  override implicit val authService: UserAuthenticationService =
    UserAuthenticationService.getInstance.get


  override val SubmitQueryAuthorization: Authorization[UserPermissions] =
    RDPermissions.SubmitQuery

  override val ReadQueryResultAuthorization: Authorization[UserPermissions] =
    RDPermissions.ReadResultSummary

  override val ReadPatientRecordAuthorization: Authorization[UserPermissions] =
    RDPermissions.ReadPatientRecord

  override val ViewValidationInfosAuthorization: Authorization[UserPermissions] =
    RDValidationPermissions.ViewValidationInfos

  override val ViewValidationReportAuthorization: Authorization[UserPermissions] =
    RDValidationPermissions.ViewValidationReport

  override val ViewInvalidPatientRecordAuthorization: Authorization[UserPermissions] =
    RDValidationPermissions.ViewInvalidPatientRecord


  private val HPOTerms =
    Extractor.AsCodingsOf[HPO]

  private val Categories =
    Extractor.Codings[RDDiagnosis.Category]


  override def FilterFrom(
    req: RequestHeader,
    patientFilter: PatientFilter
  ): RDFilters = 
    RDFilters(
      patientFilter,
      HPOFilter(
        req.queryString.get("hpo[value]") collect {
          case HPOTerms(hpos) if hpos.nonEmpty => hpos
        }
      ),
      DiagnosisFilter(
        req.queryString.get("diagnosis[category]") collect {
          case Categories(orphas) if orphas.nonEmpty => orphas
        }
      )
    )
  
}
