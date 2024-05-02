package de.dnpm.dip.rest.api


import de.dnpm.dip.auth.api.Authorization


trait ValidationAuthorizations[Agent]
{

  val ReadValidationInfos: Authorization[Agent]

  val ReadValidationReport: Authorization[Agent]

  val ReadInvalidPatientRecord: Authorization[Agent]

}
