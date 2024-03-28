package de.dnpm.dip.rest.util


import shapeless.Witness
import de.dnpm.dip.service.auth.PermissionEnumeration
import de.dnpm.dip.auth.api.{
  Authorization,
  UserPermissions
}


trait AuthorizationConversions
{

  import scala.language.implicitConversions


  implicit def permissionValueToAuthorization[E <: PermissionEnumeration](
    p: E#Value
  )(
    implicit w: Witness.Aux[E]
  ): Authorization[UserPermissions] =
    Authorization(
      _.permissions
       .collectFirst { case w.value(p) => true }
       .isDefined
    )

}
object AuthorizationConversions extends AuthorizationConversions

