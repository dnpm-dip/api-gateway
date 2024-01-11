package de.dnpm.dip.rest.api


import java.net.URI
import de.dnpm.dip.rest.util.sapphyre.{
  Link,
  Relations,
  Hyper,
  HypermediaBase
}
import de.dnpm.dip.coding.{
  CodeSystem,
  CodeSystemProvider,
  ValueSet
}



trait CatalogHypermedia extends HypermediaBase
{

  import Hyper.syntax._
  import Relations.{
    COLLECTION,
    SELF
  }


  protected val BASE_URI =
    s"$BASE_URL/api/coding"

  private val collectionLink =
    Link(s"$BASE_URI/codesystems")

  private def codeSystemLink(
    uri: URI,
    version: Option[String] = None
  ): Link =
    Link(s"$BASE_URI/codesystems?uri=${uri}${version.map(v => s"&version=$v").getOrElse("")}")

  private def valueSetLink(
    uri: URI,
    version: Option[String] = None
  ): Link =
    Link(s"$BASE_URI/valuesets?uri=${uri}${version.map(v => s"&version=$v").getOrElse("")}")




  implicit val HyperCodeSystemProviderInfo: Hyper.Mapper[CodeSystemProvider.Info[Any]] =
    Hyper.Mapper(
      info =>
        info.withLinks(
          COLLECTION   -> collectionLink,
          "codesystem" -> codeSystemLink(info.uri),
          "valueset"   -> valueSetLink(info.uri)
        )
    )

  implicit def HyperCodeSystem[S]: Hyper.Mapper[CodeSystem[S]] =
    Hyper.Mapper(
      cs =>
        cs.withLinks(
          COLLECTION  -> collectionLink,
          SELF        -> codeSystemLink(cs.uri,cs.version),
          "valueset"   -> valueSetLink(cs.uri,cs.version)
        )
    )


  implicit def HyperValueSet[S]: Hyper.Mapper[ValueSet[S]] =
    Hyper.Mapper(
      vs =>
        vs.withLinks(
          COLLECTION   -> collectionLink,
          "codesystem" -> codeSystemLink(vs.uri,vs.version),
          SELF         -> valueSetLink(vs.uri,vs.version)
        )
    )

}

object CatalogHypermedia extends CatalogHypermedia
