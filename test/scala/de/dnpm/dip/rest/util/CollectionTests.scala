package de.dnpm.dip.rest.util


import scala.util.Random
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.Inspectors._
import org.scalatest.matchers.must.Matchers._
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.libs.json.{
  Json,
  OFormat
}
import de.ekut.tbi.generators.Gen


final case class FooBar
(
  foo: Int,
  bar: String,
  nested: FooBar.Nested
)

object FooBar
{

  final case class Nested(value: Double)

  implicit val formatNested: OFormat[Nested] =
    Json.format[Nested]

  implicit val format: OFormat[FooBar] =
    Json.format[FooBar]

  implicit val gen: Gen[FooBar] =
    for {
      foo <- Gen.intsBetween(0,42)
      bar <- Gen.letters(12)
      nested <- Gen.doubles
    } yield FooBar(
      foo,
      bar,
      Nested(nested)
    )

  val lists = Gen.listOf(100,Gen.of[FooBar]) 

}


class CollectionTests extends AnyFlatSpec
{

  implicit val rnd: Random = new Random


  "Collection[FooBar]" must "have been sorted by 'foo'" in {

    implicit val req: RequestHeader = FakeRequest("GET","/foobars?sort=foo")

    val collection = Collection(FooBar.lists.next)

    val fooBars = collection.entries.map(Json.fromJson[FooBar](_).get)

    fooBars.map(_.foo) mustBe sorted

  }

  it must "have been sorted by 'foo' in DESC order" in {

    implicit val req: RequestHeader = FakeRequest("GET","/foobars?sort=-foo")

    val collection = Collection(FooBar.lists.next)

    val fooBars = collection.entries.map(Json.fromJson[FooBar](_).get)

    fooBars.map(_.foo).reverse mustBe sorted

  }


  it must "have been sorted by 'foo' and 'bar'" in {

    implicit val req: RequestHeader = FakeRequest("GET","/foobars?sort=foo,bar")

    val collection = Collection(FooBar.lists.next)

    val fooBars = collection.entries.map(Json.fromJson[FooBar](_).get)

    fooBars.map { case FooBar(foo,bar,_) => foo -> bar } mustBe sorted

  }


  it must "have been sorted by 'foo' and 'bar' in DESC order" in {

    implicit val req: RequestHeader = FakeRequest("GET","/foobars?sort=-foo,-bar")

    val collection = Collection(FooBar.lists.next)

    val fooBars = collection.entries.map(Json.fromJson[FooBar](_).get)

    fooBars.map { case FooBar(foo,bar,_) => foo -> bar }.reverse mustBe sorted

  }


  it must "have been sorted by 'foo' in DESC order and then 'bar' in ASC order" in {

    implicit val req: RequestHeader = FakeRequest("GET","/foobars?sort=-foo,bar")

    val collection = Collection(FooBar.lists.next)

    val fooBars = collection.entries.map(Json.fromJson[FooBar](_).get)

    fooBars.map(_.foo).reverse mustBe sorted

    // All 'bar' values for the same 'foo' value must be sorted ASC
    forAll(fooBars.groupMap(_.foo)(_.bar)){ case (_,bars) => bars mustBe sorted }

  }


  it must "have been sorted by 'nested.value'" in {

    implicit val req: RequestHeader = FakeRequest("GET","/foobars?sort=nested.value")

    val collection = Collection(FooBar.lists.next)

    val fooBars = collection.entries.map(Json.fromJson[FooBar](_).get)

    fooBars.map(_.nested.value) mustBe sorted

  }

}
