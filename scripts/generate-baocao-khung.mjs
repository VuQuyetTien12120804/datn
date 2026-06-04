import fs from "fs";
import {
  Document,
  Packer,
  Paragraph,
  TextRun,
  HeadingLevel,
  AlignmentType,
  PageBreak,
  Table,
  TableRow,
  TableCell,
  WidthType,
  BorderStyle,
  TableOfContents,
  LevelFormat,
  Header,
  Footer,
  PageNumber,
  NumberFormat,
} from "docx";

const PLACEHOLDER = (text) =>
  new TextRun({ text, italics: true, color: "666666" });

const BOLD = (text) => new TextRun({ text, bold: true });
const NORMAL = (text) => new TextRun({ text });
const HINT = (text) =>
  new Paragraph({
    spacing: { after: 120 },
    children: [PLACEHOLDER(text)],
  });

const heading = (text, level) =>
  new Paragraph({ heading: level, children: [BOLD(text)] });

const para = (...runs) =>
  new Paragraph({ spacing: { after: 160 }, children: runs });

const bullet = (text) =>
  new Paragraph({
    spacing: { after: 80 },
    bullet: { level: 0 },
    children: [NORMAL(text)],
  });

const bullet2 = (text) =>
  new Paragraph({
    spacing: { after: 80 },
    bullet: { level: 1 },
    children: [NORMAL(text)],
  });

const emptyLine = () => new Paragraph({ children: [] });

function tableCell(text, opts = {}) {
  return new TableCell({
    width: opts.width ? { size: opts.width, type: WidthType.PERCENTAGE } : undefined,
    children: [
      new Paragraph({
        alignment: opts.center ? AlignmentType.CENTER : AlignmentType.LEFT,
        children: [opts.bold ? BOLD(text) : NORMAL(text)],
      }),
    ],
  });
}

function makeTable(headers, rows, colWidths) {
  const headerRow = new TableRow({
    children: headers.map((h, i) =>
      tableCell(h, { bold: true, center: true, width: colWidths?.[i] })
    ),
  });
  const dataRows = rows.map(
    (row) =>
      new TableRow({
        children: row.map((c, i) =>
          tableCell(c, { width: colWidths?.[i] })
        ),
      })
  );
  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    rows: [headerRow, ...dataRows],
  });
}

const doc = new Document({
  features: { updateFields: true },
  styles: {
    default: {
      document: {
        run: { font: "Times New Roman", size: 26 },
      },
    },
    paragraphStyles: [
      {
        id: "Heading1",
        name: "Heading 1",
        basedOn: "Normal",
        next: "Normal",
        quickFormat: true,
        run: { size: 28, bold: true, font: "Times New Roman" },
        paragraph: { spacing: { before: 240, after: 240 }, outlineLevel: 0 },
      },
      {
        id: "Heading2",
        name: "Heading 2",
        basedOn: "Normal",
        next: "Normal",
        quickFormat: true,
        run: { size: 26, bold: true, font: "Times New Roman" },
        paragraph: { spacing: { before: 200, after: 120 }, outlineLevel: 1 },
      },
      {
        id: "Heading3",
        name: "Heading 3",
        basedOn: "Normal",
        next: "Normal",
        quickFormat: true,
        run: { size: 26, bold: true, font: "Times New Roman" },
        paragraph: { spacing: { before: 160, after: 80 }, outlineLevel: 2 },
      },
    ],
  },
  numbering: {
    config: [
      {
        reference: "bullets",
        levels: [
          {
            level: 0,
            format: LevelFormat.BULLET,
            text: "•",
            alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 720, hanging: 360 } } },
          },
          {
            level: 1,
            format: LevelFormat.BULLET,
            text: "◦",
            alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 1080, hanging: 360 } } },
          },
        ],
      },
    ],
  },
  sections: [
    {
      properties: {
        page: {
          margin: { top: 1440, right: 1080, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [
            new Paragraph({
              alignment: AlignmentType.CENTER,
              children: [
                new TextRun({
                  text: "BÁO CÁO ĐỒ ÁN TỐT NGHIỆP – ClinicBooking",
                  size: 20,
                  italics: true,
                }),
              ],
            }),
          ],
        }),
      },
      footers: {
        default: new Footer({
          children: [
            new Paragraph({
              alignment: AlignmentType.CENTER,
              children: [
                new TextRun({ children: [PageNumber.CURRENT], size: 22 }),
              ],
            }),
          ],
        }),
      },
      children: [
        // ===== TRANG BÌA =====
        new Paragraph({ spacing: { before: 800 } }),
        para(
          BOLD("TRƯỜNG ĐẠI HỌC GIAO THÔNG VẬN TẢI"),
          new TextRun({ break: 1 }),
          BOLD("KHOA CÔNG NGHỆ THÔNG TIN")
        ),
        emptyLine(),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          spacing: { before: 600, after: 600 },
          children: [
            BOLD("BÁO CÁO ĐỒ ÁN TỐT NGHIỆP"),
            new TextRun({ break: 1 }),
            BOLD("ĐỀ TÀI:"),
            new TextRun({ break: 1 }),
            BOLD(
              "XÂY DỰNG ỨNG DỤNG ĐẶT LỊCH CHO PHÒNG KHÁM ĐA KHOA CÓ SỬ DỤNG AI HỖ TRỢ"
            ),
            new TextRun({ break: 1 }),
            NORMAL("(Hệ thống ClinicBooking – Phòng khám Quyết Tiến)"),
          ],
        }),
        emptyLine(),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [
            NORMAL("Ngành: Công nghệ Thông tin"),
            new TextRun({ break: 1 }),
            NORMAL("Lớp: KHMT – Khóa K63"),
          ],
        }),
        emptyLine(),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [
            NORMAL("Sinh viên thực hiện: "),
            BOLD("Vũ Quyết Tiến"),
            new TextRun({ break: 1 }),
            NORMAL("Mã sinh viên: "),
            BOLD("223630716"),
          ],
        }),
        emptyLine(),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [
            NORMAL("Giảng viên hướng dẫn: "),
            BOLD("TS. Nguyễn Đình Dương"),
          ],
        }),
        emptyLine(),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          spacing: { before: 800 },
          children: [NORMAL("Hà Nội, tháng …… năm 2026")],
        }),

        new Paragraph({ children: [new PageBreak()] }),

        // ===== LỜI CAM ĐOAN =====
        heading("LỜI CAM ĐOAN", HeadingLevel.HEADING_1),
        HINT(
          "[Viết lại lời cam đoan theo mẫu của Khoa. Cam kết công trình do chính bản thân thực hiện, không sao chép.]"
        ),
        para(
          NORMAL(
            "Em xin cam đoan báo cáo đồ án tốt nghiệp với đề tài “Xây dựng ứng dụng đặt lịch cho phòng khám đa khoa có sử dụng AI hỗ trợ” là công trình nghiên cứu do em thực hiện dưới sự hướng dẫn của TS. Nguyễn Đình Dương. Các số liệu, kết quả nêu trong báo cáo là trung thực; em chịu trách nhiệm về tính chính xác của nội dung."
          )
        ),
        emptyLine(),
        new Paragraph({
          alignment: AlignmentType.RIGHT,
          children: [
            NORMAL("Sinh viên thực hiện"),
            new TextRun({ break: 1, break: 1 }),
            BOLD("Vũ Quyết Tiến"),
          ],
        }),

        new Paragraph({ children: [new PageBreak()] }),

        // ===== LỜI CẢM ƠN =====
        heading("LỜI CẢM ƠN", HeadingLevel.HEADING_1),
        HINT(
          "[Viết 1–2 trang: cảm ơn Nhà trường, Khoa CNTT, Bộ môn KHMT, GVHD, gia đình, bạn bè đã hỗ trợ trong quá trình thực hiện đồ án.]"
        ),
        HINT("[Đoạn 1: Cảm ơn Nhà trường, Khoa, Bộ môn đã tạo điều kiện học tập.]"),
        HINT(
          "[Đoạn 2: Cảm ơn TS. Nguyễn Đình Dương – hướng dẫn phương pháp, góp ý kiến trúc, review báo cáo.]"
        ),
        HINT("[Đoạn 3: Cảm ơn gia đình, bạn đồng hành, đơn vị khảo sát (nếu có).]"),
        HINT("[Kết thúc bằng lời chúc và cam kết tiếp tục học tập.]"),

        new Paragraph({ children: [new PageBreak()] }),

        // ===== MỤC LỤC =====
        heading("MỤC LỤC", HeadingLevel.HEADING_1),
        HINT(
          "[Trong Word: References → Table of Contents → chọn mục lục tự động. Nhấn F9 để cập nhật sau khi hoàn thiện nội dung.]"
        ),
        new TableOfContents("Mục lục", {
          hyperlink: true,
          headingStyleRange: "1-3",
        }),

        new Paragraph({ children: [new PageBreak()] }),

        // ===== DANH MỤC VIẾT TẮT =====
        heading("DANH MỤC CÁC KÝ HIỆU, CHỮ VIẾT TẮT", HeadingLevel.HEADING_1),
        makeTable(
          ["Ký hiệu", "Giải thích"],
          [
            ["API", "Application Programming Interface – Giao diện lập trình ứng dụng"],
            ["JWT", "JSON Web Token – Chuẩn xác thực dạng token"],
            ["REST", "Representational State Transfer – Kiến trúc API web"],
            ["ERD", "Entity Relationship Diagram – Sơ đồ thực thể liên kết"],
            ["UML", "Unified Modeling Language – Ngôn ngữ mô hình hóa thống nhất"],
            ["UI/UX", "User Interface / User Experience – Giao diện & trải nghiệm người dùng"],
            ["CSKH", "Chăm sóc khách hàng"],
            ["BN", "Bệnh nhân"],
            ["BS", "Bác sĩ"],
            ["OTP", "One-Time Password – Mật khẩu dùng một lần"],
            ["CRUD", "Create, Read, Update, Delete – Các thao tác cơ bản trên dữ liệu"],
            ["SQL Server", "Hệ quản trị cơ sở dữ liệu quan hệ của Microsoft"],
            ["Spring Boot", "Framework Java cho ứng dụng backend"],
            ["React", "Thư viện JavaScript xây dựng giao diện web"],
          ],
          [25, 75]
        ),
        para(NORMAL("Bảng 1. Danh mục viết tắt")),

        new Paragraph({ children: [new PageBreak()] }),

        // ===== DANH MỤC HÌNH BẢNG =====
        heading("DANH MỤC HÌNH VẼ, BẢNG BIỂU, ĐỒ THỊ", HeadingLevel.HEADING_1),
        HINT(
          "[Sau khi chèn hình/bảng vào báo cáo, cập nhật danh sách dưới đây (hoặc dùng Insert Caption + Update field trong Word).]"
        ),
        heading("Danh mục hình vẽ (mẫu)", HeadingLevel.HEADING_2),
        bullet("Hình 1.1 – Sơ đồ tổng quan hệ thống ClinicBooking …… trang …"),
        bullet("Hình 2.1 – Biểu đồ Use Case tổng quát …… trang …"),
        bullet("Hình 2.2 – Biểu đồ Use Case bệnh nhân …… trang …"),
        bullet("Hình 2.3 – Biểu đồ Use Case bác sĩ …… trang …"),
        bullet("Hình 2.4 – Biểu đồ Use Case quản trị viên …… trang …"),
        bullet("Hình 2.5 – Biểu đồ Activity – Luồng đặt lịch khám …… trang …"),
        bullet("Hình 2.6 – Biểu đồ Activity – Luồng duyệt & khám bệnh …… trang …"),
        bullet("Hình 2.7 – Biểu đồ Sequence – Đặt lịch khám …… trang …"),
        bullet("Hình 2.8 – Biểu đồ Sequence – Đăng nhập JWT …… trang …"),
        bullet("Hình 2.9 – Sơ đồ kiến trúc Client–Server 3 tầng …… trang …"),
        bullet("Hình 2.10 – Sơ đồ ERD cơ sở dữ liệu …… trang …"),
        bullet("Hình 2.11 – Wireframe màn hình Android (bệnh nhân) …… trang …"),
        bullet("Hình 2.12 – Wireframe màn hình Web Admin …… trang …"),
        bullet("Hình 3.1 – Giao diện đăng nhập Android …… trang …"),
        bullet("Hình 3.2 – Giao diện đặt lịch 3 bước …… trang …"),
        bullet("Hình 3.3 – Giao diện quản lý lịch hẹn Web Admin …… trang …"),
        bullet("Hình 3.4 – Dashboard báo cáo Web Admin …… trang …"),
        heading("Danh mục bảng biểu (mẫu)", HeadingLevel.HEADING_2),
        bullet("Bảng 1.1 – So sánh phương thức đặt lịch truyền thống và trực tuyến …… trang …"),
        bullet("Bảng 1.2 – Danh sách yêu cầu chức năng theo vai trò …… trang …"),
        bullet("Bảng 1.3 – Yêu cầu phi chức năng …… trang …"),
        bullet("Bảng 2.1 – Bảng mô tả Use Case đặt lịch khám …… trang …"),
        bullet("Bảng 2.2 – Danh sách bảng cơ sở dữ liệu …… trang …"),
        bullet("Bảng 2.3 – Mô tả bảng appointments …… trang …"),
        bullet("Bảng 2.4 – Một số API tiêu biểu …… trang …"),
        bullet("Bảng 3.1 – Cấu hình môi trường phát triển …… trang …"),
        bullet("Bảng 3.2 – Tài khoản demo …… trang …"),
        bullet("Bảng 4.1 – Bảng test case chức năng …… trang …"),
        bullet("Bảng 4.2 – Tổng hợp kết quả kiểm thử …… trang …"),

        new Paragraph({ children: [new PageBreak()] }),

        // ===== LỜI MỞ ĐẦU =====
        heading("LỜI MỞ ĐẦU", HeadingLevel.HEADING_1),

        heading("1. Lý do chọn đề tài", HeadingLevel.HEADING_2),
        HINT(
          "[2–3 trang: bối cảnh chuyển đổi số y tế; hạn chế đặt lịch qua điện thoại/trực tiếp; nhu cầu phòng khám đa khoa; lý do chọn đề tài phù hợp ngành CNTT.]"
        ),
        HINT(
          "[Mở đầu về Cách mạng Công nghiệp 4.0 và chuyển đổi số trong lĩnh vực y tế. Thực trạng quá tải quầy tiếp nhận, khó quản lý lịch bác sĩ, bệnh nhân phải chờ đợi lâu…]"
        ),
        HINT(
          "[Giới thiệu phòng khám đa khoa là đối tượng áp dụng. Giải thích vì sao cần hệ thống đặt lịch tập trung kết nối bệnh nhân – bác sĩ – quản trị viên.]"
        ),

        heading("2. Mục tiêu của đồ án", HeadingLevel.HEADING_2),
        heading("2.1. Mục tiêu tổng quát", HeadingLevel.HEADING_3),
        HINT(
          "[Xây dựng hệ thống ClinicBooking gồm ứng dụng Android (bệnh nhân & bác sĩ), web quản trị và backend API, phục vụ quy trình đặt – duyệt – khám lịch hẹn tại phòng khám đa khoa.]"
        ),
        heading("2.2. Mục tiêu cụ thể", HeadingLevel.HEADING_3),
        bullet("Phân tích nghiệp vụ đặt lịch khám và mô hình hóa yêu cầu bằng UML."),
        bullet("Thiết kế cơ sở dữ liệu SQL Server và kiến trúc REST API Spring Boot."),
        bullet(
          "Xây dựng ứng dụng Android cho bệnh nhân: tra cứu bác sĩ, đặt/hủy/đổi lịch, thông báo, chat."
        ),
        bullet(
          "Xây dựng module bác sĩ trên Android: xem lịch, duyệt/từ chối, cập nhật trạng thái khám."
        ),
        bullet(
          "Xây dựng web admin React: quản lý bác sĩ, chuyên khoa, slot, lịch hẹn, báo cáo."
        ),
        bullet("Kiểm thử hệ thống và đánh giá kết quả đạt được."),

        heading("3. Phạm vi và giới hạn", HeadingLevel.HEADING_2),
        heading("3.1. Phạm vi thực hiện", HeadingLevel.HEADING_3),
        bullet("Một phòng khám đa khoa (single-clinic), triển khai nội bộ."),
        bullet("Ba nhóm người dùng: Bệnh nhân, Bác sĩ, Quản trị viên."),
        bullet(
          "Chức năng cốt lõi: đặt lịch, quản lý slot/capacity, duyệt lịch, check-in, khám, chat CSKH/BN-BS."
        ),
        bullet("Công nghệ: Android (Java), Spring Boot, React + TypeScript, SQL Server."),

        heading("3.2. Giới hạn / ngoài phạm vi", HeadingLevel.HEADING_3),
        bullet("Không tích hợp thanh toán trực tuyến."),
        bullet(
          "Không lưu trữ hồ sơ bệnh án điện tử đầy đủ; chỉ ghi chú lâm sàng cơ bản trong phiên khám."
        ),
        bullet("Không kết nối hệ thống bệnh viện/bảo hiểm y tế bên ngoài."),
        bullet(
          "Chatbot AI tư vấn tự động: [ghi rõ mức độ triển khai thực tế hoặc hướng phát triển]."
        ),

        heading("4. Phương pháp nghiên cứu", HeadingLevel.HEADING_2),
        bullet(
          "Khảo sát & thu thập yêu cầu: phỏng vấn/khảo sát quy trình phòng khám, đọc tài liệu nghiệp vụ."
        ),
        bullet(
          "Phân tích & thiết kế: Use Case, Activity, Sequence, ERD, kiến trúc 3 lớp."
        ),
        bullet("Phát triển lặp (Agile/iterative): xây backend → mobile → web admin."),
        bullet(
          "Kiểm thử: test case thủ công, kiểm thử API (Postman), kiểm thử luồng end-to-end."
        ),

        heading("5. Công cụ và môi trường", HeadingLevel.HEADING_2),
        makeTable(
          ["Công cụ", "Phiên bản / Ghi chú"],
          [
            ["IDE Backend", "IntelliJ IDEA"],
            ["IDE Android", "Android Studio"],
            ["IDE Web", "Visual Studio Code"],
            ["Backend", "Java 17+, Spring Boot 3.x"],
            ["Mobile", "Android SDK, Java"],
            ["Frontend Web", "React 18, TypeScript, Vite, Ant Design"],
            ["CSDL", "Microsoft SQL Server"],
            ["API Test", "Postman / curl"],
            ["Quản lý mã nguồn", "Git, GitHub"],
            ["Thiết kế UML", "Draw.io / StarUML / PlantUML"],
          ],
          [35, 65]
        ),
        para(NORMAL("Bảng 1.1. Công cụ phát triển")),

        heading("6. Cấu trúc báo cáo", HeadingLevel.HEADING_2),
        HINT(
          "[Giới thiệu ngắn 5 phần: Chương 1 – Cơ sở lý thuyết; Chương 2 – Phân tích thiết kế; Chương 3 – Cài đặt; Chương 4 – Kiểm thử; Kết luận.]"
        ),
        HINT(
          "[Gợi ý phân bổ ~80 trang: Mở đầu ~8 | Ch1 ~12 | Ch2 ~25 | Ch3 ~25 | Ch4 ~8 | Kết luận ~2]"
        ),

        new Paragraph({ children: [new PageBreak()] }),

        // ===== CHƯƠNG 1 =====
        heading("CHƯƠNG 1. CƠ SỞ LÝ THUYẾT", HeadingLevel.HEADING_1),
        HINT(
          "[Chương 1 dự kiến ~12 trang. Trình bày nền tảng lý thuyết và khảo sát liên quan trực tiếp đến bài toán đặt lịch phòng khám.]"
        ),

        heading("1.1. Tổng quan về đặt lịch khám và chuyển đổi số y tế", HeadingLevel.HEADING_2),
        HINT(
          "[Giới thiệu khái niệm đặt lịch khám trực tuyến (Online Appointment Scheduling). Vai trò trong cải thiện trải nghiệm bệnh nhân và hiệu quả vận hành phòng khám.]"
        ),
        makeTable(
          ["Tiêu chí", "Đặt lịch truyền thống", "Đặt lịch trực tuyến (ClinicBooking)"],
          [
            ["Thời gian thao tác", "[…]", "[…]"],
            ["Khả năng tra cứu slot trống", "[…]", "[…]"],
            ["Quản lý tập trung", "[…]", "[…]"],
            ["Thông báo nhắc lịch", "[…]", "[…]"],
            ["Minh bạch trạng thái", "[…]", "[…]"],
          ],
          [30, 35, 35]
        ),
        para(NORMAL("Bảng 1.2. So sánh phương thức đặt lịch")),
        HINT(
          "[Phân tích xu hướng telehealth, patient portal, mobile-first trong y tế tại Việt Nam.]"
        ),

        heading("1.2. Khảo sát hiện trạng và thu thập yêu cầu", HeadingLevel.HEADING_2),
        heading("1.2.1. Mô tả bài toán thực tế", HeadingLevel.HEADING_3),
        HINT(
          "[Mô tả quy trình hiện tại tại phòng khám đa khoa: tiếp nhận qua tổng đài, ghi sổ tay/Excel, phân công bác sĩ theo ca, khó kiểm soát trùng slot…]"
        ),
        heading("1.2.2. Đối tượng sử dụng và vai trò", HeadingLevel.HEADING_3),
        makeTable(
          ["Vai trò", "Nhu cầu chính", "Thiết bị sử dụng"],
          [
            ["Bệnh nhân", "Đặt/hủy/đổi lịch, tra cứu BS, nhận thông báo", "Android App"],
            ["Bác sĩ", "Xem lịch, duyệt lịch, cập nhật trạng thái khám, chat BN", "Android App"],
            ["Quản trị viên", "CRUD dữ liệu master, duyệt/điều chỉnh lịch, báo cáo", "Web Admin"],
          ],
          [20, 50, 30]
        ),
        para(NORMAL("Bảng 1.3. Đối tượng sử dụng hệ thống")),

        heading("1.2.3. Yêu cầu chức năng", HeadingLevel.HEADING_3),
        HINT("[Liệt kê đầy đủ các chức năng theo vai trò – tham chiếu đặc tả yêu cầu.]"),
        makeTable(
          ["STT", "Nhóm chức năng", "Chức năng", "Đối tượng"],
          [
            ["1", "Tài khoản", "Đăng nhập, đăng ký, quên mật khẩu", "BN, BS"],
            ["2", "Hồ sơ cá nhân", "Xem/cập nhật thông tin; xem/hủy lịch đã đặt", "BN"],
            ["3", "Tra cứu", "Xem danh sách bác sĩ, chuyên khoa", "BN, BS"],
            ["4", "Đặt lịch hẹn", "Gửi yêu cầu đặt lịch; hủy lịch", "BN"],
            ["5", "Quản lý lịch cá nhân", "Xem lịch, duyệt/từ chối, cập nhật trạng thái", "BS"],
            ["6", "Quản trị", "Quản lý BS, slot, lịch hẹn, tài khoản, báo cáo", "Admin"],
            ["[…]", "[…]", "[…]", "[…]"],
          ],
          [8, 22, 45, 25]
        ),
        para(NORMAL("Bảng 1.4. Danh sách yêu cầu chức năng")),

        heading("1.2.4. Yêu cầu phi chức năng", HeadingLevel.HEADING_3),
        makeTable(
          ["Nhóm", "Yêu cầu", "Mô tả"],
          [
            ["Hiệu năng", "Thời gian phản hồi", "≤ 2 giây cho thao tác thông thường"],
            ["Bảo mật", "Xác thực & phân quyền", "JWT, RBAC PATIENT/DOCTOR/ADMIN"],
            ["Tin cậy", "Toàn vẹn dữ liệu", "Transaction, stored procedure chống double booking"],
            ["Khả dụng", "Giao diện thân thiện", "Tiếng Việt, thao tác đặt lịch đơn giản"],
            ["Mở rộng", "Kiến trúc module", "REST API, tách client/server"],
            ["[…]", "[…]", "[…]"],
          ],
          [20, 30, 50]
        ),
        para(NORMAL("Bảng 1.5. Yêu cầu phi chức năng")),

        heading("1.3. Các lý thuyết nền tảng", HeadingLevel.HEADING_2),
        bullet("Kiến trúc Client–Server và RESTful API."),
        bullet("Mô hình quan hệ và thiết kế cơ sở dữ liệu (chuẩn hóa, khóa, ràng buộc)."),
        bullet("Xác thực JWT và phân quyền theo vai trò (RBAC)."),
        bullet("Quy trình phát triển phần mềm (SDLC, Agile)."),
        bullet("UML: Use Case, Activity, Sequence, Class, Component, State."),
        HINT("[Bổ sung lý thuyết về AI/chatbot nếu đề tài có triển khai phần này.]"),

        heading("1.4. Các công nghệ, công cụ sử dụng", HeadingLevel.HEADING_2),
        heading("1.4.1. Backend – Spring Boot", HeadingLevel.HEADING_3),
        HINT(
          "[Giới thiệu Spring Boot, Spring Security, JPA/Hibernate, REST controller, DTO pattern.]"
        ),
        heading("1.4.2. Mobile – Android (Java)", HeadingLevel.HEADING_3),
        HINT(
          "[Giới thiệu Android SDK, Activity/Fragment, Retrofit, Material Design, kiến trúc Repository.]"
        ),
        heading("1.4.3. Web Admin – React + TypeScript", HeadingLevel.HEADING_3),
        HINT("[Giới thiệu React, Vite, Ant Design, axios, routing, state management.]"),
        heading("1.4.4. Cơ sở dữ liệu – SQL Server", HeadingLevel.HEADING_3),
        HINT(
          "[Giới thiệu SQL Server, stored procedure, transaction, index; lý do chọn so với PostgreSQL trong đề cương.]"
        ),

        heading("1.5. Kết luận chương", HeadingLevel.HEADING_2),
        HINT(
          "[Tóm tắt các nội dung chương 1; làm cơ sở cho chương phân tích thiết kế.]"
        ),

        new Paragraph({ children: [new PageBreak()] }),

        // ===== CHƯƠNG 2 =====
        heading("CHƯƠNG 2. PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG", HeadingLevel.HEADING_1),
        HINT(
          "[Chương 2 dự kiến ~25 trang – phần quan trọng nhất, thể hiện tư duy phân tích/thiết kế.]"
        ),

        heading("2.1. Phân tích hệ thống", HeadingLevel.HEADING_2),
        heading("2.1.1. Sơ đồ tổng quan hệ thống", HeadingLevel.HEADING_3),
        HINT("[CHÈN HÌNH / SƠ ĐỒ TẠI ĐÂY]"),
        para(NORMAL("Hình 2.1. Sơ đồ tổng quan hệ thống ClinicBooking")),
        HINT(
          "[Mô tả: Android App (BN/BS) ↔ REST API (Spring Boot) ↔ SQL Server; Web Admin ↔ REST API.]"
        ),

        heading("2.1.2. Biểu đồ Use Case", HeadingLevel.HEADING_3),
        HINT("[CHÈN HÌNH Use Case tổng quát]"),
        para(NORMAL("Hình 2.2. Biểu đồ Use Case tổng quát")),
        HINT("[CHÈN HÌNH Use Case bệnh nhân]"),
        para(NORMAL("Hình 2.3. Biểu đồ Use Case bệnh nhân")),
        HINT("[CHÈN HÌNH Use Case bác sĩ]"),
        para(NORMAL("Hình 2.4. Biểu đồ Use Case bác sĩ")),
        HINT("[CHÈN HÌNH Use Case quản trị viên]"),
        para(NORMAL("Hình 2.5. Biểu đồ Use Case quản trị viên")),
        makeTable(
          ["Use Case", "Tác nhân", "Mô tả ngắn", "Tiền điều kiện"],
          [
            ["UC01 – Đặt lịch khám", "Bệnh nhân", "Chọn BS/slot, gửi yêu cầu", "Đã đăng nhập"],
            ["UC02 – Duyệt lịch", "Bác sĩ", "Xác nhận/từ chối yêu cầu", "Có lịch pending"],
            ["UC03 – Quản lý slot", "Admin", "Thiết lập ca/khung giờ", "Quyền ADMIN"],
            ["[" + "…]", "[…]", "[…]", "[…]"],
          ],
          [22, 15, 43, 20]
        ),
        para(NORMAL("Bảng 2.1. Bảng mô tả Use Case đặt lịch khám")),

        heading("2.1.3. Biểu đồ Activity", HeadingLevel.HEADING_3),
        HINT("[CHÈN HÌNH Activity – Luồng đặt lịch khám]"),
        para(NORMAL("Hình 2.6. Biểu đồ Activity – Luồng đặt lịch khám")),
        HINT("[CHÈN HÌNH Activity – Luồng duyệt & khám bệnh]"),
        para(NORMAL("Hình 2.7. Biểu đồ Activity – Luồng duyệt & khám bệnh")),

        heading("2.1.4. Biểu đồ Sequence", HeadingLevel.HEADING_3),
        HINT("[CHÈN HÌNH Sequence – Đặt lịch khám]"),
        para(NORMAL("Hình 2.8. Biểu đồ Sequence – Đặt lịch khám")),
        HINT("[CHÈN HÌNH Sequence – Đăng nhập JWT]"),
        para(NORMAL("Hình 2.9. Biểu đồ Sequence – Đăng nhập JWT")),

        heading("2.2. Thiết kế hệ thống", HeadingLevel.HEADING_2),
        heading("2.2.1. Thiết kế kiến trúc", HeadingLevel.HEADING_3),
        HINT("[CHÈN HÌNH kiến trúc Client–Server 3 tầng]"),
        para(NORMAL("Hình 2.10. Sơ đồ kiến trúc Client–Server 3 tầng")),
        HINT(
          "[Mô tả: Presentation (Android/Web) – Business Logic (Spring Service) – Data Access (Repository/SP).]"
        ),

        heading("2.2.2. Thiết kế cơ sở dữ liệu", HeadingLevel.HEADING_3),
        HINT("[CHÈN HÌNH ERD]"),
        para(NORMAL("Hình 2.11. Sơ đồ ERD cơ sở dữ liệu")),
        makeTable(
          ["Bảng", "Mô tả"],
          [
            ["accounts", "Tài khoản người dùng, vai trò"],
            ["patients", "Thông tin bệnh nhân"],
            ["doctors", "Thông tin bác sĩ"],
            ["specialties", "Chuyên khoa"],
            ["appointment_slots", "Khung giờ khám, capacity"],
            ["appointments", "Lịch hẹn"],
            ["chat_threads, chat_messages", "Hội thoại chat"],
            ["notifications", "Thông báo hệ thống"],
            ["email_otps", "OTP xác thực email"],
            ["[…]", "[…]"],
          ],
          [30, 70]
        ),
        para(NORMAL("Bảng 2.2. Danh sách bảng cơ sở dữ liệu")),

        heading("2.2.2.1. Thiết kế bảng appointments", HeadingLevel.HEADING_3),
        makeTable(
          ["Cột", "Kiểu", "Mô tả"],
          [
            ["id", "int PK", "Khóa chính"],
            ["patient_id", "int FK", "Bệnh nhân"],
            ["doctor_id", "int FK", "Bác sĩ"],
            ["slot_id", "int FK", "Slot thời gian"],
            ["status", "nvarchar", "pending/confirmed/checked_in/…"],
            ["booking_code", "nvarchar", "Mã đặt lịch"],
            ["[…]", "[…]", "[…]"],
          ],
          [25, 20, 55]
        ),
        para(NORMAL("Bảng 2.3. Cấu trúc bảng appointments (mẫu)")),
        HINT(
          "[Mô tả stored procedure sp_create_appointment: kiểm tra slot còn chỗ, tránh double booking, transaction.]"
        ),

        heading("2.2.3. Thiết kế giao diện (UI/UX)", HeadingLevel.HEADING_3),
        HINT("[CHÈN HÌNH Wireframe Android – Bệnh nhân]"),
        para(NORMAL("Hình 2.12. Wireframe – Luồng màn hình Android (Bệnh nhân)")),
        HINT("[CHÈN HÌNH Wireframe Web Admin]"),
        para(NORMAL("Hình 2.13. Wireframe – Web Admin Dashboard & Quản lý lịch hẹn")),
        HINT(
          "[Nguyên tắc UX: rõ ràng trạng thái lịch, ít bước đặt lịch, màu sắc theo Ant Design / Material.]"
        ),

        heading("2.2.4. Thiết kế API REST", HeadingLevel.HEADING_3),
        makeTable(
          ["Method", "Endpoint", "Mô tả", "Vai trò"],
          [
            ["POST", "/api/v1/auth/login", "Đăng nhập", "Public"],
            ["GET", "/api/v1/patient/doctors", "Danh sách BS", "Patient"],
            ["POST", "/api/v1/patient/bookings", "Đặt lịch", "Patient"],
            ["PATCH", "/api/v1/doctor/appointments/{id}/status", "Cập nhật TT", "Doctor"],
            ["GET", "/api/v1/admin/appointments", "Quản lý lịch", "Admin"],
            ["[…]", "[…]", "[…]", "[…]"],
          ],
          [12, 33, 35, 20]
        ),
        para(NORMAL("Bảng 2.4. Một số API tiêu biểu")),

        heading("2.2.5. Biểu đồ Class / Component", HeadingLevel.HEADING_3),
        HINT("[CHÈN HÌNH Class Diagram – Module Booking]"),
        para(NORMAL("Hình 2.14. Class Diagram – Module Booking (Backend)")),
        HINT("[CHÈN HÌNH Component Diagram]"),
        para(NORMAL("Hình 2.15. Component Diagram – Các module hệ thống")),

        heading("2.3. Thiết kế luồng trạng thái lịch hẹn", HeadingLevel.HEADING_2),
        HINT("[CHÈN HÌNH State Diagram – Vòng đời trạng thái Appointment]"),
        para(NORMAL("Hình 2.16. State Diagram – Vòng đời trạng thái Appointment")),
        HINT(
          "[Giải thích từng trạng thái và ai được phép chuyển trạng thái: pending → confirmed → checked_in → in_progress → completed / cancelled / no_show.]"
        ),

        heading("2.4. Kết luận chương", HeadingLevel.HEADING_2),
        HINT(
          "[Tóm tắt các mô hình phân tích/thiết kế; làm cơ sở cho Chương 3 – Cài đặt.]"
        ),

        new Paragraph({ children: [new PageBreak()] }),

        // ===== CHƯƠNG 3 =====
        heading("CHƯƠNG 3. CÀI ĐẶT HỆ THỐNG", HeadingLevel.HEADING_1),
        HINT(
          "[Chương 3 dự kiến ~25 trang. Trình bày cấu trúc mã nguồn, cấu hình, và ảnh chụp màn hình thực tế.]"
        ),

        heading("3.1. Cấu trúc mã nguồn và môi trường", HeadingLevel.HEADING_2),
        makeTable(
          ["Thư mục", "Mô tả"],
          [
            ["backend-bookingcare/", "Spring Boot – controllers, services, repositories, DTO"],
            ["frontendbookingcare/", "Android – ui, api, data, res"],
            ["web-admin/", "React admin – pages, api, components"],
            ["db_main.sql", "Schema + stored procedures"],
            ["db_seed.sql", "Dữ liệu mẫu"],
          ],
          [35, 65]
        ),
        para(NORMAL("Bảng 3.1. Cấu trúc repository")),

        heading("3.1.1. Môi trường phát triển", HeadingLevel.HEADING_3),
        makeTable(
          ["Hạng mục", "Cấu hình"],
          [
            ["OS", "Windows 10/11"],
            ["JDK", "17+"],
            ["Node.js", "18+ (web-admin)"],
            ["SQL Server", "[phiên bản]"],
            ["Android SDK", "[API level]"],
            ["Backend port", "8085"],
          ],
          [35, 65]
        ),
        para(NORMAL("Bảng 3.2. Cấu hình môi trường")),

        heading("3.1.2. Cài đặt cơ sở dữ liệu", HeadingLevel.HEADING_3),
        HINT(
          "[Hướng dẫn chạy db_main.sql và db_seed.sql bằng sqlcmd/SQL Server Management Studio.]"
        ),

        heading("3.2. Cài đặt Backend (Spring Boot)", HeadingLevel.HEADING_2),
        heading("3.2.1. Cấu hình application.properties", HeadingLevel.HEADING_3),
        HINT("[Datasource SQL Server, JWT secret, CORS, email OTP (nếu có)…]"),
        heading("3.2.2. Module xác thực và phân quyền", HeadingLevel.HEADING_3),
        HINT("[AuthController, JwtFilter, Role-based access: PATIENT, DOCTOR, ADMIN.]"),
        heading("3.2.3. Module đặt lịch (Booking)", HeadingLevel.HEADING_3),
        HINT("[BookingService, gọi sp_create_appointment, validate slot, mapping DTO.]"),
        heading("3.2.4. Module chat và thông báo", HeadingLevel.HEADING_3),
        HINT("[PatientMessageController, AdminMessageController, NotificationService.]"),
        HINT(
          "[Trích đoạn mã nguồn quan trọng – Listing 3.1: BookingService.createAppointment()]"
        ),

        heading("3.3. Cài đặt ứng dụng Android", HeadingLevel.HEADING_2),
        heading("3.3.1. Kiến trúc ứng dụng", HeadingLevel.HEADING_3),
        HINT(
          "[Phân tích: api (Retrofit), data (Repository), ui (Activity/Fragment). SessionExpiredBus xử lý 401.]"
        ),
        heading("3.3.2. Chức năng bệnh nhân", HeadingLevel.HEADING_3),
        HINT("[CHÈN HÌNH màn hình đăng nhập & trang chủ Android – Bệnh nhân]"),
        para(NORMAL("Hình 3.1. Màn hình đăng nhập và trang chủ (Android – Bệnh nhân)")),
        HINT("[CHÈN HÌNH luồng đặt lịch 3 bước]"),
        para(NORMAL("Hình 3.2. Luồng đặt lịch 3 bước")),
        HINT("[CHÈN HÌNH danh sách lịch hẹn & chi tiết]"),
        para(NORMAL("Hình 3.3. Danh sách lịch hẹn và chi tiết")),

        heading("3.3.3. Chức năng bác sĩ", HeadingLevel.HEADING_3),
        HINT("[CHÈN HÌNH giao diện bác sĩ – lịch hẹn & duyệt lịch]"),
        para(NORMAL("Hình 3.4. Giao diện bác sĩ – Lịch hẹn và duyệt lịch")),
        HINT(
          "[DoctorMainActivity, duyệt/từ chối, start/complete/no-show, chat với BN.]"
        ),

        heading("3.4. Cài đặt Web Admin (React)", HeadingLevel.HEADING_2),
        heading("3.4.1. Routing và bảo vệ route", HeadingLevel.HEADING_3),
        HINT("[Login, JWT lưu localStorage/session, axios interceptor 401.]"),
        heading("3.4.2. Các module quản trị", HeadingLevel.HEADING_3),
        HINT("[CHÈN HÌNH Web Admin – Quản lý lịch hẹn]"),
        para(NORMAL("Hình 3.5. Web Admin – Quản lý lịch hẹn")),
        HINT("[CHÈN HÌNH Web Admin – Quản lý bác sĩ & slot]"),
        para(NORMAL("Hình 3.6. Web Admin – Quản lý bác sĩ và slot")),
        HINT("[CHÈN HÌNH Web Admin – Dashboard báo cáo]"),
        para(NORMAL("Hình 3.7. Web Admin – Dashboard báo cáo")),
        HINT(
          "[Mô tả: Appointments, Doctors, Specialties, Services, Slots, Patients, Messages, Reports.]"
        ),

        heading("3.5. Triển khai và vận hành", HeadingLevel.HEADING_2),
        HINT(
          "[Backend: java -jar hoặc mvn spring-boot:run. Web: npm run build. Android: assembleDebug. Ghi chú BASE_URL.]"
        ),
        makeTable(
          ["Email", "Mật khẩu", "Vai trò"],
          [
            ["admin@clinic.local", "admin123", "ADMIN"],
            ["bn001@mail.vn", "bn123456", "PATIENT"],
            ["bs07@antamclinic.vn", "bs123456", "DOCTOR"],
          ],
          [40, 30, 30]
        ),
        para(NORMAL("Bảng 3.3. Tài khoản demo")),

        heading("3.6. Kết luận chương", HeadingLevel.HEADING_2),
        HINT(
          "[Tóm tắt các module đã cài đặt; chuyển sang Chương 4 – Kiểm thử.]"
        ),

        new Paragraph({ children: [new PageBreak()] }),

        // ===== CHƯƠNG 4 =====
        heading("CHƯƠNG 4. KIỂM THỬ HỆ THỐNG", HeadingLevel.HEADING_1),
        HINT("[Chương 4 dự kiến ~8 trang.]"),

        heading("4.1. Mục tiêu và phạm vi kiểm thử", HeadingLevel.HEADING_2),
        bullet("Kiểm thử chức năng các luồng nghiệp vụ chính."),
        bullet("Kiểm thử API bằng Postman."),
        bullet("Kiểm thử giao diện Android và Web Admin."),
        bullet("Kiểm thử bảo mật cơ bản (401, phân quyền)."),

        heading("4.2. Kế hoạch kiểm thử", HeadingLevel.HEADING_2),
        makeTable(
          ["ID", "Chức năng", "Mô tả test", "Dữ liệu vào", "KQ mong đợi", "KQ"],
          [
            ["TC-01", "Đăng nhập BN", "Email/pass đúng", "bn001@mail.vn", "200 + token", "Pass/Fail"],
            ["TC-02", "Đăng nhập sai MK", "Pass sai", "[…]", "401/400", "Pass/Fail"],
            ["TC-03", "Đặt lịch hợp lệ", "Chọn slot trống", "[…]", "Tạo pending", "Pass/Fail"],
            ["TC-04", "Đặt slot đầy", "Slot hết chỗ", "[…]", "Báo lỗi", "Pass/Fail"],
            ["TC-05", "BS duyệt lịch", "pending→confirmed", "[…]", "Cập nhật OK", "Pass/Fail"],
            ["TC-06", "Check-in", "confirmed→checked_in", "[…]", "OK", "Pass/Fail"],
            ["TC-07", "Complete khám", "in_progress→completed", "[…]", "OK + note", "Pass/Fail"],
            ["TC-08", "Hủy lịch BN", "Trước giờ khám", "[…]", "cancelled", "Pass/Fail"],
            ["TC-09", "Admin đổi lịch", "Chọn slot mới", "[…]", "Rescheduled", "Pass/Fail"],
            ["TC-10", "Chat CSKH", "Gửi tin nhắn", "[…]", "Lưu thread", "Pass/Fail"],
            ["TC-11", "Export CSV", "Web admin", "[…]", "File tải về", "Pass/Fail"],
            ["TC-12", "Session hết hạn", "Token expired", "[…]", "401 → logout", "Pass/Fail"],
          ],
          [8, 14, 22, 16, 18, 12]
        ),
        para(NORMAL("Bảng 4.1. Bảng test case chức năng")),

        heading("4.3. Kết quả kiểm thử", HeadingLevel.HEADING_2),
        makeTable(
          ["Nhóm", "Tổng TC", "Pass", "Fail", "Tỷ lệ"],
          [
            ["Xác thực", "[…]", "[…]", "[…]", "[…]"],
            ["Đặt lịch", "[…]", "[…]", "[…]", "[…]"],
            ["Quản lý lịch BS/Admin", "[…]", "[…]", "[…]", "[…]"],
            ["Chat/Thông báo", "[…]", "[…]", "[…]", "[…]"],
            ["Tổng", "[…]", "[…]", "[…]", "[…]"],
          ],
          [30, 17, 17, 17, 19]
        ),
        para(NORMAL("Bảng 4.2. Tổng hợp kết quả kiểm thử")),
        HINT(
          "[Mô tả các lỗi phát hiện và cách khắc phục. Ví dụ: double booking, timezone notification, stale slot guard…]"
        ),
        HINT("[CHÈN HÌNH ảnh chụp kết quả test Postman]"),
        para(NORMAL("Hình 4.1. Ảnh chụp kết quả test Postman (API đặt lịch)")),

        heading("4.4. Đánh giá hiệu năng và bảo mật", HeadingLevel.HEADING_2),
        HINT(
          "[Thời gian phản hồi API trung bình; kiểm tra JWT; không lộ thông tin nhạy cảm trong log.]"
        ),

        heading("4.5. Kết luận chương", HeadingLevel.HEADING_2),
        HINT(
          "[Hệ thống đáp ứng yêu cầu; nêu hạn chế còn tồn tại sau kiểm thử.]"
        ),

        new Paragraph({ children: [new PageBreak()] }),

        // ===== KẾT LUẬN =====
        heading("KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN", HeadingLevel.HEADING_1),

        heading("1. Kết luận", HeadingLevel.HEADING_2),
        HINT(
          "[Tóm tắt những gì đã hoàn thành: hệ thống ClinicBooking với 3 client, quy trình đặt-duyệt-khám, web quản trị, chat CSKH. So sánh với mục tiêu ban đầu trong đề cương.]"
        ),
        heading("1.1. Ưu điểm", HeadingLevel.HEADING_3),
        bullet("Quy trình nghiệp vụ rõ ràng, trạng thái lịch hẹn minh bạch."),
        bullet("Kiến trúc tách client/server dễ bảo trì và mở rộng."),
        bullet("Capacity-aware booking qua stored procedure tránh trùng slot."),
        bullet("Giao diện tiếng Việt, thân thiện trên mobile và web."),

        heading("1.2. Hạn chế", HeadingLevel.HEADING_3),
        bullet("Chưa tích hợp thanh toán và hóa đơn điện tử."),
        bullet(
          "Chatbot AI tư vấn tự động chưa triển khai đầy đủ [điều chỉnh theo thực tế]."
        ),
        bullet("Chưa triển khai production scale (load balancing, CI/CD)."),
        bullet("Chưa có ứng dụng iOS."),

        heading("2. Hướng phát triển", HeadingLevel.HEADING_2),
        bullet("Tích hợp AI/LLM cho chatbot tư vấn đặt lịch và FAQ y tế cơ bản."),
        bullet("Tích hợp cổng thanh toán (VNPay, MoMo)."),
        bullet("Thông báo push Firebase Cloud Messaging."),
        bullet("Triển khai Docker/Kubernetes; monitoring."),
        bullet("Mở rộng đa phòng khám (multi-tenant)."),
        bullet("Phát triển phiên bản iOS/React Native."),

        new Paragraph({ children: [new PageBreak()] }),

        // ===== TÀI LIỆU THAM KHẢO =====
        heading("TÀI LIỆU THAM KHẢO", HeadingLevel.HEADING_1),
        HINT(
          "[Sắp xếp theo thứ tự trích dẫn trong báo cáo hoặc alphabet. Dùng chuẩn IEEE/APA tùy quy định Khoa.]"
        ),
        bullet("[1] Spring Boot Documentation – https://spring.io/projects/spring-boot"),
        bullet("[2] Android Developers – https://developer.android.com/"),
        bullet("[3] React Documentation – https://react.dev/"),
        bullet("[4] Microsoft SQL Server Documentation – https://learn.microsoft.com/sql/"),
        bullet("[5] Sommerville I., Software Engineering, 10th Edition, Pearson."),
        bullet("[6] Fowler M., Patterns of Enterprise Application Architecture, Addison-Wesley."),
        bullet("[7] Fielding R., Architectural Styles and the Design of Network-based Software Architectures, 2000."),
        bullet("[8] JWT RFC 7519 – https://datatracker.ietf.org/doc/html/rfc7519"),
        bullet("[9] Ant Design – https://ant.design/"),
        bullet("[10] Tài liệu đặc tả yêu cầu ClinicBooking (nội bộ đồ án)."),
        bullet("[11] [Bổ sung sách/giáo trình Phân tích thiết kế HTTT, CSDL…]"),
        bullet("[12] [Bổ sung bài báo/luận văn liên quan đặt lịch y tế]"),

        new Paragraph({ children: [new PageBreak()] }),

        // ===== PHỤ LỤC =====
        heading("PHỤ LỤC", HeadingLevel.HEADING_1),

        heading("Phụ lục A – Danh sách endpoint API (mẫu)", HeadingLevel.HEADING_2),
        HINT(
          "[Đính kèm bảng đầy đủ endpoint hoặc export từ Swagger/OpenAPI nếu có.]"
        ),

        heading("Phụ lục B – Hướng dẫn cài đặt nhanh", HeadingLevel.HEADING_2),
        HINT("[Clone repo, cấu hình DB, chạy backend/web/android.]"),

        heading("Phụ lục C – Một số mã nguồn minh họa", HeadingLevel.HEADING_2),
        HINT(
          "[BookingService.java, AppointmentController, BookingStepActivity… – trích đoạn ngắn.]"
        ),

        heading("Phụ lục D – Phiếu khảo sát / biên bản test (nếu có)", HeadingLevel.HEADING_2),
        HINT(
          "[Scan phiếu khảo sát phòng khám hoặc checklist UAT.]"
        ),
      ],
    },
  ],
});

const buffer = await Packer.toBuffer(doc);
const outPath = "d:/datn/VuQuyetTien_223630716_KHMT_K63_BaoCaoDoAn_KHUNG.docx";
fs.writeFileSync(outPath, buffer);
console.log("Created:", outPath, "size:", buffer.length);
