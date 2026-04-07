package de.dnpm.dip.rest.util


import play.api.libs.json.{
  Json,
  JsArray,
  JsObject,
  JsValue,
  Writes
}
import play.api.mvc.RequestHeader


object JsonProjector
{
/*
  private sealed trait Node 
  private case class Field(name: String) extends Node
  private case class Index(value: Int) extends Node
  private case object All extends Node

  private def project(path: List[Node], json: JsValue): Option[JsValue] =
    (path,json) match { 

      case (Nil,leaf) => Some(leaf)

      case (Field(field) :: tail, JsObject(entries)) =>
        entries.get(field).flatMap(project(tail,_))
          .map(v => Json.obj(field -> v))

      case (nodes, JsArray(entries)) =>
        nodes match { 

          case Index(i) :: tail =>
            if (0 <= i && i < entries.size) project(tail,entries(i)).map(JsArray.empty :+ _)
            else None

          case All :: tail =>
            val projected = entries.map(project(tail,_))
            Option.when(!projected.forall(_.isEmpty))(
              JsArray(projected.map(_.getOrElse(JsObject.empty)))
            )

          case _ => None
        }

      case _ => None
    }

  private val arrayElement  = "(.+)\\[(\\d+)\\]".r 
  private val arrayElements = "(.+)\\[\\*?\\]".r 

  def of(opt: Option[List[String]]): JsObject => JsObject =
    opt match { 
      case Some(projections) if projections.nonEmpty =>
        val paths =
          projections.map(
            _.split("\\.").toList.flatMap(
              node => node match { 
                case arrayElement(name,idx) => List(Field(name),Index(idx.toInt))
                case arrayElements(name)    => List(Field(name),All) 
                case name                   => List(Field(name))
              }
            )
          )

        json =>
          paths.foldLeft(JsObject.empty){
            (acc,path) => project(path,json) match {
              case Some(obj: JsObject) => acc deepMerge obj
              case _ => acc
            }
        }

      case _ => identity
    
    }
*/



  private sealed trait Node
  private case class Field(name: String) extends Node
  private case class Element(idx: Int) extends Node
//  private case class Slice(start: Int, end: Int) extends Node
  private case object All extends Node


  private sealed trait Projection
  private case class Obj(fields: Map[Node,Projection]) extends Projection
  private case class Arr(elements: Map[Node,Projection]) extends Projection
  private case object Identity extends Projection

  private object Obj 
  {
    val empty = Obj(Map.empty)
  }
  private object Arr
  {
    val empty = Arr(Map.empty)
  }

  private def apply(projection: Projection, json: JsValue): Option[JsValue] =
    (projection,json) match { 

      case (Identity,value) => Some(value)

      case (Obj(fields), js: JsObject) =>
        val projectedFields =
          fields.flatMap {
            case (Field(key),proj) => js.value.get(key).flatMap(apply(proj,_)).map(key -> _) 
            case _ => None
          }

        Option.when(projectedFields.nonEmpty)(
          JsObject(projectedFields)
        )

      case (Arr(elements), JsArray(entries)) =>
        val indexed =
          elements.flatMap { 
            case (Element(i),proj) if (entries isDefinedAt i) =>
              apply(proj,entries(i)).map(i -> _)

            case _ => None
          }

        val all = 
          elements.flatMap { 
            case (All,proj) =>
              entries.zipWithIndex.flatMap {
                case (value,i) => apply(proj,value).map(i -> _)
              }

            case _ => None
          }

        val projectedElements = (indexed ++ all) //.toMap

        Option.when(projectedElements.nonEmpty)(
          JsArray(projectedElements.toSeq.sortBy(_._1).map(_._2))
        )

      case _ => None

    }

    private def insert(nodes: List[Node], proj: Projection): Projection =
      (nodes,proj) match { 

        case (Nil,_) => Identity
 
        case ((field: Field) :: tail, Obj(fields)) => 
          Obj(fields.updatedWith(field)(_.map(insert(tail,_)).orElse(Some(insert(tail,Obj.empty)))))


        case ((element: Element) :: tail, Arr(elements)) =>
          Arr(elements.updatedWith(element)(_.map(insert(tail,_)).orElse(Some(insert(tail,Arr.empty)))))

        case (All :: tail, Arr(elements)) =>
          Arr(elements.updatedWith(All)(_.map(insert(tail,_)).orElse(Some(insert(tail,Arr.empty)))))

        case ((_: Field) :: _, _) => 
          insert(nodes,Obj.empty)

        case ((_: Element) :: _, _) =>
          insert(nodes,Arr.empty)

        case (All :: _, _) =>
          insert(nodes,Arr.empty)

      }


  private val arrayElement = "(.+)\\[(\\d+)\\]".r 
//  private val arraySlice   = "(.+)\\[(\\d+):(\\d+)\\]".r 
  private val arrayAll     = "(.+)\\[\\*?\\]".r 

  def of(opt: Option[List[String]]): JsValue => JsValue =
    opt match { 
      case Some(projections) if projections.nonEmpty =>
        val projection =
          projections.map(
            _.split("\\.").toList.flatMap(
              node => node match { 
                case arrayElement(name,idx)     => List(Field(name),Element(idx.toInt))
//                case arraySlice(name,start,end) => List(Array(name,Array.Slice(start,end)))
                case arrayAll(name)             => List(Field(name),All)
                case name                       => List(Field(name))
              }
            )
          )
          .foldLeft[Projection](Obj.empty)((proj,nodes) => insert(nodes,proj))


        json => apply(projection,json) match { 
          case Some(js) => js
          case _ => JsObject.empty
        }
        

      case _ => identity
    }

  def apply(implicit req: RequestHeader): JsValue => JsValue =
    of(req.getQueryString("project").map(_.split(",")).map(_.toList))


  object syntax
  {

    implicit class JsValueExtensions(val json: JsValue) extends AnyVal
    { 
      def projected(implicit req: RequestHeader): JsValue =
        JsonProjector(req).apply(json)
    }

    implicit class DTOExtensions[T](val t: T) extends AnyVal
    { 
      def toProjectedJson(
        implicit
        writes: Writes[T],
        req: RequestHeader
      ): JsValue =
        JsonProjector(req).apply(Json.toJson(t))
    }
  }

}

