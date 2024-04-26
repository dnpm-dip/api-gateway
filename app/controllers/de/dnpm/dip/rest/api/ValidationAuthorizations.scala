package de.dnpm.dip.rest.api


import de.dnpm.dip.auth.api.Authorization


trait ValidationAuthorizations[Agent]
{

  val ViewValidationInfosAuthorization: Authorization[Agent]

  val ViewValidationReportAuthorization: Authorization[Agent]

  val ViewInvalidPatientRecordAuthorization: Authorization[Agent]

}
