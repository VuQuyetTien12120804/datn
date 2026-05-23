package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.common.ApiException;
import com.bookingcare.backend_bookingcare.common.AppointmentStatus;
import com.bookingcare.backend_bookingcare.entity.Doctor;
import com.bookingcare.backend_bookingcare.entity.Message;
import com.bookingcare.backend_bookingcare.entity.MessageThread;
import com.bookingcare.backend_bookingcare.entity.Patient;
import com.bookingcare.backend_bookingcare.repository.AppointmentRepository;
import com.bookingcare.backend_bookingcare.repository.DoctorRepository;
import com.bookingcare.backend_bookingcare.repository.MessageRepository;
import com.bookingcare.backend_bookingcare.repository.MessageThreadRepository;
import com.bookingcare.backend_bookingcare.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MessagingService {

    private static final Set<String> CHAT_BLOCK_STATUSES = Set.of("cancelled", "no_show");
    private static final Set<String> CHAT_ACTIVE_STATUSES = Set.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.CHECKED_IN
    );
    private static final String THREAD_DOCTOR = "doctor";
    private static final String THREAD_SUPPORT = "support";

    private final MessageThreadRepository threadRepository;
    private final MessageRepository messageRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public List<Map<String, Object>> listPatientThreads(int accountId) {
        Patient patient = requirePatient(accountId);
        ensureDoctorThreadsFromAppointments(patient.getId());
        ensureSupportThread(patient.getId());

        List<MessageThread> threads = threadRepository.findByPatientIdOrderByLastMessageAtDescUpdatedAtDesc(patient.getId());
        List<Map<String, Object>> out = new ArrayList<>();
        Set<String> seenDoctorKeys = new HashSet<>();
        for (MessageThread thread : threads) {
            if (THREAD_DOCTOR.equals(thread.getThreadType()) && thread.getDoctorId() != null) {
                String identity = doctorIdentityKey(thread.getDoctorId());
                if (!seenDoctorKeys.add(identity)) {
                    continue;
                }
            }
            out.add(toPatientThreadView(thread, patient));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listDoctorThreads(int accountId) {
        Doctor doctor = requireDoctor(accountId);
        List<Integer> siblingIds = siblingDoctorIds(doctor.getId());
        List<MessageThread> threads = threadRepository.findByDoctorIdInOrderByLastMessageAtDescUpdatedAtDesc(siblingIds);
        List<Map<String, Object>> out = new ArrayList<>();
        for (MessageThread thread : threads) {
            out.add(toStaffThreadView(thread, accountId, doctor.getFullName()));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAdminThreads(int accountId) {
        List<MessageThread> threads = threadRepository.findByThreadTypeOrderByLastMessageAtDescUpdatedAtDesc(THREAD_SUPPORT);
        List<Map<String, Object>> out = new ArrayList<>();
        for (MessageThread thread : threads) {
            out.add(toStaffThreadView(thread, accountId, "Chăm Sóc Khách Hàng"));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listMessages(String role, int accountId, String threadKey) {
        MessageThread thread = resolveThreadForRead(role, accountId, threadKey);
        List<Message> messages = messageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId());
        List<Map<String, Object>> out = new ArrayList<>();
        for (Message message : messages) {
            out.add(toMessageView(message, accountId));
        }
        return out;
    }

    @Transactional
    public Map<String, Object> sendPatientMessage(int accountId, String threadKey, String content) {
        Patient patient = requirePatient(accountId);
        String trimmed = requireContent(content);
        MessageThread thread = resolveOrCreatePatientThread(patient, threadKey);
        assertPatientCanSend(patient.getId(), thread);
        return persistMessage(thread, accountId, "patient", trimmed);
    }

    @Transactional
    public Map<String, Object> sendDoctorMessage(int accountId, String threadKey, String content) {
        Doctor doctor = requireDoctor(accountId);
        String trimmed = requireContent(content);
        MessageThread thread = resolveThreadForStaff("doctor", accountId, threadKey);
        if (!THREAD_DOCTOR.equals(thread.getThreadType())
                || thread.getDoctorId() == null
                || !siblingDoctorIds(doctor.getId()).contains(thread.getDoctorId())) {
            throw new ApiException(403, "Forbidden");
        }
        return persistMessage(thread, accountId, "doctor", trimmed);
    }

    @Transactional
    public Map<String, Object> sendAdminMessage(int accountId, String threadKey, String content) {
        String trimmed = requireContent(content);
        MessageThread thread = resolveThreadForStaff("admin", accountId, threadKey);
        if (!THREAD_SUPPORT.equals(thread.getThreadType())) {
            throw new ApiException(403, "Forbidden");
        }
        return persistMessage(thread, accountId, "admin", trimmed);
    }

    @Transactional
    public void markRead(String role, int accountId, String threadKey) {
        MessageThread thread = resolveThreadForRead(role, accountId, threadKey);
        messageRepository.markReadForThread(thread.getId(), accountId, OffsetDateTime.now());
    }

    private Map<String, Object> persistMessage(MessageThread thread, int senderAccountId, String senderRole, String content) {
        OffsetDateTime now = OffsetDateTime.now();
        Message message = new Message();
        message.setThreadId(thread.getId());
        message.setSenderAccountId(senderAccountId);
        message.setSenderRole(senderRole);
        message.setContent(content);
        message.setCreatedAt(now);
        messageRepository.save(message);

        thread.setLastMessageAt(now);
        thread.setUpdatedAt(now);
        threadRepository.save(thread);

        return toMessageView(message, senderAccountId);
    }

    private void ensureDoctorThreadsFromAppointments(int patientId) {
        List<com.bookingcare.backend_bookingcare.entity.Appointment> appointments =
                appointmentRepository.findByPatientIdOrderByStartsAtDesc(patientId);
        Set<Integer> doctorIds = new HashSet<>();
        for (var appointment : appointments) {
            if (appointment.getDoctorId() == null) continue;
            String status = appointment.getStatus() != null
                    ? appointment.getStatus().toLowerCase(Locale.ROOT)
                    : "";
            if (CHAT_BLOCK_STATUSES.contains(status)) continue;
            if (!doctorIds.add(appointment.getDoctorId())) continue;
            getOrCreateDoctorThread(patientId, appointment.getDoctorId());
        }
    }

    private MessageThread ensureSupportThread(int patientId) {
        return threadRepository.findByPatientIdAndThreadType(patientId, THREAD_SUPPORT)
                .orElseGet(() -> createThread(THREAD_SUPPORT, patientId, null));
    }

    private MessageThread getOrCreateDoctorThread(int patientId, int doctorId) {
        List<Integer> siblings = siblingDoctorIds(doctorId);
        int canonicalId = siblings.get(0);
        for (Integer siblingId : siblings) {
            var existing = threadRepository.findByPatientIdAndDoctorId(patientId, siblingId);
            if (existing.isPresent()) {
                MessageThread thread = existing.get();
                if (!Integer.valueOf(canonicalId).equals(thread.getDoctorId())) {
                    thread.setDoctorId(canonicalId);
                    thread = threadRepository.save(thread);
                }
                return thread;
            }
        }
        return threadRepository.findByPatientIdAndDoctorId(patientId, canonicalId)
                .orElseGet(() -> createThread(THREAD_DOCTOR, patientId, canonicalId));
    }

    private MessageThread createThread(String type, int patientId, Integer doctorId) {
        OffsetDateTime now = OffsetDateTime.now();
        MessageThread thread = new MessageThread();
        thread.setThreadType(type);
        thread.setPatientId(patientId);
        thread.setDoctorId(doctorId);
        thread.setCreatedAt(now);
        thread.setUpdatedAt(now);
        return threadRepository.save(thread);
    }

    private MessageThread resolveOrCreatePatientThread(Patient patient, String threadKey) {
        ParsedThreadKey parsed = ParsedThreadKey.parse(threadKey);
        if (parsed.support()) {
            return ensureSupportThread(patient.getId());
        }
        doctorRepository.findById(parsed.doctorId())
                .orElseThrow(() -> new ApiException(404, "Doctor not found"));
        return getOrCreateDoctorThread(patient.getId(), parsed.doctorId());
    }

    private MessageThread resolveThreadForRead(String role, int accountId, String threadKey) {
        return switch (role.toLowerCase(Locale.ROOT)) {
            case "patient" -> resolveThreadForPatient(requirePatient(accountId), threadKey);
            case "doctor" -> resolveThreadForStaff("doctor", accountId, threadKey);
            case "admin" -> resolveThreadForStaff("admin", accountId, threadKey);
            default -> throw new ApiException(403, "Forbidden");
        };
    }

    private MessageThread resolveThreadForPatient(Patient patient, String threadKey) {
        ParsedThreadKey parsed = ParsedThreadKey.parse(threadKey);
        if (parsed.support()) {
            return ensureSupportThread(patient.getId());
        }
        doctorRepository.findById(parsed.doctorId())
                .orElseThrow(() -> new ApiException(404, "Doctor not found"));
        return getOrCreateDoctorThread(patient.getId(), parsed.doctorId());
    }

    private MessageThread resolveThreadForStaff(String role, int accountId, String threadKey) {
        if (threadKey != null && threadKey.startsWith("thread:")) {
            int threadId = parseIntSuffix(threadKey, "thread:");
            MessageThread thread = threadRepository.findById(threadId)
                    .orElseThrow(() -> new ApiException(404, "Thread not found"));
            authorizeStaffThread(role, accountId, thread);
            return thread;
        }
        if ("doctor".equals(role)) {
            Doctor doctor = requireDoctor(accountId);
            ParsedThreadKey parsed = ParsedThreadKey.parse(threadKey);
            if (parsed.support()) {
                throw new ApiException(404, "Thread not found");
            }
            List<Integer> siblingIds = siblingDoctorIds(doctor.getId());
            return threadRepository.findByDoctorIdInOrderByLastMessageAtDescUpdatedAtDesc(siblingIds).stream()
                    .filter(t -> siblingDoctorIds(parsed.doctorId()).contains(t.getDoctorId()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(404, "Thread not found"));
        }
        throw new ApiException(400, "Invalid thread key");
    }

    private void authorizeStaffThread(String role, int accountId, MessageThread thread) {
        if ("doctor".equals(role)) {
            Doctor doctor = requireDoctor(accountId);
            if (!THREAD_DOCTOR.equals(thread.getThreadType()) || thread.getDoctorId() == null) {
                throw new ApiException(403, "Forbidden");
            }
            if (!siblingDoctorIds(doctor.getId()).contains(thread.getDoctorId())) {
                throw new ApiException(403, "Forbidden");
            }
            return;
        }
        if ("admin".equals(role)) {
            if (!THREAD_SUPPORT.equals(thread.getThreadType())) {
                throw new ApiException(403, "Forbidden");
            }
            return;
        }
        throw new ApiException(403, "Forbidden");
    }

    private void assertPatientCanSend(int patientId, MessageThread thread) {
        if (THREAD_SUPPORT.equals(thread.getThreadType())) {
            return;
        }
        if (thread.getDoctorId() == null) {
            throw new ApiException(400, "Invalid doctor thread");
        }
        if (!patientCanChatWithDoctor(patientId, thread.getDoctorId())) {
            throw new ApiException(403, "Không thể nhắn tin khi lịch hẹn đã kết thúc hoặc đã hủy");
        }
    }

    private boolean patientCanChatWithDoctor(int patientId, int doctorId) {
        List<Integer> siblingIds = siblingDoctorIds(doctorId);
        return appointmentRepository.findByPatientIdOrderByStartsAtDesc(patientId).stream()
                .filter(a -> a.getDoctorId() != null && siblingIds.contains(a.getDoctorId()))
                .anyMatch(a -> CHAT_ACTIVE_STATUSES.contains(AppointmentStatus.normalize(a.getStatus())));
    }

    private List<Integer> siblingDoctorIds(int doctorId) {
        Doctor ref = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ApiException(404, "Doctor not found"));
        String identity = doctorIdentityKey(ref.getFullName());
        return doctorRepository.findAll().stream()
                .filter(d -> identity.equals(doctorIdentityKey(d.getFullName())))
                .map(Doctor::getId)
                .sorted()
                .toList();
    }

    private String doctorIdentityKey(Integer doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ApiException(404, "Doctor not found"));
        return doctorIdentityKey(doctor.getFullName());
    }

    private static String doctorIdentityKey(String fullName) {
        return normalizeDoctorName(fullName);
    }

    private static String normalizeDoctorName(String fullName) {
        if (fullName == null) return "";
        return fullName
                .replaceFirst("(?i)^(BS\\.|Bs\\.|Dr\\.)\\s*", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> toPatientThreadView(MessageThread thread, Patient patient) {
        Map<String, Object> map = baseThreadView(thread, patient.getAccountId());
        if (THREAD_SUPPORT.equals(thread.getThreadType())) {
            map.put("threadKey", "support:cskh");
            map.put("title", "Chăm Sóc Khách Hàng");
            map.put("subtitle", patient.getFullName());
        } else {
            int canonicalId = siblingDoctorIds(thread.getDoctorId()).get(0);
            Doctor doctor = doctorRepository.findById(canonicalId)
                    .orElseThrow(() -> new ApiException(404, "Doctor not found"));
            map.put("threadKey", "doctor:" + canonicalId);
            map.put("title", "Bác sĩ " + doctor.getFullName());
            map.put("subtitle", patient.getFullName());
        }
        boolean canSend = THREAD_SUPPORT.equals(thread.getThreadType())
                || patientCanChatWithDoctor(patient.getId(), thread.getDoctorId());
        map.put("locked", !canSend);
        map.put("canSend", canSend);
        return map;
    }

    private Map<String, Object> toStaffThreadView(MessageThread thread, int accountId, String fallbackTitle) {
        Patient patient = patientRepository.findById(thread.getPatientId())
                .orElseThrow(() -> new ApiException(404, "Patient not found"));
        Map<String, Object> map = baseThreadView(thread, accountId);
        map.put("threadKey", "thread:" + thread.getId());
        map.put("title", patient.getFullName());
        if (THREAD_SUPPORT.equals(thread.getThreadType())) {
            map.put("subtitle", fallbackTitle);
        } else {
            map.put("subtitle", "Bác sĩ " + fallbackTitle);
        }
        map.put("locked", false);
        map.put("canSend", true);
        return map;
    }

    private Map<String, Object> baseThreadView(MessageThread thread, int viewerAccountId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("threadId", thread.getId());
        map.put("threadType", thread.getThreadType());
        map.put("patientId", thread.getPatientId());
        map.put("doctorId", thread.getDoctorId());
        map.put("unreadCount", messageRepository.countByThreadIdAndReadAtIsNullAndSenderAccountIdNot(
                thread.getId(), viewerAccountId));

        Message last = messageRepository.findFirstByThreadIdOrderByCreatedAtDesc(thread.getId()).orElse(null);
        map.put("lastMessage", last != null ? last.getContent() : "");
        long updatedAtMs = 0L;
        if (thread.getLastMessageAt() != null) {
            updatedAtMs = thread.getLastMessageAt().toInstant().toEpochMilli();
        } else if (thread.getUpdatedAt() != null) {
            updatedAtMs = thread.getUpdatedAt().toInstant().toEpochMilli();
        }
        map.put("updatedAtMs", updatedAtMs);
        return map;
    }

    private Map<String, Object> toMessageView(Message message, int viewerAccountId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("messageId", message.getId());
        map.put("senderRole", message.getSenderRole());
        map.put("content", message.getContent());
        map.put("fromMe", message.getSenderAccountId() == viewerAccountId);
        long createdAtMs = message.getCreatedAt() != null
                ? message.getCreatedAt().toInstant().toEpochMilli()
                : System.currentTimeMillis();
        map.put("createdAtMs", createdAtMs);
        return map;
    }

    private Patient requirePatient(int accountId) {
        return patientRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ApiException(404, "Patient profile not found"));
    }

    private Doctor requireDoctor(int accountId) {
        return doctorRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ApiException(404, "Doctor profile not found"));
    }

    private static String requireContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ApiException(400, "Message content is required");
        }
        String trimmed = content.trim();
        if (trimmed.length() > 4000) {
            throw new ApiException(400, "Message too long");
        }
        return trimmed;
    }

    private static int parseIntSuffix(String value, String prefix) {
        try {
            return Integer.parseInt(value.substring(prefix.length()));
        } catch (Exception ex) {
            throw new ApiException(400, "Invalid thread key");
        }
    }

    private record ParsedThreadKey(String rawType, int doctorId) {
        static ParsedThreadKey parse(String threadKey) {
            if (threadKey == null || threadKey.isBlank()) {
                throw new ApiException(400, "Invalid thread key");
            }
            if ("support:cskh".equals(threadKey)) {
                return new ParsedThreadKey("support", 0);
            }
            if (threadKey.startsWith("doctor:")) {
                return new ParsedThreadKey("doctor", parseIntSuffix(threadKey, "doctor:"));
            }
            throw new ApiException(400, "Invalid thread key");
        }

        boolean support() {
            return "support".equals(rawType);
        }
    }
}
