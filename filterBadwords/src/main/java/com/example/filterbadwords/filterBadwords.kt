package com.example.filterbadwords
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel
import opennlp.tools.stemmer.PorterStemmer
class filterBadwords {
    private val tokenizer: TokenizerME
    private val stemmer: PorterStemmer
    private val badWords: List<String>

    init {
        // Khởi tạo tokenizer
        val modelIn = this.javaClass.getResourceAsStream("/en-token.bin")
        val model = TokenizerModel(modelIn)
        tokenizer = TokenizerME(model)

        // Khởi tạo stemmer
        stemmer = PorterStemmer()

        // Danh sách từ xấu (cần mở rộng theo nhu cầu)
        badWords = listOf("badword1", "badword2", "badword3")
    }

    fun filterBadWords(text: String): String {
        // Tokenize và chuyển về chữ thường
        val tokens = tokenizer.tokenize(text)
        val lowercaseTokens = tokens.map { it.toLowerCase() }

        // Stemming các từ
        val stemmedTokens = lowercaseTokens.map { stemmer.stem(it) }

        // Thay thế từ xấu bằng '*'
        val filteredText = stemmedTokens.fold(text) { acc, token ->
            if (badWords.contains(token)) {
                acc.replace(token, "*".repeat(token.length), ignoreCase = true)
            } else {
                acc
            }
        }

        return filteredText
    }
}