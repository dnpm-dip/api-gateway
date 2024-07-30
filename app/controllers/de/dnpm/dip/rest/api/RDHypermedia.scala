package de.dnpm.dip.rest.api


import de.dnpm.dip.rest.util.sapphyre.{
  Hyper,
  Link
}
import de.dnpm.dip.rd.query.api.RDConfig


trait RDHypermedia extends UseCaseHypermedia[RDConfig]
{

  override implicit def HyperQuery: Hyper.Mapper[QueryType] =
    super.HyperQuery
      .andThen(
        h => h.addLinks(
          "diagnostics" -> Link(s"${Uri(h.data)}/diagnostics"),
        )
      )

}
