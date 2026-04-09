package de.dnpm.dip.rest.util


import cats.data.EitherNel
import cats.syntax.either._
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
    Wildcard,
    Tree,
    Identity
  }

  private def insert(path: List[Node]): JsonProjection =
    (path,this) match { 

      case (Nil,_) => Identity

      case (_,Identity) => Identity

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
        // Only apply projections if at least one field selector exists,
        // else return None
        Option.when(
          nodes.keys.exists {
            case _: Field => true 
            case _        => false
          }
        )(
          JsObject(
            nodes.flatMap {
              case (Field(key),proj) => value.get(key).flatMap(proj).map(key -> _) 
              case _ => None
            }
          )
        )
        
      case (Tree(nodes),JsArray(entries)) =>
        // Only apply projections if Wildcard selector exists (i.e. all array elements),
        // else return None
        nodes.get(Wildcard)
          .map(entries.flatMap(_).toSeq)
          .map(JsArray(_))

      case _ => None
    }

}

object JsonProjection
{

  private type JsonPath = List[Node]

  private sealed trait Node
  private case object Root extends Node
  private case class Field(name: String) extends Node
//  private case class Element(idx: Int) extends Node
//  private case class Slice(start: Int, end: Int) extends Node
  private case object Wildcard extends Node


  import fastparse._
  import NoWhitespace._

  private object JsonPath
  {

    private def root[$: P]: P[Node] =
      P("$").map(_ => Root)
 
    private def field[$: P]: P[Node] =
      P(CharsWhileIn("a-zA-Z0-9_").!).map(Field)

    private def dotField[$: P]: P[Node] =
      P("." ~ field)
  
//    private def element[$: P]: P[Node] =
//      P("[" ~ CharsWhileIn("0-9").!.map(_.toInt) ~ "]").map(Element)
  
    private def wildcard[$: P]: P[Node] =
      P("[*]").map(_ => Wildcard)
  
    private def segment[$: P]: P[Node] =
      P(dotField | wildcard)
//      P(dotField | element | wildcard)

    /**
     * Keep the parser tolerant:
     * Allow root $ to be missing, but thus also allow for a first field to occur without a dot,
     * e.g. 'field.nested' instead of full '$.field.nested'
     */
    private def path[$: P]: P[JsonPath] =
      P(root.? ~ field.? ~ segment.rep ~ End).map {
        // Ignore the root element from the path
        case (_,Some(field),segments) => field :: segments.toList
        case (_,None,segments)        => segments.toList
      }

    def parse(input: String): Parsed[JsonPath] =
      fastparse.parse(input, path(_))
  }


  private case class Tree(nodes: Map[Node,JsonProjection]) extends JsonProjection
  private case object Identity extends JsonProjection

  private object Tree 
  {
    val empty: JsonProjection = Tree(Map.empty)
  }


  def of(opt: Option[List[String]]): EitherNel[String,JsonProjection] =
    opt match { 
      case Some(projections) if projections.nonEmpty =>
        projections.map(JsonPath.parse)
          .foldLeft[EitherNel[String,JsonProjection]](
            Tree.empty.asRight.toEitherNel
          ){
            case (acc,        Parsed.Success(jsonpath,_)) => acc.map(_ insert jsonpath)
            case (Right(_),   Parsed.Failure(err,_,_))    => err.asLeft.toEitherNel 
            case (Left(errs), Parsed.Failure(err,_,_))    => (err :: errs).asLeft 
          }

      case _ => Identity.asRight.toEitherNel
    }


  /**
   * Implicit conversion of RequestHeader into JsonProjection:
   * Parse the CSV JSONPath projections from query parameter 'project' as
   * /api/resources?project=jsonpath1,jsonpath2,...
   */
  implicit def fromRequest(implicit req: RequestHeader): EitherNel[String,JsonProjection] =
    of(req.getQueryString("project").map(_.split(",")).map(_.toList))


  object syntax
  {

    implicit class JsValueProjectionOps(val json: JsValue) extends AnyVal
    { 
      def project(implicit project: EitherNel[String,JsonProjection]): EitherNel[String,JsValue] =
        project.flatMap(p => p(json).toRight("Empty JSON projection").toEitherNel)
    }

  }

}
