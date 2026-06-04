package com.bookingcare.backend_bookingcare.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rule-based intent matcher cho chatbot ClinicBooking.
 *
 * <p>Cách hoạt động:
 * <ol>
 *   <li>Chuẩn hoá input: lowercase + bỏ dấu tiếng Việt + bỏ dấu câu.</li>
 *   <li>Quét từng intent theo thứ tự ưu tiên. Mỗi intent có nhiều "phrase" —
 *       intent nào match nhiều phrase nhất sẽ thắng (giải quyết câu mơ hồ).</li>
 *   <li>Detect câu phủ định ("không muốn đặt") để chuyển intent sang FALLBACK
 *       hoặc trả lời phù hợp.</li>
 *   <li>Mỗi intent có 2-3 câu trả lời mẫu — random pick để không lặp y hệt.</li>
 * </ol>
 *
 * <p>Đây là rule engine, KHÔNG phải LLM. Phù hợp phạm vi đồ án (offline,
 * không phụ thuộc API key). Hướng nâng cấp: tích hợp LLM với intent map này
 * làm fallback / safety net.
 */
@Component
public class ChatbotIntentMatcher {

    /** Câu trả lời + danh sách gợi ý nhanh hiển thị dưới message. */
    public record Reply(String intent, String content, List<String> suggestions) {}

    private static final List<String> DEFAULT_SUGGESTIONS = List.of(
            "Đặt lịch khám",
            "Lịch của tôi",
            "Giờ làm việc",
            "Hotline phòng khám"
    );

    /** Từ chỉ phủ định — nếu xuất hiện gần keyword thì giảm match score. */
    private static final List<String> NEGATIONS = Arrays.asList(
            "khong", "ko", "k can", "k muon", "khong muon", "khong can",
            "da", "roi", "khong phai"
    );

    /** Map intent → danh sách phrase (đã chuẩn hoá). Thứ tự = ưu tiên khi tie. */
    private static final Map<String, List<String>> INTENT_KEYWORDS = new LinkedHashMap<>();

    static {
        // ===== Intent ưu tiên cao (xử lý nguy cấp/an toàn trước) =====
        INTENT_KEYWORDS.put("EMERGENCY", Arrays.asList(
                "cap cuu", "khan cap", "nguy hiem", "kho tho", "dau tim",
                "tai nan", "chay mau", "ngat xiu", "co giat", "ngo doc"));

        INTENT_KEYWORDS.put("MEDICAL_ADVICE", Arrays.asList(
                "trieu chung", "bi benh gi", "co bi", "uong thuoc gi", "thuoc nao",
                "lieu luong", "co nguy hiem", "dieu tri the nao", "chua benh",
                "ke don", "don thuoc", "co the chua"));

        // ===== Intent chính về lịch hẹn =====
        INTENT_KEYWORDS.put("CANCEL", Arrays.asList(
                "huy lich", "huy hen", "huy dat", "cancel", "khong di kham nua",
                "bo lich", "huy phieu kham", "khong den kham"));

        INTENT_KEYWORDS.put("APPOINTMENT_LIST", Arrays.asList(
                "lich cua toi", "lich cua minh", "lich da dat", "phieu kham",
                "lich sap toi", "xem lich", "tra cuu lich", "lich su kham",
                "lich gan day"));

        INTENT_KEYWORDS.put("RESCHEDULE", Arrays.asList(
                "doi gio", "doi lich", "doi ngay", "thay doi lich", "doi sang ngay",
                "chuyen sang", "thay doi gio kham"));

        INTENT_KEYWORDS.put("CHECKIN", Arrays.asList(
                "check in", "check-in", "checkin", "den phong kham", "den kham",
                "den tan noi", "nhan phieu", "lay so thu tu"));

        INTENT_KEYWORDS.put("BOOKING", Arrays.asList(
                "dat lich", "dang ky kham", "hen kham", "muon kham", "kham benh",
                "book", "booking", "dat hen", "dang ky", "kham bac si",
                "muon di kham", "the nao de kham"));

        // ===== Intent về phòng khám =====
        INTENT_KEYWORDS.put("WORKING_HOURS", Arrays.asList(
                "gio lam", "gio mo cua", "may gio", "thoi gian lam viec",
                "lam viec luc may gio", "thu may", "ngay nao", "mo cua khi nao",
                "mo cua tu may gio", "lam viec ngay nao"));

        INTENT_KEYWORDS.put("SPECIALTY", Arrays.asList(
                "chuyen khoa", "khoa nao", "kham gi", "co khoa", "danh sach khoa",
                "co bao nhieu khoa", "phong kham co nhung khoa", "linh vuc kham"));

        INTENT_KEYWORDS.put("DOCTOR", Arrays.asList(
                "bac si", "bs", "tim bac si", "ai kham", "bac si nao tot",
                "bac si chuyen", "danh sach bac si", "bac si gioi"));

        INTENT_KEYWORDS.put("HOTLINE", Arrays.asList(
                "hotline", "lien he", "so dien thoai", "sdt", "duong day nong",
                "dia chi", "phong kham o dau", "phong kham nam o", "tim duong",
                "ban do", "vi tri"));

        INTENT_KEYWORDS.put("ABOUT_CLINIC", Arrays.asList(
                "phong kham la gi", "gioi thieu phong kham", "thong tin phong kham",
                "phong kham quyet tien", "co bao nhieu bac si"));

        // ===== Intent về tài khoản & thanh toán =====
        INTENT_KEYWORDS.put("PASSWORD", Arrays.asList(
                "quen mat khau", "mat khau", "password", "khoi phuc tai khoan",
                "doi mat khau", "lay lai mat khau", "khong dang nhap duoc"));

        INTENT_KEYWORDS.put("PAYMENT", Arrays.asList(
                "thanh toan", "tra tien", "phi kham", "gia kham", "hoa don",
                "bao nhieu tien", "chi phi", "le phi"));

        INTENT_KEYWORDS.put("INSURANCE", Arrays.asList(
                "bao hiem", "bhyt", "bao hiem y te", "bhxh"));

        // ===== Social =====
        INTENT_KEYWORDS.put("GREETING", Arrays.asList(
                "xin chao", "chao ban", "chao em", "hello", "hi ", "hi,", "alo"));

        INTENT_KEYWORDS.put("THANKS", Arrays.asList(
                "cam on", "thanks", "thank you", "tks", "tnx", "cam on em",
                "cam on ban", "ok cam on"));

        INTENT_KEYWORDS.put("BYE", Arrays.asList(
                "tam biet", "bye", "goodbye", "het roi", "het thac mac",
                "het cau hoi"));

        INTENT_KEYWORDS.put("HELP", Arrays.asList(
                "tro giup", "giup toi", "huong dan", "lam sao", "the nao",
                "help"));
    }

    /** Welcome khi tạo session mới. */
    public Reply welcomeReply() {
        return new Reply(
                "WELCOME",
                "Xin chào! Tôi là Trợ lý ảo của ClinicBooking — Phòng khám Quyết Tiến. "
                        + "Em có thể hỗ trợ về đặt lịch, hủy lịch, tra cứu chuyên khoa, bác sĩ, giờ làm việc...\n\n"
                        + "Anh/chị cần em hỗ trợ thông tin gì hôm nay?",
                DEFAULT_SUGGESTIONS
        );
    }

    /** Match input → trả về reply tương ứng. Không bao giờ trả null. */
    public Reply match(String input) {
        if (input == null || input.isBlank()) {
            return defaultReply();
        }
        String normalized = normalize(input);

        // 1. Tính score cho từng intent (số phrase match được).
        String bestIntent = null;
        int bestScore = 0;
        for (Map.Entry<String, List<String>> e : INTENT_KEYWORDS.entrySet()) {
            int score = 0;
            for (String kw : e.getValue()) {
                if (normalized.contains(kw)) score++;
            }
            // 2. Trừ điểm nếu câu có phủ định gần keyword (ngắn gọn: chỉ check sự
            //    xuất hiện chung — đủ tốt cho rule-based tiếng Việt cơ bản).
            if (score > 0 && hasNegation(normalized)) {
                score--;
            }
            if (score > bestScore) {
                bestScore = score;
                bestIntent = e.getKey();
            }
        }

        if (bestIntent == null || bestScore <= 0) {
            return defaultReply();
        }
        return responseFor(bestIntent);
    }

    private static boolean hasNegation(String normalized) {
        for (String n : NEGATIONS) {
            if (normalized.contains(n)) return true;
        }
        return false;
    }

    /** Bỏ dấu tiếng Việt + lowercase + chuẩn hoá khoảng trắng. */
    private static String normalize(String s) {
        if (s == null) return "";
        String lower = s.toLowerCase(Locale.ROOT);
        String noAccent = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace("đ", "d");
        return noAccent.replaceAll("\\s+", " ").trim();
    }

    /** Random pick 1 phần tử từ list — để câu trả lời không lặp y hệt mỗi lần. */
    private static String pick(String... options) {
        if (options.length == 0) return "";
        if (options.length == 1) return options[0];
        return options[ThreadLocalRandom.current().nextInt(options.length)];
    }

    private Reply responseFor(String intent) {
        return switch (intent) {

            case "EMERGENCY" -> new Reply(intent,
                    "⚠ Trường hợp **cấp cứu**, vui lòng gọi ngay:\n"
                            + "• 115 — Cấp cứu y tế\n"
                            + "• Hotline phòng khám: 0247 300 8899\n\n"
                            + "Em không thể tư vấn cho tình huống nguy cấp. "
                            + "Anh/chị nên đến cơ sở y tế gần nhất hoặc gọi 115 ngay lập tức.",
                    List.of("Hotline phòng khám", "Giờ làm việc"));

            case "MEDICAL_ADVICE" -> new Reply(intent,
                    "Em là trợ lý ảo, không phải bác sĩ — em **không thể tư vấn về triệu chứng, "
                            + "thuốc hoặc phác đồ điều trị**. Vui lòng đặt lịch với bác sĩ chuyên khoa "
                            + "phù hợp để được khám và tư vấn chính xác.\n\n"
                            + "Anh/chị có muốn em hướng dẫn cách đặt lịch không?",
                    List.of("Đặt lịch khám", "Chuyên khoa nào?", "Hotline phòng khám"));

            case "GREETING" -> new Reply(intent,
                    pick(
                            "Xin chào anh/chị! Em có thể hỗ trợ về đặt lịch khám, chuyên khoa, "
                                    + "giờ làm việc và liên hệ phòng khám. Anh/chị cần thông tin gì ạ?",
                            "Chào anh/chị 👋 Em là trợ lý ảo của ClinicBooking. "
                                    + "Hôm nay em có thể giúp gì cho anh/chị?",
                            "Xin chào! Rất vui được phục vụ anh/chị. Em hỗ trợ những thông tin về "
                                    + "đặt lịch, bác sĩ, chuyên khoa và quy trình khám tại phòng khám."),
                    DEFAULT_SUGGESTIONS);

            case "BOOKING" -> new Reply(intent,
                    pick(
                            "Để đặt lịch khám tại ClinicBooking, anh/chị vui lòng:\n"
                                    + "1. Mở tab Trang chủ → chọn chuyên khoa cần khám\n"
                                    + "2. Chọn bác sĩ và khung giờ trống\n"
                                    + "3. Xác nhận thông tin và gửi yêu cầu\n\n"
                                    + "Lịch sẽ ở trạng thái Chờ xác nhận; bác sĩ sẽ duyệt sớm. "
                                    + "Lưu ý: nên đặt trước giờ khám tối thiểu 30 phút.",
                            "Anh/chị có thể đặt lịch theo 3 bước:\n"
                                    + "📋 Bước 1 — Chọn chuyên khoa hoặc bác sĩ ở Trang chủ\n"
                                    + "🕐 Bước 2 — Chọn ngày và khung giờ còn trống\n"
                                    + "✅ Bước 3 — Xác nhận thông tin\n\n"
                                    + "Lịch sẽ chuyển sang trạng thái Đã xác nhận sau khi bác sĩ duyệt."),
                    List.of("Lịch của tôi", "Hủy lịch hẹn", "Chuyên khoa nào?"));

            case "CANCEL" -> new Reply(intent,
                    "Để hủy lịch, anh/chị vào tab **Lịch hẹn** → chọn phiếu khám cần hủy → bấm Hủy.\n\n"
                            + "Lưu ý:\n"
                            + "• Chỉ hủy được khi lịch ở trạng thái Chờ xác nhận hoặc Đã xác nhận\n"
                            + "• Sau khi check-in thì không thể tự hủy — vui lòng liên hệ hotline.",
                    List.of("Lịch của tôi", "Đặt lịch khám", "Hotline phòng khám"));

            case "RESCHEDULE" -> new Reply(intent,
                    "Hiện tại tính năng tự đổi giờ trên ứng dụng đang được phát triển. "
                            + "Trước mắt anh/chị có thể:\n"
                            + "• Hủy lịch cũ và đặt lịch mới với khung giờ mong muốn\n"
                            + "• Hoặc liên hệ hotline 0247 300 8899 để được nhân viên hỗ trợ đổi giờ.",
                    List.of("Hủy lịch hẹn", "Đặt lịch khám", "Hotline phòng khám"));

            case "CHECKIN" -> new Reply(intent,
                    "Khi đến phòng khám đúng ngày hẹn, anh/chị mở tab **Lịch hẹn** → chọn phiếu khám "
                            + "→ bấm **Check-in**. Sau đó vui lòng đợi bác sĩ gọi tên theo số thứ tự.\n\n"
                            + "Tip: nên đến trước giờ hẹn 10-15 phút để chuẩn bị giấy tờ.",
                    List.of("Đặt lịch khám", "Lịch của tôi", "Giờ làm việc"));

            case "APPOINTMENT_LIST" -> new Reply(intent,
                    "Anh/chị có thể xem toàn bộ lịch khám của mình ở tab **Lịch hẹn**:\n"
                            + "• Tab **Sắp tới** — các lịch chờ duyệt và đã xác nhận\n"
                            + "• Tab **Đã khám** — lịch sử khám hoàn thành\n"
                            + "• Tab **Đã hủy** — lịch đã bị hủy hoặc no-show",
                    List.of("Đặt lịch khám", "Hủy lịch hẹn", "Check-in"));

            case "WORKING_HOURS" -> new Reply(intent,
                    "🕐 **Giờ làm việc Phòng khám đa khoa Quyết Tiến:**\n\n"
                            + "• Thứ 2 - Thứ 6\n"
                            + "  - Sáng: 7:00 - 11:30\n"
                            + "  - Chiều: 13:30 - 17:30\n\n"
                            + "• Thứ 7, Chủ nhật và ngày lễ: nghỉ\n\n"
                            + "Anh/chị nên đặt lịch trước để được phục vụ nhanh nhất.",
                    List.of("Đặt lịch khám", "Chuyên khoa nào?", "Hotline phòng khám"));

            case "SPECIALTY" -> new Reply(intent,
                    "🏥 **ClinicBooking hiện có 12 chuyên khoa:**\n\n"
                            + "• Nội tổng quát   • Nhi khoa\n"
                            + "• Tai mũi họng   • Da liễu\n"
                            + "• Tim mạch       • Sản phụ khoa\n"
                            + "• Mắt            • Răng hàm mặt\n"
                            + "• Cơ xương khớp  • Tiêu hoá\n"
                            + "• Nội tiết       • Thần kinh\n\n"
                            + "Mở tab **Trang chủ** để xem danh sách bác sĩ theo từng chuyên khoa.",
                    List.of("Đặt lịch khám", "Tìm bác sĩ", "Giờ làm việc"));

            case "DOCTOR" -> new Reply(intent,
                    "Anh/chị có thể tra cứu bác sĩ theo 2 cách:\n\n"
                            + "👉 **Cách 1 — Theo chuyên khoa:** Trang chủ → chọn chuyên khoa → "
                            + "danh sách bác sĩ trong khoa đó.\n\n"
                            + "👉 **Cách 2 — Trực tiếp:** Bấm xem tất cả bác sĩ ở Trang chủ.\n\n"
                            + "Mỗi bác sĩ có hồ sơ chi tiết: chuyên khoa, giờ làm việc, lịch trống "
                            + "và nút Đặt lịch ngay.",
                    List.of("Đặt lịch khám", "Chuyên khoa nào?", "Giờ làm việc"));

            case "HOTLINE" -> new Reply(intent,
                    "📞 **Liên hệ Phòng khám đa khoa Quyết Tiến:**\n\n"
                            + "• Hotline: **0247 300 8899**\n"
                            + "• Email: lienhe@quyettien.vn\n"
                            + "• Địa chỉ: 25 P. Láng Hạ, Đống Đa, Hà Nội\n\n"
                            + "Phòng khám có chỗ đỗ xe miễn phí cho khách.",
                    List.of("Giờ làm việc", "Đặt lịch khám", "Chuyên khoa nào?"));

            case "ABOUT_CLINIC" -> new Reply(intent,
                    "🏥 **Phòng khám đa khoa Quyết Tiến** là cơ sở y tế tư nhân tại Hà Nội, "
                            + "phục vụ khám chữa bệnh ngoại trú với 12 chuyên khoa và hơn 40 bác sĩ "
                            + "có chứng chỉ hành nghề.\n\n"
                            + "Phòng khám tích hợp ứng dụng **ClinicBooking** giúp bệnh nhân đặt lịch "
                            + "trực tuyến, theo dõi lịch hẹn và nhận thông báo dễ dàng.",
                    List.of("Đặt lịch khám", "Chuyên khoa nào?", "Hotline phòng khám"));

            case "PASSWORD" -> new Reply(intent,
                    "🔑 **Đặt lại mật khẩu:**\n\n"
                            + "1. Mở màn hình Đăng nhập → bấm **Quên mật khẩu**\n"
                            + "2. Nhập email đã đăng ký để nhận mã OTP\n"
                            + "3. Nhập OTP và đặt mật khẩu mới (≥ 6 ký tự)\n\n"
                            + "Nếu vẫn không đăng nhập được, vui lòng liên hệ hotline để được hỗ trợ.",
                    List.of("Hotline phòng khám", "Đặt lịch khám"));

            case "PAYMENT" -> new Reply(intent,
                    "💵 **Phí khám:** Hiện thanh toán **trực tiếp tại quầy thu ngân** "
                            + "khi anh/chị đến phòng khám check-in.\n\n"
                            + "Phí khám tham khảo:\n"
                            + "• Khám chuyên khoa: từ 150.000đ / lượt\n"
                            + "• Khám tổng quát: từ 200.000đ / lượt\n\n"
                            + "Phiên bản tới của ứng dụng sẽ tích hợp thanh toán online qua VNPay/MoMo.",
                    List.of("Đặt lịch khám", "Bảo hiểm y tế", "Hotline phòng khám"));

            case "INSURANCE" -> new Reply(intent,
                    "Hiện tại Phòng khám Quyết Tiến **chưa kết nối trực tiếp** với hệ thống Bảo hiểm Y tế. "
                            + "Anh/chị vui lòng thanh toán phí khám tại quầy và lấy hoá đơn để làm thủ tục "
                            + "thanh toán BHYT theo quy định.\n\n"
                            + "Để biết chi tiết, vui lòng liên hệ hotline 0247 300 8899.",
                    List.of("Phí khám", "Đặt lịch khám", "Hotline phòng khám"));

            case "HELP" -> new Reply(intent,
                    "Em có thể hỗ trợ các chủ đề sau:\n\n"
                            + "• 📅 Đặt lịch / hủy lịch / xem lịch của tôi\n"
                            + "• 🏥 Thông tin chuyên khoa, bác sĩ\n"
                            + "• 🕐 Giờ làm việc, địa chỉ phòng khám\n"
                            + "• 🔑 Quên mật khẩu, phí khám, BHYT\n\n"
                            + "Anh/chị chọn một chủ đề bên dưới hoặc hỏi cụ thể nhé.",
                    DEFAULT_SUGGESTIONS);

            case "THANKS" -> new Reply(intent,
                    pick(
                            "Rất vui được hỗ trợ anh/chị! Nếu cần thêm thông tin, em luôn sẵn sàng 🙂",
                            "Không có gì ạ! Chúc anh/chị nhiều sức khoẻ.",
                            "Cảm ơn anh/chị đã sử dụng ClinicBooking 🌟 Em sẵn sàng nếu có câu hỏi tiếp theo."),
                    DEFAULT_SUGGESTIONS);

            case "BYE" -> new Reply(intent,
                    pick(
                            "Tạm biệt anh/chị! Chúc anh/chị một ngày khoẻ mạnh 🌿",
                            "Cảm ơn anh/chị! Hẹn gặp lại tại phòng khám.",
                            "Chào anh/chị, hẹn gặp lại lần sau ạ 👋"),
                    List.of("Đặt lịch khám", "Lịch của tôi"));

            default -> defaultReply();
        };
    }

    private Reply defaultReply() {
        return new Reply(
                "FALLBACK",
                pick(
                        "Em chưa hiểu rõ câu hỏi của anh/chị 🤔 Anh/chị có thể chọn một trong các chủ đề "
                                + "bên dưới, hoặc đặt câu hỏi cụ thể hơn về đặt lịch, bác sĩ hoặc phòng khám.",
                        "Xin lỗi, em chưa nắm được ý của anh/chị. Anh/chị muốn em hỗ trợ chủ đề nào?\n\n"
                                + "Em hỗ trợ tốt nhất cho: đặt lịch, hủy lịch, chuyên khoa, giờ làm việc.",
                        "Em chưa có thông tin để trả lời câu này. Anh/chị thử bấm **Trợ giúp** để xem "
                                + "danh sách chủ đề em hỗ trợ, hoặc gọi hotline 0247 300 8899 để gặp nhân viên."),
                List.of("Trợ giúp", "Đặt lịch khám", "Hotline phòng khám")
        );
    }
}
