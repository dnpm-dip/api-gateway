package de.dnpm.dip.rest.api


import java.net.URI
import de.dnpm.dip.rest.util.sapphyre.{
  Link,
  Relations,
  Hyper
}
import de.dnpm.dip.coding.{
  CodeSystem,
  ValueSet
}



trait CatalogHypermedia
{

  import Hyper.syntax._
  import Relations.{
    COLLECTION,
    SELF
  }


  private val BASE_URI = "/api/coding"


  private def CodeSystemLink(
    uri: URI,
    version: Option[String]
  ): Link =
    Link(s"$BASE_URI/codesystems?uri=${uri}${version.map(v => s"&version=$v").getOrElse("")}")

  private def ValueSetLink(
    uri: URI,
    version: Option[String]
  ): Link =
    Link(s"$BASE_URI/valuesets?uri=${uri}${version.map(v => s"&version=$v").getOrElse("")}")



  implicit val HyperCodeSystemInfo: Hyper.Mapper[CodeSystem.Info] =
    Hyper.Mapper(
      info =>
        info.withLinks(
          COLLECTION   -> Link(s"$BASE_URI/codesystems"),
          "codesystem" -> CodeSystemLink(info.uri,info.version),
          "valueset"   -> ValueSetLink(info.uri,info.version)
        )
    )


  implicit def HyperCodeSystem[S]: Hyper.Mapper[CodeSystem[S]] =
    Hyper.Mapper(
      cs =>
        cs.withLinks(
          COLLECTION  -> Link(s"$BASE_URI/codesystems"),
          SELF        -> CodeSystemLink(cs.uri,cs.version),
          "valueset"   -> ValueSetLink(cs.uri,cs.version)
        )
    )


  implicit def HyperValueSet[S]: Hyper.Mapper[ValueSet[S]] =
    Hyper.Mapper(
      vs =>
        vs.withLinks(
          COLLECTION   -> Link(s"$BASE_URI/codesystems"),
          "codesystem" -> CodeSystemLink(vs.uri,vs.version),
          SELF         -> ValueSetLink(vs.uri,vs.version)
        )
    )

}

object CatalogHypermedia extends CatalogHypermedia
