package de.dnpm.dip.rest.util


import java.net.URI
import scala.util.Try
import de.dnpm.dip.coding.{
  Code,
  Coding,
  CodeSystem
}
import cats.syntax.traverse._
import shapeless.Coproduct


abstract class Extractor[S,T]
{
  def unapply(s: S): Option[T]
}

object Extractor
{

  import scala.language.implicitConversions

  implicit def unlift[S,T](f: S => Option[T]): Extractor[S,T] =
    new Extractor[S,T]{
      override def unapply(s: S): Option[T] = f(s)
    }


  def apply[S,T](
    f: S => T
  ): Extractor[S,T] =
    new Extractor[S,T]{
      override def unapply(s: S): Option[T] =
        Some(f(s))
    }

  def optional[S,T](f: S => T): Extractor[Option[S],Option[T]] =
    apply((opt: Option[S]) => opt.map(f))

}



/*
abstract class CodingExtractor[T] extends Extractor[String,Coding[T]]
{
  self =>

  def set: Extractor[String,Set[Coding[T]]] =
    Extractor(
      _.split( ",")
       .collect { case self(coding) => coding }
       .toSet
    )

}
*/


object CodingExtractor
{
  import scala.collection.Factory


  final class Builder[T](
    val one: Extractor[String,Coding[T]]
  ){

    def many[C[_], F <: Factory[Coding[T],C[Coding[T]]]](
      fac: F
    ): Extractor[String,C[Coding[T]]] =
      Extractor(
        _.split(",")
         .collect { case one(coding) => coding }
         .to(fac)
      )

    lazy val set: Extractor[Seq[String],Set[Coding[T]]] =
      Extractor(
        _.collect { case one(coding) => coding }
         .toSet
      )

    def nested[C](ext: Extractor[String,C]): Extractor[Seq[String],Set[C]] =
      Extractor(
        _.collect { case ext(c) => c }
         .toSet
      )

/*
    lazy val nestedSets: Extractor[Seq[String],Set[Set[Coding[T]]]] =
      Extractor(
        _.map(_ split ",")
         .map(
           _.collect { case one(coding) => coding }
            .toSet
         )
         .toSet
      )
*/    
  }


  def apply[T](
    implicit cs: Coding.System[T]
  ): Builder[T] =
    new Builder[T](
      Extractor {
        s =>
          val csv = s split "\\|"

          Coding[T](
            Code(csv(0)),
            None,
            cs.uri,
            Try(csv(2)).toOption
          )
      }
    )
 

  def on[T <: Coproduct](
    implicit uris: Coding.System.UriSet[T]
  ): Builder[T] =
    new Builder[T](
      s => {
        val csv = s split "\\|"

        {
          for {
            code <- Try(csv(0))
            uri  <- Try(csv(1)).map(URI.create)
            if (uris.values contains uri)
            version = Try(csv(2)).toOption
          } yield Coding[T](
            Code(code),
            None,
            uri,
            version
          )
        }
        .toOption
      }
    )


/*  
  sealed trait Builder[T]
  {
    def one: Extractor[String,Coding[T]]

    def set: Extractor[Seq[String],Set[Coding[T]]]

    def nestedSets: Extractor[Seq[String],Set[Set[Coding[T]]]]
  }


  def apply[T](
    implicit cs: Coding.System[T]
  ): Builder[T] =
    new FixedSystemBuilder[T](cs.uri)
 

  def on[T <: Coproduct](
    implicit uris: Coding.System.UriSet[T]
  ): Builder[T] =
    new MultiSystemBuilder[T](uris)
 



  private class FixedSystemBuilder[T](
    uri: URI
  )
  extends Builder[T]
  {

    private def toCoding(
      csv: Array[String]
    ): Coding[T] =
      Coding[T](
        Code(csv(0)),
        None,
        uri,
        Try(csv(2)).toOption
      )
  
  
    override def one: Extractor[String,Coding[T]] =
      Extractor(
        psv => toCoding(psv.split("\\|"))
      )
  
    override def set: Extractor[Seq[String],Set[Coding[T]]] =
      Extractor(
        _.map(_ split "\\|")
         .map(toCoding(_))
         .toSet
      )
  
    override def nestedSets: Extractor[Seq[String],Set[Set[Coding[T]]]] =
      Extractor(
        _.map(_ split ",")
         .map(
           _.toList
            .map(_ split "\\|")
            .map(toCoding(_))
            .toSet
         )
         .toSet
      )

  }


  private class MultiSystemBuilder[T <: Coproduct](
    uris: Coding.System.UriSet[T]
  )
  extends Builder[T]
  {

    private def toCoding(
      csv: Array[String]
    ): Option[Coding[T]] =
      {
        for {
          code <- Try(csv(0))
          uri  <- Try(csv(1)).map(URI.create)
          if (uris.values contains uri)
          version = Try(csv(2)).toOption
        } yield Coding[T](
          Code(code),
          None,
          uri,
          version
        )
      }
      .toOption
  

    override def one: Extractor[String,Coding[T]] =
      psv => toCoding(psv split "\\|")
  
  
    override def set: Extractor[Seq[String],Set[Coding[T]]] =
      _.map(_ split "\\|")
       .map(toCoding(_))
       .sequence
       .map(_.toSet)
  
    override def nestedSets: Extractor[Seq[String],Set[Set[Coding[T]]]] =
      _.map(_ split ",")
       .map(
         _.toList
          .map(_ split "\\|")
          .map(toCoding(_))
          .sequence
          .map(_.toSet)
       )
       .sequence
       .map(_.toSet)
      
  }
*/

}
