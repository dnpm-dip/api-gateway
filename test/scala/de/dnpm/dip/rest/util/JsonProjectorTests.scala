package de.dnpm.dip.rest.util


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.libs.json.{
  Json,
  OWrites
}


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
      42,
      3.1415,
      Foo.Composite(
        true,
        "hello",
        Some("maybe")
      ),
      Seq(
        Bar(1,1.234),
        Bar(2,2.345),
        Bar(3,3.456),
      )
    )


  val fields = Seq("int","double","composite.string","bars.int")


  "JsonProjector" must s"have picked fields ${fields.mkString(",")}" in { 

    implicit val req: RequestHeader =
      FakeRequest("GET",s"/foos?${fields.map(field => s"project=$field").mkString("&")}")

    val projected = Json.toJsObject(foo).transform(JsonProjector.of(req))

//    println(Json.prettyPrint(projected.get))
    assert(projected.isSuccess)
  }

}
