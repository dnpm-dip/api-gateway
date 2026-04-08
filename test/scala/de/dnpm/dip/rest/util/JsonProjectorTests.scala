package de.dnpm.dip.rest.util


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.libs.json.{
  Json,
  JsArray,
  JsObject,
  OWrites
}
import JsonProjection.syntax._


case class Bar
(
  int: Int,
  double: Double
)

case class Foo
(
  int: Int,
  double: Double,
  composite: Foo.Composite,
  bars: Seq[Bar]
)

object Foo
{

  case class Composite
  (
    bool: Boolean,
    string: String,
    optString: Option[String]
  )

  implicit val writesBar: OWrites[Bar] = Json.writes[Bar]
  implicit val writesComposite: OWrites[Composite] = Json.writes[Composite]
  implicit val writes: OWrites[Foo] = Json.writes[Foo]
}


class JsonProjectorTests extends AnyFlatSpec
{

  val foo =
    Foo(
      int = 42,
      double = 3.1415,
      composite = Foo.Composite(
        bool = true,
        string = "hello",
        optString = Some("maybe")
      ),
      bars = Seq(
        Bar(int = 1, double = 1.234),
        Bar(int = 2, double = 2.345),
        Bar(int = 3, double = 3.456),
      )
    )


  val fields =
    Set(
      "int",
      "composite.string",
      "bars[1].int"
    )


  "JsonProjector" must s"have picked fields ${fields.mkString(",")}" in { 

    implicit val req: RequestHeader =
      FakeRequest("GET",s"/foos?project=${fields.mkString(",")}")

    val projected = Json.toJson(foo).project.getOrElse(JsObject.empty)

    println(Json.prettyPrint(projected))

    assert(projected.as[JsObject].keys == Set("int","composite","bars"))
    assert((projected \ "composite").as[JsObject].keys == Set("string"))
    assert((projected \ "bars").as[JsArray].value.size == 1)
    assert((projected \ "bars" \ 0).as[JsObject].keys == Set("int"))

  }

}
