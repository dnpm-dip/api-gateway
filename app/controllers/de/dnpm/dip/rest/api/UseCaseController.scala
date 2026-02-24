package de.dnpm.dip.rest.api


import java.time.LocalDateTime
import scala.util.{
  Left,
  Right,
  Success
}
import scala.concurrent.{
  Future,
  ExecutionContext
}
import scala.concurrent.duration._
import play.api.mvc.{
  Action,
  AnyContent,
  BaseController,
  RequestHeader
}
import play.api.libs.json.{
  Json,
  JsValue,
  OFormat,
  Reads
}
import play.api.cache.{
  Cached,
  AsyncCacheApi => Cache
}
import cats.Monad
import cats.data.NonEmptyList
import cats.syntax.either._
import de.dnpm.dip.util.Completer
import de.dnpm.dip.service.{
  DataUpload,
  Orchestrator,
  UsageScope
}
import Orchestrator.{
  Process,
  Saved,
  SavedWithIssues,
  Delete,
  Deleted
}
import de.dnpm.dip.service.validation.{
  ValidationReport,
  ValidationService
}
import ValidationService.{
  DataAcceptableWithIssues,
  FatalIssuesDetected,
  UnacceptableIssuesDetected,
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
  UseCaseConfig
}
import de.dnpm.dip.service.mvh.{
  MVHService,
  Report,
  Submission,
  TransferTAN
}
import de.dnpm.dip.coding.Coding
import de.dnpm.dip.model.{
  Id,
  Gender,
  VitalStatus,
  Patient,
  OpenEndPeriod,
  Site
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



abstract class UseCaseController[UseCase <: UseCaseConfig](
  implicit
  ec: ExecutionContext,
  formatPatRec: OFormat[UseCase#PatientRecord],
  formatCriteria: OFormat[UseCase#Criteria]
)
extends BaseController
with JsonOps
with AuthorizationOps[UserPermissions]
{

  this: 
    QueryAuthorizations[UserPermissions]
    with ValidationAuthorizations[UserPermissions]
    with UseCaseHypermedia[UseCase] =>


  import scala.util.chaining._


  type PatientRecord = UseCase#PatientRecord
  type Criteria      = UseCase#Criteria
  type Results       = UseCase#Results
  type Filter        = UseCase#Filter


  protected val cache: Cache
  protected val cached: Cached
  protected val cachingDuration: Duration = 15 minutes
  protected val CACHE_CONTROL_SETTINGS = "no-store"


  protected implicit val completer: Completer[PatientRecord]

  protected implicit val patientSetter: (PatientRecord,Patient) => PatientRecord

  protected implicit val authService: UserAuthenticationService =
    UserAuthenticationService.getInstance.get

  protected val validationService: ValidationService[Future,Monad[Future],PatientRecord]

  protected val queryService: QueryService[Future,Monad[Future],UseCase]

  protected val mvhService: MVHService[Future,Monad[Future],PatientRecord]

  protected final lazy val orchestrator: Orchestrator[Future,PatientRecord] =
    new Orchestrator(
      validationService,
      mvhService,
      queryService
    )


  implicit def querierFromRequest[T](
    implicit req: AuthenticatedRequest[UserPermissions,T]
  ): Querier =
    Querier(req.agent.id)

  implicit def querierFromUserPermissions(
    implicit user: UserPermissions
  ): Querier =
    Querier(user.id)


  override def OwnershipOf(id: Query.Id): Authorization[UserPermissions] =
    Authorization.async[UserPermissions](
      implicit user =>
        queryService.get(id).map {
          case Some(query) =>
            if (query.querier.value == user.id) None  // no problem
            else Some(Forbidden)

          case None =>
            Some(
              NotFound(Json.toJson(Outcome(s"Invalid Query ID, your query session probably timed out")))
            )
        }
    )

  override def OwnershipOfPreparedQuery(id: PreparedQuery.Id): Authorization[UserPermissions] =
    Authorization.async[UserPermissions](
      implicit user =>
        (queryService ? id).map(_.exists(_.querier.value == user.id))
    )


  // -------------------------------------------------------------------------- 
  // Cache Management 
  // -------------------------------------------------------------------------- 

  // Keep track of which Result keys are cached for a given Query,
  // in order to be able to remove them when the Query is updated or deleted (see below)

  protected def addCachedResult(
    query: Query.Id,
    key: String
  ) =    
    for {
      optKeys <- cache.get[Set[String]](query.toString)
      keys  = optKeys.getOrElse(Set.empty[String])
    }{
      cache.set(
        query.toString,
        keys + key,
        cachingDuration * 1.5  // ensure this info is cached longer than results
      )
    }  

  protected def clearCachedResults(query: Query.Id) =
    for { optKeys <- cache.get[Set[String]](query.toString) }{      
      for { keys <- optKeys }{
        for { key <- keys }{
          cache.remove(key)
        }
      }
    }

  // --------------------------------------------------------------------------  


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
    JsonBody[DataUpload[PatientRecord]]


  def validate =
    Action.async(patientRecordParser){ 
      req =>
        validationService.validate(req.body).map {
          case Right(DataAcceptableWithIssues(_,report)) => Ok(Json.toJson(report))
          case Right(_)                                  => Ok
          case Left(UnacceptableIssuesDetected(report))  => UnprocessableEntity(Json.toJson(report))
          case Left(FatalIssuesDetected(report))         => BadRequest(Json.toJson(report))
          case Left(ValidationService.GenericError(err)) => InternalServerError(err)
        }
    }


  def processUpload =
    Action.async(patientRecordParser){ 
      req =>
        (orchestrator ! Process(req.body)).collect {
          case Right(Saved)                   => Ok
          case Right(SavedWithIssues(report)) => Created(Json.toJson(report))
          case Left(errs) =>
            val results =
              errs.foldLeft(Map.empty[Int,Either[NonEmptyList[String],ValidationReport]]){
                (acc,err) => err match {
                  case Left(FatalIssuesDetected(report))         => acc + (BAD_REQUEST -> report.asRight)
              
                  case Left(UnacceptableIssuesDetected(report))  => acc + (UNPROCESSABLE_ENTITY -> report.asRight)
              
                  case Left(ValidationService.GenericError(msg)) => //acc + (INTERNAL_SERVER_ERROR -> NonEmptyList.of(msg).asLeft)
                    acc.updatedWith(INTERNAL_SERVER_ERROR){
                      case Some(Left(msgs)) => Some((msg :: msgs).asLeft)
                      case _ => Some(NonEmptyList.of(msg).asLeft)
                    }
              
                  case Right(Left(MVHService.InvalidTAN(msg)))   => acc + (BAD_REQUEST -> NonEmptyList.of(msg).asLeft)

                  case Right(Left(MVHService.InvalidSubmissionType(msg))) => acc + (BAD_REQUEST -> NonEmptyList.of(msg).asLeft)
              
                  case Right(Left(MVHService.GenericError(msg))) =>
                    acc.updatedWith(INTERNAL_SERVER_ERROR){
                      case Some(Left(msgs)) => Some((msg :: msgs).asLeft)
                      case _ => Some(NonEmptyList.of(msg).asLeft)
                    }
              
                  case Right(Right(QueryService.GenericError(msg))) =>
                    acc.updatedWith(INTERNAL_SERVER_ERROR){
                      case Some(Left(msgs)) => Some((msg :: msgs).asLeft)
                      case _ => Some(NonEmptyList.of(msg).asLeft)
                    }
                }
              }

            val code =
              results.keySet match {
                case codes if codes(BAD_REQUEST) => BAD_REQUEST
                case codes if codes(UNPROCESSABLE_ENTITY) => UNPROCESSABLE_ENTITY
                case _ => INTERNAL_SERVER_ERROR  
              }

            Status(code)(
              results(code)
                .leftMap(Outcome(_))
                .fold(Json.toJson(_),Json.toJson(_))
            )
            
/*            
            val result =
              errs.foldLeft(Option.empty[(Int,Either[NonEmptyList[String],ValidationReport])]){
                (acc,err) => err match {
                  case Left(FatalIssuesDetected(report))         => Some(BAD_REQUEST -> report.asRight)
              
                  case Left(UnacceptableIssuesDetected(report))  => Some(UNPROCESSABLE_ENTITY -> report.asRight)
              
                  case Left(ValidationService.GenericError(msg)) => Some(INTERNAL_SERVER_ERROR -> NonEmptyList.of(msg).asLeft)
              
                  case Right(Left(MVHService.InvalidTAN(msg)))   => Some(BAD_REQUEST -> NonEmptyList.of(msg).asLeft)
              
                  case Right(Left(MVHService.GenericError(msg))) =>
                    acc.collect {
                      case (sc,Left(msgs)) if sc == INTERNAL_SERVER_ERROR => sc -> (msg :: msgs).asLeft
                    }
                    .orElse(Some(INTERNAL_SERVER_ERROR -> NonEmptyList.of(msg).asLeft))
              
                  case Right(Right(QueryService.GenericError(msg))) =>
                    acc.collect {
                      case (sc,Left(msgs)) if sc == INTERNAL_SERVER_ERROR => sc -> (msg :: msgs).asLeft
                    }
                    .orElse(Some(INTERNAL_SERVER_ERROR -> NonEmptyList.of(msg).asLeft))
                }
              }

            result match {
              case Some(sc -> result) => Status(sc)(result.leftMap(Outcome(_)).fold(Json.toJson(_),Json.toJson(_)))
              case None => Ok
            }
*/            
        }
    }


  def deleteData(
    patId: Id[Patient],
    scopes: Option[Set[UsageScope.Value]]
  ): Action[AnyContent] =
    Action.async { 
      (orchestrator ! Delete(patId,scopes))
        .collect {
          case Right(Deleted(id)) => Ok
          case Left(errs) => InternalServerError(
            Json.toJson(
              Outcome(
                errs.collect {
                  case Left(ValidationService.GenericError(msg))    => msg
                  case Right(Left(MVHService.GenericError(msg)))    => msg
                  case Right(Right(QueryService.GenericError(msg))) => msg
                  case Right(_)                                     => "Unexpected data deletion outcome"
                  case Left(_)                                      => "Unexpected data deletion outcome"
                }
                .map(Outcome.Issue.Error(_))
              )
            )
          )
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
    AuthorizedAction(JsonBody[QueryPatch[Criteria]])(OwnershipOf(id)).async { 
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
          .andThen { 
            case Success(res) if res.header.status == OK => clearCachedResults(id)
          }
    }


  def delete(id: Query.Id): Action[AnyContent] =
    AuthorizedAction(OwnershipOf(id)).async {
      implicit req =>
        (queryService ! Query.Delete(id))
          .map(_.toOption)
          .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
          .andThen { 
            case Success(res) if res.header.status == OK => clearCachedResults(id)
          }
    }


  import CodingExtractors._

  private val Genders =
    Extractor.csvSet[Coding[Gender.Value]]

  private val VitalStatuses =
    Extractor.csvSet[Coding[VitalStatus.Value]]

  private val Sites =
    Extractor.csvSet[Coding[Site]]

  
  def PatientFilterFrom(req: RequestHeader): PatientFilter = 
    PatientFilter(
      req.queryString.get("gender").flatMap(_.headOption) collect {
        case Genders(gender) if gender.nonEmpty => gender
      },
      req.queryString.get("age[min]").flatMap(_.headOption).map(_.toInt),
      req.queryString.get("age[max]").flatMap(_.headOption).map(_.toInt),
      req.queryString.get("vitalStatus").flatMap(_.headOption) collect {
        case VitalStatuses(vs) if vs.nonEmpty => vs
      },
      req.queryString.get("site").flatMap(_.headOption) collect {
        case Sites(sites) if sites.nonEmpty => sites
      }
    )


  protected def FilterFrom(req: RequestHeader): Filter 


  protected val filterComponent: PartialFunction[String,Filter => JsValue]


  def defaultFilter(id: Query.Id, part: String) =
    AuthorizedAction(OwnershipOf(id)).async {
      implicit req =>
        queryService
          .resultSet(id)
          .map(
            _.map(_.defaultFilter)
             .map(filterComponent(part))
          ) 
          .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
    }


  def demographics(id: Query.Id) =
    cached.status(_.uri,OK,cachingDuration){
      AuthorizedAction(ReadQueryResult,OwnershipOf(id)).async {
        implicit req =>
          queryService
            .resultSet(id)
            .map(_.map(_.demographics(FilterFrom(req))))
            .map(
              JsonResult(_,s"Invalid Query ID ${id.value}")
                .withHeaders(CACHE_CONTROL -> CACHE_CONTROL_SETTINGS)
            )
            .andThen { 
              case Success(res) if res.header.status == OK => addCachedResult(id,req.uri) 
            }
      }
    }


  def patientMatches(
    implicit id: Query.Id
  ): Action[AnyContent] =
    AuthorizedAction(ReadQueryResult,OwnershipOf(id)).async {
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
              Collection(_)
                .paginated
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
    AuthorizedAction(ReadPatientRecord,OwnershipOf(id)).async {
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

  def statusInfo =
    Action.async { 
      orchestrator.statusInfo
        .map(Json.toJson(_))
        .map(Ok(_))
    }


  def peerToPeerQuery =
    JsonAction[PeerToPeerQuery[Criteria,PatientRecord]].async { 
      req =>
        (queryService ! req.body)
          .map {
            case Right(resultSet) => Ok(Json.toJson(resultSet))
            case Left(err)        => InternalServerError(err)
          }
    }


  def patientRecord(
    origin: Coding[Site],
    querier: Querier,
    patId: Id[Patient],
    snapshot: Option[Long]
  ) =
    Action.async {
      (queryService ! PatientRecordRequest[PatientRecord](origin,querier,patId,snapshot))
        .map(JsonResult(_))
    }

  def patientRecordRequest =
    JsonAction[PatientRecordRequest[PatientRecord]].async { 
      req =>
        (queryService ! req.body)
          .map(JsonResult(_))
    }


  def mvhSubmissionReports(
    start: Option[LocalDateTime],
    end: Option[LocalDateTime],
    status: Option[Set[Submission.Report.Status.Value]],
    `type`: Option[Set[Submission.Type.Value]]
  ) = 
    Action.async {
      (mvhService ? Submission.Report.Filter(start.map(OpenEndPeriod(_,end)),status,`type`))
        .map(rs => Collection(rs.toSeq))
        .map(Json.toJson(_))
        .map(Ok(_))
    }


  def mvhSubmissionReport(id: Id[TransferTAN]) = 
    Action.async {
      (mvhService ? id)
        .map(JsonResult(_))
    }


  def mvhSubmissions(
    tans: Option[Set[Id[TransferTAN]]],
    start: Option[LocalDateTime],
    end: Option[LocalDateTime]
  ) = 
    Action.async {
      (mvhService ? Submission.Filter(tans,start.map(OpenEndPeriod(_,end))))
        .map(rs => Collection(rs.toSeq))
        .map(Json.toJson(_))
        .map(Ok(_))
    }

  
  def mvhReport(criteria: Report.Criteria): Action[AnyContent]

 
  def confirmReportSubmitted(id: Id[TransferTAN]) =
    Action.async {
      (mvhService ! MVHService.ConfirmSubmitted(id))
        .collect { 
          case Right(_)                           => Ok
          case Left(MVHService.GenericError(err)) => InternalServerError(err)
        }
    }

}
