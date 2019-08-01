package io.scalaland.ocdquery.internal

import shapeless._

trait Untupler[L <: HList] {
  type In
  def apply(in: In): L
}

object Untupler {
  type Aux[L <: HList, In0] = Untupler[L] { type In = In0 }

  private def instance[L <: HList, In0](f: In0 => L): Aux[L, In0] = new Untupler[L] {
    type In = In0
    def apply(in: In0): L = f(in)
  }

  // scalastyle:off
  implicit val untupler0:       Aux[HNil, Unit]             = instance { case ()     => HNil }
  implicit def untupler1[A]:    Aux[A :: HNil, A]           = instance { case a      => a :: HNil }
  implicit def untupler2[A, B]: Aux[A :: B :: HNil, (A, B)] = instance { case (a, b) => a :: b :: HNil }
  implicit def untupler3[A, B, C]: Aux[A :: B :: C :: HNil, (A, B, C)] = instance {
    case (a, b, c) => a :: b :: c :: HNil
  }
  implicit def untupler4[A, B, C, D]: Aux[A :: B :: C :: D :: HNil, (A, B, C, D)] =
    instance { case (a, b, c, d) => a :: b :: c :: d :: HNil }

  implicit def untupler5[A, B, C, D, E]: Aux[A :: B :: C :: D :: E :: HNil, (A, B, C, D, E)] =
    instance { case (a, b, c, d, e) => a :: b :: c :: d :: e :: HNil }

  implicit def untupler6[A, B, C, D, E, F]: Aux[A :: B :: C :: D :: E :: F :: HNil, (A, B, C, D, E, F)] =
    instance { case (a, b, c, d, e, f) => a :: b :: c :: d :: e :: f :: HNil }

  implicit def untupler7[A, B, C, D, E, F, G]: Aux[A :: B :: C :: D :: E :: F :: G :: HNil, (A, B, C, D, E, F, G)] =
    instance { case (a, b, c, d, e, f, g) => a :: b :: c :: d :: e :: f :: g :: HNil }

  implicit def untupler8[A, B, C, D, E, F, G, H]: Aux[A :: B :: C :: D :: E :: F :: G :: H :: HNil,
                                                      (A, B, C, D, E, F, G, H)] =
    instance { case (a, b, c, d, e, f, g, h) => a :: b :: c :: d :: e :: f :: g :: h :: HNil }

  implicit def untupler9[A, B, C, D, E, F, G, H, I]: Aux[A :: B :: C :: D :: E :: F :: G :: H :: I :: HNil,
                                                         (A, B, C, D, E, F, G, H, I)] =
    instance { case (a, b, c, d, e, f, g, h, i) => a :: b :: c :: d :: e :: f :: g :: h :: i :: HNil }

  implicit def untupler10[A, B, C, D, E, F, G, H, I, J]: Aux[A :: B :: C :: D :: E :: F :: G :: H :: I :: J :: HNil,
                                                             (A, B, C, D, E, F, G, H, I, J)] =
    instance { case (a, b, c, d, e, f, g, h, i, j) => a :: b :: c :: d :: e :: f :: g :: h :: i :: j :: HNil }

  implicit def untupler11[A, B, C, D, E, F, G, H, I, J, K]: Aux[
    A :: B :: C :: D :: E :: F :: G :: H :: I :: J :: K :: HNil,
    (A, B, C, D, E, F, G, H, I, J, K)
  ] =
    instance { case (a, b, c, d, e, f, g, h, i, j, k) => a :: b :: c :: d :: e :: f :: g :: h :: i :: j :: k :: HNil }

  implicit def untupler12[A, B, C, D, E, F, G, H, I, J, K, L]: Aux[
    A :: B :: C :: D :: E :: F :: G :: H :: I :: J :: K :: L :: HNil,
    (A, B, C, D, E, F, G, H, I, J, K, L)
  ] =
    instance {
      case (a, b, c, d, e, f, g, h, i, j, k, l) => a :: b :: c :: d :: e :: f :: g :: h :: i :: j :: k :: l :: HNil
    }

  implicit def untupler13[A, B, C, D, E, F, G, H, I, J, K, L, M]: Aux[
    A :: B :: C :: D :: E :: F :: G :: H :: I :: J :: K :: L :: M :: HNil,
    (A, B, C, D, E, F, G, H, I, J, K, L, M)
  ] =
    instance {
      case (a, b, c, d, e, f, g, h, i, j, k, l, m) =>
        a :: b :: c :: d :: e :: f :: g :: h :: i :: j :: k :: l :: m :: HNil
    }

  implicit def untupler14[A, B, C, D, E, F, G, H, I, J, K, L, M, N]: Aux[
    A :: B :: C :: D :: E :: F :: G :: H :: I :: J :: K :: L :: M :: N :: HNil,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N)
  ] =
    instance {
      case (a, b, c, d, e, f, g, h, i, j, k, l, m, n) =>
        a :: b :: c :: d :: e :: f :: g :: h :: i :: j :: k :: l :: m :: n :: HNil
    }

  implicit def untupler15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]: Aux[
    A :: B :: C :: D :: E :: F :: G :: H :: I :: J :: K :: L :: M :: N :: O :: HNil,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O)
  ] =
    instance {
      case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) =>
        a :: b :: c :: d :: e :: f :: g :: h :: i :: j :: k :: l :: m :: n :: o :: HNil
    }

  implicit def untupler16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]: Aux[
    A :: B :: C :: D :: E :: F :: G :: H :: I :: J :: K :: L :: M :: N :: O :: P :: HNil,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P)
  ] =
    instance {
      case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) =>
        a :: b :: c :: d :: e :: f :: g :: h :: i :: j :: k :: l :: m :: n :: o :: p :: HNil
    }

  implicit def untupler17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]: Aux[
    A :: B :: C :: D :: E :: F :: G :: H :: I :: J :: K :: L :: M :: N :: O :: P :: Q :: HNil,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q)
  ] =
    instance {
      case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q) =>
        a :: b :: c :: d :: e :: f :: g :: h :: i :: j :: k :: l :: m :: n :: o :: p :: q :: HNil
    }

  implicit def untupler18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]: Aux[
    A :: B :: C :: D :: E :: F :: G :: H :: I :: J :: K :: L :: M :: N :: O :: P :: Q :: R :: HNil,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R)
  ] =
    instance {
      case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r) =>
        a :: b :: c :: d :: e :: f :: g :: h :: i :: j :: k :: l :: m :: n :: o :: p :: q :: r :: HNil
    }

  implicit def untupler19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]: Aux[
    A :: B :: C :: D :: E :: F :: G :: H :: I :: J :: K :: L :: M :: N :: O :: P :: Q :: R :: S :: HNil,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S)
  ] =
    instance {
      case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s) =>
        a :: b :: c :: d :: e :: f :: g :: h :: i :: j :: k :: l :: m :: n :: o :: p :: q :: r :: s :: HNil
    }

  implicit def untupler20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]: Aux[
    A :: B :: C :: D :: E :: F :: G :: H :: I :: J :: K :: L :: M :: N :: O :: P :: Q :: R :: S :: T :: HNil,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T)
  ] =
    instance {
      case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t) =>
        a :: b :: c :: d :: e :: f :: g :: h :: i :: j :: k :: l :: m :: n :: o :: p :: q :: r :: s :: t :: HNil
    }

  implicit def untupler21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]: Aux[
    A :: B :: C :: D :: E :: F :: G :: H :: I :: J :: K :: L :: M :: N :: O :: P :: Q :: R :: S :: T :: U :: HNil,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U)
  ] =
    instance {
      case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u) =>
        a :: b :: c :: d :: e :: f :: g :: h :: i :: j :: k :: l :: m :: n :: o :: p :: q :: r :: s :: t :: u :: HNil
    }

  implicit def untupler22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]: Aux[
    A :: B :: C :: D :: E :: F :: G :: H :: I :: J :: K :: L :: M :: N :: O :: P :: Q :: R :: S :: T :: U :: V :: HNil,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V)
  ] =
    instance {
      case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v) =>
        a :: b :: c :: d :: e :: f :: g :: h :: i :: j :: k :: l :: m :: n :: o :: p :: q :: r :: s :: t :: u :: v :: HNil
    }
  // scalastyle:on
}
