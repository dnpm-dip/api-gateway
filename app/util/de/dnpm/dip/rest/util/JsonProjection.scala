package de.dnpm.dip.rest.util


import cats.data.EitherNel
import cats.syntax.either._
import cats.syntax.traverse._
import cats.syntax.validated._
import play.api.libs.json.{
  JsArray,
  JsObject,
  JsValue,
}
import play.api.mvc.RequestHeader


sealed trait JsonProjection extends (JsValue => Option[JsValue])
{

  import JsonProjection.{
    Selector,
    Field,
    Wildcard,
    Tree,
    Identity
  }

  private def insert(segments: List[Selector]): JsonProjection =
    (segments,this) match { 

      case (Nil,_) => Identity

      case (_,Identity) => Identity

      case ((node: Selector) :: tail, Tree(nodes)) => 
        Tree( 
          nodes.updatedWith(node)(
            _.map(_ insert tail)
             .orElse(Some(Tree.empty.insert(tail)))
          )
        )

      case _ => Tree.empty.insert(segments)
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

  private type JsonPath = List[Selector]

  private sealed trait Selector
  private case object Root extends Selector
  private case class Field(name: String) extends Selector
  private case object Wildcard extends Selector


  import fastparse._
  import NoWhitespace._

  private object JsonPath
  {

    private def root[$: P]: P[Selector] =
      P("$").map(_ => Root)
 
    private def field[$: P]: P[Selector] =
      P(CharsWhileIn("a-zA-Z0-9_").!).map(Field)

    private def dotField[$: P]: P[Selector] =
      P("." ~ field)
  
    private def wildcard[$: P]: P[Selector] =
      P("[*]").map(_ => Wildcard)
  
    private def segment[$: P]: P[Selector] =
      P(dotField | wildcard)

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


  private case class Tree(nodes: Map[Selector,JsonProjection]) extends JsonProjection
  private case object Identity extends JsonProjection

  private object Tree 
  {
    val empty: JsonProjection = Tree(Map.empty)
  }


  def of(opt: Option[List[String]]): EitherNel[String,JsonProjection] =
    opt match { 
      case Some(projections) if projections.nonEmpty =>
        projections.traverse {
          case path if path.isEmpty => s"Empty JSONPath detected".invalidNel

          case path => JsonPath.parse(path) match {
            case Parsed.Success(jsonpath,_) => jsonpath.validNel
            case _: Parsed.Failure          => s"Malformed JSONPath '$path'".invalidNel
          }
        }
        .map(_.foldLeft[JsonProjection](Tree.empty)(_ insert _))
        .toEither

      case _ => Identity.rightNel
    }


  /**
   * Implicit conversion of RequestHeader into JsonProjection:
   * Parse the CSV JSONPath projections from query parameter 'project' as
   * /api/resources?project=jsonpath1,jsonpath2,...
   */
  implicit def fromRequest(implicit req: RequestHeader): EitherNel[String,JsonProjection] =
    of(
      req.getQueryString("project")
        .map(_.split(",",-1).toList.map(_.trim))
    )


  object syntax
  {

    implicit class JsValueProjectionOps(val json: JsValue) extends AnyVal
    { 
      def project(implicit project: EitherNel[String,JsonProjection]): EitherNel[String,JsValue] =
        project.flatMap(p => p(json).toRight("Empty JSON projection").toEitherNel)
    }

  }

}
