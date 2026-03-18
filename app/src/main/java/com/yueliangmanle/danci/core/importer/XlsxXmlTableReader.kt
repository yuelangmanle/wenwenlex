package com.yueliangmanle.danci.core.importer

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

data class XlsxSheetData(
    val name: String,
    val rows: List<List<String>>,
)

class XlsxXmlTableReader {
    fun readWorkbook(inputStream: InputStream): List<XlsxSheetData> {
        val entries = unzipEntries(inputStream)
        val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"])
        val relationships = parseWorkbookRelationships(entries["xl/_rels/workbook.xml.rels"])
        val workbook = parseXml(entries.getValue("xl/workbook.xml"))
        val sheets = workbook.getElementsByTagName("sheet")

        return List(sheets.length) { index ->
            val sheet = sheets.item(index) as Element
            val relationId = sheet.getAttribute("r:id")
            val target = relationships.getValue(relationId)
            val path = normalizeSheetPath(target)
            XlsxSheetData(
                name = sheet.getAttribute("name").ifBlank { "Sheet${index + 1}" },
                rows = parseSheetRows(entries.getValue(path), sharedStrings),
            )
        }
    }

    private fun unzipEntries(inputStream: InputStream): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
                zip.closeEntry()
            }
        }
        return entries
    }

    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null) return emptyList()
        val document = parseXml(bytes)
        val nodes = document.getElementsByTagName("si")
        return List(nodes.length) { index ->
            val item = nodes.item(index) as Element
            val textNodes = item.getElementsByTagName("t")
            buildString {
                for (textIndex in 0 until textNodes.length) {
                    append(textNodes.item(textIndex).textContent.orEmpty())
                }
            }
        }
    }

    private fun parseWorkbookRelationships(bytes: ByteArray?): Map<String, String> {
        if (bytes == null) return emptyMap()
        val document = parseXml(bytes)
        val nodes = document.getElementsByTagName("Relationship")
        return buildMap {
            for (index in 0 until nodes.length) {
                val relation = nodes.item(index) as Element
                put(relation.getAttribute("Id"), relation.getAttribute("Target"))
            }
        }
    }

    private fun parseSheetRows(
        bytes: ByteArray,
        sharedStrings: List<String>,
    ): List<List<String>> {
        val document = parseXml(bytes)
        val rowNodes = document.getElementsByTagName("row")
        return buildList {
            for (rowIndex in 0 until rowNodes.length) {
                val row = rowNodes.item(rowIndex) as Element
                val cellNodes = row.getElementsByTagName("c")
                val values = mutableMapOf<Int, String>()
                var maxColumn = -1
                for (cellIndex in 0 until cellNodes.length) {
                    val cell = cellNodes.item(cellIndex) as Element
                    val columnIndex = cellReferenceToIndex(cell.getAttribute("r"))
                    maxColumn = maxOf(maxColumn, columnIndex)
                    values[columnIndex] = extractCellValue(cell, sharedStrings)
                }
                if (maxColumn < 0) continue
                add(List(maxColumn + 1) { index -> values[index].orEmpty() })
            }
        }
    }

    private fun extractCellValue(
        cell: Element,
        sharedStrings: List<String>,
    ): String {
        return when (cell.getAttribute("t")) {
            "s" -> {
                val index = cell.getElementsByTagName("v").item(0)?.textContent?.toIntOrNull() ?: return ""
                sharedStrings.getOrNull(index).orEmpty()
            }
            "inlineStr" -> cell.getElementsByTagName("t").item(0)?.textContent.orEmpty()
            else -> cell.getElementsByTagName("v").item(0)?.textContent.orEmpty()
        }.trim()
    }

    private fun cellReferenceToIndex(reference: String): Int {
        val letters = reference.takeWhile(Char::isLetter).uppercase()
        if (letters.isBlank()) return 0
        var result = 0
        letters.forEach { char ->
            result = result * 26 + (char.code - 'A'.code + 1)
        }
        return result - 1
    }

    private fun normalizeSheetPath(target: String): String {
        val trimmed = target.removePrefix("/")
        return if (trimmed.startsWith("xl/")) trimmed else "xl/$trimmed"
    }

    private fun parseXml(bytes: ByteArray) =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
        }.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
}
