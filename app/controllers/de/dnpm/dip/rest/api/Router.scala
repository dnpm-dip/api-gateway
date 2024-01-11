package de.dnpm.dip.rest.api



import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._



class Router @Inject()(
  catalogRouter: CatalogRouter,
  mtbRouter: MTBQueryRouter,
  rdRouter: RDQueryRouter
)
extends SimpleRouter
{

  override val routes: Routes =
    catalogRouter
      .orElse(mtbRouter withPrefix mtbRouter.prefix)
      .orElse(rdRouter withPrefix rdRouter.prefix)
      .routes


}
