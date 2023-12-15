package com.example.filterbadwodslibrary

import opennlp.tools.stemmer.PorterStemmer
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel

class filterBadwords {


    private val tokenizer: TokenizerME
    private val stemmer: PorterStemmer
    private val badWords: List<String>

    init {
        // Khởi tạo tokenizer
        val modelIn = this.javaClass.classLoader?.getResourceAsStream("assets/en-token.bin")
        val model = TokenizerModel(modelIn)
        tokenizer = TokenizerME(model)

        // Khởi tạo stemmer
        stemmer = PorterStemmer()

        // Danh sách từ xấu (cần mở rộng theo nhu cầu)
       badWords = listOf(
            "buồi", "buoi", "dau buoi", "daubuoi", "caidaubuoi", "nhucaidaubuoi", "dau boi", "bòi", "dauboi", "caidauboi",
            "đầu bòy", "đầu bùi", "dau boy", "dauboy", "caidauboy", "b`", "cặc", "cak", "kak", "kac", "cac", "concak", "nungcak",
            "bucak", "caiconcac", "caiconcak", "cu", "cặk", "cak", "dái", "giái", "zái", "kiu", "cứt", "cuccut", "cutcut", "cứk",
            "cuk", "cười ỉa", "cười ẻ", "đéo", "đếch", "đếk", "dek", "đết", "đệt", "đách", "dech", "đ", "deo", "d", "đel", "đél",
            "del", "dell ngửi", "dell ngui", "dell chịu", "dell chiu", "dell hiểu", "dell hieu", "dellhieukieugi", "dell nói",
            "dell noi", "dellnoinhieu", "dell biết", "dell biet", "dell nghe", "dell ăn", "dell an", "dell được", "dm", "dell duoc",
            "dell làm", "dell lam", "dell đi", "dell di", "dell chạy", "dell chay", "deohieukieugi", "địt", "đm", "đmm",
            "dmm", "đmmm", "dmmm", "đmmmm", "dmmmm", "ditme", "ditmemay","concac", "đmmmmm", "dmmmmm", "đcm", "dcm", "đcmm", "dcmm", "đcmmm", "dcmmm", "đcmmmm",
            "dcmmmm", "đệch", "đệt", "dit", "dis", "diz", "đjt", "djt", "địt mẹ", "địt mịe", "địt má", "địt mía", "địt ba", "địt bà",
            "địt cha", "địt con", "địt bố", "địt cụ", "dis me", "disme", "dismje", "dismia", "dis mia", "dis mie", "đis mía", "đis mịa",
            "ditmemayconcho", "ditmemay", "ditmethangoccho", "ditmeconcho", "dmconcho", "dmcs", "ditmecondi", "ditmecondicho", "đụ",
            "duma","đụ mẹ", "đụ mịa", "đụ mịe", "đụ má", "đụ cha", "đụ bà", "đú cha", "đú con mẹ", "đú má", "đú mẹ", "đù cha", "đù má", "đù mẹ",
            "đù mịe", "đù mịa", "đủ cha", "đủ má", "đủ mẹ", "đủ mé", "đủ mía", "đủ mịa", "đủ mịe", "đủ mie", "đủ mia", "đìu", "đờ mờ", "đê mờ",
            "đờ ma ma", "đờ mama", "đê mama", "đề mama", "đê ma ma", "đề ma ma", "dou", "doma", "duoma", "dou má", "duo má", "dou ma", "đou má",
            "đìu má", "á đù", "á đìu", "đậu mẹ", "đậu má", "đĩ", "di~", "đuỹ", "điếm", "cđĩ", "cdi~", "đilol", "điloz", "đilon", "diloz", "dilol",
            "dilon", "condi", "condi~", "dime", "di me", "dimemay", "condime", "condimay", "condimemay", "con di cho", "con di cho", "condicho",
            "bitch", "biz", "bít chi", "con bích", "con bic", "con bíc", "con bít", "phò", "4`", "lồn", "l`", "loz", "lìn", "nulo", "ml", "matlon",
            "cailon", "matlol", "matloz", "thml", "thangmatlon", "thangml", "đỗn lì", "tml", "thml", "diml", "dml", "hãm", "xàm lol", "xam lol", "xạo lol",
            "xao lol", "con lol", "ăn lol", "an lol", "mát lol", "mat lol", "cái lol", "cai lol", "lòi lol", "loi lol", "ham lol", "củ lol", "cu lol", "ngu lol",
            "tuổi lol", "tuoi lol", "mõm lol", "mồm lol", "mom lol", "như lol", "nhu lol", "nứng lol", "nung lol", "nug lol", "nuglol", "rảnh lol", "ranh lol",
            "đách lol", "dach lol", "mu lol", "banh lol", "tét lol", "tet lol", "vạch lol", "vach lol", "cào lol", "cao lol", "tung lol", "mặt lol", "mát lol",
            "mat lol", "xàm lon", "xam lon", "xạo lon", "xao lon", "con lon", "ăn lon", "an lon", "mát lon", "mat lon", "cái lon", "cai lon", "lòi lon", "loi lon",
            "ham lon", "củ lon", "cu lon", "ngu lon", "tuổi lon", "tuoi lon", "mõm lon", "mồm lon", "mom lon", "như lon", "nhu lon", "nứng lon", "nung lon", "nug lon",
            "nuglon", "rảnh lon", "ranh lon", "đách lon", "dach lon", "mu lon", "banh lon", "tét lon", "tet lon", "vạch lon", "vach lon", "cào lon", "cao lon", "tung lon",
            "mặt lon", "mát lon", "mat lon", "cái lờ", "cl", "clgt", "cờ lờ gờ tờ", "cái lề gì thốn", "đốn cửa lòng", "sml", "sapmatlol", "sapmatlon", "sapmatloz",
            "sấp mặt", "sap mat", "vlon", "vloz", "vlol", "vailon", "vai lon", "vai lol", "vailol", "nốn lừng", "vcl", "vl", "vleu", "chịch", "chich", "vãi", "v~", "đụ",
            "nứng", "nug", "đút đít", "chổng mông", "banh háng", "xéo háng", "xhct", "xephinh", "la liếm", "đổ vỏ", "xoạc", "xoac", "chich choac", "húp sò", "fuck", "fck",
            "đụ", "bỏ bú", "buscu", "ngu", "óc chó", "occho", "lao cho", "láo chó", "bố láo", "chó má", "cờ hó", "sảng", "thằng chó", "thang cho", "thang cho", "chó điên",
            "thằng điên", "thang dien", "đồ điên", "sủa bậy", "sủa tiếp", "sủa đi", "sủa càn", "mẹ bà", "mẹ cha mày", "me cha may", "mẹ cha anh", "mẹ cha nhà anh", "mẹ cha nhà mày",
            "me cha nha may", "mả cha mày", "mả cha nhà mày", "ma cha may", "ma cha nha may", "mả mẹ", "mả cha", "kệ mẹ", "kệ mịe", "kệ mịa", "kệ mje", "kệ mja", "ke me", "ke mie", "ke mia",
            "ke mja", "ke mje", "bỏ mẹ", "bỏ mịa", "bỏ mịe", "bỏ mja", "bỏ mje", "bo me", "bo mia", "bo mie", "bo mje", "bo mja", "chetme", "chet me", "chết mẹ", "chết mịa", "chết mja",
            "chết mịe", "chết mie", "chet mia", "chet mie", "chet mja", "chet mje", "thấy mẹ", "thấy mịe", "thấy mịa", "thay me", "thay mie", "thay mia", "tổ cha", "bà cha mày", "cmn", "cmnl",
            "tiên sư nhà mày", "tiên sư bố", "tổ sư","dick")
    }

    fun removeSpecialCharactersInWords(input: String): String? {
        // Loại bỏ các ký tự đặc biệt giữa các chữ cái
        return input.replace("(\\p{L})[.,~`'^](\\p{L})".toRegex(), "$1$2")
    }
    fun filterBadWords(text: String?): String? {
        if (text != null) {
            val tokens = tokenizer.tokenize(text)
            var processedText = text

            tokens.forEach { token ->
                // Loại bỏ các ký tự đặc biệt từ token
                val cleanedToken = removeSpecialCharactersInWords(token)

                // Chuyển token đã làm sạch về chữ thường và stem
                val stemmedCleanedToken = stemmer.stem(cleanedToken!!.toLowerCase())

                // Kiểm tra nếu token là từ xấu
                if (badWords.contains(stemmedCleanedToken)) {
                    // Thay thế token gốc trong văn bản bằng dấu '*'
                    processedText = processedText!!.replace(Regex("\\b$token\\b", RegexOption.IGNORE_CASE), "*".repeat(token.length))
                }
            }
            return processedText
        } else {
            return null
        }
    }
}