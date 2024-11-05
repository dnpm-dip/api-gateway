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
        q => q.addLinks(
          "patient-filter"   -> Link(s"${Uri(q.data)}/filters/patient"),
          "diagnosis-filter" -> Link(s"${Uri(q.data)}/filters/diagnosis"),
          "hpo-filter"       -> Link(s"${Uri(q.data)}/filters/hpo"),
          "diagnostics"      -> Link(s"${Uri(q.data)}/diagnostics"),
        )
      )

}
