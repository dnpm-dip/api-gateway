package de.dnpm.dip.rest.util


import scala.concurrent.{
  Future,
  ExecutionContext
}
import scala.util.Either
import cats.data.{
  Ior,
  IorNel,
  NonEmptyList
}
import cats.syntax.either._
import play.api.libs.json.{
  Json,
  JsValue,
  Reads,
  OWrites,
  Writes
}
import play.api.mvc.{
  BaseController,
  ActionBuilder,
  BodyParser,
  Request,
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

      override val executionContext =
        ec

      override val parser: BodyParser[T] =
        JsonBody[T]

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


  def JsonResult[T: Writes](
    xor: Either[NonEmptyList[String],T],
    err: JsValue => Result
  ): Result =
    xor.leftMap(
      Outcome(_)
    )
    .bimap(
      Json.toJson(_),
      Json.toJson(_)
    )
    .fold(
      err(_),
      Ok(_)
    )


  def JsonResult[T: Writes](
    opt: Option[T],
    err: => String = "Resource Not Found"
  ): Result =
    JsonResult(
      opt.toRight(err).toEitherNel,
      NotFound(_)
    )


}
