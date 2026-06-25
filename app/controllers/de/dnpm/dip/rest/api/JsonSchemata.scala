package de.dnpm.dip.rest.api


import scala.jdk.CollectionConverters._
import cats.Eval
import cats.syntax.either._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import com.networknt.schema.{
  JsonSchemaFactory,
  SpecVersion
}
import com.fasterxml.jackson.databind.ObjectMapper
import de.dnpm.dip.rest.util.Outcome


trait JsonSchemata[T]
{

  /**
   * Map of JSON schemata by schema spec version
   *
   * Eval used as value type to allow lazily populating the Map with Eval.later(...)
   */
  protected val formattedSchemata: Map[String,Eval[String]]


  /**
   * Function taking a DataUpload[T] payload as JSON String to perform validation
   * against the expected JSON schema.
   * The Left(Result) case serves to indicate that the payload couldn't be processed
   * as JSON in the first place, e.g. when malformed
   */
  protected lazy val schemaValidator: String => Either[Result,List[String]] = {

    val objectMapper = new ObjectMapper

    lazy val schema =
      JsonSchemaFactory
        .getInstance(SpecVersion.VersionFlag.V202012)
        .getSchema(
          formattedSchemata("draft-12").value
        )

    jsonString =>
      // Ensure parsing errors from malformed JSON are reported as 400 BadRequest
      Either.catchNonFatal(objectMapper.readTree(jsonString))
        .bimap(
          _ => BadRequest(Json.toJson(Outcome("Malformed body: Content is not valid JSON"))),
          s => schema.validate(s).asScala.map(_.getMessage).toList
        )

  }

}
