package com.example.frontend_bookingcare.api;

import com.example.frontend_bookingcare.BuildConfig;
import com.example.frontend_bookingcare.network.TokenAuthInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {

    private static volatile Retrofit retrofit;
    private static volatile AuthApiService authApi;
    private static volatile DoctorApiService doctorApi;
    private static volatile SpecialtyApiService specialtyApi;
    private static volatile BookingApiService bookingApi;
    private static volatile DoctorPanelApiService doctorPanelApi;
    private static volatile LegalApiService legalApi;
    private static volatile PatientAppointmentsApiService patientAppointmentsApi;
    private static volatile PatientProfileApiService patientProfileApi;
    private static volatile PatientChatApiService patientChatApi;
    private static volatile DoctorChatApiService doctorChatApi;
    private static volatile ChatbotApiService chatbotApi;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private RetrofitClient() {
    }

    public static Gson gson() {
        return GSON;
    }

    private static Retrofit retrofit() {
        if (retrofit == null) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null) {
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            // Interceptor tự refresh access token khi server trả 401/403 rồi
                            // replay request gốc — tránh phải đăng nhập lại sau 1 giờ.
                            .addInterceptor(new TokenAuthInterceptor(BuildConfig.API_BASE_URL, GSON))
                            .build();
                    retrofit = new Retrofit.Builder()
                            .baseUrl(BuildConfig.API_BASE_URL)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create(GSON))
                            .build();
                }
            }
        }
        return retrofit;
    }

    public static AuthApiService authApi() {
        if (authApi == null) {
            synchronized (RetrofitClient.class) {
                if (authApi == null) {
                    authApi = retrofit().create(AuthApiService.class);
                }
            }
        }
        return authApi;
    }

    public static DoctorApiService doctorApi() {
        if (doctorApi == null) {
            synchronized (RetrofitClient.class) {
                if (doctorApi == null) {
                    doctorApi = retrofit().create(DoctorApiService.class);
                }
            }
        }
        return doctorApi;
    }

    public static SpecialtyApiService specialtyApi() {
        if (specialtyApi == null) {
            synchronized (RetrofitClient.class) {
                if (specialtyApi == null) {
                    specialtyApi = retrofit().create(SpecialtyApiService.class);
                }
            }
        }
        return specialtyApi;
    }

    public static BookingApiService bookingApi() {
        if (bookingApi == null) {
            synchronized (RetrofitClient.class) {
                if (bookingApi == null) {
                    bookingApi = retrofit().create(BookingApiService.class);
                }
            }
        }
        return bookingApi;
    }

    public static DoctorPanelApiService doctorPanelApi() {
        if (doctorPanelApi == null) {
            synchronized (RetrofitClient.class) {
                if (doctorPanelApi == null) {
                    doctorPanelApi = retrofit().create(DoctorPanelApiService.class);
                }
            }
        }
        return doctorPanelApi;
    }

    public static LegalApiService legalApi() {
        if (legalApi == null) {
            synchronized (RetrofitClient.class) {
                if (legalApi == null) {
                    legalApi = retrofit().create(LegalApiService.class);
                }
            }
        }
        return legalApi;
    }

    public static PatientAppointmentsApiService patientAppointmentsApi() {
        if (patientAppointmentsApi == null) {
            synchronized (RetrofitClient.class) {
                if (patientAppointmentsApi == null) {
                    patientAppointmentsApi = retrofit().create(PatientAppointmentsApiService.class);
                }
            }
        }
        return patientAppointmentsApi;
    }

    public static PatientProfileApiService patientProfileApi() {
        if (patientProfileApi == null) {
            synchronized (RetrofitClient.class) {
                if (patientProfileApi == null) {
                    patientProfileApi = retrofit().create(PatientProfileApiService.class);
                }
            }
        }
        return patientProfileApi;
    }

    public static PatientChatApiService patientChatApi() {
        if (patientChatApi == null) {
            synchronized (RetrofitClient.class) {
                if (patientChatApi == null) {
                    patientChatApi = retrofit().create(PatientChatApiService.class);
                }
            }
        }
        return patientChatApi;
    }

    public static DoctorChatApiService doctorChatApi() {
        if (doctorChatApi == null) {
            synchronized (RetrofitClient.class) {
                if (doctorChatApi == null) {
                    doctorChatApi = retrofit().create(DoctorChatApiService.class);
                }
            }
        }
        return doctorChatApi;
    }

    public static ChatbotApiService chatbotApi() {
        if (chatbotApi == null) {
            synchronized (RetrofitClient.class) {
                if (chatbotApi == null) {
                    chatbotApi = retrofit().create(ChatbotApiService.class);
                }
            }
        }
        return chatbotApi;
    }
}
