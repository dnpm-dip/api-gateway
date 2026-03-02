package de.dnpm.dip.rest.util


import scala.util.chaining._
import play.api.libs.json.{
  Json,
  JsValue,
  JsBoolean,
  JsObject,
  JsNumber,
  JsString,
  OWrites
}
import play.api.mvc.RequestHeader



final class Collection[+T] private (
  val entries: Seq[JsObject],
  val size: Int,
  val offset: Option[Int],
  val limit: Option[Int]
)


object Collection
{

  def apply[T: OWrites](
    ts: Seq[T]
  )(
    implicit req: RequestHeader
  ): Collection[T] = {

    val ordering = Collection.ordering(req)
    val offset = req.getQueryString("offset").map(_.toInt)
    val limit = req.getQueryString("limit").map(_.toInt)

    val entries =
      ts.map(Json.toJsObject(_))
        .pipe(jsons => ordering.fold(jsons)(jsons.sorted(_))) // First sort...
        .pipe(jsons => offset.fold(jsons)(jsons.drop))        // .. then apply pagination
        .pipe(jsons => limit.fold(jsons)(jsons.take))

    new Collection[T](
      entries,
      ts.size, // Total size info of collection (in case pagination were applied)
      offset,
      limit
    )
  }


  private object OrderedBy
  {
    object DESC {
      def unapply(attribute: String): Option[String] =
        Option.when(attribute startsWith "-")(attribute substring 1)
    }


    private val jsValue: Ordering[JsValue] =
      new Ordering[JsValue]{
        def compare(left: JsValue, right: JsValue): Int = { 
          (left,right) match { 
            case (JsString(l),JsString(r))    => l compare r
            case (JsNumber(l),JsNumber(r))    => l compare r
            case (l: JsBoolean, r: JsBoolean) => l.value compare r.value
            case _ => 0
          }
        }
      }


    private def value(
      nodes: List[String],
      json: JsValue
    ): Option[JsValue] =
      nodes match {
        case head :: Nil => (json \ head).toOption
        case head :: tail => tail.foldLeft(json \ head)((lookup,node) => (lookup \ node)).toOption
        case Nil => Some(json)
      }


    def apply(attributes: List[String]): Ordering[JsObject] =
      new Ordering[JsObject]{
        def compare(left: JsObject, right: JsObject): Int = {
          attributes.foldLeft(0){
            case (result,rawAttribute) if result == 0 =>

              // Determine whether DESC ordering is requested and the
              // attribute name stripped of leading dash "-foo" -> "foo"
              val (attribute,ordering) = rawAttribute match {
                case DESC(attribute) => attribute -> jsValue.reverse
                case attribute => attribute -> jsValue
              }

              val path = attribute.split("\\.").toList

              (value(path,left),value(path,right)) match {
                case (Some(l),Some(r)) => ordering.compare(l,r)
                case (Some(_), None) => 1
                case (None,Some(_)) => -1
                case _ => 0
              }

            case (result,_) => result
          }
        }
      }
  }


  private def ordering(req: RequestHeader): Option[Ordering[JsObject]] =
    req.getQueryString("sort")
      .map(_.split(",").toList)  // Ordering possible by multiple attributes
      .map(OrderedBy(_))


  implicit def writes[T]: OWrites[Collection[T]] =
    OWrites {
      coll =>
        Json.obj(
          "size"    -> Json.toJson(coll.size),
          "entries" -> Json.toJson(coll.entries),
        )
        .pipe(
          js => coll.offset.fold(js)(n => js + ("offset" -> Json.toJson(n)))
        )
        .pipe(
          js => coll.limit.fold(js)(n => js + ("limit" -> Json.toJson(n)))
        )
    }

}
