import cats.Id
import io.scalaland.ocdquery._
import shapeless.Generic

package object example {

  val SqlEntityRepo: Repo.EntityRepo[SqlEntityF] = {
    implicit val read: doobie.Read[Repo.ForEntity[SqlEntityF]#Entity] = QuasiAuto.read(Generic[SqlEntityF[Id, Id]])
    Repo.forEntity[SqlEntityF]("sql_entity".tableName, DefaultColumnNames.forEntity[SqlEntityF])
  }

  val SqlValueRepo: Repo.ValueRepo[SqlValueF] = {
    implicit val read: doobie.Read[Repo.ForValue[SqlValueF]#Value] = QuasiAuto.read(Generic[SqlValueF[Id]])
    Repo.forValue[SqlValueF]("sql_value".tableName, DefaultColumnNames.forValue[SqlValueF])
  }
}
