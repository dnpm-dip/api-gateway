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
  ValueSet,
  ValueSetProvider
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

  private val codeSystemsLink =
    Link(s"$BASE_URI/codesystems")

  private val valueSetsLink =
    Link(s"$BASE_URI/valuesets")

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
    info => info.withLinks(
      COLLECTION   -> codeSystemsLink,
      "codesystem" -> codeSystemLink(info.uri),
      "valueset"   -> valueSetLink(info.uri)
    )

  implicit def HyperCodeSystem[S]: Hyper.Mapper[CodeSystem[S]] =
    cs => cs.withLinks(
      COLLECTION -> codeSystemsLink,
      SELF       -> codeSystemLink(cs.uri,cs.version),
      "valueset" -> valueSetLink(cs.uri,cs.version)
    )


  implicit val HyperValueSetProviderInfo: Hyper.Mapper[ValueSetProvider.Info] =
    info => info.withLinks(
      COLLECTION -> valueSetsLink,
      "valueset" -> valueSetLink(info.uri)
    )

  implicit def HyperValueSet[S]: Hyper.Mapper[ValueSet[S]] =
    vs => vs.withLinks(
      COLLECTION -> valueSetsLink,
      SELF       -> valueSetLink(vs.uri,vs.version)
    )

}

object CatalogHypermedia extends CatalogHypermedia
