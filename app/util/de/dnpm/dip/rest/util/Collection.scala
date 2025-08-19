package de.dnpm.dip.rest.util


import scala.util.chaining._
import play.api.libs.json.{
  Json,
  Writes,
  OWrites
}
import play.api.mvc.RequestHeader



final case class Collection[+T]
(
  private val seq: Seq[T],
  offset: Option[Int] = None,
  limit: Option[Int] = None
){

  def entries: Seq[T] =
    seq
      .pipe(ts => offset.fold(ts)(ts.drop))
      .pipe(ts => limit.fold(ts)(ts.take))

  def size: Int =
   seq.size

  def map[U](f: T => U): Collection[U] =
    this.copy(seq = seq map f)


  def paginated(implicit req: RequestHeader): Collection[T] =
    this.copy(
      offset = req.queryString.get("offset").flatMap(_.headOption.map(_.toInt)),
      limit = req.queryString.get("limit").flatMap(_.headOption.map(_.toInt))
    )

}


object Collection
{

  implicit def writes[T: Writes]: OWrites[Collection[T]] =
    OWrites {
      coll =>
        Json.obj(
          "size"    -> Json.toJson(coll.size),
          "entries" -> Json.toJson(coll.entries),
        )
        .pipe(
          js =>
            coll.offset.fold(js)(n => js + ("offset" -> Json.toJson(n)))
        )
        .pipe(
          js =>
            coll.limit.fold(js)(n => js + ("limit" -> Json.toJson(n)))
        )
    }

}
