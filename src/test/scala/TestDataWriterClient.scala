import io.gatling.core.result.message.Status
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.session.{GroupBlock, Session}

class TestDataWriterClient extends DataWriterClient{
  override def writeRequestData(session: Session, requestName: String, requestStartDate: Long,
                                requestEndDate: Long, responseStartDate: Long, responseEndDate: Long,
                                status: Status, message: Option[String], extraInfo: List[Any]): Unit = {}

  override def writeGroupData(session: Session, group: GroupBlock, exitDate: Long): Unit = {}
}
