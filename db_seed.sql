/*
  BookingCare Clinic DB - SEED DATA (REALISTIC)
  Run AFTER `db_main.sql`.

  Goals:
  - No "demo" rows. Data resembles a real Vietnamese outpatient clinic.
  - "Relatively many" rows so admin dashboards look populated:
    - 1 clinic + legal docs
    - 3 roles (admin/doctor/patient)
    - 1 admin account
    - 40 doctor accounts + 40 doctors
    - 150 patient accounts + 150 patients
    - 12 specialties, 30 services, 12 rooms
    - weekly schedules for all doctors
    - appointment slots for next 21 days
    - ~260 appointments with invoices & payments for completed ones

  Notes:
  - Passwords are plaintext because your current backend login works with seeded plaintext.
  - Script is designed for a fresh DB (DROP/CREATE). It does NOT try to be idempotent.
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
(N'CLINIC-HN-01', N'Phòng khám đa khoa An Tâm', N'02473008899', N'lienhe@antamclinic.vn', N'25 P. Láng Hạ, Đống Đa, Hà Nội', N'Asia/Ho_Chi_Minh');

/* ===== 2) Legal docs (for mobile app) ===== */
INSERT INTO dbo.app_legal_documents(code, title, body_html, version)
VALUES
(N'TERMS', N'Điều khoản sử dụng', N'<p>Điều khoản sử dụng dịch vụ BookingCare Clinic.</p>', 1),
(N'PRIVACY', N'Chính sách bảo mật', N'<p>Chúng tôi tôn trọng và bảo vệ dữ liệu cá nhân của bạn.</p>', 1);

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
DECLARE @adminId int = 1;
SET IDENTITY_INSERT dbo.accounts ON;
INSERT INTO dbo.accounts(id, role_id, email, phone, password, full_name, status, is_email_verified)
VALUES
(@adminId, 1, N'admin@clinic.local', N'0901000001', N'admin123', N'Quản trị hệ thống', N'active', 1);

-- Doctors
DECLARE @doctorCount int = 40;
DECLARE @i int = 1;
WHILE @i <= @doctorCount
BEGIN
  DECLARE @accId int = 1000 + @i;
  DECLARE @isFemale bit = CASE WHEN (@i % 3) = 0 THEN 1 ELSE 0 END;
  DECLARE @full nvarchar(255) =
    CASE WHEN @isFemale = 1
      THEN (N'BS. ' + CHOOSE((@i % 10) + 1, N'Nguyễn',N'Trần',N'Lê',N'Phạm',N'Hoàng',N'Vũ',N'Đặng',N'Bùi',N'Đỗ',N'Ngô') + N' ' +
            CHOOSE((@i % 10) + 1, N'Thị',N'Ngọc',N'Minh',N'Hồng',N'Lan',N'Anh',N'Phương',N'Thu',N'Hà',N'Yến') + N' ' +
            CHOOSE((@i % 10) + 1, N'An',N'Bình',N'Châu',N'Dung',N'Giang',N'Hạnh',N'Khánh',N'Linh',N'Nga',N'Quỳnh'))
      ELSE (N'BS. ' + CHOOSE((@i % 10) + 1, N'Nguyễn',N'Trần',N'Lê',N'Phạm',N'Hoàng',N'Vũ',N'Đặng',N'Bùi',N'Đỗ',N'Ngô') + N' ' +
            CHOOSE((@i % 10) + 1, N'Văn',N'Quang',N'Đức',N'Hữu',N'Gia',N'Thành',N'Trung',N'Hoàng',N'Khải',N'Phúc') + N' ' +
            CHOOSE((@i % 10) + 1, N'Anh',N'Bảo',N'Cường',N'Duy',N'Hiếu',N'Khang',N'Long',N'Minh',N'Nam',N'Phát'))
    END;

  DECLARE @email nvarchar(255) = N'bs' + RIGHT(N'00' + CAST(@i AS nvarchar(10)), 2) + N'@antamclinic.vn';
  DECLARE @phone nvarchar(30)  = N'09' + RIGHT(N'00000000' + CAST(10000000 + @i AS nvarchar(20)), 8); -- unique

  INSERT INTO dbo.accounts(id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@accId, 2, @email, @phone, N'bs123456', @full, N'active', 1);

  SET @i += 1;
END

-- Patients
DECLARE @patientCount int = 150;
SET @i = 1;
WHILE @i <= @patientCount
BEGIN
  DECLARE @pAccId int = 2000 + @i;
  DECLARE @pFemale bit = CASE WHEN (@i % 2) = 0 THEN 1 ELSE 0 END;
  DECLARE @pfull nvarchar(255) =
    CASE WHEN @pFemale = 1
      THEN (CHOOSE((@i % 10) + 1, N'Nguyễn',N'Trần',N'Lê',N'Phạm',N'Hoàng',N'Vũ',N'Đặng',N'Bùi',N'Đỗ',N'Ngô') + N' ' +
            N'Thị ' + CHOOSE((@i % 10) + 1, N'An',N'Bích',N'Chi',N'Diệp',N'Giang',N'Hà',N'Khánh',N'Lan',N'Nga',N'Quỳnh'))
      ELSE (CHOOSE((@i % 10) + 1, N'Nguyễn',N'Trần',N'Lê',N'Phạm',N'Hoàng',N'Vũ',N'Đặng',N'Bùi',N'Đỗ',N'Ngô') + N' ' +
            N'Văn ' + CHOOSE((@i % 10) + 1, N'An',N'Bình',N'Chính',N'Dũng',N'Giáp',N'Hải',N'Khôi',N'Lâm',N'Nam',N'Quân'))
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
  DECLARE @gender nvarchar(20) = CASE WHEN (@i % 3)=0 THEN N'female' ELSE N'male' END;
  DECLARE @dob date = DATEFROMPARTS(1978 + (@i % 15), ((@i % 12) + 1), ((@i % 27) + 1));
  DECLARE @license nvarchar(100) = N'VN-' + RIGHT(N'0000' + CAST(@i AS nvarchar(10)), 4);
  DECLARE @bio nvarchar(max) = N'Khám và tư vấn theo hướng dẫn chuyên môn. Ưu tiên giải thích rõ ràng, theo dõi sát điều trị.';

  INSERT INTO dbo.doctors(
    id, account_id, full_name, gender, dob, phone, email, license_no, bio, avatar_url,
    rating, visits_count, room_location, schedule_text, education_json, certificates_json
  )
  SELECT
    @dId, a.id, a.full_name, @gender, @dob, a.phone, a.email, @license, @bio,
    CASE WHEN @gender = N'female' THEN @avatarFemale ELSE @avatarMale END,
    CAST(4.2 + ((@i % 8) * 0.1) AS decimal(2,1)),
    (30 + (@i * 7)) % 600,
    NULL,
    NULL,
    N'["ĐH Y Hà Nội","Chứng chỉ hành nghề"]',
    N'["Chứng chỉ CME","Chứng chỉ chuyên khoa"]'
  FROM dbo.accounts a WHERE a.id = @accId2;

  SET @i += 1;
END
SET IDENTITY_INSERT dbo.doctors OFF;

-- Map each doctor to 1 primary specialty + sometimes 1 secondary
DECLARE @specIds TABLE(id int);
INSERT INTO @specIds(id) SELECT id FROM dbo.specialties ORDER BY id;

SET @i = 1;
WHILE @i <= @doctorCount
BEGIN
  DECLARE @primarySpec int = ((@i - 1) % 12) + 1;
  INSERT INTO dbo.doctor_specialties(doctor_id, specialty_id) VALUES (@i, @primarySpec);

  IF (@i % 5) = 0
  BEGIN
    DECLARE @secondarySpec int = ((@i + 3) % 12) + 1;
    IF @secondarySpec <> @primarySpec
      INSERT INTO dbo.doctor_specialties(doctor_id, specialty_id) VALUES (@i, @secondarySpec);
  END

  SET @i += 1;
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
DECLARE @apptCount int = 260;
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

/* ===== 13) Invoices & Payments for completed appointments ===== */
DECLARE @completed TABLE(appt_id int, service_id int, price bigint);
INSERT INTO @completed(appt_id, service_id, price)
SELECT a.id, a.service_id, ISNULL(s.price_cents, 0)
FROM dbo.appointments a
LEFT JOIN dbo.services s ON s.id = a.service_id
WHERE a.status = N'completed';

DECLARE @aid int, @sid int, @price bigint;
DECLARE cur2 CURSOR LOCAL FAST_FORWARD FOR SELECT appt_id, service_id, price FROM @completed;
OPEN cur2;
FETCH NEXT FROM cur2 INTO @aid, @sid, @price;
WHILE @@FETCH_STATUS = 0
BEGIN
  INSERT INTO dbo.invoices(appointment_id, subtotal_cents, discount_cents, total_cents, currency, note)
  VALUES (@aid, @price, 0, @price, N'VND', N'Hóa đơn khám bệnh');

  DECLARE @invId int = SCOPE_IDENTITY();
  DECLARE @svcName nvarchar(255) = (SELECT TOP 1 name FROM dbo.services WHERE id = @sid);

  INSERT INTO dbo.invoice_items(invoice_id, service_id, name, qty, unit_price_cents, amount_cents)
  VALUES (@invId, @sid, ISNULL(@svcName, N'Dịch vụ khám'), 1, @price, @price);

  DECLARE @provider nvarchar(30);
  DECLARE @m int = (ABS(CHECKSUM(NEWID())) % 3);
  SET @provider = CASE @m WHEN 0 THEN N'cash' WHEN 1 THEN N'bank_transfer' ELSE N'momo' END;

  INSERT INTO dbo.payments(invoice_id, provider, status, amount_cents, currency, provider_txn_id, paid_at, meta)
  VALUES (
    @invId,
    @provider,
    N'paid',
    @price,
    N'VND',
    N'TXN-' + CAST(@invId AS nvarchar(20)),
    SYSDATETIMEOFFSET(),
    N'{"seed":true}'
  );

  FETCH NEXT FROM cur2 INTO @aid, @sid, @price;
END
CLOSE cur2;
DEALLOCATE cur2;

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
UNION ALL SELECT N'appointments', COUNT(*) FROM dbo.appointments
UNION ALL SELECT N'invoices', COUNT(*) FROM dbo.invoices
UNION ALL SELECT N'payments', COUNT(*) FROM dbo.payments;
GO

