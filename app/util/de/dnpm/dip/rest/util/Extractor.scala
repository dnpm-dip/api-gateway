package de.dnpm.dip.rest.util


/*
abstract class Extractor[S,T](
  f: S => T
)
{
  final def unapply(s: S): Option[T] =
    Some(f(s))
}
*/


abstract class Extractor[S,T]
{
  def unapply(s: S): Option[T]
}


object Extractor
{

  def apply[S,T](
    f: S => T
  ): Extractor[S,T] =
    new Extractor[S,T]{
      override def unapply(s: S): Option[T] =
        Some(f(s))
    }

}

