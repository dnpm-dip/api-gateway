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
  ResultSet
}
import de.dnpm.dip.coding.Coding 
import de.dnpm.dip.rd.model.{ 
  HPO, Orphanet
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



class RDQueryController @Inject()(
  override val controllerComponents: ControllerComponents,
)(
  implicit ec: ExecutionContext,
)
extends QueryController[RDConfig]
with QueryAuthorizations[UserPermissions]
{

  import scala.util.chaining._


  override lazy val prefix = "rd"


  override val service: RDQueryService =
    RDQueryService.getInstance.get


  override implicit val authService: UserAuthenticationService =
    UserAuthenticationService.getInstance.get


  override val SubmitQuery: Authorization[UserPermissions] =
    Authorization(
      _.permissions
       .exists { case RDPermissions(p) => p == RDPermissions.SubmitQuery }
    )

  override val ReadQueryResult: Authorization[UserPermissions] =
    Authorization(
      _.permissions
       .exists { case RDPermissions(p) => p == RDPermissions.ReadResultSummary }
    )

  override val ReadPatientRecord: Authorization[UserPermissions] =
    Authorization(
      _.permissions
       .exists { case RDPermissions(p) => p == RDPermissions.ReadPatientRecord }
    )

  private val HPOTerms =
    Extractor.AsCodingsOf[HPO]

  private val Categories =
    Extractor.AsCodingsOf[Orphanet]


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
