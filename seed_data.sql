/*
 * BookingCare — Seed data cho phần BÁC SĨ
 *
 * Nội dung: chỉ insert các bảng liên quan trực tiếp tới việc hiển thị
 * danh sách bác sĩ (roles, accounts của bác sĩ, specialties, doctors,
 * doctor_specialties). Các phần khác (clinic, patient, services, rooms,
 * appointment slots, lịch hẹn...) đã được loại bỏ để file gọn và dễ chạy.
 *
 * File được lưu UTF-8 WITH BOM. Có thể chạy bằng:
 *   - Mở trong SSMS và Execute (F5)
 *   - sqlcmd -S localhost -U sa -P 123456 -d clinic_db -i seed_data.sql
 *     (SQL Server 2016+ tự đọc UTF-8 BOM; nếu version cũ thì thêm -f 65001)
 *
 * Có thể chạy lại nhiều lần an toàn: mọi INSERT đều bọc IF NOT EXISTS.
 */

USE clinic_db;
GO

SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_WARNINGS ON;
GO

BEGIN TRANSACTION;

/* ===== 1) Roles ===== */
SET IDENTITY_INSERT dbo.roles ON;
IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE id = 1)
  INSERT INTO dbo.roles (id, code, name, description, is_active, is_deleted)
  VALUES (1, N'admin', N'Administrator', N'Quản trị hệ thống', 1, 0);

IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE id = 2)
  INSERT INTO dbo.roles (id, code, name, description, is_active, is_deleted)
  VALUES (2, N'doctor', N'Bác sĩ', N'Bác sĩ khám bệnh', 1, 0);

IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE id = 3)
  INSERT INTO dbo.roles (id, code, name, description, is_active, is_deleted)
  VALUES (3, N'patient', N'Bệnh nhân', N'Người dùng đặt lịch', 1, 0);
SET IDENTITY_INSERT dbo.roles OFF;

DECLARE @RoleDoctorId int = 2;

/* ===== 2) Accounts cho 14 bác sĩ ===== */
DECLARE
  @DocAcc1  int = 101, @DocAcc2  int = 102, @DocAcc3  int = 103, @DocAcc4  int = 104,
  @DocAcc5  int = 105, @DocAcc6  int = 106, @DocAcc7  int = 107, @DocAcc8  int = 108,
  @DocAcc9  int = 109, @DocAcc10 int = 110, @DocAcc11 int = 111, @DocAcc12 int = 112,
  @DocAcc13 int = 113, @DocAcc14 int = 114;

SET IDENTITY_INSERT dbo.accounts ON;

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE id = @DocAcc1)
  INSERT INTO dbo.accounts (id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@DocAcc1, @RoleDoctorId, N'doctor.nguyenvana@antamclinic.local', N'0900000101', N'doctor123', N'BS. Nguyễn Văn A', N'active', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE id = @DocAcc2)
  INSERT INTO dbo.accounts (id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@DocAcc2, @RoleDoctorId, N'doctor.tranthib@antamclinic.local', N'0900000102', N'doctor123', N'BS. Trần Thị B', N'active', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE id = @DocAcc3)
  INSERT INTO dbo.accounts (id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@DocAcc3, @RoleDoctorId, N'doctor.lequangc@antamclinic.local', N'0900000103', N'doctor123', N'BS. Lê Quang C', N'active', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE id = @DocAcc4)
  INSERT INTO dbo.accounts (id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@DocAcc4, @RoleDoctorId, N'doctor.phamthid@antamclinic.local', N'0900000104', N'doctor123', N'BS. Phạm Thị D', N'active', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE id = @DocAcc5)
  INSERT INTO dbo.accounts (id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@DocAcc5, @RoleDoctorId, N'doctor.dangvane@antamclinic.local', N'0900000105', N'doctor123', N'BS. Đặng Văn E', N'active', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE id = @DocAcc6)
  INSERT INTO dbo.accounts (id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@DocAcc6, @RoleDoctorId, N'doctor.vothif@antamclinic.local', N'0900000106', N'doctor123', N'BS. Võ Thị F', N'active', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE id = @DocAcc7)
  INSERT INTO dbo.accounts (id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@DocAcc7, @RoleDoctorId, N'doctor.hoangvang@antamclinic.local', N'0900000107', N'doctor123', N'BS. Hoàng Văn G', N'active', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE id = @DocAcc8)
  INSERT INTO dbo.accounts (id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@DocAcc8, @RoleDoctorId, N'doctor.ngothih@antamclinic.local', N'0900000108', N'doctor123', N'BS. Ngô Thị H', N'active', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE id = @DocAcc9)
  INSERT INTO dbo.accounts (id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@DocAcc9, @RoleDoctorId, N'doctor.nguyenvani@antamclinic.local', N'0900000109', N'doctor123', N'BS. Nguyễn Văn I', N'active', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE id = @DocAcc10)
  INSERT INTO dbo.accounts (id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@DocAcc10, @RoleDoctorId, N'doctor.lethij@antamclinic.local', N'0900000110', N'doctor123', N'BS. Lê Thị J', N'active', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE id = @DocAcc11)
  INSERT INTO dbo.accounts (id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@DocAcc11, @RoleDoctorId, N'doctor.tranvank@antamclinic.local', N'0900000111', N'doctor123', N'BS. Trần Văn K', N'active', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE id = @DocAcc12)
  INSERT INTO dbo.accounts (id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@DocAcc12, @RoleDoctorId, N'doctor.phamthil@antamclinic.local', N'0900000112', N'doctor123', N'BS. Phạm Thị L', N'active', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE id = @DocAcc13)
  INSERT INTO dbo.accounts (id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@DocAcc13, @RoleDoctorId, N'doctor.buivanm@antamclinic.local', N'0900000113', N'doctor123', N'BS. Bùi Văn M', N'active', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE id = @DocAcc14)
  INSERT INTO dbo.accounts (id, role_id, email, phone, password, full_name, status, is_email_verified)
  VALUES (@DocAcc14, @RoleDoctorId, N'doctor.dothin@antamclinic.local', N'0900000114', N'doctor123', N'BS. Đỗ Thị N', N'active', 1);

SET IDENTITY_INSERT dbo.accounts OFF;

/* ===== 3) Specialties ===== */
DECLARE
  @SpecInternal int = 1,  @SpecPeds int = 2,    @SpecEnt int = 3,
  @SpecDerm int = 4,      @SpecCardio int = 5,  @SpecObgyn int = 6,
  @SpecEye int = 7,       @SpecDental int = 8,  @SpecOrtho int = 9,
  @SpecGastro int = 10,   @SpecEndo int = 11,   @SpecNeuro int = 12;

SET IDENTITY_INSERT dbo.specialties ON;

IF NOT EXISTS (SELECT 1 FROM dbo.specialties WHERE id = @SpecInternal)
  INSERT INTO dbo.specialties (id, code, name, description)
  VALUES (@SpecInternal, N'INT', N'Nội tổng quát', N'Khám nội, kiểm tra tổng quát');

IF NOT EXISTS (SELECT 1 FROM dbo.specialties WHERE id = @SpecPeds)
  INSERT INTO dbo.specialties (id, code, name, description)
  VALUES (@SpecPeds, N'PED', N'Nhi khoa', N'Khám và tư vấn sức khỏe trẻ em');

IF NOT EXISTS (SELECT 1 FROM dbo.specialties WHERE id = @SpecEnt)
  INSERT INTO dbo.specialties (id, code, name, description)
  VALUES (@SpecEnt, N'ENT', N'Tai Mũi Họng', N'Khám Tai-Mũi-Họng');

IF NOT EXISTS (SELECT 1 FROM dbo.specialties WHERE id = @SpecDerm)
  INSERT INTO dbo.specialties (id, code, name, description)
  VALUES (@SpecDerm, N'DERM', N'Da liễu', N'Khám và điều trị bệnh da');

IF NOT EXISTS (SELECT 1 FROM dbo.specialties WHERE id = @SpecCardio)
  INSERT INTO dbo.specialties (id, code, name, description)
  VALUES (@SpecCardio, N'CARD', N'Tim mạch', N'Khám tim mạch, huyết áp');

IF NOT EXISTS (SELECT 1 FROM dbo.specialties WHERE id = @SpecObgyn)
  INSERT INTO dbo.specialties (id, code, name, description)
  VALUES (@SpecObgyn, N'OBGYN', N'Sản - Phụ khoa', N'Khám phụ khoa, tư vấn sản');

IF NOT EXISTS (SELECT 1 FROM dbo.specialties WHERE id = @SpecEye)
  INSERT INTO dbo.specialties (id, code, name, description)
  VALUES (@SpecEye, N'EYE', N'Mắt', N'Khám mắt, đo thị lực');

IF NOT EXISTS (SELECT 1 FROM dbo.specialties WHERE id = @SpecDental)
  INSERT INTO dbo.specialties (id, code, name, description)
  VALUES (@SpecDental, N'DENT', N'Răng - Hàm - Mặt', N'Khám răng, vệ sinh răng miệng');

IF NOT EXISTS (SELECT 1 FROM dbo.specialties WHERE id = @SpecOrtho)
  INSERT INTO dbo.specialties (id, code, name, description)
  VALUES (@SpecOrtho, N'ORTHO', N'Cơ - Xương - Khớp', N'Khám cơ xương khớp');

IF NOT EXISTS (SELECT 1 FROM dbo.specialties WHERE id = @SpecGastro)
  INSERT INTO dbo.specialties (id, code, name, description)
  VALUES (@SpecGastro, N'GI', N'Tiêu hóa', N'Khám tiêu hóa');

IF NOT EXISTS (SELECT 1 FROM dbo.specialties WHERE id = @SpecEndo)
  INSERT INTO dbo.specialties (id, code, name, description)
  VALUES (@SpecEndo, N'ENDO', N'Nội tiết', N'Khám nội tiết (đái tháo đường, tuyến giáp)');

IF NOT EXISTS (SELECT 1 FROM dbo.specialties WHERE id = @SpecNeuro)
  INSERT INTO dbo.specialties (id, code, name, description)
  VALUES (@SpecNeuro, N'NEURO', N'Thần kinh', N'Khám thần kinh');

SET IDENTITY_INSERT dbo.specialties OFF;

/* ===== 4) Doctors ===== */
DECLARE
  @Dr1 int = 1,  @Dr2 int = 2,  @Dr3 int = 3,  @Dr4 int = 4,  @Dr5 int = 5,
  @Dr6 int = 6,  @Dr7 int = 7,  @Dr8 int = 8,  @Dr9 int = 9,  @Dr10 int = 10,
  @Dr11 int = 11, @Dr12 int = 12, @Dr13 int = 13, @Dr14 int = 14;

SET IDENTITY_INSERT dbo.doctors ON;

IF NOT EXISTS (SELECT 1 FROM dbo.doctors WHERE id = @Dr1)
  INSERT INTO dbo.doctors (id, account_id, full_name, gender, phone, email, license_no, bio)
  VALUES (@Dr1, @DocAcc1, N'BS. Nguyễn Văn A', N'male', N'0900000101', N'doctor.nguyenvana@antamclinic.local', N'AT-INT-0001', N'Nội tổng quát');

IF NOT EXISTS (SELECT 1 FROM dbo.doctors WHERE id = @Dr2)
  INSERT INTO dbo.doctors (id, account_id, full_name, gender, phone, email, license_no, bio)
  VALUES (@Dr2, @DocAcc2, N'BS. Trần Thị B', N'female', N'0900000102', N'doctor.tranthib@antamclinic.local', N'AT-PED-0001', N'Nhi khoa');

IF NOT EXISTS (SELECT 1 FROM dbo.doctors WHERE id = @Dr3)
  INSERT INTO dbo.doctors (id, account_id, full_name, gender, phone, email, license_no, bio)
  VALUES (@Dr3, @DocAcc3, N'BS. Lê Quang C', N'male', N'0900000103', N'doctor.lequangc@antamclinic.local', N'AT-ENT-0001', N'Tai Mũi Họng');

IF NOT EXISTS (SELECT 1 FROM dbo.doctors WHERE id = @Dr4)
  INSERT INTO dbo.doctors (id, account_id, full_name, gender, phone, email, license_no, bio)
  VALUES (@Dr4, @DocAcc4, N'BS. Phạm Thị D', N'female', N'0900000104', N'doctor.phamthid@antamclinic.local', N'AT-DERM-0001', N'Da liễu');

IF NOT EXISTS (SELECT 1 FROM dbo.doctors WHERE id = @Dr5)
  INSERT INTO dbo.doctors (id, account_id, full_name, gender, phone, email, license_no, bio)
  VALUES (@Dr5, @DocAcc5, N'BS. Đặng Văn E', N'male', N'0900000105', N'doctor.dangvane@antamclinic.local', N'AT-CARD-0001', N'Tim mạch');

IF NOT EXISTS (SELECT 1 FROM dbo.doctors WHERE id = @Dr6)
  INSERT INTO dbo.doctors (id, account_id, full_name, gender, phone, email, license_no, bio)
  VALUES (@Dr6, @DocAcc6, N'BS. Võ Thị F', N'female', N'0900000106', N'doctor.vothif@antamclinic.local', N'AT-INT-0002', N'Nội tổng quát');

IF NOT EXISTS (SELECT 1 FROM dbo.doctors WHERE id = @Dr7)
  INSERT INTO dbo.doctors (id, account_id, full_name, gender, phone, email, license_no, bio)
  VALUES (@Dr7, @DocAcc7, N'BS. Hoàng Văn G', N'male', N'0900000107', N'doctor.hoangvang@antamclinic.local', N'AT-PED-0002', N'Nhi khoa');

IF NOT EXISTS (SELECT 1 FROM dbo.doctors WHERE id = @Dr8)
  INSERT INTO dbo.doctors (id, account_id, full_name, gender, phone, email, license_no, bio)
  VALUES (@Dr8, @DocAcc8, N'BS. Ngô Thị H', N'female', N'0900000108', N'doctor.ngothih@antamclinic.local', N'AT-ENT-0002', N'Tai Mũi Họng');

IF NOT EXISTS (SELECT 1 FROM dbo.doctors WHERE id = @Dr9)
  INSERT INTO dbo.doctors (id, account_id, full_name, gender, phone, email, license_no, bio)
  VALUES (@Dr9, @DocAcc9, N'BS. Nguyễn Văn I', N'male', N'0900000109', N'doctor.nguyenvani@antamclinic.local', N'AT-DERM-0002', N'Da liễu');

IF NOT EXISTS (SELECT 1 FROM dbo.doctors WHERE id = @Dr10)
  INSERT INTO dbo.doctors (id, account_id, full_name, gender, phone, email, license_no, bio)
  VALUES (@Dr10, @DocAcc10, N'BS. Lê Thị J', N'female', N'0900000110', N'doctor.lethij@antamclinic.local', N'AT-CARD-0002', N'Tim mạch');

IF NOT EXISTS (SELECT 1 FROM dbo.doctors WHERE id = @Dr11)
  INSERT INTO dbo.doctors (id, account_id, full_name, gender, phone, email, license_no, bio)
  VALUES (@Dr11, @DocAcc11, N'BS. Trần Văn K', N'male', N'0900000111', N'doctor.tranvank@antamclinic.local', N'AT-OBGYN-0001', N'Sản - Phụ khoa');

IF NOT EXISTS (SELECT 1 FROM dbo.doctors WHERE id = @Dr12)
  INSERT INTO dbo.doctors (id, account_id, full_name, gender, phone, email, license_no, bio)
  VALUES (@Dr12, @DocAcc12, N'BS. Phạm Thị L', N'female', N'0900000112', N'doctor.phamthil@antamclinic.local', N'AT-EYE-0001', N'Khám mắt');

IF NOT EXISTS (SELECT 1 FROM dbo.doctors WHERE id = @Dr13)
  INSERT INTO dbo.doctors (id, account_id, full_name, gender, phone, email, license_no, bio)
  VALUES (@Dr13, @DocAcc13, N'BS. Bùi Văn M', N'male', N'0900000113', N'doctor.buivanm@antamclinic.local', N'AT-GI-0001', N'Tiêu hóa');

IF NOT EXISTS (SELECT 1 FROM dbo.doctors WHERE id = @Dr14)
  INSERT INTO dbo.doctors (id, account_id, full_name, gender, phone, email, license_no, bio)
  VALUES (@Dr14, @DocAcc14, N'BS. Đỗ Thị N', N'female', N'0900000114', N'doctor.dothin@antamclinic.local', N'AT-ENDO-0001', N'Nội tiết');

SET IDENTITY_INSERT dbo.doctors OFF;

/* ===== 5) Doctor ↔ Specialty (mỗi bác sĩ 1 chuyên khoa chính) ===== */
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr1 AND specialty_id = @SpecInternal)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr1, @SpecInternal);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr2 AND specialty_id = @SpecPeds)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr2, @SpecPeds);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr3 AND specialty_id = @SpecEnt)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr3, @SpecEnt);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr4 AND specialty_id = @SpecDerm)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr4, @SpecDerm);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr5 AND specialty_id = @SpecCardio)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr5, @SpecCardio);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr6 AND specialty_id = @SpecInternal)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr6, @SpecInternal);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr7 AND specialty_id = @SpecPeds)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr7, @SpecPeds);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr8 AND specialty_id = @SpecEnt)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr8, @SpecEnt);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr9 AND specialty_id = @SpecDerm)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr9, @SpecDerm);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr10 AND specialty_id = @SpecCardio)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr10, @SpecCardio);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr11 AND specialty_id = @SpecObgyn)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr11, @SpecObgyn);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr12 AND specialty_id = @SpecEye)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr12, @SpecEye);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr13 AND specialty_id = @SpecGastro)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr13, @SpecGastro);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr14 AND specialty_id = @SpecEndo)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr14, @SpecEndo);

/* Một số bác sĩ có thêm chuyên khoa phụ */
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr1 AND specialty_id = @SpecGastro)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr1, @SpecGastro);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr1 AND specialty_id = @SpecEndo)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr1, @SpecEndo);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr3 AND specialty_id = @SpecDerm)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr3, @SpecDerm);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr4 AND specialty_id = @SpecEnt)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr4, @SpecEnt);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr5 AND specialty_id = @SpecEndo)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr5, @SpecEndo);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr10 AND specialty_id = @SpecInternal)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr10, @SpecInternal);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr10 AND specialty_id = @SpecEndo)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr10, @SpecEndo);
IF NOT EXISTS (SELECT 1 FROM dbo.doctor_specialties WHERE doctor_id = @Dr13 AND specialty_id = @SpecInternal)
  INSERT INTO dbo.doctor_specialties (doctor_id, specialty_id) VALUES (@Dr13, @SpecInternal);

COMMIT TRANSACTION;
GO

PRINT N'Seed bác sĩ hoàn tất: 14 doctors, 12 specialties (Unicode UTF-8 BOM).';
GO

/* Kiểm tra nhanh */
SELECT d.id AS doctor_id,
       d.full_name,
       STRING_AGG(s.name, N', ') WITHIN GROUP (ORDER BY s.name) AS specialties
FROM dbo.doctors d
LEFT JOIN dbo.doctor_specialties ds ON ds.doctor_id = d.id
LEFT JOIN dbo.specialties s ON s.id = ds.specialty_id
GROUP BY d.id, d.full_name
ORDER BY d.id;
