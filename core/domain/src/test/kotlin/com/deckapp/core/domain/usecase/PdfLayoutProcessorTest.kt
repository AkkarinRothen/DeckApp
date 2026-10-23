package com.deckapp.core.domain.usecase

import android.net.Uri
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.model.CardContentMode
import com.deckapp.core.model.CardFace
import com.deckapp.core.model.PdfLayoutMode
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfLayoutProcessorTest {

    private val fileRepository = mockk<FileRepository>()
    private val processor = PdfLayoutProcessor(fileRepository)
    private val dummyUri = mockk<Uri>()

    @Test
    fun `process GRID mode respects startPage and endPage range`() = runTest {
        coEvery { fileRepository.getPdfPageCount(dummyUri) } returns 10
        coEvery {
            fileRepository.renderPdfGridCellAndSave(
                uri = dummyUri,
                pageIndex = any(),
                col = any(),
                row = any(),
                totalCols = any(),
                totalRows = any(),
                deckId = 1L,
                fileName = any(),
                pageRenderWidth = any(),
                autoTrimCell = any(),
                horizontalSplitRatio = any()
            )
        } answers {
            val page = it.invocation.args[1] as Int
            val col = it.invocation.args[2] as Int
            val row = it.invocation.args[3] as Int
            "/img/page_${page}_r${row}_c${col}.jpg"
        }

        val extractedCards = mutableListOf<String>()

        // Procesar páginas 2 a 3 (índices 2 y 3, total 2 páginas) con grilla 2x2 = 8 cartas
        processor.process(
            pdfUri = dummyUri,
            deckId = 1L,
            contentMode = CardContentMode.IMAGE_ONLY,
            layoutMode = PdfLayoutMode.GRID,
            gridCols = 2,
            gridRows = 2,
            startPage = 2,
            endPage = 3,
            autoTrimCells = true,
            splitRatio = 0.5f,
            onProgress = { _, _ -> },
            saveCard = { frontPath, _, _ ->
                extractedCards.add(frontPath)
            }
        )

        assertEquals(8, extractedCards.size)
        assertEquals("/img/page_2_r0_c0.jpg", extractedCards.first())
        assertEquals("/img/page_3_r1_c1.jpg", extractedCards.last())
    }

    @Test
    fun `process ALTERNATING_PAGES respects page range and binds front and back`() = runTest {
        coEvery { fileRepository.getPdfPageCount(dummyUri) } returns 10
        coEvery {
            fileRepository.renderPdfGridCellAndSave(
                uri = dummyUri,
                pageIndex = any(),
                col = any(),
                row = any(),
                totalCols = any(),
                totalRows = any(),
                deckId = 1L,
                fileName = any(),
                pageRenderWidth = any(),
                autoTrimCell = any(),
                horizontalSplitRatio = any()
            )
        } answers {
            val page = it.invocation.args[1] as Int
            "/img/page_${page}.jpg"
        }

        val cards = mutableListOf<Pair<String, List<CardFace>>>()

        // Rango 0 a 3: Páginas 0 (frente) + 1 (dorso); Páginas 2 (frente) + 3 (dorso) -> 2 cartas para 1x1
        processor.process(
            pdfUri = dummyUri,
            deckId = 1L,
            contentMode = CardContentMode.DOUBLE_SIDED_FULL,
            layoutMode = PdfLayoutMode.ALTERNATING_PAGES,
            gridCols = 1,
            gridRows = 1,
            startPage = 0,
            endPage = 3,
            autoTrimCells = true,
            splitRatio = 0.5f,
            onProgress = { _, _ -> },
            saveCard = { front, backs, _ ->
                cards.add(front to backs)
            }
        )

        assertEquals(2, cards.size)
        assertEquals("/img/page_0.jpg", cards[0].first)
        assertEquals("/img/page_1.jpg", cards[0].second.first().imagePath)
        assertEquals("/img/page_2.jpg", cards[1].first)
        assertEquals("/img/page_3.jpg", cards[1].second.first().imagePath)
    }

    @Test
    fun `process when startPage is greater than endPage extracts zero cards safely`() = runTest {
        coEvery { fileRepository.getPdfPageCount(dummyUri) } returns 10

        var extractedCount = 0
        processor.process(
            pdfUri = dummyUri,
            deckId = 1L,
            contentMode = CardContentMode.IMAGE_ONLY,
            layoutMode = PdfLayoutMode.GRID,
            gridCols = 1,
            gridRows = 1,
            startPage = 5,
            endPage = 2,
            autoTrimCells = true,
            splitRatio = 0.5f,
            onProgress = { _, _ -> },
            saveCard = { _, _, _ -> extractedCount++ }
        )

        assertEquals(0, extractedCount)
    }

    @Test
    fun `process ALTERNATING_PAGES with 2 columns mirrors back column for duplex alignment`() = runTest {
        coEvery { fileRepository.getPdfPageCount(dummyUri) } returns 2
        coEvery {
            fileRepository.renderPdfGridCellAndSave(
                uri = dummyUri,
                pageIndex = any(),
                col = any(),
                row = any(),
                totalCols = any(),
                totalRows = any(),
                deckId = 1L,
                fileName = any(),
                pageRenderWidth = any(),
                autoTrimCell = any(),
                horizontalSplitRatio = any()
            )
        } answers {
            val page = it.invocation.args[1] as Int
            val col = it.invocation.args[2] as Int
            val row = it.invocation.args[3] as Int
            "/img/page_${page}_r${row}_c${col}.jpg"
        }

        val cards = mutableListOf<Pair<String, List<CardFace>>>()

        // Page 0 (front) and Page 1 (back), 2 cols x 1 row -> 2 cards
        // col 0 front -> col 1 back (mirrored)
        // col 1 front -> col 0 back (mirrored)
        processor.process(
            pdfUri = dummyUri,
            deckId = 1L,
            contentMode = CardContentMode.DOUBLE_SIDED_FULL,
            layoutMode = PdfLayoutMode.ALTERNATING_PAGES,
            gridCols = 2,
            gridRows = 1,
            startPage = 0,
            endPage = 1,
            autoTrimCells = true,
            splitRatio = 0.5f,
            onProgress = { _, _ -> },
            saveCard = { front, backs, _ ->
                cards.add(front to backs)
            }
        )

        assertEquals(2, cards.size)
        // First card: front col 0, back col 1
        assertEquals("/img/page_0_r0_c0.jpg", cards[0].first)
        assertEquals("/img/page_1_r0_c1.jpg", cards[0].second.first().imagePath)
        // Second card: front col 1, back col 0
        assertEquals("/img/page_0_r0_c1.jpg", cards[1].first)
        assertEquals("/img/page_1_r0_c0.jpg", cards[1].second.first().imagePath)
    }
}

