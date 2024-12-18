package de.dnpm.dip.rest.util.sapphyre


import scala.util.chaining._
import play.api.libs.json.OWrites



class Hyper[+T] private (
  val data: T
)
extends Hypermediable


/*
case class Hyper[+T] private (
  data: T,
  links: Map[String,Link] = Map.empty,
  operations: Map[String,Operation] = Map.empty,
)
{

  def addLinks(ls: (String,Link)*) =
    this.copy(links = links ++ ls)

  def withLinks(ls: (String,Link)*) =
    this.copy(links = ls.toMap)

  def addOperations(ops: (String,Operation)*) =
    this.copy(operations = operations ++ ops)

  def withOperations(ops: (String,Operation)*) =
    this.copy(operations = ops.toMap)

}
*/


object Hyper
{

  def apply[T](t: T)(
    implicit tr: Mapper[T]
  ): Hyper[T] =
    tr(t)


  @annotation.implicitNotFound(
    "Couldn't find implicit Hyper.Mapper[${T}]. Define one or ensure it is in scope."
  )
  trait Mapper[T]
  {
    self =>

    def apply(t: T): Hyper[T]
/*
    def addLinks(ls: (String,Link)*): Mapper[T] =
      t => self(t).addLinks(ls: _*)

    def addOperations(ops: (String,Operation)*): Mapper[T] =
      t => self(t).addOperations(ops: _*)
*/

    def andThen(f: Hyper[T] => Hyper[T]): Mapper[T] =
      t => f(self(t))
  }

  object Mapper
  {

    implicit def of[T](f: T => Hyper[T]): Mapper[T] =
      new Mapper[T]{
        override def apply(t: T): Hyper[T] = f(t)
      }
  }


  object syntax
  {

    implicit class HyperExtensions[T](val t: T) extends AnyVal
    {

      def withLinks(ls: (String,Link)*): Hyper[T] =
        new Hyper(t).withLinks(ls: _*)
        
      def withOperations(ops: (String,Operation)*): Hyper[T] =
        new Hyper(t).withOperations(ops: _*)

/*
      def withLinks(ls: (String,Link)*) =
        Hyper(
          data = t,
          links = ls.toMap
        )

      def withOperations(ops: (String,Operation)*) =
        Hyper(
          data = t,
          operations = ops.toMap
        )
*/

    }

  }

/*
  implicit def writesHyper[T: OWrites]: OWrites[Hyper[T]] =
    OWrites {
      t =>
        Json.toJsObject(t.data)
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
*/

  implicit def writesHyper[T](implicit wt: OWrites[T]): OWrites[Hyper[T]] =
    Hypermediable.writes(wt.contramap[Hyper[T]](_.data))


  implicit def liftPredicate[T](
    f: T => Boolean
  ): Hyper[T] => Boolean =
    _.data pipe f

}
