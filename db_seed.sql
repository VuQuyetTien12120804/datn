/*
  ClinicBooking — SEED DATA
  File duy nhất cho dữ liệu mẫu. Chạy SAU `db_main.sql`.

  Nội dung:
  - Phòng khám Quyết Tiến, legal docs (TERMS/PRIVACY/FAQ)
  - 40 bác sĩ (không trùng tên), BS. Bùi Hoàng Minh #7 đa chuyên khoa
  - 10 bệnh nhân, slot 21 ngày, ~60 lịch hẹn mẫu
  - Không seed hóa đơn/thanh toán (app không có luồng thanh toán)

  Tài khoản test:
  - Admin:   admin@clinic.local / admin123
  - Bệnh nhân: bn001@mail.vn / bn123456
  - Bác sĩ:  bs07@antamclinic.vn / bs123456

  Mật khẩu plaintext — chỉ dùng LOCAL DEV.
*/

USE clinic_db;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET ANSI_PADDING ON;
GO

/* Wait until DB is ONLINE (avoid "in transition" right after DROP/CREATE) */
DECLARE @try int = 0;
WHILE @try < 60
  AND EXISTS (SELECT 1 FROM sys.databases WHERE name = N'clinic_db' AND state_desc <> N'ONLINE')
BEGIN
  WAITFOR DELAY '00:00:01';
  SET @try += 1;
END
GO

BEGIN TRANSACTION;

/* ===== 1) Clinic metadata ===== */
INSERT INTO dbo.clinics(code, name, phone, email, address, timezone)
VALUES
(N'CLINIC-HN-01', N'Phòng khám đa khoa Quyết Tiến', N'02473008899', N'lienhe@quyettien.vn', N'25 P. Láng Hạ, Đống Đa, Hà Nội', N'Asia/Ho_Chi_Minh');

/* ===== 2) Legal docs (for mobile app) ===== */
INSERT INTO dbo.app_legal_documents(code, title, body_html, version)
VALUES
(N'TERMS', N'Điều khoản sử dụng', N'<h2>1. Phạm vi</h2><p>Ứng dụng ClinicBooking của Phòng khám Quyết Tiến hỗ trợ đặt lịch khám, tra cứu lịch hẹn và liên hệ CSKH.</p><h2>2. Tài khoản</h2><p>Bạn chịu trách nhiệm bảo mật thông tin đăng nhập và thông báo kịp thời nếu phát hiện truy cập trái phép.</p><h2>3. Đặt lịch</h2><p>Lịch hẹn có hiệu lực sau khi bác sĩ/phòng khám xác nhận. Vui lòng đến đúng giờ hoặc huỷ trước nếu không thể tham gia.</p>', 1),
(N'PRIVACY', N'Chính sách bảo mật', N'<h2>1. Dữ liệu thu thập</h2><p>Họ tên, email, số điện thoại, ngày sinh, giới tính, địa chỉ và thông tin lịch khám.</p><h2>2. Mục đích</h2><p>Phục vụ đặt lịch, khám chữa bệnh, liên hệ và cải thiện dịch vụ.</p><h2>3. Bảo vệ</h2><p>Dữ liệu được lưu trữ an toàn và chỉ chia sẻ khi có yêu cầu pháp luật hoặc cần thiết cho điều trị.</p>', 1),
(N'SERVICE', N'Điều khoản dịch vụ', N'<h2>Phạm vi dịch vụ</h2><p>Ứng dụng ClinicBooking hỗ trợ đặt lịch khám, tra cứu lịch hẹn và liên hệ Phòng khám Quyết Tiến.</p><h2>Thanh toán</h2><p>Phí khám được thu tại phòng khám trừ khi có thông báo khác.</p>', 1),
(N'FAQ', N'Câu hỏi thường gặp', N'<h2>Đặt lịch khám</h2><p><strong>Làm sao để đặt lịch?</strong> Chọn bác sĩ → chọn ngày/giờ → xác nhận thông tin → hoàn tất.</p><p><strong>Huỷ lịch?</strong> Vào tab Lịch khám, mở phiếu khám và chọn Huỷ (trước giờ hẹn).</p><h2>Check-in</h2><p>Check-in được bật trong ngày khám sau khi bác sĩ duyệt lịch.</p><h2>Liên hệ</h2><p>Hotline: 0247 300 8899 — Email: lienhe@quyettien.vn</p>', 1);

/* ===== 3) Roles ===== */
SET IDENTITY_INSERT dbo.roles ON;
INSERT INTO dbo.roles(id, code, name, description, is_active, is_deleted)
VALUES
(1, N'admin',  N'Administrator', N'Quản trị hệ thống', 1, 0),
(2, N'doctor', N'Bác sĩ',        N'Bác sĩ khám bệnh', 1, 0),
(3, N'patient',N'Bệnh nhân',     N'Người dùng đặt lịch', 1, 0);
SET IDENTITY_INSERT dbo.roles OFF;

/* ===== 4) Specialties ===== */
DECLARE @spec TABLE(code nvarchar(50), name nvarchar(255), descr nvarchar(max));
INSERT INTO @spec(code, name, descr) VALUES
(N'INT',   N'Nội tổng quát',            N'Khám nội, kiểm tra tổng quát'),
(N'PED',   N'Nhi khoa',                 N'Khám và tư vấn sức khỏe trẻ em'),
(N'ENT',   N'Tai Mũi Họng',             N'Khám TMH, nội soi khi cần'),
(N'DERM',  N'Da liễu',                  N'Khám và điều trị các bệnh da'),
(N'CARD',  N'Tim mạch',                 N'Khám tim mạch, tăng huyết áp'),
(N'OBGYN', N'Sản - Phụ khoa',           N'Khám phụ khoa, tư vấn thai kỳ'),
(N'EYE',   N'Mắt',                      N'Khám mắt, đo thị lực, tư vấn'),
(N'DENT',  N'Răng - Hàm - Mặt',         N'Khám răng, điều trị nha khoa'),
(N'ORTHO', N'Cơ - Xương - Khớp',        N'Khám cơ xương khớp, đau lưng'),
(N'GI',    N'Tiêu hóa',                 N'Khám tiêu hóa, đau dạ dày'),
(N'ENDO',  N'Nội tiết',                 N'Đái tháo đường, tuyến giáp'),
(N'NEURO', N'Thần kinh',                N'Đau đầu, chóng mặt, thần kinh');

INSERT INTO dbo.specialties(code, name, description)
SELECT code, name, descr FROM @spec;

/* ===== 5) Rooms ===== */
DECLARE @rooms TABLE(code nvarchar(50), name nvarchar(255), floor nvarchar(50), note nvarchar(max));
INSERT INTO @rooms(code, name, floor, note) VALUES
(N'R101', N'Phòng khám 101', N'1', N'Nội tổng quát'),
(N'R102', N'Phòng khám 102', N'1', N'Nhi khoa'),
(N'R103', N'Phòng khám 103', N'1', N'Tiêu hóa'),
(N'R104', N'Phòng khám 104', N'1', N'Nội tiết'),
(N'R201', N'Phòng khám 201', N'2', N'Tai Mũi Họng'),
(N'R202', N'Phòng khám 202', N'2', N'Da liễu'),
(N'R203', N'Phòng khám 203', N'2', N'Tim mạch'),
(N'R301', N'Phòng khám 301', N'3', N'Sản - Phụ khoa'),
(N'R302', N'Phòng khám 302', N'3', N'Sản - Phụ khoa'),
(N'R303', N'Phòng khám 303', N'3', N'Mắt'),
(N'R304', N'Phòng khám 304', N'3', N'Răng - Hàm - Mặt'),
(N'R306', N'Phòng khám 306', N'3', N'Cơ - Xương - Khớp');

INSERT INTO dbo.rooms(code, name, floor, note)
SELECT code, name, floor, note FROM @rooms;

/* ===== 6) Services ===== */
DECLARE @svc TABLE(
  code nvarchar(50),
  name nvarchar(255),
  spec_code nvarchar(50),
  duration int,
  price bigint,
  descr nvarchar(max)
);

INSERT INTO @svc(code, name, spec_code, duration, price, descr) VALUES
(N'INT-GEN',   N'Khám nội tổng quát',            N'INT',   20, 150000, N'Khám, tư vấn, kê đơn'),
(N'INT-HEAL',  N'Tư vấn sức khỏe định kỳ',       N'INT',   30, 250000, N'Đánh giá tổng quát'),
(N'PED-GEN',   N'Khám nhi tổng quát',            N'PED',   20, 150000, N'Khám & tư vấn cho trẻ'),
(N'PED-VAC',   N'Tư vấn tiêm chủng',             N'PED',   15, 100000, N'Tư vấn lịch tiêm'),
(N'ENT-GEN',   N'Khám tai mũi họng',             N'ENT',   20, 160000, N'Khám TMH'),
(N'ENT-ENDO',  N'Nội soi tai mũi họng',          N'ENT',   25, 300000, N'Nội soi khi cần'),
(N'DERM-GEN',  N'Khám da liễu',                  N'DERM',  20, 170000, N'Khám bệnh da'),
(N'DERM-ACNE', N'Điều trị mụn (tái khám)',        N'DERM',  15, 120000, N'Theo phác đồ'),
(N'CARD-GEN',  N'Khám tim mạch',                 N'CARD',  25, 200000, N'Khám tim mạch'),
(N'CARD-ECG',  N'Điện tim (ECG)',                N'CARD',  15, 180000, N'Đo điện tim'),
(N'OB-GEN',    N'Khám phụ khoa',                 N'OBGYN', 25, 220000, N'Khám phụ khoa'),
(N'OB-SONO',   N'Siêu âm (tổng quát)',           N'OBGYN', 20, 250000, N'Siêu âm chẩn đoán'),
(N'EYE-GEN',   N'Khám mắt',                      N'EYE',   20, 170000, N'Khám & đo thị lực'),
(N'EYE-REF',   N'Đo khúc xạ',                    N'EYE',   15, 150000, N'Đo kính'),
(N'DENT-CLEAN',N'Cạo vôi răng',                  N'DENT',  30, 300000, N'Vệ sinh răng miệng'),
(N'DENT-FILL', N'Trám răng',                     N'DENT',  30, 350000, N'Trám răng sâu'),
(N'ORTHO-GEN', N'Khám cơ xương khớp',            N'ORTHO', 25, 200000, N'Khám đau lưng/khớp'),
(N'ORTHO-PT',  N'Tư vấn phục hồi chức năng',     N'ORTHO', 30, 250000, N'Bài tập & theo dõi'),
(N'GI-GEN',    N'Khám tiêu hóa',                 N'GI',    25, 200000, N'Khám tiêu hóa'),
(N'GI-ULS',    N'Siêu âm bụng',                  N'GI',    20, 250000, N'Siêu âm bụng'),
(N'ENDO-GEN',  N'Khám nội tiết',                 N'ENDO',  25, 210000, N'Đái tháo đường/tuyến giáp'),
(N'ENDO-TSH',  N'Tư vấn tuyến giáp (tái khám)',  N'ENDO',  15, 150000, N'Theo dõi điều trị'),
(N'NEURO-GEN', N'Khám thần kinh',                N'NEURO', 25, 220000, N'Khám thần kinh');

-- add a few generic add-ons
INSERT INTO @svc(code, name, spec_code, duration, price, descr) VALUES
(N'GEN-BP',    N'Đo huyết áp & tư vấn nhanh',    N'INT',   10,  50000, N'Dịch vụ nhanh'),
(N'GEN-GLU',   N'Đo đường huyết nhanh',          N'ENDO',  10,  70000, N'Dịch vụ nhanh'),
(N'GEN-BMI',   N'Đánh giá BMI & dinh dưỡng',     N'INT',   10,  70000, N'Dịch vụ nhanh'),
(N'GEN-LAB',   N'Tư vấn xét nghiệm cơ bản',      N'INT',   15, 120000, N'Gợi ý xét nghiệm');

INSERT INTO dbo.services(specialty_id, code, name, description, duration_minutes, price_cents, is_active)
SELECT s.id, v.code, v.name, v.descr, v.duration, v.price, 1
FROM @svc v
JOIN dbo.specialties s ON s.code = v.spec_code;

/* ===== 7) Accounts: admin + doctors + patients ===== */
/* NOTE: Plaintext passwords below are for LOCAL DEV ONLY (e.g. bs123456). Hash in production. */
DECLARE @adminId int = 1;
SET IDENTITY_INSERT dbo.accounts ON;
INSERT INTO dbo.accounts(id, role_id, email, phone, password, full_name, status, is_email_verified)
VALUES
(@adminId, 1, N'admin@clinic.local', N'0901000001', N'admin123', N'Quản trị hệ thống', N'active', 1);

-- 40 bác sĩ — tên KHÔNG trùng (tránh lỗi CHOOSE lặp mỗi 10 ID như BS. Bùi Hoàng Minh x3).
DECLARE @docNames TABLE(i int NOT NULL PRIMARY KEY, full_name nvarchar(255) NOT NULL);
INSERT INTO @docNames(i, full_name) VALUES
(1,  N'BS. Nguyễn Văn An'),
(2,  N'BS. Trần Quang Bảo'),
(3,  N'BS. Lê Đức Cường'),
(4,  N'BS. Phạm Gia Dũng'),
(5,  N'BS. Hoàng Văn Em'),
(6,  N'BS. Vũ Thị Phương'),
(7,  N'BS. Bùi Hoàng Minh'),
(8,  N'BS. Đặng Khánh Nam'),
(9,  N'BS. Đỗ Thu Oanh'),
(10, N'BS. Ngô Minh Phúc'),
(11, N'BS. Nguyễn Thị Quỳnh'),
(12, N'BS. Trần Văn Sơn'),
(13, N'BS. Lê Ngọc Tuyết'),
(14, N'BS. Phạm Hữu Uy'),
(15, N'BS. Hoàng Thị Vi'),
(16, N'BS. Vũ Quốc Xuân'),
(17, N'BS. Đặng Anh Yến'),
(18, N'BS. Bùi Thu Linh'),
(19, N'BS. Đỗ Văn Kiên'),
(20, N'BS. Ngô Thị Lan'),
(21, N'BS. Nguyễn Văn Hùng'),
(22, N'BS. Trần Thị Hoa'),
(23, N'BS. Lê Văn Khang'),
(24, N'BS. Phạm Thị Mai'),
(25, N'BS. Hoàng Văn Nghĩa'),
(26, N'BS. Vũ Thị Oanh'),
(27, N'BS. Đặng Văn Phong'),
(28, N'BS. Bùi Thị Quyên'),
(29, N'BS. Đỗ Văn Sáng'),
(30, N'BS. Ngô Thị Thảo'),
(31, N'BS. Nguyễn Văn Uy'),
(32, N'BS. Trần Thị Vân'),
(33, N'BS. Lê Văn Đạt'),
(34, N'BS. Phạm Thị Yến'),
(35, N'BS. Hoàng Văn Bình'),
(36, N'BS. Vũ Thị Chi'),
(37, N'BS. Đặng Văn Dũng'),
(38, N'BS. Bùi Thị Ngọc'),
(39, N'BS. Đỗ Văn Long'),
(40, N'BS. Ngô Thị Hằng');

DECLARE @doctorCount int = 40;
DECLARE @i int = 1;
WHILE @i <= @doctorCount
BEGIN
  DECLARE @accId int = 1000 + @i;
  DECLARE @full nvarchar(255) = (SELECT full_name FROM @docNames WHERE i = @i);

  DECLARE @email nvarchar(255) = N'bs' + RIGHT(N'00' + CAST(@i AS nvarchar(10)), 2) + N'@antamclinic.vn';
  DECLARE @phone nvarchar(30)  = N'09' + RIGHT(N'00000000' + CAST(10000000 + @i AS nvarchar(20)), 8);

  INSERT INTO dbo.accounts(id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@accId, 2, @email, @phone, N'bs123456', @full, N'active', 1);

  SET @i += 1;
END

-- Patients
DECLARE @patientCount int = 10;
SET @i = 1;
WHILE @i <= @patientCount
BEGIN
  DECLARE @pAccId int = 2000 + @i;
  DECLARE @pFemale bit = CASE WHEN (@i % 2) = 0 THEN 1 ELSE 0 END;
  -- Dùng 2 index khác nhau cho họ và tên để không trùng họ tên đầy đủ.
  -- firstIdx chạy nhanh (đổi mỗi patient), lastIdx đổi mỗi 15 patient.
  -- → Hỗ trợ cả 10 patient (10 tên unique) và 150 patient (11 × 15 = 165 combo).
  DECLARE @lastIdx int = (((@i - 1) / 15) % 11) + 1;
  DECLARE @firstIdx int = ((@i - 1) % 15) + 1;
  DECLARE @pfull nvarchar(255) =
    CASE WHEN @pFemale = 1
      THEN (CHOOSE(@lastIdx, N'Nguyễn',N'Trần',N'Lê',N'Phạm',N'Hoàng',N'Vũ',N'Đặng',N'Bùi',N'Đỗ',N'Ngô',N'Phan') + N' ' +
            N'Thị ' + CHOOSE(@firstIdx,
              N'An',N'Bích',N'Chi',N'Diệp',N'Giang',N'Hà',N'Khánh',N'Lan',N'Nga',N'Quỳnh',
              N'Hương',N'Mai',N'Thảo',N'Trang',N'Vân'))
      ELSE (CHOOSE(@lastIdx, N'Nguyễn',N'Trần',N'Lê',N'Phạm',N'Hoàng',N'Vũ',N'Đặng',N'Bùi',N'Đỗ',N'Ngô',N'Phan') + N' ' +
            N'Văn ' + CHOOSE(@firstIdx,
              N'An',N'Bình',N'Chính',N'Dũng',N'Giáp',N'Hải',N'Khôi',N'Lâm',N'Nam',N'Quân',
              N'Sơn',N'Tuấn',N'Việt',N'Xuân',N'Phong'))
    END;

  DECLARE @pemail nvarchar(255) = N'bn' + RIGHT(N'000' + CAST(@i AS nvarchar(10)), 3) + N'@mail.vn';
  DECLARE @pphone nvarchar(30)  = N'08' + RIGHT(N'00000000' + CAST(20000000 + @i AS nvarchar(20)), 8);

  INSERT INTO dbo.accounts(id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@pAccId, 3, @pemail, @pphone, N'bn123456', @pfull, N'active', 1);

  SET @i += 1;
END

SET IDENTITY_INSERT dbo.accounts OFF;

/* ===== 8) Doctors table + specialties mapping ===== */
DECLARE @avatarMale nvarchar(500) = N'https://api.dicebear.com/7.x/avataaars/png?seed=doctor-male';
DECLARE @avatarFemale nvarchar(500) = N'https://api.dicebear.com/7.x/avataaars/png?seed=doctor-female';

SET IDENTITY_INSERT dbo.doctors ON;
SET @i = 1;
WHILE @i <= @doctorCount
BEGIN
  DECLARE @dId int = @i;
  DECLARE @accId2 int = 1000 + @i;
  DECLARE @dFull nvarchar(255) = (SELECT full_name FROM @docNames WHERE i = @i);
  DECLARE @gender nvarchar(20) = CASE WHEN (@i % 3)=0 THEN N'female' ELSE N'male' END;
  DECLARE @dob date = DATEFROMPARTS(1978 + (@i % 15), ((@i % 12) + 1), ((@i % 27) + 1));
  DECLARE @license nvarchar(100) = N'VN-' + RIGHT(N'0000' + CAST(@i AS nvarchar(10)), 4);
  DECLARE @bio nvarchar(max) = N'Khám và tư vấn theo hướng dẫn chuyên môn. Ưu tiên giải thích rõ ràng, theo dõi sát điều trị.';
  DECLARE @roomLoc nvarchar(255) = CASE WHEN @i = 7 THEN N'Phòng 203, tầng 2' ELSE NULL END;

  INSERT INTO dbo.doctors(
    id, account_id, full_name, gender, dob, phone, email, license_no, bio, avatar_url,
    rating, visits_count, room_location, schedule_text, education_json, certificates_json
  )
  SELECT
    @dId, a.id, @dFull, @gender, @dob, a.phone, a.email, @license, @bio,
    CASE WHEN @gender = N'female' THEN @avatarFemale ELSE @avatarMale END,
    CAST(4.2 + ((@i % 8) * 0.1) AS decimal(2,1)),
    (30 + (@i * 7)) % 600,
    @roomLoc,
    NULL,
    N'["ĐH Y Hà Nội","Chứng chỉ hành nghề"]',
    N'["Chứng chỉ CME","Chứng chỉ chuyên khoa"]'
  FROM dbo.accounts a WHERE a.id = @accId2;

  SET @i += 1;
END
SET IDENTITY_INSERT dbo.doctors OFF;

-- Map specialties: doctor #7 = đa chuyên khoa; các bác sĩ khác = 1 chính (+ phụ mỗi 5 người)
SET @i = 1;
WHILE @i <= @doctorCount
BEGIN
  IF @i = 7
  BEGIN
    INSERT INTO dbo.doctor_specialties(doctor_id, specialty_id)
    SELECT 7, s.id
    FROM dbo.specialties s
    WHERE s.code IN (N'EYE', N'INT', N'CARD');
  END
  ELSE
  BEGIN
    DECLARE @primarySpec int = ((@i - 1) % 12) + 1;
    INSERT INTO dbo.doctor_specialties(doctor_id, specialty_id) VALUES (@i, @primarySpec);

    IF (@i % 5) = 0
    BEGIN
      DECLARE @secondarySpec int = ((@i + 3) % 12) + 1;
      IF @secondarySpec <> @primarySpec
        INSERT INTO dbo.doctor_specialties(doctor_id, specialty_id) VALUES (@i, @secondarySpec);
    END
  END

  SET @i += 1;
END

-- Sanity: không trùng tên bác sĩ trong seed
IF EXISTS (
  SELECT full_name FROM dbo.doctors GROUP BY full_name HAVING COUNT(*) > 1
)
BEGIN
  RAISERROR(N'Seed error: duplicate doctor full_name detected.', 16, 1);
END

/* ===== 9) Patients table ===== */
SET IDENTITY_INSERT dbo.patients ON;
SET @i = 1;
WHILE @i <= @patientCount
BEGIN
  DECLARE @patientIdSeed int = @i;
  DECLARE @pAccId2 int = 2000 + @i;
  DECLARE @pgender nvarchar(20) = CASE WHEN (@i % 2)=0 THEN N'female' ELSE N'male' END;
  DECLARE @pdob date = DATEFROMPARTS(1965 + (@i % 45), ((@i % 12) + 1), ((@i % 27) + 1));
  DECLARE @addr nvarchar(500) =
    CHOOSE((@i % 6) + 1,
      N'Đống Đa, Hà Nội', N'Cầu Giấy, Hà Nội', N'Thanh Xuân, Hà Nội',
      N'Hai Bà Trưng, Hà Nội', N'Ba Đình, Hà Nội', N'Hoàng Mai, Hà Nội');

  INSERT INTO dbo.patients(
    id, account_id, full_name, dob, gender, phone, email, address,
    insurance_no, emergency_contact_name, emergency_contact_phone
  )
  SELECT
    @patientIdSeed, a.id, a.full_name, @pdob, @pgender, a.phone, a.email,
    @addr,
    N'BHYT-' + RIGHT(N'000000' + CAST(@i AS nvarchar(10)), 6),
    N'Người nhà ' + a.full_name,
    N'09' + RIGHT(N'00000000' + CAST(30000000 + @i AS nvarchar(20)), 8)
  FROM dbo.accounts a WHERE a.id = @pAccId2;

  SET @i += 1;
END
SET IDENTITY_INSERT dbo.patients OFF;

-- Sanity: không trùng họ tên bệnh nhân trong seed
IF EXISTS (
  SELECT full_name FROM dbo.patients GROUP BY full_name HAVING COUNT(*) > 1
)
BEGIN
  RAISERROR(N'Seed error: duplicate patient full_name detected.', 16, 1);
END

/* ===== 10) Weekly schedules for all doctors ===== */
DECLARE @roomIds TABLE(id int);
INSERT INTO @roomIds(id) SELECT id FROM dbo.rooms ORDER BY id;

-- 2 sessions/day patterns
DECLARE @sess TABLE(start_time time(0), end_time time(0), slot_minutes int);
INSERT INTO @sess VALUES ('08:00','11:30',30), ('13:30','17:00',30);

SET @i = 1;
WHILE @i <= @doctorCount
BEGIN
  DECLARE @d int = @i;
  DECLARE @dow1 tinyint = CASE WHEN (@i % 6) < 3 THEN 2 ELSE 3 END;  -- 2 or 3
  DECLARE @dow2 tinyint = @dow1 + 2;                                 -- 4 or 5
  DECLARE @dow3 tinyint = 6;                                         -- Fri
  DECLARE @dow4 tinyint = CASE WHEN (@i % 4)=0 THEN 7 ELSE NULL END;  -- some Sat

  DECLARE @room int = (SELECT TOP 1 id FROM dbo.rooms ORDER BY ABS(CHECKSUM(NEWID()))); -- random-ish

  INSERT INTO dbo.doctor_weekly_schedules(doctor_id, room_id, day_of_week, start_time, end_time, slot_minutes, is_active)
  SELECT @d, @room, x.day_of_week, s.start_time, s.end_time, s.slot_minutes, 1
  FROM (VALUES (@dow1), (@dow2), (@dow3)) x(day_of_week)
  CROSS JOIN (SELECT TOP 1 * FROM @sess ORDER BY ABS(CHECKSUM(NEWID()))) s;

  IF @dow4 IS NOT NULL
  BEGIN
    INSERT INTO dbo.doctor_weekly_schedules(doctor_id, room_id, day_of_week, start_time, end_time, slot_minutes, is_active)
    VALUES (@d, @room, @dow4, '08:00', '11:00', 30, 1);
  END

  SET @i += 1;
END

/* ===== 11) Generate appointment slots for next 21 days ===== */
DECLARE @startDate date = CAST(SYSDATETIMEOFFSET() AT TIME ZONE 'SE Asia Standard Time' AS date);
DECLARE @days int = 21;
DECLARE @d0 int = 0;

WHILE @d0 < @days
BEGIN
  DECLARE @curDate date = DATEADD(day, @d0, @startDate);
  DECLARE @dw int = DATEPART(WEEKDAY, @curDate); -- depends on DATEFIRST; we'll map manually by DATENAME
  DECLARE @vnDow tinyint =
    CASE DATENAME(WEEKDAY, @curDate)
      WHEN N'Monday' THEN 2
      WHEN N'Tuesday' THEN 3
      WHEN N'Wednesday' THEN 4
      WHEN N'Thursday' THEN 5
      WHEN N'Friday' THEN 6
      WHEN N'Saturday' THEN 7
      ELSE 8
    END;

  DECLARE cur CURSOR LOCAL FAST_FORWARD FOR
    SELECT doctor_id, room_id, start_time, end_time, slot_minutes
    FROM dbo.doctor_weekly_schedules
    WHERE day_of_week = @vnDow AND is_active = 1;

  DECLARE @doc int, @room3 int, @st time(0), @et time(0), @mins int;
  OPEN cur;
  FETCH NEXT FROM cur INTO @doc, @room3, @st, @et, @mins;
  WHILE @@FETCH_STATUS = 0
  BEGIN
    DECLARE @t time(0) = @st;
    WHILE (DATEADD(minute, @mins, CAST(@t as datetime2)) <= CAST(@et as datetime2))
    BEGIN
      DECLARE @slotStart datetimeoffset(0) =
        TODATETIMEOFFSET(
          DATEADD(SECOND, DATEDIFF(SECOND, CAST('00:00:00' AS time(0)), @t), CAST(@curDate AS datetime2)),
          7*60
        );
      DECLARE @slotEnd   datetimeoffset(0) = DATEADD(minute, @mins, @slotStart);

      INSERT INTO dbo.appointment_slots(doctor_id, room_id, starts_at, ends_at, capacity, booked_count, is_active)
      VALUES (@doc, @room3, @slotStart, @slotEnd, 1, 0, 1);

      SET @t = CAST(DATEADD(minute, @mins, CAST(@t as datetime2)) AS time(0));
    END

    FETCH NEXT FROM cur INTO @doc, @room3, @st, @et, @mins;
  END
  CLOSE cur;
  DEALLOCATE cur;

  SET @d0 += 1;
END

/* ===== 12) Create appointments (use random slots) ===== */
DECLARE @apptCount int = 60;
DECLARE @k int = 1;

WHILE @k <= @apptCount
BEGIN
  DECLARE @slotId int = (SELECT TOP 1 id FROM dbo.appointment_slots WHERE booked_count = 0 ORDER BY NEWID());
  DECLARE @pId int = ((ABS(CHECKSUM(NEWID())) % @patientCount) + 1);
  DECLARE @docId int, @roomId int, @sa datetimeoffset(0), @ea datetimeoffset(0);
  SELECT @docId = doctor_id, @roomId = room_id, @sa = starts_at, @ea = ends_at
  FROM dbo.appointment_slots WHERE id = @slotId;

  DECLARE @serviceId int =
    (SELECT TOP 1 sv.id
     FROM dbo.services sv
     JOIN dbo.doctor_specialties ds ON ds.specialty_id = sv.specialty_id
     WHERE ds.doctor_id = @docId AND sv.is_active = 1
     ORDER BY NEWID());

  DECLARE @status nvarchar(20) =
    CASE
      WHEN (@k % 10) IN (1,2,3,4) THEN N'completed'
      WHEN (@k % 10) IN (5,6)     THEN N'confirmed'
      WHEN (@k % 10) = 7          THEN N'checked_in'
      WHEN (@k % 10) = 8          THEN N'cancelled'
      WHEN (@k % 10) = 9          THEN N'no_show'
      ELSE N'pending'
    END;

  INSERT INTO dbo.appointments(
    patient_id, doctor_id, service_id, slot_id, room_id, starts_at, ends_at, status,
    reason, note, created_by_account_id,
    confirmed_at, checked_in_at, completed_at, cancelled_at, cancel_reason
  )
  VALUES(
    @pId, @docId, @serviceId, @slotId, @roomId, @sa, @ea, @status,
    N'Triệu chứng: ho, sốt nhẹ / đau đầu / mệt mỏi (tùy ca)',
    CASE WHEN @status IN (N'completed', N'checked_in') THEN N'Đã tư vấn theo phác đồ.' ELSE NULL END,
    1,
    CASE WHEN @status IN (N'confirmed',N'checked_in',N'completed') THEN DATEADD(hour, -24, @sa) ELSE NULL END,
    CASE WHEN @status IN (N'checked_in',N'completed') THEN DATEADD(minute, -15, @sa) ELSE NULL END,
    CASE WHEN @status = N'completed' THEN DATEADD(minute, 5, @ea) ELSE NULL END,
    CASE WHEN @status = N'cancelled' THEN DATEADD(hour, -12, @sa) ELSE NULL END,
    CASE WHEN @status = N'cancelled' THEN N'Bận đột xuất' ELSE NULL END
  );

  UPDATE dbo.appointment_slots SET booked_count = 1 WHERE id = @slotId;

  SET @k += 1;
END

COMMIT TRANSACTION;
GO

PRINT N'Seed completed.';
GO

/* Quick counts */
SELECT N'accounts' AS [table], COUNT(*) AS cnt FROM dbo.accounts
UNION ALL SELECT N'doctors', COUNT(*) FROM dbo.doctors
UNION ALL SELECT N'patients', COUNT(*) FROM dbo.patients
UNION ALL SELECT N'specialties', COUNT(*) FROM dbo.specialties
UNION ALL SELECT N'services', COUNT(*) FROM dbo.services
UNION ALL SELECT N'rooms', COUNT(*) FROM dbo.rooms
UNION ALL SELECT N'weekly_schedules', COUNT(*) FROM dbo.doctor_weekly_schedules
UNION ALL SELECT N'slots', COUNT(*) FROM dbo.appointment_slots
UNION ALL SELECT N'appointments', COUNT(*) FROM dbo.appointments;
GO

