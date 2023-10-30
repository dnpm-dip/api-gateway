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
  BaseController
}
import play.api.libs.json.{
  Json,
  Format,
  Reads,
  Writes
}
import cats.Monad
import de.dnpm.dip.service.query.{
  Data,
  PatientMatch,
  PeerToPeerQuery,
  PatientRecordRequest,
  Query,
  Querier,
  QueryService,
  UseCaseConfig
}
import de.dnpm.dip.coding.Coding
import de.dnpm.dip.model.{
  Id,
  Patient,
  Snapshot
}
import de.dnpm.dip.rest.util._
import de.dnpm.dip.rest.util.sapphyre.Hyper


abstract class QueryController[UseCase <: UseCaseConfig]
(
  implicit
  ec: ExecutionContext,
  jsCriteria: Format[UseCase#Criteria],
  jsFilters: Format[UseCase#Filters],
  jsPatRec: Format[UseCase#PatientRecord],
  jsSummary: Writes[UseCase#Results#Summary],
)
extends BaseController
   with JsonOps
   with QueryHypermedia[UseCase]
{

  type PatientRecord = UseCase#PatientRecord
  type Criteria      = UseCase#Criteria
  type Filters       = UseCase#Filters
  type Results       = UseCase#Results

  import cats.data.NonEmptyList
  import cats.syntax.either._
  import Data.{Outcome,Save,Saved,Delete,Deleted}

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
  // Query Operations
  // --------------------------------------------------------------------------  

  def submit(mode: Coding[Query.Mode.Value]) =
    JsonAction[Criteria].async { 
      req =>
        (service ! Query.Submit(mode,req.body))
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
    JsonActionOpt[Criteria].async { 
      req =>
        (service ! Query.Update(id,mode,req.body))
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,InternalServerError(_)))
    }


  def update(id: Query.Id) =
    JsonAction[Query.Update[Criteria]].async{ 
      req =>
        (service ! req.body)
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,InternalServerError(_)))

    }


  def applyFilters(id: Query.Id) =
    JsonAction[Query.ApplyFilters[Filters]].async{ 
      req =>
        (service ! req.body)
          .map(_.map(Hyper(_)))
          .map(JsonResult(_,InternalServerError(_)))

    }

  def delete(id: Query.Id): Action[AnyContent] =
    Action.async { 
      (service ! Query.Delete(id))
        .map(_.toOption)
        .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
    }


  def summary(
    implicit id: Query.Id
  ): Action[AnyContent] =
    Action.async { 
      service.summary(id)
        .map(_.map(Hyper(_)))
        .map(JsonResult(_))
    }

  
  def patientMatches(
    offset: Option[Int],
    length: Option[Int],
  )(
    implicit id: Query.Id
  ): Action[AnyContent] =
    Action.async {
      service.patientMatches(id,offset,length)
        .map(_.map(_.map(Hyper(_))))
        .map(_.map(Collection(_)))
        .map(JsonResult(_))
      
    }


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
