package de.dnpm.dip.rest.api


import de.dnpm.dip.auth.api.Authorization
import de.dnpm.dip.service.query.Query



trait QueryAuthorizations[Agent]
{

  val SubmitQuery: Authorization[Agent]

  val ReadQueryResult: Authorization[Agent]

  val ReadPatientRecord: Authorization[Agent]

  def OwnerOf(id: Query.Id): Authorization[Agent]

}
