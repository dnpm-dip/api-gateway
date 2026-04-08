package de.dnpm.dip.rest.util


import play.api.libs.json.{
  JsArray,
  JsObject,
  JsValue,
}
import play.api.mvc.RequestHeader


sealed trait JsonProjection extends (JsValue => Option[JsValue])
{

  import JsonProjection.{
    Node,
    Field,
    Element,
    Slice,
    All,
    Tree,
    Identity
  }

  private def insert(path: List[Node]): JsonProjection =
    (path,this) match { 

      case (Nil,_) => Identity

      case ((node: Node) :: tail, Tree(nodes)) => 
        Tree( 
          nodes.updatedWith(node)(
            _.map(_ insert tail)
             .orElse(Some(Tree.empty.insert(tail)))
          )
        )

      case _ => Tree.empty.insert(path)
    }
    
    
  override def apply(json: JsValue): Option[JsValue] =
    (this,json) match {

      case (Identity,_) => Some(json)

      case (Tree(nodes), JsObject(value)) =>
        val projectedFields =
          nodes.flatMap {
            case (Field(key),proj) => value.get(key).flatMap(proj).map(key -> _) 
            case _ => None
          }

        Option.when(projectedFields.nonEmpty)(
          JsObject(projectedFields)
        )

        
      case (Tree(nodes),JsArray(entries)) =>
        val indexed =
          nodes.flatMap { 
            case (Element(i),proj) if (entries isDefinedAt i) => proj(entries(i)).map(i -> _)
            case _ => None
          }

        val sliced = 
          nodes.flatMap { 
            case (Slice(start,end),proj) =>
              entries.zipWithIndex
                .slice(start,end)
                .flatMap { case (value,i) => proj(value).map(i -> _) }

            case _ => None
          }

        val all = 
          nodes.flatMap { 
            case (All,proj) =>
              entries.zipWithIndex.flatMap { case (value,i) => proj(value).map(i -> _) }

            case _ => None
          }

        val projectedElements = (indexed ++ sliced ++ all)

        Option.when(projectedElements.nonEmpty)(
          JsArray(projectedElements.toSeq.sortBy(_._1).map(_._2))
        )

      case _ => None
    }

}

object JsonProjection
{

  private sealed trait Node
  private case class Field(name: String) extends Node
  private case class Element(idx: Int) extends Node
  private case class Slice(start: Int, end: Int) extends Node
  private case object All extends Node

  private case class Tree(nodes: Map[Node,JsonProjection]) extends JsonProjection
  private case object Identity extends JsonProjection

  private object Tree 
  {
    val empty: JsonProjection = Tree(Map.empty)
  }


  //TODO: Generalize parser 
  private val objectField       = "\\['([a-zA-Z]+)'\\]".r 
  private val namedArrayElement = "(.+)\\[(\\d+)\\]".r 
  private val namedArraySlice   = "(.+)\\[(\\d+):(\\d+)\\]".r 
  private val namedArrayAll     = "(.+)\\[\\*?\\]".r 

  def of(opt: Option[List[String]]): JsonProjection =
    opt match { 
      case Some(projections) if projections.nonEmpty =>
        projections.map(
          _.split("\\.").toList.flatMap(
            node => node match { 
              case namedArrayElement(name,idx)     => List(Field(name),Element(idx.toInt))
              case namedArraySlice(name,start,end) => List(Field(name),Slice(start.toInt,end.toInt))
              case namedArrayAll(name)             => List(Field(name),All)
              case objectField(name)               => List(Field(name))
              case name                            => List(Field(name))
            }
          )
        )
        .foldLeft[JsonProjection](Tree.empty)(_ insert _)

      case _ => Identity
    }

  /**
   * Implicit conversion of RequestHeader into JsONProject:
   * Parse the CSV JSON Path projections from query parameter 'project' as
   * /api/resources?project=jsonpath1,jsonpath2,...
   */
  implicit def fromRequest(implicit req: RequestHeader): JsonProjection =
    of(req.getQueryString("project").map(_.split(",")).map(_.toList))


  object syntax
  {

    implicit class JsValueProjectionOps(val json: JsValue) extends AnyVal
    { 
      def project(implicit project: JsonProjection): Option[JsValue] =
        project(json)
    }

  }

}
