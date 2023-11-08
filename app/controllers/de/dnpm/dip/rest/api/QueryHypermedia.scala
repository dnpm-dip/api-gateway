package de.dnpm.dip.rest.api


import java.net.URI
import play.api.libs.json.Json
import de.dnpm.dip.rest.util.Collection
import de.dnpm.dip.rest.util.sapphyre.{
  Operation,
  Link,
  Relations,
  Method,
  Hyper,
  HypermediaBase
}
import de.dnpm.dip.model.Patient
import de.dnpm.dip.service.query.{
  Collection => Coll,
  PatientMatch,
  Query,
  UseCaseConfig
}
import scala.util.chaining._



trait QueryHypermedia[UseCase <: UseCaseConfig] extends HypermediaBase
{

  self: QueryController[UseCase] =>


  type QueryType = Query[UseCase#Criteria,UseCase#Filters]


  import Hyper.syntax._
  import Relations.{
    COLLECTION,
    SELF,
    UPDATE
  }
  import Method.{GET,POST,PUT}



  val prefix: String


  protected val BASE_URI =
    s"$BASE_URL/api/$prefix/queries"


  private def QueryUri(id: Query.Id) =
    s"$BASE_URI/${id.value}"

  private def Uri(query: QueryType) =
    QueryUri(query.id)



  implicit val HyperQuery: Hyper.Mapper[QueryType] =
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


  implicit val HyperQueryCollection: Hyper.Mapper[Collection[Hyper[QueryType]]] =
    Hyper.Mapper { 
      _.withOperations(
        "submit" -> Operation(POST, Link(BASE_URI))
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


  implicit def HyperPatientMatches(
    implicit id: Query.Id,
  ): Hyper.Mapper[Coll[Hyper[PatientMatch[UseCase#Criteria]]]] =
    Hyper.Mapper {
      matches =>

        val prev =
          matches.limit.flatMap(
            n => 
              matches.offset.map(_ - n)
                .collect {
                  case off if (off > 0) => off - n -> n
                  case _                => 0 -> n
                }
          )

        val next =
          matches.limit.collect {
            case n if (matches.size - n >= 0) =>
              (matches.offset.getOrElse(0) + n -> n)
          }

      matches.withLinks(
        "query" -> Link(QueryUri(id)),
       )
       .pipe { coll =>

         prev match {
           case Some(0 -> n) =>
             coll.addLinks(
               "prev" -> Link(s"${QueryUri(id)}/patient-matches?limit=${n}"),
             )
           case Some(off -> n) =>
             coll.addLinks(
               "prev" -> Link(s"${QueryUri(id)}/patient-matches?offset=${off}&limit=${n}")
             )
           case _ => coll
         }
       }
       .pipe { coll =>

         next match {
           case Some(off -> n) =>
             coll.addLinks(
               "next" -> Link(s"${QueryUri(id)}/patient-matches?offset=${off}&limit=${n}"),
             )
           case _ => coll
         }
       }
    }

/*
  def HyperPatientMatches(
    id: Query.Id,
    offset: Option[Int],
    limit: Option[Int]
  ): Hyper.Mapper[Collection[Hyper[PatientMatch[UseCase#Criteria]]]] =
    Hyper.Mapper {
      matches =>

        val prev =
          limit.flatMap(
            n => 
              offset.map(_ - n)
                .collect {
                  case off if (off > 0) => off - n -> n
                  case _                => 0 -> n
                }
          )

        val next =
          limit.collect {
            case n if (matches.entries.size - n >= 0) =>
              (offset.getOrElse(0) + n -> n)
          }

      matches.withLinks(
        "query" -> Link(QueryUri(id)),
       )
       .pipe { coll =>

         prev match {
           case Some(0 -> n) =>
             coll.addLinks(
               "prev" -> Link(s"${QueryUri(id)}/patient-matches?limit=${n}"),
             )
           case Some(off -> n) =>
             coll.addLinks(
               "prev" -> Link(s"${QueryUri(id)}/patient-matches?offset=${off}&limit=${n}")
             )
           case _ => coll
         }
       }
       .pipe { coll =>

         next match {
           case Some(off -> n) =>
             coll.addLinks(
               "next" -> Link(s"${QueryUri(id)}/patient-matches?offset=${off}&limit=${n}"),
             )
           case _ => coll
         }
       }
    }
*/

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
