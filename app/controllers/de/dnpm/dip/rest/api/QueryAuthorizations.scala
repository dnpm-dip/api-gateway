package de.dnpm.dip.rest.api


import de.dnpm.dip.auth.api.Authorization
import de.dnpm.dip.service.query.{
  PreparedQuery,
  Query
}



trait QueryAuthorizations[Agent]
{

  val SubmitQuery: Authorization[Agent]

  val ReadQueryResult: Authorization[Agent]

  val ReadPatientRecord: Authorization[Agent]

  def OwnershipOf(id: Query.Id): Authorization[Agent]

  def OwnershipOfPreparedQuery(id: PreparedQuery.Id): Authorization[Agent]

}
