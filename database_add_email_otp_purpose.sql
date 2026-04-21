/*
  Chạy script này trên database đã tồn tại (clinic_db) nếu bảng email_verification_otp chưa có cột purpose.
  Cài mới từ database_clinic_booking.sql đã gồm cột purpose — không cần chạy lại.
*/
IF NOT EXISTS (
  SELECT 1
  FROM sys.columns c
  INNER JOIN sys.tables t ON c.object_id = t.object_id
  WHERE t.name = N'email_verification_otp' AND c.name = N'purpose'
)
BEGIN
  ALTER TABLE dbo.email_verification_otp
    ADD purpose nvarchar(32) NOT NULL
    CONSTRAINT DF_email_verification_otp_purpose DEFAULT N'REGISTER';
END
GO
