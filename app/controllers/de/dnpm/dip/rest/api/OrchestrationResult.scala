package de.dnpm.dip.rest.api


import cats.Semigroup
import cats.data.{
  EitherNel,
  NonEmptyList
}
import cats.syntax.either._
import de.dnpm.dip.service.Orchestrator
import de.dnpm.dip.service.validation.{
  ValidationReport,
  ValidationService
}
import ValidationService.{
  FatalIssuesDetected,
  UnacceptableIssuesDetected
}
import de.dnpm.dip.service.mvh.MVHService
import MVHService.{
  InvalidTAN,
  InvalidSubmissionType
}
import de.dnpm.dip.service.query.QueryService
import play.api.mvc.Result
import play.api.mvc.ControllerHelpers.{
  BAD_REQUEST,
  INTERNAL_SERVER_ERROR,
  UNPROCESSABLE_ENTITY,
  Status
}
import play.api.libs.json.Json
import de.dnpm.dip.rest.util.Outcome


object OrchestrationResult
{

  private final case class Error
  (
    status: Int,
    value: EitherNel[String,ValidationReport]
  )

  import shapeless.Poly1

  private object toError extends Poly1
  { 
    implicit val validationServiceError: Case.Aux[ValidationService.Error,Error] = at {
      case FatalIssuesDetected(report)         => Error(BAD_REQUEST,report.rightNel)
      case UnacceptableIssuesDetected(report)  => Error(UNPROCESSABLE_ENTITY,report.rightNel)
      case ValidationService.GenericError(msg) => Error(INTERNAL_SERVER_ERROR,msg.leftNel)
    }

    implicit val mvhServiceError: Case.Aux[MVHService.Error,Error] = at { 
      case InvalidTAN(msg)              => Error(BAD_REQUEST,msg.leftNel)
      case InvalidSubmissionType(msg)   => Error(BAD_REQUEST,msg.leftNel)
      case MVHService.GenericError(msg) => Error(INTERNAL_SERVER_ERROR,msg.leftNel)
    }

    implicit val queryServiceError: Case.Aux[QueryService.DataError,Error] = at { 
      case QueryService.GenericError(msg) => Error(INTERNAL_SERVER_ERROR,msg.leftNel)
    }
  } 


  private def priority: Int => Int =
    Map(
      BAD_REQUEST           -> 3,
      UNPROCESSABLE_ENTITY  -> 2,
      INTERNAL_SERVER_ERROR -> 1
    )
    .orElse { case _ => 0 }
 

  private def maxByPriority(sc1: Int, sc2: Int) =
    Set(sc1,sc2).maxBy(priority)


  private implicit val semigroup: Semigroup[Error] =
    Semigroup.instance  {
      case (err1 @ Error(status1,Right(_)), err2 @ Error(status2,Right(_))) =>
        if (priority(status1) > priority(status2)) err1
        else err2
      case (Error(status1,value @ Right(_)),Error(_,Left(_)))       => Error(status1,value) 
      case (Error(_,Left(_)),Error(status2,value @ Right(_)))       => Error(status2,value)
      case (Error(status1,Left(msgs1)),Error(status2,Left(msgs2)))  => Error(maxByPriority(status1,status2),(msgs1 ::: msgs2).asLeft)
    }


  def apply(errors: NonEmptyList[Orchestrator.Error]): Result =
    errors.map(_ fold toError).reduce match {
      case Error(status,Right(validationReport)) => Status(status)(Json.toJson(validationReport))
      case Error(status,Left(messages)) => Status(status)(Json.toJson(Outcome(messages)))
    }
}


/*
object OrchestrationResult
{

  private sealed trait Error
  {
    def status: Int
  }

  private final case class ValidationError
  (
    status: Int,
    report: ValidationReport
  )
  extends Error

  private final case class MessageError
  (
    status: Int,
    message: String
  )
  extends Error

  import shapeless.Poly1

  private object toError extends Poly1
  { 
    implicit val validationServiceError: Case.Aux[ValidationService.Error,Error] = at {
      case FatalIssuesDetected(report)         => ValidationError(BAD_REQUEST,report)
      case UnacceptableIssuesDetected(report)  => ValidationError(UNPROCESSABLE_ENTITY,report)
      case ValidationService.GenericError(msg) => MessageError(INTERNAL_SERVER_ERROR,msg)
    }

    implicit val mvhServiceError: Case.Aux[MVHService.Error,Error] = at { 
      case InvalidTAN(msg)              => MessageError(BAD_REQUEST,msg)
      case InvalidSubmissionType(msg)   => MessageError(BAD_REQUEST,msg)
      case MVHService.GenericError(msg) => MessageError(INTERNAL_SERVER_ERROR,msg)
    }

    implicit val queryServiceError: Case.Aux[QueryService.DataError,Error] = at { 
      case QueryService.GenericError(msg) => MessageError(INTERNAL_SERVER_ERROR,msg)
    }
  } 


  private def priority: Int => Int =
    Map(
      BAD_REQUEST           -> 3,
      UNPROCESSABLE_ENTITY  -> 2,
      INTERNAL_SERVER_ERROR -> 1
    )
    .orElse { case _ => 0 }
 
  private def maxByPriority(sc1: Int, sc2: Int) =
    Set(sc1,sc2).maxBy(priority)


  def apply(errors: NonEmptyList[Orchestrator.Error]): Result =
    errors.map(_ fold toError)
      .foldLeft(Option.empty[(Int,EitherNel[String,ValidationReport])]){
        case (acc @ Some(status -> xor),error) =>
          error match { 
            case err: ValidationError => 
              if (priority(err.status) > priority(status)) Some(err.status -> err.report.asRight.toEitherNel)
              else acc
            
            case err: MessageError => 
              Some(maxByPriority(status,err.status) -> xor.leftMap(err.message :: _))
          } 
        
        case (None,error) => Some(
          (
            error.status,
            error match { 
              case err: ValidationError => err.report.asRight.toEitherNel
              case err: MessageError    => err.message.asLeft.toEitherNel 
            }
          ) 
        )
      }
      .map {
        case (status,Right(validationReport)) => Status(status)(Json.toJson(validationReport))
        case (status,Left(messages)) => Status(status)(Json.toJson(Outcome(messages)))
      }
      .get

}
*/
