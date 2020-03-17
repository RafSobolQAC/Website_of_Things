package controllers

import authentication.AuthenticationAction
import javax.inject._
import play.api.mvc._

@Singleton
class ThingController @Inject()(cc: ControllerComponents, authAction: AuthenticationAction) extends AbstractController(cc) {

}
