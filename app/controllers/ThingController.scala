package controllers

import javax.inject.{Inject, _}
import models.{Thing, ThingWithID}
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.play.json._
import models.JsonFormats._
import play.api.libs.json.{JsValue, Json}
import reactivemongo.api.Cursor
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.{JSONCollection, _}
import services.MongoServices
import utils.TestDataCreator

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source
import scala.util.Random


class ThingController @Inject()(
                                 components: ControllerComponents,
                                 val reactiveMongoApi: ReactiveMongoApi,
                                 val mongoServices: MongoServices
                               ) extends AbstractController(components)
  with MongoController with ReactiveMongoComponents with play.api.i18n.I18nSupport {

  implicit def ec: ExecutionContext = components.executionContext

  def collection: Future[JSONCollection] = database.map(_.collection[JSONCollection]("things"))

  var thingsList: List[ThingWithID] = List()

  def makeThings = {
    val cursor: Future[Cursor[ThingWithID]] = collection.map { el =>
      el.find(Json.obj())
        .cursor[ThingWithID]()
    }

    cursor.flatMap(el => {
      println(el + " is here!")
      el.collect[List](
        -1,
        Cursor.FailOnError[List[ThingWithID]]()
      )
    })
      .map { things =>
        println("I'm in thingslist!")
        thingsList = things
      }
  }

  def create(thing: Thing) = Action.async { implicit request: Request[AnyContent] =>
    mongoServices.create(thing).map(_ => {
      Await.result(makeThings, Duration.Inf)
      Ok(views.html.things(Thing.createThingForm, thingsList))
    })
  }


  def updateThing(id: String) = Action.async { implicit request: Request[AnyContent] =>
    Thing.createThingForm.bindFromRequest.fold({ formWithErrors =>
      Future.successful(BadRequest(views.html.updatething(thingsList, id, formWithErrors)))
    }, { thing =>
      mongoServices.updateThing(id, thingWithNoEmptyTags(thing)).map(_ => {
        Await.result(makeThings, Duration.Inf)
        Ok(views.html.things(Thing.createThingForm, thingsList))

      })
    })
  }

  def showUpdateForm(id: String) = Action { implicit request: Request[AnyContent] =>
    Await.result(makeThings, Duration.Inf)
    Ok(views.html.updatething(thingsList, id, Thing.createThingForm))
  }

  def showThingForm = Action { implicit request: Request[AnyContent] =>
    Await.result(makeThings, Duration.Inf)
    Ok(views.html.things(Thing.createThingForm, thingsList))
  }

  def showSearchForm = Action { implicit request: Request[AnyContent] =>
    Await.result(makeThings, Duration.Inf)
    Ok(views.html.search(Thing.createSearchForm, thingsList))
  }

  def thingWithNoEmptyTags(thing: Thing) = {
    thing.tags = thing.tags.filter(el => el.nonEmpty)
    thing
  }

  def addToDb() = Future {

    (1 to 50).toList.foreach(el => {
      val listTags: ListBuffer[String] = new ListBuffer[String]
      (1 to Random.nextInt(4)).toList.foreach(_ => {
        listTags += TestDataCreator.tags(Random.nextInt(TestDataCreator.tags.length))
      }
      )

      collection.flatMap(_.insert.one(
        Thing(TestDataCreator.names(Random.nextInt(TestDataCreator.names.length)), TestDataCreator.prices(Random.nextInt(TestDataCreator.prices.length)), listTags.toList)
      )
      )
    })
  }

  def makeAddToDb: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Await.result(addToDb(), Duration.Inf)
    Ok("Added items!")
  }

  def submitForm: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    Thing.createThingForm.bindFromRequest.fold({ formWithErrors =>
      println(formWithErrors)
      Future.successful(BadRequest(views.html.things(formWithErrors, thingsList)))
    }, { thing =>
      collection.flatMap(_.insert.one(thingWithNoEmptyTags(thing))).map(_ => {
        Await.result(makeThings, Duration.Inf)
        Ok(views.html.things(Thing.createThingForm, thingsList))
      }
      )
    })
  }

  def deleteThing(id: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    mongoServices.deleteThing(id).map(_ => {
      Await.result(makeThings, Duration.Inf)
      Ok(views.html.things(Thing.createThingForm, thingsList))
    })
  }

  def submitSearchForm: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    Thing.createSearchForm.bindFromRequest.fold({ formWithErrors =>
      Future.successful(BadRequest(views.html.search(formWithErrors, thingsList)))
    }, { search =>
      mongoServices.getThings(getOrNothing(filterGetterGetter(search.filter, search.search))).map { things =>
        thingsList = things
        Ok(views.html.search(Thing.createSearchForm.fill(search), thingsList))
      }
    })
  }

  def getThings(filter: Option[(String, Json.JsValueWrapper)] = None): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    mongoServices.getThings(getOrNothing(filter)).map { things =>
      thingsList = things
      Ok(views.html.justThings(thingsList))
    }
  }

  def getOrNothing(filter: Option[(String, Json.JsValueWrapper)]) = {
    if (filter.isDefined) {
      Json.obj(filter.get)
    } else {
      Json.obj()
    }
  }

  def filterGetterGetter(filtered: String, value: String) = {
    if (filtered == "price") {
      try {
        filterGetter(filtered, BigDecimal(value))
      } catch {
        case _: Throwable => None
      }
    }
    else {
      if (value.nonEmpty) filterGetter(filtered, value)
      else None
    }
  }

  def filterGetter(filtered: String, value: Any) = {
    value match {
      case decimal: BigDecimal => getBigDecWithFilter(filtered, decimal)
      case string: String => getThingsWithFilter(filtered, string)
    }

  }

  def getBigDecWithFilter(filtered: String, value: BigDecimal) = {
    Some((filtered, Json.toJsFieldJsValueWrapper(value)))
  }

  def getThingsWithFilter(filtered: String, value: String) = {
    Some((filtered, Json.toJsFieldJsValueWrapper(value)))
  }


}
