package com.swp.adnV2.AdnV2.service;

import com.swp.adnV2.AdnV2.dto.AppointmentRequest;
import com.swp.adnV2.AdnV2.dto.AppointmentResponse;
import com.swp.adnV2.AdnV2.dto.AppointmentUpdateRequest;
import com.swp.adnV2.AdnV2.entity.*;
import com.swp.adnV2.AdnV2.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppointmentService {
    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private KitRepository kitRepository;

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private SampleTypeRepository sampleTypeRepository;  

    @Transactional
    public ResponseEntity<?> createGuestAppointment(Long serviceId, AppointmentRequest request) {
        List<String> errors = new ArrayList<>();
        if (request.getAppointmentDate() == null) {
            errors.add("Appointment date is required");
        }
        if (request.getFullName() == null || request.getFullName().isEmpty()) {
            errors.add("Full name is required");
        }
        if (request.getPhone() == null || request.getPhone().isEmpty()) {
            errors.add("Phone number is required");
        }
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            errors.add("Email is required");
        }
        if (request.getGender() == null || request.getGender().isEmpty()) {
            errors.add("Gender is required");
        }
        if (request.getServiceType() == null || request.getServiceType().isEmpty()) {
            errors.add("Service type is required");
        }

        TestPurpose testPurpose = TestPurpose.fromDisplayName(request.getTestPurpose());
        if(testPurpose == TestPurpose.OTHER && !("Khác".equalsIgnoreCase(request.getTestPurpose()))) {
            errors.add("Test purpose must be one of :Hành chính, Dân sự, Khác");
        }

        if(testPurpose == TestPurpose.HANH_CHINH){
            String fingerprintFile = request.getFingerprintFile();
            if(fingerprintFile == null || fingerprintFile.isEmpty()) {
                errors.add("Fingerprint file is required for test purpose 'Hành chính'");
            }
        }

        Services services = serviceRepository.findServicesByServiceId(serviceId);
        if (services == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Service not found with ID: " + serviceId);
        }


        KitComponent kitComponent = null;
        List<String> sampleTypeNames = request.getSampleTypes();
        int requestedSampleCount = (sampleTypeNames != null) ? sampleTypeNames.size() : 0;

        if (request.getKitComponentName() != null && !request.getKitComponentName().isEmpty()) {
            kitComponent = kitRepository.findByComponentName(request.getKitComponentName());
            if (kitComponent == null) {
                return ResponseEntity.badRequest().body("Invalid kit component name");
            }
            // Kiểm tra kitComponent này có thuộc service đã chọn không
            if (!kitComponent.getService().getServiceId().equals(serviceId)) {
                return ResponseEntity.badRequest().body("Kit component không thuộc dịch vụ đã chọn!");
            }
            // Kiểm tra số lượng kit đủ không
            if (kitComponent.getQuantity() < requestedSampleCount) {
                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("message", "Số lượng kit không đủ cho số lượng mẫu đăng ký!");
                errorBody.put("kitAvailable", kitComponent.getQuantity());
                errorBody.put("kitRequired", requestedSampleCount);
                return ResponseEntity.badRequest().body(errorBody);
            }
        }

        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(errors);
        }
        try {
            Appointment appointment = new Appointment();
            appointment.setAppointmentDate(request.getAppointmentDate());
            appointment.setFullName(request.getFullName());
            appointment.setDob(request.getDob());
            appointment.setPhone(request.getPhone());
            appointment.setEmail(request.getEmail());
            appointment.setGender(request.getGender());
            appointment.setTestPurpose(request.getTestPurpose());
            appointment.setServiceType(request.getServiceType());
            appointment.setCollectionSampleTime(request.getCollectionTime());
            appointment.setTestCategory(request.getTestCategory());
            appointment.setFingerprintFile(request.getFingerprintFile());
            appointment.setCollectionLocation(request.getCollectionLocation());
            appointment.setDistrict(request.getDistrict());
            appointment.setProvince(request.getProvince());
            appointment.setStatus("PENDING");

            appointment.setService(services);
            appointment.setKitComponent(kitComponent);

            Optional<Guest> guestOpt = guestRepository.findByPhone(request.getPhone());
            Guest guest;
            if (guestOpt.isPresent()) {
                guest = guestOpt.get();
            } else {
                guest = new Guest();
                guest.setFullName(request.getFullName());
                guest.setGender(request.getGender());
                guest.setDateOfBirth(request.getDob());
                guest.setPhone(request.getPhone());
                guest.setEmail(request.getEmail());
                guest = guestRepository.save(guest);
            }
            appointment.setGuest(guest);
            appointment = appointmentRepository.save(appointment);

            if (sampleTypeNames != null && !sampleTypeNames.isEmpty()) {
                for (String sampleTypeName : sampleTypeNames) {
                    Optional<SampleType> optSampleType = sampleTypeRepository.findByName(sampleTypeName);
                    if (optSampleType.isEmpty()) {
                        return ResponseEntity.badRequest().body("Invalid sample type: " + sampleTypeName);
                    }
                    SampleType sampleType = optSampleType.get();
                    CollectedSample collectedSample = new CollectedSample();
                    collectedSample.setAppointment(appointment);
                    collectedSample.setSampleType(sampleType);
                    collectedSample.setKitComponent(kitComponent); // Luôn dùng kit của appointment
                    sampleRepository.save(collectedSample);
                }
            }

            // Trừ số lượng kit sau khi lưu sample thành công
            if (kitComponent != null && requestedSampleCount > 0) {
                kitComponent.setQuantity(kitComponent.getQuantity() - requestedSampleCount);
                kitRepository.save(kitComponent);
            }

            AppointmentResponse appointmentResponse = convertToAppointmentResponse(appointment);
            return ResponseEntity.status(HttpStatus.CREATED).body(appointmentResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Failed to create appointment: " + e.getMessage());
        }
    }
    @Transactional
    public ResponseEntity<?> deleteAppointment(Long appointmentId) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isPresent()) {
            Appointment appointment = appointmentOpt.get();

            // Check trạng thái là PENDING hoặc CONFIRMED
            if ("PENDING".equalsIgnoreCase(appointment.getStatus()) || "CONFIRMED".equalsIgnoreCase(appointment.getStatus())) {
                KitComponent kitComponent = appointment.getKitComponent();
                if (kitComponent != null) {
                    // Đếm số lượng sample trong appointment này
                    int sampleCount = sampleRepository.findByAppointment_AppointmentId(appointmentId).size();
                    kitComponent.setQuantity(kitComponent.getQuantity() + sampleCount);
                    kitRepository.save(kitComponent);
                }
            }

            // Đánh dấu appointment là không hoạt động thay vì xóa
            appointment.setActive(false);
            appointmentRepository.save(appointment);

            return ResponseEntity.ok("Cuộc hẹn đã được đánh dấu là không hoạt động");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy cuộc hẹn với ID " + appointmentId);
        }
    }

    public AppointmentResponse convertToAppointmentResponse(Appointment appointment) {
        AppointmentResponse response = new AppointmentResponse();
        response.setAppointmentId(appointment.getAppointmentId());
        response.setFullName(appointment.getFullName());
        response.setDob(appointment.getDob());
        response.setPhone(appointment.getPhone());
        response.setEmail(appointment.getEmail());
        response.setGender(appointment.getGender());
        response.setTestPurpose(appointment.getTestPurpose());
        response.setTestCategory(appointment.getTestCategory());
        response.setServiceType(appointment.getServiceType());
        response.setCollectionSampleTime(appointment.getCollectionSampleTime());
        response.setFingerprintFile(appointment.getFingerprintFile());
        response.setCollectionLocation(appointment.getCollectionLocation());
        response.setDistrict(appointment.getDistrict());
        response.setProvince(appointment.getProvince());
        response.setStatus(appointment.getStatus());
        response.setAppointmentDate(appointment.getAppointmentDate());
        if (appointment.getUser() != null) {
            response.setUserId(appointment.getUser().getUserId());
        } else {
            response.setUserId(null);
        }

        if (appointment.getKitComponent() != null) {
            response.setKitComponentName(appointment.getKitComponent().getComponentName());
        } else {
            response.setKitComponentName(null);
        }

        List<CollectedSample> collectedSamples = sampleRepository.findByAppointment_AppointmentId(appointment.getAppointmentId());
        if (collectedSamples != null && !collectedSamples.isEmpty()) {
            List<String> sampleTypes = collectedSamples.stream()
                    .map(sample -> sample.getSampleType() != null ? sample.getSampleType().getName() : null)
                    .collect(Collectors.toList());
            response.setSampleTypes(sampleTypes);

            // mapping SampleInfo list mới
            List<AppointmentResponse.SampleInfo> sampleInfos = collectedSamples.stream().map(sample -> {
                AppointmentResponse.SampleInfo info = new AppointmentResponse.SampleInfo();
                info.setSampleId(sample.getSampleId());
                info.setSampleType(sample.getSampleType() != null ? sample.getSampleType().getName() : null);
                if (sample.getParticipant() != null) {
                    info.setParticipantFullName(sample.getParticipant().getFullName());
                } else {
                    info.setParticipantFullName(null);
                }
                return info;
            }).collect(Collectors.toList());
            response.setSamples(sampleInfos);
        } else {
            response.setSampleTypes(Collections.emptyList());
            response.setSamples(Collections.emptyList());
        }

        Payment payment = paymentRepository.findByAppointment_AppointmentId(appointment.getAppointmentId());
        if (payment != null) {
            response.setPaymentStatus(payment.getStatus());
        } else {
            response.setPaymentStatus("Pending");
        }

        return response;
    }

    public ResponseEntity<List<AppointmentResponse>> getAllAppointments() {
        List<Appointment> appointments = appointmentRepository.findByIsActiveTrue();
        if (appointments.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<AppointmentResponse> responseList = appointments.stream()
                .map(this::convertToAppointmentResponse) // Sử dụng method reference nếu có thể
                .collect(Collectors.toList());
        return new ResponseEntity<>(responseList, HttpStatus.OK);
    }


    public List<Appointment> getAppointmentByUsernameAndStatus(String username, String status) {
        if(status != null && !status.isEmpty()){
            try {
                StatusAppointment statusEnum = StatusAppointment.valueOf(status.toUpperCase());
                return appointmentRepository.findByUsers_UsernameAndStatus(username, status);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid status: " + status);
            }
        } else {
            return appointmentRepository.findByUsers_UsernameAndIsActiveTrue(username);
        }
    }

    @Transactional
public ResponseEntity<?> createAppointment(Long serviceId,AppointmentRequest request, String username) {
    List<String> errors = new ArrayList<>();
    if (request.getAppointmentDate() == null) errors.add("Appointment date is required");
    if (request.getFullName() == null || request.getFullName().isEmpty()) errors.add("Full name is required");
    if (request.getPhone() == null || request.getPhone().isEmpty()) errors.add("Phone number is required");
    if (request.getEmail() == null || request.getEmail().isEmpty()) errors.add("Email is required");
    if (request.getGender() == null || request.getGender().isEmpty()) errors.add("Gender is required");
    if (request.getServiceType() == null || request.getServiceType().isEmpty()) errors.add("Service type is required");

    TestPurpose testPurpose = TestPurpose.fromDisplayName(request.getTestPurpose());
    if(testPurpose == TestPurpose.OTHER && !("Khác".equalsIgnoreCase(request.getTestPurpose()))) {
        errors.add("Test purpose must be one of :Hành chính, Dân sự, Khác");
    }

    if(testPurpose == TestPurpose.HANH_CHINH){
        String fingerprintFile = request.getFingerprintFile();
        if(fingerprintFile == null || fingerprintFile.isEmpty()) {
            errors.add("Fingerprint file is required for test purpose 'Hành chính'");
        }
    }

        Services services = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found with ID: " + serviceId));

    KitComponent kitComponent = null;
    List<String> sampleTypes = request.getSampleTypes();
    int requestedSampleCount = (sampleTypes != null) ? sampleTypes.size() : 0;

    if (request.getKitComponentName() != null && !request.getKitComponentName().isEmpty()) {
        kitComponent = kitRepository.findByComponentName(request.getKitComponentName());
        if (kitComponent == null) {
            errors.add("Invalid kit component name");
        } else if (services != null && !kitComponent.getService().getServiceId().equals(serviceId)) {
            errors.add("Kit component không thuộc dịch vụ đã chọn!");
        } else if (kitComponent.getQuantity() < requestedSampleCount) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Số lượng kit không đủ cho số lượng mẫu đăng ký!");
            errorBody.put("kitAvailable", kitComponent.getQuantity());
            errorBody.put("kitRequired", requestedSampleCount);
            return ResponseEntity.badRequest().body(errorBody);
        }
    }

        // Validate user nếu có
        Users users = null;
        if (username != null && !username.trim().isEmpty()) {
            users = userRepository.findByUsername(username);
            if (users == null) {
                errors.add("User with username " + username + " not found");
            }
        }

        // Nếu có lỗi thì trả về luôn
        if (!errors.isEmpty()) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Validation failed");
            errorBody.put("errors", errors);
            return ResponseEntity.badRequest().body(errorBody);
        }
    try {
        Appointment appointment = new Appointment();
        appointment.setFullName(request.getFullName());
        appointment.setDob(request.getDob());
        appointment.setPhone(request.getPhone());
        appointment.setEmail(request.getEmail());
        appointment.setGender(request.getGender());
        appointment.setTestPurpose(request.getTestPurpose());
        appointment.setServiceType(request.getServiceType());
        appointment.setCollectionSampleTime(request.getCollectionTime());
        appointment.setTestCategory(request.getTestCategory());
        appointment.setFingerprintFile(request.getFingerprintFile());
        appointment.setCollectionLocation(request.getCollectionLocation());
        appointment.setDistrict(request.getDistrict());
        appointment.setProvince(request.getProvince());
        appointment.setStatus("PENDING");
        appointment.setService(services);
        appointment.setKitComponent(kitComponent);
        if (users != null) appointment.setUser(users);
        appointment.setAppointmentDate(request.getAppointmentDate());


        appointment = appointmentRepository.save(appointment);

        if (sampleTypes != null && !sampleTypes.isEmpty()) {
            for (String sampleTypeName : sampleTypes) {
                Optional<SampleType> optionalSampleType = sampleTypeRepository.findByName(sampleTypeName);
                if (optionalSampleType.isEmpty()) {
                    Map<String, Object> errorBody = new HashMap<>();
                    errorBody.put("message", "Invalid sample type: " + sampleTypeName);
                    return ResponseEntity.badRequest().body(errorBody);
                }
                SampleType sampleType = optionalSampleType.get();
                CollectedSample collectedSample = new CollectedSample();
                collectedSample.setAppointment(appointment);
                collectedSample.setUsers(appointment.getUser());
                collectedSample.setCollectedDate(LocalDate.now());
                collectedSample.setKitComponent(kitComponent);
                collectedSample.setSampleType(sampleType);
                sampleRepository.save(collectedSample);
            }
        }

        // Trừ số lượng kit sau khi đã lưu hết sample
        if (kitComponent != null && requestedSampleCount > 0) {
            kitComponent.setQuantity(kitComponent.getQuantity() - requestedSampleCount);
            kitRepository.save(kitComponent);
        }

        AppointmentResponse response = convertToAppointmentResponse(appointment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (Exception e) {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("message", "Failed to create appointment");
        errorBody.put("error", e.getMessage());
        return ResponseEntity.badRequest().body(errorBody);
    }
}


    public ResponseEntity<?> viewAppointments(String username){
        try {
            Users user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User with username " + username + " not found");
            }
            List<Appointment> appointments = appointmentRepository.findByUsers_UserIdAndIsActiveTrue(user.getUserId());

            List<AppointmentResponse> responseList = appointments.stream()
                    .map(this::convertToAppointmentResponse)
                    .collect(Collectors.toList());
            return new ResponseEntity<>(responseList, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error: " + e.getMessage());
        }


    }



    /**
     * Xem thông tin chi tiết của một cuộc hẹn
     * @param appointmentId ID của cuộc hẹn
     * @return ResponseEntity chứa thông tin cuộc hẹn hoặc thông báo lỗi
     */
    public ResponseEntity<?> getAppointmentById(Long appointmentId) {
        try {
            Optional<Appointment> appointment = appointmentRepository.findById(appointmentId);
            if (appointment.isPresent()) {
                AppointmentResponse response = convertToAppointmentResponse(appointment.get());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Appointment with ID " + appointmentId + " not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error: " + e.getMessage());
        }
    }


    public ResponseEntity<?> updateAppointment(Long appointmentId, AppointmentUpdateRequest updateRequest) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isPresent()) {
            Appointment appointment = appointmentOpt.get();
            String oldStatus = appointment.getStatus();
            boolean kitRestored = false;

            if (updateRequest.getStatus() != null && !updateRequest.getStatus().isEmpty()) {
                boolean isValid = Arrays.stream(StatusAppointment.values())
                        .anyMatch(s -> s.name().equalsIgnoreCase(updateRequest.getStatus()));
                if (!isValid) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Invalid status: " + updateRequest.getStatus());
                }
                String newStatus = updateRequest.getStatus().toUpperCase();

                // --- Kiểm tra nếu chuyển sang CANCELLED và trạng thái cũ là PENDING hoặc CONFIRMED ---
                if ("CANCELLED".equalsIgnoreCase(newStatus)
                        && ("PENDING".equalsIgnoreCase(oldStatus) || "CONFIRMED".equalsIgnoreCase(oldStatus))) {
                    KitComponent kitComponent = appointment.getKitComponent();
                    if (kitComponent != null) {
                        int sampleCount = sampleRepository.findByAppointment_AppointmentId(appointmentId).size();
                        kitComponent.setQuantity(kitComponent.getQuantity() + sampleCount);
                        kitRepository.save(kitComponent);
                        kitRestored = true;
                    }
                }
                appointment.setStatus(newStatus);
            }
            boolean updatedSample = false;

            if (updateRequest.getKitComponentName() != null && !updateRequest.getKitComponentName().isEmpty()) {
                // Lấy service từ appointment
                Services service = appointment.getService();
                if (service == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Appointment does not have an associated service");
                }

                Long serviceId = service.getServiceId();

                // Tìm kiếm KitComponent trong service này với tên đã cho
                List<KitComponent> matchingComponents = kitRepository.findByService_ServiceIdAndComponentNameContainingIgnoreCase(
                        serviceId, updateRequest.getKitComponentName());

                if (matchingComponents.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("No kit component with name '" + updateRequest.getKitComponentName() +
                                    "' found for the service ID " + serviceId);
                }

                // Lấy KitComponent đầu tiên khớp (hoặc có thể thêm logic để chọn cái phù hợp nhất)
                KitComponent kitComponent = matchingComponents.get(0);

                List<CollectedSample> collectedSamples = sampleRepository.findByAppointment_AppointmentId(appointmentId);
                if (!collectedSamples.isEmpty()) {
                    for (CollectedSample collectedSample : collectedSamples) {
                        collectedSample.setKitComponent(kitComponent);
                        sampleRepository.save(collectedSample);
                    }
                    updatedSample = true;
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("No sample found for the appointment ID " + appointmentId);
                }

            }

            // Cập nhật file kết quả nếu được cung cấp (giờ sẽ cập nhật vào Result)
            if (updateRequest.getResultFile() != null && !updateRequest.getResultFile().isEmpty()) {
                List<CollectedSample> collectedSamples = sampleRepository.findByAppointment_AppointmentId(appointmentId);
                if (collectedSamples == null || collectedSamples.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("No sample found for the appointment ID " + appointmentId);
                }
                for (CollectedSample collectedSample : collectedSamples) {
                    Result result = resultRepository.findByCollectedSample_SampleId(collectedSample.getSampleId());
                    if (result != null) {
                        result.setResultData(updateRequest.getResultFile());
                        resultRepository.save(result);
                    }
                    // Nếu chưa có result, bạn có thể tạo mới ở đây nếu nghiệp vụ yêu cầu
                }
            }

            appointmentRepository.save(appointment);
            String msg = kitRestored ? "Appointment updated and kit quantity restored successfully" : "Appointment updated successfully";
            return ResponseEntity.ok(msg);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Appointment with ID " + appointmentId + " not found");
        }
    }

    public ResponseEntity<?> findAppointmentsByEmailAndPhone(String email, String phone) {
        try {
            List<Appointment> appointments = appointmentRepository.findByEmailAndPhoneAndIsActiveTrue(email, phone);
            if (appointments.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            List<AppointmentResponse> responseList = appointments.stream()
                    .map(this::convertToAppointmentResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Failed to find appointments: " + e.getMessage());
        }
    }
}
