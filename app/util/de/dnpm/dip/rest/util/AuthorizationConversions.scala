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
    v: E#Value
  )(
    implicit w: Witness.Aux[E]
  ): Authorization[UserPermissions] =
    Authorization(
      _.permissions
       .exists {
         case w.value(p) if p == v => true
         case _                    => false
       }
    )



}
object AuthorizationConversions extends AuthorizationConversions

