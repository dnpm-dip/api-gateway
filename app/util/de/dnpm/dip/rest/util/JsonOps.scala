package de.dnpm.dip.rest.util


import scala.concurrent.{
  Future,
  ExecutionContext
}
import scala.util.Either
import cats.data.{
  Ior,
  IorNel
}
import cats.syntax.either._
import play.api.libs.json.{
  Json,
  JsValue,
  OWrites,
  Reads,
  Writes
}
import play.api.mvc.{
  BaseController,
  ActionBuilder,
  BodyParser,
  Request,
  RequestHeader,
  Result
}


trait JsonOps
{

  self: BaseController =>


  def JsonBody[T: Reads](
    implicit ec: ExecutionContext
  ): BodyParser[T] =
    parse
      .tolerantJson
      .validate(
        _.validate[T]
         .asEither
         .leftMap(
           errs => BadRequest(Json.toJson(Outcome(errs))),
         )
       )


  def JsonBodyOpt[T: Reads](
    implicit ec: ExecutionContext
  ): BodyParser[Option[T]] =
    parse
      .tolerantJson
      .validate(
        _.validateOpt[T]
         .asEither
         .leftMap(
           errs => BadRequest(Json.toJson(Outcome(errs))),
         )
       )


  def JsonAction[T: Reads](
    implicit ec: ExecutionContext
  ): ActionBuilder[Request,T] =
    new ActionBuilder[Request,T]{

      override val executionContext = ec

      override val parser: BodyParser[T] = JsonBody[T]

      override def invokeBlock[A](
        request: Request[A],
        block: (Request[A]) => Future[Result]
      ): Future[Result] = {
        block(request)
      }

    }


  def JsonActionOpt[T: Reads](
    implicit ec: ExecutionContext
  ): ActionBuilder[Request,Option[T]] =
    new ActionBuilder[Request,Option[T]]{

      override val executionContext =
        ec

      override val parser =
        JsonBodyOpt[T]

      override def invokeBlock[A](
        request: Request[A],
        block: (Request[A]) => Future[Result]
      ): Future[Result] = {
        block(request)
      }

    }


  def JsonResult[T: OWrites](
    ior: IorNel[String,T],
    err: JsValue => Result,
  ): Result =
    ior.leftMap(Outcome(_)) match {
      case Ior.Left(out)   => err(Json.toJson(out))
      case Ior.Right(t)    => Ok(Json.toJson(t))
      case Ior.Both(out,t) => Ok(Json.toJsObject(t) + ("_issues" -> Json.toJson(out.issues)))
    }


  def JsonResult[E,T: Writes](
    xor: Either[E,T]
  )(
    outcome: E => (Int,Outcome)
  ): Result =
    xor match {
      case Right(t) => Ok(Json.toJson(t))

      case Left(err) =>
        val (code,out) = outcome(err)
        Status(code)(Json.toJson(out))      
    }


  def JsonResult[T: Writes](
    opt: Option[T],
    err: => String = "Resource Not Found"
  ): Result =
    JsonResult(
      opt.toRight(err).toEitherNel
    )(
      NOT_FOUND -> Outcome(_) 
    )


  def ProjectedJsonResult[T: Writes](t: T)(
    implicit req: RequestHeader
  ): Result = {
    import JsonProjection.syntax._

    Json.toJson(t).project match { 
      case Right(json) => Ok(json)
      case Left(errs)  => BadRequest(Json.toJson(Outcome(errs)))
    }
  }

}
