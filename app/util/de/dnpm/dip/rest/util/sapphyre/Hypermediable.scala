package de.dnpm.dip.rest.util.sapphyre


import scala.collection.mutable.Map
import scala.util.chaining._
import play.api.libs.json.{
  Json,
  JsObject,
  Writes,
  OWrites
}


abstract class Hypermediable
{

  protected[sapphyre] var links: Map[String,Link] = Map.empty
  
  protected[sapphyre] var operations: Map[String,Operation] = Map.empty


  def addLinks(ls: (String,Link)*): this.type = {
    links ++= ls
    this
  }

  def withLinks(ls: (String,Link)*): this.type = {
    links = ls.to(Map)
    this
  }

  def addOperations(ops: (String,Operation)*): this.type = {
    operations ++= ops
    this
  }

  def withOperations(ops: (String,Operation)*): this.type = {
    operations = ops.to(Map)
    this
  }

}


object Hypermediable
{

  def writes[T <: Hypermediable](w: OWrites[T]): OWrites[T] =
    OWrites {
      t =>
        w.writes(t)
          .pipe(
            js =>
              t.links.nonEmpty match {
                case true  => js + ("_links" -> Json.toJson(t.links))
                case false => js
              }
          )
          .pipe(
            js =>
              t.operations.nonEmpty match {
                case true  => js + ("_operations" -> Json.toJson(t.operations))
                case false => js
              }
          )
    }

}
