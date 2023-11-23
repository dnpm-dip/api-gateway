package de.dnpm.dip.rest.api


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
import de.dnpm.dip.rest.util._
import de.dnpm.dip.rest.util.sapphyre.Hyper



final case class PartialQuery[Criteria]
(
  criteria: Criteria
)
object PartialQuery
{
  implicit def format[Criteria: Reads]: Reads[PartialQuery[Criteria]] =
    Json.reads[PartialQuery[Criteria]]
}


final case class PatchQuery[Criteria]
(
  name: Option[String],
  mode: Option[Coding[Query.Mode.Value]],
  criteria: Option[Criteria]
)
object PatchQuery
{
  implicit def format[Criteria: Reads]: Reads[PatchQuery[Criteria]] =
    Json.reads[PatchQuery[Criteria]]
}



abstract class QueryController[UseCase <: UseCaseConfig]
(
  implicit
  ec: ExecutionContext,
  jsCriteria: OFormat[UseCase#Criteria],
  jsFilters: Writes[UseCase#Filter],
  jsPatRec: OFormat[UseCase#PatientRecord],
  jsSummary: OWrites[UseCase#Results#Summary],
)
extends BaseController
   with JsonOps
   with QueryHypermedia[UseCase]
{

  import scala.util.chaining._
  import cats.data.NonEmptyList
  import cats.syntax.either._
  import Data.{Outcome,Save,Saved,Delete,Deleted}


  type PatientRecord = UseCase#PatientRecord
  type Criteria      = UseCase#Criteria
  type Filter        = UseCase#Filter
  type Results       = UseCase#Results


  val service: QueryService[Future,Monad[Future],UseCase,String]


  override lazy val prefix = "rd"


  //TODO: extract from authenticated request
  implicit val querier: Querier =
    Querier("Dummy-Querier-ID")


  // --------------------------------------------------------------------------  
  // Data Operations
  // --------------------------------------------------------------------------  

  def upload =
    JsonAction[PatientRecord].async { 
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
    JsonAction[PreparedQuery.Create[Criteria]].async { 
      req =>
        (service ! req.body)
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,InternalServerError(_)))

    }

  def getPreparedQuery(id: PreparedQuery.Id): Action[AnyContent] =
    Action.async { 
      (service ? id)
        .map(_.map(Hyper(_)))
        .map(JsonResult(_,s"Invalid PreparedQuery ID ${id.value}"))
    }

  def getPreparedQueries: Action[AnyContent] =
    Action.async { 
      (service ? PreparedQuery.Query(Some(querier)))
        .map(_.map(Hyper(_)))
        .map(Collection(_))
        .map(Hyper(_))
        .map(Json.toJson(_))
        .map(Ok(_))
    }

  def updatePreparedQuery(id: PreparedQuery.Id) =
    JsonAction[PatchQuery[Criteria]].async { 
      req =>
        (service ! PreparedQuery.Update(id,req.body.name,req.body.criteria))
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,InternalServerError(_)))
    }

  def deletePreparedQuery(id: PreparedQuery.Id): Action[AnyContent] =
    Action.async { 
      (service ! PreparedQuery.Delete(id))
       .map(
         JsonResult(_,_ => BadRequest(s"Invalid PreparedQuery ID ${id.value}"))
       )
    }



  // --------------------------------------------------------------------------  
  // Query Operations
  // --------------------------------------------------------------------------  

  def submit(mode: Coding[Query.Mode.Value]) =
    JsonAction[PartialQuery[Criteria]].async { 
      req =>
        (service ! Query.Submit(mode,req.body.criteria))
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,InternalServerError(_)))
    }


  def submit =
    JsonAction[Query.Submit[Criteria]].async { 
      req =>
        (service ! req.body)
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,InternalServerError(_)))

    }


  def get(id: Query.Id): Action[AnyContent] =
    Action.async { 
      service.get(id)
        .map(_.map(Hyper(_)))
        .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
    }


  def update(
    id: Query.Id,
    mode: Option[Coding[Query.Mode.Value]]
  ) =
    JsonActionOpt[PartialQuery[Criteria]].async { 
      req =>
        (service ! Query.Update(id,mode,req.body.map(_.criteria)))
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,InternalServerError(_)))
    }


  def update(id: Query.Id) =
    JsonAction[PatchQuery[Criteria]].async{ 
      req =>
        (service ! Query.Update(id,req.body.mode,req.body.criteria))
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,InternalServerError(_)))

    }


  def delete(id: Query.Id): Action[AnyContent] =
    Action.async { 
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


  def FilterFrom(
    req: RequestHeader,
    patientFilter: PatientFilter
  ): Filter 


  def summary(
    implicit id: Query.Id
  ): Action[AnyContent] =
    Action.async {
      req =>
      service.summary(
        id,
        FilterFrom(
          req,
          PatientFilterFrom(req)
        )
      )
      .map(_.map(Hyper(_)))
      .map(JsonResult(_))
    }


  def patientMatches(
    offset: Option[Int],
    limit: Option[Int]
  )(
    implicit id: Query.Id
  ): Action[AnyContent] =
    Action.async {
      req =>
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
      .map(JsonResult(_))
      
    }

/*
  def patientMatches(
    offset: Option[Int],
    limit: Option[Int],
    patientFilter: PatientFilter
  )(
    implicit id: Query.Id
  ): Action[AnyContent] =
    Action.async {
      req =>
      service.patientMatches(
        id,
        FilterFrom(req,patientFilter)
      )
      .map(
        _.map(
          Collection(_,offset,limit)
            .map(Hyper(_))
            .pipe(Hyper(_))
        )
      )
      .map(JsonResult(_))
      
    }
*/

  def patientRecord(
    implicit
    id: Query.Id,
    patId: Id[Patient]
  ): Action[AnyContent] =
    Action.async {
      service.patientRecord(id,patId)
        .map(_.map(Hyper(_)))
        .map(JsonResult(_,s"Invalid Query ID ${id.value} or Patient ID ${patId.value}"))
      
    }


  def queries: Action[AnyContent] =
    Action.async { 
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
