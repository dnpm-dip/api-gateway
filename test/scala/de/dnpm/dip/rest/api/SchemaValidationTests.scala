package de.dnpm.dip.rest.api


import scala.util.Random
import scala.util.chaining._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatest.EitherValues._
import de.ekut.tbi.generators.Gen
import de.dnpm.dip.mtb.model.MTBPatientRecord
import de.dnpm.dip.mtb.gens.Generators._
import play.api.libs.json.Json



class SchemaValidationTests extends AnyFlatSpec with MTBJsonSchemata
{

  implicit val rnd: Random = new Random(42)


  "SchemaValidator" must "have succeeded on correct JSON input" in { 

    val json =
      Gen.of[MTBPatientRecord].next
        .pipe(Json.toJson(_))
        .pipe(Json.stringify)

    schemaValidator(json).value must be (empty) 

  }


  it must "have failed on JSON input missing required fields" in { 

    val json =
      Gen.of[MTBPatientRecord].next
        .pipe(Json.toJsObject(_))
        .pipe(js => js - "patient")
        .pipe(Json.stringify)

    schemaValidator(json).value must not be (empty) 

  }

  it must "have failed on JSON input with unexpected additional fields" in { 

    val json =
      Gen.of[MTBPatientRecord].next
        .pipe(Json.toJsObject(_))
        .pipe(js =>
          js ++ Json.obj("additionalString" -> "foo")
        )
        .pipe(Json.stringify)

    schemaValidator(json).value must not be (empty) 

  }

}
