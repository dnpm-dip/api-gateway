package de.dnpm.dip.rest.util.sapphyre


import play.api.libs.json.{
  Json,
  JsObject,
  Writes
}


case class Hyper[T] private (
  data: T,
//  links: Option[Map[String,Link]] = None,
//  operations: Option[Map[String,Operation]] = None,
  links: Map[String,Link] = Map.empty,
  operations: Map[String,Operation] = Map.empty,
)
{
/*  
  def addLinks(ls: (String,Link)*) =
    this.copy(links = links.map(_ ++ ls).orElse(Some(ls.toMap)))

  def withLinks(ls: (String,Link)*) =
    this.copy(links = Some(ls.toMap))


  def addOperations(ops: (String,Operation)*) =
    this.copy(operations = operations.map(_ ++ ops).orElse(Some(ops.toMap)))

  def withOperations(ops: (String,Operation)*) =
    this.copy(operations = Some(ops.toMap))
*/

  def addLinks(ls: (String,Link)*) =
    this.copy(links = links ++ ls)

  def withLinks(ls: (String,Link)*) =
    this.copy(links = ls.toMap)

  def addOperations(ops: (String,Operation)*) =
    this.copy(operations = operations ++ ops)

  def withOperations(ops: (String,Operation)*) =
    this.copy(operations = ops.toMap)

}


object Hyper
{

  @annotation.implicitNotFound(
    "Couldn't find implicit Hyper.Mapper[${T}]. Define one or ensure it is in scope."
  )
  trait Mapper[T]
  {
    self =>

    def apply(t: T): Hyper[T]

    def addLinks(ls: (String,Link)*): Mapper[T] =
      Mapper(
        t => self(t).addLinks(ls: _*)
      )

    def addOperations(ops: (String,Operation)*): Mapper[T] =
      Mapper(
        t => self(t).addOperations(ops: _*)
      )

  }

  object Mapper
  {
    def apply[T](f: T => Hyper[T]): Mapper[T] =
      new Mapper[T]{
        override def apply(t: T): Hyper[T] = f(t)
      }
  }


  def apply[T](t: T)(
    implicit tr: Mapper[T]
  ): Hyper[T] =
    tr(t)


  val Api =
    JsObject.empty


  object syntax
  {

    implicit class HyperExtensions[T](val t: T) extends AnyVal
    {
      def withLinks(ls: (String,Link)*) =
        Hyper(
          data = t,
          links = ls.toMap
//          links = Some(ls.toMap)
        )

      def withOperations(ops: (String,Operation)*) =
        Hyper(
          data = t,
          operations = ops.toMap
//          operations = Some(ops.toMap)
        )

    }

  }


  implicit def writesHyper[T: Writes]: Writes[Hyper[T]] =
    Writes {
      t =>
//        Json.toJsObject(t.data) ++
        Json.toJson(t.data).as[JsObject] ++
          Json.obj(
            "_links"      -> Json.toJson(t.links),
            "_operations" -> Json.toJson(t.operations)
          )
    }

}
