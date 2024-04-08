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
import cats.Monad
import de.dnpm.dip.service.query.{
  Data,
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


abstract class QueryController[UseCase <: UseCaseConfig]
(
  implicit
  ec: ExecutionContext,
  jsCriteria: OFormat[UseCase#Criteria],
  jsFilters: Writes[UseCase#Filter],
  jsPatRec: OFormat[UseCase#PatientRecord],
  jsSummary: OWrites[UseCase#Results#SummaryType],
)
extends BaseController
   with JsonOps
   with QueryHypermedia[UseCase]
   with AuthorizationOps[UserPermissions]
{

  this: QueryAuthorizations[UserPermissions] =>


  import scala.util.chaining._
  import cats.data.NonEmptyList
  import cats.syntax.either._
  import Data.{Outcome,Save,Saved,Delete,Deleted}


  type PatientRecord = UseCase#PatientRecord
  type Criteria      = UseCase#Criteria
  type Filter        = UseCase#Filter
  type Results       = UseCase#Results


  val service: QueryService[Future,Monad[Future],UseCase,String]

  implicit val authService: UserAuthenticationService


  import scala.language.implicitConversions

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
        service.get(id).map(_.exists(_.querier.value == user.id))
    }

  override def OwnershipOfPreparedQuery(id: PreparedQuery.Id): Authorization[UserPermissions] =
    Authorization.async {
      implicit user =>
        (service ? id).map(_.exists(_.querier.value == user.id))
    }




  def sites: Action[AnyContent] =
    Action.async {
      service.sites
        .map(Json.toJson(_))
        .map(Ok(_))
    }


  // --------------------------------------------------------------------------  
  // Data Operations
  // --------------------------------------------------------------------------  

  def validate =
    JsonAction[PatientRecord].async {
      req =>
        // If this point is reached, the request payload could be successfully deserialized and is thus valid
        // TODO: add subsequent semantic validation by data handling service
        Future.successful(Ok("Valid"))
    }


  protected val patientRecordParser =
    JsonBody[PatientRecord]

  def upload =
    Action.async(patientRecordParser){ 
      req =>
        (service ! Save(req.body))
          .map {
            case Right(Saved(snp)) => Created(Json.toJson(snp))
            case Right(_)          => InternalServerError("Unexpected data upload outcome")
            case Left(err)         => InternalServerError(err)
          }
    }


  def deletePatient(patId: Id[Patient]): Action[AnyContent] =
    Action.async { 
      (service ! Delete(patId))
        .map {
          case Right(Deleted(id)) => Ok(s"Deleted data of Patient ${id.value}")
          case Right(_)           => InternalServerError("Unexpected data deletion outcome")
          case Left(err)          => InternalServerError(err)
        }
         
    }


  // --------------------------------------------------------------------------  
  // PreparedQuery Operations
  // --------------------------------------------------------------------------  

  def createPreparedQuery =
    AuthorizedAction(JsonBody[PreparedQuery.Create[Criteria]])(SubmitQueryAuthorization)
      .async { 
        implicit req =>
          (service ! req.body)
            .map(_.map(Hyper(_)))
            .map(JsonResult(_,InternalServerError(_)))
      }

  def getPreparedQuery(id: PreparedQuery.Id): Action[AnyContent] =
    AuthenticatedAction.async { 
      implicit req =>
      (service ? id)
        .map(_.map(Hyper(_)))
        .map(JsonResult(_,s"Invalid PreparedQuery ID ${id.value}"))
    }

  def getPreparedQueries: Action[AnyContent] =
    AuthenticatedAction.async { 
      implicit req =>
        (service ? PreparedQuery.Query(Some(Querier(req.agent.id))))
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
          (service ! PreparedQuery.Update(id,req.body.name,req.body.criteria))
            .map(_.map(Hyper(_)))
            .map(JsonResult(_,InternalServerError(_)))
      }
      
  def deletePreparedQuery(id: PreparedQuery.Id): Action[AnyContent] =
    AuthorizedAction(OwnershipOfPreparedQuery(id))
      .async { 
        implicit req =>
        (service ! PreparedQuery.Delete(id))
         .map(
           JsonResult(_,_ => BadRequest(s"Invalid PreparedQuery ID ${id.value}"))
         )
      }



  // --------------------------------------------------------------------------  
  // Query Operations
  // --------------------------------------------------------------------------  

  def submit =
    AuthorizedAction(JsonBody[Query.Submit[Criteria]])(SubmitQueryAuthorization).async { 
      implicit req =>
        (service ! req.body)
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,InternalServerError(_)))
    }


  def get(id: Query.Id): Action[AnyContent] =
    AuthorizedAction(OwnershipOf(id)).async {
      implicit req =>
        service.get(id)
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
    }


  def update(id: Query.Id) =
    AuthorizedAction(JsonBody[QueryPatch[Criteria]])(OwnershipOf(id)).async{ 
      implicit req =>
        (service ! Query.Update(id,req.body.mode,req.body.sites,req.body.criteria))
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,InternalServerError(_)))
    }


  def delete(id: Query.Id): Action[AnyContent] =
    AuthorizedAction(OwnershipOf(id)).async {
      implicit req =>
        (service ! Query.Delete(id))
          .map(_.toOption)
          .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
    }


  private val Genders =
    Extractor.AsCodings[Gender.Value]

  private val VitalStatuses =
    Extractor.AsCodings[VitalStatus.Value]

  private val Sites =
    Extractor.AsCodingsOf[Site]

  
  def PatientFilterFrom(
    req: RequestHeader
  ): PatientFilter = {

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


  protected def FilterFrom(
    req: RequestHeader,
    patientFilter: PatientFilter
  ): Filter 


  def summary(
    id: Query.Id
  ): Action[AnyContent] =
    AuthorizedAction(ReadQueryResultAuthorization AND OwnershipOf(id)).async {
      implicit req =>
        service.summary(
          id,
          FilterFrom(
            req,
            PatientFilterFrom(req)
          )
        )
        .map(_.map(Hyper(_)))
        .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
    }


  def patientMatches(
    offset: Option[Int],
    limit: Option[Int]
  )(
    implicit id: Query.Id
  ): Action[AnyContent] =
    AuthorizedAction(ReadQueryResultAuthorization AND OwnershipOf(id)).async {
      implicit req =>
        service.patientMatches(
          id,
          FilterFrom(
            req,
            PatientFilterFrom(req)
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
    AuthorizedAction(ReadPatientRecordAuthorization AND OwnershipOf(id)).async {
      implicit req =>
        service.patientRecord(id,patId)
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,s"Invalid Query ID ${id.value} or Patient ID ${patId.value}"))
    }


  def queries: Action[AnyContent] =
    AuthenticatedAction.async {
      implicit req =>
        service.queries
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
        (service ! req.body)
          .map {
            case Right(resultSet) => Ok(Json.toJson(resultSet))
            case Left(err)        => InternalServerError(err)
          }
    }


  def patientRecordRequest =
    JsonAction[PatientRecordRequest[PatientRecord]].async { 
      req =>
        (service ! req.body)
          .map(JsonResult(_))
    }


}
