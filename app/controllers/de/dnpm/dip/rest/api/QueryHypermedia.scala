package de.dnpm.dip.rest.api


import java.net.URI
import play.api.libs.json.Json
import de.dnpm.dip.rest.util.sapphyre.{
  Operation,
  Link,
  Relations,
  Method,
  Hyper
}
import de.dnpm.dip.model.Patient
import de.dnpm.dip.service.query.{
  PatientMatch,
  Query,
  UseCaseConfig
}


/*
class QueryHypermedia[UseCase <: UseCaseConfig](
  prefix: String
)
{
  type PatientRecord = UseCase#PatientRecord
  type Criteria      = UseCase#Criteria
  type Filters       = UseCase#Filters
  type Results       = UseCase#Results
*/

trait QueryHypermedia[UseCase <: UseCaseConfig]
{

  self: QueryController[UseCase] =>


  import Hyper.syntax._
  import Relations.{
    COLLECTION,
    SELF,
    UPDATE
  }
  import Method.{GET,POST,PUT}



  val prefix: String


  private val BASE_URL =
    Option(System.getProperty("de.dnpm.dip.rest.api.baseurl"))
      .getOrElse("")


  protected val BASE_URI =
    s"$BASE_URL/api/$prefix/query"


  private def QueryUri(id: Query.Id) =
    s"$BASE_URI/${id.value}"

  private def Uri(query: Query[UseCase#Criteria,UseCase#Filters]) =
    QueryUri(query.id)



  private val HyperQueryOps =
    Hyper.Api
      .withOperations(
        "submit" -> Operation(POST, Link(BASE_URI))
      )

  def HyperQueryApi =
    Action { Ok(Json.toJson(HyperQueryOps)) }
      


  implicit val HyperQuery: Hyper.Mapper[Query[UseCase#Criteria,UseCase#Filters]] =
    Hyper.Mapper {
      query =>

        val selfLink =
          Link(Uri(query))

        query.withLinks(
          SELF              -> selfLink,
          "summary"         -> Link(s"${Uri(query)}/summary"),
          "patient-matches" -> Link(s"${Uri(query)}/patient-matches")
        )
        .withOperations(
          UPDATE   -> Operation(PUT, selfLink),
          "filter" -> Operation(PUT, Link(s"${Uri(query)}/filters"))
        )
    }


  implicit def HyperSummary(
    implicit id: Query.Id
  ): Hyper.Mapper[Results#Summary] =
    Hyper.Mapper {
      _.withLinks(
        "query"           -> Link(QueryUri(id)),
        "patient-matches" -> Link(s"${QueryUri(id)}/patient-matches")
      )
    }


  implicit def HyperPatientMatch(
    implicit id: Query.Id
  ): Hyper.Mapper[PatientMatch[UseCase#Criteria]] =
    Hyper.Mapper {
      patMatch =>
        patMatch.withLinks(
          "query"          -> Link(QueryUri(id)),
          "patient-record" -> Link(s"${QueryUri(id)}/patient-record/${patMatch.id.value}")
        )
    }


  implicit def HyperPatientRecord(
    implicit id: Query.Id
  ): Hyper.Mapper[UseCase#PatientRecord] =
    Hyper.Mapper {
      patRec =>

        import scala.language.reflectiveCalls

        patRec.withLinks(
          "query" -> Link(QueryUri(id)),
          SELF    -> Link(s"${QueryUri(id)}/patient-record/${patRec.patient.id.value}")
        )
    }

}
