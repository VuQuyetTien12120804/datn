IF DB_ID(N'clinic_db') IS NULL
  CREATE DATABASE clinic_db;
GO
USE clinic_db;
GO

/*
  Single-clinic schema (Direction A):
  - Keep `dbo.clinics` for metadata.
  - Do NOT store `clinic_id` in child tables to avoid cross-table mismatch risks.
*/

/* ===== 1) clinics (metadata) ===== */
CREATE TABLE dbo.clinics (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_clinics PRIMARY KEY,
  code nvarchar(50) NULL CONSTRAINT UQ_clinics_code UNIQUE,
  name nvarchar(255) NOT NULL,
  phone nvarchar(30) NULL,
  email nvarchar(255) NULL,
  address nvarchar(500) NULL,
  timezone nvarchar(64) NOT NULL CONSTRAINT DF_clinics_timezone DEFAULT N'Asia/Ho_Chi_Minh',
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_clinics_created_at DEFAULT SYSDATETIMEOFFSET(),
  updated_at datetimeoffset(0) NOT NULL CONSTRAINT DF_clinics_updated_at DEFAULT SYSDATETIMEOFFSET()
);
GO

CREATE TABLE dbo.roles (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_roles PRIMARY KEY,
  code nvarchar(50) NOT NULL CONSTRAINT UQ_roles_code UNIQUE,
  name nvarchar(100) NOT NULL,
  description nvarchar(500) NULL,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_roles_created_at DEFAULT SYSDATETIMEOFFSET(),
  updated_at datetimeoffset(0) NOT NULL CONSTRAINT DF_roles_updated_at DEFAULT SYSDATETIMEOFFSET(),
  is_active bit NOT NULL CONSTRAINT DF_roles_is_active DEFAULT 1,
  is_deleted bit NOT NULL CONSTRAINT DF_roles_is_deleted DEFAULT 0
);
GO

/* ===== 2) accounts / roles ===== */
CREATE TABLE dbo.accounts (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_accounts PRIMARY KEY,
  role_id int NOT NULL,
  email nvarchar(255) NULL,
  phone nvarchar(30) NULL,
  password nvarchar(255) NULL,
  full_name nvarchar(255) NOT NULL,
  avatar_url nvarchar(500) NULL,
  status nvarchar(20) NOT NULL CONSTRAINT DF_accounts_status DEFAULT N'active',
  is_email_verified bit NOT NULL CONSTRAINT DF_accounts_is_email_verified DEFAULT 0,
  last_login_at datetimeoffset(0) NULL,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_accounts_created_at DEFAULT SYSDATETIMEOFFSET(),
  updated_at datetimeoffset(0) NOT NULL CONSTRAINT DF_accounts_updated_at DEFAULT SYSDATETIMEOFFSET(),
  CONSTRAINT CK_accounts_email_or_phone CHECK (email IS NOT NULL OR phone IS NOT NULL),
  CONSTRAINT CK_accounts_status CHECK (status IN (N'active', N'inactive', N'blocked')),
  CONSTRAINT FK_accounts_role FOREIGN KEY (role_id) REFERENCES dbo.roles(id) ON DELETE NO ACTION
);
GO

/* ===== 3) specialties / doctors / patients ===== */
CREATE TABLE dbo.specialties (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_specialties PRIMARY KEY,
  code nvarchar(50) NULL,
  name nvarchar(255) NOT NULL,
  description nvarchar(max) NULL,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_specialties_created_at DEFAULT SYSDATETIMEOFFSET()
);
GO

CREATE TABLE dbo.doctors (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_doctors PRIMARY KEY,
  account_id int NULL,
  full_name nvarchar(255) NOT NULL,
  gender nvarchar(20) NOT NULL CONSTRAINT DF_doctors_gender DEFAULT N'unknown',
  dob date NULL,
  phone nvarchar(30) NULL,
  email nvarchar(255) NULL,
  license_no nvarchar(100) NULL,
  bio nvarchar(max) NULL,
  avatar_url nvarchar(500) NULL,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_doctors_created_at DEFAULT SYSDATETIMEOFFSET(),
  updated_at datetimeoffset(0) NOT NULL CONSTRAINT DF_doctors_updated_at DEFAULT SYSDATETIMEOFFSET(),
  CONSTRAINT CK_doctors_gender CHECK (gender IN (N'unknown', N'male', N'female', N'other')),
  CONSTRAINT FK_doctors_account FOREIGN KEY (account_id) REFERENCES dbo.accounts(id) ON DELETE SET NULL
);
GO

CREATE TABLE dbo.patients (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_patients PRIMARY KEY,
  account_id int NULL,
  full_name nvarchar(255) NOT NULL,
  dob date NULL,
  gender nvarchar(20) NOT NULL CONSTRAINT DF_patients_gender DEFAULT N'unknown',
  phone nvarchar(30) NULL,
  email nvarchar(255) NULL,
  address nvarchar(500) NULL,
  insurance_no nvarchar(100) NULL,
  emergency_contact_name nvarchar(255) NULL,
  emergency_contact_phone nvarchar(30) NULL,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_patients_created_at DEFAULT SYSDATETIMEOFFSET(),
  updated_at datetimeoffset(0) NOT NULL CONSTRAINT DF_patients_updated_at DEFAULT SYSDATETIMEOFFSET(),
  CONSTRAINT CK_patients_gender CHECK (gender IN (N'unknown', N'male', N'female', N'other')),
  CONSTRAINT FK_patients_account FOREIGN KEY (account_id) REFERENCES dbo.accounts(id) ON DELETE SET NULL
);
GO

CREATE TABLE dbo.doctor_specialties (
  doctor_id int NOT NULL,
  specialty_id int NOT NULL,
  CONSTRAINT PK_doctor_specialties PRIMARY KEY (doctor_id, specialty_id),
  -- Tránh multiple cascade paths: chỉ cascade 1 phía
  CONSTRAINT FK_doctor_specialties_doctor FOREIGN KEY (doctor_id) REFERENCES dbo.doctors(id) ON DELETE CASCADE,
  CONSTRAINT FK_doctor_specialties_specialty FOREIGN KEY (specialty_id) REFERENCES dbo.specialties(id) ON DELETE NO ACTION
);
GO

/* ===== 4) services / rooms ===== */
CREATE TABLE dbo.services (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_services PRIMARY KEY,
  specialty_id int NULL,
  code nvarchar(50) NULL,
  name nvarchar(255) NOT NULL,
  description nvarchar(max) NULL,
  duration_minutes int NOT NULL CONSTRAINT DF_services_duration DEFAULT 15,
  price_cents bigint NOT NULL CONSTRAINT DF_services_price DEFAULT 0,
  is_active bit NOT NULL CONSTRAINT DF_services_is_active DEFAULT 1,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_services_created_at DEFAULT SYSDATETIMEOFFSET(),
  updated_at datetimeoffset(0) NOT NULL CONSTRAINT DF_services_updated_at DEFAULT SYSDATETIMEOFFSET(),
  CONSTRAINT CK_services_duration CHECK (duration_minutes BETWEEN 5 AND 1440),
  CONSTRAINT FK_services_specialty FOREIGN KEY (specialty_id) REFERENCES dbo.specialties(id) ON DELETE SET NULL
);
GO

CREATE TABLE dbo.rooms (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_rooms PRIMARY KEY,
  code nvarchar(50) NULL,
  name nvarchar(255) NOT NULL,
  floor nvarchar(50) NULL,
  note nvarchar(max) NULL,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_rooms_created_at DEFAULT SYSDATETIMEOFFSET(),
  updated_at datetimeoffset(0) NOT NULL CONSTRAINT DF_rooms_updated_at DEFAULT SYSDATETIMEOFFSET()
);
GO

/* ===== 5) scheduling ===== */
CREATE TABLE dbo.doctor_working_hours (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_doctor_working_hours PRIMARY KEY,
  doctor_id int NOT NULL,
  room_id int NULL,
  day_of_week tinyint NOT NULL,
  start_time time(0) NOT NULL,
  end_time time(0) NOT NULL,
  slot_minutes int NOT NULL CONSTRAINT DF_dwh_slot_minutes DEFAULT 15,
  is_active bit NOT NULL CONSTRAINT DF_dwh_is_active DEFAULT 1,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_dwh_created_at DEFAULT SYSDATETIMEOFFSET(),
  CONSTRAINT CK_dwh_day CHECK (day_of_week BETWEEN 0 AND 6),
  CONSTRAINT CK_dwh_slot_minutes CHECK (slot_minutes BETWEEN 5 AND 240),
  CONSTRAINT CK_dwh_time_range CHECK (start_time < end_time),
  CONSTRAINT FK_dwh_doctor FOREIGN KEY (doctor_id) REFERENCES dbo.doctors(id) ON DELETE CASCADE,
  CONSTRAINT FK_dwh_room FOREIGN KEY (room_id) REFERENCES dbo.rooms(id) ON DELETE SET NULL
);
GO

CREATE TABLE dbo.doctor_time_off (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_doctor_time_off PRIMARY KEY,
  doctor_id int NOT NULL,
  starts_at datetimeoffset(0) NOT NULL,
  ends_at datetimeoffset(0) NOT NULL,
  reason nvarchar(500) NULL,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_dto_created_at DEFAULT SYSDATETIMEOFFSET(),
  CONSTRAINT CK_dto_range CHECK (starts_at < ends_at),
  CONSTRAINT FK_dto_doctor FOREIGN KEY (doctor_id) REFERENCES dbo.doctors(id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.appointment_slots (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_appointment_slots PRIMARY KEY,
  doctor_id int NOT NULL,
  room_id int NULL,
  starts_at datetimeoffset(0) NOT NULL,
  ends_at datetimeoffset(0) NOT NULL,
  capacity int NOT NULL CONSTRAINT DF_slots_capacity DEFAULT 1,
  booked_count int NOT NULL CONSTRAINT DF_slots_booked DEFAULT 0,
  is_active bit NOT NULL CONSTRAINT DF_slots_is_active DEFAULT 1,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_slots_created_at DEFAULT SYSDATETIMEOFFSET(),
  CONSTRAINT CK_slots_range CHECK (starts_at < ends_at),
  CONSTRAINT CK_slots_capacity CHECK (capacity BETWEEN 1 AND 50),
  CONSTRAINT CK_slots_booked CHECK (booked_count >= 0),
  CONSTRAINT CK_slots_booked_le_capacity CHECK (booked_count <= capacity),
  CONSTRAINT FK_slots_doctor FOREIGN KEY (doctor_id) REFERENCES dbo.doctors(id) ON DELETE CASCADE,
  CONSTRAINT FK_slots_room FOREIGN KEY (room_id) REFERENCES dbo.rooms(id) ON DELETE SET NULL
);
GO

/* ===== 6) appointments ===== */
CREATE TABLE dbo.appointments (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_appointments PRIMARY KEY,
  patient_id int NOT NULL,
  doctor_id int NOT NULL,
  service_id int NULL,
  slot_id int NULL,
  room_id int NULL,
  starts_at datetimeoffset(0) NOT NULL,
  ends_at datetimeoffset(0) NOT NULL,
  status nvarchar(20) NOT NULL CONSTRAINT DF_appointments_status DEFAULT N'pending',
  reason nvarchar(max) NULL,
  note nvarchar(max) NULL,
  cancel_reason nvarchar(500) NULL,
  cancelled_at datetimeoffset(0) NULL,
  confirmed_at datetimeoffset(0) NULL,
  checked_in_at datetimeoffset(0) NULL,
  completed_at datetimeoffset(0) NULL,
  created_by_account_id int NULL,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_appointments_created_at DEFAULT SYSDATETIMEOFFSET(),
  updated_at datetimeoffset(0) NOT NULL CONSTRAINT DF_appointments_updated_at DEFAULT SYSDATETIMEOFFSET(),
  CONSTRAINT CK_appt_range CHECK (starts_at < ends_at),
  CONSTRAINT CK_appt_status CHECK (status IN (N'pending',N'confirmed',N'checked_in',N'completed',N'cancelled',N'no_show')),
  CONSTRAINT FK_appt_patient FOREIGN KEY (patient_id) REFERENCES dbo.patients(id) ON DELETE NO ACTION,
  CONSTRAINT FK_appt_doctor FOREIGN KEY (doctor_id) REFERENCES dbo.doctors(id) ON DELETE NO ACTION,
  CONSTRAINT FK_appt_service FOREIGN KEY (service_id) REFERENCES dbo.services(id) ON DELETE SET NULL,
  CONSTRAINT FK_appt_slot FOREIGN KEY (slot_id) REFERENCES dbo.appointment_slots(id) ON DELETE SET NULL,
  CONSTRAINT FK_appt_room FOREIGN KEY (room_id) REFERENCES dbo.rooms(id) ON DELETE SET NULL,
  CONSTRAINT FK_appt_created_by_account FOREIGN KEY (created_by_account_id) REFERENCES dbo.accounts(id) ON DELETE SET NULL
);
GO


/* ===== 7) invoices / payments ===== */
CREATE TABLE dbo.invoices (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_invoices PRIMARY KEY,
  appointment_id int NULL,
  subtotal_cents bigint NOT NULL CONSTRAINT DF_invoices_subtotal DEFAULT 0,
  discount_cents bigint NOT NULL CONSTRAINT DF_invoices_discount DEFAULT 0,
  total_cents bigint NOT NULL CONSTRAINT DF_invoices_total DEFAULT 0,
  currency nvarchar(10) NOT NULL CONSTRAINT DF_invoices_currency DEFAULT N'VND',
  note nvarchar(max) NULL,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_invoices_created_at DEFAULT SYSDATETIMEOFFSET(),
  CONSTRAINT FK_invoices_appointment FOREIGN KEY (appointment_id) REFERENCES dbo.appointments(id) ON DELETE SET NULL
);
GO

CREATE TABLE dbo.invoice_items (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_invoice_items PRIMARY KEY,
  invoice_id int NOT NULL,
  service_id int NULL,
  name nvarchar(255) NOT NULL,
  qty int NOT NULL CONSTRAINT DF_invoice_items_qty DEFAULT 1,
  unit_price_cents bigint NOT NULL CONSTRAINT DF_invoice_items_unit DEFAULT 0,
  amount_cents bigint NOT NULL CONSTRAINT DF_invoice_items_amount DEFAULT 0,
  CONSTRAINT CK_invoice_items_qty CHECK (qty > 0),
  CONSTRAINT FK_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES dbo.invoices(id) ON DELETE CASCADE,
  CONSTRAINT FK_invoice_items_service FOREIGN KEY (service_id) REFERENCES dbo.services(id) ON DELETE SET NULL
);
GO

CREATE TABLE dbo.payments (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_payments PRIMARY KEY,
  invoice_id int NULL,
  provider nvarchar(30) NOT NULL CONSTRAINT DF_payments_provider DEFAULT N'cash',
  status nvarchar(20) NOT NULL CONSTRAINT DF_payments_status DEFAULT N'pending',
  amount_cents bigint NOT NULL CONSTRAINT DF_payments_amount DEFAULT 0,
  currency nvarchar(10) NOT NULL CONSTRAINT DF_payments_currency DEFAULT N'VND',
  provider_txn_id nvarchar(100) NULL,
  paid_at datetimeoffset(0) NULL,
  meta nvarchar(max) NOT NULL CONSTRAINT DF_payments_meta DEFAULT N'{}',
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_payments_created_at DEFAULT SYSDATETIMEOFFSET(),
  CONSTRAINT CK_payments_provider CHECK (provider IN (N'cash',N'vnpay',N'momo',N'zalopay',N'bank_transfer',N'stripe',N'other')),
  CONSTRAINT CK_payments_status CHECK (status IN (N'pending',N'paid',N'failed',N'refunded',N'cancelled')),
  CONSTRAINT CK_payments_meta_isjson CHECK (ISJSON(meta) = 1),
  CONSTRAINT FK_payments_invoice FOREIGN KEY (invoice_id) REFERENCES dbo.invoices(id) ON DELETE SET NULL
);
GO

/* ===== 9) chatbot ===== */
CREATE TABLE dbo.chatbot_sessions (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_chatbot_sessions PRIMARY KEY,
  account_id int NULL,
  patient_id int NULL,
  channel nvarchar(20) NOT NULL CONSTRAINT DF_chatbot_sessions_channel DEFAULT N'web',
  title nvarchar(255) NULL,
  is_closed bit NOT NULL CONSTRAINT DF_chatbot_sessions_is_closed DEFAULT 0,
  closed_at datetimeoffset(0) NULL,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_chatbot_sessions_created_at DEFAULT SYSDATETIMEOFFSET(),
  updated_at datetimeoffset(0) NOT NULL CONSTRAINT DF_chatbot_sessions_updated_at DEFAULT SYSDATETIMEOFFSET(),
  CONSTRAINT CK_chatbot_sessions_channel CHECK (channel IN (N'web',N'mobile',N'zalo',N'facebook',N'other')),
  CONSTRAINT FK_chatbot_sessions_account FOREIGN KEY (account_id) REFERENCES dbo.accounts(id) ON DELETE SET NULL,
  CONSTRAINT FK_chatbot_sessions_patient FOREIGN KEY (patient_id) REFERENCES dbo.patients(id) ON DELETE SET NULL
);
GO

CREATE TABLE dbo.chatbot_messages (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_chatbot_messages PRIMARY KEY,
  session_id int NOT NULL,
  sender nvarchar(20) NOT NULL,
  content nvarchar(max) NOT NULL,
  model nvarchar(100) NULL,
  prompt_tokens int NULL,
  completion_tokens int NULL,
  latency_ms int NULL,
  meta nvarchar(max) NOT NULL CONSTRAINT DF_chatbot_messages_meta DEFAULT N'{}',
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_chatbot_messages_created_at DEFAULT SYSDATETIMEOFFSET(),
  CONSTRAINT CK_chatbot_messages_sender CHECK (sender IN (N'user',N'assistant',N'system',N'tool')),
  CONSTRAINT CK_chatbot_messages_meta_isjson CHECK (ISJSON(meta) = 1),
  CONSTRAINT FK_chatbot_messages_session FOREIGN KEY (session_id) REFERENCES dbo.chatbot_sessions(id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.chatbot_tool_calls (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_chatbot_tool_calls PRIMARY KEY,
  message_id int NOT NULL,
  tool_name nvarchar(200) NOT NULL,
  input nvarchar(max) NOT NULL,
  output nvarchar(max) NULL,
  is_error bit NOT NULL CONSTRAINT DF_chatbot_tool_calls_is_error DEFAULT 0,
  error nvarchar(max) NULL,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_chatbot_tool_calls_created_at DEFAULT SYSDATETIMEOFFSET(),
  CONSTRAINT CK_chatbot_tool_calls_input_isjson CHECK (ISJSON(input) = 1),
  CONSTRAINT CK_chatbot_tool_calls_output_isjson CHECK (output IS NULL OR ISJSON(output) = 1),
  CONSTRAINT FK_chatbot_tool_calls_message FOREIGN KEY (message_id) REFERENCES dbo.chatbot_messages(id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.chatbot_feedback (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_chatbot_feedback PRIMARY KEY,
  message_id int NOT NULL,
  account_id int NULL,
  rating tinyint NULL,
  comment nvarchar(max) NULL,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_chatbot_feedback_created_at DEFAULT SYSDATETIMEOFFSET(),
  CONSTRAINT CK_chatbot_feedback_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),
  CONSTRAINT FK_chatbot_feedback_message FOREIGN KEY (message_id) REFERENCES dbo.chatbot_messages(id) ON DELETE CASCADE,
  CONSTRAINT FK_chatbot_feedback_account FOREIGN KEY (account_id) REFERENCES dbo.accounts(id) ON DELETE SET NULL
);
GO

/* ===== EmailVerificationOtp (OTP email; account_id NULL = trước khi đăng ký) ===== */

CREATE TABLE dbo.email_verification_otp (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_email_verification_otp PRIMARY KEY,
  account_id int NULL,
  email nvarchar(255) NOT NULL,
  purpose nvarchar(32) NOT NULL CONSTRAINT DF_EVO_purpose DEFAULT N'REGISTER',

  otp_hash nvarchar(255) NOT NULL,            -- lưu HASH OTP, không lưu OTP plain
  expires_at datetimeoffset(0) NOT NULL,
  consumed_at datetimeoffset(0) NULL,

  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_EVO_created_at DEFAULT SYSDATETIMEOFFSET(),

  CONSTRAINT FK_EVO_account FOREIGN KEY (account_id) REFERENCES dbo.accounts(id) ON DELETE CASCADE,
  CONSTRAINT CK_EVO_expires CHECK (expires_at > created_at)
);
GO
/* ===== RefreshToken ===== */
CREATE TABLE dbo.refresh_tokens (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_refresh_tokens PRIMARY KEY,
  account_id int NOT NULL,

  token_hash nvarchar(255) NOT NULL,          -- lưu HASH refresh token
  expires_at datetimeoffset(0) NOT NULL,
  revoked_at datetimeoffset(0) NULL,

  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_RT_created_at DEFAULT SYSDATETIMEOFFSET(),

  CONSTRAINT FK_RT_account FOREIGN KEY (account_id) REFERENCES dbo.accounts(id) ON DELETE CASCADE,
  CONSTRAINT CK_RT_expires CHECK (expires_at > created_at)
);
GO

/* ===== Access token blacklist (logout / JTI) ===== */
CREATE TABLE dbo.access_token_blacklist (
  id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_access_token_blacklist PRIMARY KEY,
  jti nvarchar(64) NOT NULL,
  account_id int NULL,
  expires_at datetimeoffset(0) NOT NULL,
  created_at datetimeoffset(0) NOT NULL CONSTRAINT DF_atb_created_at DEFAULT SYSDATETIMEOFFSET(),
  CONSTRAINT FK_atb_account FOREIGN KEY (account_id) REFERENCES dbo.accounts(id) ON DELETE SET NULL
);
GO

/* ===== Optional: stored procedure đặt lịch an toàn (chống trùng giờ bác sĩ) ===== */
CREATE OR ALTER PROCEDURE dbo.sp_create_appointment
  @patient_id int,
  @doctor_id int,
  @service_id int = NULL,
  @slot_id int = NULL,
  @room_id int = NULL,
  @starts_at datetimeoffset(0),
  @ends_at datetimeoffset(0),
  @reason nvarchar(max) = NULL,
  @note nvarchar(max) = NULL,
  @created_by_account_id int = NULL,
  @new_appointment_id int OUTPUT
AS
BEGIN
  SET NOCOUNT ON;
  SET XACT_ABORT ON;

  IF (@starts_at >= @ends_at)
    THROW 50001, 'Invalid time range', 1;

  -- Serializable để tránh race condition khi 2 người đặt cùng lúc
  SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
  BEGIN TRANSACTION;

  -- Nếu dùng slot_id (capacity=1) thì khóa slot để tránh double booking
  IF (@slot_id IS NOT NULL)
  BEGIN
    -- lock row slot
    SELECT 1
    FROM dbo.appointment_slots WITH (UPDLOCK, HOLDLOCK)
    WHERE id = @slot_id AND doctor_id = @doctor_id AND starts_at = @starts_at AND ends_at = @ends_at;

    IF (@@ROWCOUNT = 0)
    BEGIN
      ROLLBACK TRANSACTION;
      THROW 50002, 'Slot not found or mismatch', 1;
    END

    -- slot_id unique ở appointments => chỉ cần check tồn tại
    IF EXISTS (SELECT 1 FROM dbo.appointments WITH (UPDLOCK, HOLDLOCK) WHERE slot_id = @slot_id AND status IN (N'pending',N'confirmed',N'checked_in'))
    BEGIN
      ROLLBACK TRANSACTION;
      THROW 50003, 'Slot already booked', 1;
    END
  END

  -- Chặn trùng giờ theo doctor (các trạng thái còn hiệu lực)
  IF EXISTS (
    SELECT 1
    FROM dbo.appointments WITH (UPDLOCK, HOLDLOCK)
    WHERE doctor_id = @doctor_id
      AND status IN (N'pending',N'confirmed',N'checked_in')
      AND @starts_at < ends_at
      AND @ends_at > starts_at
  )
  BEGIN
    ROLLBACK TRANSACTION;
    THROW 50004, 'Doctor time is not available (overlap)', 1;
  END

  INSERT INTO dbo.appointments (
    patient_id, doctor_id, service_id, slot_id, room_id,
    starts_at, ends_at, status, reason, note, created_by_account_id
  )
  VALUES (
    @patient_id, @doctor_id, @service_id, @slot_id, @room_id,
    @starts_at, @ends_at, N'pending', @reason, @note, @created_by_account_id
  );

  SET @new_appointment_id = SCOPE_IDENTITY();

  COMMIT TRANSACTION;
END
GO

/* ===== Indexes (all indexes at the end) ===== */
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
GO

/* roles */
CREATE INDEX IX_roles_active_not_deleted ON dbo.roles(is_deleted, is_active) INCLUDE (code);
GO

/* accounts */
CREATE UNIQUE INDEX UX_accounts_email ON dbo.accounts(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX UX_accounts_phone ON dbo.accounts(phone) WHERE phone IS NOT NULL;
CREATE INDEX IX_accounts_role_id ON dbo.accounts(role_id);
GO

/* specialties */
CREATE UNIQUE INDEX UX_specialties_name ON dbo.specialties(name);
CREATE UNIQUE INDEX UX_specialties_code ON dbo.specialties(code) WHERE code IS NOT NULL;
GO

/* doctors */
CREATE UNIQUE INDEX UX_doctors_account_id ON dbo.doctors(account_id) WHERE account_id IS NOT NULL;
CREATE UNIQUE INDEX UX_doctors_email ON dbo.doctors(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX UX_doctors_phone ON dbo.doctors(phone) WHERE phone IS NOT NULL;
GO

/* patients */
CREATE UNIQUE INDEX UX_patients_account_id ON dbo.patients(account_id) WHERE account_id IS NOT NULL;
CREATE UNIQUE INDEX UX_patients_email ON dbo.patients(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX UX_patients_phone ON dbo.patients(phone) WHERE phone IS NOT NULL;
GO

/* doctor_specialties */
CREATE INDEX IX_doctor_specialties_specialty_id ON dbo.doctor_specialties(specialty_id);
GO

/* services */
CREATE UNIQUE INDEX UX_services_name ON dbo.services(name);
CREATE UNIQUE INDEX UX_services_code ON dbo.services(code) WHERE code IS NOT NULL;
CREATE INDEX IX_services_specialty ON dbo.services(specialty_id);
GO

/* rooms */
CREATE UNIQUE INDEX UX_rooms_name ON dbo.rooms(name);
CREATE UNIQUE INDEX UX_rooms_code ON dbo.rooms(code) WHERE code IS NOT NULL;
GO

/* doctor_working_hours */
CREATE UNIQUE INDEX UX_dwh_unique ON dbo.doctor_working_hours(doctor_id, day_of_week, start_time, end_time);
CREATE INDEX IX_dwh_room_id ON dbo.doctor_working_hours(room_id) WHERE room_id IS NOT NULL;
GO

/* doctor_time_off */
CREATE INDEX IX_dto_doctor_time ON dbo.doctor_time_off(doctor_id, starts_at);
GO

/* appointment_slots */
CREATE UNIQUE INDEX UX_slots_doctor_time ON dbo.appointment_slots(doctor_id, starts_at, ends_at);
CREATE INDEX IX_slots_doctor_starts ON dbo.appointment_slots(doctor_id, starts_at);
CREATE INDEX IX_slots_room_starts ON dbo.appointment_slots(room_id, starts_at) WHERE room_id IS NOT NULL;
GO

/* appointments */
CREATE UNIQUE INDEX UX_appointments_slot_id ON dbo.appointments(slot_id) WHERE slot_id IS NOT NULL;
CREATE INDEX IX_appointments_patient_time ON dbo.appointments(patient_id, starts_at DESC);
CREATE INDEX IX_appointments_doctor_time ON dbo.appointments(doctor_id, starts_at DESC);
CREATE INDEX IX_appointments_status ON dbo.appointments(status);
CREATE INDEX IX_appointments_doctor_status_time ON dbo.appointments(doctor_id, status, starts_at);
CREATE INDEX IX_appointments_room_time ON dbo.appointments(room_id, starts_at DESC) WHERE room_id IS NOT NULL;
CREATE INDEX IX_appointments_service_time ON dbo.appointments(service_id, starts_at DESC) WHERE service_id IS NOT NULL;
GO

/* invoices */
CREATE UNIQUE INDEX UX_invoices_appointment_id ON dbo.invoices(appointment_id) WHERE appointment_id IS NOT NULL;
CREATE INDEX IX_invoices_created_at ON dbo.invoices(created_at DESC);
GO

/* invoice_items */
CREATE INDEX IX_invoice_items_invoice ON dbo.invoice_items(invoice_id);
CREATE INDEX IX_invoice_items_service ON dbo.invoice_items(service_id) WHERE service_id IS NOT NULL;
GO

/* payments */
CREATE UNIQUE INDEX UX_payments_provider_txn ON dbo.payments(provider, provider_txn_id) WHERE provider_txn_id IS NOT NULL;
CREATE INDEX IX_payments_invoice_id ON dbo.payments(invoice_id);
CREATE INDEX IX_payments_status_paid_at ON dbo.payments(status, paid_at DESC) INCLUDE (provider, amount_cents);
GO

/* chatbot */
CREATE INDEX IX_chatbot_sessions_account ON dbo.chatbot_sessions(account_id, created_at DESC);
CREATE INDEX IX_chatbot_sessions_patient ON dbo.chatbot_sessions(patient_id, created_at DESC) WHERE patient_id IS NOT NULL;
GO
CREATE INDEX IX_chatbot_messages_session ON dbo.chatbot_messages(session_id, created_at);
GO
CREATE INDEX IX_chatbot_tool_calls_message_id ON dbo.chatbot_tool_calls(message_id);
GO
CREATE UNIQUE INDEX UX_chatbot_feedback_message_account ON dbo.chatbot_feedback(message_id, account_id) WHERE account_id IS NOT NULL;
GO

/* email_verification_otp */
CREATE INDEX IX_EVO_account_active
ON dbo.email_verification_otp(account_id, expires_at)
INCLUDE (consumed_at, created_at);
GO
CREATE INDEX IX_EVO_email_expires
ON dbo.email_verification_otp(email, expires_at)
INCLUDE (consumed_at, otp_hash, created_at);
GO

/* refresh_tokens */
CREATE UNIQUE INDEX UX_RT_token_hash ON dbo.refresh_tokens(token_hash);
CREATE INDEX IX_RT_account_valid ON dbo.refresh_tokens(account_id, expires_at) INCLUDE (revoked_at, created_at);
GO

/* access_token_blacklist */
CREATE UNIQUE INDEX UX_atb_jti ON dbo.access_token_blacklist(jti);
CREATE INDEX IX_atb_expires_at ON dbo.access_token_blacklist(expires_at);
GO
