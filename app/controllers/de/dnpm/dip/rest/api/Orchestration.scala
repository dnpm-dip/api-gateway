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


object Orchestration
{

  final case class APIError
  (
    status: Int,
    value: EitherNel[String,ValidationReport]
  )

  import shapeless.Poly1

  private object toError extends Poly1
  { 
    implicit val validationServiceError: Case.Aux[ValidationService.Error,APIError] = at {
      case FatalIssuesDetected(report)         => APIError(BAD_REQUEST,report.rightNel)
      case UnacceptableIssuesDetected(report)  => APIError(UNPROCESSABLE_ENTITY,report.rightNel)
      case ValidationService.GenericError(msg) => APIError(INTERNAL_SERVER_ERROR,msg.leftNel)
    }

    implicit val mvhServiceError: Case.Aux[MVHService.Error,APIError] = at { 
      case InvalidTAN(msg)              => APIError(BAD_REQUEST,msg.leftNel)
      case InvalidSubmissionType(msg)   => APIError(BAD_REQUEST,msg.leftNel)
      case MVHService.GenericError(msg) => APIError(INTERNAL_SERVER_ERROR,msg.leftNel)
    }

    implicit val queryServiceError: Case.Aux[QueryService.DataError,APIError] = at { 
      case QueryService.GenericError(msg) => APIError(INTERNAL_SERVER_ERROR,msg.leftNel)
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

  // Combination logic of APIErrors
  private implicit val semigroup: Semigroup[APIError] =
    Semigroup.instance {
      case (err1 @ APIError(status1,Right(_)), err2 @ APIError(status2,Right(_))) =>
        if (priority(status1) > priority(status2)) err1
        else err2

      // A ValidationReport error primes over generic error messages
      case (APIError(status1,validationReport @ Right(_)),APIError(_,Left(_))) =>
        APIError(status1,validationReport) 
      case (APIError(_,Left(_)),APIError(status2,validationReport @ Right(_))) =>
        APIError(status2,validationReport)

      // Else concatenate multiple error messages	
      case (APIError(status1,Left(msgs1)),APIError(status2,Left(msgs2))) =>
        APIError(maxByPriority(status1,status2),(msgs1 ::: msgs2).asLeft)
    }


  object APIError
  {
    def apply(errors: NonEmptyList[Orchestrator.Error]): Result =
      errors.map(_ fold toError).reduce match {
        case APIError(status,Right(validationReport)) => Status(status)(Json.toJson(validationReport))
        case APIError(status,Left(messages)) => Status(status)(Json.toJson(Outcome(messages)))
      }
  }

/*
  def ErrorResult(errors: NonEmptyList[Orchestrator.Error]): Result =
    errors.map(_ fold toError).reduce match {
      case Error(status,Right(validationReport)) => Status(status)(Json.toJson(validationReport))
      case Error(status,Left(messages)) => Status(status)(Json.toJson(Outcome(messages)))
    }
*/
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
