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
import de.dnpm.dip.service.query.{
  PatientFilter,
  Query,
  Querier,
  ResultSet
}
import de.dnpm.dip.coding.Coding 
import de.dnpm.dip.coding.icd.ICD10GM 
import de.dnpm.dip.mtb.query.api.{
  MTBConfig,
  MTBFilters,
  DiagnosisFilter,
  MTBPermissions,
  MTBQueryService,
  MTBResultSet
}
import de.dnpm.dip.auth.api.{
  Authorization,
  UserPermissions,
  UserAuthenticationService
}


class MTBQueryController @Inject()(
  override val controllerComponents: ControllerComponents,
)(
  implicit ec: ExecutionContext,
)
extends QueryController[MTBConfig]
with QueryAuthorizations[UserPermissions]
{

  override lazy val prefix = "mtb"

  override val service: MTBQueryService =
    MTBQueryService.getInstance.get

  override implicit val authService: UserAuthenticationService =
    UserAuthenticationService.getInstance.get


  override val SubmitQuery: Authorization[UserPermissions] =
    Authorization(
      _.permissions
       .exists { case MTBPermissions(p) => p == MTBPermissions.SubmitQuery }
    )

  override val ReadQueryResult: Authorization[UserPermissions] =
    Authorization(
      _.permissions
       .exists { case MTBPermissions(p) => p == MTBPermissions.ReadResultSummary }
    )

  override val ReadPatientRecord: Authorization[UserPermissions] =
    Authorization(
      _.permissions
       .exists { case MTBPermissions(p) => p == MTBPermissions.ReadPatientRecord }
    )


  private val DiagnosisCodes =
    Extractor.AsCodingsOf[ICD10GM]

  override def FilterFrom(
    req: RequestHeader,
    patientFilter: PatientFilter
  ): MTBFilters = 
    MTBFilters(
      patientFilter,
      DiagnosisFilter(
        req.queryString.get("diagnosis[code]") collect {
          case DiagnosisCodes(icd10s) if icd10s.nonEmpty => icd10s
        }
      )
    )
  
}
