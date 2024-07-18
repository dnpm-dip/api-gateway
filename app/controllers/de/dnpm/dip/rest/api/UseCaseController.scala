package de.dnpm.dip.rest.api


import javax.inject.Inject
import scala.util.{
  Left,
  Right
}
import scala.concurrent.{
  Future,
  ExecutionContext
}
import play.api.mvc.{
  Action,
  AnyContent,
  BaseController,
  RequestHeader
}
import play.api.libs.json.{
  Json,
  Format,
  OFormat,
  Reads,
  Writes,
  OWrites
}
import cats.data.Ior
import cats.Monad
import de.dnpm.dip.util.Completer
import de.dnpm.dip.service.Orchestrator
import de.dnpm.dip.service.Data.{
  Save,
  Saved,
  SavedWithIssues,
  Delete,
  Deleted,
  FatalIssuesDetected,
  UnacceptableIssuesDetected,
  GenericError
}
import de.dnpm.dip.service.validation.ValidationService
import ValidationService.{
  Validate,
  DataAcceptableWithIssues
}
import de.dnpm.dip.service.query.{
  PatientFilter,
  PatientMatch,
  PeerToPeerQuery,
  PatientRecordRequest,
  Query,
  Querier,
  QueryService,
  PreparedQuery,
  ResultSet,
  UseCaseConfig
}
import de.dnpm.dip.coding.{
  Coding,
  CodeSystem
}
import de.dnpm.dip.model.{
  Id,
  Gender,
  VitalStatus,
  Patient,
  Site,
  Snapshot
}
import de.dnpm.dip.auth.api.{
  AuthenticatedRequest,
  Authorization,
  AuthorizationOps,
  UserPermissions,
  UserAuthenticationService
}
import de.dnpm.dip.rest.util._
import de.dnpm.dip.rest.util.sapphyre.Hyper


final case class QueryPatch[Criteria]
(
  name: Option[String],
  mode: Option[Coding[Query.Mode.Value]],
  sites: Option[Set[Coding[Site]]],
  criteria: Option[Criteria]
)
object QueryPatch
{
  implicit def format[Criteria: Reads]: Reads[QueryPatch[Criteria]] =
    Json.reads[QueryPatch[Criteria]]
}


abstract class UseCaseController[UseCase <: UseCaseConfig]
(
  implicit
  ec: ExecutionContext,
  formatPatRec: OFormat[UseCase#PatientRecord],
  formatCriteria: OFormat[UseCase#Criteria],
  writesFilters: Writes[UseCase#Filter],
  writesSummary: OWrites[UseCase#Results#SummaryType],
)
extends BaseController
with JsonOps
with AuthorizationOps[UserPermissions]
{

  this: 
    QueryAuthorizations[UserPermissions]
    with ValidationAuthorizations[UserPermissions]
    with UseCaseHypermedia[UseCase] =>


  import scala.language.implicitConversions
  import scala.util.chaining._
  import cats.data.NonEmptyList
  import cats.syntax.either._
  import Completer.syntax._


  type PatientRecord = UseCase#PatientRecord
  type Criteria      = UseCase#Criteria
  type Results       = UseCase#Results
  type Filter        = UseCase#Filter


  protected implicit val completer: Completer[PatientRecord]

  protected implicit val authService: UserAuthenticationService =
    UserAuthenticationService.getInstance.get

  protected val validationService: ValidationService[Future,Monad[Future],PatientRecord]

  protected val queryService: QueryService[Future,Monad[Future],UseCase]

  protected final lazy val orchestrator: Orchestrator[Future,PatientRecord] =
    new Orchestrator(validationService,queryService)


  // For implicit conversion of Filter DTO to predicate function
  import queryService.filterToPredicate


  implicit def querierFromRequest[T](
    implicit req: AuthenticatedRequest[UserPermissions,T]
  ): Querier =
    Querier(req.agent.id)

  implicit def querierFromUserPermissions(
    implicit user: UserPermissions
  ): Querier =
    Querier(user.id)


  override def OwnershipOf(id: Query.Id): Authorization[UserPermissions] =
    Authorization.async {
      implicit user =>
        queryService.get(id).map(_.exists(_.querier.value == user.id))
    }

  override def OwnershipOfPreparedQuery(id: PreparedQuery.Id): Authorization[UserPermissions] =
    Authorization.async {
      implicit user =>
        (queryService ? id).map(_.exists(_.querier.value == user.id))
    }



  def sites: Action[AnyContent] =
    Action.async {
      queryService.sites
        .map(Json.toJson(_))
        .map(Ok(_))
    }


  // --------------------------------------------------------------------------  
  // Data Operations
  // --------------------------------------------------------------------------  

  protected val patientRecordParser =
    JsonBody[PatientRecord]

  def validate =
    Action.async(patientRecordParser){ 
      req =>
        (validationService ! Validate(req.body)).map {
          case Right(DataAcceptableWithIssues(_,report)) => Ok(Json.toJson(report))
          case Right(_)                                  => Ok("Valid")
          case Left(UnacceptableIssuesDetected(report))  => UnprocessableEntity(Json.toJson(report))
          case Left(FatalIssuesDetected(report))         => BadRequest(Json.toJson(report))
          case Left(GenericError(err))                   => InternalServerError(err)
          case _                                         => InternalServerError("Unexpected data upload outcome")
        }
    }


  def upload =
    Action.async(patientRecordParser){ 
      req =>
        (orchestrator ! Save(req.body))
          .map {
            case Right(Saved(snp))                        => Ok(Json.toJson(snp))
            case Right(SavedWithIssues(snp,report))       => Created(Json.toJson(report))
            case Left(UnacceptableIssuesDetected(report)) => UnprocessableEntity(Json.toJson(report))
            case Left(FatalIssuesDetected(report))        => BadRequest(Json.toJson(report))
            case Left(GenericError(err))                  => InternalServerError(err)
            case _                                        => InternalServerError("Unexpected data upload outcome")
          }
    }


  def deleteData(patId: Id[Patient]): Action[AnyContent] =
    Action.async { 
      (orchestrator ! Delete(patId))
        .map {
          case Right(Deleted(id))      => Ok(s"Deleted data of Patient ${id.value}")
          case Left(GenericError(err)) => InternalServerError(err)
          case _                       => InternalServerError("Unexpected data deletion outcome")
        }
         
    }

  // --------------------------------------------------------------------------  
  // Validation Result Operations
  // --------------------------------------------------------------------------  

  def validationInfos =
    AuthorizedAction(ReadValidationInfos).async {
      req =>
        (validationService ? ValidationService.Filter.empty)
          .map(_.map(Hyper(_)).toSeq)  
          .map(Collection(_))
          .map(Json.toJson(_))
          .map(Ok(_))
    }  

  def validationReport(id: Id[Patient]) =
    AuthorizedAction(ReadValidationReport).async {
      req =>
        (validationService.dataQualityReport(id))
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,s"Invalid Patient ID ${id.value}"))
    }  

  def validationPatientRecord(id: Id[Patient]) =
    AuthorizedAction(ReadInvalidPatientRecord).async {
      req =>
        (validationService.patientRecord(id))
          .map(JsonResult(_,s"Invalid Patient ID ${id.value}"))
    }  


  // --------------------------------------------------------------------------  
  // PreparedQuery Operations
  // --------------------------------------------------------------------------  

  def createPreparedQuery =
    AuthorizedAction(JsonBody[PreparedQuery.Create[Criteria]])(SubmitQuery)
      .async { 
        implicit req =>
          (queryService ! req.body)
            .map(_.map(Hyper(_)))
            .map(JsonResult(_,InternalServerError(_)))
      }

  def getPreparedQuery(id: PreparedQuery.Id): Action[AnyContent] =
    AuthenticatedAction.async { 
      implicit req =>
        (queryService ? id)
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,s"Invalid PreparedQuery ID ${id.value}"))
    }

  def getPreparedQueries: Action[AnyContent] =
    AuthenticatedAction.async { 
      implicit req =>
        (queryService ? PreparedQuery.Filter(Some(Querier(req.agent.id))))
          .map(_.map(Hyper(_)))
          .map(Collection(_))
          .map(Hyper(_))
          .map(Json.toJson(_))
          .map(Ok(_))
    }

  def updatePreparedQuery(id: PreparedQuery.Id) =
    AuthorizedAction(JsonBody[QueryPatch[Criteria]])(OwnershipOfPreparedQuery(id))
      .async { 
        implicit req =>
          (queryService ! PreparedQuery.Update(id,req.body.name,req.body.criteria))
            .map(_.map(Hyper(_)))
            .map(JsonResult(_,InternalServerError(_)))
      }
      
  def deletePreparedQuery(id: PreparedQuery.Id): Action[AnyContent] =
    AuthorizedAction(OwnershipOfPreparedQuery(id))
      .async { 
        implicit req =>
        (queryService ! PreparedQuery.Delete(id))
         .map(
           JsonResult(_,_ => BadRequest(s"Invalid PreparedQuery ID ${id.value}"))
         )
      }


  // --------------------------------------------------------------------------  
  // Query Operations
  // --------------------------------------------------------------------------  

  def submit =
    AuthorizedAction(JsonBody[Query.Submit[Criteria]])(SubmitQuery).async { 
      implicit req =>
        (queryService ! req.body)
          .map {
            case Right(query) =>
              Ok(Json.toJson(Hyper(query)))
           
            case Left(err) =>
              err match {
                case Query.ConnectionErrors(errs) => BadGateway(Json.toJson(Outcome(errs)))
                case Query.NoResults              => NotFound(Json.toJson(Outcome("Query returned no results")))
                case Query.GenericError(msg)      => InternalServerError(Json.toJson(Outcome(msg)))
                case Query.InvalidId              => InternalServerError(Json.toJson(Outcome("Unexpected Query error")))
              }
          }
    }


  def get(id: Query.Id): Action[AnyContent] =
    AuthorizedAction(OwnershipOf(id)).async {
      implicit req =>
        queryService.get(id)
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
    }


  def update(id: Query.Id) =
    AuthorizedAction(JsonBody[QueryPatch[Criteria]])(OwnershipOf(id)).async{ 
      implicit req =>
        (queryService ! Query.Update(id,req.body.mode,req.body.sites,req.body.criteria))
          .map {
            case Right(query) =>
              Ok(Json.toJson(Hyper(query)))
            
            case Left(err) =>
              err match {
                case Query.ConnectionErrors(errs) => BadGateway(Json.toJson(Outcome(errs)))
                case Query.NoResults              => NotFound(Json.toJson(Outcome("Query returned no results")))
                case Query.GenericError(msg)      => InternalServerError(Json.toJson(Outcome(msg)))
                case Query.InvalidId              => NotFound(Json.toJson(Outcome(s"Invalid Query ID ${id.value}")))
              }
          }
    }


  def delete(id: Query.Id): Action[AnyContent] =
    AuthorizedAction(OwnershipOf(id)).async {
      implicit req =>
        (queryService ! Query.Delete(id))
          .map(_.toOption)
          .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
    }

  private val Genders =
    Extractor.AsCodings[Gender.Value]

  private val VitalStatuses =
    Extractor.AsCodings[VitalStatus.Value]

  private val Sites =
    Extractor.AsCodingsOf[Site]

  
  def PatientFilterFrom(req: RequestHeader): PatientFilter = {

    PatientFilter(
      req.queryString.get("gender").collect {
        case Genders(gender) if gender.nonEmpty => gender
      },
      req.queryString.get("age[min]").flatMap(_.headOption).map(_.toInt),
      req.queryString.get("age[max]").flatMap(_.headOption).map(_.toInt),
      req.queryString.get("vitalStatus").collect {
        case VitalStatuses(vs) if vs.nonEmpty => vs
      },
      req.queryString.get("site").collect {
        case Sites(sites) if sites.nonEmpty => sites
      }
    )

  }


  protected def FilterFrom(req: RequestHeader): Filter 

  def summary(
    id: Query.Id
  ): Action[AnyContent] =
    AuthorizedAction(ReadQueryResult AND OwnershipOf(id)).async {
      implicit req =>
        queryService
          .resultSet(id)
          .map(_.map(_.summary(FilterFrom(req))))
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
    }


  def demographics(
    id: Query.Id
  ): Action[AnyContent] =
    AuthorizedAction(ReadQueryResult AND OwnershipOf(id)).async {
      implicit req =>
        queryService
          .resultSet(id)
          .map(_.map(_.demographics(FilterFrom(req))))
          .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
    }


  def patientMatches(
    offset: Option[Int],
    limit: Option[Int]
  )(
    implicit id: Query.Id
  ): Action[AnyContent] =
    AuthorizedAction(ReadQueryResult AND OwnershipOf(id)).async {
      implicit req =>
        queryService
          .resultSet(id)
          .map(
            _.map(
              _.patientMatches(FilterFrom(req))
               .asInstanceOf[Seq[PatientMatch[Criteria]]]
            )
          )
          .map(
            _.map(
              Collection(_,offset,limit)
                .map(Hyper(_))
                .pipe(Hyper(_))
            )
          )
          .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
      
    }


  def patientRecord(
    implicit
    id: Query.Id,
    patId: Id[Patient]
  ): Action[AnyContent] =
    AuthorizedAction(ReadPatientRecord AND OwnershipOf(id)).async {
      implicit req =>
        queryService.patientRecord(id,patId)
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,s"Invalid Query ID ${id.value} or Patient ID ${patId.value}"))
    }


  def queries: Action[AnyContent] =
    AuthenticatedAction.async {
      implicit req =>
        queryService.queries
          .map(_.map(Hyper(_)))
          .map(Collection(_))
          .map(Hyper(_))
          .map(Json.toJson(_))
          .map(Ok(_))
    }


  // --------------------------------------------------------------------------  
  // Peer-to-Peer Operations
  // --------------------------------------------------------------------------  

  def peerToPeerQuery =
    JsonAction[PeerToPeerQuery[Criteria,PatientRecord]].async { 
      req =>
        (queryService ! req.body)
          .map {
            case Right(resultSet) => Ok(Json.toJson(resultSet))
            case Left(err)        => InternalServerError(err)
          }
    }


  def patientRecordRequest =
    JsonAction[PatientRecordRequest[PatientRecord]].async { 
      req =>
        (queryService ! req.body)
          .map(JsonResult(_))
    }


}
