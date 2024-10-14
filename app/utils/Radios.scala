package utils

import play.api.data.Field
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

object Radios {
  def yesNo(
             field: Field
           )(implicit messages: Messages): Seq[RadioItem] = {

    Seq(
      RadioItem(
        id = Some(field.id),
        value = Some("true"),
        content = Text(messages("site.yes"))
      ),
      RadioItem(
        id = Some(s"${field.id}-no"),
        value = Some("false"),
        content = Text(messages("site.no"))
      )
    )
  }
}
