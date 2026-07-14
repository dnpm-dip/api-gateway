package de.dnpm.dip.rest.util


import scala.concurrent.{
  Future,
  ExecutionContext
}
import cats.data.{
  Ior,
  IorNel,
  NonEmptyList
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
  BaseControllerHelpers,
  ActionBuilder,
  BodyParser,
  PlayBodyParsers,
  Request,
  RequestHeader,
  Result
}
import play.api.mvc.Results.BadRequest


trait CustomBodyParsers
{

  def parse: PlayBodyParsers

  def JsonBody[T: Reads](
    implicit ec: ExecutionContext
  ): BodyParser[T] =
    parse.tolerantJson
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
    parse.tolerantJson
      .validate(
        _.validateOpt[T]
         .asEither
         .leftMap(
           errs => BadRequest(Json.toJson(Outcome(errs))),
         )
       )

  /**
   * Utility method to build a BodyParser[T] that performs validation based on the injected function
   * before proceeding with JSON-to-DTO deserialization.
   *
   * @param schemaValidator: Function to validate a JSON string:
   * Returns a Result in case of failure to even process the JSON (malformed, etc) else a potentially empty list of validation errors
   */
  def SchemaValidatedJsonBody[T: Reads](
    schemaValidator: String => Either[Result,List[String]]
  )(
    implicit ec: ExecutionContext
  ): BodyParser[T] =
    // ByteString parser used deliberately here, because the expected payload type is JSON,
    // but the request should be accepted irrespective of the specified Content-type and charset
    parse.byteString
      .validate(
        bs =>
          // JSON string should be UTF-8 encoded, so ensure this
          Either.catchNonFatal(bs.decodeString("UTF-8"))
            .leftMap(t => BadRequest(Json.toJson(Outcome("Malformed body: JSON content is expected to be decodable as UTF-8"))))
      )
      .validate {
        jsonString =>
          schemaValidator(jsonString).flatMap {
            errs => NonEmptyList.fromList(errs) match {
                
              case Some(errors) =>
                BadRequest(Json.toJson(Outcome(errors))).asLeft
              
              case None  =>
                Json.parse(jsonString).validate[T]
                  .asEither
                  .leftMap(errors => BadRequest(Json.toJson(Outcome(errors))))
            }
          }
      }

}


trait JsonOps extends CustomBodyParsers
{

  self: BaseControllerHelpers =>


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

      override val executionContext = ec

      override val parser = JsonBodyOpt[T]

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
