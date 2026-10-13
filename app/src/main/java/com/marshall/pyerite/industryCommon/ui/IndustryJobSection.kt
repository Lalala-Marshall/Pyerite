package com.marshall.pyerite.industryCommon.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R
import com.marshall.pyerite.industryCommon.model.IndustryBucket
import com.marshall.pyerite.industryCommon.model.IndustryJob
import com.marshall.pyerite.localization.ContentLanguage
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.util.NumberDisplayFormatter

@Composable
internal fun IndustryJobSection(
    title: String,
    jobs: List<IndustryJob>,
    bucket: IndustryBucket,
    language: ContentLanguage,
    nowMs: Long,
    onOpenType: (Int) -> Unit,
    showInstaller: Boolean = true,
) {
    BaseContainer(
        title = title,
        titleTrailingContent = {
            Text(
                text = stringResource(
                    R.string.corporation_industry_section_count,
                    integerCount(jobs.size),
                ),
                color = colorResource(R.color.hint_text),
                fontSize = dimensionResource(R.dimen.list_section_subheader_text_size).value.sp,
            )
        },
        useSystemBarsPadding = false,
        modifier = Modifier.padding(top = dimensionResource(R.dimen.type_detail_section_gap)),
    ) {
        jobs.forEachIndexed { index, job ->
            IndustryJobRow(
                job = job,
                bucket = bucket,
                language = language,
                nowMs = nowMs,
                showInstaller = showInstaller,
                onClick = { onOpenType(job.detailTypeId) },
            )
            if (index < jobs.lastIndex) {
                HorizontalDivider(
                    thickness = dimensionResource(R.dimen.detail_divider_thickness),
                    color = colorResource(R.color.border),
                )
            }
        }
    }
}

private fun integerCount(count: Int): String =
    NumberDisplayFormatter.format(count.toLong(), NumberDisplayFormatter.Style.FULL)
