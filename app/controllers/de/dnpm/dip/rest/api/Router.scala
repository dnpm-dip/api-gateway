package de.dnpm.dip.rest.api


import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter


class Router @Inject()(
  catalogRouter: CatalogRouter,
  adminRouter: AdminRouter,
  mtbRouter: MTBRouter,
  rdRouter: RDRouter
)
extends SimpleRouter
{

  override val routes: Routes =
    Seq(
      adminRouter,
      catalogRouter,
      mtbRouter withPrefix mtbRouter.prefix,
      rdRouter withPrefix rdRouter.prefix
    )
    .reduce(_ orElse _)
    .routes

}
