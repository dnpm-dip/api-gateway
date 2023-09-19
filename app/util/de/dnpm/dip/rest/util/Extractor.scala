package de.dnpm.dip.rest.util



abstract class Extractor[S,T](
  f: S => T
)
{
  final def unapply(s: S): Option[T] =
    Some(f(s))
}
