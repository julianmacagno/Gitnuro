package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.domain.extensions.removeLineDelimiters
import com.jetpackduba.gitnuro.domain.models.LineType
import com.jetpackduba.gitnuro.domain.models.SelectedLineText

object LineTextEditor {
    fun remove(fileLineList: List<String>, selectedLineTextList: List<SelectedLineText>): List<String> {
        val resultList = fileLineList.toMutableList()
        val emptiedIndexSet = mutableSetOf<Int>()

        selectedLineTextList.forEach { (line, start, end) ->
            check(line.lineType == LineType.ADDED) { "Only added lines can be edited" }

            val index = line.newLineNumber
            val fileLine = resultList[index]
            val body = fileLine.removeLineDelimiters()

            check(body == line.text.removeLineDelimiters()) { "The file changed since the diff was loaded" }

            val newBody = body.removeRange(start, end)

            if (newBody.isEmpty()) {
                emptiedIndexSet += index
            } else {
                resultList[index] = newBody + fileLine.removePrefix(body)
            }
        }

        return resultList.filterIndexed { index, _ -> index !in emptiedIndexSet }
    }
}
