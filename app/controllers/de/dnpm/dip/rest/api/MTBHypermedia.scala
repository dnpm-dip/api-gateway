package de.dnpm.dip.rest.api


import de.dnpm.dip.rest.util.sapphyre.{
  Hyper,
  Link
}
import de.dnpm.dip.mtb.query.api.MTBConfig


trait MTBHypermedia extends UseCaseHypermedia[MTBConfig]
{

  override implicit def HyperQuery: Hyper.Mapper[QueryType] =
    super.HyperQuery
      .andThen(
        query => query.addLinks(
          "patient-filter"               -> Link(s"${Uri(query.data)}/filters/patient"),
          "diagnosis-filter"             -> Link(s"${Uri(query.data)}/filters/diagnosis"),
          "recommendation-filter"        -> Link(s"${Uri(query.data)}/filters/therapy-recommendation"),
          "therapy-filter"               -> Link(s"${Uri(query.data)}/filters/therapy"),
          "kaplan-meier-config"          -> Link(s"$BASE_URI/kaplan-meier/config"),
          "kaplan-meier-stats"           -> Link(s"${Uri(query.data)}/survival-statistics[?type={type}&grouping={grouping}]"),
          "tumor-diagnostics"            -> Link(s"${Uri(query.data)}/tumor-diagnostics"),
          "medication"                   -> Link(s"${Uri(query.data)}/medication"),
          "therapy-responses"            -> Link(s"${Uri(query.data)}/therapy-responses"),
          "gene-alterations"             -> Link(s"${Uri(query.data)}/gene-alterations"),
          "altered-gene-distributions"   -> Link(s"${Uri(query.data)}/altered-gene-distributions")

        )
      )

}
