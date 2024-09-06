package de.dnpm.dip.rest.util


import java.time.{
  LocalDate,
  LocalDateTime
}
import java.time.format.DateTimeFormatter.{
  ISO_LOCAL_DATE,
  ISO_LOCAL_DATE_TIME
}


abstract class Extractor[S,T]
{
  self =>

  def unapply(s: S): Option[T]

  def map[U](f: T => U): Extractor[S,U] =
    self.unapply(_).map(f)
}

object Extractor
{

  import scala.language.implicitConversions


  def apply[S,T](
    f: S => T
  ): Extractor[S,T] =
    new Extractor[S,T]{
      override def unapply(s: S): Option[T] =
        Some(f(s))
    }


  implicit def unlift[S,T](f: S => Option[T]): Extractor[S,T] =
    new Extractor[S,T]{
      override def unapply(s: S): Option[T] = f(s)
    }


  implicit lazy val isoDate: Extractor[String,LocalDate] =
    Extractor(
      LocalDate.parse(_,ISO_LOCAL_DATE)
    )

  implicit lazy val isoDateTime: Extractor[String,LocalDateTime] =
    Extractor(
      LocalDateTime.parse(_,ISO_LOCAL_DATE_TIME)
    )

  implicit def option[T](
    implicit ext: Extractor[String,T]
  ): Extractor[Option[String],Option[T]] =
    Extractor(
      _.map { case ext(t) => t }
    )


  def csv[T](del: String)(
    implicit ext: Extractor[String,T]
  ): Extractor[String,Seq[T]] =
    Extractor(
      _.split(del)
       .toSeq
       .map { case ext(t) => t }
    )

  def csv[T](
    implicit ext: Extractor[String,T]
  ): Extractor[String,Seq[T]] =
    csv(",")

  def csvSet[T](del: String)(
    implicit ext: Extractor[String,T]
  ): Extractor[String,Set[T]] =
    csv[T](del).map(_.toSet)

  def csvSet[T](
    implicit ext: Extractor[String,T]
  ): Extractor[String,Set[T]] =
   csvSet(",")


  def seq[T](
    implicit ext: Extractor[String,T]
  ): Extractor[Seq[String],Seq[T]] =
    Extractor(
      _.map { case ext(t) => t }
    )

  def set[T](
    implicit ext: Extractor[String,T]
  ): Extractor[Seq[String],Set[T]] =
    seq[T].map(_.toSet)

}
